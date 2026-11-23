package com.dgp.boosterpump.session

import com.dgp.boosterpump.controller.ControllerProfiles
import com.dgp.boosterpump.controller.ControllerReader
import com.dgp.boosterpump.controller.ControllerSnapshot
import com.dgp.boosterpump.controller.RegisterReading
import com.dgp.boosterpump.controller.register.AccessPolicy
import com.dgp.boosterpump.controller.register.Bpc02RegisterMap
import com.dgp.boosterpump.controller.register.RegisterDefinition
import com.dgp.boosterpump.controller.register.VendorTable
import com.dgp.boosterpump.gateway.DataSourceMode
import com.dgp.boosterpump.gateway.GatewayConfig
import com.dgp.boosterpump.gateway.validate
import com.dgp.boosterpump.modbus.ModbusReadClient
import com.dgp.boosterpump.modbus.ModbusTransport
import com.dgp.boosterpump.modbus.ModbusWriteClient
import com.dgp.boosterpump.modbus.ReadFunction
import com.dgp.boosterpump.modbus.TcpModbusTransport
import com.dgp.boosterpump.modbus.TransactionRecord
import com.dgp.boosterpump.simulation.SimulatedBpc02Transport
import com.dgp.boosterpump.simulation.SimulationScenario
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Connection lifecycle state presented to the UI. */
sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data class Connecting(val target: String, val isSimulated: Boolean) : ConnectionState
    data class Connected(
        val target: String,
        val isSimulated: Boolean,
        val lastPollMillis: Long?,
        val pollCount: Int,
        val partialError: String? = null,
    ) : ConnectionState

    data class Error(
        val target: String,
        val isSimulated: Boolean,
        val message: String,
        val retryInSeconds: Int? = null,
    ) : ConnectionState
}

/** Result of a single manual read-only probe. */
data class ProbeResult(
    val function: ReadFunction,
    val pduAddress: Int,
    val quantity: Int,
    val values: List<Int>?,
    val error: String?,
    val timestampMillis: Long,
)

/** Result of a verified hardware configuration write (FC05/FC06) with read-back check. */
data class WriteResult(
    val definition: RegisterDefinition,
    val requestedValue: Int,
    val readBackValue: Int?,
    val success: Boolean,
    val error: String?,
    val timestampMillis: Long,
)

/** Result of an on-demand read of the hardware configuration addresses. */
data class ConfigSnapshotState(
    val loading: Boolean = false,
    val writingKey: String? = null,
    val readings: List<RegisterReading> = emptyList(),
    val errors: List<String> = emptyList(),
    val lastWrite: WriteResult? = null,
    val timestampMillis: Long? = null,
)

/** One timestamped pressure reading kept in memory for the live trend chart. */
data class PressureSample(val timestampMillis: Long, val pressureBar: Double)

/**
 * Coordinates the Modbus transport, polling loop, transaction log, on-demand
 * configuration read, verified FC05/FC06 writes with RS485/EEPROM settle timing & read-back
 * confirmation, Reset Pump Error command (Word 99 = 25), and single-request read probe.
 *
 * Writes are NEVER executed automatically on startup/reconnect and are strictly limited
 * to registers marked [AccessPolicy.READ_WRITE_VERIFIED] within documented bounds.
 */
