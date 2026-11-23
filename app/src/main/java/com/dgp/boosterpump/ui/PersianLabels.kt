package com.dgp.boosterpump.ui

import com.dgp.boosterpump.controller.Alarm
import com.dgp.boosterpump.controller.ControllerModel
import com.dgp.boosterpump.controller.RunTime
import com.dgp.boosterpump.controller.SystemErrorBit
import com.dgp.boosterpump.controller.SystemErrorBit2
import com.dgp.boosterpump.controller.register.Bpc02RegisterMap
import com.dgp.boosterpump.controller.register.RegisterDefinition
import com.dgp.boosterpump.controller.register.VendorTable
import com.dgp.boosterpump.controller.register.Verification
import com.dgp.boosterpump.gateway.ConfigValidation
import com.dgp.boosterpump.gateway.DataSourceMode
import com.dgp.boosterpump.modbus.Framing
import com.dgp.boosterpump.modbus.ReadFunction
import com.dgp.boosterpump.simulation.SimulationScenario

/**
 * Centralized Persian (fa) localization for all industrial telemetry labels, alarms,
 * gateway settings, scenarios, and all 154 official DGP BPC02 registers.
 */
object PersianLabels {

    fun modeLabel(mode: DataSourceMode): String = when (mode) {
        DataSourceMode.LIVE -> "اتصال واقعی (مبدل ES 487)"
        DataSourceMode.SIMULATION -> "حالت شبیه‌سازی (بدون سخت‌افزار)"
    }

    fun modelLabel(model: ControllerModel): String = when (model) {
        ControllerModel.BPC02 -> "کنترلر DGP BPC02"
        ControllerModel.BPC04 -> "کنترلر DGP BPC04 (به زودی)"
    }

    fun modelNote(model: ControllerModel): String = when (model) {
        ControllerModel.BPC02 -> "نقشه کامل ۱۵۴ رجیستر مطابق فایل رسمی DGP (9600 8N1، توابع FC01/FC03/FC05/FC06، آفست 0)."
        ControllerModel.BPC04 -> "در دست توسعه — نیازمند نقشه رجیستر رسمی و تست سخت‌افزاری BPC04."
    }

    fun framingLabel(framing: Framing): String = when (framing) {
        Framing.MODBUS_TCP -> "Modbus TCP (MBAP)"
        Framing.RTU_OVER_TCP -> "RTU روی TCP (شفاف)"
    }

    fun framingDescription(framing: Framing): String = when (framing) {
        Framing.MODBUS_TCP -> "مبدل سمین ES 487 بسته‌های Modbus TCP/IP را به Modbus RTU تبدیل می‌کند (حالت پیشنهادی Server)."
        Framing.RTU_OVER_TCP -> "فریم خام RTU همراه با CRC مستقیماً روی سوکت TCP ارسال می‌شود (حالت Transparent)."
    }

    fun readFunctionLabel(fn: ReadFunction): String = when (fn) {
        ReadFunction.READ_COILS -> "FC01 — خواندن کویل‌ها (تاییدشده)"
        ReadFunction.READ_DISCRETE_INPUTS -> "FC02 — ورودی گسسته (عدم پشتیبانی در BPC02)"
        ReadFunction.READ_HOLDING_REGISTERS -> "FC03 — خواندن رجیستر نگهدارنده (تاییدشده)"
        ReadFunction.READ_INPUT_REGISTERS -> "FC04 — رجیستر ورودی (عدم پشتیبانی در BPC02)"
    }

    fun readFunctionShort(fn: ReadFunction): String = when (fn) {
        ReadFunction.READ_COILS -> "FC01 کویل"
        ReadFunction.READ_DISCRETE_INPUTS -> "FC02 گسسته"
        ReadFunction.READ_HOLDING_REGISTERS -> "FC03 هولدینگ"
        ReadFunction.READ_INPUT_REGISTERS -> "FC04 ورودی"
    }

    fun controlStatusShort(status: Int?): String = when (status) {
        0 -> "توقف (Disable)"
        1 -> "مد PID"
        2 -> "مد ON/OFF"
        else -> "نامشخص"
    }

