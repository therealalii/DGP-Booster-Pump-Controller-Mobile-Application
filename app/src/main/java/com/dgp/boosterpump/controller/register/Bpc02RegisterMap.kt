package com.dgp.boosterpump.controller.register

/**
 * DGP BPC02 register map, transcribed from the BPC-02 User Manual (pages 25-28,
 * "ModBus Address" section: Protocol ModBus-RTU, 9600 8-N-1).
 *
 * Every entry records the manual page and row number. Nothing here is invented:
 *  - Units/scales are only set where the manual prints them.
 *  - Drive frequency (Word 10) has a printed range 0-1600 but no unit/scale -> scale = null.
 *  - The Modbus function code for each table is NOT documented and is configurable.
 *  - Addresses are the printed values; any PDU offset is applied by the protocol mapping.
 */
object Bpc02RegisterMap {

    private const val SRC_BIT = "BPC-02 manual p.25-26, Bit Address table"
    private const val SRC_WORD = "BPC-02 manual p.27, Word Address table"
    private const val SRC_CFG_BIT = "BPC-02 manual p.26-27, Bit Address table (hardware settings)"
    private const val SRC_CFG_WORD = "BPC-02 manual p.28 (hardware settings)"
    private const val SRC_MANUAL = "BPC-02 manual p.28, manual-mode note"

    private val ON_OFF = mapOf(0 to "OFF", 1 to "ON")
    private val FAIL = mapOf(0 to "Normal", 1 to "Failed")
    private val CONNECTED = mapOf(0 to "Not connected", 1 to "Connected")
    private val ENABLE = mapOf(0 to "Disabled", 1 to "Enabled")

    /** Number of pumps with status/failure bits in the Bit table (Pump1..Pump6). */
    const val STATUS_PUMP_COUNT = 6

    /** Number of pumps with run-time words in the Word table (Pump1..Pump4). */
    const val RUN_TIME_PUMP_COUNT = 4

    const val OUTPUT_COUNT = 10
    const val PR_INPUT_COUNT = 4
    const val MFI_INPUT_COUNT = 4

    // ------------------------------------------------------------------ keys

    fun outputKey(i: Int) = "output_$i"
    const val DR_STATUS = "dr_status"
    fun prKey(n: Int) = "pr_$n"
    fun mfiKey(n: Int) = "mfi_$n"
    fun pumpStatusKey(n: Int) = "pump_${n}_status"
    const val DRIVE_FAIL = "drive_fail"
    fun pumpFailKey(n: Int) = "pump_${n}_fail"
    const val FLOATER_LOW = "floater_low"
    const val FLOATER_HIGH = "floater_high"

    const val DISPLAYED_PAGE = "displayed_page"
    const val SYSTEM_PRESSURE = "system_pressure"
    const val V_RN = "v_rn"
    const val V_SN = "v_sn"
    const val V_TN = "v_tn"
    const val V_RS = "v_rs"
    const val V_ST = "v_st"
    const val V_TR = "v_tr"
    const val MAINS_FREQUENCY = "mains_frequency"
    const val DRIVE_FREQUENCY = "drive_frequency"
    fun runSecondsKey(n: Int) = "pump_${n}_run_sec"
    fun runMinutesKey(n: Int) = "pump_${n}_run_min"
    fun runHoursKey(n: Int) = "pump_${n}_run_hour"
    const val SYSTEM_ERROR = "system_error"

    // ------------------------------------------------------------------ polled bit table

    private val bitStatus: List<RegisterDefinition> = buildList {
        for (i in 0 until OUTPUT_COUNT) {
            add(bit(outputKey(i), "Output $i status", i, RegisterGroup.DIGITAL_IO, ON_OFF, row = i + 1))
        }
        add(
            bit(
                DR_STATUS, "DR status", 10, RegisterGroup.DIGITAL_IO, ON_OFF, row = 11,
                notes = "Printed as 'DR Status'. The physical signal is not described in the Modbus table.",
            ),
        )
        for (n in 1..PR_INPUT_COUNT) {
            add(
                bit(
                    prKey(n), "PR$n input status", 10 + n, RegisterGroup.DIGITAL_IO, ON_OFF, row = 11 + n,
                    notes = "Manual p.8: PR$n is the direct-contactor feedback input of pump $n.",
                ),
            )
        }
        for (n in 1..MFI_INPUT_COUNT) {
            add(
                bit(
                    mfiKey(n), "MFI$n input status", 14 + n, RegisterGroup.DIGITAL_IO, ON_OFF, row = 15 + n,
                    notes = "Multi-function input; its role is configured on the controller (MFI$n Type).",
                ),
            )
        }
        for (n in 1..STATUS_PUMP_COUNT) {
            add(bit(pumpStatusKey(n), "Pump $n status", 18 + n, RegisterGroup.PUMP_STATUS, ON_OFF, row = 19 + n))
        }
        add(bit(DRIVE_FAIL, "Drive fail", 25, RegisterGroup.ALARM, FAIL, row = 26))
        for (n in 1..STATUS_PUMP_COUNT) {
            add(bit(pumpFailKey(n), "Pump $n fail", 25 + n, RegisterGroup.ALARM, FAIL, row = 26 + n))
        }
        add(bit(FLOATER_LOW, "Floater L level", 32, RegisterGroup.DIGITAL_IO, CONNECTED, row = 33))
        add(bit(FLOATER_HIGH, "Floater H level", 33, RegisterGroup.DIGITAL_IO, CONNECTED, row = 34))
    }

