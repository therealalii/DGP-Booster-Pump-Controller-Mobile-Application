package com.dgp.boosterpump.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.SettingsEthernet
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dgp.boosterpump.gateway.DataSourceMode
import com.dgp.boosterpump.session.ConnectionState
import com.dgp.boosterpump.ui.components.ConnectionHeaderStrip
import com.dgp.boosterpump.ui.components.SimulationBanner
import com.dgp.boosterpump.ui.screens.ControllerDetailsScreen
import com.dgp.boosterpump.ui.screens.DashboardScreen
import com.dgp.boosterpump.ui.screens.DiagnosticsScreen
import com.dgp.boosterpump.ui.screens.GatewaySettingsScreen
import com.dgp.boosterpump.ui.screens.PumpsAndIoScreen
import com.dgp.boosterpump.viewmodel.GatewaySettingsViewModel
import com.dgp.boosterpump.viewmodel.MonitoringViewModel

private enum class Destination(val route: String, val label: String, val icon: ImageVector) {
    DASHBOARD("dashboard", "Dashboard", Icons.Outlined.Dashboard),
    PUMPS("pumps", "Pumps & I/O", Icons.Outlined.Tune),
    DETAILS("details", "Details", Icons.Outlined.Info),
    DIAGNOSTICS("diagnostics", "Diag", Icons.Outlined.BugReport),
    GATEWAY("gateway", "Gateway", Icons.Outlined.SettingsEthernet),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DgpApp(
    monitoringViewModel: MonitoringViewModel,
    settingsViewModel: GatewaySettingsViewModel,
) {
    val navController = rememberNavController()
    val uiState by monitoringViewModel.uiState.collectAsStateWithLifecycle()
    val formState by settingsViewModel.form.collectAsStateWithLifecycle()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Destination.DASHBOARD.route

    val showSimBanner = uiState.snapshot?.isSimulated == true ||
        (uiState.connectionState as? ConnectionState.Connected)?.isSimulated == true ||
        (uiState.connectionState as? ConnectionState.Connecting)?.isSimulated == true ||
        (uiState.connectionState as? ConnectionState.Error)?.isSimulated == true ||
        (uiState.connectionState is ConnectionState.Disconnected && uiState.savedConfig.mode == DataSourceMode.SIMULATION)

    fun navigateTo(dest: Destination) {
        navController.navigate(dest.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("DGP Booster Pump Monitor") },
                )
                if (showSimBanner) {
                    SimulationBanner(
                        scenario = uiState.simulationScenario,
                        onScenarioSelected = monitoringViewModel::setSimulationScenario,
                    )
                }
                ConnectionHeaderStrip(
                    state = uiState.connectionState,
                    onConnectClick = monitoringViewModel::connectWithSavedConfig,
                    onDisconnectClick = monitoringViewModel::disconnect,
                )
            }
        },
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { dest ->
                    NavigationBarItem(
                        selected = currentRoute == dest.route,
                        onClick = { navigateTo(dest) },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label, maxLines = 1) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.DASHBOARD.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            composable(Destination.DASHBOARD.route) {
                DashboardScreen(
                    uiState = uiState,
                    onConnect = monitoringViewModel::connectWithSavedConfig,
                    onOpenGatewaySettings = { navigateTo(Destination.GATEWAY) },
                    onOpenPumps = { navigateTo(Destination.PUMPS) },
                )
            }
            composable(Destination.PUMPS.route) {
                PumpsAndIoScreen(uiState = uiState)
            }
            composable(Destination.DETAILS.route) {
                ControllerDetailsScreen(
                    uiState = uiState,
                    onReadHardwareConfig = monitoringViewModel::readHardwareConfiguration,
                )
            }
            composable(Destination.DIAGNOSTICS.route) {
                DiagnosticsScreen(
                    uiState = uiState,
                    onRunProbe = monitoringViewModel::runReadProbe,
                    onClearLog = monitoringViewModel::clearTransactionLog,
                )
            }
            composable(Destination.GATEWAY.route) {
                GatewaySettingsScreen(
                    form = formState,
                    viewModel = settingsViewModel,
                    onSavedAndConnected = { navigateTo(Destination.DASHBOARD) },
                )
            }
        }
    }
}
