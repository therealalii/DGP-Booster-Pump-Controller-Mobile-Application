package com.dgp.boosterpump.config

import com.dgp.boosterpump.auth.UserRole
import com.dgp.boosterpump.controller.Bpc02Catalog
import com.dgp.boosterpump.controller.ControllerModel
import com.dgp.boosterpump.controller.ControllerSnapshot
import com.dgp.boosterpump.controller.RegisterReading
import com.dgp.boosterpump.controller.register.Bpc02RegisterMap
import com.dgp.boosterpump.controller.register.RegisterDefinition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Metadata parsed from an exported DGP controller configuration file. */
data class ExportedConfigMetadata(
    val schemaVersion: Int,
    val format: String,
    val controllerModelName: String,
    val exportedAt: String?,
    val appVersion: String?,
    val serialNumber: Int?,
    val wiringMode: Int?,
    val numberOfPumps: Int?,
    val numberOfDrives: Int?,
)

/** One validated writable parameter ready to be applied to the controller. */
data class ImportParameterChange(
    val definition: RegisterDefinition,
    val currentValue: Int?,
    val targetValue: Int,
) {
    val isChanged: Boolean get() = currentValue != targetValue
}

/** One read-only parameter included in the file as project reference (skipped on write). */
data class ReadOnlyReferenceEntry(
    val definition: RegisterDefinition,
    val currentValue: Int?,
    val fileValue: Int,
) {
    val differsFromController: Boolean get() = currentValue != null && currentValue != fileValue
}

/** Severity of a validation finding discovered during configuration import. */
enum class ValidationSeverity { ERROR, WARNING }

/** One structured validation error or compatibility warning reported to the user before apply. */
data class ConfigValidationIssue(
    val severity: ValidationSeverity,
    val parameterKey: String?,
    val title: String,
    val detailFa: String,
)

/**
 * Full outcome of parsing and validating an imported JSON configuration file against the
 * connected controller model, current live values, and signed-in [UserRole].
 *
 * Safety invariant: [canApply] is true ONLY when there are zero [ValidationSeverity.ERROR]
 * findings AND at least one permitted writable parameter in [writableEntries]. Invalid or
 * incompatible files can NEVER be partially applied.
 */
data class ConfigImportValidationResult(
    val metadata: ExportedConfigMetadata?,
    val writableEntries: List<ImportParameterChange>,
    val readOnlyEntries: List<ReadOnlyReferenceEntry>,
    val roleRestrictedKeys: List<String>,
    val issues: List<ConfigValidationIssue>,
) {
    val errors: List<ConfigValidationIssue> get() = issues.filter { it.severity == ValidationSeverity.ERROR }
    val warnings: List<ConfigValidationIssue> get() = issues.filter { it.severity == ValidationSeverity.WARNING }
    val changesToApply: List<ImportParameterChange> get() = writableEntries.filter { it.isChanged }
    val unchangedWritableCount: Int get() = writableEntries.count { !it.isChanged }

    /** Strictly blocks applying if any structural, model, type, range, or key error exists. */
    val canApply: Boolean get() = errors.isEmpty() && writableEntries.isNotEmpty()
}

/**
 * Portable JSON exporter and strict validator for DGP BPC-02 controller configurations.
 *
 * Exported files include schema version, controller model, export timestamp, station architecture
 * summary, and all read hardware configuration parameters (both writable settings and read-only
 * project reference parameters such as Wiring Mode, Main Pumps, Drive Count, Sensor Type, and MFx).
 */
object ControllerConfigTransfer {

    const val SCHEMA_VERSION = 1
    const val FORMAT_ID = "dgp-bpc-config"
    const val MAX_IMPORT_BYTES = 512 * 1024

