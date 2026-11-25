package com.dgp.boosterpump.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dgp.boosterpump.auth.AppUser
import com.dgp.boosterpump.auth.AuthSessionStore
import com.dgp.boosterpump.auth.InMemoryOperatorPermissionsStore
import com.dgp.boosterpump.auth.OperatorPermission
import com.dgp.boosterpump.auth.OperatorPermissions
import com.dgp.boosterpump.auth.OperatorPermissionsStore
import com.dgp.boosterpump.auth.UserRole
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
import com.dgp.boosterpump.simulation.SimulatedStationProfile
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
    val simulationProfile: SimulatedStationProfile = SimulatedStationProfile(),
    val transactions: List<TransactionRecord> = emptyList(),
    val configSnapshot: ConfigSnapshotState = ConfigSnapshotState(),
    val probeResult: ProbeResult? = null,
)

class MonitoringViewModel(
    private val configRepository: GatewayConfigRepository,
    private val session: ControllerSession,
    private val permissionsStore: OperatorPermissionsStore = InMemoryOperatorPermissionsStore(),
    private val authSession: AuthSessionStore = AuthSessionStore(),
) : ViewModel() {

    /** Industrial control-room obsidian dark theme is enabled by default; users can toggle daylight mode. */
    private val _darkTheme = MutableStateFlow(true)
    val darkTheme: StateFlow<Boolean> = _darkTheme.asStateFlow()

    /** Plays the animated DGP splash sequence once per cold launch (survives configuration changes). */
    private val _splashCompleted = MutableStateFlow(false)
    val splashCompleted: StateFlow<Boolean> = _splashCompleted.asStateFlow()

    /** Currently signed-in administrator or operator; null shows the dark DGP LoginScreen. */
    val currentUser: StateFlow<AppUser?> = authSession.currentUser

    /** Live admin-configured permissions applied to [UserRole.OPERATOR] (default: read-only). */
    val operatorPermissions: StateFlow<OperatorPermissions> = permissionsStore.permissions

    fun toggleDarkTheme() {
        _darkTheme.value = !_darkTheme.value
    }

    fun markSplashCompleted() {
        _splashCompleted.value = true
    }

    fun login(user: AppUser) {
        authSession.login(user)
    }

    fun logout() {
        session.disconnect()
        authSession.logout()
    }

    /**
     * Resolves the currently signed-in user's effective permissions.
     * Returns null when signed out; [UserRole.ADMIN] always receives [OperatorPermissions.FULL_ADMIN].
     */
    private fun currentEffectivePermissions(): OperatorPermissions? {
        val role = authSession.currentUser.value?.role ?: return null
        return role.effectivePermissions(permissionsStore.permissions.value)
    }

    /** Admin-only: toggle one individual [OperatorPermission] for the OPERATOR account. */
    fun setOperatorPermission(permission: OperatorPermission, enabled: Boolean) {
        val role = authSession.currentUser.value?.role ?: return
        if (!role.canManageOperatorPermissions) return
        viewModelScope.launch {
            permissionsStore.setPermission(permission, enabled)
        }
    }

    /** Admin-only: apply a full [OperatorPermissions] configuration (e.g. preset). */
    fun applyOperatorPermissions(permissions: OperatorPermissions) {
        val role = authSession.currentUser.value?.role ?: return
        if (!role.canManageOperatorPermissions) return
        viewModelScope.launch {
            permissionsStore.save(permissions)
        }
    }

    /** Admin-only: restore the default strict read-only policy for OPERATOR accounts. */
    fun resetOperatorPermissionsToReadOnly() {
        val role = authSession.currentUser.value?.role ?: return
        if (!role.canManageOperatorPermissions) return
        viewModelScope.launch {
            permissionsStore.resetToDefault()
        }
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

    private val telemetryFlow = combine(
        coreFlow,
        session.simulationProfile,
        session.pressureHistory,
        session.transactions,
        session.configSnapshot,
    ) { core, simProfile, history, tx, cfgSnap ->
        core.copy(
            simulationProfile = simProfile,
            pressureHistory = history,
            transactions = tx,
            configSnapshot = cfgSnap,
        )
    }

    val uiState: StateFlow<MonitoringUiState> = combine(
        telemetryFlow,
        session.probeResult,
    ) { state, probe ->
        state.copy(probeResult = probe)
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

    fun setSimulationScenario(scenario: SimulationScenario) {
        val effective = currentEffectivePermissions() ?: return
        if (!effective.canManageGateway) return
        session.setSimulationScenario(scenario)
    }

    fun setSimulationProfile(profile: SimulatedStationProfile) {
        val effective = currentEffectivePermissions() ?: return
        if (!effective.canManageGateway) return
        session.setSimulationProfile(profile)
    }

    fun readHardwareConfiguration() = session.readHardwareConfiguration()

    fun writeHardwareParameter(
        definition: com.dgp.boosterpump.controller.register.RegisterDefinition,
        newValue: Int,
    ) {
        val role = authSession.currentUser.value?.role ?: return
        if (!role.canWriteRegister(definition, permissionsStore.permissions.value)) return
        session.writeHardwareParameter(definition, newValue)
    }

    fun applyImportedConfiguration(
        entries: List<Pair<com.dgp.boosterpump.controller.register.RegisterDefinition, Int>>,
    ) {
        val role = authSession.currentUser.value?.role ?: return
        val opPerms = permissionsStore.permissions.value
        val effective = role.effectivePermissions(opPerms)
        if (!effective.canImportConfig) return
        if (entries.isEmpty()) return
        // Enforce role & operator write permission across EVERY parameter before touching Modbus
        if (entries.any { (def, _) -> !role.canWriteRegister(def, opPerms) }) return
        session.applyImportedConfiguration(entries)
    }

    fun clearImportProgress() = session.clearImportProgress()

    fun resetPumpErrors() {
        val effective = currentEffectivePermissions() ?: return
        if (!effective.canResetPumpErrors) return
        session.resetPumpErrors()
    }

    fun clearLastWriteNotice() = session.clearLastWriteNotice()

    fun runReadProbe(function: ReadFunction, pduAddress: Int, quantity: Int) {
        val effective = currentEffectivePermissions() ?: return
        if (!effective.canAccessDiagnostics) return
        session.runReadProbe(function, pduAddress, quantity)
    }

    fun clearTransactionLog() {
        val effective = currentEffectivePermissions() ?: return
        if (!effective.canAccessDiagnostics) return
        session.clearTransactionLog()
    }

    companion object {
        fun factory(
            configRepository: GatewayConfigRepository,
            session: ControllerSession,
            permissionsStore: OperatorPermissionsStore,
            authSession: AuthSessionStore,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                MonitoringViewModel(configRepository, session, permissionsStore, authSession) as T
        }
    }
}