    fun scenarioLabel(s: SimulationScenario): String = when (s) {
        SimulationScenario.NORMAL_DUTY -> "کارکرد عادی (۱ پمپ با درایو)"
        SimulationScenario.PEAK_DEMAND -> "اوج مصرف (۳ پمپ هم‌زمان)"
        SimulationScenario.FLOATER_LOW_ALARM -> "خطای کمبود آب مخزن (فلوتر)"
        SimulationScenario.PUMP_AND_DRIVE_FAULT -> "خطای درایو، پمپ ۲ و فشار/ولتاژ بالا"
        SimulationScenario.LINK_TIMEOUT -> "شبیه‌سازی قطع ارتباط (Timeout)"
    }

    fun scenarioDescription(s: SimulationScenario): String = when (s) {
        SimulationScenario.NORMAL_DUTY -> "فشار ~۴.۲ بار (هدف ۴.۵۰ بار)، پمپ ۱ در مدار با درایو ۳۸.۵ هرتز، شبکه سه‌فاز سالم."
        SimulationScenario.PEAK_DEMAND -> "مصرف بالا ~۳.۸۵ بار، پمپ‌های ۱ تا ۳ در مدار و درایو ۱ روی ۴۷.۵ هرتز."
        SimulationScenario.FLOATER_LOW_ALARM -> "فلوتر پایین و بالا قطع، بیت ۰ رجیستر ۸۰ (FLOATER_ERROR) فعال، پمپ‌ها متوقف."
        SimulationScenario.PUMP_AND_DRIVE_FAULT -> "خطای درایو، پمپ ۲، حداکثر فشار و حداکثر ولتاژ — قابل تست با ریست خطا (کد ۲۵)."
        SimulationScenario.LINK_TIMEOUT -> "عدم پاسخ‌دهی باس RS485 برای تست نمایش خطا و اتصال مجدد خودکار."
    }

    fun verificationLabel(v: Verification): String = when (v) {
        Verification.HARDWARE_VERIFIED -> "تایید سخت‌افزاری"
        Verification.DOCUMENTED -> "نقشه رسمی DGP"
        Verification.AMBIGUOUS -> "نیازمند بررسی"
    }

    fun tableLabel(table: VendorTable): String = when (table) {
        VendorTable.BIT -> "کویل (FC01)"
        VendorTable.WORD -> "ورد (FC03)"
    }

    fun errorBitTitle(bit: SystemErrorBit): String = when (bit) {
        SystemErrorBit.FLOATER -> "خطای فلوتر (کمبود آب مخزن)"
        SystemErrorBit.MAX_PRESSURE -> "خطای حداکثر فشار سیستم"
        SystemErrorBit.PRESSURE_SENSOR_DISCONNECTED -> "قطع شدن سنسور فشار"
        SystemErrorBit.PHASE_SENSITIVITY -> "خطای عدم تقارن / حساسیت فاز"
        SystemErrorBit.PHASE_MAX_VOLTAGE -> "خطای حداکثر ولتاژ کنترل فاز"
        SystemErrorBit.PHASE_MIN_VOLTAGE -> "خطای حداقل ولتاژ کنترل فاز"
        SystemErrorBit.PHASE_SEQUENCE -> "خطای جابه‌جایی توالی فازها (R/S/T)"
        SystemErrorBit.DRIVE -> "خطای درایو / اینورتر (DRIVE_ERROR)"
        SystemErrorBit.PUMP1 -> "خطای پمپ ۱"
        SystemErrorBit.PUMP2 -> "خطای پمپ ۲"
        SystemErrorBit.PUMP3 -> "خطای پمپ ۳"
        SystemErrorBit.PUMP4 -> "خطای پمپ ۴"
        SystemErrorBit.PUMP5 -> "خطای پمپ ۵"
        SystemErrorBit.PUMP6 -> "خطای پمپ ۶"
        SystemErrorBit.ALL_PUMP -> "خطای همه پمپ‌ها (ALL_PUMP_ERROR)"
        SystemErrorBit.SYSTEM_STOP_BY_USER -> "توقف سیستم توسط کاربر"
    }