    /** Generates a clean default filename such as `DGP_BPC02_Config_20261125_1430.json`. */
    fun suggestedFileName(
        model: ControllerModel = ControllerModel.BPC02,
        timestampMillis: Long = System.currentTimeMillis(),
    ): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date(timestampMillis))
        return "DGP_${model.name}_Config_$stamp.json"
    }

    /**
     * Serializes the controller's current configuration snapshot into a human-readable, portable
     * JSON document. Combines [configReadings] with freshest [liveSnapshot] values when available.
     */
    fun exportToJson(
        model: ControllerModel,
        configReadings: List<RegisterReading>,
        liveSnapshot: ControllerSnapshot?,
        appVersion: String = "1.0.0",
        timestampMillis: Long = System.currentTimeMillis(),
    ): String {
        val mergedValues = LinkedHashMap<String, Int>()
        configReadings.forEach { reading ->
            reading.raw?.let { mergedValues[reading.definition.key] = it }
        }
        liveSnapshot?.readings?.forEach { reading ->
            if (reading.raw != null && reading.definition.key in Bpc02ConfigKeys) {
                mergedValues[reading.definition.key] = reading.raw
            }
        }

        val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val deviceInfo = LinkedHashMap<String, JsonValue>().apply {
            mergedValues["serial_number"]?.let { put("serialNumber", JsonValue.IntNum(it.toLong())) }
            (liveSnapshot?.wiringMode ?: mergedValues["wiring_mode"])?.let {
                put("wiringMode", JsonValue.IntNum(it.toLong()))
            }
            (liveSnapshot?.configuredPumpCount ?: mergedValues["number_of_pump"])?.let {
                put("numberOfPumps", JsonValue.IntNum(it.toLong()))
            }
            (liveSnapshot?.variablePumpCount ?: mergedValues["number_of_variable_pump"])?.let {
                put("numberOfDrives", JsonValue.IntNum(it.toLong()))
            }
        }

        val parametersObj = LinkedHashMap<String, JsonValue>()
        for (def in Bpc02RegisterMap.configuration) {
            val raw = mergedValues[def.key] ?: continue
            val entry = LinkedHashMap<String, JsonValue>().apply {
                put("name", JsonValue.Str(Bpc02Catalog.fullParameterName(def)))
                put("raw", JsonValue.IntNum(raw.toLong()))
                put("display", JsonValue.Str(Bpc02Catalog.formatValue(def, raw)))
                put("writable", JsonValue.Bool(def.isWritable))
            }
            parametersObj[def.key] = JsonValue.Obj(entry)
        }

        val root = LinkedHashMap<String, JsonValue>().apply {
            put("schemaVersion", JsonValue.IntNum(SCHEMA_VERSION.toLong()))
            put("format", JsonValue.Str(FORMAT_ID))
            put("controllerModel", JsonValue.Str(model.name))
            put("exportedAt", JsonValue.Str(isoFormatter.format(Date(timestampMillis))))
            put("appVersion", JsonValue.Str(appVersion))
            put("deviceInfo", JsonValue.Obj(deviceInfo))
            put("parameters", JsonValue.Obj(parametersObj))
        }

        return MinimalJson.stringify(JsonValue.Obj(root))
    }

    /**
     * Parses and strictly validates [jsonText] before any write is ever allowed:
     * - Verifies valid JSON object root, `format`, `schemaVersion`, and `controllerModel`.
     * - Verifies every parameter key exists in `Bpc02RegisterMap.configuration`.
     * - Verifies exact integer data type (rejects strings, floats, booleans, nulls).
     * - Verifies value is within documented `minValue..maxValue` and valid `enumLabels` codes.
     * - Flags hardware architecture mismatches (Wiring Mode, Number of Pumps, Drives, Sensor Type)
     *   as clear warnings so technicians never accidentally apply a mismatched station profile.
     * - Enforces [userRole] write permissions.
     */
    fun parseAndValidate(
        jsonText: String,
        targetModel: ControllerModel,
        currentValuesByKey: Map<String, Int?>,
        userRole: UserRole,
    ): ConfigImportValidationResult {
        val issues = mutableListOf<ConfigValidationIssue>()

        if (jsonText.isBlank()) {
            issues += ConfigValidationIssue(
                severity = ValidationSeverity.ERROR,
                parameterKey = null,
                title = "Empty File",
                detailFa = "فایل انتخاب‌شده خالی است و هیچ تنظیماتی در آن وجود ندارد.",
            )
            return ConfigImportValidationResult(null, emptyList(), emptyList(), emptyList(), issues)
        }

        val rootVal = try {
            MinimalJson.parse(jsonText)
        } catch (e: JsonParseException) {
            issues += ConfigValidationIssue(
                severity = ValidationSeverity.ERROR,
                parameterKey = null,
                title = "Invalid JSON Format",
                detailFa = "ساختار فایل JSON معتبر نیست یا آسیب دیده است (${e.message}).",
            )
            return ConfigImportValidationResult(null, emptyList(), emptyList(), emptyList(), issues)
        }

        val root = rootVal as? JsonValue.Obj ?: run {
            issues += ConfigValidationIssue(
                severity = ValidationSeverity.ERROR,
                parameterKey = null,
                title = "Invalid Root Structure",
                detailFa = "ساختار اصلی فایل باید یک آبجکت JSON معتبر باشد.",
            )
            return ConfigImportValidationResult(null, emptyList(), emptyList(), emptyList(), issues)
        }

        // 1. Validate format & schemaVersion
        val formatStr = (root.entries["format"] as? JsonValue.Str)?.value
        if (formatStr != FORMAT_ID) {
            issues += ConfigValidationIssue(
                severity = ValidationSeverity.ERROR,
                parameterKey = "format",
                title = "Unrecognized File Format",
                detailFa = "این فایل یک فایل پشتیبان رسمی تنظیمات کنترلر DGP نیست (format نامعتبر است).",
            )
        }

        val schemaNum = (root.entries["schemaVersion"] as? JsonValue.IntNum)?.value?.toInt()
        if (schemaNum == null) {
            issues += ConfigValidationIssue(
                severity = ValidationSeverity.ERROR,
                parameterKey = "schemaVersion",
                title = "Missing Schema Version",
                detailFa = "نسخه ساختار فایل (schemaVersion) مشخص نشده یا عددی نیست.",
            )
        } else if (schemaNum < 1 || schemaNum > SCHEMA_VERSION) {
            issues += ConfigValidationIssue(
                severity = ValidationSeverity.ERROR,
                parameterKey = "schemaVersion",
                title = "Unsupported Schema Version (v$schemaNum)",
                detailFa = "نسخه این فایل تنظیمات (v$schemaNum) توسط نسخه فعلی برنامه پشتیبانی نمی‌شود.",
            )
        }

        // 2. Validate controller model compatibility
        val modelName = (root.entries["controllerModel"] as? JsonValue.Str)?.value
        if (modelName.isNullOrBlank()) {
            issues += ConfigValidationIssue(
                severity = ValidationSeverity.ERROR,
                parameterKey = "controllerModel",
                title = "Missing Controller Model",
                detailFa = "مدل کنترلر در فایل مشخص نشده است و امکان بررسی سازگاری وجود ندارد.",
            )
        } else if (!modelName.equals(targetModel.name, ignoreCase = true)) {
            issues += ConfigValidationIssue(
                severity = ValidationSeverity.ERROR,
                parameterKey = "controllerModel",
                title = "Incompatible Controller Model ($modelName ≠ ${targetModel.name})",
                detailFa = "این فایل متعلق به کنترلر $modelName است و با کنترلر فعلی (${targetModel.label}) سازگار نیست.",
            )
        }

        val deviceInfoObj = root.entries["deviceInfo"] as? JsonValue.Obj
        val metadata = ExportedConfigMetadata(
            schemaVersion = schemaNum ?: -1,
            format = formatStr ?: "unknown",
            controllerModelName = modelName ?: "Unknown",
            exportedAt = (root.entries["exportedAt"] as? JsonValue.Str)?.value,
            appVersion = (root.entries["appVersion"] as? JsonValue.Str)?.value,
            serialNumber = (deviceInfoObj?.entries?.get("serialNumber") as? JsonValue.IntNum)?.value?.toInt(),
            wiringMode = (deviceInfoObj?.entries?.get("wiringMode") as? JsonValue.IntNum)?.value?.toInt(),
            numberOfPumps = (deviceInfoObj?.entries?.get("numberOfPumps") as? JsonValue.IntNum)?.value?.toInt(),
            numberOfDrives = (deviceInfoObj?.entries?.get("numberOfDrives") as? JsonValue.IntNum)?.value?.toInt(),
        )

        // 3. Validate parameters object
        val paramsVal = root.entries["parameters"]
        val paramsObj = paramsVal as? JsonValue.Obj
        if (paramsObj == null || paramsObj.entries.isEmpty()) {
            issues += ConfigValidationIssue(
                severity = ValidationSeverity.ERROR,
                parameterKey = "parameters",
                title = "Missing Parameters",
                detailFa = "بخش parameters در فایل وجود ندارد یا خالی است.",
            )
            return ConfigImportValidationResult(metadata, emptyList(), emptyList(), emptyList(), issues)
        }

        val configDefByKey = Bpc02RegisterMap.configuration.associateBy { it.key }
        val writableEntries = mutableListOf<ImportParameterChange>()
        val readOnlyEntries = mutableListOf<ReadOnlyReferenceEntry>()
        val roleRestrictedKeys = mutableListOf<String>()

        for ((key, rawJson) in paramsObj.entries) {
            val def = configDefByKey[key]
            if (def == null) {
                issues += ConfigValidationIssue(
                    severity = ValidationSeverity.ERROR,
                    parameterKey = key,
                    title = "Unknown Parameter: $key",
                    detailFa = "پارامتر «$key» در نقشه رسمی کنترلر ${targetModel.label} تعریف نشده و پشتیبانی نمی‌شود.",
                )
                continue
            }

            // Accept either canonical object `{ "raw": 450, ... }` or direct integer shorthand `450`
            val rawValueNode: JsonValue? = when (rawJson) {
                is JsonValue.Obj -> rawJson.entries["raw"]
                else -> rawJson
            }
            val parsedLong: Long? = (rawValueNode as? JsonValue.IntNum)?.value
            if (parsedLong == null || parsedLong < Int.MIN_VALUE || parsedLong > Int.MAX_VALUE) {
                issues += ConfigValidationIssue(
                    severity = ValidationSeverity.ERROR,
                    parameterKey = key,
                    title = "Invalid Data Type: ${Bpc02Catalog.fullParameterName(def)}",
                    detailFa = "مقدار پارامتر ${Bpc02Catalog.fullParameterName(def)} باید یک عدد صحیح معتبر باشد.",
                )
                continue
            }
            val rawInt = parsedLong.toInt()

            // Range & enum bounds validation
            val min = def.minValue ?: 0
            val max = def.maxValue ?: 0xFFFF
            if (def.enumLabels.isNotEmpty()) {
                if (rawInt !in def.enumLabels.keys) {
                    val allowed = def.enumLabels.keys.sorted().joinToString(", ") {
                        "$it=${Bpc02Catalog.enumOptionLabel(def, it)}"
                    }
                    issues += ConfigValidationIssue(
                        severity = ValidationSeverity.ERROR,
                        parameterKey = key,
                        title = "Invalid Option: ${Bpc02Catalog.fullParameterName(def)}",
                        detailFa = "مقدار $rawInt برای ${Bpc02Catalog.fullParameterName(def)} مجاز نیست (مقادیر مجاز: $allowed).",
                    )
                    continue
                }
            } else if (rawInt !in min..max) {
                issues += ConfigValidationIssue(
                    severity = ValidationSeverity.ERROR,
                    parameterKey = key,
                    title = "Out of Range: ${Bpc02Catalog.fullParameterName(def)}",
                    detailFa = "مقدار ${Bpc02Catalog.formatValue(def, rawInt)} خارج از بازه مجاز (${Bpc02Catalog.formatRange(def)}) است.",
                )
                continue
            }

            val currentVal = currentValuesByKey[key]
            if (!def.isWritable) {
                val entry = ReadOnlyReferenceEntry(def, currentVal, rawInt)
                readOnlyEntries += entry
                if (entry.differsFromController && key in ARCHITECTURE_WARNING_KEYS) {
                    issues += ConfigValidationIssue(
                        severity = ValidationSeverity.WARNING,
                        parameterKey = key,
                        title = "Hardware Mismatch: ${Bpc02Catalog.fullParameterName(def)}",
                        detailFa = "در فایل «${Bpc02Catalog.formatValue(def, rawInt)}» ثبت شده اما روی کنترلر فعلی «${Bpc02Catalog.formatValue(def, currentVal!!)}» است. این پارامتر فقط از روی نمایشگر کنترلر قابل تغییر است.",
                    )
                }
            } else if (!userRole.canWriteRegister(def)) {
                roleRestrictedKeys += key
                issues += ConfigValidationIssue(
                    severity = ValidationSeverity.ERROR,
                    parameterKey = key,
                    title = "Permission Required: ${Bpc02Catalog.fullParameterName(def)}",
                    detailFa = "اعمال پارامتر ${Bpc02Catalog.fullParameterName(def)} نیازمند سطح دسترسی «مدیر سیستم» است.",
                )
            } else {
                writableEntries += ImportParameterChange(def, currentVal, rawInt)
            }
        }

        if (writableEntries.isEmpty() && issues.none { it.severity == ValidationSeverity.ERROR }) {
            issues += ConfigValidationIssue(
                severity = ValidationSeverity.ERROR,
                parameterKey = null,
                title = "No Writable Parameters",
                detailFa = "هیچ پارامتر قابل نوشتنی در فایل برای اعمال روی کنترلر وجود ندارد.",
            )
        }

        return ConfigImportValidationResult(
            metadata = metadata,
            writableEntries = writableEntries,
            readOnlyEntries = readOnlyEntries,
            roleRestrictedKeys = roleRestrictedKeys,
            issues = issues,
        )
    }

    private val Bpc02ConfigKeys: Set<String> = Bpc02RegisterMap.configuration.map { it.key }.toSet()

    private val ARCHITECTURE_WARNING_KEYS = setOf(
        "wiring_mode",
        "number_of_pump",
        "number_of_variable_pump",
        "pressure_sensor_input_type",
        "pressure_sensor_output_type",
        "control_phase_type",
        "floater_type",
    )
}
