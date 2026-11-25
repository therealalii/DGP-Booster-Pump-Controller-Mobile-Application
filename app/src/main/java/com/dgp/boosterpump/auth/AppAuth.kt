package com.dgp.boosterpump.auth

import com.dgp.boosterpump.controller.register.Bpc02RegisterMap
import com.dgp.boosterpump.controller.register.RegisterDefinition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Logical grouping of individually customizable [OperatorPermission] entries shown in the
 * Admin Panel («دسترسی های اپراتور»).
 */
enum class PermissionGroup(
    val titleFa: String,
    val subtitleFa: String,
) {
    VIEW(
        titleFa = "مشاهده صفحات و اطلاعات",
        subtitleFa = "بخش‌هایی که اپراتور مجاز به مشاهده آن‌هاست (فقط خواندنی)",
    ),
    DAILY_CONTROL(
        titleFa = "کنترل‌های عملیاتی روزانه",
        subtitleFa = "فرمان‌ها و تغییرات روزمره ایستگاه پمپاژ روی کنترلر",
    ),
    HARDWARE_SETTINGS(
        titleFa = "تنظیمات پیشرفته کنترلر (Hardware Menu)",
        subtitleFa = "اجازه ویرایش پارامترهای تخصصی منوی BPC-02",
    ),
    SYSTEM_TOOLS(
        titleFa = "فایل تنظیمات، اتصال و عیب‌یابی",
        subtitleFa = "پشتیبان‌گیری JSON، تنظیمات مبدل شبکه و ابزارهای فنی",
    ),
}

/**
 * Every application feature and write operation that the Administrator can individually grant
 * or revoke for the [UserRole.OPERATOR] account.
 *
 * Default policy:
 * - Operators start strictly READ-ONLY ([defaultGranted] is true ONLY for non-modifying views,
 *   and false for EVERY write, command, import, gateway, and diagnostic operation).
 * - Administrators ([UserRole.ADMIN]) always retain every permission regardless of these flags.
 */
