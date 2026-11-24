package com.dgp.boosterpump.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dgp.boosterpump.auth.AppUser
import com.dgp.boosterpump.controller.ControllerSnapshot
import com.dgp.boosterpump.gateway.GatewayConfig
import com.dgp.boosterpump.gateway.GatewayConfigRepository
import com.dgp.boosterpump.modbus.ReadFunction
import com.dgp.boosterpump.modbus.TransactionRecord
import com.dgp.boosterpump.session.ConfigSnapshotState
import com.dgp.boosterpump.session.ConnectionState
import com.dgp.boosterpump.session.ControllerSession
import com.dgp.boosterpump.session.PressureSample
import com.dgp.boosterpump.session.ProbeResult
import com.dgp.boosterpump.simulation.SimulationScenario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MonitoringUiState(
    val savedConfig: GatewayConfig = GatewayConfig(),
    val activeConfig: GatewayConfig? = null,
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val snapshot: ControllerSnapshot? = null,
    val pressureHistory: List<PressureSample> = emptyList(),
    val simulationScenario: SimulationScenario = SimulationScenario.NORMAL_DUTY,
    val transactions: List<TransactionRecord> = emptyList(),
    val configSnapshot: ConfigSnapshotState = ConfigSnapshotState(),
    val probeResult: ProbeResult? = null,
)

class MonitoringViewModel(
    private val configRepository: GatewayConfigRepository,
    private val session: ControllerSession,
) : ViewModel() {

    /** Industrial control-room obsidian dark theme is enabled by default; users can toggle daylight mode. */
    private val _darkTheme = MutableStateFlow(true)
    val darkTheme: StateFlow<Boolean> = _darkTheme.asStateFlow()

    /** Plays the animated DGP splash sequence once per cold launch (survives configuration changes). */
    private val _splashCompleted = MutableStateFlow(false)
    val splashCompleted: StateFlow<Boolean> = _splashCompleted.asStateFlow()

    /** Currently signed-in operator/installer/admin user; null shows the dark DGP LoginScreen. */
    private val _currentUser = MutableStateFlow<AppUser?>(null)
    val currentUser: StateFlow<AppUser?> = _currentUser.asStateFlow()

    fun toggleDarkTheme() {
        _darkTheme.value = !_darkTheme.value
    }

    fun markSplashCompleted() {
        _splashCompleted.value = true
    }

    fun login(user: AppUser) {
        _currentUser.value = user
    }

    fun logout() {
        session.disconnect()
        _currentUser.value = null
    }

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
        session.pressureHistory,
        session.transactions,
        session.configSnapshot,
        session.probeResult,
    ) { core, history, tx, cfgSnap, probe ->
        core.copy(
            pressureHistory = history,
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

    fun writeHardwareParameter(
        definition: com.dgp.boosterpump.controller.register.RegisterDefinition,
        newValue: Int,
    ) {
        val role = _currentUser.value?.role ?: return
        if (!role.canWriteRegister(definition)) return
        session.writeHardwareParameter(definition, newValue)
    }

    fun resetPumpErrors() {
        val role = _currentUser.value?.role ?: return
        if (!role.canOperateBasicControls) return
        session.resetPumpErrors()
    }

    fun clearLastWriteNotice() = session.clearLastWriteNotice()

    fun runReadProbe(function: ReadFunction, pduAddress: Int, quantity: Int) {
        val role = _currentUser.value?.role ?: return
        if (!role.canAccessDiagnostics) return
        session.runReadProbe(function, pduAddress, quantity)
    }

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
