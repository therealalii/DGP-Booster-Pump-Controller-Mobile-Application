package com.dgp.boosterpump.gateway

import com.dgp.boosterpump.controller.ControllerModel
import com.dgp.boosterpump.controller.register.VendorTable
import com.dgp.boosterpump.modbus.Framing
import com.dgp.boosterpump.modbus.ReadFunction

/** Where data comes from. Simulation data must never be presented as live data. */
enum class DataSourceMode(val label: String) {
    LIVE("Live gateway"),
    SIMULATION("Simulation (no hardware)"),
}

/**
 * How vendor tables map onto Modbus functions and PDU addresses.
 *
 * The BPC02 manual does not document which function codes back its "Bit" and "Word" tables
 * or whether printed addresses need an offset. These defaults are the first hypotheses of
 * the read-only test plan (DGP_BPC02_Modbus_Research_Findings.txt §8) and MUST be confirmed
 * by comparing readings with the controller LCD.
 */
data class ProtocolMapping(
    val bitReadFunction: ReadFunction = ReadFunction.READ_COILS,
    val wordReadFunction: ReadFunction = ReadFunction.READ_HOLDING_REGISTERS,
    /** Added to every documented address to obtain the PDU address. 0 = use printed address directly. */
    val addressOffset: Int = 0,
) {
    init {
        require(bitReadFunction.isBitAccess) { "Bit table must use FC01 or FC02" }
        require(!wordReadFunction.isBitAccess) { "Word table must use FC03 or FC04" }
        require(addressOffset in ALLOWED_OFFSETS) { "Address offset must be one of $ALLOWED_OFFSETS" }
    }

    fun functionFor(table: VendorTable): ReadFunction = when (table) {
        VendorTable.BIT -> bitReadFunction
        VendorTable.WORD -> wordReadFunction
    }

    fun pduAddress(documentedAddress: Int): Int = documentedAddress + addressOffset

    companion object {
        val ALLOWED_OFFSETS = listOf(-1, 0, 1)
    }
}

/**
 * Complete connection configuration. Nothing network-specific is hard-coded: host must be
 * entered by the user; port and Unit ID are pre-filled only with documented values that the
 * user is told to confirm (Modbus/TCP registered port 502, BPC02 manual default address 1).
 */
data class GatewayConfig(
    val mode: DataSourceMode = DataSourceMode.SIMULATION,
    val controllerModel: ControllerModel = ControllerModel.BPC02,
    val host: String = "",
    val port: Int = 502,
    val unitId: Int = 1,
    val framing: Framing = Framing.MODBUS_TCP,
    val connectTimeoutMs: Int = 3000,
    val responseTimeoutMs: Int = 1500,
    val pollIntervalMs: Int = 2000,
    val mapping: ProtocolMapping = ProtocolMapping(),
) {
    companion object {
        val PORT_RANGE = 1..65535
        val UNIT_ID_RANGE = 1..247
        val CONNECT_TIMEOUT_RANGE = 500..30_000
        val RESPONSE_TIMEOUT_RANGE = 200..10_000
        val POLL_INTERVAL_RANGE = 500..60_000
    }
}

/** Field-level validation errors for [GatewayConfig]. */
data class ConfigValidation(val errors: Map<Field, String>) {
    enum class Field { HOST, PORT, UNIT_ID, CONNECT_TIMEOUT, RESPONSE_TIMEOUT, POLL_INTERVAL, MODEL }

    val isValid: Boolean get() = errors.isEmpty()
}

private val HOSTNAME_REGEX = Regex("^[A-Za-z0-9]([A-Za-z0-9-]{0,61}[A-Za-z0-9])?(\\.[A-Za-z0-9]([A-Za-z0-9-]{0,61}[A-Za-z0-9])?)*$")
private val IPV4_REGEX = Regex("^\\d{1,3}(\\.\\d{1,3}){3}$")

fun isValidHost(host: String): Boolean {
    val h = host.trim()
    if (h.isEmpty() || h.length > 253) return false
    if (IPV4_REGEX.matches(h)) return h.split('.').all { it.toInt() in 0..255 }
    if (h.all { it.isDigit() || it == '.' }) return false // malformed dotted number
    return HOSTNAME_REGEX.matches(h)
}

fun GatewayConfig.validate(): ConfigValidation {
    val errors = mutableMapOf<ConfigValidation.Field, String>()
    if (!controllerModel.isSupported) {
        errors[ConfigValidation.Field.MODEL] = "${controllerModel.label} register map is not available yet"
    }
    if (mode == DataSourceMode.LIVE) {
        if (!isValidHost(host)) errors[ConfigValidation.Field.HOST] = "Enter the gateway IPv4 address or host name"
        if (port !in GatewayConfig.PORT_RANGE) errors[ConfigValidation.Field.PORT] = "Port must be 1-65535"
        if (unitId !in GatewayConfig.UNIT_ID_RANGE) errors[ConfigValidation.Field.UNIT_ID] = "Unit ID must be 1-247"
        if (connectTimeoutMs !in GatewayConfig.CONNECT_TIMEOUT_RANGE) {
            errors[ConfigValidation.Field.CONNECT_TIMEOUT] = "Connect timeout must be 500-30000 ms"
        }
        if (responseTimeoutMs !in GatewayConfig.RESPONSE_TIMEOUT_RANGE) {
            errors[ConfigValidation.Field.RESPONSE_TIMEOUT] = "Response timeout must be 200-10000 ms"
        }
    }
    if (pollIntervalMs !in GatewayConfig.POLL_INTERVAL_RANGE) {
        errors[ConfigValidation.Field.POLL_INTERVAL] = "Poll interval must be 500-60000 ms"
    }
    return ConfigValidation(errors)
}