enum class OperatorPermission(
    val storageKey: String,
    val group: PermissionGroup,
    val titleFa: String,
    val titleEn: String,
    val descriptionFa: String,
    val isWriteOperation: Boolean,
    val defaultGranted: Boolean,
) {
    // 1. Read-only monitoring visibility (enabled by default so operators have read-only access)
    VIEW_PUMPS_AND_IO(
        storageKey = "perm_view_pumps_io",
        group = PermissionGroup.VIEW,
        titleFa = "مشاهده صفحه پمپ‌ها و ورودی/خروجی",
        titleEn = "Pumps & Digital I/O (Read-Only)",
        descriptionFa = "نمایش وضعیت لحظه‌ای پمپ‌ها، زمان کارکرد، خروجی رله‌ها و ورودی‌های حفاظتی",
        isWriteOperation = false,
        defaultGranted = true,
    ),
    VIEW_HARDWARE_MENU(
        storageKey = "perm_view_hardware_menu",
        group = PermissionGroup.VIEW,
        titleFa = "مشاهده منوی تنظیمات کنترلر",
        titleEn = "Hardware Menu BPC-02 (Read-Only)",
        descriptionFa = "مشاهده مقادیر ۶ بخش منوی کنترلر و راهنمای فارسی بدون امکان تغییر",
        isWriteOperation = false,
        defaultGranted = true,
    ),

    // 2. Daily operational write controls (disabled by default)
    EDIT_PRESSURE_SETPOINT(
        storageKey = "perm_edit_setpoint",
        group = PermissionGroup.DAILY_CONTROL,
        titleFa = "تغییر فشار هدف",
        titleEn = "System Pressure Setpoint",
        descriptionFa = "اجازه ویرایش فشار تنظیمی سیستم روی کنترلر",
        isWriteOperation = true,
        defaultGranted = false,
    ),
    TOGGLE_PUMPS(
        storageKey = "perm_toggle_pumps",
        group = PermissionGroup.DAILY_CONTROL,
        titleFa = "فعال / غیرفعال کردن پمپ‌ها",
        titleEn = "Pump 1–6 Service ON/OFF",
        descriptionFa = "خارج کردن یا بازگرداندن پمپ‌ها به مدار (حالت سرویس S)",
        isWriteOperation = true,
        defaultGranted = false,
    ),
    TOGGLE_DRIVE(
        storageKey = "perm_toggle_drive",
        group = PermissionGroup.DAILY_CONTROL,
        titleFa = "فعال / غیرفعال کردن درایو",
        titleEn = "Drive Service ON/OFF",
        descriptionFa = "فعال یا غیرفعال کردن اینورتر (درایو) در حالت‌های کاری ۲ و ۳",
        isWriteOperation = true,
        defaultGranted = false,
    ),
    RESET_PUMP_ERRORS(
        storageKey = "perm_reset_errors",
        group = PermissionGroup.DAILY_CONTROL,
        titleFa = "رفع خطای پمپ‌ها",
        titleEn = "Reset Pump Error",
        descriptionFa = "ارسال فرمان پاک‌سازی خطاهای ثبت‌شده کنترلر از خانه و کنترل سریع",
        isWriteOperation = true,
        defaultGranted = false,
    ),
    EDIT_CONTROL_STATUS(
        storageKey = "perm_edit_control_status",
        group = PermissionGroup.DAILY_CONTROL,
        titleFa = "تغییر حالت کنترل سیستم",
        titleEn = "Control Status (Disable / PID / ON-OFF)",
        descriptionFa = "تغییر وضعیت عملکرد کنترلر بین Disable، PID و ON/OFF",
        isWriteOperation = true,
        defaultGranted = false,
    ),

    // 3. Hardware Menu (BPC-02) advanced parameter writes (disabled by default)
    EDIT_CONTROL_PHASE(
        storageKey = "perm_edit_control_phase",
        group = PermissionGroup.HARDWARE_SETTINGS,
        titleFa = "ویرایش تنظیمات کنترل فاز",
        titleEn = "Control Phase (Voltage, Delay, Sensitivity)",
        descriptionFa = "تغییر حداقل و حداکثر ولتاژ، زمان تاخیر و حساسیت حفاظت کنترل فاز",
        isWriteOperation = true,
        defaultGranted = false,
    ),
    EDIT_FLOATER(
        storageKey = "perm_edit_floater",
        group = PermissionGroup.HARDWARE_SETTINGS,
        titleFa = "ویرایش تنظیمات فلوتر",
        titleEn = "Floater (Trigger Level, Delay)",
        descriptionFa = "تغییر سطح تحریک و زمان تاخیر قطع و وصل فلوتر مخزن",
        isWriteOperation = true,
        defaultGranted = false,
    ),
    EDIT_SENSOR_CALIBRATION(
        storageKey = "perm_edit_sensor_calib",
        group = PermissionGroup.HARDWARE_SETTINGS,
        titleFa = "ویرایش کالیبراسیون سنسور فشار",
        titleEn = "Sensor (Calib Const)",
        descriptionFa = "تغییر ضریب کالیبراسیون سنسور فشار در بخش Sensor",
        isWriteOperation = true,
        defaultGranted = false,
    ),
    EDIT_SLEEP_WAKEUP(
        storageKey = "perm_edit_sleep_wakeup",
        group = PermissionGroup.HARDWARE_SETTINGS,
        titleFa = "ویرایش تنظیمات خواب و بیداری",
        titleEn = "Sleep & Wake Up Parameters",
        descriptionFa = "تغییر فرکانس خواب، تاخیر خواب، گپ خواب و اختلاف فشار بیداری",
        isWriteOperation = true,
        defaultGranted = false,
    ),
    EDIT_PUMP_TIMING(
        storageKey = "perm_edit_pump_timing",
        group = PermissionGroup.HARDWARE_SETTINGS,
        titleFa = "ویرایش زمان‌بندی و تعویض پمپ‌ها",
        titleEn = "Add/Remove Delay, Constant %, Time Changing, Jockey",
        descriptionFa = "تغییر تاخیر افزودن/حذف پمپ، درصد پمپ ثابت، زمان نوبت‌دهی و جاکی پمپ",
        isWriteOperation = true,
        defaultGranted = false,
    ),
    EDIT_AUTO_SERVICE(
        storageKey = "perm_edit_auto_service",
        group = PermissionGroup.HARDWARE_SETTINGS,
        titleFa = "ویرایش سرویس خودکار پمپ‌ها",
        titleEn = "Auto Service (Off Time, On Time, Interval)",
        descriptionFa = "تغییر زمان‌بندی روشن‌شدن خودکار پمپ‌ها برای جلوگیری از گریپاژ",
        isWriteOperation = true,
        defaultGranted = false,
    ),
    EDIT_PID(
        storageKey = "perm_edit_pid",
        group = PermissionGroup.HARDWARE_SETTINGS,
        titleFa = "ویرایش ضرایب PID",
        titleEn = "PID Tuning (P, I, D Values)",
        descriptionFa = "تغییر ضرایب تناسبی (P)، انتگرالی (I) و مشتقی (D) کنترل فشار",
        isWriteOperation = true,
        defaultGranted = false,
    ),

    // 4. Configuration transfer, network connection & diagnostics (disabled by default)
    EXPORT_CONFIG(
        storageKey = "perm_export_config",
        group = PermissionGroup.SYSTEM_TOOLS,
        titleFa = "خروجی گرفتن از فایل تنظیمات",
        titleEn = "Export Configuration (JSON)",
        descriptionFa = "ذخیره و اشتراک‌گذاری فایل پشتیبان تنظیمات کنترلر با فرمت JSON",
        isWriteOperation = false,
        defaultGranted = false,
    ),
    IMPORT_CONFIG(
        storageKey = "perm_import_config",
        group = PermissionGroup.SYSTEM_TOOLS,
        titleFa = "بارگذاری و اعمال فایل تنظیمات",
        titleEn = "Import Configuration (JSON)",
        descriptionFa = "وارد کردن فایل JSON و اعمال پارامترهایی که مجوز ویرایش آن‌ها برای اپراتور فعال است",
        isWriteOperation = true,
        defaultGranted = false,
    ),
    MANAGE_GATEWAY(
        storageKey = "perm_manage_gateway",
        group = PermissionGroup.SYSTEM_TOOLS,
        titleFa = "تنظیمات اتصال و مبدل شبکه",
        titleEn = "Gateway & Connection Settings",
        descriptionFa = "دسترسی به صفحه اتصال و تغییر IP، Port، آدرس Modbus، زمان‌بندی و حالت نمایشی",
        isWriteOperation = true,
        defaultGranted = false,
    ),
    ACCESS_DIAGNOSTICS(
        storageKey = "perm_access_diagnostics",
        group = PermissionGroup.SYSTEM_TOOLS,
        titleFa = "ابزار عیب‌یابی فنی ارتباط",
        titleEn = "Technical Diagnostics (Probe, Log, Map)",
        descriptionFa = "دسترسی به تست خواندن دستی Modbus، لاگ فریم‌های TX/RX و نقشه رجیسترها",
        isWriteOperation = false,
        defaultGranted = false,
    ),
    ;
}