    // ------------------------------------------------------------------ polled word table

    private val wordStatus: List<RegisterDefinition> = buildList {
        add(word(DISPLAYED_PAGE, "Displayed page number", 0, RegisterGroup.DISPLAY, row = 1))
        add(word(SYSTEM_PRESSURE, "System pressure", 2, RegisterGroup.MEASUREMENT, "bar", 0.01, row = 2))
        add(word(V_RN, "Voltage R-N", 3, RegisterGroup.MEASUREMENT, "V", 1.0, row = 3))
        add(word(V_SN, "Voltage S-N", 4, RegisterGroup.MEASUREMENT, "V", 1.0, row = 4))
        add(word(V_TN, "Voltage T-N", 5, RegisterGroup.MEASUREMENT, "V", 1.0, row = 5))
        add(word(V_RS, "Voltage R-S", 6, RegisterGroup.MEASUREMENT, "V", 1.0, row = 6))
        add(word(V_ST, "Voltage S-T", 7, RegisterGroup.MEASUREMENT, "V", 1.0, row = 7))
        add(word(V_TR, "Voltage T-R", 8, RegisterGroup.MEASUREMENT, "V", 1.0, row = 8))
        add(word(MAINS_FREQUENCY, "Mains frequency", 9, RegisterGroup.MEASUREMENT, "Hz", 0.01, row = 9))
        add(
            word(
                DRIVE_FREQUENCY, "Drive frequency", 10, RegisterGroup.MEASUREMENT, row = 10,
                verification = Verification.AMBIGUOUS,
                notes = "Manual prints range 0-1600 without unit or scale. Shown as raw value until confirmed.",
            ),
        )
        for (n in 1..RUN_TIME_PUMP_COUNT) {
            val base = 34 + (n - 1) * 3
            val row = 11 + (n - 1) * 3
            add(word(runSecondsKey(n), "Pump $n work time (seconds part)", base, RegisterGroup.RUN_TIME, "s", 1.0, row = row))
            add(word(runMinutesKey(n), "Pump $n work time (minutes part)", base + 1, RegisterGroup.RUN_TIME, "min", 1.0, row = row + 1))
            add(word(runHoursKey(n), "Pump $n work time (hours part)", base + 2, RegisterGroup.RUN_TIME, "h", 1.0, row = row + 2))
        }
        add(
            RegisterDefinition(
                key = SYSTEM_ERROR,
                label = "System error",
                table = VendorTable.WORD,
                documentedAddress = 80,
                rawType = RawType.BITFIELD16,
                group = RegisterGroup.ALARM,
                source = "$SRC_WORD, row 25",
                notes = "Bits 3-10 documented. Bit numbering assumed LSB = bit 0 (unverified).",
            ),
        )
    }

    // ------------------------------------------------------------------ on-demand configuration (read only)

    private val MFI_TYPES = mapOf(0 to "Disable", 1 to "PS", 2 to "MP", 3 to "EMG", 4 to "CP", 5 to "FL")
    private val MFO_TYPES = mapOf(0 to "Disable", 1 to "E Valve", 2 to "Fan", 3 to "Alarm", 4 to "DC3", 5 to "MC3")
    private const val CFG_WARNING = "Hardware setting. Manual warns that wrong changes can damage the booster hardware. Read-only in this app."
    private const val CFG_TABLE_NOTE = "Listed after the Word table on p.28 but numbered as a continuation of the Bit table; " +
        "the values imply 16-bit words. Table assignment unverified."

