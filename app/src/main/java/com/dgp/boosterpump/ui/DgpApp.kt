package com.dgp.boosterpump.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.SettingsEthernet
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dgp.boosterpump.R
import com.dgp.boosterpump.auth.AppUser
import com.dgp.boosterpump.gateway.DataSourceMode
import com.dgp.boosterpump.session.ConnectionState
import com.dgp.boosterpump.ui.components.ConnectionHeaderStrip
import com.dgp.boosterpump.ui.components.DgpBottomNavBar
import com.dgp.boosterpump.ui.components.NavBarEntry
import com.dgp.boosterpump.ui.components.SimulationBanner
import com.dgp.boosterpump.ui.screens.ControlAndConfigScreen
import com.dgp.boosterpump.ui.screens.DashboardScreen
import com.dgp.boosterpump.ui.screens.DiagnosticsScreen
import com.dgp.boosterpump.ui.screens.GatewaySettingsScreen
import com.dgp.boosterpump.ui.screens.LoginScreen
import com.dgp.boosterpump.ui.screens.PumpsAndIoScreen
import com.dgp.boosterpump.ui.screens.SplashLoadingScreen
import com.dgp.boosterpump.ui.theme.LocalScadaPalette
import com.dgp.boosterpump.viewmodel.GatewaySettingsViewModel
import com.dgp.boosterpump.viewmodel.MonitoringViewModel

private enum class AppStage { SPLASH, LOGIN, MAIN }

private enum class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
) {
    DASHBOARD("dashboard", "خانه", Icons.Outlined.Dashboard, Icons.Filled.Dashboard),
    PUMPS("pumps", "پمپ‌ها", Icons.Outlined.WaterDrop, Icons.Filled.WaterDrop),
    CONTROL("control", "تنظیمات", Icons.Outlined.AdminPanelSettings, Icons.Filled.AdminPanelSettings),
    GATEWAY("gateway", "اتصال", Icons.Outlined.SettingsEthernet, Icons.Filled.SettingsEthernet),
    DIAGNOSTICS("diagnostics", "عیب‌یابی", Icons.Outlined.BugReport, Icons.Filled.BugReport),
}

