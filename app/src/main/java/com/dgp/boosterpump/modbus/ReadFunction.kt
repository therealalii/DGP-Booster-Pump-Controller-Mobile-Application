package com.dgp.boosterpump.modbus

/**
 * Modbus *read* function codes supported by this application.
 *
 * Write function codes (05, 06, 15, 16) are intentionally NOT defined here. The BPC02
 * documentation does not confirm which write functions, tables or addresses are valid,
 * so the communication layer is physically unable to build a write request. A separate,
 * explicitly reviewed write API must be added once DGP confirms the specification.
 */
enum class ReadFunction(val code: Int, val label: String, val isBitAccess: Boolean) {
    READ_COILS(0x01, "FC01 Read Coils", isBitAccess = true),
    READ_DISCRETE_INPUTS(0x02, "FC02 Read Discrete Inputs", isBitAccess = true),
    READ_HOLDING_REGISTERS(0x03, "FC03 Read Holding Registers", isBitAccess = false),
    READ_INPUT_REGISTERS(0x04, "FC04 Read Input Registers", isBitAccess = false);

    val maxQuantity: Int get() = if (isBitAccess) ModbusPdu.MAX_READ_BITS else ModbusPdu.MAX_READ_REGISTERS

    companion object {
        fun fromCode(code: Int): ReadFunction? = entries.firstOrNull { it.code == code }
    }
}
