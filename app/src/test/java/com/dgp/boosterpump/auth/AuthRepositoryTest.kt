package com.dgp.boosterpump.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRepositoryTest {

    @Test
    fun authenticatesAllDocumentedRoleAccounts() {
        assertEquals(6, AuthRepository.accountCount)

        val admin = AuthRepository.authenticate("admin", "Dgp@2026")
        assertNotNull(admin)
        assertEquals(UserRole.ADMIN, admin!!.role)
        assertTrue(admin.role.canWriteConfig)
        assertTrue(admin.role.canEditGateway)
        assertTrue(admin.role.canRunDiagnostics)

        val nasab = AuthRepository.authenticate("  NASAB ", "Nasab@1405")
        assertNotNull(nasab)
        assertEquals(UserRole.INSTALLER, nasab!!.role)
        assertTrue(nasab.role.canWriteConfig)
        assertTrue(nasab.role.canEditGateway)

        val installer = AuthRepository.authenticate("installer", "Inst@487")
        assertNotNull(installer)
        assertEquals(UserRole.INSTALLER, installer!!.role)

        val service = AuthRepository.authenticate("service", "Serv@2026")
        assertNotNull(service)
        assertEquals(UserRole.SERVICE, service!!.role)
        assertFalse(service.role.canWriteConfig)
        assertFalse(service.role.canEditGateway)
        assertTrue(service.role.canRunDiagnostics)

        val operator = AuthRepository.authenticate("operator", "Pump@1234")
        assertNotNull(operator)
        assertEquals(UserRole.OPERATOR, operator!!.role)
        assertFalse(operator.role.canWriteConfig)
        assertFalse(operator.role.canEditGateway)
        assertFalse(operator.role.canRunDiagnostics)

        val guest = AuthRepository.authenticate("guest", "1234")
        assertNotNull(guest)
        assertEquals(UserRole.GUEST, guest!!.role)
        assertFalse(guest.role.canWriteConfig)
        assertFalse(guest.role.canEditGateway)
    }

    @Test
    fun rejectsWrongPasswordsUnknownUsersAndBlankInputs() {
        assertNull(AuthRepository.authenticate("admin", "dgp@2026")) // password is case-sensitive
        assertNull(AuthRepository.authenticate("nasab", "wrong"))
        assertNull(AuthRepository.authenticate("unknown", "1234"))
        assertNull(AuthRepository.authenticate("", "Dgp@2026"))
        assertNull(AuthRepository.authenticate("admin", ""))
    }
}
