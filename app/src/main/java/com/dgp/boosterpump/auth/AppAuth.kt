package com.dgp.boosterpump.auth

/**
 * Operational access roles for the DGP Booster Pump Controller application.
 *
 * - [ADMIN] & [INSTALLER] ("نصاب"): Full commissioning & verified FC05/FC06 hardware config writes + ES 487 gateway config.
 * - [SERVICE]: Diagnostics, live monitoring & read-only hardware config inspection.
 * - [OPERATOR] & [GUEST]: Safe live monitoring & read-only status inspection (no config writes or gateway changes).
 */
enum class UserRole(
    val titleFa: String,
    val subtitleFa: String,
    val canWriteConfig: Boolean,
    val canEditGateway: Boolean,
    val canRunDiagnostics: Boolean,
) {
    ADMIN(
        titleFa = "مدیر سیستم",
        subtitleFa = "دسترسی کامل به مانیتورینگ، تنظیمات سخت‌افزار (FC05/06)، عیب‌یابی و مبدل ES 487",
        canWriteConfig = true,
        canEditGateway = true,
        canRunDiagnostics = true,
    ),
    INSTALLER(
        titleFa = "نصاب و راه‌انداز",
        subtitleFa = "دسترسی کامل نصب، پیکربندی مبدل سمین ES 487 و ویرایش پارامترهای سخت‌افزاری BPC02",
        canWriteConfig = true,
        canEditGateway = true,
        canRunDiagnostics = true,
    ),
    SERVICE(
        titleFa = "تکنسین سرویس",
        subtitleFa = "مانیتورینگ، ترمینال عیب‌یابی TX/RX، تست خواندن و مشاهده فقط‌خواندنی تنظیمات",
        canWriteConfig = false,
        canEditGateway = false,
        canRunDiagnostics = true,
    ),
    OPERATOR(
        titleFa = "اپراتور بهره‌بردار",
        subtitleFa = "مشاهده زنده فشار، مخزن، پمپ‌ها، آلارم‌ها و وضعیت تنظیمات (بدون امکان تغییر سخت‌افزار)",
        canWriteConfig = false,
        canEditGateway = false,
        canRunDiagnostics = false,
    ),
    GUEST(
        titleFa = "کاربر مهمان (دمو)",
        subtitleFa = "دسترسی نمایشی ایمن برای بررسی مانیتورینگ و حالت شبیه‌سازی (فقط خواندنی)",
        canWriteConfig = false,
        canEditGateway = false,
        canRunDiagnostics = true,
    ),
}

/** Authenticated operator session model. */
data class AppUser(
    val username: String,
    val displayNameFa: String,
    val role: UserRole,
)

/** Hardcoded account entry (documented in APP_USERS.txt). */
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
            displayNameFa = "مدیر کل سامانه",
            role = UserRole.ADMIN,
        ),
        AccountRecord(
            username = "nasab",
            password = "Nasab@1405",
            displayNameFa = "کارشناس نصب و راه‌اندازی",
            role = UserRole.INSTALLER,
        ),
        AccountRecord(
            username = "installer",
            password = "Inst@487",
            displayNameFa = "نصاب فنی DGP",
            role = UserRole.INSTALLER,
        ),
        AccountRecord(
            username = "service",
            password = "Serv@2026",
            displayNameFa = "تکنسین سرویس و نگهداری",
            role = UserRole.SERVICE,
        ),
        AccountRecord(
            username = "operator",
            password = "Pump@1234",
            displayNameFa = "اپراتور موتورخانه",
            role = UserRole.OPERATOR,
        ),
        AccountRecord(
            username = "guest",
            password = "1234",
            displayNameFa = "کاربر مهمان (دمو)",
            role = UserRole.GUEST,
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

    /** Returns count of configured built-in accounts (without exposing passwords). */
    val accountCount: Int get() = accounts.size
}
