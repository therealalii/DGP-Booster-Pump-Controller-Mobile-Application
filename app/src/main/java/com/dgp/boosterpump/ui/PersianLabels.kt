package com.dgp.boosterpump.ui

import com.dgp.boosterpump.controller.Alarm
import com.dgp.boosterpump.controller.ControllerModel
import com.dgp.boosterpump.controller.RunTime
import com.dgp.boosterpump.controller.SystemErrorBit
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
 * gateway settings, scenarios, and register map metadata.
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
        ControllerModel.BPC02 -> "نقشه رجیستر تاییدشده روی سخت‌افزار واقعی (9600 8N1، توابع FC01 و FC03، آفست 0)."
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

    fun scenarioLabel(s: SimulationScenario): String = when (s) {
        SimulationScenario.NORMAL_DUTY -> "کارکرد عادی (۱ پمپ با درایو)"
        SimulationScenario.PEAK_DEMAND -> "اوج مصرف (۳ پمپ هم‌زمان)"
        SimulationScenario.FLOATER_LOW_ALARM -> "خطای کمبود آب مخزن (فلوتر)"
        SimulationScenario.PUMP_AND_DRIVE_FAULT -> "خطای درایو، پمپ ۲ و فشار/ولتاژ بالا"
        SimulationScenario.LINK_TIMEOUT -> "شبیه‌سازی قطع ارتباط (Timeout)"
    }

    fun scenarioDescription(s: SimulationScenario): String = when (s) {
        SimulationScenario.NORMAL_DUTY -> "فشار پایدار ~۴.۲ بار، پمپ ۱ فعال، شبکه سه‌فاز سالم و مخزن پر."
        SimulationScenario.PEAK_DEMAND -> "مصرف بالا ~۳.۸۵ بار، پمپ‌های ۱ تا ۳ در مدار با دور بالاتر درایو."
        SimulationScenario.FLOATER_LOW_ALARM -> "فلوتر پایین و بالا قطع، بیت ۳ خطای سیستم فعال، همه پمپ‌ها متوقف."
        SimulationScenario.PUMP_AND_DRIVE_FAULT -> "خطای درایو (بیت ۲۵)، خطای پمپ ۲ (بیت ۲۷)، حداکثر فشار و ولتاژ فعال."
        SimulationScenario.LINK_TIMEOUT -> "عدم پاسخ‌دهی باس RS485 برای تست نمایش خطا و اتصال مجدد خودکار."
    }

    fun verificationLabel(v: Verification): String = when (v) {
        Verification.HARDWARE_VERIFIED -> "تایید سخت‌افزاری"
        Verification.DOCUMENTED -> "مستند (دفترچه)"
        Verification.AMBIGUOUS -> "مقیاس مبهم"
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
        SystemErrorBit.PUMP1 -> "خطای پمپ ۱ (گزارش در کلمه خطا)"
    }

    fun errorBitHint(bit: SystemErrorBit): String = when (bit) {
        SystemErrorBit.FLOATER -> "سطح آب منبع ذخیره از حد مجاز کمتر شده است؛ منبع و فلوتر بررسی شود."
        SystemErrorBit.MAX_PRESSURE -> "فشار کلکتور از حد مجاز تنظیم‌شده فراتر رفته است."
        SystemErrorBit.PRESSURE_SENSOR_DISCONNECTED -> "کابل و اتصالات سنسور فشار به کنترلر بررسی شود."
        SystemErrorBit.PHASE_SENSITIVITY -> "اختلاف ولتاژ بین فازهای R، S و T بیش از حد مجاز است."
        SystemErrorBit.PHASE_MAX_VOLTAGE -> "ولتاژ ورودی شبکه از سقف مجاز بالاتر رفته است."
        SystemErrorBit.PHASE_MIN_VOLTAGE -> "ولتاژ ورودی شبکه از حداقل مجاز پایین‌تر آمده است."
        SystemErrorBit.PHASE_SEQUENCE -> "ترتیب فازهای ورودی تابلو (R-S-T) جابه‌جا شده است."
        SystemErrorBit.PUMP1 -> "بیت ۱۰ رجیستر ۸۰ فعال شده است؛ بی‌متال و رله حفاظتی پمپ ۱ بررسی شود."
    }

    fun localizeAlarm(alarm: Alarm): Pair<String, String> {
        // Match known system error bits first
        SystemErrorBit.entries.firstOrNull { it.label == alarm.label }?.let { bit ->
            return errorBitTitle(bit) to "بیت ${bit.bit} رجیستر ۸۰ — ${errorBitHint(bit)}"
        }
        if (alarm.label == "Drive fail") {
            return "خطای درایو / اینورتر" to "کویل ۲۵ خطای درایو را گزارش می‌دهد؛ کد خطای روی LCD درایو بررسی شود."
        }
        if (alarm.label.startsWith("Pump ") && alarm.label.endsWith(" fail")) {
            val num = alarm.label.removePrefix("Pump ").removeSuffix(" fail")
            return "خطای پمپ $num" to "کویل خطای پمپ $num فعال است؛ بی‌متال، جریان موتور و ترمینال PR$num بررسی شود."
        }
        if (alarm.label.startsWith("Undocumented error bit ")) {
            val bit = alarm.label.removePrefix("Undocumented error bit ")
            return "بیت خطای ثبت‌نشده ($bit)" to "بیت $bit رجیستر ۸۰ فعال است ولی در دفترچه BPC-02 تعریف نشده است."
        }
        return alarm.label to alarm.detail
    }

    fun registerLabel(def: RegisterDefinition): String = when (def.key) {
        Bpc02RegisterMap.DR_STATUS -> "وضعیت DR"
        Bpc02RegisterMap.DRIVE_FAIL -> "خطای درایو"
        Bpc02RegisterMap.FLOATER_LOW -> "فلوتر سطح پایین مخزن (L)"
        Bpc02RegisterMap.FLOATER_HIGH -> "فلوتر سطح بالا مخزن (H)"
        Bpc02RegisterMap.DISPLAYED_PAGE -> "شماره صفحه نمایش LCD"
        Bpc02RegisterMap.SYSTEM_PRESSURE -> "فشار سیستم"
        Bpc02RegisterMap.V_RN -> "ولتاژ فاز به نول R-N"
        Bpc02RegisterMap.V_SN -> "ولتاژ فاز به نول S-N"
        Bpc02RegisterMap.V_TN -> "ولتاژ فاز به نول T-N"
        Bpc02RegisterMap.V_RS -> "ولتاژ فاز به فاز R-S"
        Bpc02RegisterMap.V_ST -> "ولتاژ فاز به فاز S-T"
        Bpc02RegisterMap.V_TR -> "ولتاژ فاز به فاز T-R"
        Bpc02RegisterMap.MAINS_FREQUENCY -> "فرکانس برق شهر"
        Bpc02RegisterMap.DRIVE_FREQUENCY -> "فرکانس درایو (خام ۰–۱۶۰۰)"
        Bpc02RegisterMap.SYSTEM_ERROR -> "کلمه خطای سیستم (بیت‌فیلد)"
        "control_phase_activation" -> "فعال‌سازی کنترل فاز داخلی"
        "off_changing_activation" -> "تغییر نوبت در خاموشی (OFF Changing)"
        "time_changing_activation" -> "تغییر نوبت زمانی (Time Changing)"
        "reserve_auto_activation" -> "رزرو خودکار پمپ"
        "reserve_auto_type" -> "نوع کنتاکت رزرو خودکار"
        "control_status" -> "وضعیت کنترل (Control Status)"
        "sleep_gap" -> "اختلاف فشار خواب (Sleep Gap)"
        "auto_service_off_time" -> "زمان توقف اتو سرویس"
        "auto_service_on_time" -> "مدت روشن بودن اتو سرویس"
        "auto_service_interval" -> "بازه زمانی اتو سرویس"
        "modbus_address" -> "آدرس شبکه مدباس (Slave ID)"
        "multi_function_register" -> "رجیستر چندمنظوره حالت دستی (آدرس ۹۹)"
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

    fun formatRegisterValue(def: RegisterDefinition, raw: Int): String = when {
        def.enumLabels == mapOf(0 to "OFF", 1 to "ON") -> if (raw == 1) "روشن (ON)" else "خاموش (OFF)"
        def.enumLabels == mapOf(0 to "Normal", 1 to "Failed") -> if (raw == 1) "خطا (Failed)" else "عادی (Normal)"
        def.enumLabels == mapOf(0 to "Not connected", 1 to "Connected") -> if (raw == 1) "متصل" else "قطع"
        def.enumLabels == mapOf(0 to "Disabled", 1 to "Enabled") -> if (raw == 1) "فعال" else "غیرفعال"
        def.enumLabels == mapOf(0 to "NO", 1 to "NC") -> if (raw == 1) "بسته (NC)" else "باز (NO)"
        def.unit == "bar" && def.scale != null -> "%.2f بار".format(raw * def.scale)
        def.unit == "Hz" && def.scale != null -> {
            val dec = if (def.scale >= 0.1) 1 else 2
            "%.${dec}f هرتز".format(raw * def.scale)
        }
        def.unit == "V" && def.scale != null -> "$raw ولت"
        def.unit == "h" -> "$raw ساعت"
        def.unit == "min" -> "$raw دقیقه"
        def.unit == "s" && def.scale != null ->
            if (def.scale < 1.0) "%.1f ثانیه".format(raw * def.scale) else "$raw ثانیه"
        def.key == Bpc02RegisterMap.DRIVE_FREQUENCY -> "خام $raw (واحد نامشخص)"
        else -> def.format(raw)
    }

    fun formatRunTime(rt: RunTime): String = "%d ساعت %02d دقیقه %02d ثانیه".format(rt.hours, rt.minutes, rt.seconds)

    fun formatRunTimeCompact(rt: RunTime): String = "%02d:%02d:%02d".format(rt.hours, rt.minutes, rt.seconds)

    fun validationError(field: ConfigValidation.Field, fallback: String): String = when (field) {
        ConfigValidation.Field.HOST -> "آدرس IP یا نام میزبان مبدل را صحیح وارد کنید (مثلاً 192.168.1.50)"
        ConfigValidation.Field.PORT -> "پورت TCP باید عددی بین ۱ تا ۶۵۵۳۵ باشد"
        ConfigValidation.Field.UNIT_ID -> "شناسه Slave / Unit ID باید بین ۱ تا ۲۴۷ باشد"
        ConfigValidation.Field.CONNECT_TIMEOUT -> "زمان انتظار اتصال باید بین ۵۰۰ تا ۳۰۰۰۰ میلی‌ثانیه باشد"
        ConfigValidation.Field.RESPONSE_TIMEOUT -> "زمان انتظار پاسخ باید بین ۲۰۰ تا ۱۰۰۰۰ میلی‌ثانیه باشد"
        ConfigValidation.Field.POLL_INTERVAL -> "بازه نمونه‌برداری باید بین ۵۰۰ تا ۶۰۰۰۰ میلی‌ثانیه باشد"
        ConfigValidation.Field.MODEL -> "نقشه رجیستر مدل انتخاب‌شده هنوز آماده نیست"
    }.ifBlank { fallback }
}