    fun errorBitHint(bit: SystemErrorBit): String = when (bit) {
        SystemErrorBit.FLOATER -> "سطح آب منبع ذخیره از حد مجاز کمتر شده است؛ منبع و فلوتر بررسی شود."
        SystemErrorBit.MAX_PRESSURE -> "فشار کلکتور از سقف مجاز تنظیم‌شده فراتر رفته است."
        SystemErrorBit.PRESSURE_SENSOR_DISCONNECTED -> "کابل و اتصالات سنسور فشار به کنترلر بررسی شود."
        SystemErrorBit.PHASE_SENSITIVITY -> "اختلاف ولتاژ بین فازهای R، S و T بیش از حد مجاز است."
        SystemErrorBit.PHASE_MAX_VOLTAGE -> "ولتاژ ورودی شبکه از سقف مجاز بالاتر رفته است."
        SystemErrorBit.PHASE_MIN_VOLTAGE -> "ولتاژ ورودی شبکه از حداقل مجاز پایین‌تر آمده است."
        SystemErrorBit.PHASE_SEQUENCE -> "ترتیب فازهای ورودی تابلو (R-S-T) جابه‌جا شده است."
        SystemErrorBit.DRIVE -> "اینورتر خطا گزارش کرده است؛ کد خطای روی نمایشگر درایو بررسی شود."
        SystemErrorBit.PUMP1 -> "بی‌متال، کلید حرارتی و مدار حفاظتی پمپ ۱ بررسی شود."
        SystemErrorBit.PUMP2 -> "بی‌متال، کلید حرارتی و مدار حفاظتی پمپ ۲ بررسی شود."
        SystemErrorBit.PUMP3 -> "بی‌متال، کلید حرارتی و مدار حفاظتی پمپ ۳ بررسی شود."
        SystemErrorBit.PUMP4 -> "بی‌متال، کلید حرارتی و مدار حفاظتی پمپ ۴ بررسی شود."
        SystemErrorBit.PUMP5 -> "بی‌متال، کلید حرارتی و مدار حفاظتی پمپ ۵ بررسی شود."
        SystemErrorBit.PUMP6 -> "بی‌متال، کلید حرارتی و مدار حفاظتی پمپ ۶ بررسی شود."
        SystemErrorBit.ALL_PUMP -> "تمامی پمپ‌های ایستگاه دچار خطا شده‌اند و سیستم قادر به تامین فشار نیست."
        SystemErrorBit.SYSTEM_STOP_BY_USER -> "سیستم از طریق پنل یا مدباس در حالت توقف قرار گرفته است."
    }

    fun errorBit2Title(bit: SystemErrorBit2): String = when (bit) {
        SystemErrorBit2.MODE3_MAIN_PUMP -> "خطای پمپ اصلی در مد ۳"
        SystemErrorBit2.MODE3_DRIVE -> "خطای درایو در مد ۳"
        SystemErrorBit2.FULL_LOAD -> "خطای کارکرد تمام‌بار (Full Load)"
        SystemErrorBit2.SYSTEM_STOPPED -> "سیستم متوقف است (System Stopped)"
        SystemErrorBit2.EMG_STOPPED -> "توقف اضطراری فعال شد (EMG)"
        SystemErrorBit2.BATTERY_VOLTAGE -> "خطای ولتاژ باتری بکاپ کنترلر"
    }

    fun errorBit2Hint(bit: SystemErrorBit2): String = when (bit) {
        SystemErrorBit2.MODE3_MAIN_PUMP -> "خطا در پمپ اصلی پیکربندی مد ۳ گزارش شده است."
        SystemErrorBit2.MODE3_DRIVE -> "خطا در اینورتر پیکربندی مد ۳ گزارش شده است."
        SystemErrorBit2.FULL_LOAD -> "ایستگاه بیش از زمان مجاز Full Load با تمام توان کار کرده و به فشار هدف نرسیده است."
        SystemErrorBit2.SYSTEM_STOPPED -> "کنترلر در وضعیت توقف قرار دارد (وضعیت کنترل یا کلید توقف بررسی شود)."
        SystemErrorBit2.EMG_STOPPED -> "ورودی توقف اضطراری (Emergency Stop) فعال شده است."
        SystemErrorBit2.BATTERY_VOLTAGE -> "ولتاژ باتری ساعت/حافظه کنترلر ضعیف است و نیاز به بررسی دارد."
    }

