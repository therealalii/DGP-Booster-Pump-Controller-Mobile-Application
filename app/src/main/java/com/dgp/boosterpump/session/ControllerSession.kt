package com.dgp.boosterpump.session

import com.dgp.boosterpump.controller.Bpc02Catalog
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
import com.dgp.boosterpump.simulation.SimulatedStationProfile
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

/** Live progress & summary of applying a validated imported JSON configuration batch. */
data class ConfigImportProgress(
    val applying: Boolean = false,
    val completed: Boolean = false,
    val totalCount: Int = 0,
    val processedCount: Int = 0,
    val appliedCount: Int = 0,
    val currentParameterName: String? = null,
    val failedDetails: List<String> = emptyList(),
    val timestampMillis: Long? = null,
) {
    val allSucceeded: Boolean get() = completed && failedDetails.isEmpty() && appliedCount == totalCount
}

/** Result of an on-demand read of the hardware configuration addresses. */
data class ConfigSnapshotState(
    val loading: Boolean = false,
    val writingKey: String? = null,
    val readings: List<RegisterReading> = emptyList(),
    val errors: List<String> = emptyList(),
    val lastWrite: WriteResult? = null,
    val importProgress: ConfigImportProgress? = null,
    val timestampMillis: Long? = null,
)

/** One timestamped pressure reading kept in memory for the live trend chart. */
data class PressureSample(val timestampMillis: Long, val pressureBar: Double)

/**
 * Coordinates the Modbus transport, polling loop, transaction log, on-demand
 * configuration read, verified FC05/FC06 writes with RS485/EEPROM settle timing & read-back
 * confirmation, sequential verified configuration import, Reset Pump Error command (Word 99 = 25),
 * and single-request read probe.
 *
 * Writes are NEVER executed automatically on startup/reconnect and are strictly limited
 * to registers marked [AccessPolicy.READ_WRITE_VERIFIED] within documented bounds.
 */
