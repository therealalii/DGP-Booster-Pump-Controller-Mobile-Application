package com.dgp.boosterpump.controller

import com.dgp.boosterpump.controller.register.Bpc02RegisterMap
import com.dgp.boosterpump.controller.register.RegisterDefinition
import java.util.Locale

/**
 * Authoritative DGP BPC-02 official catalog metadata derived directly from `BPC-02.pdf`
 * (DGP BPC-02 User Manual) and `Booster Pump Memory Map.xlsx`:
 *
 * 1. Exact English controller menu categories, page names, parameter names, enum values, units,
 *    and alarm/error names matching the physical BPC-02 LCD interface and official documentation.
 * 2. Concise, user-friendly Persian (`fa`) help explanations for every parameter and key system
 *    indicator, prioritizing official manual guidance so ordinary users immediately understand
 *    what each parameter does and how it affects the booster pump system.
 */
object Bpc02Catalog {

    /** Help entry presented when the user taps the `?` icon next to a parameter or status label. */
    data class ParameterHelp(
        val key: String,
        val englishName: String,
        val fullEnglishName: String,
        val menuPath: String,
        val helpFa: String,
        val rangeHint: String? = null,
        val isWritable: Boolean = false,
    )

    /** Mode relevance for pages inside `Hardware › 4. Control`. */
    enum class ControlPageMode {
        ALWAYS,
        PID_ONLY,
        ON_OFF_ONLY,
    }

    /** Top-level physical BPC-02 `Menu` entries matching `controller-pics-new/photo_6`. */
    enum class MainMenuSection(
        val index: Int,
        val title: String,
        val subtitleEn: String,
    ) {
        SYSTEM(1, "System", "Password Setting, Blight, Contrast, Beep On Click / Error"),
        INPUT(2, "Input", "Drive, PR1–PR4, MFI1–MFI4 Live Inputs"),
        TIME_DATE(3, "Time/Date", "Clock, Date, Backup Battery (VBat)"),
        HARDWARE(4, "Hardware", "Control Phase, Floater, Sensor, Control, MFx, Serial, Network, Reset"),
    }

    /** Configuration registers belonging to top-level `Menu › 1. System` (`photo_5`). */
    val systemRegisterKeys: List<String> = listOf("lcd_contrast")

    /** One page inside a physical DGP BPC-02 Hardware menu category. */
    data class MenuPage(
        val number: Int,
        val title: String,
        val registerKeys: List<String>,
        val controlMode: ControlPageMode = ControlPageMode.ALWAYS,
        val displayRegisterKeys: List<String> = registerKeys,
    )

    /**
     * Exact physical DGP BPC-02 `4. Hardware` menu categories matching `controller-pics-new`
     * (`photo_9` & `photo_10`) and `BPC-02.pdf`:
     *  1. Control Phase
     *  2. Floater
     *  3. Sensor
     *  4. Control (with mode-specific ON/OFF Pump 1–4 pages & PID pages)
     *  5. MFx
     *  6. Serial Number
     *  7. Network
     *  8. Reset to Factory
     */
    enum class MenuCategory(
        val index: Int,
        val title: String,
        val subtitleEn: String,
        val pages: List<MenuPage>,
    ) {
        CONTROL_PHASE(
            index = 1,
            title = "Control Phase",
            subtitleEn = "Status, Type (1P/3P), Min/Max Voltage, Delay, Sensitivity",
            pages = listOf(
                MenuPage(
                    number = 1,
                    title = "Voltage & Phase Type",
                    registerKeys = listOf(
                        "control_phase_activation",
                        "control_phase_type",
                        "control_phase_min_voltage",
                        "control_phase_max_voltage",
                    ),
                ),
                MenuPage(
                    number = 2,
                    title = "Delay & Sensitivity",
                    registerKeys = listOf(
                        "control_phase_delay",
                        "control_phase_sensitivity",
                    ),
                ),
            ),
        ),
        FLOATER(
            index = 2,
            title = "Floater",
            subtitleEn = "Type (Disable / Internal / External), Trg, Dly",
            pages = listOf(
                MenuPage(
                    number = 1,
                    title = "Floater",
                    registerKeys = listOf(
                        "floater_type",
                        "floater_trigger_level",
                        "floater_delay",
                    ),
                ),
            ),
        ),
        SENSOR(
            index = 3,
            title = "Sensor",
            subtitleEn = "Input Type, Output Type, Max Pressure, Calib Const",
            pages = listOf(
                MenuPage(
                    number = 1,
                    title = "Input Type",
                    registerKeys = listOf("pressure_sensor_input_type"),
                ),
                MenuPage(
                    number = 2,
                    title = "Output Type",
                    registerKeys = listOf("pressure_sensor_output_type"),
                ),
                MenuPage(
                    number = 3,
                    title = "Max Pressure & Calib Const",
                    registerKeys = listOf(
                        "max_pressure_value",
                        "sensor_calibration_value",
                    ),
                ),
            ),
        ),
        CONTROL(
            index = 4,
            title = "Control",
            subtitleEn = "Pump, Wiring, Status, Control (Disable / PID / ON-OFF), Pump 1–4, Sleep, Wake Up, PID",
            pages = listOf(
                MenuPage(
                    number = 1,
                    title = "Pump",
                    registerKeys = listOf("number_of_pump", "number_of_variable_pump"),
                ),
                MenuPage(
                    number = 2,
                    title = "Wiring",
                    registerKeys = listOf("wiring_mode"),
                ),
                MenuPage(
                    number = 3,
                    title = "Status",
                    registerKeys = listOf(
                        Bpc02RegisterMap.DRIVE_ACTIVATION,
                        "pump_1_activation",
                        "pump_2_activation",
                        "pump_3_activation",
                        "pump_4_activation",
                        "pump_5_activation",
                        "pump_6_activation",
                    ),
                ),
                MenuPage(
                    number = 4,
                    title = "Control",
                    registerKeys = listOf(
                        Bpc02RegisterMap.CONTROL_STATUS,
                        "control_direction",
                        Bpc02RegisterMap.SYSTEM_PRESSURE_SETPOINT,
                    ),
                ),
                // Exact ON/OFF Pump 1..4 pages matching controller-pics-new/photo_1, photo_2, photo_7:
                // On Delay -> Off Delay -> On Pressure -> Off Pressure
                MenuPage(
                    number = 5,
                    title = "Pump 1",
                    registerKeys = listOf(
                        "pump_1_on_delay",
                        "pump_1_off_delay",
                        "pump_1_off_pressure",
                    ),
                    controlMode = ControlPageMode.ON_OFF_ONLY,
                    displayRegisterKeys = listOf(
                        "pump_1_on_delay",
                        "pump_1_off_delay",
                        Bpc02RegisterMap.SYSTEM_PRESSURE_SETPOINT,
                        "pump_1_off_pressure",
                    ),
                ),
                MenuPage(
                    number = 6,
                    title = "Pump 2",
                    registerKeys = listOf(
                        "pump_2_on_delay",
                        "pump_2_off_delay",
                        "pump_2_on_pressure",
                        "pump_2_off_pressure",
                    ),
                    controlMode = ControlPageMode.ON_OFF_ONLY,
                ),
                MenuPage(
                    number = 7,
                    title = "Pump 3",
                    registerKeys = listOf(
                        "pump_3_on_delay",
                        "pump_3_off_delay",
                        "pump_3_on_pressure",
                        "pump_3_off_pressure",
                    ),
                    controlMode = ControlPageMode.ON_OFF_ONLY,
                ),
                MenuPage(
                    number = 8,
                    title = "Pump 4",
                    registerKeys = listOf(
                        "pump_4_on_delay",
                        "pump_4_off_delay",
                        "pump_4_on_pressure",
                        "pump_4_off_pressure",
                    ),
                    controlMode = ControlPageMode.ON_OFF_ONLY,
                ),
                MenuPage(
                    number = 9,
                    title = "Add Remove Pump",
                    registerKeys = listOf(
                        "constant_pump_percent",
                        "add_delay",
                        "remove_delay",
                        "mode3_switch_time",
                    ),
                    controlMode = ControlPageMode.PID_ONLY,
                ),
                MenuPage(
                    number = 10,
                    title = "Sleep",
                    registerKeys = listOf(
                        "sleep_frequency",
                        "sleep_off_delay",
                        "sleep_gap",
                    ),
                    controlMode = ControlPageMode.PID_ONLY,
                ),
                MenuPage(
                    number = 11,
                    title = "Wake Up",
                    registerKeys = listOf(
                        "wakeup_differential",
                        "wakeup_on_delay",
                        "start_frequency",
                        "stop_frequency",
                    ),
                    controlMode = ControlPageMode.PID_ONLY,
                ),
                MenuPage(
                    number = 12,
                    title = "Off Changing",
                    registerKeys = listOf(
                        "off_changing_activation",
                        "jockey_type",
                    ),
                ),
                MenuPage(
                    number = 13,
                    title = "Time Changing",
                    registerKeys = listOf(
                        "time_changing_activation",
                        "time_changing_time",
                    ),
                ),
                MenuPage(
                    number = 14,
                    title = "Reserve Automatic",
                    registerKeys = listOf(
                        "reserve_auto_activation",
                        "reserve_auto_type",
                    ),
                ),
                MenuPage(
                    number = 15,
                    title = "Full Load",
                    registerKeys = listOf(
                        "full_load_status",
                        "full_load_pressure",
                        "full_load_time",
                    ),
                ),
                MenuPage(
                    number = 16,
                    title = "Auto Service",
                    registerKeys = listOf(
                        "auto_service_off_time",
                        "auto_service_on_time",
                        "auto_service_interval",
                    ),
                ),
                MenuPage(
                    number = 17,
                    title = "PID",
                    registerKeys = listOf(
                        "pid_p_value",
                        "pid_i_value",
                        "pid_d_value",
                    ),
                    controlMode = ControlPageMode.PID_ONLY,
                ),
            ),
        ),
        MFX(
            index = 5,
            title = "MFx",
            subtitleEn = "MFI1–MFI4, MFO1–MFO4, AO1 / AO2",
            pages = listOf(
                MenuPage(
                    number = 1,
                    title = "MFI (Pages 1–4)",
                    registerKeys = listOf("mfi_1_type", "mfi_2_type", "mfi_3_type", "mfi_4_type"),
                ),
                MenuPage(
                    number = 2,
                    title = "MFO (Pages 5–8)",
                    registerKeys = listOf("mfo_1_type", "mfo_2_type", "mfo_3_type", "mfo_4_type"),
                ),
                MenuPage(
                    number = 3,
                    title = "AO1 / AO2 (Pages 9–10)",
                    registerKeys = listOf("ao1_type", "ao2_type", "ao1_gain", "ao2_gain"),
                ),
            ),
        ),
        SERIAL_NUMBER(
            index = 6,
            title = "Serial Number",
            subtitleEn = "Controller Serial Number",
            pages = listOf(
                MenuPage(
                    number = 1,
                    title = "Serial Number",
                    registerKeys = listOf("serial_number"),
                ),
            ),
        ),
        NETWORK(
            index = 7,
            title = "Network",
            subtitleEn = "ModBus-RTU Network Address (Slave ID)",
            pages = listOf(
                MenuPage(
                    number = 1,
                    title = "Network",
                    registerKeys = listOf("modbus_address"),
                ),
            ),
        ),
        RESET_TO_FACTORY(
            index = 8,
            title = "Reset to Factory",
            subtitleEn = "Factory Default Reset (Controller Keypad Only)",
            pages = emptyList(),
        ),
    }

