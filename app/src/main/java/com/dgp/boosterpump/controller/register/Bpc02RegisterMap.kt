package com.dgp.boosterpump.controller.register

/**
 * Complete official DGP BPC02 Modbus RTU register map (9600 8-N-1), transcribed 1:1 from
 * DGP's official "Booster Pump Memory Map.xlsx" (56 Bit addresses + 98 Word addresses = 154
 * registers) and cross-verified against physical bench tests (BPC02_FULL_READ_RESULTS.txt &
 * BPC02_WRITE_TEST_RESULTS.txt).
 *
 * Confirmed BPC02 Modbus function codes:
 *  - Supported: FC01 (Read Coils), FC03 (Read Holding Registers),
 *               FC05 (Write Single Coil), FC06 (Write Single Holding Register).
 *  - Unsupported (4-byte valid-CRC rejection): FC02, FC04, FC15 (0x0F), FC16 (0x10).
 *  - Zero-based PDU addressing (offset = 0) confirmed across all blocks.
 *  - Coil 4096 is Mode 2,3 Drive Activation (RW); Coils 4097..4102 are Pump 1..6 Activation (RW).
 *  - Coils 4106..4110 and Words 4180..4187 are strictly R (Read-Only) in firmware.
 *  - Manual outputs (Coils 50..59, Words 50..51) are R (Read-Only) in firmware (Exception 0x02).
 *  - Word 99 (Multi Function Register, RW) supports command 25 = Reset Pump Error.
 */
object Bpc02RegisterMap {

    private const val SRC_BIT = "Booster Pump Memory Map.xlsx, Bit Address table (FC01/FC05)"
    private const val SRC_WORD = "Booster Pump Memory Map.xlsx, Word Address table (FC03/FC06)"

    private val ON_OFF = mapOf(0 to "OFF", 1 to "ON")
    private val FAIL = mapOf(0 to "Normal", 1 to "Failed")
    private val CONNECTED = mapOf(0 to "Not connected", 1 to "Connected")
    private val ENABLE = mapOf(0 to "Disabled", 1 to "Enabled")
    private val RESERVE_TYPE = mapOf(0 to "NC", 1 to "NO")
    private val CONTROL_PHASE_TYPE = mapOf(1 to "1P", 3 to "3P")
    private val FLOATER_TYPE = mapOf(0 to "Disable", 1 to "Internal", 2 to "External")
    private val SENSOR_OUTPUT_TYPE = mapOf(
        2 to "4~20mA",
        3 to "0~20mA",
        4 to "0~5V",
        5 to "0~10V",
        6 to "2~10V",
    )
    private val JOCKEY_TYPE = mapOf(0 to "Disable", 1 to "Jockey On", 2 to "Jockey Off")
    private val SENSOR_INPUT_TYPE = mapOf(
        0 to "0-6 bar",
        1 to "0-10 bar",
        2 to "0-16 bar",
        3 to "0-25 bar",
        4 to "0-40 bar",
        5 to "0-60 bar",
        10 to "PS",
    )
    private val CONTROL_STATUS_ENUM = mapOf(0 to "Disable", 1 to "PID", 2 to "ON/OFF")
    private val MFI_TYPES = mapOf(
        0 to "DISABLE",
        1 to "PS",
        2 to "TS",
        3 to "MP",
        4 to "EMG",
        5 to "CP",
        6 to "FL",
        7 to "PR5",
        8 to "PR6",
        9 to "RESET",
        10 to "FIX",
    )
    private val MFO_TYPES = mapOf(
        0 to "DISABLE",
        1 to "MC3",
        2 to "DC4",
        3 to "MC4",
        4 to "FAN",
        5 to "ALARM",
        6 to "E_VALVE",
        7 to "RUN",
        8 to "READY",
        9 to "FILLER",
        10 to "P1_ERR",
        11 to "P2_ERR",
        12 to "P3_ERR",
        13 to "P4_ERR",
    )
    private val CONTROL_DIRECTION_ENUM = mapOf(0 to "Down", 1 to "Up")
    private val AO_TYPE_ENUM = mapOf(0 to "Pump1", 1 to "Pump2")
    private val MULTI_FUNCTION_ENUM = mapOf(0 to "No Function", 25 to "Reset Pump Error")

    private const val RW_NOTE = "Official RW parameter (FC05/FC06). Confirm safe operating conditions before writing."
    private const val RO_NOTE = "Read-only (R) in official DGP BPC02 firmware memory map."

    /** Number of pumps with status/failure/activation bits (Pump 1..Pump 6). */
    const val STATUS_PUMP_COUNT = 6

