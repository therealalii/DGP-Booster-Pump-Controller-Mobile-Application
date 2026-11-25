package com.dgp.boosterpump.auth

import com.dgp.boosterpump.controller.register.Bpc02RegisterMap
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRepositoryTest {

    @Test
    fun authenticatesStrictlyTwoAccessLevelsAdministratorAndOperator() {
        assertEquals(2, AuthRepository.accountCount)

        val admin = AuthRepository.authenticate("  ADMIN ", "Dgp@2026")
        assertNotNull(admin)
        assertEquals(UserRole.ADMIN, admin!!.role)
        assertTrue(admin.role.canOperateBasicControls)
        assertTrue(admin.role.canEditAdvancedSettings)
        assertTrue(admin.role.canManageConnection)
        assertTrue(admin.role.canAccessDiagnostics)
        assertTrue(admin.role.canManageOperatorPermissions)

        val operator = AuthRepository.authenticate("operator", "Pump@1234")
        assertNotNull(operator)
        assertEquals(UserRole.OPERATOR, operator!!.role)
        assertFalse("Operator must be read-only by default", operator.role.canOperateBasicControls)
        assertFalse(operator.role.canEditAdvancedSettings)
        assertFalse(operator.role.canManageConnection)
        assertFalse(operator.role.canAccessDiagnostics)
        assertFalse("Only ADMIN can manage operator permissions", operator.role.canManageOperatorPermissions)
    }

    @Test
    fun defaultOperatorPermissionsAreStrictlyReadOnlyWithZeroWriteAccess() {
        val defaultPerms = OperatorPermissions.READ_ONLY_DEFAULT

        // Read-only monitoring visibility is ON by default
        assertTrue(defaultPerms.canViewPumpsAndIo)
        assertTrue(defaultPerms.canViewHardwareMenu)

        // Every write, command, import, export, gateway, and diagnostic operation is OFF by default
        assertFalse(defaultPerms.hasAnyWritePermission)
        assertEquals(0, defaultPerms.grantedWriteCount)
        assertFalse(defaultPerms.canEditPressureSetpoint)
        assertFalse(defaultPerms.canTogglePumps)
        assertFalse(defaultPerms.canToggleDrive)
        assertFalse(defaultPerms.canResetPumpErrors)
        assertFalse(defaultPerms.canEditControlStatus)
        assertFalse(defaultPerms.canExportConfig)
        assertFalse(defaultPerms.canImportConfig)
        assertFalse(defaultPerms.canManageGateway)
        assertFalse(defaultPerms.canAccessDiagnostics)

        // Default OPERATOR cannot write ANY register in the entire BPC-02 map
        for (def in Bpc02RegisterMap.all) {
            assertFalse(
                "Default read-only OPERATOR must NOT write '${def.key}'",
                UserRole.OPERATOR.canWriteRegister(def),
            )
            assertFalse(
                "Default read-only OPERATOR must NOT write '${def.key}' with READ_ONLY_DEFAULT",
                UserRole.OPERATOR.canWriteRegister(def, defaultPerms),
            )
        }
    }

    @Test
    fun adminAlwaysRetainsFullAccessRegardlessOfOperatorPermissions() {
        val emptyOperatorPerms = OperatorPermissions(emptySet())
        val effectiveAdmin = UserRole.ADMIN.effectivePermissions(emptyOperatorPerms)

        assertEquals(OperatorPermissions.FULL_ADMIN, effectiveAdmin)
        for (perm in OperatorPermission.entries) {
            assertTrue("ADMIN must always retain $perm", perm in effectiveAdmin)
        }

        val setpoint = Bpc02RegisterMap.pressureSetpointDef
        val resetErr = Bpc02RegisterMap.multiFunctionRegisterDef
        val driveAct = Bpc02RegisterMap.driveActivationDef
        val pump1Act = Bpc02RegisterMap.byKey("pump_1_activation")
        val controlMode = Bpc02RegisterMap.controlStatusDef
        val sleepGap = Bpc02RegisterMap.byKey("sleep_gap")
        val pidP = Bpc02RegisterMap.byKey("pid_p_value")
        val modbusAddr = Bpc02RegisterMap.byKey("modbus_address")
        val manualOut = Bpc02RegisterMap.byKey("output_0_manual")

        // Administrator can always write all hardware RW registers, never R-only or locked registers
        assertTrue(UserRole.ADMIN.canWriteRegister(setpoint, emptyOperatorPerms))
        assertTrue(UserRole.ADMIN.canWriteRegister(resetErr, emptyOperatorPerms))
        assertTrue(UserRole.ADMIN.canWriteRegister(driveAct, emptyOperatorPerms))
        assertTrue(UserRole.ADMIN.canWriteRegister(pump1Act, emptyOperatorPerms))
        assertTrue(UserRole.ADMIN.canWriteRegister(controlMode, emptyOperatorPerms))
        assertTrue(UserRole.ADMIN.canWriteRegister(sleepGap, emptyOperatorPerms))
        assertTrue(UserRole.ADMIN.canWriteRegister(pidP, emptyOperatorPerms))
        assertFalse(UserRole.ADMIN.canWriteRegister(modbusAddr, emptyOperatorPerms))
        assertFalse(UserRole.ADMIN.canWriteRegister(manualOut, emptyOperatorPerms))
    }

    @Test
    fun everyWritableBpc02RegisterMapsToAnIndividualOperatorPermission() {
        val writableDefs = Bpc02RegisterMap.all.filter { it.isWritable }
        val readOnlyDefs = Bpc02RegisterMap.all.filter { !it.isWritable }
        assertTrue("BPC02 register map must have writable registers", writableDefs.isNotEmpty())

        for (def in writableDefs) {
            val requiredPerm = OperatorPermissions.requiredPermissionForRegister(def.key)
            assertNotNull("Writable register '${def.key}' must map to an OperatorPermission", requiredPerm)

            // With ONLY this permission granted, OPERATOR can write this register
            val grantedOnlyThis = OperatorPermissions(setOf(requiredPerm!!))
            assertTrue(
                "Granting $requiredPerm must allow OPERATOR to write '${def.key}'",
                UserRole.OPERATOR.canWriteRegister(def, grantedOnlyThis),
            )

            // With every permission EXCEPT this one granted, OPERATOR is blocked from writing this register
            val grantedAllExceptThis = OperatorPermissions(OperatorPermission.entries.toSet() - requiredPerm)
            assertFalse(
                "Revoking $requiredPerm must block OPERATOR from writing '${def.key}'",
                UserRole.OPERATOR.canWriteRegister(def, grantedAllExceptThis),
            )
        }

        // Hardware read-only and locked registers can NEVER be written even with FULL_ADMIN permissions
        for (def in readOnlyDefs) {
            assertFalse(
                "Read-only register '${def.key}' must never be writable by OPERATOR",
                UserRole.OPERATOR.canWriteRegister(def, OperatorPermissions.FULL_ADMIN),
            )
            assertFalse(
                "Read-only register '${def.key}' must never be writable by ADMIN",
                UserRole.ADMIN.canWriteRegister(def, OperatorPermissions.FULL_ADMIN),
            )
        }
    }

    @Test
    fun allOperatorPermissionsAreIndividuallyCustomizableInStore() = runBlocking {
        val store = InMemoryOperatorPermissionsStore()
        assertEquals(OperatorPermissions.READ_ONLY_DEFAULT, store.permissions.value)

        // Toggle every single permission individually and verify exact isolation
        for (perm in OperatorPermission.entries) {
            val before = store.permissions.value
            val toggledValue = perm !in before

            store.setPermission(perm, toggledValue)
            val after = store.permissions.value

            assertEquals(toggledValue, perm in after)
            for (other in OperatorPermission.entries) {
                if (other != perm) {
                    assertEquals(
                        "Toggling $perm must not affect $other",
                        other in before,
                        other in after,
                    )
                }
            }

            // Restore individual permission back to original state
            store.setPermission(perm, !toggledValue)
            assertEquals(before, store.permissions.value)
        }

        // Preset and reset-to-default
        store.save(OperatorPermissions.DAILY_OPERATOR_PRESET)
        assertTrue(store.permissions.value.canEditPressureSetpoint)
        assertTrue(store.permissions.value.canTogglePumps)
        assertTrue(store.permissions.value.canToggleDrive)
        assertTrue(store.permissions.value.canResetPumpErrors)
        assertFalse(store.permissions.value.canEditControlStatus)
        assertFalse(store.permissions.value.canManageGateway)

        store.resetToDefault()
        assertEquals(OperatorPermissions.READ_ONLY_DEFAULT, store.permissions.value)
        assertFalse(store.permissions.value.hasAnyWritePermission)
    }

    @Test
    fun rejectsWrongPasswordsRemovedLegacyUsersAndBlankInputs() {
        assertNull(AuthRepository.authenticate("admin", "dgp@2026")) // password is case-sensitive
        assertNull(AuthRepository.authenticate("operator", "wrong"))
        assertNull(AuthRepository.authenticate("nasab", "Nasab@1405"))
        assertNull(AuthRepository.authenticate("installer", "Inst@487"))
        assertNull(AuthRepository.authenticate("service", "Serv@2026"))
        assertNull(AuthRepository.authenticate("guest", "1234"))
        assertNull(AuthRepository.authenticate("unknown", "1234"))
        assertNull(AuthRepository.authenticate("", "Dgp@2026"))
        assertNull(AuthRepository.authenticate("admin", ""))
    }
}