    /** Complete deduplicated register key coverage across `1. System` and `4. Hardware`. */
    val allMenuRegisterKeys: List<String>
        get() = systemRegisterKeys + MenuCategory.entries.flatMap { cat -> cat.pages.flatMap { it.registerKeys } }

    /** Official allowed options for MFI1–MFI4 (Words 4180–4183, Read-Only over Modbus). */
    val mfiAllowedOptions: List<String> = listOf(
        "Disable", "PS", "TS", "MP", "EMG", "CP", "FL", "PR5", "PR6", "Reset", "Fix",
    )

    /** Official allowed options for MFO1–MFO4 (Words 4184–4187 / Relay OUT 7–10, Read-Only over Modbus). */
    val mfoAllowedOptions: List<String> = listOf(
        "Disable", "MC3", "DC4", "MC4", "Fan", "Alarm", "E.Valve", "P.Run", "Ready", "Filler",
        "P1_ERR", "P2_ERR", "P3_ERR", "P4_ERR",
    )

    fun mfiOptionLabel(raw: Int?): String = when (raw) {
        null -> "—"
        0 -> "Disable"
        1 -> "PS"
        2 -> "TS"
        3 -> "MP"
        4 -> "EMG"
        5 -> "CP"
        6 -> "FL"
        7 -> "PR5"
        8 -> "PR6"
        9 -> "Reset"
        10 -> "Fix"
        else -> "Unknown ($raw)"
    }

    fun mfoOptionLabel(raw: Int?): String = when (raw) {
        null -> "—"
        0 -> "Disable"
        1 -> "MC3"
        2 -> "DC4"
        3 -> "MC4"
        4 -> "Fan"
        5 -> "Alarm"
        6 -> "E.Valve"
        7 -> "P.Run"
        8 -> "Ready"
        9 -> "Filler"
        10 -> "P1_ERR"
        11 -> "P2_ERR"
        12 -> "P3_ERR"
        13 -> "P4_ERR"
        else -> "Unknown ($raw)"
    }

    /** Exact short English parameter name as displayed on the BPC-02 LCD page. */
    fun lcdParameterName(def: RegisterDefinition): String = when (def.key) {
        // Control Phase (BPC-02.pdf p.13)
        "control_phase_activation" -> "Status"
        "control_phase_type" -> "Type"
        "control_phase_min_voltage" -> "Min"
        "control_phase_max_voltage" -> "Max"
        "control_phase_delay" -> "Delay"
        "control_phase_sensitivity" -> "Sensitivity"

        // Floater (BPC-02.pdf p.14)
        "floater_type" -> "Type"
        "floater_trigger_level" -> "Trg"
        "floater_delay" -> "Dly"

        // Sensor (BPC-02.pdf p.14)
        "pressure_sensor_input_type" -> "Input Type"
        "pressure_sensor_output_type" -> "Output Type"
        "max_pressure_value" -> "Max Pressure"
        "sensor_calibration_value" -> "Calib Const"

        // Control — Pump & Wiring & Status & Control (BPC-02.pdf p.15–16, photo_4)
        "number_of_pump" -> "Main"
        "number_of_variable_pump" -> "Drive"
        "wiring_mode" -> "Wiring Type"
        Bpc02RegisterMap.DRIVE_ACTIVATION -> "Drive"
        Bpc02RegisterMap.CONTROL_STATUS -> "1. Status"
        "control_direction" -> "Dir"
        Bpc02RegisterMap.SYSTEM_PRESSURE_SETPOINT -> "Setpoint"

        // Control — Add Remove Pump (BPC-02.pdf p.16)
        "constant_pump_percent" -> "Percent"
        "add_delay" -> "Add Delay"
        "remove_delay" -> "Remove Delay"
        "mode3_switch_time" -> "Switch Delay"

        // Control — Sleep (BPC-02.pdf p.17)
        "sleep_frequency" -> "Frequency"
        "sleep_off_delay" -> "Off Delay"
        "sleep_gap" -> "Gap"

        // Control — Wake Up (BPC-02.pdf p.17)
        "wakeup_differential" -> "Differential"
        "wakeup_on_delay" -> "On Delay"
        "start_frequency" -> "Start Frq"
        "stop_frequency" -> "Stop Frq"

        // Control — Off Changing / Time Changing / Reserve Automatic / Full Load / Auto Service / PID (p.18–19, photo_3)
        "off_changing_activation" -> "Status"
        "jockey_type" -> "Jockey"
        "time_changing_activation" -> "Status"
        "time_changing_time" -> "Time"
        "reserve_auto_activation" -> "Status"
        "reserve_auto_type" -> "Type"
        "full_load_status" -> "Status"
        "full_load_pressure" -> "Pressure"
        "full_load_time" -> "On Delay"
        "auto_service_off_time" -> "Off Time"
        "auto_service_on_time" -> "On Time"
        "auto_service_interval" -> "Interval Time"
        "pid_p_value" -> "1. P"
        "pid_i_value" -> "2. I"
        "pid_d_value" -> "3. D"

        // MFx (BPC-02.pdf p.19–20)
        "ao1_type" -> "AO1 Type"
        "ao2_type" -> "AO2 Type"
        "ao1_gain" -> "AO1 Gain"
        "ao2_gain" -> "AO2 Gain"

        // Serial Number, Network & System (photo_5, photo_10)
        "serial_number" -> "Serial Number"
        "modbus_address" -> "Network Address"
        "lcd_contrast" -> "Contrast"

        // Live telemetry & dynamic keys
        Bpc02RegisterMap.SYSTEM_PRESSURE -> "System Pressure"
        Bpc02RegisterMap.DR_STATUS -> "Drive Input (DR)"
        Bpc02RegisterMap.DRIVE_FAIL -> "Drive Fail"
        Bpc02RegisterMap.FLOATER_LOW -> "Floater L"
        Bpc02RegisterMap.FLOATER_HIGH -> "Floater H"
        Bpc02RegisterMap.V_RN -> "VRN"
        Bpc02RegisterMap.V_SN -> "VSN"
        Bpc02RegisterMap.V_TN -> "VTN"
        Bpc02RegisterMap.V_RS -> "VRS"
        Bpc02RegisterMap.V_ST -> "VST"
        Bpc02RegisterMap.V_TR -> "VTR"
        Bpc02RegisterMap.MAINS_FREQUENCY -> "Mains FRQ"
        Bpc02RegisterMap.DRIVE1_FREQUENCY -> "Drive 1 FRQ"
        Bpc02RegisterMap.DRIVE2_FREQUENCY -> "Drive 2 FRQ"
        Bpc02RegisterMap.MULTI_FUNCTION_REGISTER -> "Reset Pump Error"
        else -> when {
            def.key.startsWith("pump_") && def.key.endsWith("_activation") ->
                "Pump ${def.key.removePrefix("pump_").removeSuffix("_activation")}"
            // Inside Pump 1..4 pages (`photo_1`, `photo_2`, `photo_7`) exact LCD labels are short:
            def.key.startsWith("pump_") && def.key.endsWith("_on_pressure") -> "On Pressure"
            def.key.startsWith("pump_") && def.key.endsWith("_on_delay") -> "On Delay"
            def.key.startsWith("pump_") && def.key.endsWith("_off_pressure") -> "Off Pressure"
            def.key.startsWith("pump_") && def.key.endsWith("_off_delay") -> "Off Delay"
            def.key.startsWith("mfi_") && def.key.endsWith("_type") ->
                "MFI${def.key.removePrefix("mfi_").removeSuffix("_type")} Type"
            def.key.startsWith("mfo_") && def.key.endsWith("_type") ->
                "MFO${def.key.removePrefix("mfo_").removeSuffix("_type")} Type"
            else -> def.label
        }
    }

