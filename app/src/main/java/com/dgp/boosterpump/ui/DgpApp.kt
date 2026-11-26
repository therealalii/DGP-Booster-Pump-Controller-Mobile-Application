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
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.DeveloperBoard
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.SettingsEthernet
import androidx.compose.material.icons.outlined.VerifiedUser
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
import com.dgp.boosterpump.auth.UserRole
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
import com.dgp.boosterpump.ui.screens.OperatorPermissionsScreen
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
    PUMPS("pumps", "تجهیزات", Icons.Outlined.DeveloperBoard, Icons.Filled.DeveloperBoard),
    CONTROL("control", "تنظیمات", Icons.Outlined.AdminPanelSettings, Icons.Filled.AdminPanelSettings),
    GATEWAY("gateway", "اتصال", Icons.Outlined.SettingsEthernet, Icons.Filled.SettingsEthernet),
    DIAGNOSTICS("diagnostics", "عیب‌یابی", Icons.Outlined.BugReport, Icons.Filled.BugReport),
    OPERATOR_PERMISSIONS("operator_permissions", "دسترسی های اپراتور", Icons.Outlined.ManageAccounts, Icons.Filled.ManageAccounts),
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
    val operatorPermissions by monitoringViewModel.operatorPermissions.collectAsStateWithLifecycle()
    val formState by settingsViewModel.form.collectAsStateWithLifecycle()
    var showLogoutDialog by remember { mutableStateOf(false) }

    val effectivePermissions = remember(user.role, operatorPermissions) {
        user.role.effectivePermissions(operatorPermissions)
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Destination.DASHBOARD.route

    val showSimBanner = uiState.snapshot?.isSimulated == true ||
        (uiState.connectionState as? ConnectionState.Connected)?.isSimulated == true ||
        (uiState.connectionState as? ConnectionState.Connecting)?.isSimulated == true ||
        (uiState.connectionState as? ConnectionState.Error)?.isSimulated == true ||
        (uiState.connectionState is ConnectionState.Disconnected && uiState.savedConfig.mode == DataSourceMode.SIMULATION)

    val navEntries = remember(effectivePermissions) {
        buildList {
            add(Destination.DASHBOARD)
            if (effectivePermissions.canViewPumpsAndIo) {
                add(Destination.PUMPS)
            }
            add(Destination.CONTROL)
            if (effectivePermissions.canManageGateway) {
                add(Destination.GATEWAY)
            } else if (effectivePermissions.canAccessDiagnostics) {
                add(Destination.DIAGNOSTICS)
            }
        }.map { NavBarEntry(it.route, it.label, it.icon, it.selectedIcon) }
    }
    val activeBottomRoute = when (currentRoute) {
        Destination.DIAGNOSTICS.route -> if (effectivePermissions.canManageGateway) {
            Destination.GATEWAY.route
        } else {
            Destination.DIAGNOSTICS.route
        }
        Destination.OPERATOR_PERMISSIONS.route -> Destination.GATEWAY.route
        else -> currentRoute
    }

    fun navigateTo(route: String) {
        // 1. Tapping the active parent tab while inside one of its sub-screens
        //    (e.g. tapping «اتصال» while on «دسترسی های اپراتور» or «عیب‌یابی»)
        //    pops directly back to that tab's root screen.
        if (currentRoute != route && activeBottomRoute == route) {
            if (navController.popBackStack(route, inclusive = false)) {
                return
            }
        }
        // 2. Already on the exact root destination -> nothing to do.
        if (currentRoute == route) return

        // 3. Leaving a transient sub-screen for a different tab -> pop the sub-screen first so
        //    saveState stores the parent tab root rather than trapping the user in the sub-screen.
        val isTransientSubScreen = currentRoute == Destination.OPERATOR_PERMISSIONS.route ||
            (currentRoute == Destination.DIAGNOSTICS.route && effectivePermissions.canManageGateway)
        if (isTransientSubScreen) {
            navController.popBackStack()
        }

        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun openOperatorPermissions() {
        if (user.role.canManageOperatorPermissions) {
            navController.navigate(Destination.OPERATOR_PERMISSIONS.route) {
                launchSingleTop = true
            }
        }
    }

    if (showLogoutDialog) {
        val roleDetailText = when {
            user.role == UserRole.ADMIN -> user.role.subtitleFa
            effectivePermissions.grantedWriteCount == 0 ->
                "دسترسی فعلی: فقط خواندنی (مشاهده مانیتورینگ و وضعیت بدون امکان تغییر)"
            else ->
                "دسترسی فعلی: اپراتور با ${effectivePermissions.grantedWriteCount} مجوز تغییر فعال‌شده توسط مدیر"
        }
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
                                roleDetailText,
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
                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp, lineHeight = 18.sp),
                                    maxLines = 2,
                                )
                                val badgeAccent = if (user.role == UserRole.ADMIN) p.gold else p.cyan
                                val badgeBg = if (user.role == UserRole.ADMIN) p.goldSoft else p.cyanSoft
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
                        profile = uiState.simulationProfile,
                        canConfigureDemo = effectivePermissions.canManageGateway,
                        onScenarioSelected = monitoringViewModel::setSimulationScenario,
                        onProfileChanged = monitoringViewModel::setSimulationProfile,
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
                    canResetErrors = effectivePermissions.canResetPumpErrors,
                    canOpenGatewaySettings = effectivePermissions.canManageGateway,
                    canOpenPumps = effectivePermissions.canViewPumpsAndIo,
                    onConnect = monitoringViewModel::connectWithSavedConfig,
                    onResetPumpErrors = monitoringViewModel::resetPumpErrors,
                    onOpenGatewaySettings = {
                        if (effectivePermissions.canManageGateway) navigateTo(Destination.GATEWAY.route)
                    },
                    onOpenPumps = {
                        if (effectivePermissions.canViewPumpsAndIo) navigateTo(Destination.PUMPS.route)
                    },
                )
            }
            composable(Destination.PUMPS.route) {
                if (effectivePermissions.canViewPumpsAndIo) {
                    PumpsAndIoScreen(uiState = uiState)
                }
            }
            composable(Destination.CONTROL.route) {
                ControlAndConfigScreen(
                    uiState = uiState,
                    userRole = user.role,
                    operatorPermissions = operatorPermissions,
                    onReadHardwareConfig = monitoringViewModel::readHardwareConfiguration,
                    onWriteParameter = monitoringViewModel::writeHardwareParameter,
                    onApplyImportedConfig = monitoringViewModel::applyImportedConfiguration,
                    onDismissImportProgress = monitoringViewModel::clearImportProgress,
                    onResetPumpErrors = monitoringViewModel::resetPumpErrors,
                    onDismissWriteNotice = monitoringViewModel::clearLastWriteNotice,
                )
            }
            composable(Destination.GATEWAY.route) {
                if (effectivePermissions.canManageGateway) {
                    GatewaySettingsScreen(
                        form = formState,
                        viewModel = settingsViewModel,
                        canEditGateway = effectivePermissions.canManageGateway,
                        canOpenDiagnostics = effectivePermissions.canAccessDiagnostics,
                        canManageOperatorPermissions = user.role.canManageOperatorPermissions,
                        operatorPermissions = operatorPermissions,
                        onSavedAndConnected = { navigateTo(Destination.DASHBOARD.route) },
                        onOpenDiagnostics = {
                            if (effectivePermissions.canAccessDiagnostics) {
                                navController.navigate(Destination.DIAGNOSTICS.route) {
                                    launchSingleTop = true
                                }
                            }
                        },
                        onOpenOperatorPermissions = ::openOperatorPermissions,
                    )
                }
            }
            composable(Destination.DIAGNOSTICS.route) {
                if (effectivePermissions.canAccessDiagnostics) {
                    DiagnosticsScreen(
                        uiState = uiState,
                        canRunProbe = effectivePermissions.canAccessDiagnostics,
                        isAdmin = user.role == UserRole.ADMIN,
                        onRunProbe = monitoringViewModel::runReadProbe,
                        onClearLog = monitoringViewModel::clearTransactionLog,
                        onBack = if (effectivePermissions.canManageGateway) {
                            { navController.popBackStack() }
                        } else {
                            null
                        },
                    )
                }
            }
            composable(Destination.OPERATOR_PERMISSIONS.route) {
                if (user.role.canManageOperatorPermissions) {
                    OperatorPermissionsScreen(
                        permissions = operatorPermissions,
                        onTogglePermission = monitoringViewModel::setOperatorPermission,
                        onApplyPreset = monitoringViewModel::applyOperatorPermissions,
                        onResetToReadOnly = monitoringViewModel::resetOperatorPermissionsToReadOnly,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}