    fun localizeAlarm(alarm: Alarm): Pair<String, String> {
        SystemErrorBit.entries.firstOrNull { it.label == alarm.label }?.let { bit ->
            return errorBitTitle(bit) to "بیت ${bit.bit} ورد ۸۰ — ${errorBitHint(bit)}"
        }
        SystemErrorBit2.entries.firstOrNull { it.label == alarm.label }?.let { bit ->
            return errorBit2Title(bit) to "بیت ${bit.bit} ورد ۸۱ — ${errorBit2Hint(bit)}"
        }
        if (alarm.label == "Drive fail") {
            return "خطای درایو / اینورتر" to "کویل ۲۵ خطای درایو را گزارش می‌دهد؛ کد خطای روی LCD درایو بررسی شود."
        }
        if (alarm.label.startsWith("Pump ") && alarm.label.endsWith(" fail")) {
            val num = alarm.label.removePrefix("Pump ").removeSuffix(" fail")
            return "خطای پمپ $num" to "کویل خطای پمپ $num فعال است؛ بی‌متال، جریان موتور و ترمینال PR$num بررسی شود."
        }
        if (alarm.label.startsWith("Undocumented error word2 bit ")) {
            val bit = alarm.label.removePrefix("Undocumented error word2 bit ")
            return "بیت خطای ثبت‌نشده ورد ۸۱ ($bit)" to "بیت $bit رجیستر ۸۱ فعال است."
        }
        if (alarm.label.startsWith("Undocumented error bit ")) {
            val bit = alarm.label.removePrefix("Undocumented error bit ")
            return "بیت خطای ثبت‌نشده ورد ۸۰ ($bit)" to "بیت $bit رجیستر ۸۰ فعال است."
        }
        return alarm.label to alarm.detail
    }

