package com.dgp.boosterpump.controller.register

/**
 * Vendor table in which a value is documented. The BPC02 manual publishes a "Bit Address"
 * table and a "Word Address" table, but does NOT state which Modbus table (coils vs discrete
 * inputs, holding vs input registers) backs each. The actual read function is chosen in
 * [com.dgp.boosterpump.gateway.ProtocolMapping] and must be confirmed on hardware.
 */
enum class VendorTable(val label: String) {
    BIT("Bit Address table"),
    WORD("Word Address table"),
}

/** How the raw value is interpreted. */
enum class RawType(val label: String) {
    BIT("Bit (0/1)"),

    /** 16-bit value. Signedness is not documented by DGP; treated as unsigned. */
    UINT16("UINT16 (signedness undocumented)"),

    /** 16-bit bit field. */
    BITFIELD16("16-bit bit field"),

    /** 16-bit value with documented enumeration. */
    ENUM16("16-bit enumeration"),
}

/** Evidence level of a register definition. */
enum class Verification(val label: String, val description: String) {
    /** Address, description and unit are printed in the BPC-02 manual. Modbus function code is not. */
    DOCUMENTED("Documented", "Printed in the BPC-02 manual; Modbus table/function not yet confirmed on hardware."),

    /** Printed in the manual but with an ambiguity (unclear scale, glyph, or table). */
    AMBIGUOUS("Ambiguous", "Printed in the BPC-02 manual with an unresolved ambiguity; treat value with caution."),

    /** Confirmed against a real controller (raw value matched the controller LCD). */
    HARDWARE_VERIFIED("Hardware verified", "Confirmed against a real BPC02 controller."),
}

/** Access policy enforced by the application (not by the controller). */
enum class AccessPolicy {
    /** Read by the app. */
    READ,

    /** Documented control item. Writes are LOCKED until verified and approved; the app never reads or writes it. */
    CONTROL_LOCKED,
}

enum class RegisterGroup(val label: String) {
    DISPLAY("Display"),
    MEASUREMENT("Measurements"),
    DIGITAL_IO("Digital I/O"),
    PUMP_STATUS("Pump status"),
    ALARM("Alarms"),
    RUN_TIME("Pump run times"),
    CONFIGURATION("Hardware configuration"),
    MANUAL_CONTROL("Manual control"),
}

/**
 * One documented data point. This is the single place where addresses, units and scale
 * factors live; no other code should contain BPC register addresses.
 *
 * @property documentedAddress the address exactly as printed in the vendor table.
 * @property scale multiply the raw value by this to obtain [unit]; null when undocumented.
 * @property enumLabels documented meaning of raw values, if any.
 * @property source page/row reference into the BPC-02 manual.
 */
data class RegisterDefinition(
    val key: String,
    val label: String,
    val table: VendorTable,
    val documentedAddress: Int,
    val rawType: RawType,
    val group: RegisterGroup,
    val unit: String? = null,
    val scale: Double? = null,
    val enumLabels: Map<Int, String> = emptyMap(),
    val access: AccessPolicy = AccessPolicy.READ,
    val verification: Verification = Verification.DOCUMENTED,
    val source: String,
    val notes: String? = null,
) {
    /** Formats a raw value for display using only documented scaling. */
    fun format(raw: Int): String = when (rawType) {
        RawType.BIT -> enumLabels[raw] ?: raw.toString()
        RawType.BITFIELD16 -> "0x%04X".format(raw)
        RawType.ENUM16 -> enumLabels[raw]?.let { "$it ($raw)" } ?: "Undocumented value $raw"
        RawType.UINT16 -> if (scale != null) {
            val decimals = when {
                scale >= 1.0 -> 0
                scale >= 0.1 -> 1
                else -> 2
            }
            "%.${decimals}f".format(raw * scale) + (unit?.let { " $it" } ?: "")
        } else {
            "raw $raw" + (unit?.let { " ($it, scale unverified)" } ?: "")
        }
    }
}

/** A contiguous run of documented addresses read with a single Modbus request. */
data class ReadBlock(
    val table: VendorTable,
    val startAddress: Int,
    val count: Int,
) {
    val endAddress: Int get() = startAddress + count - 1
    val label: String get() = "${table.name} $startAddress..$endAddress"
}