@Composable
fun DgpApp(
    monitoringViewModel: MonitoringViewModel,
    settingsViewModel: GatewaySettingsViewModel,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
) {
    val splashCompleted by monitoringViewModel.splashCompleted.collectAsStateWithLifecycle()
    val currentUser by monitoringViewModel.currentUser.collectAsStateWithLifecycle()

    val stage = when {
        !splashCompleted -> AppStage.SPLASH
        currentUser == null -> AppStage.LOGIN
        else -> AppStage.MAIN
    }

    AnimatedContent(
        targetState = stage,
        transitionSpec = {
            (fadeIn(tween(420)) + scaleIn(initialScale = 0.97f, animationSpec = tween(420))) togetherWith
                fadeOut(tween(260))
        },
        label = "app_stage_transition",
    ) { target ->
        when (target) {
            AppStage.SPLASH -> SplashLoadingScreen(
                onFinished = monitoringViewModel::markSplashCompleted,
            )
            AppStage.LOGIN -> LoginScreen(
                onAuthenticated = monitoringViewModel::login,
            )
            AppStage.MAIN -> currentUser?.let { user ->
                MainScadaShell(
                    user = user,
                    monitoringViewModel = monitoringViewModel,
                    settingsViewModel = settingsViewModel,
                    darkTheme = darkTheme,
                    onToggleTheme = onToggleTheme,
                    onLogout = monitoringViewModel::logout,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScadaShell(
    user: AppUser,
    monitoringViewModel: MonitoringViewModel,
    settingsViewModel: GatewaySettingsViewModel,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onLogout: () -> Unit,
) {
    val p = LocalScadaPalette.current
    val navController = rememberNavController()
    val uiState by monitoringViewModel.uiState.collectAsStateWithLifecycle()
    val formState by settingsViewModel.form.collectAsStateWithLifecycle()
    var showLogoutDialog by remember { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Destination.DASHBOARD.route

    val showSimBanner = uiState.snapshot?.isSimulated == true ||
        (uiState.connectionState as? ConnectionState.Connected)?.isSimulated == true ||
        (uiState.connectionState as? ConnectionState.Connecting)?.isSimulated == true ||
        (uiState.connectionState as? ConnectionState.Error)?.isSimulated == true ||
        (uiState.connectionState is ConnectionState.Disconnected && uiState.savedConfig.mode == DataSourceMode.SIMULATION)

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val navEntries = remember(user.role) {
        buildList {
            add(Destination.DASHBOARD)
            add(Destination.PUMPS)
            add(Destination.CONTROL)
            if (user.role.canManageConnection) {
                add(Destination.GATEWAY)
            }
        }.map { NavBarEntry(it.route, it.label, it.icon, it.selectedIcon) }
    }
    val activeBottomRoute = if (currentRoute == Destination.DIAGNOSTICS.route) {
        Destination.GATEWAY.route
    } else {
        currentRoute
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = p.panel,
            icon = { Icon(Icons.Outlined.VerifiedUser, contentDescription = null, tint = p.gold) },
            title = { Text("خروج از حساب کاربری", style = MaterialTheme.typography.titleMedium, color = p.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = p.panelElevated,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(0.8.dp, p.panelBorder),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                "${user.displayNameFa}  •  ${user.role.titleFa}",
                                style = MaterialTheme.typography.titleSmall,
                                color = p.gold,
                            )
                            Text(
                                user.role.subtitleFa,
                                style = MaterialTheme.typography.bodySmall,
                                color = p.textSecondary,
                            )
                        }
                    }
                    Text(
                        "با خروج از حساب، اتصال فعلی قطع شده و به صفحه ورود بازمی‌گردید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = p.textSecondary,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = p.fault, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text("خروج از حساب")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("انصراف", color = p.textSecondary) }
            },
        )
    }

    Scaffold(
        containerColor = p.canvas,
        topBar = {
            Column {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = p.panel,
                        titleContentColor = p.textPrimary,
                        actionIconContentColor = p.textSecondary,
                    ),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.dgp_emblem_dark),
                                contentDescription = "DGP",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape),
                            )
                            Column(
                                modifier = Modifier.weight(1f, fill = false),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    "کنترلر بوستر پمپ DGP",
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                val badgeAccent = if (user.role.canEditAdvancedSettings) p.gold else p.cyan
                                val badgeBg = if (user.role.canEditAdvancedSettings) p.goldSoft else p.cyanSoft
                                Surface(
                                    color = badgeBg,
                                    shape = RoundedCornerShape(999.dp),
                                    border = BorderStroke(0.7.dp, badgeAccent.copy(alpha = 0.42f)),
                                ) {
                                    Text(
                                        text = user.role.titleFa,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                        color = badgeAccent,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp),
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        Box(
                            modifier = Modifier
                                .padding(end = 2.dp)
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(p.panelElevated)
                                .border(0.8.dp, p.panelBorder, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            IconButton(onClick = onToggleTheme) {
                                Icon(
                                    imageVector = if (darkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                                    contentDescription = if (darkTheme) "تم روشن" else "تم تیره",
                                    tint = p.textSecondary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .padding(start = 6.dp, end = 10.dp)
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(p.goldSoft)
                                .border(0.8.dp, p.gold.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            IconButton(onClick = { showLogoutDialog = true }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Logout,
                                    contentDescription = "خروج از حساب کاربری",
                                    tint = p.gold,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    },
                )
                // Subtle metallic hairline separating header from telemetry strip
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.8.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(p.panelBorder, p.gold.copy(alpha = 0.30f), p.panelBorder),
                            ),
                        ),
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
            DgpBottomNavBar(
                entries = navEntries,
                currentRoute = activeBottomRoute,
                onSelect = ::navigateTo,
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.DASHBOARD.route,
            enterTransition = { fadeIn(tween(220)) },
            exitTransition = { fadeOut(tween(170)) },
            modifier = Modifier
                .fillMaxSize()
                .background(p.canvas)
                .padding(innerPadding),
        ) {
            composable(Destination.DASHBOARD.route) {
                DashboardScreen(
                    uiState = uiState,
                    canResetErrors = user.role.canOperateBasicControls,
                    canOpenGatewaySettings = user.role.canManageConnection,
                    onConnect = monitoringViewModel::connectWithSavedConfig,
                    onResetPumpErrors = monitoringViewModel::resetPumpErrors,
                    onOpenGatewaySettings = {
                        if (user.role.canManageConnection) navigateTo(Destination.GATEWAY.route)
                    },
                    onOpenPumps = { navigateTo(Destination.PUMPS.route) },
                )
            }
            composable(Destination.PUMPS.route) {
                PumpsAndIoScreen(uiState = uiState)
            }
            composable(Destination.CONTROL.route) {
                ControlAndConfigScreen(
                    uiState = uiState,
                    userRole = user.role,
                    onReadHardwareConfig = monitoringViewModel::readHardwareConfiguration,
                    onWriteParameter = monitoringViewModel::writeHardwareParameter,
                    onResetPumpErrors = monitoringViewModel::resetPumpErrors,
                    onDismissWriteNotice = monitoringViewModel::clearLastWriteNotice,
                )
            }
            composable(Destination.GATEWAY.route) {
                if (user.role.canManageConnection) {
                    GatewaySettingsScreen(
                        form = formState,
                        viewModel = settingsViewModel,
                        canEditGateway = user.role.canManageConnection,
                        canOpenDiagnostics = user.role.canAccessDiagnostics,
                        onSavedAndConnected = { navigateTo(Destination.DASHBOARD.route) },
                        onOpenDiagnostics = {
                            if (user.role.canAccessDiagnostics) {
                                navController.navigate(Destination.DIAGNOSTICS.route) {
                                    launchSingleTop = true
                                }
                            }
                        },
                    )
                }
            }
            composable(Destination.DIAGNOSTICS.route) {
                if (user.role.canAccessDiagnostics) {
                    DiagnosticsScreen(
                        uiState = uiState,
                        canRunProbe = user.role.canAccessDiagnostics,
                        onRunProbe = monitoringViewModel::runReadProbe,
                        onClearLog = monitoringViewModel::clearTransactionLog,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}