    fun registerLabel(def: RegisterDefinition): String = when (def.key) {
        Bpc02RegisterMap.DR_STATUS -> "وضعیت DR (کنتاکتور/درایو)"
        Bpc02RegisterMap.DRIVE_FAIL -> "خطای درایو"
        Bpc02RegisterMap.DRIVE_ACTIVATION -> "فعال‌سازی درایو (مد ۲ و ۳)"
        Bpc02RegisterMap.FLOATER_LOW -> "فلوتر سطح پایین مخزن (L)"
        Bpc02RegisterMap.FLOATER_HIGH -> "فلوتر سطح بالا مخزن (H)"
        Bpc02RegisterMap.DISPLAYED_PAGE -> "شماره صفحه نمایش LCD"
        Bpc02RegisterMap.SYSTEM_PRESSURE -> "فشار لحظه‌ای سیستم"
        Bpc02RegisterMap.SYSTEM_PRESSURE_SETPOINT -> "فشار هدف سیستم (Setpoint)"
        Bpc02RegisterMap.V_RN -> "ولتاژ فاز به نول R-N"
        Bpc02RegisterMap.V_SN -> "ولتاژ فاز به نول S-N"
        Bpc02RegisterMap.V_TN -> "ولتاژ فاز به نول T-N"
        Bpc02RegisterMap.V_RS -> "ولتاژ فاز به فاز R-S"
        Bpc02RegisterMap.V_ST -> "ولتاژ فاز به فاز S-T"
        Bpc02RegisterMap.V_TR -> "ولتاژ فاز به فاز T-R"
        Bpc02RegisterMap.MAINS_FREQUENCY -> "فرکانس برق شهر"
        Bpc02RegisterMap.DRIVE1_FREQUENCY -> "فرکانس درایو ۱"
        Bpc02RegisterMap.DRIVE2_FREQUENCY -> "فرکانس درایو ۲"
        Bpc02RegisterMap.SYSTEM_ERROR -> "کلمه خطای سیستم ۱ (ورد ۸۰)"
        Bpc02RegisterMap.SYSTEM_ERROR_2 -> "کلمه خطای سیستم ۲ (ورد ۸۱)"
        Bpc02RegisterMap.MULTI_FUNCTION_REGISTER -> "فرمان چندمنظوره — ریست خطای پمپ (کد ۲۵)"
        Bpc02RegisterMap.CONTROL_STATUS -> "حالت عملکرد کنترلر (Control Status)"
        "control_phase_activation" -> "فعال‌سازی کنترل فاز داخلی"
        "off_changing_activation" -> "تغییر نوبت در خاموشی (OFF Changing)"
        "time_changing_activation" -> "تغییر نوبت زمانی (Time Changing)"
        "reserve_auto_activation" -> "رزرو خودکار پمپ"
        "reserve_auto_type" -> "نوع کنتاکت رزرو خودکار"
        "control_phase_type" -> "نوع برق ورودی کنترل فاز"
        "control_phase_min_voltage" -> "حداقل ولتاژ مجاز کنترل فاز"
        "control_phase_max_voltage" -> "حداکثر ولتاژ مجاز کنترل فاز"
        "control_phase_delay" -> "تاخیر قطع/وصل کنترل فاز"
        "control_phase_sensitivity" -> "حساسیت عدم تقارن کنترل فاز"
        "floater_trigger_level" -> "آستانه حساسیت فلوتر (مقاومت)"
        "floater_type" -> "نوع فلوتر مخزن"
        "floater_delay" -> "تاخیر قطع/وصل فلوتر"
        "pressure_sensor_output_type" -> "نوع سیگنال خروجی سنسور فشار"
        "pressure_sensor_input_type" -> "رنج ورودی سنسور فشار"
        "number_of_pump" -> "تعداد کل پمپ‌ها"
        "number_of_variable_pump" -> "تعداد پمپ‌های دورمتغیر (درایودار)"
        "jockey_type" -> "حالت پمپ جاکی (Jockey)"
        "pid_p_value" -> "ضریب تناسبی PID (مقدار P)"
        "pid_i_value" -> "ضریب انتگرالی PID (مقدار I)"
        "pid_d_value" -> "ضریب مشتقی PID (مقدار D)"
        "max_pressure_value" -> "حداکثر فشار حفاظتی سیستم"
        "sensor_calibration_value" -> "ضریب کالیبراسیون سنسور فشار"
        "wakeup_differential" -> "اختلاف فشار بیداری (Wakeup Diff)"
        "sleep_frequency" -> "فرکانس خواب (Sleep Frequency)"
        "sleep_off_delay" -> "تاخیر ورود به خواب (Sleep Off Delay)"
        "mode3_switch_time" -> "زمان سوییچ مد ۳"
        "time_changing_time" -> "بازه زمان نوبت‌دهی پمپ‌ها"
        "add_delay" -> "تاخیر افزودن پمپ کمکی (Add Delay)"
        "remove_delay" -> "تاخیر خروج پمپ کمکی (Remove Delay)"
        "wakeup_on_delay" -> "تاخیر بیداری پمپ (Wakeup On Delay)"
        "constant_pump_percent" -> "درصد کارکرد پمپ دورثابت"
        "wiring_mode" -> "حالت سیم‌بندی (Wiring Mode)"
        "sleep_gap" -> "اختلاف فشار خواب (Sleep Gap)"
        "auto_service_off_time" -> "زمان توقف اتو سرویس"
        "auto_service_on_time" -> "مدت روشن بودن اتو سرویس"
        "auto_service_interval" -> "بازه زمانی اتو سرویس"
        "serial_number" -> "شماره سریال کنترلر"
        "start_frequency" -> "فرکانس شروع (Start Frequency)"
        "stop_frequency" -> "فرکانس توقف (Stop Frequency)"
        "full_load_status" -> "وضعیت حفاظت تمام‌بار (Full Load)"
        "full_load_pressure" -> "فشار آستانه تمام‌بار"
        "full_load_time" -> "زمان مجاز کارکرد تمام‌بار"
        "control_direction" -> "جهت کنترل (Control Direction)"
        "modbus_address" -> "آدرس شبکه مدباس (Slave ID)"
        "ao1_type" -> "تخصیص خروجی آنالوگ ۱ (AO1)"
        "ao2_type" -> "تخصیص خروجی آنالوگ ۲ (AO2)"
        "lcd_contrast" -> "کنتراست نمایشگر LCD"
        "ao1_gain" -> "ضریب بهره خروجی آنالوگ ۱"
        "ao2_gain" -> "ضریب بهره خروجی آنالوگ ۲"
        else -> when {
            def.key.startsWith("output_") && def.key.endsWith("_manual") ->
                "خروجی دستی ${def.key.removePrefix("output_").removeSuffix("_manual")}"
            def.key.startsWith("output_") ->
                "وضعیت رله خروجی ${def.key.removePrefix("output_")}"
            def.key.startsWith("pr_") ->
                "ورودی برگشت کنتاکتور PR${def.key.removePrefix("pr_")}"
            def.key.startsWith("mfi_") && def.key.endsWith("_type") ->
                "نوع ورودی چندمنظوره MFI${def.key.removePrefix("mfi_").removeSuffix("_type")}"
            def.key.startsWith("mfi_") ->
                "وضعیت ورودی چندمنظوره MFI${def.key.removePrefix("mfi_")}"
            def.key.startsWith("mfo_") && def.key.endsWith("_type") ->
                "نوع خروجی چندمنظوره MFO${def.key.removePrefix("mfo_").removeSuffix("_type")}"
            def.key.startsWith("pump_") && def.key.endsWith("_status") ->
                "وضعیت کارکرد پمپ ${def.key.removePrefix("pump_").removeSuffix("_status")}"
            def.key.startsWith("pump_") && def.key.endsWith("_fail") ->
                "وضعیت خطای پمپ ${def.key.removePrefix("pump_").removeSuffix("_fail")}"
            def.key.startsWith("pump_") && def.key.endsWith("_activation") ->
                "فعال‌سازی پمپ ${def.key.removePrefix("pump_").removeSuffix("_activation")}"
            def.key.startsWith("pump_") && def.key.endsWith("_on_pressure") ->
                "فشار روشن شدن پمپ ${def.key.removePrefix("pump_").removeSuffix("_on_pressure")}"
            def.key.startsWith("pump_") && def.key.endsWith("_on_delay") ->
                "تاخیر روشن شدن پمپ ${def.key.removePrefix("pump_").removeSuffix("_on_delay")}"
            def.key.startsWith("pump_") && def.key.endsWith("_off_pressure") ->
                "فشار خاموش شدن پمپ ${def.key.removePrefix("pump_").removeSuffix("_off_pressure")}"
            def.key.startsWith("pump_") && def.key.endsWith("_off_delay") ->
                "تاخیر خاموش شدن پمپ ${def.key.removePrefix("pump_").removeSuffix("_off_delay")}"
            def.key.startsWith("pump_") && def.key.endsWith("_run_sec") ->
                "کارکرد پمپ ${def.key.removePrefix("pump_").removeSuffix("_run_sec")} (ثانیه)"
            def.key.startsWith("pump_") && def.key.endsWith("_run_min") ->
                "کارکرد پمپ ${def.key.removePrefix("pump_").removeSuffix("_run_min")} (دقیقه)"
            def.key.startsWith("pump_") && def.key.endsWith("_run_hour") ->
                "کارکرد پمپ ${def.key.removePrefix("pump_").removeSuffix("_run_hour")} (ساعت)"
            def.key.startsWith("analog_output_") ->
                "خروجی آنالوگ دستی ${def.key.removePrefix("analog_output_").removeSuffix("_manual")}"
            else -> def.label
        }
    }

