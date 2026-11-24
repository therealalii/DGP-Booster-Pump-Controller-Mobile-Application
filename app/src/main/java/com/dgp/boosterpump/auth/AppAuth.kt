package com.dgp.boosterpump.auth

import com.dgp.boosterpump.controller.register.Bpc02RegisterMap
import com.dgp.boosterpump.controller.register.RegisterDefinition

/**
 * Two-tier access model for the DGP Booster Pump Controller application:
 *
 * - [ADMIN] (Full Access): Complete access to monitoring, all controller settings, converter
 *   connection setup, and technical diagnostics.
 * - [OPERATOR] (Client Access): Safe daily operation (live monitoring, setpoint adjustment,
 *   pump enable/disable for maintenance, and fault reset) without access to advanced hardware
 *   parameters, converter network settings, or technical diagnostics.
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
        subtitleFa = "دسترسی کامل به مانیتورینگ، تمامی تنظیمات کنترلر، مبدل شبکه و عیب‌یابی",
        canOperateBasicControls = true,
        canEditAdvancedSettings = true,
        canManageConnection = true,
        canAccessDiagnostics = true,
    ),
    OPERATOR(
        titleFa = "اپراتور (کاربر)",
        subtitleFa = "پایش وضعیت ایستگاه، تنظیم فشار، فعال/غیرفعال کردن پمپ‌ها و رفع خطا",
        canOperateBasicControls = true,
        canEditAdvancedSettings = false,
        canManageConnection = false,
        canAccessDiagnostics = false,
    ),
    ;

    /** Legacy convenience alias reflecting full configuration write access. */
    val canWriteConfig: Boolean get() = canEditAdvancedSettings

    /** Legacy convenience alias reflecting converter/gateway configuration access. */
    val canEditGateway: Boolean get() = canManageConnection

    /** Legacy convenience alias reflecting technical diagnostics access. */
    val canRunDiagnostics: Boolean get() = canAccessDiagnostics

    /**
     * Authoritative per-register write permission check enforced across both UI and ViewModel.
     */
    fun canWriteRegister(definition: RegisterDefinition): Boolean {
        if (!definition.isWritable) return false
        return when (this) {
            ADMIN -> true
            OPERATOR -> definition.key in OPERATOR_WRITABLE_KEYS
        }
    }

    companion object {
        /** Normal daily operational parameters permitted for Client Access (Operator). */
        val OPERATOR_WRITABLE_KEYS: Set<String> = buildSet {
            add(Bpc02RegisterMap.SYSTEM_PRESSURE_SETPOINT)
            add(Bpc02RegisterMap.MULTI_FUNCTION_REGISTER)
            add(Bpc02RegisterMap.DRIVE_ACTIVATION)
            for (n in 1..Bpc02RegisterMap.STATUS_PUMP_COUNT) {
                add(Bpc02RegisterMap.pumpActivationKey(n))
            }
        }
    }
}

/** Authenticated session user. */
data class AppUser(
    val username: String,
    val displayNameFa: String,
    val role: UserRole,
)

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