/**
 * Immutable snapshot of permissions currently granted to the [UserRole.OPERATOR] account.
 *
 * Defaults to [READ_ONLY_DEFAULT], where operators can only monitor live data and view
 * settings without modifying or writing anything.
 */
data class OperatorPermissions(
    val granted: Set<OperatorPermission> = DEFAULT_GRANTED,
) {
    operator fun contains(permission: OperatorPermission): Boolean = permission in granted

    fun has(permission: OperatorPermission): Boolean = permission in granted

    fun withPermission(permission: OperatorPermission, enabled: Boolean): OperatorPermissions =
        copy(granted = if (enabled) granted + permission else granted - permission)

    val canViewPumpsAndIo: Boolean get() = OperatorPermission.VIEW_PUMPS_AND_IO in granted
    val canViewHardwareMenu: Boolean get() = OperatorPermission.VIEW_HARDWARE_MENU in granted
    val canEditPressureSetpoint: Boolean get() = OperatorPermission.EDIT_PRESSURE_SETPOINT in granted
    val canTogglePumps: Boolean get() = OperatorPermission.TOGGLE_PUMPS in granted
    val canToggleDrive: Boolean get() = OperatorPermission.TOGGLE_DRIVE in granted
    val canResetPumpErrors: Boolean get() = OperatorPermission.RESET_PUMP_ERRORS in granted
    val canEditControlStatus: Boolean get() = OperatorPermission.EDIT_CONTROL_STATUS in granted
    val canExportConfig: Boolean get() = OperatorPermission.EXPORT_CONFIG in granted
    val canImportConfig: Boolean get() = OperatorPermission.IMPORT_CONFIG in granted
    val canManageGateway: Boolean get() = OperatorPermission.MANAGE_GATEWAY in granted
    val canAccessDiagnostics: Boolean get() = OperatorPermission.ACCESS_DIAGNOSTICS in granted

    val hasAnyWritePermission: Boolean get() = granted.any { it.isWriteOperation }

    val hasAnyHardwareMenuWrite: Boolean
        get() = granted.any { it.group == PermissionGroup.HARDWARE_SETTINGS }

    val hasAnyDailyControlWrite: Boolean
        get() = granted.any { it.group == PermissionGroup.DAILY_CONTROL }

    val grantedWriteCount: Int get() = granted.count { it.isWriteOperation }

    val totalWriteCount: Int get() = OperatorPermission.entries.count { it.isWriteOperation }

    val grantedTotalCount: Int get() = granted.size

    val totalPermissionCount: Int get() = OperatorPermission.entries.size

    /**
     * Checks whether this permission set allows writing to [definition].
     * Always returns false for hardware read-only / locked registers.
     */
    fun canWriteRegister(definition: RegisterDefinition): Boolean {
        if (!definition.isWritable) return false
        val required = requiredPermissionForRegister(definition.key) ?: return false
        return required in granted
    }

    companion object {
        /** Default read-only operator permissions: monitoring views ON, every write/action OFF. */
        val DEFAULT_GRANTED: Set<OperatorPermission> =
            OperatorPermission.entries.filter { it.defaultGranted }.toSet()

        val READ_ONLY_DEFAULT = OperatorPermissions(DEFAULT_GRANTED)

        /** Full unrestricted permission set always retained by [UserRole.ADMIN]. */
        val FULL_ADMIN = OperatorPermissions(OperatorPermission.entries.toSet())

        /** Optional admin convenience preset granting everyday operator controls. */
        val DAILY_OPERATOR_PRESET = OperatorPermissions(
            DEFAULT_GRANTED + setOf(
                OperatorPermission.EDIT_PRESSURE_SETPOINT,
                OperatorPermission.TOGGLE_PUMPS,
                OperatorPermission.TOGGLE_DRIVE,
                OperatorPermission.RESET_PUMP_ERRORS,
            ),
        )

        private val PUMP_ACTIVATION_KEYS: Set<String> =
            (1..Bpc02RegisterMap.STATUS_PUMP_COUNT).map { Bpc02RegisterMap.pumpActivationKey(it) }.toSet()

        /**
         * Authoritative mapping from every writable BPC-02 register key to its exact
         * [OperatorPermission]. Returns null for read-only, locked, or unknown registers.
         */
        fun requiredPermissionForRegister(registerKey: String): OperatorPermission? = when (registerKey) {
            Bpc02RegisterMap.SYSTEM_PRESSURE_SETPOINT -> OperatorPermission.EDIT_PRESSURE_SETPOINT
            Bpc02RegisterMap.DRIVE_ACTIVATION -> OperatorPermission.TOGGLE_DRIVE
            Bpc02RegisterMap.MULTI_FUNCTION_REGISTER -> OperatorPermission.RESET_PUMP_ERRORS
            Bpc02RegisterMap.CONTROL_STATUS -> OperatorPermission.EDIT_CONTROL_STATUS
            "control_phase_min_voltage",
            "control_phase_max_voltage",
            "control_phase_delay",
            "control_phase_sensitivity",
            -> OperatorPermission.EDIT_CONTROL_PHASE
            "floater_trigger_level",
            "floater_delay",
            -> OperatorPermission.EDIT_FLOATER
            "sensor_calibration_value" -> OperatorPermission.EDIT_SENSOR_CALIBRATION
            "sleep_frequency",
            "sleep_off_delay",
            "sleep_gap",
            "wakeup_differential",
            "wakeup_on_delay",
            -> OperatorPermission.EDIT_SLEEP_WAKEUP
            "constant_pump_percent",
            "add_delay",
            "remove_delay",
            "jockey_type",
            "time_changing_time",
            -> OperatorPermission.EDIT_PUMP_TIMING
            "auto_service_off_time",
            "auto_service_on_time",
            "auto_service_interval",
            -> OperatorPermission.EDIT_AUTO_SERVICE
            "pid_p_value",
            "pid_i_value",
            "pid_d_value",
            -> OperatorPermission.EDIT_PID
            else -> if (registerKey in PUMP_ACTIVATION_KEYS) {
                OperatorPermission.TOGGLE_PUMPS
            } else {
                null
            }
        }
    }
}