    fun enumOptionLabel(def: RegisterDefinition, raw: Int): String = when {
        def.key == Bpc02RegisterMap.CONTROL_STATUS -> when (raw) {
            0 -> "توقف / غیرفعال (Disable)"
            1 -> "کنترل دورمتغیر (PID)"
            2 -> "کنترل پله‌ای (ON/OFF)"
            else -> "نامشخص ($raw)"
        }
        def.key == "jockey_type" -> when (raw) {
            0 -> "غیرفعال (Disable)"
            1 -> "جاکی روشن (Jockey On)"
            2 -> "جاکی خاموش (Jockey Off)"
            else -> "نامشخص ($raw)"
        }
        def.key == "floater_type" -> when (raw) {
            0 -> "غیرفعال (Disable)"
            1 -> "داخلی (Internal)"
            2 -> "خارجی (External)"
            else -> "نامشخص ($raw)"
        }
        def.key == "control_phase_type" -> when (raw) {
            1 -> "تک‌فاز (1P)"
            3 -> "سه‌فاز (3P)"
            else -> "نامشخص ($raw)"
        }
        def.key == "pressure_sensor_output_type" -> when (raw) {
            2 -> "جریان 4~20mA"
            3 -> "جریان 0~20mA"
            4 -> "ولتاژ 0~5V"
            5 -> "ولتاژ 0~10V"
            6 -> "ولتاژ 2~10V"
            else -> "نامشخص ($raw)"
        }
        def.key == "pressure_sensor_input_type" -> when (raw) {
            0 -> "0 تا 6 بار"
            1 -> "0 تا 10 بار"
            2 -> "0 تا 16 بار"
            3 -> "0 تا 25 بار"
            4 -> "0 تا 40 بار"
            5 -> "0 تا 60 بار"
            10 -> "پرشر سوئیچ (PS)"
            else -> "نامشخص ($raw)"
        }
        def.key == "control_direction" -> when (raw) {
            0 -> "کاهشی (Down)"
            1 -> "افزایشی (Up)"
            else -> "نامشخص ($raw)"
        }
        def.key == "ao1_type" || def.key == "ao2_type" -> when (raw) {
            0 -> "پمپ ۱ (Pump1)"
            1 -> "پمپ ۲ (Pump2)"
            else -> "نامشخص ($raw)"
        }
        def.key == Bpc02RegisterMap.MULTI_FUNCTION_REGISTER -> when (raw) {
            0 -> "بدون فرمان (No Function)"
            25 -> "ریست خطای پمپ‌ها (Reset Pump Error)"
            else -> "کد $raw"
        }
        def.key.startsWith("mfi_") && def.key.endsWith("_type") -> when (raw) {
            0 -> "غیرفعال (DISABLE)"
            1 -> "پرشر سوئیچ (PS)"
            2 -> "ترموسوئیچ (TS)"
            3 -> "حداکثر فشار (MP)"
            4 -> "توقف اضطراری (EMG)"
            5 -> "کنترل فاز خارجی (CP)"
            6 -> "فلوتر خارجی (FL)"
            7 -> "فیدبک پمپ ۵ (PR5)"
            8 -> "فیدبک پمپ ۶ (PR6)"
            9 -> "ریست خطا (RESET)"
            10 -> "دور ثابت (FIX)"
            else -> "مقدار نامشخص ($raw)"
        }
        def.key.startsWith("mfo_") && def.key.endsWith("_type") -> when (raw) {
            0 -> "غیرفعال (DISABLE)"
            1 -> "کنتاکتور مستقیم پمپ ۳ (MC3)"
            2 -> "کنتاکتور درایو پمپ ۴ (DC4)"
            3 -> "کنتاکتور مستقیم پمپ ۴ (MC4)"
            4 -> "فن تابلو (FAN)"
            5 -> "آلارم خطا (ALARM)"
            6 -> "شیر برقی (E_VALVE)"
            7 -> "وضعیت کارکرد (RUN)"
            8 -> "آماده‌به‌کار (READY)"
            9 -> "پمپ پرکن (FILLER)"
            10 -> "خطای پمپ ۱ (P1_ERR)"
            11 -> "خطای پمپ ۲ (P2_ERR)"
            12 -> "خطای پمپ ۳ (P3_ERR)"
            13 -> "خطای پمپ ۴ (P4_ERR)"
            else -> "مقدار نامشخص ($raw)"
        }
        def.key == "reserve_auto_type" -> if (raw == 0) "بسته (NC)" else "باز (NO)"
        def.enumLabels == mapOf(0 to "Disabled", 1 to "Enabled") -> if (raw == 1) "فعال" else "غیرفعال"
        else -> def.enumLabels[raw]?.let { "$it ($raw)" } ?: raw.toString()
    }

