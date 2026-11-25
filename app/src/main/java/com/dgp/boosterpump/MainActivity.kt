package com.dgp.boosterpump

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dgp.boosterpump.ui.DgpApp
import com.dgp.boosterpump.ui.theme.DgpTheme
import com.dgp.boosterpump.viewmodel.GatewaySettingsViewModel
import com.dgp.boosterpump.viewmodel.MonitoringViewModel

class MainActivity : ComponentActivity() {

    private val appContainer by lazy { (application as DgpApplication).container }

    private val monitoringViewModel: MonitoringViewModel by viewModels {
        MonitoringViewModel.factory(
            configRepository = appContainer.configRepository,
            session = appContainer.session,
            permissionsStore = appContainer.permissionsRepository,
            authSession = appContainer.authSession,
        )
    }

    private val settingsViewModel: GatewaySettingsViewModel by viewModels {
        GatewaySettingsViewModel.factory(
            configRepository = appContainer.configRepository,
            session = appContainer.session,
            permissionsStore = appContainer.permissionsRepository,
            authSession = appContainer.authSession,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            val darkTheme by monitoringViewModel.darkTheme.collectAsStateWithLifecycle()
            val splashCompleted by monitoringViewModel.splashCompleted.collectAsStateWithLifecycle()
            val currentUser by monitoringViewModel.currentUser.collectAsStateWithLifecycle()
            val useDarkBars = !splashCompleted || currentUser == null || darkTheme

            LaunchedEffect(useDarkBars) {
                if (useDarkBars) {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
                        navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
                    )
                } else {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                        navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                    )
                }
            }

            DgpTheme(darkTheme = darkTheme) {
                DgpApp(
                    monitoringViewModel = monitoringViewModel,
                    settingsViewModel = settingsViewModel,
                    darkTheme = darkTheme,
                    onToggleTheme = monitoringViewModel::toggleDarkTheme,
                )
            }
        }
    }
}
