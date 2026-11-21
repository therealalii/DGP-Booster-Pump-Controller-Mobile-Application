package com.dgp.boosterpump.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dgp.boosterpump.controller.ControllerModel
import com.dgp.boosterpump.gateway.ConfigValidation
import com.dgp.boosterpump.gateway.DataSourceMode
import com.dgp.boosterpump.gateway.GatewayConfig
import com.dgp.boosterpump.gateway.GatewayConfigRepository
import com.dgp.boosterpump.gateway.ProtocolMapping
import com.dgp.boosterpump.gateway.validate
import com.dgp.boosterpump.modbus.Framing
import com.dgp.boosterpump.modbus.ReadFunction
import com.dgp.boosterpump.session.ControllerSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GatewayFormState(
    val loaded: Boolean = false,
    val mode: DataSourceMode = DataSourceMode.SIMULATION,
    val controllerModel: ControllerModel = ControllerModel.BPC02,
    val host: String = "",
    val portText: String = "502",
    val unitIdText: String = "1",
    val framing: Framing = Framing.MODBUS_TCP,
    val connectTimeoutText: String = "3000",
    val responseTimeoutText: String = "1500",
    val pollIntervalText: String = "2000",
    val bitReadFunction: ReadFunction = ReadFunction.READ_COILS,
    val wordReadFunction: ReadFunction = ReadFunction.READ_HOLDING_REGISTERS,
    val addressOffset: Int = 0,
    val errors: Map<ConfigValidation.Field, String> = emptyMap(),
    val statusMessage: String? = null,
) {
    fun toConfigOrNull(): GatewayConfig? {
        val port = portText.trim().toIntOrNull() ?: return null
        val unitId = unitIdText.trim().toIntOrNull() ?: return null
        val connectMs = connectTimeoutText.trim().toIntOrNull() ?: return null
        val responseMs = responseTimeoutText.trim().toIntOrNull() ?: return null
        val pollMs = pollIntervalText.trim().toIntOrNull() ?: return null
        return GatewayConfig(
            mode = mode,
            controllerModel = controllerModel,
            host = host.trim(),
            port = port,
            unitId = unitId,
            framing = framing,
            connectTimeoutMs = connectMs,
            responseTimeoutMs = responseMs,
            pollIntervalMs = pollMs,
            mapping = ProtocolMapping(
                bitReadFunction = bitReadFunction,
                wordReadFunction = wordReadFunction,
                addressOffset = addressOffset,
            ),
        )
    }
}

class GatewaySettingsViewModel(
    private val configRepository: GatewayConfigRepository,
    private val session: ControllerSession,
) : ViewModel() {

    private val _form = MutableStateFlow(GatewayFormState())
    val form: StateFlow<GatewayFormState> = _form.asStateFlow()

    init {
        viewModelScope.launch {
            val saved = configRepository.config.first()
            _form.value = GatewayFormState(
                loaded = true,
                mode = saved.mode,
                controllerModel = saved.controllerModel,
                host = saved.host,
                portText = saved.port.toString(),
                unitIdText = saved.unitId.toString(),
                framing = saved.framing,
                connectTimeoutText = saved.connectTimeoutMs.toString(),
                responseTimeoutText = saved.responseTimeoutMs.toString(),
                pollIntervalText = saved.pollIntervalMs.toString(),
                bitReadFunction = saved.mapping.bitReadFunction,
                wordReadFunction = saved.mapping.wordReadFunction,
                addressOffset = saved.mapping.addressOffset,
            )
        }
    }

    fun updateMode(mode: DataSourceMode) = _form.update { it.copy(mode = mode, errors = emptyMap(), statusMessage = null) }
    fun updateModel(model: ControllerModel) = _form.update { it.copy(controllerModel = model, errors = emptyMap(), statusMessage = null) }
    fun updateHost(host: String) = _form.update { it.copy(host = host, statusMessage = null) }
    fun updatePort(text: String) = _form.update { it.copy(portText = text, statusMessage = null) }
    fun updateUnitId(text: String) = _form.update { it.copy(unitIdText = text, statusMessage = null) }
    fun updateFraming(framing: Framing) = _form.update { it.copy(framing = framing, statusMessage = null) }
    fun updateConnectTimeout(text: String) = _form.update { it.copy(connectTimeoutText = text, statusMessage = null) }
    fun updateResponseTimeout(text: String) = _form.update { it.copy(responseTimeoutText = text, statusMessage = null) }
    fun updatePollInterval(text: String) = _form.update { it.copy(pollIntervalText = text, statusMessage = null) }
    fun updateBitFunction(fn: ReadFunction) {
        if (fn.isBitAccess) _form.update { it.copy(bitReadFunction = fn, statusMessage = null) }
    }

    fun updateWordFunction(fn: ReadFunction) {
        if (!fn.isBitAccess) _form.update { it.copy(wordReadFunction = fn, statusMessage = null) }
    }

    fun updateAddressOffset(offset: Int) {
        if (offset in ProtocolMapping.ALLOWED_OFFSETS) {
            _form.update { it.copy(addressOffset = offset, statusMessage = null) }
        }
    }

    fun save(andConnect: Boolean, onConnected: () -> Unit = {}) {
        val current = _form.value
        val parseErrors = mutableMapOf<ConfigValidation.Field, String>()
        val port = current.portText.trim().toIntOrNull()
        val unitId = current.unitIdText.trim().toIntOrNull()
        val connectMs = current.connectTimeoutText.trim().toIntOrNull()
        val responseMs = current.responseTimeoutText.trim().toIntOrNull()
        val pollMs = current.pollIntervalText.trim().toIntOrNull()

        if (current.mode == DataSourceMode.LIVE) {
            if (port == null) parseErrors[ConfigValidation.Field.PORT] = "Enter a valid number"
            if (unitId == null) parseErrors[ConfigValidation.Field.UNIT_ID] = "Enter a valid number"
            if (connectMs == null) parseErrors[ConfigValidation.Field.CONNECT_TIMEOUT] = "Enter milliseconds"
            if (responseMs == null) parseErrors[ConfigValidation.Field.RESPONSE_TIMEOUT] = "Enter milliseconds"
        }
        if (pollMs == null) parseErrors[ConfigValidation.Field.POLL_INTERVAL] = "Enter milliseconds"

        val candidate = GatewayConfig(
            mode = current.mode,
            controllerModel = current.controllerModel,
            host = current.host.trim(),
            port = port ?: 502,
            unitId = unitId ?: 1,
            framing = current.framing,
            connectTimeoutMs = connectMs ?: 3000,
            responseTimeoutMs = responseMs ?: 1500,
            pollIntervalMs = pollMs ?: 2000,
            mapping = ProtocolMapping(
                bitReadFunction = current.bitReadFunction,
                wordReadFunction = current.wordReadFunction,
                addressOffset = current.addressOffset,
            ),
        )
        val validation = candidate.validate()
        val merged = validation.errors + parseErrors
        if (merged.isNotEmpty()) {
            _form.update { it.copy(errors = merged, statusMessage = null) }
            return
        }

        viewModelScope.launch {
            configRepository.save(candidate)
            _form.update {
                it.copy(
                    errors = emptyMap(),
                    statusMessage = if (andConnect) "Settings saved. Connecting…" else "Settings saved.",
                )
            }
            if (andConnect) {
                session.connect(candidate)
                onConnected()
            }
        }
    }

    companion object {
        fun factory(
            configRepository: GatewayConfigRepository,
            session: ControllerSession,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                GatewaySettingsViewModel(configRepository, session) as T
        }
    }
}