    fun formatRegisterValue(def: RegisterDefinition, raw: Int): String = when {
        def.enumLabels == mapOf(0 to "OFF", 1 to "ON") -> if (raw == 1) "روشن (ON)" else "خاموش (OFF)"
        def.enumLabels == mapOf(0 to "Normal", 1 to "Failed") -> if (raw == 1) "خطا (Failed)" else "عادی (Normal)"
        def.enumLabels == mapOf(0 to "Not connected", 1 to "Connected") -> if (raw == 1) "متصل" else "قطع"
        def.enumLabels == mapOf(0 to "Disabled", 1 to "Enabled") -> if (raw == 1) "فعال" else "غیرفعال"
        def.key == "reserve_auto_type" -> enumOptionLabel(def, raw)
        def.enumLabels.isNotEmpty() -> enumOptionLabel(def, raw)
        def.unit == "bar" && def.scale != null -> "%.2f بار".format(raw * def.scale)
        def.unit == "Hz" && def.scale != null -> {
            val dec = if (def.scale >= 0.1) 1 else 2
            "%.${dec}f هرتز".format(raw * def.scale)
        }
        def.unit == "V" && def.scale != null -> "$raw ولت"
        def.unit == "%" && def.scale != null -> "$raw درصد"
        def.unit == "kΩ" && def.scale != null -> "$raw کیلواهم"
        def.unit == "h" -> "$raw ساعت"
        def.unit == "min" -> "$raw دقیقه"
        def.unit == "s" && def.scale != null -> when {
            def.scale < 0.1 -> "%.2f ثانیه".format(raw * def.scale)
            def.scale < 1.0 -> "%.1f ثانیه".format(raw * def.scale)
            else -> "$raw ثانیه"
        }
        else -> def.format(raw)
    }

