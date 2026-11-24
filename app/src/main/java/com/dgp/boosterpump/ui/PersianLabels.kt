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
        DataSourceMode.LIVE -> "اتصال به دستگاه واقعی"
        DataSourceMode.SIMULATION -> "حالت نمایشی (آزمایشی)"
    }

    fun modelLabel(model: ControllerModel): String = when (model) {
        ControllerModel.BPC02 -> "کنترلر DGP BPC02"
        ControllerModel.BPC04 -> "کنترلر DGP BPC04"
    }

    fun modelNote(model: ControllerModel): String = when (model) {
        ControllerModel.BPC02 -> "پشتیبانی کامل از مانیتورینگ زنده و تنظیمات کنترلر بوستر پمپ DGP BPC02."
        ControllerModel.BPC04 -> "در نسخه‌های آینده اضافه خواهد شد."
    }

    fun framingLabel(framing: Framing): String = when (framing) {
        Framing.MODBUS_TCP -> "Modbus TCP (استاندارد)"
        Framing.RTU_OVER_TCP -> "RTU روی TCP"
    }

    fun framingDescription(framing: Framing): String = when (framing) {
        Framing.MODBUS_TCP -> "حالت استاندارد مبدل شبکه برای ارتباط پایدار با کنترلر بوستر پمپ."
        Framing.RTU_OVER_TCP -> "ارسال مستقیم بسته‌های سریال روی شبکه."
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
        0 -> "خاموش"
        1 -> "دورمتغیر (PID)"
        2 -> "پله‌ای (ON/OFF)"
        else -> "نامشخص"
    }

    fun wiringModeShort(mode: Int?): String = when (mode) {
        1 -> "مد ۱"
        2 -> "مد ۲"
        3 -> "مد ۳"
        else -> "نامشخص"
    }

    fun wiringModeLabel(mode: Int?): String = when (mode) {
        1 -> "مد ۱ — دورمتغیر مستقل"
        2 -> "مد ۲ — چنج‌اور با درایو"
        3 -> "مد ۳ — سوییچ روی شبکه"
        else -> "نامشخص"
    }

    fun driverCountLabel(count: Int?): String = when (count) {
        null -> "—"
        0 -> "بدون درایو"
        1 -> "۱ درایو"
        2 -> "۲ درایو"
        else -> "$count درایو"
    }

    fun scenarioLabel(s: SimulationScenario): String = when (s) {
        SimulationScenario.NORMAL_DUTY -> "کارکرد عادی (۱ پمپ با درایو)"
        SimulationScenario.PEAK_DEMAND -> "مصرف بالا (۳ پمپ هم‌زمان)"
        SimulationScenario.FLOATER_LOW_ALARM -> "هشدار کمبود آب مخزن"
        SimulationScenario.PUMP_AND_DRIVE_FAULT -> "خطای درایو و پمپ ۲"
        SimulationScenario.LINK_TIMEOUT -> "شبیه‌سازی قطع ارتباط"
    }

    fun scenarioDescription(s: SimulationScenario): String = when (s) {
        SimulationScenario.NORMAL_DUTY -> "فشار ~۴.۲ بار (هدف ۴.۵۰ بار)، پمپ ۱ در مدار با درایو ۳۸.۵ هرتز، برق سه‌فاز سالم."
        SimulationScenario.PEAK_DEMAND -> "مصرف بالا ~۳.۸۵ بار، پمپ‌های ۱ تا ۳ در مدار و درایو روی ۴۷.۵ هرتز."
        SimulationScenario.FLOATER_LOW_ALARM -> "سطح آب مخزن پایین آمده و ایستگاه برای حفاظت پمپ‌ها متوقف شده است."
        SimulationScenario.PUMP_AND_DRIVE_FAULT -> "نمایش خطای اینورتر، پمپ ۲، فشار بالا و ولتاژ بالا — قابل آزمایش با رفع خطا."
        SimulationScenario.LINK_TIMEOUT -> "شبیه‌سازی عدم پاسخ‌دهی مبدل برای بررسی نمایش پیام قطع ارتباط."
    }

    fun verificationLabel(v: Verification): String = when (v) {
        Verification.HARDWARE_VERIFIED -> "تایید سخت‌افزاری"
        Verification.DOCUMENTED -> "مستند رسمی DGP"
        Verification.AMBIGUOUS -> "نیازمند بررسی"
    }

    fun tableLabel(table: VendorTable): String = when (table) {
        VendorTable.BIT -> "کویل (FC01)"
        VendorTable.WORD -> "ورد (FC03)"
    }

    fun errorBitTitle(bit: SystemErrorBit): String = when (bit) {
        SystemErrorBit.FLOATER -> "کمبود آب مخزن (فلوتر)"
        SystemErrorBit.MAX_PRESSURE -> "افزایش بیش از حد فشار"
        SystemErrorBit.PRESSURE_SENSOR_DISCONNECTED -> "قطع شدن سنسور فشار"
        SystemErrorBit.PHASE_SENSITIVITY -> "عدم تقارن ولتاژ فازها"
        SystemErrorBit.PHASE_MAX_VOLTAGE -> "افزایش بیش از حد ولتاژ برق"
        SystemErrorBit.PHASE_MIN_VOLTAGE -> "افت ولتاژ برق ورودی"
        SystemErrorBit.PHASE_SEQUENCE -> "جابه‌جایی ترتیب فازها"
        SystemErrorBit.DRIVE -> "خطای درایو (اینورتر)"
        SystemErrorBit.PUMP1 -> "خطای پمپ ۱"
        SystemErrorBit.PUMP2 -> "خطای پمپ ۲"
        SystemErrorBit.PUMP3 -> "خطای پمپ ۳"
        SystemErrorBit.PUMP4 -> "خطای پمپ ۴"
        SystemErrorBit.PUMP5 -> "خطای پمپ ۵"
        SystemErrorBit.PUMP6 -> "خطای پمپ ۶"
        SystemErrorBit.ALL_PUMP -> "خطای تمامی پمپ‌ها"
        SystemErrorBit.SYSTEM_STOP_BY_USER -> "توقف سیستم توسط کاربر"
    }

    fun errorBitHint(bit: SystemErrorBit): String = when (bit) {
        SystemErrorBit.FLOATER -> "سطح آب منبع ذخیره از حد مجاز کمتر شده است؛ منبع آب و فلوتر بررسی شود."
        SystemErrorBit.MAX_PRESSURE -> "فشار خط از سقف مجاز تنظیم‌شده فراتر رفته است."
        SystemErrorBit.PRESSURE_SENSOR_DISCONNECTED -> "کابل و اتصالات سنسور فشار بررسی شود."
        SystemErrorBit.PHASE_SENSITIVITY -> "اختلاف ولتاژ بین فازهای برق ورودی بیش از حد مجاز است."
        SystemErrorBit.PHASE_MAX_VOLTAGE -> "ولتاژ برق ورودی از سقف مجاز بالاتر رفته است."
        SystemErrorBit.PHASE_MIN_VOLTAGE -> "ولتاژ برق ورودی از حداقل مجاز پایین‌تر آمده است."
        SystemErrorBit.PHASE_SEQUENCE -> "ترتیب فازهای برق ورودی تابلو (R-S-T) جابه‌جا شده است."
        SystemErrorBit.DRIVE -> "اینورتر خطا گزارش کرده است؛ کد خطای روی نمایشگر درایو بررسی شود."
        SystemErrorBit.PUMP1 -> "بی‌متال، کلید حرارتی و مدار حفاظتی پمپ ۱ بررسی شود."
        SystemErrorBit.PUMP2 -> "بی‌متال، کلید حرارتی و مدار حفاظتی پمپ ۲ بررسی شود."
        SystemErrorBit.PUMP3 -> "بی‌متال، کلید حرارتی و مدار حفاظتی پمپ ۳ بررسی شود."
        SystemErrorBit.PUMP4 -> "بی‌متال، کلید حرارتی و مدار حفاظتی پمپ ۴ بررسی شود."
        SystemErrorBit.PUMP5 -> "بی‌متال، کلید حرارتی و مدار حفاظتی پمپ ۵ بررسی شود."
        SystemErrorBit.PUMP6 -> "بی‌متال، کلید حرارتی و مدار حفاظتی پمپ ۶ بررسی شود."
        SystemErrorBit.ALL_PUMP -> "تمامی پمپ‌های ایستگاه دچار خطا شده‌اند و سیستم قادر به تامین فشار نیست."
        SystemErrorBit.SYSTEM_STOP_BY_USER -> "ایستگاه در حالت خاموش قرار گرفته است."
    }

    fun errorBit2Title(bit: SystemErrorBit2): String = when (bit) {
        SystemErrorBit2.MODE3_MAIN_PUMP -> "خطای پمپ اصلی در مد ۳"
        SystemErrorBit2.MODE3_DRIVE -> "خطای درایو در مد ۳"
        SystemErrorBit2.FULL_LOAD -> "خطای کارکرد تمام‌بار"
        SystemErrorBit2.SYSTEM_STOPPED -> "ایستگاه در حالت توقف است"
        SystemErrorBit2.EMG_STOPPED -> "توقف اضطراری فعال است"
        SystemErrorBit2.BATTERY_VOLTAGE -> "ضعیف شدن باتری داخلی کنترلر"
    }

    fun errorBit2Hint(bit: SystemErrorBit2): String = when (bit) {
        SystemErrorBit2.MODE3_MAIN_PUMP -> "خطا در پمپ اصلی در مد ۳ گزارش شده است."
        SystemErrorBit2.MODE3_DRIVE -> "خطا در اینورتر در مد ۳ گزارش شده است."
        SystemErrorBit2.FULL_LOAD -> "ایستگاه بیش از زمان مجاز با تمام توان کار کرده و به فشار هدف نرسیده است."
        SystemErrorBit2.SYSTEM_STOPPED -> "کنترلر در وضعیت توقف قرار دارد؛ حالت عملکرد یا کلید توقف بررسی شود."
        SystemErrorBit2.EMG_STOPPED -> "کلید توقف اضطراری فعال شده است."
        SystemErrorBit2.BATTERY_VOLTAGE -> "باتری پشتیبان کنترلر ضعیف است و نیاز به بررسی دارد."
    }

    fun localizeAlarm(alarm: Alarm): Pair<String, String> {
        SystemErrorBit.entries.firstOrNull { it.label == alarm.label }?.let { bit ->
            return errorBitTitle(bit) to errorBitHint(bit)
        }
        SystemErrorBit2.entries.firstOrNull { it.label == alarm.label }?.let { bit ->
            return errorBit2Title(bit) to errorBit2Hint(bit)
        }
        if (alarm.label == "Drive fail") {
            return "خطای درایو (اینورتر)" to "اینورتر خطا گزارش داده است؛ نمایشگر درایو بررسی شود."
        }
        if (alarm.label.startsWith("Pump ") && alarm.label.endsWith(" fail")) {
            val num = alarm.label.removePrefix("Pump ").removeSuffix(" fail")
            return "خطای پمپ $num" to "مدار حفاظتی، بی‌متال و جریان موتور پمپ $num بررسی شود."
        }
        if (alarm.label.startsWith("Undocumented error word2 bit ") || alarm.label.startsWith("Undocumented error bit ")) {
            return "هشدار حفاظتی کنترلر" to "یک هشدار حفاظتی روی کنترلر فعال شده است؛ نمایشگر تابلو بررسی شود."
        }
        return alarm.label to alarm.detail
    }

    fun registerLabel(def: RegisterDefinition): String = when (def.key) {
        Bpc02RegisterMap.DR_STATUS -> "وضعیت خروجی درایو"
        Bpc02RegisterMap.DRIVE_FAIL -> "خطای درایو"
        Bpc02RegisterMap.DRIVE_ACTIVATION -> "وضعیت سرویس درایو (مد ۲ و ۳)"
        Bpc02RegisterMap.FLOATER_LOW -> "فلوتر سطح پایین مخزن"
        Bpc02RegisterMap.FLOATER_HIGH -> "فلوتر سطح بالا مخزن"
        Bpc02RegisterMap.DISPLAYED_PAGE -> "شماره صفحه نمایش کنترلر"
        Bpc02RegisterMap.SYSTEM_PRESSURE -> "فشار لحظه‌ای سیستم"
        Bpc02RegisterMap.SYSTEM_PRESSURE_SETPOINT -> "فشار تنظیم‌شده سیستم"
        Bpc02RegisterMap.V_RN -> "ولتاژ فاز R به نول"
        Bpc02RegisterMap.V_SN -> "ولتاژ فاز S به نول"
        Bpc02RegisterMap.V_TN -> "ولتاژ فاز T به نول"
        Bpc02RegisterMap.V_RS -> "ولتاژ بین فاز R و S"
        Bpc02RegisterMap.V_ST -> "ولتاژ بین فاز S و T"
        Bpc02RegisterMap.V_TR -> "ولتاژ بین فاز T و R"
        Bpc02RegisterMap.MAINS_FREQUENCY -> "فرکانس برق شهر"
        Bpc02RegisterMap.DRIVE1_FREQUENCY -> "فرکانس درایو ۱"
        Bpc02RegisterMap.DRIVE2_FREQUENCY -> "فرکانس درایو ۲"
        Bpc02RegisterMap.SYSTEM_ERROR -> "خطاهای اصلی سیستم"
        Bpc02RegisterMap.SYSTEM_ERROR_2 -> "خطاهای تکمیلی سیستم"
        Bpc02RegisterMap.MULTI_FUNCTION_REGISTER -> "رفع خطای پمپ‌ها"
        Bpc02RegisterMap.CONTROL_STATUS -> "حالت عملکرد کنترلر"
        "control_phase_activation" -> "وضعیت کنترل فاز داخلی"
        "off_changing_activation" -> "تغییر نوبت پس از خاموشی"
        "time_changing_activation" -> "تغییر نوبت زمانی پمپ‌ها"
        "reserve_auto_activation" -> "رزرو خودکار پمپ"
        "reserve_auto_type" -> "نوع کنتاکت رزرو خودکار"
        "control_phase_type" -> "نوع برق ورودی"
        "control_phase_min_voltage" -> "حداقل ولتاژ مجاز"
        "control_phase_max_voltage" -> "حداکثر ولتاژ مجاز"
        "control_phase_delay" -> "تاخیر قطع و وصل کنترل فاز"
        "control_phase_sensitivity" -> "حساسیت عدم تقارن فاز"
        "floater_trigger_level" -> "حساسیت سنسور فلوتر"
        "floater_type" -> "نوع فلوتر مخزن"
        "floater_delay" -> "تاخیر قطع و وصل فلوتر"
        "pressure_sensor_output_type" -> "نوع سیگنال خروجی سنسور"
        "pressure_sensor_input_type" -> "رنج اندازه‌گیری سنسور فشار"
        "number_of_pump" -> "تعداد کل پمپ‌ها"
        "number_of_variable_pump" -> "تعداد درایوها (پمپ دورمتغیر)"
        "jockey_type" -> "حالت پمپ جاکی"
        "pid_p_value" -> "ضریب تناسبی (P)"
        "pid_i_value" -> "ضریب انتگرالی (I)"
        "pid_d_value" -> "ضریب مشتقی (D)"
        "max_pressure_value" -> "حداکثر فشار حفاظتی"
        "sensor_calibration_value" -> "کالیبراسیون سنسور فشار"
        "wakeup_differential" -> "افت فشار برای بیداری پمپ"
        "sleep_frequency" -> "فرکانس ورود به حالت خواب"
        "sleep_off_delay" -> "تاخیر ورود به حالت خواب"
        "mode3_switch_time" -> "زمان تعویض در مد ۳"
        "time_changing_time" -> "بازه زمانی تغییر نوبت پمپ‌ها"
        "add_delay" -> "تاخیر ورود پمپ کمکی"
        "remove_delay" -> "تاخیر خروج پمپ کمکی"
        "wakeup_on_delay" -> "تاخیر بیداری پمپ"
        "constant_pump_percent" -> "درصد کارکرد پمپ دورثابت"
        "wiring_mode" -> "مد کاری (نحوه سیم‌بندی)"
        "sleep_gap" -> "اختلاف فشار حالت خواب"
        "auto_service_off_time" -> "زمان توقف اتو سرویس"
        "auto_service_on_time" -> "مدت کارکرد اتو سرویس"
        "auto_service_interval" -> "فاصله زمانی اتو سرویس"
        "serial_number" -> "شماره سریال دستگاه"
        "start_frequency" -> "فرکانس شروع به کار"
        "stop_frequency" -> "فرکانس توقف درایو"
        "full_load_status" -> "حفاظت کارکرد تمام‌بار"
        "full_load_pressure" -> "فشار آستانه تمام‌بار"
        "full_load_time" -> "زمان مجاز تمام‌بار"
        "control_direction" -> "جهت کنترل فشار"
        "modbus_address" -> "شماره دستگاه در شبکه"
        "ao1_type" -> "تخصیص خروجی آنالوگ ۱"
        "ao2_type" -> "تخصیص خروجی آنالوگ ۲"
        "lcd_contrast" -> "کنتراست نمایشگر کنترلر"
        "ao1_gain" -> "بهره خروجی آنالوگ ۱"
        "ao2_gain" -> "بهره خروجی آنالوگ ۲"
        else -> when {
            def.key.startsWith("output_") && def.key.endsWith("_manual") ->
                "خروجی دستی ${def.key.removePrefix("output_").removeSuffix("_manual")}"
            def.key.startsWith("output_") ->
                "رله خروجی ${def.key.removePrefix("output_")}"
            def.key.startsWith("pr_") ->
                "فیدبک کنتاکتور پمپ ${def.key.removePrefix("pr_")}"
            def.key.startsWith("mfi_") && def.key.endsWith("_type") ->
                "کاربرد ورودی چندمنظوره ${def.key.removePrefix("mfi_").removeSuffix("_type")}"
            def.key.startsWith("mfi_") ->
                "ورودی چندمنظوره ${def.key.removePrefix("mfi_")}"
            def.key.startsWith("mfo_") && def.key.endsWith("_type") ->
                "کاربرد خروجی چندمنظوره ${def.key.removePrefix("mfo_").removeSuffix("_type")}"
            def.key.startsWith("pump_") && def.key.endsWith("_status") ->
                "وضعیت کارکرد پمپ ${def.key.removePrefix("pump_").removeSuffix("_status")}"
            def.key.startsWith("pump_") && def.key.endsWith("_fail") ->
                "وضعیت خطای پمپ ${def.key.removePrefix("pump_").removeSuffix("_fail")}"
            def.key.startsWith("pump_") && def.key.endsWith("_activation") ->
                "پمپ ${def.key.removePrefix("pump_").removeSuffix("_activation")}"
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
            0 -> "خاموش"
            1 -> "دورمتغیر (PID)"
            2 -> "پله‌ای (ON/OFF)"
            else -> "نامشخص ($raw)"
        }
        def.key == "wiring_mode" -> wiringModeLabel(raw)
        def.key == "jockey_type" -> when (raw) {
            0 -> "غیرفعال"
            1 -> "جاکی روشن"
            2 -> "جاکی خاموش"
            else -> "نامشخص ($raw)"
        }
        def.key == "floater_type" -> when (raw) {
            0 -> "غیرفعال"
            1 -> "فلوتر داخلی"
            2 -> "فلوتر خارجی"
            else -> "نامشخص ($raw)"
        }
        def.key == "control_phase_type" -> when (raw) {
            1 -> "تک‌فاز"
            3 -> "سه‌فاز"
            else -> "نامشخص ($raw)"
        }
        def.key == "pressure_sensor_output_type" -> when (raw) {
            2 -> "جریان 4 تا 20 میلی‌آمپر"
            3 -> "جریان 0 تا 20 میلی‌آمپر"
            4 -> "ولتاژ 0 تا 5 ولت"
            5 -> "ولتاژ 0 تا 10 ولت"
            6 -> "ولتاژ 2 تا 10 ولت"
            else -> "نامشخص ($raw)"
        }
        def.key == "pressure_sensor_input_type" -> when (raw) {
            0 -> "0 تا 6 بار"
            1 -> "0 تا 10 بار"
            2 -> "0 تا 16 بار"
            3 -> "0 تا 25 بار"
            4 -> "0 تا 40 بار"
            5 -> "0 تا 60 بار"
            10 -> "پرشر سوئیچ"
            else -> "نامشخص ($raw)"
        }
        def.key == "control_direction" -> when (raw) {
            0 -> "کاهشی"
            1 -> "افزایشی"
            else -> "نامشخص ($raw)"
        }
        def.key == "ao1_type" || def.key == "ao2_type" -> when (raw) {
            0 -> "پمپ ۱"
            1 -> "پمپ ۲"
            else -> "نامشخص ($raw)"
        }
        def.key == Bpc02RegisterMap.MULTI_FUNCTION_REGISTER -> when (raw) {
            0 -> "آماده"
            25 -> "رفع خطای پمپ‌ها"
            else -> "فرمان $raw"
        }
        def.key.startsWith("mfi_") && def.key.endsWith("_type") -> when (raw) {
            0 -> "غیرفعال"
            1 -> "پرشر سوئیچ"
            2 -> "ترموسوئیچ"
            3 -> "حداکثر فشار"
            4 -> "توقف اضطراری"
            5 -> "کنترل فاز خارجی"
            6 -> "فلوتر خارجی"
            7 -> "فیدبک پمپ ۵"
            8 -> "فیدبک پمپ ۶"
            9 -> "ریست خطا"
            10 -> "دور ثابت"
            else -> "نامشخص ($raw)"
        }
        def.key.startsWith("mfo_") && def.key.endsWith("_type") -> when (raw) {
            0 -> "غیرفعال"
            1 -> "کنتاکتور مستقیم پمپ ۳"
            2 -> "کنتاکتور درایو پمپ ۴"
            3 -> "کنتاکتور مستقیم پمپ ۴"
            4 -> "فن تابلو"
            5 -> "آلارم خطا"
            6 -> "شیر برقی"
            7 -> "وضعیت کارکرد"
            8 -> "آماده‌به‌کار"
            9 -> "پمپ پرکن"
            10 -> "خطای پمپ ۱"
            11 -> "خطای پمپ ۲"
            12 -> "خطای پمپ ۳"
            13 -> "خطای پمپ ۴"
            else -> "نامشخص ($raw)"
        }
        def.key == "reserve_auto_type" -> if (raw == 0) "بسته (NC)" else "باز (NO)"
        def.enumLabels == mapOf(0 to "Disabled", 1 to "Enabled") -> if (raw == 1) "فعال" else "غیرفعال"
        else -> def.enumLabels[raw] ?: raw.toString()
    }

    fun formatRegisterValue(def: RegisterDefinition, raw: Int): String = when {
        def.key == "wiring_mode" -> wiringModeLabel(raw)
        def.key == "number_of_pump" -> "$raw پمپ"
        def.key == "number_of_variable_pump" -> driverCountLabel(raw)
        def.enumLabels == mapOf(0 to "OFF", 1 to "ON") -> if (raw == 1) "روشن" else "خاموش"
        def.enumLabels == mapOf(0 to "Normal", 1 to "Failed") -> if (raw == 1) "خطا" else "عادی"
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
        ConfigValidation.Field.HOST -> "آدرس IP مبدل شبکه را صحیح وارد کنید (مثلاً 192.168.1.74)"
        ConfigValidation.Field.PORT -> "پورت شبکه باید عددی بین ۱ تا ۶۵۵۳۵ باشد"
        ConfigValidation.Field.UNIT_ID -> "شماره دستگاه باید بین ۱ تا ۲۴۷ باشد"
        ConfigValidation.Field.CONNECT_TIMEOUT -> "زمان انتظار اتصال باید بین ۵۰۰ تا ۳۰۰۰۰ میلی‌ثانیه باشد"
        ConfigValidation.Field.RESPONSE_TIMEOUT -> "مهلت پاسخ دستگاه باید بین ۲۰۰ تا ۱۰۰۰۰ میلی‌ثانیه باشد"
        ConfigValidation.Field.POLL_INTERVAL -> "بازه بروزرسانی باید بین ۵۰۰ تا ۶۰۰۰۰ میلی‌ثانیه باشد"
        ConfigValidation.Field.MODEL -> "مدل انتخاب‌شده هنوز آماده نیست"
    }.ifBlank { fallback }
}