    /** Full unambiguous English catalog name for standalone banners, import diffs, and help headers. */
    fun fullParameterName(def: RegisterDefinition): String = when (def.key) {
        "control_phase_activation" -> "Control Phase Status"
        "control_phase_type" -> "Control Phase Type"
        "control_phase_min_voltage" -> "Control Phase Min Voltage"
        "control_phase_max_voltage" -> "Control Phase Max Voltage"
        "control_phase_delay" -> "Control Phase Delay"
        "control_phase_sensitivity" -> "Control Phase Sensitivity"
        "floater_type" -> "Floater Type"
        "floater_trigger_level" -> "Floater Trg (Trigger Level)"
        "floater_delay" -> "Floater Dly (Delay)"
        "pressure_sensor_input_type" -> "Sensor Input Type"
        "pressure_sensor_output_type" -> "Sensor Output Type"
        "max_pressure_value" -> "Sensor Max Pressure"
        "sensor_calibration_value" -> "Sensor Calib Const"
        "number_of_pump" -> "Main (Number of Pumps)"
        "number_of_variable_pump" -> "Drive (Variable Pumps)"
        "wiring_mode" -> "Wiring Mode"
        Bpc02RegisterMap.DRIVE_ACTIVATION -> "Drive Status (Service Enable)"
        Bpc02RegisterMap.CONTROL_STATUS -> "Control Status"
        "control_direction" -> "Control Dir"
        Bpc02RegisterMap.SYSTEM_PRESSURE_SETPOINT -> "Setpoint / Pump 1 On Pressure"
        "constant_pump_percent" -> "Add/Remove Percent"
        "add_delay" -> "Add Delay"
        "remove_delay" -> "Remove Delay"
        "mode3_switch_time" -> "Mode 3 Switch Delay"
        "sleep_frequency" -> "Sleep Frequency"
        "sleep_off_delay" -> "Sleep Off Delay"
        "sleep_gap" -> "Sleep Gap"
        "wakeup_differential" -> "Wake Up Differential"
        "wakeup_on_delay" -> "Wake Up On Delay"
        "start_frequency" -> "Start Frq"
        "stop_frequency" -> "Stop Frq"
        "off_changing_activation" -> "Off Changing Status"
        "jockey_type" -> "Jockey"
        "time_changing_activation" -> "Time Changing Status"
        "time_changing_time" -> "Time Changing Time"
        "reserve_auto_activation" -> "Reserve Automatic Status"
        "reserve_auto_type" -> "Reserve Automatic Type"
        "full_load_status" -> "Full Load Status"
        "full_load_pressure" -> "Full Load Pressure"
        "full_load_time" -> "Full Load On Delay"
        "auto_service_off_time" -> "Auto Service Off Time"
        "auto_service_on_time" -> "Auto Service On Time"
        "auto_service_interval" -> "Auto Service Interval Time"
        "pid_p_value" -> "PID P"
        "pid_i_value" -> "PID I"
        "pid_d_value" -> "PID D"
        else -> when {
            def.key.startsWith("pump_") && def.key.endsWith("_on_pressure") ->
                "Pump ${def.key.removePrefix("pump_").removeSuffix("_on_pressure")} On Pressure"
            def.key.startsWith("pump_") && def.key.endsWith("_on_delay") ->
                "Pump ${def.key.removePrefix("pump_").removeSuffix("_on_delay")} On Delay"
            def.key.startsWith("pump_") && def.key.endsWith("_off_pressure") ->
                "Pump ${def.key.removePrefix("pump_").removeSuffix("_off_pressure")} Off Pressure"
            def.key.startsWith("pump_") && def.key.endsWith("_off_delay") ->
                "Pump ${def.key.removePrefix("pump_").removeSuffix("_off_delay")} Off Delay"
            else -> lcdParameterName(def)
        }
    }

    /** Controller menu breadcrumb path for a register (e.g. `Hardware › 4. Control › Sleep`). */
    fun menuPathForKey(key: String): String {
        if (key in systemRegisterKeys) return "Menu › 1. System"
        for (cat in MenuCategory.entries) {
            for (page in cat.pages) {
                if (key in page.registerKeys) {
                    return if (cat.pages.size == 1) {
                        "Hardware › ${cat.index}. ${cat.title}"
                    } else {
                        "Hardware › ${cat.index}. ${cat.title} › ${page.title}"
                    }
                }
            }
        }
        return "BPC-02 Live Telemetry"
    }

    /** Official English Control Status label (Word 4173). */
    fun controlStatusLabel(status: Int?): String = when (status) {
        0 -> "Disable"
        1 -> "PID"
        2 -> "ON/OFF"
        else -> "Unknown"
    }

    /** Official English Wiring Mode label (Word 4170). */
    fun wiringModeLabel(mode: Int?): String = when (mode) {
        1 -> "Mode 1"
        2 -> "Mode 2"
        3 -> "Mode 3"
        else -> "Mode —"
    }

    /** Station Mode title displaying exact pump and drive counts (`Mode X — تعداد پمپ N / تعداد درایو D`). */
    fun wiringModeArchitecture(mode: Int?, pumpCount: Int? = null, driveCount: Int? = null): String {
        val modePart = wiringModeLabel(mode)
        if (pumpCount == null && driveCount == null) return modePart
        val pTxt = pumpCount?.toString() ?: "—"
        val dTxt = driveCount?.toString() ?: "—"
        return "$modePart — تعداد پمپ: $pTxt / تعداد درایو: $dTxt"
    }

    fun driverCountLabel(count: Int?): String = when (count) {
        null -> "—"
        0 -> "0 Drives (DOL)"
        1 -> "1 Drive"
        else -> "$count Drives"
    }

