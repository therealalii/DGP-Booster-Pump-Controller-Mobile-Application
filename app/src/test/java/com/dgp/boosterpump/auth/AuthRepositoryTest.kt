package com.dgp.boosterpump.auth

import com.dgp.boosterpump.controller.register.Bpc02RegisterMap
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

        val operator = AuthRepository.authenticate("operator", "Pump@1234")
        assertNotNull(operator)
        assertEquals(UserRole.OPERATOR, operator!!.role)
        assertTrue(operator.role.canOperateBasicControls)
        assertFalse(operator.role.canEditAdvancedSettings)
        assertFalse(operator.role.canManageConnection)
        assertFalse(operator.role.canAccessDiagnostics)
    }

    @Test
    fun enforcesPerRegisterPermissionsBetweenAdminAndOperator() {
        val setpoint = Bpc02RegisterMap.pressureSetpointDef
        val resetErr = Bpc02RegisterMap.multiFunctionRegisterDef
        val driveAct = Bpc02RegisterMap.driveActivationDef
        val pump1Act = Bpc02RegisterMap.byKey("pump_1_activation")
        val controlMode = Bpc02RegisterMap.controlStatusDef
        val sleepGap = Bpc02RegisterMap.byKey("sleep_gap")
        val pidP = Bpc02RegisterMap.byKey("pid_p_value")
        val modbusAddr = Bpc02RegisterMap.byKey("modbus_address")
        val manualOut = Bpc02RegisterMap.byKey("output_0_manual")

        // Administrator can write all RW registers, but never R-only or locked registers
        assertTrue(UserRole.ADMIN.canWriteRegister(setpoint))
        assertTrue(UserRole.ADMIN.canWriteRegister(resetErr))
        assertTrue(UserRole.ADMIN.canWriteRegister(driveAct))
        assertTrue(UserRole.ADMIN.canWriteRegister(pump1Act))
        assertTrue(UserRole.ADMIN.canWriteRegister(controlMode))
        assertTrue(UserRole.ADMIN.canWriteRegister(sleepGap))
        assertTrue(UserRole.ADMIN.canWriteRegister(pidP))
        assertFalse(UserRole.ADMIN.canWriteRegister(modbusAddr))
        assertFalse(UserRole.ADMIN.canWriteRegister(manualOut))

        // Operator can operate basic daily controls only (setpoint, pump/drive ON-OFF, error reset)
        assertTrue(UserRole.OPERATOR.canWriteRegister(setpoint))
        assertTrue(UserRole.OPERATOR.canWriteRegister(resetErr))
        assertTrue(UserRole.OPERATOR.canWriteRegister(driveAct))
        assertTrue(UserRole.OPERATOR.canWriteRegister(pump1Act))
        // Operator is blocked from advanced controller configuration registers
        assertFalse(UserRole.OPERATOR.canWriteRegister(controlMode))
        assertFalse(UserRole.OPERATOR.canWriteRegister(sleepGap))
        assertFalse(UserRole.OPERATOR.canWriteRegister(pidP))
        assertFalse(UserRole.OPERATOR.canWriteRegister(modbusAddr))
        assertFalse(UserRole.OPERATOR.canWriteRegister(manualOut))
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