    /** Hardware configuration entries. Read only on explicit user request, never polled, never written. */
    val configuration: List<RegisterDefinition> = buildList {
        for (n in 1..6) {
            add(cfgBit("pump_${n}_activation", "Pump $n activation", 4095 + n, ENABLE, row = 44 + n))
        }
        add(cfgBit("control_phase_activation", "Control phase activation", 4106, ENABLE, row = 51))
        add(cfgBit("off_changing_activation", "OFF changing activation", 4107, ENABLE, row = 52))
        add(cfgBit("time_changing_activation", "Time changing activation", 4108, ENABLE, row = 53))
        add(cfgBit("reserve_auto_activation", "Reserve automatic activation", 4109, ENABLE, row = 54))
        add(cfgBit("reserve_auto_type", "Reserve automatic type", 4110, mapOf(0 to "NO", 1 to "NC"), row = 55))

        add(cfgWord("control_status", "Control status", 4173, row = 56, rangeNote = "Documented range 0-1."))
        add(cfgWord("sleep_gap", "Sleep gap", 4175, row = 57, unit = "bar", scale = 0.01, rangeNote = "Documented range 1-100."))
        add(cfgWord("auto_service_off_time", "Auto service off time", 4177, row = 58, unit = "h", scale = 1.0, rangeNote = "Documented range 1-9999."))
        add(
            cfgWord(
                "auto_service_on_time", "Auto service on time", 4178, row = 59, unit = "s", scale = 0.1,
                rangeNote = "Documented range 1-999. Address printed as '417B'; read as 4178 (sequence 4177/4179).",
                verification = Verification.AMBIGUOUS,
            ),
        )
        add(cfgWord("auto_service_interval", "Auto service interval", 4179, row = 60, unit = "min", scale = 1.0, rangeNote = "Documented range 1-99."))
        add(
            cfgEnum(
                "mfi_1_type", "MFI1 type", 4180, MFI_TYPES, row = 61,
                extra = "Address printed as '41B0'; read as 4180 (sequence 4179/4181).",
                verification = Verification.AMBIGUOUS,
            ),
        )
        add(cfgEnum("mfi_2_type", "MFI2 type", 4181, MFI_TYPES, row = 62))
        add(cfgEnum("mfi_3_type", "MFI3 type", 4182, MFI_TYPES, row = 63))
        add(cfgEnum("mfi_4_type", "MFI4 type", 4183, MFI_TYPES, row = 64))
        add(cfgEnum("mfo_1_type", "MFO1 type", 4184, MFO_TYPES, row = 65))
        add(cfgEnum("mfo_2_type", "MFO2 type", 4185, MFO_TYPES, row = 66))
        add(cfgEnum("mfo_3_type", "MFO3 type", 4186, MFO_TYPES, row = 67))
        add(cfgWord("modbus_address", "Modbus address", 4195, row = 68, rangeNote = "Value column shows 1."))
    }

    // ------------------------------------------------------------------ manual control (LOCKED)

    /**
     * Control items described in the manual. They are listed so the UI can explain what is
     * locked and why; the application never reads or writes these addresses.
     */
    val manualControl: List<RegisterDefinition> = buildList {
        add(
            RegisterDefinition(
                key = "multi_function_register",
                label = "Multi Function Register (manual mode)",
                table = VendorTable.WORD,
                documentedAddress = 99,
                rawType = RawType.UINT16,
                group = RegisterGroup.MANUAL_CONTROL,
                access = AccessPolicy.CONTROL_LOCKED,
                verification = Verification.AMBIGUOUS,
                source = SRC_MANUAL,
                notes = "Manual: write 1234 to enter manual mode, 0 to return to automatic. " +
                    "Not listed in the Word table; Modbus table and write function code unconfirmed.",
            ),
        )
        for (i in 0 until OUTPUT_COUNT) {
            add(
                RegisterDefinition(
                    key = "output_${i}_manual",
                    label = "Output $i manual",
                    table = VendorTable.BIT,
                    documentedAddress = 50 + i,
                    rawType = RawType.BIT,
                    group = RegisterGroup.MANUAL_CONTROL,
                    enumLabels = ON_OFF,
                    access = AccessPolicy.CONTROL_LOCKED,
                    source = "$SRC_BIT, row ${35 + i}",
                    notes = "Switches a physical controller output in manual mode. Write function code unconfirmed.",
                ),
            )
        }
        for (i in 0..1) {
            add(
                RegisterDefinition(
                    key = "analog_output_${i}_manual",
                    label = "Analog output $i manual",
                    table = VendorTable.WORD,
                    documentedAddress = 50 + i,
                    rawType = RawType.UINT16,
                    group = RegisterGroup.MANUAL_CONTROL,
                    unit = "Hz",
                    scale = 0.1,
                    access = AccessPolicy.CONTROL_LOCKED,
                    source = "$SRC_WORD, row ${23 + i}",
                    notes = "Documented range 0-500 (0.1 Hz). Sets drive speed in manual mode. Write function code unconfirmed.",
                ),
            )
        }
    }