/**
 * Two-tier access model for the DGP Booster Pump Controller application:
 *
 * - [ADMIN] (Full Access): Always retains unrestricted access to monitoring, all controller
 *   settings, converter connection setup, technical diagnostics, and managing operator permissions.
 * - [OPERATOR] (Customizable Access): Read-only by default (cannot modify or write anything).
 *   Individual write and feature permissions can be explicitly granted or revoked by the
 *   Administrator via «دسترسی های اپراتور».
 */
enum class UserRole(
    val titleFa: String,
    val subtitleFa: String,
    val canOperateBasicControls: Boolean,
    val canEditAdvancedSettings: Boolean,
    val canManageConnection: Boolean,
    val canAccessDiagnostics: Boolean,
) {
    ADMIN(
        titleFa = "مدیر سیستم",
        subtitleFa = "دسترسی کامل به مانیتورینگ، تمامی تنظیمات کنترلر، مبدل شبکه، عیب‌یابی و مدیریت دسترسی اپراتور",
        canOperateBasicControls = true,
        canEditAdvancedSettings = true,
        canManageConnection = true,
        canAccessDiagnostics = true,
    ),
    OPERATOR(
        titleFa = "اپراتور (کاربر)",
        subtitleFa = "دسترسی پیش‌فرض فقط خواندنی — مجوزهای ویرایش توسط مدیر سیستم قابل تنظیم است",
        canOperateBasicControls = false,
        canEditAdvancedSettings = false,
        canManageConnection = false,
        canAccessDiagnostics = false,
    ),
    ;

    /** Only [ADMIN] can view or customize operator permissions. */
    val canManageOperatorPermissions: Boolean get() = this == ADMIN

    /** Legacy convenience alias reflecting default full configuration write access. */
    val canWriteConfig: Boolean get() = canEditAdvancedSettings

    /** Legacy convenience alias reflecting default converter/gateway configuration access. */
    val canEditGateway: Boolean get() = canManageConnection

    /** Legacy convenience alias reflecting default technical diagnostics access. */
    val canRunDiagnostics: Boolean get() = canAccessDiagnostics

    /**
     * Resolves the effective [OperatorPermissions] for this role:
     * - [ADMIN] ALWAYS retains [OperatorPermissions.FULL_ADMIN].
     * - [OPERATOR] receives the admin-configured [operatorPermissions].
     */
    fun effectivePermissions(
        operatorPermissions: OperatorPermissions = OperatorPermissions.READ_ONLY_DEFAULT,
    ): OperatorPermissions = when (this) {
        ADMIN -> OperatorPermissions.FULL_ADMIN
        OPERATOR -> operatorPermissions
    }

    /**
     * Authoritative per-register write permission check enforced across UI, ViewModels, and
     * JSON configuration import.
     *
     * By default [operatorPermissions] is [OperatorPermissions.READ_ONLY_DEFAULT], so [OPERATOR]
     * cannot write any register unless explicitly granted by [ADMIN].
     */
    fun canWriteRegister(
        definition: RegisterDefinition,
        operatorPermissions: OperatorPermissions = OperatorPermissions.READ_ONLY_DEFAULT,
    ): Boolean {
        if (!definition.isWritable) return false
        return when (this) {
            ADMIN -> true
            OPERATOR -> operatorPermissions.canWriteRegister(definition)
        }
    }
}