    /** Official English enum option label matching the BPC-02 LCD screen & `BPC-02.pdf`. */
    fun enumOptionLabel(def: RegisterDefinition, raw: Int): String = when {
        def.key == Bpc02RegisterMap.CONTROL_STATUS -> controlStatusLabel(raw)
        def.key == "wiring_mode" -> wiringModeLabel(raw)
        def.key == "jockey_type" -> when (raw) {
            0 -> "Disable"
            1 -> "On"
            2 -> "Off"
            else -> "Unknown ($raw)"
        }
        def.key == "floater_type" -> when (raw) {
            0 -> "Disable"
            1 -> "Internal"
            2 -> "External"
            else -> "Unknown ($raw)"
        }
        def.key == "control_phase_type" -> when (raw) {
            1 -> "1P"
            3 -> "3P"
            else -> "Unknown ($raw)"
        }
        def.key == "pressure_sensor_output_type" -> when (raw) {
            2 -> "4-20mA"
            3 -> "0-20mA"
            4 -> "0-5V"
            5 -> "0-10V"
            6 -> "2-10V"
            else -> "Unknown ($raw)"
        }
        def.key == "pressure_sensor_input_type" -> when (raw) {
            0 -> "0-6Bar"
            1 -> "0-10Bar"
            2 -> "0-16Bar"
            3 -> "0-25Bar"
            4 -> "0-40Bar"
            5 -> "0-60Bar"
            10 -> "PS"
            else -> "Unknown ($raw)"
        }
        def.key == "control_direction" -> when (raw) {
            0 -> "Down"
            1 -> "Up"
            else -> "Unknown ($raw)"
        }
        def.key == "ao1_type" || def.key == "ao2_type" -> when (raw) {
            0 -> "Pump Speed 1"
            1 -> "Pump Speed 2"
            else -> "Unknown ($raw)"
        }
        def.key == Bpc02RegisterMap.MULTI_FUNCTION_REGISTER -> when (raw) {
            0 -> "Idle"
            25 -> "Reset Pump Error"
            else -> "Cmd $raw"
        }
        def.key.startsWith("mfi_") && def.key.endsWith("_type") -> when (raw) {
            0 -> "Disable"
            1 -> "PS"
            2 -> "TS"
            3 -> "MP"
            4 -> "EMG"
            5 -> "CP"
            6 -> "FL"
            7 -> "PR5"
            8 -> "PR6"
            9 -> "Reset"
            10 -> "Fix"
            else -> "Unknown ($raw)"
        }
        def.key.startsWith("mfo_") && def.key.endsWith("_type") -> when (raw) {
            0 -> "Disable"
            1 -> "MC3"
            2 -> "DC4"
            3 -> "MC4"
            4 -> "Fan"
            5 -> "Alarm"
            6 -> "E.Valve"
            7 -> "P.Run"
            8 -> "Ready"
            9 -> "Filler"
            10 -> "P1_ERR"
            11 -> "P2_ERR"
            12 -> "P3_ERR"
            13 -> "P4_ERR"
            else -> "Unknown ($raw)"
        }
        def.key == "reserve_auto_type" -> if (raw == 0) "NC" else "NO"
        def.enumLabels == mapOf(0 to "Disabled", 1 to "Enabled") -> if (raw == 1) "Enable" else "Disable"
        def.enumLabels == mapOf(0 to "OFF", 1 to "ON") -> if (raw == 1) "ON" else "OFF"
        def.enumLabels == mapOf(0 to "Normal", 1 to "Failed") -> if (raw == 1) "Failed" else "Normal"
        def.enumLabels == mapOf(0 to "Not connected", 1 to "Connected") -> if (raw == 1) "Connected" else "Open"
        else -> def.enumLabels[raw] ?: raw.toString()
    }

    /** Formats any BPC-02 register value using official English catalog units (`Bar`, `Hz`, `Sec`, `Min`, `Hour`, `V`, `%`, `KΩ`). */
    fun formatValue(def: RegisterDefinition, raw: Int): String = when {
        def.key == "wiring_mode" -> wiringModeLabel(raw)
        def.key == "number_of_pump" -> "$raw"
        def.key == "number_of_variable_pump" -> "$raw"
        def.enumLabels.isNotEmpty() -> enumOptionLabel(def, raw)
        def.unit == "bar" && def.scale != null -> String.format(Locale.US, "%.2f Bar", raw * def.scale)
        def.unit == "Hz" && def.scale != null -> {
            val dec = if (def.scale >= 0.1) 1 else 2
            String.format(Locale.US, "%.${dec}f Hz", raw * def.scale)
        }
        def.unit == "V" -> "$raw V"
        def.unit == "%" -> "$raw%"
        def.unit == "kΩ" -> "$raw KΩ"
        def.unit == "h" -> "$raw Hour"
        def.unit == "min" -> "$raw Min"
        def.unit == "s" && def.scale != null -> when {
            def.scale < 0.1 -> String.format(Locale.US, "%.2f Sec", raw * def.scale)
            def.scale < 1.0 -> String.format(Locale.US, "%.1f Sec", raw * def.scale)
            else -> "$raw Sec"
        }
        def.scale != null && def.scale < 0.001 -> String.format(Locale.US, "%.4f", raw * def.scale)
        else -> def.format(raw)
    }

    /** Formats allowed numeric/enum range in official English units for help cards & edit dialogs. */
    fun formatRange(def: RegisterDefinition): String? {
        if (def.enumLabels.isNotEmpty()) {
            return def.enumLabels.keys.sorted().joinToString(" / ") { enumOptionLabel(def, it) }
        }
        val min = def.minValue ?: return null
        val max = def.maxValue ?: return null
        return "${formatValue(def, min)} – ${formatValue(def, max)}"
    }

    /**
     * Official English alarm title (matching `BPC-02.pdf` page 23 Fault Finding table) paired with
     * a clear Persian user explanation of the cause and recommended check.
     */
    fun alarmTitleAndPersianDetail(alarm: Alarm): Pair<String, String> {
        SystemErrorBit.entries.firstOrNull { it.label == alarm.label }?.let { bit ->
            return errorBitEnglishTitle(bit) to errorBitPersianHint(bit)
        }
        SystemErrorBit2.entries.firstOrNull { it.label == alarm.label }?.let { bit ->
            return errorBit2EnglishTitle(bit) to errorBit2PersianHint(bit)
        }
        if (alarm.label == "Drive fail") {
            return "Drive Error" to "اینورتر (درایو) خطا گزارش کرده است؛ کد خطای نمایشگر درایو و ترمینال رله فالت را بررسی کنید."
        }
        if (alarm.label.startsWith("Pump ") && alarm.label.endsWith(" fail")) {
            val num = alarm.label.removePrefix("Pump ").removeSuffix(" fail")
            return "Pump $num Error" to "بی‌متال یا کلید حرارتی پمپ $num عمل کرده است؛ جریان موتور و ورودی PR$num را بررسی کنید."
        }
        if (alarm.label.startsWith("Undocumented error")) {
            return "Controller Fault" to "یک خطای حفاظتی روی کنترلر فعال شده است؛ صفحه Errors روی نمایشگر دستگاه را بررسی کنید."
        }
        return alarm.label to alarm.detail
    }

    fun errorBitEnglishTitle(bit: SystemErrorBit): String = when (bit) {
        SystemErrorBit.FLOATER -> "Floater Error"
        SystemErrorBit.MAX_PRESSURE -> "Maximum Pressure Error"
        SystemErrorBit.PRESSURE_SENSOR_DISCONNECTED -> "Disconnected Sensor"
        SystemErrorBit.PHASE_SENSITIVITY -> "Sensitivity Error"
        SystemErrorBit.PHASE_MAX_VOLTAGE -> "Maximum Voltage"
        SystemErrorBit.PHASE_MIN_VOLTAGE -> "Minimum Voltage"
        SystemErrorBit.PHASE_SEQUENCE -> "Sequence Error"
        SystemErrorBit.DRIVE -> "Drive Error"
        SystemErrorBit.PUMP1 -> "Pump 1 Error"
        SystemErrorBit.PUMP2 -> "Pump 2 Error"
        SystemErrorBit.PUMP3 -> "Pump 3 Error"
        SystemErrorBit.PUMP4 -> "Pump 4 Error"
        SystemErrorBit.PUMP5 -> "Pump 5 Error"
        SystemErrorBit.PUMP6 -> "Pump 6 Error"
        SystemErrorBit.ALL_PUMP -> "All Pump Fault"
        SystemErrorBit.SYSTEM_STOP_BY_USER -> "System Stopped (User)"
    }