    /** Number of pumps with run-time words in the Word table (Pump 1..Pump 4). */
    const val RUN_TIME_PUMP_COUNT = 4

    const val OUTPUT_COUNT = 10
    const val PR_INPUT_COUNT = 4
    const val MFI_INPUT_COUNT = 4
    const val MFO_OUTPUT_COUNT = 4

    /** Command value written to Word 99 to reset active pump errors. */
    const val CMD_RESET_PUMP_ERROR = 25

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

    const val DRIVE_ACTIVATION = "drive_activation"
    fun pumpActivationKey(n: Int) = "pump_${n}_activation"

    const val DISPLAYED_PAGE = "displayed_page"
    const val SYSTEM_PRESSURE = "system_pressure"
    const val V_RN = "v_rn"
    const val V_SN = "v_sn"
    const val V_TN = "v_tn"
    const val V_RS = "v_rs"
    const val V_ST = "v_st"
    const val V_TR = "v_tr"
    const val MAINS_FREQUENCY = "mains_frequency"
    const val DRIVE1_FREQUENCY = "drive1_frequency"
    const val DRIVE2_FREQUENCY = "drive2_frequency"
    fun runSecondsKey(n: Int) = "pump_${n}_run_sec"
    fun runMinutesKey(n: Int) = "pump_${n}_run_min"
    fun runHoursKey(n: Int) = "pump_${n}_run_hour"
    const val SYSTEM_ERROR = "system_error"
    const val SYSTEM_ERROR_2 = "system_error_2"
    const val MULTI_FUNCTION_REGISTER = "multi_function_register"

    const val SYSTEM_PRESSURE_SETPOINT = "system_pressure_setpoint"
    const val CONTROL_STATUS = "control_status"

    // ------------------------------------------------------------------ shared live-polled RW control registers

    val driveActivationDef: RegisterDefinition = cfgBit(
        key = DRIVE_ACTIVATION,
        label = "Mode 2,3 Drive Activation",
        address = 4096,
        labels = ENABLE,
        row = 44,
        access = AccessPolicy.READ_WRITE_VERIFIED,
    )

    val pumpActivationDefs: List<RegisterDefinition> = (1..STATUS_PUMP_COUNT).map { n ->
        cfgBit(
            key = pumpActivationKey(n),
            label = "Pump $n activation",
            address = 4096 + n, // 4097=Pump1 .. 4102=Pump6 per Booster Pump Memory Map.xlsx
            labels = ENABLE,
            row = 44 + n,
            access = AccessPolicy.READ_WRITE_VERIFIED,
        )
    }

    val pressureSetpointDef: RegisterDefinition = cfgWord(
        key = SYSTEM_PRESSURE_SETPOINT,
        label = "System Pressure Setpoint",
        address = 4096,
        row = 29,
        unit = "bar",
        scale = 0.01,
        minValue = 0,
        maxValue = 6000,
        access = AccessPolicy.READ_WRITE_VERIFIED,
        rangeNote = "Target discharge pressure setpoint (0~6000 = 0.00–60.00 bar).",
    )

    val controlStatusDef: RegisterDefinition = cfgEnum(
        key = CONTROL_STATUS,
        label = "Control Status",
        address = 4173,
        labels = CONTROL_STATUS_ENUM,
        row = 73,
        access = AccessPolicy.READ_WRITE_VERIFIED,
        extra = "0: Disable (stops booster station), 1: PID, 2: ON/OFF.",
    )

    val numberOfPumpDef: RegisterDefinition = cfgWord(
        key = "number_of_pump",
        label = "Number Of Pump",
        address = 4133,
        row = 54,
        scale = 1.0,
        minValue = 1,
        maxValue = 4,
        access = AccessPolicy.READ,
        rangeNote = "Range 1–4.",
    )

    val numberOfVariablePumpDef: RegisterDefinition = cfgWord(
        key = "number_of_variable_pump",
        label = "Number Of Variable Pump",
        address = 4134,
        row = 55,
        scale = 1.0,
        minValue = 0,
        maxValue = 2,
        access = AccessPolicy.READ,
        rangeNote = "Range 0–2.",
    )

    val wiringModeDef: RegisterDefinition = cfgWord(
        key = "wiring_mode",
        label = "Wiring Mode",
        address = 4170,
        row = 72,
        scale = 1.0,
        minValue = 1,
        maxValue = 3,
        access = AccessPolicy.READ,
        rangeNote = "Wiring Mode 1–3.",
    )

    // ------------------------------------------------------------------ polled bit table (FC01: 0..33 & 4096..4102)