/** Authenticated session user. */
data class AppUser(
    val username: String,
    val displayNameFa: String,
    val role: UserRole,
)

/** Shared in-memory holder of the active authenticated user across ViewModels. */
class AuthSessionStore(initialUser: AppUser? = null) {
    private val _currentUser = MutableStateFlow(initialUser)
    val currentUser: StateFlow<AppUser?> = _currentUser.asStateFlow()

    fun login(user: AppUser) {
        _currentUser.value = user
    }

    fun logout() {
        _currentUser.value = null
    }
}

/** Hardcoded offline account entry (documented in APP_USERS.txt). */
internal data class AccountRecord(
    val username: String,
    val password: String,
    val displayNameFa: String,
    val role: UserRole,
)

/**
 * Local offline authenticator for field & plant-room usage (works without internet access).
 * All credentials are documented in APP_USERS.txt in the repository root.
 */
object AuthRepository {

    private val accounts: List<AccountRecord> = listOf(
        AccountRecord(
            username = "admin",
            password = "Dgp@2026",
            displayNameFa = "مدیر سیستم",
            role = UserRole.ADMIN,
        ),
        AccountRecord(
            username = "operator",
            password = "Pump@1234",
            displayNameFa = "اپراتور بهره‌بردار",
            role = UserRole.OPERATOR,
        ),
    )

    /**
     * Validates [username] (case-insensitive, trimmed) and [password] (exact match).
     * Returns [AppUser] on success, or null if credentials do not match.
     */
    fun authenticate(username: String, password: String): AppUser? {
        val cleanUser = username.trim()
        if (cleanUser.isEmpty() || password.isEmpty()) return null
        val match = accounts.firstOrNull {
            it.username.equals(cleanUser, ignoreCase = true) && it.password == password
        } ?: return null
        return AppUser(
            username = match.username,
            displayNameFa = match.displayNameFa,
            role = match.role,
        )
    }

    /** Number of built-in user accounts (Administrator & Operator). */
    val accountCount: Int get() = accounts.size
}