    private fun errorBitPersianHint(bit: SystemErrorBit): String = when (bit) {
        SystemErrorBit.FLOATER -> "سطح آب مخزن ذخیره از حد مجاز کمتر شده است؛ منبع آب و فلوتر را بررسی کنید."
        SystemErrorBit.MAX_PRESSURE -> "فشار آب از حد مجاز Max Pressure بالاتر رفته است؛ با کاهش فشار یا مصرف مجدد برطرف می‌شود."
        SystemErrorBit.PRESSURE_SENSOR_DISCONNECTED -> "ارتباط سنسور فشار با کنترلر قطع شده است؛ کابل و اتصالات سنسور را بررسی کنید."
        SystemErrorBit.PHASE_SENSITIVITY -> "اختلاف ولتاژ بین فازهای R، S و T بیش از حد مجاز تنظیم‌شده در Control Phase است."
        SystemErrorBit.PHASE_MAX_VOLTAGE -> "ولتاژ برق ورودی از سقف مجاز (Max Voltage) بالاتر رفته است."
        SystemErrorBit.PHASE_MIN_VOLTAGE -> "ولتاژ برق ورودی از حداقل مجاز (Min Voltage) پایین‌تر آمده است."
        SystemErrorBit.PHASE_SEQUENCE -> "ترتیب فازهای برق ورودی تابلو (R-S-T) جابه‌جا شده است."
        SystemErrorBit.DRIVE -> "درایو خطا داده است؛ کد خطای روی نمایشگر درایو و سیم‌بندی ترمینال فالت درایو را بررسی کنید."
        SystemErrorBit.PUMP1 -> "بی‌متال یا کلید حرارتی پمپ ۱ عمل کرده است؛ جریان موتور و ورودی PR1 را بررسی کنید."
        SystemErrorBit.PUMP2 -> "بی‌متال یا کلید حرارتی پمپ ۲ عمل کرده است؛ جریان موتور و ورودی PR2 را بررسی کنید."
        SystemErrorBit.PUMP3 -> "بی‌متال یا کلید حرارتی پمپ ۳ عمل کرده است؛ جریان موتور و ورودی PR3 را بررسی کنید."
        SystemErrorBit.PUMP4 -> "بی‌متال یا کلید حرارتی پمپ ۴ عمل کرده است؛ جریان موتور و ورودی PR4 را بررسی کنید."
        SystemErrorBit.PUMP5 -> "مدار حفاظتی یا بی‌متال پمپ ۵ عمل کرده است؛ موتور و کلید حرارتی پمپ ۵ را بررسی کنید."
        SystemErrorBit.PUMP6 -> "مدار حفاظتی یا بی‌متال پمپ ۶ عمل کرده است؛ موتور و کلید حرارتی پمپ ۶ را بررسی کنید."
        SystemErrorBit.ALL_PUMP -> "همه پمپ‌های موجود در سیستم دچار خطا شده‌اند و ایستگاه متوقف است."
        SystemErrorBit.SYSTEM_STOP_BY_USER -> "سیستم توسط کاربر روی حالت خاموش (Disable) قرار گرفته است."
    }

    fun errorBit2EnglishTitle(bit: SystemErrorBit2): String = when (bit) {
        SystemErrorBit2.MODE3_MAIN_PUMP -> "Mode 3 Main Pump Error"
        SystemErrorBit2.MODE3_DRIVE -> "Mode 3 Drive Error"
        SystemErrorBit2.FULL_LOAD -> "Full Load Error"
        SystemErrorBit2.SYSTEM_STOPPED -> "System Stopped"
        SystemErrorBit2.EMG_STOPPED -> "EMG Stopped"
        SystemErrorBit2.BATTERY_VOLTAGE -> "Battery Voltage Error"
    }

    private fun errorBit2PersianHint(bit: SystemErrorBit2): String = when (bit) {
        SystemErrorBit2.MODE3_MAIN_PUMP -> "خطا در کنتاکتور یا پمپ اصلی هنگام کارکرد در Mode 3 رخ داده است."
        SystemErrorBit2.MODE3_DRIVE -> "خطا در درایو هنگام کارکرد در Mode 3 رخ داده است."
        SystemErrorBit2.FULL_LOAD -> "فشار در تمام‌بار به حد حفاظتی نرسیده است؛ مخزن آب، شیرهای مکش/دهش، هواگرفتگی، جهت چرخش پمپ‌ها یا ترکیدگی لوله بررسی شود."
        SystemErrorBit2.SYSTEM_STOPPED -> "کنترلر در حالت توقف (Disable) است؛ برای شروع به کار، Control Status را روی PID یا ON/OFF بگذارید."
        SystemErrorBit2.EMG_STOPPED -> "شستی توقف اضطراری (EMG) تحریک شده است؛ پس از رفع خطر آن را آزاد کنید."
        SystemErrorBit2.BATTERY_VOLTAGE -> "ولتاژ باتری داخلی ساعت و حافظه کنترلر ضعیف شده و نیاز به بررسی یا تعویض دارد."
    }

    /**
     * Returns the complete help card metadata (English official name + clear Persian user
     * explanation from `BPC-02.pdf`) for any BPC-02 [RegisterDefinition].
     */
    fun helpFor(def: RegisterDefinition): ParameterHelp = ParameterHelp(
        key = def.key,
        englishName = lcdParameterName(def),
        fullEnglishName = fullParameterName(def),
        menuPath = menuPathForKey(def.key),
        helpFa = persianHelpForKey(def.key),
        rangeHint = formatRange(def),
        isWritable = def.isWritable,
    )