    private val bitStatus: List<RegisterDefinition> = buildList {
        for (i in 0 until OUTPUT_COUNT) {
            add(bit(outputKey(i), "Output $i status", i, RegisterGroup.DIGITAL_IO, ON_OFF, row = i + 1))
        }
        add(bit(DR_STATUS, "DR status", 10, RegisterGroup.DIGITAL_IO, ON_OFF, row = 11))
        for (n in 1..PR_INPUT_COUNT) {
            add(bit(prKey(n), "PR$n input status", 10 + n, RegisterGroup.DIGITAL_IO, ON_OFF, row = 11 + n))
        }
        for (n in 1..MFI_INPUT_COUNT) {
            add(bit(mfiKey(n), "MFI$n input status", 14 + n, RegisterGroup.DIGITAL_IO, ON_OFF, row = 15 + n))
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
        // Polled live so manual deactivation ('S' on LCD) updates immediately on Dashboard & Pumps screens.
        add(driveActivationDef)
        addAll(pumpActivationDefs)
    }

    // ------------------------------------------------------------------ polled word table (FC03)

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
                DRIVE1_FREQUENCY, "Drive 1 frequency", 25, RegisterGroup.MEASUREMENT,
                unit = "Hz", scale = 0.01, row = 10,
                notes = "Range 0~5000 (0.00–50.00 Hz).",
            ),
        )
        add(
            word(
                DRIVE2_FREQUENCY, "Drive 2 frequency", 26, RegisterGroup.MEASUREMENT,
                unit = "Hz", scale = 0.01, row = 11,
                notes = "Range 0~5000 (0.00–50.00 Hz).",
            ),
        )
        for (n in 1..RUN_TIME_PUMP_COUNT) {
            val base = 34 + (n - 1) * 3
            val row = 12 + (n - 1) * 3
            add(word(runSecondsKey(n), "Pump $n work time (seconds)", base, RegisterGroup.RUN_TIME, "s", 1.0, row = row))
            add(word(runMinutesKey(n), "Pump $n work time (minutes)", base + 1, RegisterGroup.RUN_TIME, "min", 1.0, row = row + 1))
            add(word(runHoursKey(n), "Pump $n work time (hours)", base + 2, RegisterGroup.RUN_TIME, "h", 1.0, row = row + 2))
        }
        add(
            RegisterDefinition(
                key = SYSTEM_ERROR,
                label = "System error (Word 1)",
                table = VendorTable.WORD,
                documentedAddress = 80,
                rawType = RawType.BITFIELD16,
                group = RegisterGroup.ALARM,
                verification = Verification.HARDWARE_VERIFIED,
                source = "$SRC_WORD, row 26",
                notes = "Bits 0–15: Floater, Max Pressure, Sensor Disconnect, Control Phase (3–6), Drive (7), Pump 1–6 (8–13), All Pump (14), System Stop by User (15).",
            ),
        )
        add(
            RegisterDefinition(
                key = SYSTEM_ERROR_2,
                label = "System error (Word 2)",
                table = VendorTable.WORD,
                documentedAddress = 81,
                rawType = RawType.BITFIELD16,
                group = RegisterGroup.ALARM,
                verification = Verification.DOCUMENTED,
                source = "$SRC_WORD, row 27",
                notes = "Bits 0–7: Mode3 Main Pump (0), Mode3 Drive (1), Full Load (2), Reserve (3), System Stopped (4), EMG Stopped (5), Reserve (6), Battery Voltage (7).",
            ),
        )
        // Live polled setpoint, operating mode, configured pump/driver counts, and control status
        add(pressureSetpointDef)
        add(numberOfPumpDef)
        add(numberOfVariablePumpDef)
        add(wiringModeDef)
        add(controlStatusDef)
    }

    // ------------------------------------------------------------------ hardware configuration (4096..4210)

    /** Complete hardware configuration map (RW and R registers strictly per official Excel). */
    val configuration: List<RegisterDefinition> = buildList {
        // ---- Coils 4096..4102 (RW) ----
        add(driveActivationDef)
        addAll(pumpActivationDefs)

        // ---- Coils 4106..4110 (R - Read-Only in official Excel) ----
        add(cfgBit("control_phase_activation", "Control phase activation", 4106, ENABLE, row = 51, access = AccessPolicy.READ))
        add(cfgBit("off_changing_activation", "OFF changing activation", 4107, ENABLE, row = 52, access = AccessPolicy.READ))
        add(cfgBit("time_changing_activation", "Time changing activation", 4108, ENABLE, row = 53, access = AccessPolicy.READ))
        add(cfgBit("reserve_auto_activation", "Reserve automatic activation", 4109, ENABLE, row = 54, access = AccessPolicy.READ))
        add(cfgBit("reserve_auto_type", "Reserve automatic type", 4110, RESERVE_TYPE, row = 55, access = AccessPolicy.READ))

        // ---- Word 4096 (RW) ----
        add(pressureSetpointDef)

        // ---- Words 4097..4111 (R - Pump 1..4 ON/OFF pressures & delays) ----
        add(cfgWord("pump_1_on_delay", "On Delay Pump 1", 4097, row = 30, unit = "s", scale = 1.0, minValue = 0, maxValue = 9999, access = AccessPolicy.READ, rangeNote = "Range 0–9999 s."))
        add(cfgWord("pump_1_off_pressure", "OFF Pressure Pump 1", 4098, row = 31, unit = "bar", scale = 0.01, minValue = 0, maxValue = 6000, access = AccessPolicy.READ, rangeNote = "Range 0.00–60.00 bar."))
        add(cfgWord("pump_1_off_delay", "OFF Delay Pump 1", 4099, row = 32, unit = "s", scale = 1.0, minValue = 0, maxValue = 9999, access = AccessPolicy.READ, rangeNote = "Range 0–9999 s."))

        add(cfgWord("pump_2_on_pressure", "On Pressure Pump 2", 4100, row = 33, unit = "bar", scale = 0.01, minValue = 0, maxValue = 6000, access = AccessPolicy.READ, rangeNote = "Range 0.00–60.00 bar."))
        add(cfgWord("pump_2_on_delay", "On Delay Pump 2", 4101, row = 34, unit = "s", scale = 1.0, minValue = 0, maxValue = 9999, access = AccessPolicy.READ, rangeNote = "Range 0–9999 s."))
        add(cfgWord("pump_2_off_pressure", "OFF Pressure Pump 2", 4102, row = 35, unit = "bar", scale = 0.01, minValue = 0, maxValue = 6000, access = AccessPolicy.READ, rangeNote = "Range 0.00–60.00 bar."))
        add(cfgWord("pump_2_off_delay", "OFF Delay Pump 2", 4103, row = 36, unit = "s", scale = 1.0, minValue = 0, maxValue = 9999, access = AccessPolicy.READ, rangeNote = "Range 0–9999 s."))

        add(cfgWord("pump_3_on_pressure", "On Pressure Pump 3", 4104, row = 37, unit = "bar", scale = 0.01, minValue = 0, maxValue = 6000, access = AccessPolicy.READ, rangeNote = "Range 0.00–60.00 bar."))
        add(cfgWord("pump_3_on_delay", "On Delay Pump 3", 4105, row = 38, unit = "s", scale = 1.0, minValue = 0, maxValue = 9999, access = AccessPolicy.READ, rangeNote = "Range 0–9999 s."))
        add(cfgWord("pump_3_off_pressure", "OFF Pressure Pump 3", 4106, row = 39, unit = "bar", scale = 0.01, minValue = 0, maxValue = 6000, access = AccessPolicy.READ, rangeNote = "Range 0.00–60.00 bar."))
        add(cfgWord("pump_3_off_delay", "OFF Delay Pump 3", 4107, row = 40, unit = "s", scale = 1.0, minValue = 0, maxValue = 9999, access = AccessPolicy.READ, rangeNote = "Range 0–9999 s."))

        add(cfgWord("pump_4_on_pressure", "On Pressure Pump 4", 4108, row = 41, unit = "bar", scale = 0.01, minValue = 0, maxValue = 6000, access = AccessPolicy.READ, rangeNote = "Range 0.00–60.00 bar."))
        add(cfgWord("pump_4_on_delay", "On Delay Pump 4", 4109, row = 42, unit = "s", scale = 1.0, minValue = 0, maxValue = 9999, access = AccessPolicy.READ, rangeNote = "Range 0–9999 s."))
        add(cfgWord("pump_4_off_pressure", "OFF Pressure Pump 4", 4110, row = 43, unit = "bar", scale = 0.01, minValue = 0, maxValue = 6000, access = AccessPolicy.READ, rangeNote = "Range 0.00–60.00 bar."))
        add(cfgWord("pump_4_off_delay", "OFF Delay Pump 4", 4111, row = 44, unit = "s", scale = 1.0, minValue = 0, maxValue = 9999, access = AccessPolicy.READ, rangeNote = "Range 0–9999 s."))

        // ---- Words 4122..4125 (Control Phase) ----
        add(cfgEnum("control_phase_type", "Control Phase Type", 4122, CONTROL_PHASE_TYPE, row = 45, access = AccessPolicy.READ))
        add(cfgWord("control_phase_min_voltage", "Control Phase Min Voltage", 4123, row = 46, unit = "V", scale = 1.0, minValue = 180, maxValue = 210, rangeNote = "Range 180–210 V."))
        add(cfgWord("control_phase_max_voltage", "Control Phase Max Voltage", 4124, row = 47, unit = "V", scale = 1.0, minValue = 240, maxValue = 280, rangeNote = "Range 240–280 V."))
        add(cfgWord("control_phase_delay", "Control Phase On/Off Delay", 4125, row = 48, unit = "s", scale = 1.0, minValue = 1, maxValue = 99, rangeNote = "Range 1–99 s."))

        // ---- Words 4127..4129 (Control Phase Sensitivity & Floater) ----
        add(cfgWord("control_phase_sensitivity", "Control Phase Sensitivity", 4127, row = 49, unit = "%", scale = 1.0, minValue = 5, maxValue = 20, rangeNote = "Range 5–20 %."))
        add(cfgWord("floater_trigger_level", "Floater Trigger Level", 4128, row = 50, unit = "kΩ", scale = 1.0, minValue = 0, maxValue = 1000, rangeNote = "Range 0–1000 kΩ."))
        add(cfgWord("floater_delay", "Floater On/Off Delay", 4129, row = 52, unit = "s", scale = 1.0, minValue = 0, maxValue = 99, rangeNote = "Range 0–99 s."))

        // ---- Words 4131..4138 (Floater/Sensor/Pumps R; Jockey & PID RW) ----
        add(cfgEnum("floater_type", "Floater Type", 4131, FLOATER_TYPE, row = 51, access = AccessPolicy.READ))
        add(cfgEnum("pressure_sensor_output_type", "Pressure Sensor Output Type", 4132, SENSOR_OUTPUT_TYPE, row = 53, access = AccessPolicy.READ))
        add(numberOfPumpDef)
        add(numberOfVariablePumpDef)
        add(cfgEnum("jockey_type", "Jockey Type", 4135, JOCKEY_TYPE, row = 56, access = AccessPolicy.READ_WRITE_VERIFIED))
        add(cfgWord("pid_p_value", "P Value", 4136, row = 57, scale = 1.0, minValue = 0, maxValue = 999, rangeNote = "PID Proportional gain (0–999)."))
        add(cfgWord("pid_i_value", "I Value", 4137, row = 58, scale = 1.0, minValue = 0, maxValue = 999, rangeNote = "PID Integral gain (0–999)."))
        add(cfgWord("pid_d_value", "D Value", 4138, row = 59, scale = 1.0, minValue = 0, maxValue = 999, rangeNote = "PID Derivative gain (0–999)."))

        // ---- Words 4142..4143 (Max Pressure R, Sensor Calibration RW) ----
        add(cfgWord("max_pressure_value", "Max Pressure Value", 4142, row = 60, unit = "bar", scale = 0.01, minValue = 0, maxValue = 2500, access = AccessPolicy.READ, rangeNote = "Range 0.00–25.00 bar."))
        add(cfgWord("sensor_calibration_value", "Sensor Calibration Value", 4143, row = 61, scale = 1.0, minValue = 1, maxValue = 999, rangeNote = "Range 1–999."))

        // ---- Words 4149..4152 (Wakeup/Sleep RW, Sensor Input Type R) ----
        add(cfgWord("wakeup_differential", "Wake up Differential", 4149, row = 62, unit = "bar", scale = 0.01, minValue = 0, maxValue = 1000, rangeNote = "Range 0.00–10.00 bar."))
        add(cfgWord("sleep_frequency", "Sleep Frequency", 4150, row = 63, unit = "Hz", scale = 0.01, minValue = 0, maxValue = 5000, rangeNote = "Range 0.00–50.00 Hz."))
        add(cfgWord("sleep_off_delay", "Sleep Off Delay", 4151, row = 64, unit = "s", scale = 1.0, minValue = 0, maxValue = 99, rangeNote = "Range 0–99 s."))
        add(cfgEnum("pressure_sensor_input_type", "Pressure Sensor Input Type", 4152, SENSOR_INPUT_TYPE, row = 65, access = AccessPolicy.READ))

        // ---- Word 4156 (Mode 3 Switch Time R) ----
        add(cfgWord("mode3_switch_time", "Mode 3 Switch Time", 4156, row = 66, unit = "s", scale = 0.01, minValue = 2, maxValue = 100, access = AccessPolicy.READ, rangeNote = "Range 0.02–1.00 s."))

        // ---- Words 4161..4164, 4166 (Timing & Constant Pump RW) ----
        add(cfgWord("time_changing_time", "Time Changing Time", 4161, row = 67, unit = "min", scale = 1.0, minValue = 1, maxValue = 9999, rangeNote = "Range 1–9999 min."))
        add(cfgWord("add_delay", "Add Delay", 4162, row = 68, unit = "s", scale = 0.1, minValue = 0, maxValue = 999, rangeNote = "Range 0.0–99.9 s."))
        add(cfgWord("remove_delay", "Remove Delay", 4163, row = 69, unit = "s", scale = 0.1, minValue = 0, maxValue = 999, rangeNote = "Range 0.0–99.9 s."))
        add(cfgWord("wakeup_on_delay", "Wake up On Delay", 4164, row = 70, unit = "s", scale = 1.0, minValue = 0, maxValue = 99, rangeNote = "Range 0–99 s."))
        add(cfgWord("constant_pump_percent", "Constant Pump Percent", 4166, row = 71, unit = "%", scale = 1.0, minValue = 0, maxValue = 100, rangeNote = "Range 0–100 %."))

        // ---- Word 4170 (Wiring Mode R), Word 4173 (Control Status RW), Word 4175 (Sleep Gap RW) ----
        add(wiringModeDef)
        add(controlStatusDef)
        add(
            cfgWord(
                key = "sleep_gap",
                label = "Sleep Gap",
                address = 4175,
                row = 74,
                unit = "bar",
                scale = 0.01,
                minValue = 1,
                maxValue = 100,
                rangeNote = "FC06 write verified on hardware (40 -> 41 -> 40; range 1–100 = 0.01–1.00 bar).",
                verification = Verification.HARDWARE_VERIFIED,
            ),
        )

        // ---- Words 4177..4179 (Auto Service RW) ----
        add(cfgWord("auto_service_off_time", "Auto Service Off Time", 4177, row = 75, unit = "h", scale = 1.0, minValue = 1, maxValue = 9999, rangeNote = "Range 1–9999 h.", verification = Verification.HARDWARE_VERIFIED))
        add(cfgWord("auto_service_on_time", "Auto Service On Time", 4178, row = 76, unit = "s", scale = 0.1, minValue = 1, maxValue = 999, rangeNote = "Range 0.1–99.9 s.", verification = Verification.HARDWARE_VERIFIED))
        add(cfgWord("auto_service_interval", "Auto Service Interval", 4179, row = 77, unit = "min", scale = 1.0, minValue = 1, maxValue = 99, rangeNote = "Range 1–99 min.", verification = Verification.HARDWARE_VERIFIED))

        // ---- Words 4180..4187 (MFI1..4 & MFO1..4 Types — R in official Excel) ----
        for (n in 1..MFI_INPUT_COUNT) {
            add(cfgEnum("mfi_${n}_type", "MFI$n Type", 4179 + n, MFI_TYPES, row = 77 + n, access = AccessPolicy.READ, verification = Verification.HARDWARE_VERIFIED))
        }
        for (n in 1..MFO_OUTPUT_COUNT) {
            add(cfgEnum("mfo_${n}_type", "MFO$n Type", 4183 + n, MFO_TYPES, row = 81 + n, access = AccessPolicy.READ, verification = if (n <= 3) Verification.HARDWARE_VERIFIED else Verification.DOCUMENTED))
        }

        // ---- Words 4188..4195 (System / Drive / Full Load / Modbus Address — R) ----
        add(cfgWord("serial_number", "Serial Number", 4188, row = 86, scale = 1.0, minValue = 0, maxValue = 99999, access = AccessPolicy.READ, rangeNote = "Controller serial number."))
        add(cfgWord("start_frequency", "Start Frequency", 4189, row = 87, unit = "Hz", scale = 0.01, minValue = 100, maxValue = 5000, access = AccessPolicy.READ, rangeNote = "Range 1.00–50.00 Hz."))
        add(cfgEnum("full_load_status", "Full Load Status", 4190, ENABLE, row = 88, access = AccessPolicy.READ))
        add(cfgWord("full_load_pressure", "Full Load Pressure", 4191, row = 89, unit = "bar", scale = 0.01, minValue = 0, maxValue = 9999, access = AccessPolicy.READ, rangeNote = "Range 0.00–99.99 bar."))
        add(cfgWord("full_load_time", "Full Load Time", 4192, row = 90, unit = "s", scale = 1.0, minValue = 0, maxValue = 9999, access = AccessPolicy.READ, rangeNote = "Range 0–9999 s."))
        add(
            cfgWord(
                key = "stop_frequency",
                label = "Stop Frequency",
                address = 4193,
                row = 91,
                unit = "Hz",
                scale = 0.01,
                minValue = 100,
                maxValue = 5000,
                access = AccessPolicy.READ,
                rangeNote = "Range 1.00–50.00 Hz (Excel col G typo '0.01Bar' corrected to 0.01 Hz matching Start Frequency 4189).",
            ),
        )
        add(cfgEnum("control_direction", "Control Direction", 4194, CONTROL_DIRECTION_ENUM, row = 92, access = AccessPolicy.READ))
        add(cfgWord("modbus_address", "ModBus Address", 4195, row = 93, scale = 1.0, minValue = 1, maxValue = 247, access = AccessPolicy.READ, rangeNote = "Modbus Slave ID (verified = 1).", verification = Verification.HARDWARE_VERIFIED))

        // ---- Words 4206..4210 (AO Type, LCD Contrast, AO Gain — R) ----
        add(cfgEnum("ao1_type", "AO1 Type", 4206, AO_TYPE_ENUM, row = 94, access = AccessPolicy.READ))
        add(cfgEnum("ao2_type", "AO2 Type", 4207, AO_TYPE_ENUM, row = 95, access = AccessPolicy.READ))
        add(cfgWord("lcd_contrast", "LCD Contrast", 4208, row = 96, scale = 1.0, minValue = 0, maxValue = 20, access = AccessPolicy.READ, rangeNote = "Range 0–20."))
        add(cfgWord("ao1_gain", "AO1 Gain", 4209, row = 97, scale = 0.0001, minValue = 900, maxValue = 1100, access = AccessPolicy.READ, rangeNote = "Range 900–1100 (× 1E-4)."))
        add(cfgWord("ao2_gain", "AO2 Gain", 4210, row = 98, scale = 0.0001, minValue = 900, maxValue = 1100, access = AccessPolicy.READ, rangeNote = "Range 900–1100 (× 1E-4)."))
    }

    // ------------------------------------------------------------------ command register (Word 99, RW)

    val multiFunctionRegisterDef: RegisterDefinition = RegisterDefinition(
        key = MULTI_FUNCTION_REGISTER,
        label = "Multi Function Register (Reset Pump Error)",
        table = VendorTable.WORD,
        documentedAddress = 99,
        rawType = RawType.ENUM16,
        group = RegisterGroup.COMMAND,
        minValue = CMD_RESET_PUMP_ERROR,
        maxValue = CMD_RESET_PUMP_ERROR,
        enumLabels = MULTI_FUNCTION_ENUM,
        access = AccessPolicy.READ_WRITE_VERIFIED,
        verification = Verification.HARDWARE_VERIFIED,
        source = "$SRC_WORD, row 28",
        notes = "Official RW command register: 0 = No Function, 25 = Reset Pump Error (pulse command, auto-clears to 0).",
    )

    val commands: List<RegisterDefinition> = listOf(multiFunctionRegisterDef)

    // ------------------------------------------------------------------ manual outputs (R in official firmware -> LOCKED)

    /**
     * Manual output registers are explicitly marked Read-Only (R) in DGP's official
     * Booster Pump Memory Map.xlsx, matching physical bench results (FC05 Coil 56 &
     * FC06 Word 50 returned Exception 0x02 Illegal Data Address).
     */
    val manualControl: List<RegisterDefinition> = buildList {
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
                    verification = Verification.HARDWARE_VERIFIED,
                    source = "$SRC_BIT, row ${35 + i}",
                    notes = "Marked Read-Only (R) in official DGP Excel memory map and confirmed Exception 0x02 on FC05 write.",
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
                    minValue = 0,
                    maxValue = 500,
                    access = AccessPolicy.CONTROL_LOCKED,
                    verification = Verification.HARDWARE_VERIFIED,
                    source = "$SRC_WORD, row ${24 + i}",
                    notes = "Marked Read-Only (R) in official DGP Excel memory map and confirmed Exception 0x02 on FC06 write.",
                ),
            )
        }
    }

    // ------------------------------------------------------------------ aggregates

    /** Values read on every live poll cycle. */
    val polled: List<RegisterDefinition> = bitStatus + wordStatus

    /** Deduplicated complete registry of all 154 documented BPC02 registers. */
    val all: List<RegisterDefinition> = (polled + commands + configuration + manualControl).distinctBy { it.key }

    private val byKey: Map<String, RegisterDefinition> = all.associateBy { it.key }

    fun byKey(key: String): RegisterDefinition =
        byKey[key] ?: throw IllegalArgumentException("Unknown BPC02 register key '$key'")

    /** Contiguous live read blocks covering [polled], skipping all undocumented gaps. */
    val pollBlocks: List<ReadBlock> = buildBlocks(polled)

    /** High-priority live blocks read on every poll cycle (5 compact blocks for stable 9600-baud RS485). */
    val fastPollBlocks: List<ReadBlock> = listOf(
        ReadBlock(VendorTable.BIT, 0, 34), // Outputs, DR, PR, MFI, Pump 1..6 Run/Fail, Floaters
        ReadBlock(VendorTable.BIT, 4096, 7), // Drive + Pump 1..6 Activation ('S' state)
        ReadBlock(VendorTable.WORD, 2, 8), // System Pressure, Voltages, Mains Frequency
        ReadBlock(VendorTable.WORD, 25, 2), // Drive 1 & Drive 2 Frequency (0.01 Hz)
        ReadBlock(VendorTable.WORD, 80, 2), // System Error Word 1 & Word 2
    )

    /** Slower-changing telemetry blocks polled on connect and refreshed periodically to avoid congesting RS485. */
    val slowPollBlocks: List<ReadBlock> = listOf(
        ReadBlock(VendorTable.WORD, 0, 1), // Displayed LCD Page
        ReadBlock(VendorTable.WORD, 34, 12), // Pump 1..4 Work Time
        ReadBlock(VendorTable.WORD, 4096, 1), // System Pressure Setpoint
        ReadBlock(VendorTable.WORD, 4133, 2), // Number Of Pump (4133) & Number Of Variable Pump / Drivers (4134)
        ReadBlock(VendorTable.WORD, 4170, 1), // Wiring / Operating Mode (1..3)
        ReadBlock(VendorTable.WORD, 4173, 1), // Control Status (0=Disable, 1=PID, 2=ON/OFF)
    )

    /** Contiguous on-demand read blocks covering [configuration], skipping all undocumented gaps. */
    val configurationBlocks: List<ReadBlock> = buildBlocks(configuration)

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
        access = AccessPolicy.READ,
        verification = Verification.HARDWARE_VERIFIED,
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
        verification: Verification = Verification.HARDWARE_VERIFIED,
        notes: String? = null,
    ) = RegisterDefinition(
        key = key, label = label, table = VendorTable.WORD, documentedAddress = address,
        rawType = RawType.UINT16, group = group, unit = unit, scale = scale,
        access = AccessPolicy.READ,
        verification = verification, source = "$SRC_WORD, row $row", notes = notes,
    )

    private fun cfgBit(
        key: String,
        label: String,
        address: Int,
        labels: Map<Int, String>,
        row: Int,
        access: AccessPolicy = AccessPolicy.READ,
    ) = RegisterDefinition(
        key = key, label = label, table = VendorTable.BIT, documentedAddress = address,
        rawType = RawType.BIT, group = RegisterGroup.CONFIGURATION,
        minValue = 0, maxValue = 1,
        enumLabels = labels,
        access = access,
        verification = Verification.HARDWARE_VERIFIED,
        source = "$SRC_BIT, row $row",
        notes = if (access == AccessPolicy.READ_WRITE_VERIFIED) RW_NOTE else RO_NOTE,
    )

    private fun cfgWord(
        key: String,
        label: String,
        address: Int,
        row: Int,
        unit: String? = null,
        scale: Double? = null,
        minValue: Int? = null,
        maxValue: Int? = null,
        access: AccessPolicy = AccessPolicy.READ_WRITE_VERIFIED,
        rangeNote: String,
        verification: Verification = Verification.DOCUMENTED,
    ) = RegisterDefinition(
        key = key, label = label, table = VendorTable.WORD, documentedAddress = address,
        rawType = RawType.UINT16, group = RegisterGroup.CONFIGURATION,
        unit = unit, scale = scale,
        minValue = minValue, maxValue = maxValue,
        access = access,
        verification = verification,
        source = "$SRC_WORD, row $row",
        notes = "$rangeNote ${if (access == AccessPolicy.READ_WRITE_VERIFIED) RW_NOTE else RO_NOTE}",
    )

    private fun cfgEnum(
        key: String,
        label: String,
        address: Int,
        labels: Map<Int, String>,
        row: Int,
        access: AccessPolicy = AccessPolicy.READ,
        extra: String = "",
        verification: Verification = Verification.DOCUMENTED,
    ) = RegisterDefinition(
        key = key, label = label, table = VendorTable.WORD, documentedAddress = address,
        rawType = RawType.ENUM16, group = RegisterGroup.CONFIGURATION,
        minValue = labels.keys.minOrNull(), maxValue = labels.keys.maxOrNull(),
        enumLabels = labels,
        access = access,
        verification = verification,
        source = "$SRC_WORD, row $row",
        notes = listOf(extra, if (access == AccessPolicy.READ_WRITE_VERIFIED) RW_NOTE else RO_NOTE)
            .filter { it.isNotBlank() }
            .joinToString(" "),
    )
}