    // ------------------------------------------------------------------ aggregates

    /** Values read on every poll cycle. */
    val polled: List<RegisterDefinition> = bitStatus + wordStatus

    val all: List<RegisterDefinition> = polled + configuration + manualControl

    private val byKey: Map<String, RegisterDefinition> = all.associateBy { it.key }

    fun byKey(key: String): RegisterDefinition =
        byKey[key] ?: throw IllegalArgumentException("Unknown BPC02 register key '$key'")

    /**
     * Read blocks covering [polled]. Blocks only span documented addresses so that the
     * controller is never asked for an undocumented address (Word 1, 11-33, 46-79 are gaps).
     */
    val pollBlocks: List<ReadBlock> = buildBlocks(polled)

    /** Groups definitions of one table into contiguous runs of documented addresses. */
    fun buildBlocks(definitions: List<RegisterDefinition>): List<ReadBlock> =
        definitions.groupBy { it.table }.flatMap { (table, defs) ->
            val addresses = defs.map { it.documentedAddress }.distinct().sorted()
            val blocks = mutableListOf<ReadBlock>()
            var start = addresses.first()
            var prev = start
            for (a in addresses.drop(1)) {
                if (a != prev + 1) {
                    blocks += ReadBlock(table, start, prev - start + 1)
                    start = a
                }
                prev = a
            }
            blocks += ReadBlock(table, start, prev - start + 1)
            blocks
        }

    // ------------------------------------------------------------------ builders

    private fun bit(
        key: String,
        label: String,
        address: Int,
        group: RegisterGroup,
        labels: Map<Int, String>,
        row: Int,
        notes: String? = null,
    ) = RegisterDefinition(
        key = key, label = label, table = VendorTable.BIT, documentedAddress = address,
        rawType = RawType.BIT, group = group, enumLabels = labels,
        source = "$SRC_BIT, row $row", notes = notes,
    )

    private fun word(
        key: String,
        label: String,
        address: Int,
        group: RegisterGroup,
        unit: String? = null,
        scale: Double? = null,
        row: Int,
        verification: Verification = Verification.DOCUMENTED,
        notes: String? = null,
    ) = RegisterDefinition(
        key = key, label = label, table = VendorTable.WORD, documentedAddress = address,
        rawType = RawType.UINT16, group = group, unit = unit, scale = scale,
        verification = verification, source = "$SRC_WORD, row $row", notes = notes,
    )

    private fun cfgBit(key: String, label: String, address: Int, labels: Map<Int, String>, row: Int) =
        RegisterDefinition(
            key = key, label = label, table = VendorTable.BIT, documentedAddress = address,
            rawType = RawType.BIT, group = RegisterGroup.CONFIGURATION, enumLabels = labels,
            source = "$SRC_CFG_BIT, row $row", notes = CFG_WARNING,
        )

    private fun cfgWord(
        key: String,
        label: String,
        address: Int,
        row: Int,
        unit: String? = null,
        scale: Double? = null,
        rangeNote: String,
        verification: Verification = Verification.AMBIGUOUS,
    ) = RegisterDefinition(
        key = key, label = label, table = VendorTable.WORD, documentedAddress = address,
        rawType = RawType.UINT16, group = RegisterGroup.CONFIGURATION, unit = unit, scale = scale,
        verification = verification, source = "$SRC_CFG_WORD, row $row",
        notes = "$rangeNote $CFG_TABLE_NOTE $CFG_WARNING",
    )

    private fun cfgEnum(
        key: String,
        label: String,
        address: Int,
        labels: Map<Int, String>,
        row: Int,
        extra: String = "",
        verification: Verification = Verification.AMBIGUOUS,
    ) = RegisterDefinition(
        key = key, label = label, table = VendorTable.WORD, documentedAddress = address,
        rawType = RawType.ENUM16, group = RegisterGroup.CONFIGURATION, enumLabels = labels,
        verification = verification, source = "$SRC_CFG_WORD, row $row",
        notes = listOf(extra, CFG_TABLE_NOTE, CFG_WARNING).filter { it.isNotBlank() }.joinToString(" "),
    )
}