class ControllerSession(
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val transportFactory: (GatewayConfig, () -> SimulationScenario) -> ModbusTransport = ::defaultTransport,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _snapshot = MutableStateFlow<ControllerSnapshot?>(null)
    val snapshot: StateFlow<ControllerSnapshot?> = _snapshot.asStateFlow()

    private val _pressureHistory = MutableStateFlow<List<PressureSample>>(emptyList())
    val pressureHistory: StateFlow<List<PressureSample>> = _pressureHistory.asStateFlow()

    private val _activeConfig = MutableStateFlow<GatewayConfig?>(null)
    val activeConfig: StateFlow<GatewayConfig?> = _activeConfig.asStateFlow()

    private val _simulationScenario = MutableStateFlow(SimulationScenario.NORMAL_DUTY)
    val simulationScenario: StateFlow<SimulationScenario> = _simulationScenario.asStateFlow()

    private val _transactions = MutableStateFlow<List<TransactionRecord>>(emptyList())
    val transactions: StateFlow<List<TransactionRecord>> = _transactions.asStateFlow()

    private val _configSnapshot = MutableStateFlow(ConfigSnapshotState())
    val configSnapshot: StateFlow<ConfigSnapshotState> = _configSnapshot.asStateFlow()

    private val _probeResult = MutableStateFlow<ProbeResult?>(null)
    val probeResult: StateFlow<ProbeResult?> = _probeResult.asStateFlow()

    private val ioMutex = Mutex()
    private var pollJob: Job? = null
    private var transport: ModbusTransport? = null
    private var readClient: ModbusReadClient? = null
    private var writeClient: ModbusWriteClient? = null
    private var cachedSlowBits: Map<Int, Boolean> = emptyMap()
    private var cachedSlowWords: Map<Int, Int> = emptyMap()

    fun setSimulationScenario(scenario: SimulationScenario) {
        _simulationScenario.value = scenario
    }

    fun clearTransactionLog() {
        _transactions.value = emptyList()
    }

    fun clearLastWriteNotice() {
        _configSnapshot.update { it.copy(lastWrite = null) }
    }

    /** Starts connecting and polling with [config]. Any previous session is stopped first. */
    fun connect(config: GatewayConfig) {
        val validation = config.validate()
        if (!validation.isValid) {
            _connectionState.value = ConnectionState.Error(
                target = describeTarget(config),
                isSimulated = config.mode == DataSourceMode.SIMULATION,
                message = validation.errors.values.first(),
            )
            return
        }

        disconnect()
        _activeConfig.value = config
        _snapshot.value = null
        _pressureHistory.value = emptyList()
        _configSnapshot.value = ConfigSnapshotState()
        _probeResult.value = null
        cachedSlowBits = emptyMap()
        cachedSlowWords = emptyMap()

        val profile = ControllerProfiles.forModel(config.controllerModel)
        val target = describeTarget(config)
        val isSim = config.mode == DataSourceMode.SIMULATION
        val interBlockDelay = if (isSim) 0L else LIVE_INTER_BLOCK_DELAY_MS

        pollJob = scope.launch(ioDispatcher) {
            var pollCount = 0
            var consecutiveLinkErrors = 0

            while (isActive) {
                try {
                    val includeSlow = isSim || pollCount == 0 || (pollCount % LIVE_SLOW_POLL_EVERY_N == 0)
                    val blocksToRead = when {
                        isSim -> profile.pollBlocks
                        includeSlow -> profile.fastPollBlocks + profile.slowPollBlocks
                        else -> profile.fastPollBlocks
                    }

                    // Check/establish transport and read poll blocks atomically under ioMutex so a
                    // concurrent write cannot leave pollJob holding a stale/closed client reference.
                    val readRaw = ioMutex.withLock {
                        val silentReopen = !isSim && pollCount > 0 && consecutiveLinkErrors == 1
                        ensureConnectedLocked(config, target, isSim, silent = silentReopen)
                        val activeReaderClient = readClient
                            ?: throw IllegalStateException("Read client not initialized")
                        val reader = ControllerReader(activeReaderClient, config.mapping)
                        reader.readBlocks(blocksToRead, interBlockDelayMs = interBlockDelay)
                    }

                    if (!isSim && includeSlow) {
                        val slowBitAddrs = profile.slowPollBlocks
                            .filter { it.table == VendorTable.BIT }
                            .flatMap { b -> (b.startAddress..b.endAddress) }
                            .toSet()
                        val slowWordAddrs = profile.slowPollBlocks
                            .filter { it.table == VendorTable.WORD }
                            .flatMap { b -> (b.startAddress..b.endAddress) }
                            .toSet()
                        cachedSlowBits = readRaw.bits.filterKeys { it in slowBitAddrs }
                        cachedSlowWords = readRaw.words.filterKeys { it in slowWordAddrs }
                    }

                    val mergedRaw = if (!isSim && !includeSlow) {
                        readRaw.copy(
                            bits = cachedSlowBits + readRaw.bits,
                            words = cachedSlowWords + readRaw.words,
                        )
                    } else {
                        readRaw
                    }

                    val now = clock()
                    val snap = profile.decode(mergedRaw, config.mode, now)
                    pollCount++
                    consecutiveLinkErrors = 0

                    snap.pressureBar?.let { bar ->
                        _pressureHistory.update { prev ->
                            (prev + PressureSample(now, bar)).takeLast(MAX_HISTORY_POINTS)
                        }
                    }
                    _snapshot.value = snap

                    val partial = mergedRaw.blockErrors.takeIf { it.isNotEmpty() }?.let { errs ->
                        "${errs.size} of ${blocksToRead.size} read block(s) rejected: ${errs.first().message}"
                    }
                    _connectionState.value = ConnectionState.Connected(
                        target = target,
                        isSimulated = isSim,
                        lastPollMillis = now,
                        pollCount = pollCount,
                        partialError = partial,
                    )
                    delay(config.pollIntervalMs.toLong())
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    ioMutex.withLock { closeTransportLocked() }
                    consecutiveLinkErrors++
                    // On a live RS485/Ethernet converter (Samin ES 487), a single transient dropped
                    // frame or socket reset should recover quietly without flashing red for 3 seconds.
                    if (!isSim && pollCount > 0 && consecutiveLinkErrors == 1) {
                        delay(LIVE_SILENT_RECOVER_DELAY_MS)
                        continue
                    }
                    val msg = e.message ?: e::class.java.simpleName
                    val retrySec = 3
                    for (rem in retrySec downTo 1) {
                        _connectionState.value = ConnectionState.Error(target, isSim, msg, retryInSeconds = rem)
                        delay(1000)
                    }
                }
            }
        }
    }

    fun disconnect() {
        pollJob?.cancel()
        pollJob = null
        closeTransportLocked()
        _connectionState.value = ConnectionState.Disconnected
    }

    /**
     * Reads all documented hardware configuration registers once using contiguous blocks
     * derived from Booster Pump Memory Map.xlsx.
     */
    fun readHardwareConfiguration() {
        val cfg = _activeConfig.value ?: return
        val profile = ControllerProfiles.forModel(cfg.controllerModel)
        val interBlockDelay = if (cfg.mode == DataSourceMode.SIMULATION) 0L else LIVE_INTER_BLOCK_DELAY_MS

        scope.launch(ioDispatcher) {
            _configSnapshot.update { it.copy(loading = true, errors = emptyList()) }
            try {
                val raw = ioMutex.withLock {
                    val activeReaderClient = readClient
                        ?: throw IllegalStateException("Not connected")
                    val reader = ControllerReader(activeReaderClient, cfg.mapping)
                    reader.readBlocks(profile.configurationBlocks, interBlockDelayMs = interBlockDelay)
                }
                val readings = profile.configuration.map { def ->
                    val value = when (def.table) {
                        VendorTable.BIT -> raw.bits[def.documentedAddress]?.let { if (it) 1 else 0 }
                        VendorTable.WORD -> raw.words[def.documentedAddress]
                    }
                    RegisterReading(def, value)
                }
                _configSnapshot.update {
                    it.copy(
                        loading = false,
                        readings = readings,
                        errors = raw.blockErrors.map { err -> "${err.block.label}: ${err.message}" },
                        timestampMillis = clock(),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _configSnapshot.update {
                    it.copy(loading = false, errors = listOf(e.message ?: "Read failed"))
                }
            }
        }
    }

    /** Sends official Reset Pump Error command (FC06 Word 99 = 25). */
    fun resetPumpErrors() {
        writeHardwareParameter(
            definition = Bpc02RegisterMap.multiFunctionRegisterDef,
            newValue = Bpc02RegisterMap.CMD_RESET_PUMP_ERROR,
        )
    }

    /**
     * Writes a single official RW register using FC05 (Coil) or FC06 (Word), pausing cleanly
     * for RS485 bus turnaround and BPC02 EEPROM commit before reading back to confirm storage.
     *
     * Guardrails:
     *  - Rejects any register not marked [AccessPolicy.READ_WRITE_VERIFIED].
     *  - Rejects out-of-range values before touching the wire.
     *  - Never queued for later replay if disconnected.
     */
    fun writeHardwareParameter(definition: RegisterDefinition, newValue: Int) {
        if (definition.access != AccessPolicy.READ_WRITE_VERIFIED) {
            _configSnapshot.update {
                it.copy(
                    lastWrite = WriteResult(
                        definition = definition,
                        requestedValue = newValue,
                        readBackValue = null,
                        success = false,
                        error = "Write blocked by safety policy: ${definition.label} is read-only or locked.",
                        timestampMillis = clock(),
                    ),
                )
            }
            return
        }
        val min = definition.minValue ?: 0
        val max = definition.maxValue ?: if (definition.table == VendorTable.BIT) 1 else 0xFFFF
        if (newValue !in min..max) {
            _configSnapshot.update {
                it.copy(
                    lastWrite = WriteResult(
                        definition = definition,
                        requestedValue = newValue,
                        readBackValue = null,
                        success = false,
                        error = "Value $newValue outside allowed range $min..$max.",
                        timestampMillis = clock(),
                    ),
                )
            }
            return
        }

        val cfg = _activeConfig.value
        if (cfg == null || transport?.isOpen != true || writeClient == null || readClient == null) {
            _configSnapshot.update {
                it.copy(
                    lastWrite = WriteResult(
                        definition = definition,
                        requestedValue = newValue,
                        readBackValue = null,
                        success = false,
                        error = "Not connected — write command discarded (writes are never queued).",
                        timestampMillis = clock(),
                    ),
                )
            }
            return
        }

        val isLive = cfg.mode == DataSourceMode.LIVE
        val isPulseCommand = definition.key == Bpc02RegisterMap.MULTI_FUNCTION_REGISTER

        scope.launch(ioDispatcher) {
            _configSnapshot.update { it.copy(writingKey = definition.key) }
            try {
                val pduAddr = cfg.mapping.pduAddress(definition.documentedAddress)
                val outcome: Pair<Int?, String?> = ioMutex.withLock {
                    val activeWrite = writeClient
                        ?: throw IllegalStateException("Not connected")
                    val activeRead = readClient
                        ?: throw IllegalStateException("Not connected")

                    // 1. Let RS485 bus & Samin ES 487 serial buffer settle after prior poll frames.
                    if (isLive) delay(LIVE_PRE_WRITE_QUIET_MS)

                    // 2. Execute FC05 / FC06 single write (echo verified by ModbusWriteClient).
                    when (definition.table) {
                        VendorTable.BIT -> activeWrite.writeSingleCoil(pduAddr, newValue != 0)
                        VendorTable.WORD -> activeWrite.writeSingleRegister(pduAddr, newValue)
                    }

                    // 3. Pause for BPC02 EEPROM commit (~240-300 ms observed on hardware).
                    if (isLive) delay(LIVE_POST_WRITE_SETTLE_MS)

                    // 4. Pulse commands (Word 99 = 25) auto-clear to 0 in firmware; echo confirms acceptance.
                    if (isPulseCommand) {
                        newValue to null
                    } else {
                        // 5. Read back to verify controller stored the value, with 1 retry if bus is still busy.
                        readBackWithRetry(cfg, activeRead, definition.table, pduAddr, isLive)
                    }
                }

                val (readBack, readBackWarn) = outcome
                val matched = isPulseCommand || (readBack == newValue)
                val errorText = when {
                    matched && readBackWarn == null -> null
                    readBack == null && readBackWarn != null ->
                        "Write echo confirmed by controller, but read-back failed: $readBackWarn"
                    !matched -> "Read-back mismatch: wrote $newValue, read back $readBack"
                    else -> null
                }
                val success = matched && readBackWarn == null
                val now = clock()
                val result = WriteResult(
                    definition = definition,
                    requestedValue = newValue,
                    readBackValue = readBack,
                    success = success,
                    error = errorText,
                    timestampMillis = now,
                )

                val effectiveValue = readBack ?: newValue
                // Patch live snapshot BEFORE publishing writingKey = null so observers always see updated live state.
                if (matched && !isPulseCommand) {
                    patchLiveSnapshotAfterWrite(definition, effectiveValue)
                }

                _configSnapshot.update { state ->
                    val updatedReadings = state.readings.map { r ->
                        if (r.definition.key == definition.key) r.copy(raw = effectiveValue) else r
                    }
                    state.copy(
                        writingKey = null,
                        readings = updatedReadings,
                        lastWrite = result,
                        timestampMillis = now,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _configSnapshot.update {
                    it.copy(
                        writingKey = null,
                        lastWrite = WriteResult(
                            definition = definition,
                            requestedValue = newValue,
                            readBackValue = null,
                            success = false,
                            error = e.message ?: e::class.java.simpleName,
                            timestampMillis = clock(),
                        ),
                    )
                }
            }
        }
    }

    private suspend fun readBackWithRetry(
        cfg: GatewayConfig,
        activeRead: ModbusReadClient,
        table: VendorTable,
        pduAddr: Int,
        isLive: Boolean,
    ): Pair<Int?, String?> {
        var lastErr: Exception? = null
        repeat(2) { attempt ->
            if (attempt > 0 && isLive) delay(LIVE_POST_WRITE_SETTLE_MS)
            try {
                val value = when (table) {
                    VendorTable.BIT -> {
                        val bits = activeRead.readBits(cfg.mapping.bitReadFunction, pduAddr, 1)
                        if (bits.firstOrNull() == true) 1 else 0
                    }
                    VendorTable.WORD -> {
                        val regs = activeRead.readRegisters(cfg.mapping.wordReadFunction, pduAddr, 1)
                        regs.first()
                    }
                }
                return value to null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastErr = e
                if (transport?.isOpen != true) {
                    return null to (e.message ?: e::class.java.simpleName)
                }
            }
        }
        return null to (lastErr?.message ?: "Read-back failed")
    }

    private fun patchLiveSnapshotAfterWrite(definition: RegisterDefinition, value: Int) {
        if (definition.table == VendorTable.WORD && definition.documentedAddress in cachedSlowWords) {
            cachedSlowWords = cachedSlowWords + (definition.documentedAddress to value)
        } else if (definition.table == VendorTable.BIT && definition.documentedAddress in cachedSlowBits) {
            cachedSlowBits = cachedSlowBits + (definition.documentedAddress to (value != 0))
        }
        _snapshot.update { snap ->
            if (snap == null) return@update null
            val patchedReadings = snap.readings.map { r ->
                if (r.definition.key == definition.key) r.copy(raw = value) else r
            }
            when {
                definition.key == Bpc02RegisterMap.DRIVE_ACTIVATION ->
                    snap.copy(driveEnabled = value != 0, readings = patchedReadings)
                definition.key == Bpc02RegisterMap.SYSTEM_PRESSURE_SETPOINT ->
                    snap.copy(pressureSetpointBar = value * 0.01, readings = patchedReadings)
                definition.key == Bpc02RegisterMap.CONTROL_STATUS ->
                    snap.copy(controlStatus = value, readings = patchedReadings)
                definition.key.startsWith("pump_") && definition.key.endsWith("_activation") -> {
                    val pumpNum = definition.key.removePrefix("pump_").removeSuffix("_activation").toIntOrNull()
                    if (pumpNum != null) {
                        val updatedPumps = snap.pumps.map { pump ->
                            if (pump.number == pumpNum) pump.copy(enabled = value != 0) else pump
                        }
                        snap.copy(pumps = updatedPumps, readings = patchedReadings)
                    } else {
                        snap
                    }
                }
                else -> snap
            }
        }
    }

    /** Executes a single read-only Modbus request (FC01-FC04) directly against the active link. */
    fun runReadProbe(function: ReadFunction, pduAddress: Int, quantity: Int) {
        if (readClient == null) {
            _probeResult.value = ProbeResult(
                function, pduAddress, quantity, values = null,
                error = "Connect to a gateway (or simulation) before running a probe.",
                timestampMillis = clock(),
            )
            return
        }
        scope.launch(ioDispatcher) {
            try {
                val values = ioMutex.withLock {
                    val activeRead = readClient ?: throw IllegalStateException("Not connected")
                    if (function.isBitAccess) {
                        activeRead.readBits(function, pduAddress, quantity).map { if (it) 1 else 0 }
                    } else {
                        activeRead.readRegisters(function, pduAddress, quantity).toList()
                    }
                }
                _probeResult.value = ProbeResult(function, pduAddress, quantity, values, null, clock())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _probeResult.value = ProbeResult(
                    function, pduAddress, quantity, null,
                    e.message ?: e::class.java.simpleName, clock(),
                )
            }
        }
    }

    private suspend fun ensureConnectedLocked(
        config: GatewayConfig,
        target: String,
        isSim: Boolean,
        silent: Boolean = false,
    ) {
        if (transport?.isOpen == true && readClient != null && writeClient != null) return
        if (!silent) {
            _connectionState.value = ConnectionState.Connecting(target, isSim)
        }
        val t = transportFactory(config) { _simulationScenario.value }
        t.open()
        transport = t
        readClient = ModbusReadClient(
            transport = t,
            unitId = config.unitId,
            clock = clock,
            onTransaction = ::appendTransaction,
        )
        writeClient = ModbusWriteClient(
            transport = t,
            unitId = config.unitId,
            clock = clock,
            onTransaction = ::appendTransaction,
        )
    }

    private fun appendTransaction(record: TransactionRecord) {
        _transactions.update { prev ->
            (listOf(record) + prev).take(MAX_LOG_ENTRIES)
        }
    }

    private fun closeTransportLocked() {
        runCatching { transport?.close() }
        transport = null
        readClient = null
        writeClient = null
    }

    companion object {
        const val MAX_LOG_ENTRIES = 120
        const val MAX_HISTORY_POINTS = 40

        /** Comfortable spacing between consecutive live RS485 requests via Samin ES 487. */
        const val LIVE_INTER_BLOCK_DELAY_MS = 65L

        /** Refresh slower telemetry (work timers, setpoint, control mode, LCD page) every 4th live cycle. */
        const val LIVE_SLOW_POLL_EVERY_N = 4

        /** Pause before silently reopening after a single transient ES 487 glitch. */
        const val LIVE_SILENT_RECOVER_DELAY_MS = 350L

        /** Bus quiet period before transmitting an FC05/FC06 write frame on live RS485. */
        const val LIVE_PRE_WRITE_QUIET_MS = 150L

        /** EEPROM commit settle delay after FC05/FC06 echo before issuing read-back. */
        const val LIVE_POST_WRITE_SETTLE_MS = 350L

        fun describeTarget(config: GatewayConfig): String = when (config.mode) {
            DataSourceMode.SIMULATION -> "Simulated ${config.controllerModel.label}"
            DataSourceMode.LIVE -> "${config.host.trim()}:${config.port} (Unit ${config.unitId})"
        }

        fun defaultTransport(config: GatewayConfig, scenario: () -> SimulationScenario): ModbusTransport =
            when (config.mode) {
                DataSourceMode.SIMULATION -> SimulatedBpc02Transport(config.mapping, scenario)
                DataSourceMode.LIVE -> TcpModbusTransport(
                    host = config.host.trim(),
                    port = config.port,
                    framing = config.framing,
                    connectTimeoutMs = config.connectTimeoutMs,
                    responseTimeoutMs = config.responseTimeoutMs,
                )
            }
    }
}