    /**
     * Help entries for key non-register / composite UI concepts (e.g. Mode, Schematic, Voltages)
     * so every `?` button across Dashboard, Pumps & I/O, and Settings shares one unified model.
     */
    fun conceptHelp(topicKey: String): ParameterHelp = when (topicKey) {
        "system_pressure" -> helpFor(Bpc02RegisterMap.byKey(Bpc02RegisterMap.SYSTEM_PRESSURE))
        "setpoint" -> helpFor(Bpc02RegisterMap.pressureSetpointDef)
        "wiring_mode" -> helpFor(Bpc02RegisterMap.wiringModeDef)
        "control_status" -> helpFor(Bpc02RegisterMap.controlStatusDef)
        "number_of_pump" -> helpFor(Bpc02RegisterMap.numberOfPumpDef)
        "number_of_variable_pump" -> helpFor(Bpc02RegisterMap.numberOfVariablePumpDef)
        "drive_frequency" -> ParameterHelp(
            key = topicKey,
            englishName = "Drive FRQ",
            fullEnglishName = "Drive Output Frequency",
            menuPath = "Home Page › Drive",
            helpFa = "فرکانس لحظه‌ای خروجی اینورتر (بر حسب هرتز) است که سرعت چرخش پمپ دورمتغیر را مشخص می‌کند. هرچه مصرف آب بیشتر شود، کنترلر این فرکانس را بالا می‌برد تا فشار خط ثابت بماند.",
            rangeHint = "0.00 – 50.00 Hz",
        )
        "mains_frequency" -> ParameterHelp(
            key = topicKey,
            englishName = "Mains FRQ",
            fullEnglishName = "Mains Supply Frequency",
            menuPath = "Home Page › Voltmeter",
            helpFa = "فرکانس برق ورودی شبکه شهر است که به‌صورت زنده توسط کنترلر اندازه‌گیری می‌شود (در حالت استاندارد حدود ۵۰ هرتز است).",
            rangeHint = "50 Hz / 60 Hz",
        )
        "floater_level" -> ParameterHelp(
            key = topicKey,
            englishName = "Floater (H / L)",
            fullEnglishName = "Water Tank Floater Level",
            menuPath = "Home Page › Water Tank",
            helpFa = "وضعیت الکترودهای سطح بالا (H) و سطح پایین (L) داخل مخزن ذخیره آب را نشان می‌دهد. اگر آب از سطح پایین (L) کمتر شود، کنترلر برای جلوگیری از خشک کار کردن و سوختن پمپ‌ها، خطای Floater Error داده و ایستگاه را متوقف می‌کند.",
            rangeHint = "H = High Level / L = Low Level",
        )
        "reset_pump_error" -> helpFor(Bpc02RegisterMap.multiFunctionRegisterDef)
        "dr_status" -> ParameterHelp(
            key = Bpc02RegisterMap.DR_STATUS,
            englishName = "Drive Input (DR)",
            fullEnglishName = "Drive Feedback Digital Input (Coil 10)",
            menuPath = "Menu › 2. Input",
            helpFa = "ورودی دیجیتال فیدبک درایو (ترمینال Drive روی صفحه Inputs کنترلر، آدرس Coil 10، فقط‌خواندنی) است که سیگنال لحظه‌ای آماده/فعال بودن اینورتر را به کنترلر گزارش می‌دهد. این ورودی با «Drive Status» (آدرس Coil 4096 که کلید فعال/غیرفعال سرویس درایو است) متفاوت است.",
            rangeHint = "ON / OFF (Coil 10 — Read-Only)",
        )
        "mfi_overview" -> ParameterHelp(
            key = topicKey,
            englishName = "MFI1–MFI4 Options",
            fullEnglishName = "Multi-Function Digital Inputs (Words 4180–4183)",
            menuPath = "Hardware › 5. MFx › MFI",
            helpFa = "ورودی‌های دیجیتال چندمنظوره کنترلر هستند و هرکدام می‌توانند روی یکی از گزینه‌های Disable، PS (پرشر سوئیچ)، TS، MP (حداکثر فشار)، EMG (توقف اضطراری)، CP (کنترل فاز خارجی)، FL (فلوتر خارجی)، PR5/PR6 (فیدبک پمپ ۵ و ۶)، Reset یا Fix تنظیم شوند. نوع عملکرد MFIها در ModBus فقط‌خواندنی است و از منوی روی نمایشگر کنترلر (Hardware › 5. MFx) ویرایش می‌شود.",
            rangeHint = mfiAllowedOptions.joinToString(" / "),
        )
        "mfo_overview" -> ParameterHelp(
            key = topicKey,
            englishName = "MFO1–MFO4 (OUT 7–10)",
            fullEnglishName = "Multi-Function Relay Outputs (Words 4184–4187)",
            menuPath = "Hardware › 5. MFx › MFO",
            helpFa = "چهار خروجی رله آخر کنترلر (OUT 7 تا OUT 10، معادل Coils 6 تا 9) به رله‌های چندمنظوره MFO1 تا MFO4 اختصاص دارند و می‌توانند روی گزینه‌های Disable، MC3، DC4، MC4، Fan (فن تابلو)، Alarm (آلارم خطا)، E.Valve (شیر برقی)، P.Run (در حال کار)، Ready (آماده به کار)، Filler یا خطای پمپ‌های ۱ تا ۴ تنظیم شوند. نوع عملکرد MFOها از طریق ModBus فقط‌خواندنی است و از روی نمایشگر کنترلر (Hardware › 5. MFx) قابل تغییر است.",
            rangeHint = mfoAllowedOptions.joinToString(" / "),
        )
        "reset_to_factory" -> ParameterHelp(
            key = topicKey,
            englishName = "Reset to Factory",
            fullEnglishName = "Factory Default Reset (Keypad Only)",
            menuPath = "Hardware › 8. Reset to Factory",
            helpFa = "بازنشانی کامل تنظیمات کنترلر به حالت کارخانه است. برای حفظ ایمنی ایستگاه و جلوگیری از پاک‌شدن ناخواسته تنظیمات از راه دور، این دستور در پروتکل ModBus وجود ندارد و تنها از روی صفحه‌کلید خود کنترلر قابل اجراست.",
        )
        "config_transfer" -> ParameterHelp(
            key = topicKey,
            englishName = "Import / Export Config",
            fullEnglishName = "Configuration Import & Export (JSON)",
            menuPath = "Settings › Configuration Backup",
            helpFa = "با خروجی گرفتن (Export)، تمام تنظیمات کنترلر در یک فایل JSON ذخیره می‌شود تا برای پشتیبان‌گیری یا انتقال سریع به پروژه‌ها و کنترلرهای مشابه استفاده شود. هنگام بارگذاری (Import)، فایل قبل از اعمال کاملاً بررسی و اعتبارسنجی می‌شود و فقط پس از تایید شما اعمال می‌گردد.",
        )
        else -> ParameterHelp(
            key = topicKey,
            englishName = topicKey,
            fullEnglishName = topicKey,
            menuPath = "BPC-02",
            helpFa = persianHelpForKey(topicKey),
        )
    }

