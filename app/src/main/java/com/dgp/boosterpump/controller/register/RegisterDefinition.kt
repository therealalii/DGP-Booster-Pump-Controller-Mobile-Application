package com.dgp.boosterpump.controller.register

/**
 * Vendor table in which a value is documented.
 * Hardware tests on DGP BPC02 confirmed:
 *  - BIT table = Coils (FC01 read, FC05 write)
 *  - WORD table = Holding Registers (FC03 read, FC06 write)
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
    /** Address, description and unit are printed in the BPC-02 manual. */
    DOCUMENTED("Documented", "Printed in the BPC-02 manual; awaiting full hardware confirmation."),

    /** Printed in the manual with an ambiguity (e.g. Drive Frequency 0-1600 without unit/scale). */
    AMBIGUOUS("Ambiguous", "Printed in the BPC-02 manual with an unresolved ambiguity; displayed raw."),

    /** Confirmed against a real BPC02 controller. */
    HARDWARE_VERIFIED("Hardware verified", "Confirmed against a real BPC02 controller."),
}

/** Access policy enforced by the application. */
enum class AccessPolicy {
    /** Read-only (live telemetry or protected parameters such as Slave ID 4195 / Control Status 4173). */
    READ,

    /** Hardware-verified writable via FC05 (Bit) or FC06 (Word), subject to explicit confirmation & range check. */
    READ_WRITE_VERIFIED,

    /** Manual-control item whose write behavior was rejected/unverified on hardware; strictly locked. */
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
 * One documented data point. Single source of truth for addresses, units, scale factors,
 * valid write bounds, access policies, and hardware verification status.
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
    val minValue: Int? = null,
    val maxValue: Int? = null,
    val enumLabels: Map<Int, String> = emptyMap(),
    val access: AccessPolicy = AccessPolicy.READ,
    val verification: Verification = Verification.DOCUMENTED,
    val source: String,
    val notes: String? = null,
) {
    val isWritable: Boolean get() = access == AccessPolicy.READ_WRITE_VERIFIED

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
