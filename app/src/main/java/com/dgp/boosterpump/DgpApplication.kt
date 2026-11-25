package com.dgp.boosterpump

import android.app.Application
import android.content.Context
import com.dgp.boosterpump.auth.AuthSessionStore
import com.dgp.boosterpump.auth.DataStoreOperatorPermissionsRepository
import com.dgp.boosterpump.auth.OperatorPermissionsStore
import com.dgp.boosterpump.gateway.GatewayConfigRepository
import com.dgp.boosterpump.session.ControllerSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Simple manual dependency container scoped to the Application so the active TCP session
 * survives screen rotation and navigation changes.
 */
class AppContainer(context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val configRepository = GatewayConfigRepository(context.applicationContext)
    val permissionsRepository: OperatorPermissionsStore =
        DataStoreOperatorPermissionsRepository(context.applicationContext, appScope)
    val authSession = AuthSessionStore()
    val session = ControllerSession(scope = appScope)
}

class DgpApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