class ControllerSession(
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val transportFactory: ((GatewayConfig, () -> SimulationScenario) -> ModbusTransport)? = null,
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

    private val _simulationProfile = MutableStateFlow(SimulatedStationProfile())
    val simulationProfile: StateFlow<SimulatedStationProfile> = _simulationProfile.asStateFlow()

    private val _transactions = MutableStateFlow<List<TransactionRecord>>(emptyList())
    val transactions: StateFlow<List<TransactionRecord>> = _transactions.asStateFlow()

    private val _configSnapshot = MutableStateFlow(ConfigSnapshotState())
    val configSnapshot: StateFlow<ConfigSnapshotState> = _configSnapshot.asStateFlow()

    private val _probeResult = MutableStateFlow<ProbeResult?>(null)
    val probeResult: StateFlow<ProbeResult?> = _probeResult.asStateFlow()

    private val ioMutex = Mutex()
    @Volatile
    private var sessionGeneration = 0L
    private var pollJob: Job? = null
    private var transport: ModbusTransport? = null
    private var readClient: ModbusReadClient? = null
    private var writeClient: ModbusWriteClient? = null
    private var cachedSlowBits: Map<Int, Boolean> = emptyMap()
    private var cachedSlowWords: Map<Int, Int> = emptyMap()

    fun setSimulationScenario(scenario: SimulationScenario) {
        _simulationScenario.value = scenario
    }

    fun setSimulationProfile(profile: SimulatedStationProfile) {
        val normalized = profile.normalized()
        _simulationProfile.value = normalized
        // Immediately reflect updated simulated architecture in live snapshot for responsive UI
        val cfg = _activeConfig.value
        if (cfg?.mode == DataSourceMode.SIMULATION) {
            _snapshot.update { snap ->
                snap?.copy(
                    wiringMode = normalized.wiringMode,
                    configuredPumpCount = normalized.pumpCount,
                    variablePumpCount = normalized.driveCount,
                )
            }
            _configSnapshot.update { state ->
                if (state.readings.isEmpty()) {
                    state
                } else {
                    state.copy(
                        readings = state.readings.map { r ->
                            when (r.definition.key) {
                                "wiring_mode" -> r.copy(raw = normalized.wiringMode)
                                "number_of_pump" -> r.copy(raw = normalized.pumpCount)
                                "number_of_variable_pump" -> r.copy(raw = normalized.driveCount)
                                else -> r
                            }
                        },
                    )
                }
            }
        }
    }

    fun clearTransactionLog() {
        _transactions.value = emptyList()
    }

    fun clearLastWriteNotice() {
        _configSnapshot.update { it.copy(lastWrite = null) }
    }

    fun clearImportProgress() {
        _configSnapshot.update { it.copy(importProgress = null) }
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
        val generation = ++sessionGeneration
        _activeConfig.value = config

        val profile = ControllerProfiles.forModel(config.controllerModel)
        val target = describeTarget(config)
        val isSim = config.mode == DataSourceMode.SIMULATION
        val interBlockDelay = if (isSim) 0L else LIVE_INTER_BLOCK_DELAY_MS

        pollJob = scope.launch(ioDispatcher) {
            var pollCount = 0
            var consecutiveLinkErrors = 0

            while (isActive && generation == sessionGeneration) {
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
                        if (generation != sessionGeneration) return@launch
                        val silentReopen = !isSim && pollCount > 0 && consecutiveLinkErrors == 1
                        ensureConnectedLocked(config, target, isSim, silent = silentReopen)
                        val activeReaderClient = readClient
                            ?: throw IllegalStateException("Read client not initialized")
                        val reader = ControllerReader(activeReaderClient, config.mapping)
                        reader.readBlocks(blocksToRead, interBlockDelayMs = interBlockDelay)
                    }

                    if (generation != sessionGeneration) return@launch

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

                    if (generation != sessionGeneration) return@launch
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
                    if (generation != sessionGeneration) return@launch
                    ioMutex.withLock { closeTransportLocked() }
                    consecutiveLinkErrors++
                    if (!isSim && pollCount > 0 && consecutiveLinkErrors == 1) {
                        delay(LIVE_SILENT_RECOVER_DELAY_MS)
                        continue
                    }
                    val msg = e.message ?: e::class.java.simpleName
                    val retrySec = 3
                    for (rem in retrySec downTo 1) {
                        if (generation != sessionGeneration) return@launch
                        _connectionState.value = ConnectionState.Error(target, isSim, msg, retryInSeconds = rem)
                        delay(1000)
                    }
                }
            }
        }
    }

    /** Stops polling, closes the transport, and clears all session state so no stale telemetry ever lingers. */
    fun disconnect() {
        sessionGeneration++
        pollJob?.cancel()
        pollJob = null
        closeTransportLocked()
        cachedSlowBits = emptyMap()
        cachedSlowWords = emptyMap()
        _activeConfig.value = null
        _snapshot.value = null
        _pressureHistory.value = emptyList()
        _configSnapshot.value = ConfigSnapshotState()
        _probeResult.value = null
        _connectionState.value = ConnectionState.Disconnected
    }

    /**
     * Reads all documented hardware configuration registers once using contiguous blocks
     * derived from Booster Pump Memory Map.xlsx.
     */
    fun readHardwareConfiguration() {
        val cfg = _activeConfig.value ?: return
        val generation = sessionGeneration
        val profile = ControllerProfiles.forModel(cfg.controllerModel)
        val interBlockDelay = if (cfg.mode == DataSourceMode.SIMULATION) 0L else LIVE_INTER_BLOCK_DELAY_MS

        scope.launch(ioDispatcher) {
            if (generation != sessionGeneration) return@launch
            _configSnapshot.update { it.copy(loading = true, errors = emptyList()) }
            try {
                val raw = ioMutex.withLock {
                    if (generation != sessionGeneration) return@launch
                    val activeReaderClient = readClient
                        ?: throw IllegalStateException("Not connected")
                    val reader = ControllerReader(activeReaderClient, cfg.mapping)
                    reader.readBlocks(profile.configurationBlocks, interBlockDelayMs = interBlockDelay)
                }
                if (generation != sessionGeneration) return@launch
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
                if (generation != sessionGeneration) return@launch
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
        val generation = sessionGeneration
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
            if (generation != sessionGeneration) return@launch
            _configSnapshot.update { it.copy(writingKey = definition.key) }
            try {
                val pduAddr = cfg.mapping.pduAddress(definition.documentedAddress)
                val outcome: Pair<Int?, String?> = ioMutex.withLock {
                    if (generation != sessionGeneration) return@launch
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

                if (generation != sessionGeneration) return@launch

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
                if (generation != sessionGeneration) return@launch
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
                        val isEnabled = value != 0
                        val updatedPumps = snap.pumps.map { pump ->
                            if (pump.number == pumpNum) {
                                pump.copy(
                                    enabled = isEnabled,
                                    running = if (!isEnabled) false else pump.running,
                                )
                            } else {
                                pump
                            }
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

    /**
     * Applies a user-confirmed batch of imported configuration parameters sequentially using
     * verified single-register FC05/FC06 writes with RS485 quiet timing and read-back verification.
     *
     * Safety guardrails:
     *  - Pre-validates EVERY entry's [AccessPolicy.READ_WRITE_VERIFIED] and numeric/enum bounds
     *    before transmitting a single frame; any violation aborts the entire batch immediately.
     *  - Never queues writes when disconnected.
     */
    fun applyImportedConfiguration(entries: List<Pair<RegisterDefinition, Int>>) {
        if (entries.isEmpty()) return

        // Upfront atomic safety validation — never partially apply an invalid batch!
        val validationErrors = mutableListOf<String>()
        for ((def, value) in entries) {
            val name = Bpc02Catalog.fullParameterName(def)
            if (def.access != AccessPolicy.READ_WRITE_VERIFIED) {
                validationErrors += "$name: read-only or locked register"
                continue
            }
            val min = def.minValue ?: 0
            val max = def.maxValue ?: if (def.table == VendorTable.BIT) 1 else 0xFFFF
            if (def.enumLabels.isNotEmpty() && value !in def.enumLabels.keys) {
                validationErrors += "$name: unsupported option ($value)"
            } else if (value !in min..max) {
                validationErrors += "$name: value $value outside $min..$max"
            }
        }
        if (validationErrors.isNotEmpty()) {
            _configSnapshot.update {
                it.copy(
                    importProgress = ConfigImportProgress(
                        applying = false,
                        completed = true,
                        totalCount = entries.size,
                        processedCount = 0,
                        appliedCount = 0,
                        failedDetails = validationErrors,
                        timestampMillis = clock(),
                    ),
                )
            }
            return
        }

        val cfg = _activeConfig.value
        val generation = sessionGeneration
        if (cfg == null || transport?.isOpen != true || writeClient == null || readClient == null) {
            _configSnapshot.update {
                it.copy(
                    importProgress = ConfigImportProgress(
                        applying = false,
                        completed = true,
                        totalCount = entries.size,
                        processedCount = 0,
                        appliedCount = 0,
                        failedDetails = listOf("Not connected — import aborted (writes are never queued)."),
                        timestampMillis = clock(),
                    ),
                )
            }
            return
        }

        val isLive = cfg.mode == DataSourceMode.LIVE

        scope.launch(ioDispatcher) {
            if (generation != sessionGeneration) return@launch
            var applied = 0
            var processed = 0
            val failures = mutableListOf<String>()

            _configSnapshot.update {
                it.copy(
                    writingKey = entries.first().first.key,
                    importProgress = ConfigImportProgress(
                        applying = true,
                        completed = false,
                        totalCount = entries.size,
                        processedCount = 0,
                        appliedCount = 0,
                        currentParameterName = Bpc02Catalog.fullParameterName(entries.first().first),
                    ),
                )
            }

            try {
                for ((def, targetValue) in entries) {
                    if (generation != sessionGeneration) return@launch
                    val displayName = Bpc02Catalog.fullParameterName(def)
                    _configSnapshot.update {
                        it.copy(
                            writingKey = def.key,
                            importProgress = ConfigImportProgress(
                                applying = true,
                                completed = false,
                                totalCount = entries.size,
                                processedCount = processed,
                                appliedCount = applied,
                                currentParameterName = displayName,
                                failedDetails = failures.toList(),
                            ),
                        )
                    }

                    val pduAddr = cfg.mapping.pduAddress(def.documentedAddress)
                    try {
                        val outcome = ioMutex.withLock {
                            if (generation != sessionGeneration) return@launch
                            val activeWrite = writeClient ?: throw IllegalStateException("Not connected")
                            val activeRead = readClient ?: throw IllegalStateException("Not connected")
                            if (isLive) delay(LIVE_PRE_WRITE_QUIET_MS)
                            when (def.table) {
                                VendorTable.BIT -> activeWrite.writeSingleCoil(pduAddr, targetValue != 0)
                                VendorTable.WORD -> activeWrite.writeSingleRegister(pduAddr, targetValue)
                            }
                            if (isLive) delay(LIVE_POST_WRITE_SETTLE_MS)
                            readBackWithRetry(cfg, activeRead, def.table, pduAddr, isLive)
                        }
                        if (generation != sessionGeneration) return@launch
                        val (readBack, warn) = outcome
                        if (readBack == targetValue && warn == null) {
                            applied++
                            patchLiveSnapshotAfterWrite(def, readBack)
                            _configSnapshot.update { state ->
                                state.copy(
                                    readings = state.readings.map { r ->
                                        if (r.definition.key == def.key) r.copy(raw = readBack) else r
                                    },
                                )
                            }
                        } else {
                            failures += "$displayName: ${warn ?: "read-back mismatch ($readBack ≠ $targetValue)"}"
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        failures += "$displayName: ${e.message ?: e::class.java.simpleName}"
                        if (transport?.isOpen != true) {
                            processed++
                            break
                        }
                    }
                    processed++
                }
            } finally {
                if (generation == sessionGeneration) {
                    val now = clock()
                    _configSnapshot.update { state ->
                        state.copy(
                            writingKey = null,
                            timestampMillis = now,
                            importProgress = ConfigImportProgress(
                                applying = false,
                                completed = true,
                                totalCount = entries.size,
                                processedCount = processed,
                                appliedCount = applied,
                                currentParameterName = null,
                                failedDetails = failures.toList(),
                                timestampMillis = now,
                            ),
                        )
                    }
                }
            }
        }
    }

    /** Executes a single read-only Modbus request (FC01-FC04) directly against the active link. */
    fun runReadProbe(function: ReadFunction, pduAddress: Int, quantity: Int) {
        val generation = sessionGeneration
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
                    if (generation != sessionGeneration) return@launch
                    val activeRead = readClient ?: throw IllegalStateException("Not connected")
                    if (function.isBitAccess) {
                        activeRead.readBits(function, pduAddress, quantity).map { if (it) 1 else 0 }
                    } else {
                        activeRead.readRegisters(function, pduAddress, quantity).toList()
                    }
                }
                if (generation != sessionGeneration) return@launch
                _probeResult.value = ProbeResult(function, pduAddress, quantity, values, null, clock())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (generation != sessionGeneration) return@launch
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
        val t = transportFactory?.invoke(config) { _simulationScenario.value }
            ?: defaultTransport(config, { _simulationScenario.value }, { _simulationProfile.value })
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

        fun defaultTransport(
            config: GatewayConfig,
            scenario: () -> SimulationScenario,
            profile: () -> SimulatedStationProfile = { SimulatedStationProfile() },
        ): ModbusTransport =
            when (config.mode) {
                DataSourceMode.SIMULATION -> SimulatedBpc02Transport(config.mapping, scenario, profile)
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
