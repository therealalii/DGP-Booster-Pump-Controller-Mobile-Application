package com.dgp.boosterpump.gateway

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dgp.boosterpump.modbus.ReadFunction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.gatewayDataStore: DataStore<Preferences> by preferencesDataStore(name = "gateway_config")

/** Persists [GatewayConfig] locally. No credentials are stored. */
class GatewayConfigRepository(private val context: Context) {

    private object Keys {
        val MODE = stringPreferencesKey("mode")
        val MODEL = stringPreferencesKey("model")
        val HOST = stringPreferencesKey("host")
        val PORT = intPreferencesKey("port")
        val UNIT_ID = intPreferencesKey("unit_id")
        val FRAMING = stringPreferencesKey("framing")
        val CONNECT_TIMEOUT = intPreferencesKey("connect_timeout_ms")
        val RESPONSE_TIMEOUT = intPreferencesKey("response_timeout_ms")
        val POLL_INTERVAL = intPreferencesKey("poll_interval_ms")
        val BIT_FC = intPreferencesKey("bit_function_code")
        val WORD_FC = intPreferencesKey("word_function_code")
        val ADDRESS_OFFSET = intPreferencesKey("address_offset")
    }

    val config: Flow<GatewayConfig> = context.gatewayDataStore.data.map { it.toConfig() }

    suspend fun current(): GatewayConfig = config.first()

    suspend fun save(config: GatewayConfig) {
        context.gatewayDataStore.edit { p ->
            p[Keys.MODE] = config.mode.name
            p[Keys.MODEL] = config.controllerModel.name
            p[Keys.HOST] = config.host.trim()
            p[Keys.PORT] = config.port
            p[Keys.UNIT_ID] = config.unitId
            p[Keys.FRAMING] = config.framing.name
            p[Keys.CONNECT_TIMEOUT] = config.connectTimeoutMs
            p[Keys.RESPONSE_TIMEOUT] = config.responseTimeoutMs
            p[Keys.POLL_INTERVAL] = config.pollIntervalMs
            p[Keys.BIT_FC] = config.mapping.bitReadFunction.code
            p[Keys.WORD_FC] = config.mapping.wordReadFunction.code
            p[Keys.ADDRESS_OFFSET] = config.mapping.addressOffset
        }
    }

    private fun Preferences.toConfig(): GatewayConfig {
        val d = GatewayConfig()
        val bitFc = this[Keys.BIT_FC]?.let(ReadFunction::fromCode)?.takeIf { it.isBitAccess }
        val wordFc = this[Keys.WORD_FC]?.let(ReadFunction::fromCode)?.takeIf { !it.isBitAccess }
        val offset = this[Keys.ADDRESS_OFFSET]?.takeIf { it in ProtocolMapping.ALLOWED_OFFSETS }
        return GatewayConfig(
            mode = enumOr(this[Keys.MODE], d.mode),
            controllerModel = enumOr(this[Keys.MODEL], d.controllerModel),
            host = this[Keys.HOST] ?: d.host,
            port = this[Keys.PORT] ?: d.port,
            unitId = this[Keys.UNIT_ID] ?: d.unitId,
            framing = enumOr(this[Keys.FRAMING], d.framing),
            connectTimeoutMs = this[Keys.CONNECT_TIMEOUT] ?: d.connectTimeoutMs,
            responseTimeoutMs = this[Keys.RESPONSE_TIMEOUT] ?: d.responseTimeoutMs,
            pollIntervalMs = this[Keys.POLL_INTERVAL] ?: d.pollIntervalMs,
            mapping = ProtocolMapping(
                bitReadFunction = bitFc ?: d.mapping.bitReadFunction,
                wordReadFunction = wordFc ?: d.mapping.wordReadFunction,
                addressOffset = offset ?: d.mapping.addressOffset,
            ),
        )
    }

    private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
        name?.let { n -> enumValues<E>().firstOrNull { it.name == n } } ?: default
}