    /**
     * Concise, accurate, easy-to-understand Persian explanations for ALL 82 hardware configuration
     * registers and key live registers, sourced directly from `BPC-02.pdf` pages 8–24.
     */
    fun persianHelpForKey(key: String): String = when (key) {
        // ---- 1. Control Phase (BPC-02.pdf p.13) ----
        "control_phase_activation" ->
            "کنترل فاز داخلی دستگاه را فعال (Enable) یا غیرفعال (Disable) می‌کند. در حالت فعال، کنترلر از پمپ‌ها در برابر قطع فاز، جابه‌جایی فاز و نوسان ولتاژ محافظت می‌کند."
        "control_phase_type" ->
            "مشخص می‌کند که سیستم برق ورودی و حفاظت کنترل فاز برای برق تک‌فاز (1P) تنظیم شود یا سه‌فاز (3P)."
        "control_phase_min_voltage" ->
            "حداقل ولتاژ مجاز برای هر فاز است. اگر ولتاژ برق ورودی از این مقدار کمتر شود، پس از زمان تاخیر، خطای افت ولتاژ (Minimum Voltage) اعلام و برای محافظت از موتورها سیستم متوقف می‌شود."
        "control_phase_max_voltage" ->
            "حداکثر ولتاژ مجاز برای هر فاز است. اگر ولتاژ برق ورودی از این حد بالاتر برود، کنترلر خطای افزایش ولتاژ (Maximum Voltage) داده و از سوختن پمپ‌ها و درایو جلوگیری می‌کند."
        "control_phase_delay" ->
            "زمان تاخیر برای اعلام خطاهای کنترل فاز است. اگر نوسان ولتاژ خیلی کوتاه باشد و از این زمان کمتر طول بکشد، سیستم بی‌دلیل متوقف نمی‌شود."
        "control_phase_sensitivity" ->
            "حداکثر درصد اختلاف مجاز ولتاژ بین فازها (عدم تقارن فاز) را مشخص می‌کند. اگر اختلاف فازها از این درصد بیشتر شود، خطای Sensitivity Error فعال می‌شود."

        // ---- 2. Floater (BPC-02.pdf p.14) ----
        "floater_type" ->
            "نحوه تشخیص سطح آب مخزن ذخیره را مشخص می‌کند: Disable (بدون کنترل سطح)، Internal (اتصال مستقیم الکترودها به کنترلر)، یا External (استفاده از رله سطح‌سنج خارجی از طریق ورودی MFI)."
        "floater_trigger_level" ->
            "حساسیت فلوتر داخلی کنترلر برای سختی‌های مختلف آب است. در مناطقی که آب سختی (رسانایی) بیشتری دارد، کاهش این مقدار به تشخیص دقیق‌تر سطح آب کمک می‌کند."
        "floater_delay" ->
            "زمان تاخیر برای اعلام خطای کم‌آبی مخزن است. به‌عنوان مثال اگر روی ۵ ثانیه باشد، ۵ ثانیه پس از بی‌آب شدن مخزن خطای Floater Error ثبت و پمپ‌ها متوقف می‌شوند."

        // ---- 3. Sensor (BPC-02.pdf p.14) ----
        "pressure_sensor_input_type" ->
            "بازه اندازه‌گیری سنسور فشار نصب‌شده روی کلکتور را مشخص می‌کند (مثل 0-10Bar یا 0-16Bar). در پروژه‌هایی که به‌جای ترنسمیتر آنالوگ از پرشر سوئیچ استفاده می‌شود، گزینه PS انتخاب می‌شود."
        "pressure_sensor_output_type" ->
            "نوع سیگنال الکتریکی خروجی سنسور فشار متصل به کنترلر را تعیین می‌کند (رایج‌ترین حالت جریان 4-20mA یا ولتاژ 0-10V است)."
        "max_pressure_value" ->
            "سقف فشار حفاظتی برای جلوگیری از آسیب به لوله‌ها و اتصالات است. طبق راهنمای دستگاه معمولاً ۰.۲ تا ۰.۵ بار بالاتر از فشار Setpoint تنظیم می‌شود؛ اگر فشار از این حد بگذرد سیستم متوقف می‌شود."
        "sensor_calibration_value" ->
            "ضریب کالیبراسیون سنسور فشار است. اگر بین عدد فشار روی کنترلر و گیج عقربه‌ای روی کلکتور اختلاف وجود داشته باشد، با تنظیم این عدد این اختلاف برطرف می‌شود."

        // ---- 4. Control — Pages 1..4 (BPC-02.pdf p.15–16, p.22) ----
        "number_of_pump" ->
            "تعداد کل پمپ‌های اصلی ایستگاه را مشخص می‌کند (تا ۴ پمپ در مد ۲ و ۳ و تا ۶ پمپ در مد ۱). شماتیک سیستم و مدیریت نوبت‌دهی بر اساس این عدد عمل می‌کند."
        "number_of_variable_pump" ->
            "تعداد اینورترها (درایوهای دورمتغیر) متصل به کنترلر را مشخص می‌کند. در مد ۱ تا ۲ درایو مجزا و در مد ۲ و ۳ یک درایو مشترک قابل استفاده است."
        "wiring_mode" ->
            "مد سیم‌بندی و نحوه همکاری پمپ‌ها با درایو را تعیین می‌کند: Mode 1 (درایو مستقل برای پمپ‌های اول و بقیه مستقیم)، Mode 2 (چنج‌اور پمپ اول با درایو و ورود مستقیم پمپ‌های کمکی)، Mode 3 (راه‌اندازی نرم همه پمپ‌ها با یک درایو و انتقال به برق شبکه)."
        Bpc02RegisterMap.DRIVE_ACTIVATION ->
            "وضعیت در مدار بودن درایو (اینورتر) را کنترل می‌کند. در صورت غیرفعال کردن (Disable)، درایو برای سرویس از مدار خارج می‌شود و کنترلر مطابق مد کاری عمل می‌کند."
        Bpc02RegisterMap.CONTROL_STATUS ->
            "حالت اصلی عملکرد کنترلر را تعیین می‌کند: Disable (توقف کامل سیستم)، PID (تثبیت دقیق فشار با تغییر سرعت درایو)، یا ON/OFF (کنترل پله‌ای روشن/خاموش برای پمپ‌های دورثابت یا آتش‌نشانی)."
        "control_direction" ->
            "جهت کنترل فشار را مشخص می‌کند: در حالت Up (استاندارد بوستر پمپ آبرسانی) با کاهش فشار دور پمپ بالا می‌رود تا به Setpoint برسد؛ حالت Down برای کاربردهای کاهش فشار یا مکش است."
        Bpc02RegisterMap.SYSTEM_PRESSURE_SETPOINT ->
            "فشار هدف و مطلوب شبکه آب ساختمان است (بر حسب بار). کنترلر سرعت درایو و تعداد پمپ‌های روشن را طوری تنظیم می‌کند که فشار خط روی این عدد ثابت بماند. طبق کاتالوگ باید حداقل ۱ بار کمتر از حداکثر هد پمپ تنظیم شود."

        // ---- 4. Control — Page 5: Add Remove Pump (BPC-02.pdf p.16) ----
        "constant_pump_percent" ->
            "تعیین می‌کند چه درصدی از ظرفیت پمپ کمکی بعدی مورد نیاز باشد تا وارد مدار شود (معمولاً حدود ۱۵٪). این تنظیم از روشن و خاموش شدن پیاپی پمپ دوم و نوسان فشار جلوگیری می‌کند."
        "add_delay" ->
            "زمان انتظار قبل از وارد کردن پمپ کمکی بعدی به مدار هنگام افزایش مصرف است (توصیه کاتالوگ حدود ۴ ثانیه) تا با افت‌های لحظه‌ای، پمپ اضافی بی‌دلیل روشن نشود."
        "remove_delay" ->
            "زمان انتظار قبل از خارج کردن پمپ کمکی هنگام کاهش مصرف است (توصیه کاتالوگ حدود ۱ ثانیه) تا از خاموش شدن عجولانه در کاهش مصرف‌های لحظه‌ای جلوگیری شود."
        "mode3_switch_time" ->
            "در Mode 3، زمان تاخیر جابه‌جایی پمپ از کنتاکتور درایو به کنتاکتور برق شبکه است تا اتصال کوتاه رخ ندهد. بسته به توان پمپ معمولاً بین ۳۵ تا ۵۰ میلی‌ثانیه تنظیم می‌شود."

        // ---- 4. Control — Page 6: Sleep (BPC-02.pdf p.17) ----
        "sleep_frequency" ->
            "فرکانسی است که در صورت کاهش مصرف و رسیدن دور درایو به آن یا پایین‌تر، کنترلر آماده رفتن به حالت خواب (Sleep) و خاموش کردن پمپ می‌شود. طبق کاتالوگ معمولاً ۲ هرتز بالاتر از Start Frq تنظیم می‌شود."
        "sleep_off_delay" ->
            "مدت زمانی است که شرایط حالت خواب باید پایدار بماند تا کنترلر پمپ را خاموش کرده و وارد حالت Sleep شود."
        "sleep_gap" ->
            "حداکثر نوسان مجاز فشار اطراف Setpoint برای تایید پایدار بودن سیستم جهت ورود به حالت خواب است (توصیه کاتالوگ حدود ۰.۲۰ بار)."

        // ---- 4. Control — Page 7: Wake Up (BPC-02.pdf p.17) ----
        "wakeup_differential" ->
            "مشخص می‌کند وقتی سیستم در حالت خواب است، فشار چقدر از Setpoint پایین‌تر بیاید تا پمپ دوباره بیدار شده و روشن شود (مقدار مناسب طبق کاتالوگ حدود ۰.۵۰ بار است)."
        "wakeup_on_delay" ->
            "پس از روشن شدن پمپ و رسیدن به فرکانس شروع (Start Frq)، پس از این زمان تاخیر کنترلر PID فعال می‌شود. معمولاً بین ۳ تا ۵ ثانیه متناسب با زمان شتاب‌گیری (ACC) درایو تنظیم می‌شود."
        "start_frequency" ->
            "فرکانس شروع به کار سریع درایو هنگام روشن شدن پمپ است تا از افت فشار اولیه در شبکه جلوگیری شود. این عدد فرکانسی است که در حالت کلکتور بسته، فشار Setpoint را تامین می‌کند."
        "stop_frequency" ->
            "فرکانسی است که هنگام خروج پمپ از مدار، به‌جای کاهش دور تا صفر هرتز، پمپ در آن قطع می‌شود (معمولاً بین ۲۰ تا ۲۵ هرتز جایی که پمپ دیگر فشار موثری تولید نمی‌کند)."

        // ---- 4. Control — Pages 8..13 (BPC-02.pdf p.18–19) ----
        "off_changing_activation" ->
            "قابلیت جابه‌جایی نوبت پمپ‌ها (Change Over) پس از هر بار خاموش و روشن شدن سیستم را فعال یا غیرفعال می‌کند تا استهلاک به‌طور مساوی بین پمپ‌ها تقسیم شود."
        "jockey_type" ->
            "عملکرد پمپ اول به‌عنوان پمپ پیشرو (جاکی) را تعیین می‌کند: Disable (غیرفعال)، On (پمپ پیشرو پس از ورود پمپ‌های بعدی هم روشن می‌ماند)، Off (با ورود پمپ‌های اصلی، پمپ پیشرو خاموش می‌شود)."
        "time_changing_activation" ->
            "تغییر نوبت خودکار پمپ‌ها بر اساس زمان کارکرد را فعال یا غیرفعال می‌کند. در مصرف‌های پیوسته باعث می‌شود یک پمپ به‌طور مداوم زیر بار نماند."
        "time_changing_time" ->
            "مدت زمان کارکرد پمپ (بر حسب دقیقه) که پس از رسیدن به آن، کنترلر پمپ فعال را خاموش کرده و پمپ بعدی را جایگزین می‌کند."
        "reserve_auto_activation" ->
            "در صورت فعال بودن، اگر یکی از پمپ‌ها دچار خطا شود، کنترلر به‌طور خودکار پمپ سالم دیگری را جایگزین آن می‌کند و خطای پمپ معیوب را نمایش می‌دهد."
        "reserve_auto_type" ->
            "نوع تیغه کمکی کنتاکتور برای تشخیص سلامت پمپ را مشخص می‌کند: NC (تیغه بسته) یا NO (تیغه باز). طبق کاتالوگ استفاده از تیغه NO توصیه می‌شود."
        "full_load_status" ->
            "حفاظت کارکرد تمام‌بار (Full Load) را فعال یا غیرفعال می‌کند. این حفاظت در زمان بی‌آبی، هوا گرفتن پمپ، بسته بودن شیرها، برعکس بودن دور موتور یا ترکیدگی لوله از سوختن پمپ‌ها جلوگیری می‌کند."
        "full_load_pressure" ->
            "فشار حفاظتی تمام‌بار است (معمولاً حدود ۱.۵ بار پایین‌تر از Setpoint). اگر آخرین پمپ با حداکثر دور کار کند اما فشار به این عدد نرسد، شمارنده حفاظت فعال می‌شود."
        "full_load_time" ->
            "مدت زمانی است که اگر سیستم در حالت تمام‌بار نتواند به فشار حفاظتی برسد، کنترلر خطای Full Load Error داده و ایستگاه را متوقف می‌کند (معمولاً حدود ۴۰ ثانیه)."
        "auto_service_off_time" ->
            "حداکثر مدت خاموش ماندن یک پمپ (بر حسب ساعت) است. اگر پمپی بیش از این مدت کار نکند، کنترلر برای جلوگیری از رسوب و گیرپاژ آن را به‌صورت لحظه‌ای روشن می‌کند."
        "auto_service_on_time" ->
            "مدت زمانی است که پمپ در چرخه اتو سرویس به‌صورت لحظه‌ای روشن می‌ماند (بر حسب ثانیه) تا پروانه آن بچرخد و از قفل شدن جلوگیری شود."
        "auto_service_interval" ->
            "فاصله زمانی (بر حسب دقیقه) بین روشن شدن پمپ‌ها در حالت اتو سرویس است تا اگر زمان سرویس چند پمپ هم‌زمان شد، با فاصله از هم روشن شوند."
        "pid_p_value" ->
            "ضریب تناسبی (Proportional) در کنترلر PID است و سرعت واکنش سیستم به تغییر فشار را تعیین می‌کند. مقدار پیش‌فرض کاتالوگ (250) برای اکثر پروژه‌ها مناسب است."
        "pid_i_value" ->
            "ضریب انتگرالی (Integral) در کنترلر PID است و خطای ماندگار فشار نسبت به Setpoint را حذف می‌کند. مقدار پیش‌فرض کاتالوگ (80) برای بیشتر پروژه‌ها ایده‌آل است."
        "pid_d_value" ->
            "ضریب مشتقی (Derivative) در کنترلر PID است و به میرا کردن نوسانات سریع فشار کمک می‌کند. طبق کاتالوگ مقدار پیش‌فرض (0) در ۹۵٪ پروژه‌ها بهترین نتیجه را می‌دهد."

        // ---- 5. MFx (BPC-02.pdf p.19–20) ----
        "ao1_type", "ao2_type" ->
            "مشخص می‌کند این خروجی آنالوگ (0–10V) فرمان سرعت کدام درایو (Pump Speed 1 یا Pump Speed 2) را ارسال کند. در صورت خرابی خروجی اول، می‌توان از خروجی دوم به‌عنوان جایگزین استفاده کرد."
        "ao1_gain", "ao2_gain" ->
            "ضریب تنظیم دقیق (Gain) ولتاژ خروجی آنالوگ برای هم‌خوانی دقیق فرکانس فرمان کنترلر با فرکانس خوانده‌شده روی اینورتر است."

        // ---- 6. Serial Number, Network & System (BPC-02.pdf p.12, p.21) ----
        "serial_number" ->
            "شماره سریال ثبت‌شده برای دستگاه است که در بالای صفحه خطاهای کنترلر نمایش داده می‌شود و به شناسایی دستگاه در پروژه کمک می‌کند."
        "modbus_address" ->
            "آدرس شبکه RS-485 کنترلر در پروتکل ModBus-RTU (با تنظیمات 9600-8-N-1) برای ارتباط با مبدل، اپلیکیشن یا سیستم مدیریت ساختمان (BMS) است."
        "lcd_contrast" ->
            "شدت وضوح و کنتراست صفحه نمایشگر LCD کنترلر است و در بازه ۰ تا ۲۰ تنظیم می‌شود."

        // ---- Live Telemetry & Command ----
        Bpc02RegisterMap.SYSTEM_PRESSURE ->
            "فشار لحظه‌ای آب در کلکتور خروجی ایستگاه است که به‌طور پیوسته از سنسور فشار خوانده می‌شود."
        Bpc02RegisterMap.DR_STATUS ->
            "ورودی دیجیتال فیدبک درایو (آدرس Coil 10، فقط‌خواندنی، نمایش‌داده‌شده با عنوان Drive در صفحه Inputs کنترلر) است و وضعیت سیگنال برگشتی از اینورتر را نشان می‌دهد."
        Bpc02RegisterMap.MULTI_FUNCTION_REGISTER ->
            "فرمان پاک‌سازی خطاهای ثبت‌شده پمپ‌ها را به کنترلر ارسال می‌کند. پیش از رفع خطا، از برطرف شدن عامل اصلی خطا (مثل بی‌متال یا منبع آب) مطمئن شوید."

        else -> when {
            key.startsWith("pump_") && key.endsWith("_activation") -> {
                val n = key.removePrefix("pump_").removeSuffix("_activation")
                "پمپ $n را در مدار قرار می‌دهد (Enable) یا برای تعمیر و سرویس از مدار خارج می‌کند (Disable). در حالت غیرفعال، حرف S روی نمایشگر کنترلر نمایش داده می‌شود و سایر پمپ‌ها به کار ادامه می‌دهند."
            }
            key.startsWith("pump_") && key.endsWith("_on_pressure") -> {
                val n = key.removePrefix("pump_").removeSuffix("_on_pressure")
                "در حالت کنترل پله‌ای (ON/OFF)، اگر فشار شبکه به این مقدار یا کمتر برسد، پمپ $n وارد مدار می‌شود."
            }
            key.startsWith("pump_") && key.endsWith("_on_delay") -> {
                val n = key.removePrefix("pump_").removeSuffix("_on_delay")
                "زمان تاخیر برای روشن شدن پمپ $n پس از رسیدن فشار به آستانه روشن شدن است تا از روشن شدن ناگهانی در نوسانات کوتاه جلوگیری شود."
            }
            key.startsWith("pump_") && key.endsWith("_off_pressure") -> {
                val n = key.removePrefix("pump_").removeSuffix("_off_pressure")
                "در حالت کنترل پله‌ای (ON/OFF)، وقتی فشار به این مقدار یا بالاتر برسد، پمپ $n از مدار خارج می‌شود."
            }
            key.startsWith("pump_") && key.endsWith("_off_delay") -> {
                val n = key.removePrefix("pump_").removeSuffix("_off_delay")
                "زمان تاخیر برای خاموش شدن پمپ $n پس از رسیدن به فشار قطع است تا سیستم نرم‌تر و بدون ضربه قوچ متوقف شود."
            }
            key.startsWith("mfi_") && key.endsWith("_type") -> {
                val n = key.removePrefix("mfi_").removeSuffix("_type")
                "کاربرد ورودی دیجیتال چندمنظوره MFI$n را تعیین می‌کند؛ مثلاً پرشر سوئیچ (PS)، حداکثر فشار (MP)، قطع اضطراری (EMG)، کنترل فاز خارجی (CP)، فلوتر خارجی (FL) یا فیدبک کنتاکتور پمپ ۵ و ۶ (PR5/PR6)."
            }
            key.startsWith("mfo_") && key.endsWith("_type") -> {
                val n = key.removePrefix("mfo_").removeSuffix("_type")
                "کاربرد رله خروجی چندمنظوره MFO$n را مشخص می‌کند؛ مانند فرمان فن تابلو (Fan)، آلارم خطا (Alarm)، شیر برقی اتوسرویس (E.Valve)، کنتاکتورهای کمکی (MC3/MC4/DC4)، وضعیت کارکرد (P.Run) یا آماده به کار (Ready)."
            }
            else ->
                "این پارامتر بخشی از تنظیمات فنی کنترلر بوستر پمپ DGP BPC-02 است و عملکرد بخش مربوطه را در سیستم تنظیم می‌کند."
        }
    }
}