    fun formatRunTime(rt: RunTime): String = "%d ساعت %02d دقیقه %02d ثانیه".format(rt.hours, rt.minutes, rt.seconds)

    fun formatRunTimeCompact(rt: RunTime): String = "%02d:%02d:%02d".format(rt.hours, rt.minutes, rt.seconds)

    fun validationError(field: ConfigValidation.Field, fallback: String): String = when (field) {
        ConfigValidation.Field.HOST -> "آدرس IP یا نام میزبان مبدل را صحیح وارد کنید (مثلاً 192.168.1.74)"
        ConfigValidation.Field.PORT -> "پورت TCP باید عددی بین ۱ تا ۶۵۵۳۵ باشد"
        ConfigValidation.Field.UNIT_ID -> "شناسه Slave / Unit ID باید بین ۱ تا ۲۴۷ باشد"
        ConfigValidation.Field.CONNECT_TIMEOUT -> "زمان انتظار اتصال باید بین ۵۰۰ تا ۳۰۰۰۰ میلی‌ثانیه باشد"
        ConfigValidation.Field.RESPONSE_TIMEOUT -> "زمان انتظار پاسخ باید بین ۲۰۰ تا ۱۰۰۰۰ میلی‌ثانیه باشد"
        ConfigValidation.Field.POLL_INTERVAL -> "بازه نمونه‌برداری باید بین ۵۰۰ تا ۶۰۰۰۰ میلی‌ثانیه باشد"
        ConfigValidation.Field.MODEL -> "نقشه رجیستر مدل انتخاب‌شده هنوز آماده نیست"
    }.ifBlank { fallback }
}
