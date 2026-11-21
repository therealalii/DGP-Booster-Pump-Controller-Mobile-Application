package com.dgp.boosterpump

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.dgp.boosterpump.ui.DgpApp
import com.dgp.boosterpump.ui.theme.DgpTheme
import com.dgp.boosterpump.viewmodel.GatewaySettingsViewModel
import com.dgp.boosterpump.viewmodel.MonitoringViewModel

class MainActivity : ComponentActivity() {

    private val appContainer by lazy { (application as DgpApplication).container }

    private val monitoringViewModel: MonitoringViewModel by viewModels {
        MonitoringViewModel.factory(appContainer.configRepository, appContainer.session)
    }

    private val settingsViewModel: GatewaySettingsViewModel by viewModels {
        GatewaySettingsViewModel.factory(appContainer.configRepository, appContainer.session)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DgpTheme {
                DgpApp(
                    monitoringViewModel = monitoringViewModel,
                    settingsViewModel = settingsViewModel,
                )
            }
        }
    }
}
