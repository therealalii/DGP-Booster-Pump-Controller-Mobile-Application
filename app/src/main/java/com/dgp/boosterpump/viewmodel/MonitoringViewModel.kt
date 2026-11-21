package com.dgp.boosterpump.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dgp.boosterpump.controller.ControllerSnapshot
import com.dgp.boosterpump.gateway.GatewayConfig
import com.dgp.boosterpump.gateway.GatewayConfigRepository
import com.dgp.boosterpump.modbus.ReadFunction
import com.dgp.boosterpump.modbus.TransactionRecord
import com.dgp.boosterpump.session.ConfigSnapshotState
import com.dgp.boosterpump.session.ConnectionState
import com.dgp.boosterpump.session.ControllerSession
import com.dgp.boosterpump.session.ProbeResult
import com.dgp.boosterpump.simulation.SimulationScenario
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MonitoringUiState(
    val savedConfig: GatewayConfig = GatewayConfig(),
    val activeConfig: GatewayConfig? = null,
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val snapshot: ControllerSnapshot? = null,
    val simulationScenario: SimulationScenario = SimulationScenario.NORMAL_DUTY,
    val transactions: List<TransactionRecord> = emptyList(),
    val configSnapshot: ConfigSnapshotState = ConfigSnapshotState(),
    val probeResult: ProbeResult? = null,
)

class MonitoringViewModel(
    private val configRepository: GatewayConfigRepository,
    private val session: ControllerSession,
) : ViewModel() {

    private val coreFlow = combine(
        configRepository.config,
        session.activeConfig,
        session.connectionState,
        session.snapshot,
        session.simulationScenario,
    ) { saved, active, conn, snap, scenario ->
        MonitoringUiState(
            savedConfig = saved,
            activeConfig = active,
            connectionState = conn,
            snapshot = snap,
            simulationScenario = scenario,
        )
    }

    val uiState: StateFlow<MonitoringUiState> = combine(
        coreFlow,
        session.transactions,
        session.configSnapshot,
        session.probeResult,
    ) { core, tx, cfgSnap, probe ->
        core.copy(
            transactions = tx,
            configSnapshot = cfgSnap,
            probeResult = probe,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MonitoringUiState(),
    )

    fun connectWithSavedConfig() {
        viewModelScope.launch {
            session.connect(configRepository.current())
        }
    }

    fun disconnect() = session.disconnect()

    fun setSimulationScenario(scenario: SimulationScenario) = session.setSimulationScenario(scenario)

    fun readHardwareConfiguration() = session.readHardwareConfiguration()

    fun runReadProbe(function: ReadFunction, pduAddress: Int, quantity: Int) =
        session.runReadProbe(function, pduAddress, quantity)

    fun clearTransactionLog() = session.clearTransactionLog()

    companion object {
        fun factory(
            configRepository: GatewayConfigRepository,
            session: ControllerSession,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                MonitoringViewModel(configRepository, session) as T
        }
    }
}
