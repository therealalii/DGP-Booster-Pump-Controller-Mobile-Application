package com.dgp.boosterpump.session

import com.dgp.boosterpump.controller.SystemErrorBit
import com.dgp.boosterpump.controller.register.Bpc02RegisterMap
import com.dgp.boosterpump.gateway.DataSourceMode
import com.dgp.boosterpump.gateway.GatewayConfig
import com.dgp.boosterpump.gateway.ProtocolMapping
import com.dgp.boosterpump.modbus.ReadFunction
import com.dgp.boosterpump.simulation.SimulatedBpc02Transport
import com.dgp.boosterpump.simulation.SimulationScenario
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ControllerSessionTest {

    @Test
    fun simulationSessionPollsScenariosProbesAndReadsHardwareConfig() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val session = ControllerSession(
            scope = scope,
            ioDispatcher = Dispatchers.Default,
            transportFactory = { cfg, scenario -> SimulatedBpc02Transport(cfg.mapping, scenario, responseDelayMs = 5L) },
        )

        try {
            val simConfig = GatewayConfig(
                mode = DataSourceMode.SIMULATION,
                pollIntervalMs = 500,
                mapping = ProtocolMapping(
                    bitReadFunction = ReadFunction.READ_DISCRETE_INPUTS,
                    wordReadFunction = ReadFunction.READ_INPUT_REGISTERS,
                    addressOffset = 0,
                ),
            )
            session.connect(simConfig)

            val snap1 = withTimeout(4000) { session.snapshot.first { it != null } }!!
            assertTrue("Simulated snapshot must always be flagged simulated", snap1.isSimulated)
            assertNotNull(snap1.pressureBar)
            assertTrue(snap1.pressureBar!! in 3.5..5.0)
            assertTrue(snap1.alarms.isEmpty())
            assertTrue(session.transactions.value.isNotEmpty())
            assertTrue("Pressure history must record live samples", session.pressureHistory.value.isNotEmpty())

            // Switch to floater alarm scenario and confirm alarm appears on next poll
            session.setSimulationScenario(SimulationScenario.FLOATER_LOW_ALARM)
            val snap2 = withTimeout(4000) {
                session.snapshot.first { it != null && SystemErrorBit.FLOATER in it.activeSystemErrors }
            }!!
            assertEquals(false, snap2.floaterLowConnected)
            assertEquals(0, snap2.runningPumpCount)

            // Read-only probe against Word 2 using the configured FC04
            session.runReadProbe(ReadFunction.READ_INPUT_REGISTERS, pduAddress = 2, quantity = 1)
            val probe = withTimeout(3000) { session.probeResult.first { it != null } }!!
            assertEquals(null, probe.error)
            assertEquals(listOf(115), probe.values)

            // On-demand hardware configuration read
            session.readHardwareConfiguration()
            val cfgState = withTimeout(3000) { session.configSnapshot.first { it.readings.isNotEmpty() && !it.loading } }
            assertEquals(24, cfgState.readings.size)

            // Simulated timeout transitions connection state to Error
            session.setSimulationScenario(SimulationScenario.LINK_TIMEOUT)
            val errState = withTimeout(4000) { session.connectionState.first { it is ConnectionState.Error } }
            assertTrue((errState as ConnectionState.Error).isSimulated)

            session.disconnect()
            assertEquals(ConnectionState.Disconnected, session.connectionState.value)
        } finally {
            session.disconnect()
            scope.cancel()
        }
    }

    @Test
    fun verifiedHardwareWritesPersistWithReadBackAndSafetyGuardsBlockLockedRegisters() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val session = ControllerSession(
            scope = scope,
            ioDispatcher = Dispatchers.Default,
            transportFactory = { cfg, scenario -> SimulatedBpc02Transport(cfg.mapping, scenario, responseDelayMs = 5L) },
        )

        try {
            // Default verified BPC02 mapping (FC01 + FC03 + offset 0)
            session.connect(GatewayConfig(mode = DataSourceMode.SIMULATION, pollIntervalMs = 500))
            withTimeout(4000) { session.snapshot.first { it != null } }

            // Load initial hardware config matching BPC02_FULL_READ_RESULTS.txt defaults
            session.readHardwareConfiguration()
            val initialCfg = withTimeout(3000) { session.configSnapshot.first { it.readings.size == 24 && !it.loading } }
            assertEquals(40, initialCfg.readings.first { it.definition.key == "sleep_gap" }.raw)
            assertEquals(1, initialCfg.readings.first { it.definition.key == "pump_6_activation" }.raw)
            assertEquals(0, initialCfg.readings.first { it.definition.key == "reserve_auto_activation" }.raw)
            assertEquals(1, initialCfg.readings.first { it.definition.key == "reserve_auto_type" }.raw)

            // 1. FC06 write to Sleep Gap (4175 = 45 -> 0.45 bar) with immediate read-back
            val sleepGap = Bpc02RegisterMap.byKey("sleep_gap")
            session.writeHardwareParameter(sleepGap, 45)
            val afterWordWrite = withTimeout(3000) {
                session.configSnapshot.first { it.lastWrite?.definition?.key == "sleep_gap" && it.writingKey == null }
            }
            assertTrue(afterWordWrite.lastWrite!!.success)
            assertEquals(45, afterWordWrite.lastWrite!!.readBackValue)
            assertEquals(45, afterWordWrite.readings.first { it.definition.key == "sleep_gap" }.raw)
            assertTrue(session.transactions.value.any { it.isWrite && it.functionCode == 0x06 && it.pduAddress == 4175 })

            // 2. FC05 write to Pump 6 Activation (4101 = 0 -> Disabled) with immediate read-back
            val pump6 = Bpc02RegisterMap.byKey("pump_6_activation")
            session.writeHardwareParameter(pump6, 0)
            val afterCoilWrite = withTimeout(3000) {
                session.configSnapshot.first { it.lastWrite?.definition?.key == "pump_6_activation" && it.writingKey == null }
            }
            assertTrue(afterCoilWrite.lastWrite!!.success)
            assertEquals(0, afterCoilWrite.lastWrite!!.readBackValue)
            assertEquals(0, afterCoilWrite.readings.first { it.definition.key == "pump_6_activation" }.raw)
            assertTrue(session.transactions.value.any { it.isWrite && it.functionCode == 0x05 && it.pduAddress == 4101 })

            // 3. Safety guardrails MUST block protected & locked registers + out-of-range values
            val txCountBeforeGuards = session.transactions.value.count { it.isWrite }

            session.writeHardwareParameter(Bpc02RegisterMap.byKey("modbus_address"), 2)
            assertFalse(session.configSnapshot.value.lastWrite!!.success)

            session.writeHardwareParameter(Bpc02RegisterMap.byKey("control_status"), 1)
            assertFalse(session.configSnapshot.value.lastWrite!!.success)

            session.writeHardwareParameter(Bpc02RegisterMap.byKey("multi_function_register"), 1234)
            assertFalse(session.configSnapshot.value.lastWrite!!.success)

            session.writeHardwareParameter(sleepGap, 101) // max is 100
            assertFalse(session.configSnapshot.value.lastWrite!!.success)

            assertEquals(
                "Blocked writes must never transmit any write frame",
                txCountBeforeGuards,
                session.transactions.value.count { it.isWrite },
            )
        } finally {
            session.disconnect()
            scope.cancel()
        }
    }
}
