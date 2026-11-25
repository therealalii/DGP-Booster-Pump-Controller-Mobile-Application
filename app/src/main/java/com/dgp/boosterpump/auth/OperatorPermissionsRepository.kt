package com.dgp.boosterpump.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

private val Context.operatorPermissionsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "operator_permissions",
)

/**
 * Persistence contract for admin-managed [OperatorPermissions].
 *
 * Always starts with [OperatorPermissions.READ_ONLY_DEFAULT] so operators have strictly read-only
 * access until an [UserRole.ADMIN] explicitly grants individual permissions.
 */
interface OperatorPermissionsStore {
    val permissions: StateFlow<OperatorPermissions>

    suspend fun setPermission(permission: OperatorPermission, enabled: Boolean)

    suspend fun save(permissions: OperatorPermissions)

    suspend fun resetToDefault()
}

/**
 * Android DataStore-backed implementation persisting every individual [OperatorPermission]
 * locally across sessions and app restarts.
 */
class DataStoreOperatorPermissionsRepository(
    private val context: Context,
    scope: CoroutineScope,
) : OperatorPermissionsStore {

    override val permissions: StateFlow<OperatorPermissions> =
        context.operatorPermissionsDataStore.data
            .map { prefs -> prefs.toOperatorPermissions() }
            .stateIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                initialValue = OperatorPermissions.READ_ONLY_DEFAULT,
            )

    override suspend fun setPermission(permission: OperatorPermission, enabled: Boolean) {
        context.operatorPermissionsDataStore.edit { prefs ->
            prefs[booleanPreferencesKey(permission.storageKey)] = enabled
        }
    }

    override suspend fun save(permissions: OperatorPermissions) {
        context.operatorPermissionsDataStore.edit { prefs ->
            OperatorPermission.entries.forEach { perm ->
                prefs[booleanPreferencesKey(perm.storageKey)] = perm in permissions.granted
            }
        }
    }

    override suspend fun resetToDefault() {
        save(OperatorPermissions.READ_ONLY_DEFAULT)
    }

    private fun Preferences.toOperatorPermissions(): OperatorPermissions {
        val granted = buildSet {
            OperatorPermission.entries.forEach { perm ->
                val enabled = this@toOperatorPermissions[booleanPreferencesKey(perm.storageKey)]
                    ?: perm.defaultGranted
                if (enabled) add(perm)
            }
        }
        return OperatorPermissions(granted)
    }
}

/** Lightweight in-memory store used in unit tests and previews. */
class InMemoryOperatorPermissionsStore(
    initial: OperatorPermissions = OperatorPermissions.READ_ONLY_DEFAULT,
) : OperatorPermissionsStore {

    private val _permissions = MutableStateFlow(initial)
    override val permissions: StateFlow<OperatorPermissions> = _permissions.asStateFlow()

    override suspend fun setPermission(permission: OperatorPermission, enabled: Boolean) {
        _permissions.update { it.withPermission(permission, enabled) }
    }

    override suspend fun save(permissions: OperatorPermissions) {
        _permissions.value = permissions
    }

    override suspend fun resetToDefault() {
        _permissions.value = OperatorPermissions.READ_ONLY_DEFAULT
    }
}
