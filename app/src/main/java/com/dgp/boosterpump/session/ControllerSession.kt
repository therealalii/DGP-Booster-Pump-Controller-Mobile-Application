package com.dgp.boosterpump.session

import com.dgp.boosterpump.controller.ControllerProfiles
import com.dgp.boosterpump.controller.ControllerReader
import com.dgp.boosterpump.controller.ControllerSnapshot
import com.dgp.boosterpump.controller.RegisterReading
import com.dgp.boosterpump.controller.register.AccessPolicy
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

/** Result of a verified hardware configuration write (FC05/FC06) with immediate read-back check. */
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
 * Coordinates the Modbus transport, read-only polling loop, transaction log, on-demand
 * configuration read, verified FC05/FC06 configuration writes with immediate read-back
 * confirmation, and single-request read probe.
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

    fun setSimulationScenario(scenario: SimulationScenario) {
        _simulationScenario.value = scenario
    }

    fun clearTransactionLog() {
        _transactions.value = emptyList()
    }

    fun clearLastWriteNotice() {
        _configSnapshot.update { it.copy(lastWrite = null) }
    }

    /** Starts connecting and read-only polling with [config]. Any previous session is stopped first. */
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

        val profile = ControllerProfiles.forModel(config.controllerModel)
        val target = describeTarget(config)
        val isSim = config.mode == DataSourceMode.SIMULATION

        pollJob = scope.launch(ioDispatcher) {
            var pollCount = 0
            while (isActive) {
                try {
                    ensureConnected(config, target, isSim)
                    val activeReaderClient = readClient ?: continue
                    val reader = ControllerReader(activeReaderClient, config.mapping)

                    val raw = ioMutex.withLock { reader.readBlocks(profile.pollBlocks) }
                    val now = clock()
                    val snap = profile.decode(raw, config.mode, now)
                    pollCount++
                    snap.pressureBar?.let { bar ->
                        _pressureHistory.update { prev ->
                            (prev + PressureSample(now, bar)).takeLast(MAX_HISTORY_POINTS)
                        }
                    }
                    _snapshot.value = snap

                    val partial = raw.blockErrors.takeIf { it.isNotEmpty() }?.let { errs ->
                        "${errs.size} of ${profile.pollBlocks.size} read block(s) rejected: ${errs.first().message}"
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
                    closeTransport()
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
        closeTransport()
        _connectionState.value = ConnectionState.Disconnected
    }

    /**
     * Reads all documented hardware configuration registers once using the hardware-verified
     * contiguous blocks (4096..4101, 4106..4110 via FC01; 4173, 4175, 4177..4186, 4195 via FC03).
     */
    fun readHardwareConfiguration() {
        val cfg = _activeConfig.value ?: return
        val activeReaderClient = readClient ?: return
        val profile = ControllerProfiles.forModel(cfg.controllerModel)

        scope.launch(ioDispatcher) {
            _configSnapshot.update { it.copy(loading = true, errors = emptyList()) }
            try {
                val reader = ControllerReader(activeReaderClient, cfg.mapping)
                val raw = ioMutex.withLock { reader.readBlocks(profile.configurationBlocks) }
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

    /**
     * Writes a single hardware-verified configuration register using FC05 (Coil) or FC06 (Word),
     * then immediately reads it back via FC01/FC03 to verify the controller stored the new value.
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
                        error = "Write blocked by safety policy: ${definition.label} is not verified writable.",
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
        val activeWrite = writeClient
        val activeRead = readClient
        if (cfg == null || activeWrite == null || activeRead == null) {
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

        scope.launch(ioDispatcher) {
            _configSnapshot.update { it.copy(writingKey = definition.key) }
            try {
                val pduAddr = cfg.mapping.pduAddress(definition.documentedAddress)
                val readBack: Int = ioMutex.withLock {
                    when (definition.table) {
                        VendorTable.BIT -> {
                            activeWrite.writeSingleCoil(pduAddr, newValue != 0)
                            val bits = activeRead.readBits(cfg.mapping.bitReadFunction, pduAddr, 1)
                            if (bits.firstOrNull() == true) 1 else 0
                        }
                        VendorTable.WORD -> {
                            activeWrite.writeSingleRegister(pduAddr, newValue)
                            val regs = activeRead.readRegisters(cfg.mapping.wordReadFunction, pduAddr, 1)
                            regs.first()
                        }
                    }
                }
                val matched = (readBack == newValue)
                val result = WriteResult(
                    definition = definition,
                    requestedValue = newValue,
                    readBackValue = readBack,
                    success = matched,
                    error = if (matched) null else "Read-back mismatch: wrote $newValue, read back $readBack",
                    timestampMillis = clock(),
                )
                _configSnapshot.update { state ->
                    val updatedReadings = state.readings.map { r ->
                        if (r.definition.key == definition.key) r.copy(raw = readBack) else r
                    }
                    state.copy(
                        writingKey = null,
                        readings = updatedReadings,
                        lastWrite = result,
                        timestampMillis = clock(),
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

    /** Executes a single read-only Modbus request (FC01-FC04) directly against the active link. */
    fun runReadProbe(function: ReadFunction, pduAddress: Int, quantity: Int) {
        val activeRead = readClient
        if (activeRead == null) {
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

    private suspend fun ensureConnected(config: GatewayConfig, target: String, isSim: Boolean) {
        if (transport?.isOpen == true && readClient != null && writeClient != null) return
        _connectionState.value = ConnectionState.Connecting(target, isSim)
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

    private fun closeTransport() {
        runCatching { transport?.close() }
        transport = null
        readClient = null
        writeClient = null
    }

    companion object {
        const val MAX_LOG_ENTRIES = 120
        const val MAX_HISTORY_POINTS = 40

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
