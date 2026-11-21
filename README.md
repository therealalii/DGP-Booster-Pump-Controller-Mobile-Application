# DGP Booster Pump Controller — Android Application

Industrial Android application (Kotlin, Jetpack Compose, Material 3, MVVM) for monitoring **DGP Booster Pump Controllers** over Modbus TCP/IP through an Ethernet-to-RS485 gateway.

- **Initial target:** DGP **BPC02**
- **Future target:** DGP **BPC04** (architecture ready via `ControllerProfile`; disabled until the BPC04 register map is confirmed)
- **Safety posture:** **Strict READ-ONLY mode.** Write function codes (`05`, `06`, `0F`, `10`) are intentionally omitted from the Modbus client so no unverified write can ever reach a physical controller.

---

## 1. Hardware & Communication Architecture

```text
Android App (Modbus TCP Client)
        │  Local Wi-Fi / LAN (TCP/IP)
        ▼
ENTES ETMO-10 Ethernet Gateway (Modbus TCP ↔ Modbus RTU, or Transparent)
        │  RS485 (2-wire A/B + GND, 9600 baud, 8N1)
        ▼
DGP BPC02 Booster Pump Controller (Modbus RTU Slave)
```

### ENTES ETMO-10 Notes (verified against ENTES ETMO documentation)

- The current ENTES catalog variant is **ETMO-10-SH** (DIN3 rail, 85–265 VAC/DC, Ethernet 10/100, RS485 up to 32 devices, supports both **Modbus TCP/RTU** gateway mode and **Transparent** serial-over-TCP mode).
- Configure the ETMO-10 via its web interface (USB micro-cable `http://usb.entes` / `192.168.7.1`, or its assigned LAN IP):
  1. **Serial Port:** `9600` bps, `8` data bits, `No` parity, `1` stop bit (`9600 8N1`) — matching the BPC02 manual (p. 21 & p. 25).
  2. **Working Mode:** **MODBUS** (TCP Server) for standard Modbus TCP (MBAP) framing, or **Transparent** if using RTU-over-TCP framing. Both framings are selectable in the app.
  3. **TCP Port:** Commonly `502` (or `700` on some ENTES configurations). Confirm the configured port on your gateway; do not assume factory defaults.
  4. **BPC02 Slave / Unit ID:** Read or set in the BPC02 `Hardware → Network` menu (1–247; the manual lists `1` in row 68).
- The transport layer (`ModbusTransport`) is gateway-agnostic so any compatible Modbus TCP or transparent RS485-to-Ethernet gateway can be used without changing controller or UI code.

---

## 2. Confirmed vs. Unconfirmed BPC02 Protocol Information

Derived from `BPC-02.pdf` (pages 21, 25–28) and `DGP_BPC02_Modbus_Research_Findings.txt`:

### Confirmed by `BPC-02.pdf`
- **Serial protocol:** Modbus RTU over RS485, **9600 8N1**.
- **Bit Address table (read-only status):**
  - `0–9`: Digital Output 0–9 status (`0=OFF`, `1=ON`)
  - `10`: DR status (`0=OFF`, `1=ON`)
  - `11–14`: PR1–PR4 direct-contactor feedback status (`0=OFF`, `1=ON`)
  - `15–18`: MFI1–MFI4 multi-function input status (`0=OFF`, `1=ON`)
  - `19–24`: Pump 1–6 status (`0=OFF`, `1=ON`)
  - `25`: Drive fail (`0=Normal`, `1=Failed`)
  - `26–31`: Pump 1–6 fail (`0=Normal`, `1=Failed`)
  - `32`: Low-level floater (`0=Not connected`, `1=Connected`)
  - `33`: High-level floater (`0=Not connected`, `1=Connected`)
- **Word Address table (measurements & faults):**
  - `0`: Displayed LCD page number
  - `2`: System pressure (`0.01 bar`)
  - `3–5`: Phase-to-neutral voltages `Vrn`, `Vsn`, `Vtn` (`Volt`)
  - `6–8`: Phase-to-phase voltages `Vrs`, `Vst`, `Vtr` (`Volt`)
  - `9`: Mains frequency (`0.01 Hz`)
  - `10`: Drive frequency (printed range `0–1600`, **unit/scale not documented — displayed as raw value**)
  - `34–45`: Pump 1–4 operating time (3 registers per pump: seconds, minutes, hours). Pumps 5–6 have no documented run-time words.
  - `80`: System error bit field — documented bits: `3` Floater, `4` Max Pressure, `5` Pressure Sensor Disconnected, `6` Phase Sensitivity, `7` Phase Max Voltage, `8` Phase Min Voltage, `9` Phase Sequence, `10` Pump 1 Error. Any other set bit is surfaced explicitly as an undocumented bit warning.
- **Hardware configuration addresses (read-only on demand, never written):** Bits `4096–4101`, `4106–4110`; Words `4173`, `4175`, `4177`, `4178` (printed `417B`), `4179`, `4180` (printed `41B0`), `4181–4186`, `4195`.

### Unconfirmed (Configurable in App — Must Be Verified on Hardware)
- **Modbus function codes:** The manual does not state whether the Bit table is Coils (`FC01`) or Discrete Inputs (`FC02`), or whether the Word table is Holding Registers (`FC03`) or Input Registers (`FC04`). Both pairs are selectable in **Gateway Settings**.
- **PDU address offset:** Both tables start at `0`, so offset `0` (printed = PDU) is the primary hypothesis; `-1` and `+1` are selectable for verification.
- **Data signedness:** Not declared in the manual; 16-bit registers are decoded as unsigned `UINT16`.
- **Drive frequency (Word 10):** Range `0–1600`, no unit/scale printed. Shown raw until compared against the controller LCD.
- **Manual control writes (LOCKED):** Address `99` (`Multi Function Register`: `1234` enter manual / `0` return to automatic), Bit `50–59` (manual outputs), Word `50–51` (manual analog speed `0–500`, `0.1 Hz`). Disabled until DGP confirms table types and write function codes and engineering authorizes bench testing.

---

## 3. Application Architecture

```text
app/src/main/java/com/dgp/boosterpump/
├── modbus/          # ReadFunction (FC01–FC04 only), ModbusPdu, Crc16, ModbusTransport,
│                    # TcpModbusTransport (MBAP + RTU-over-TCP), ModbusReadClient
├── controller/      # ControllerProfile (BPC02 + BPC04 extension point), ControllerReader,
│   └── register/    # ControllerSnapshot, SystemErrorBit, RegisterDefinition, Bpc02RegisterMap
├── gateway/         # GatewayConfig, ProtocolMapping, validation, DataStore repository
├── simulation/      # SimulatedBpc02Transport (5 test scenarios through the real Modbus decoder)
├── session/         # ControllerSession (polling loop, backoff, transaction log, read probe)
├── viewmodel/       # MonitoringViewModel, GatewaySettingsViewModel
└── ui/              # Material 3 industrial theme, shared components, 5 Compose screens
```

### Key Features
- **Dashboard:** Pressure (bar + gauge), Drive frequency (raw, unverified badge), Active alarms (Word 80 + fault bits), Pump 1–6 tiles, 3-phase supply voltages (`Vrn/Vsn/Vtn`, `Vrs/Vst/Vtr`) & mains frequency, tank floaters, and explicit Disconnected / Connecting / Error / Partial-Error states.
- **Pumps & I/O:** Pump 1–6 status and Pump 1–4 run times, DR/Drive Fail/Floaters, Digital Outputs 0–9, PR1–PR4 and MFI1–MFI4 inputs, and the Manual Control safety-lock panel.
- **Details & Map:** Full centralized BPC02 register table with raw + engineering values and verification status (`Documented` / `Ambiguous`), on-demand read-only hardware configuration readout, and locked control register inventory.
- **Diagnostics:** Read-only single-request Modbus probe (`FC01–FC04`) with BPC02 verification presets (`W2` Pressure, `W9–10` Frequencies, `B19` Pumps, `W80` Errors) and a live raw hex TX/RX transaction log.
- **Gateway Settings:** Live vs. Simulation selector, BPC02 / BPC04 (planned) selector, IP/port/Unit ID, MBAP vs. RTU-over-TCP framing, configurable Bit/Word function codes and PDU offset, and poll/timeout settings persisted in Jetpack DataStore.
- **Simulation Mode:** Clearly identified at all times by a persistent amber `SIMULATION MODE — NOT LIVE HARDWARE` banner with selectable scenarios (Normal Duty, Peak Demand, Low-Tank Floater Fault, Pump & Drive Fault, Simulated Timeout).

---

## 4. Build & Test Instructions

### Prerequisites
- JDK 17+
- Android SDK (API 35, `ANDROID_HOME` or `local.properties` pointing to your SDK)

### Commands
```bash
# Run all unit tests (Modbus codec, loopback TCP/RTU transport, register map, session)
./gradlew testDebugUnitTest

# Build debug APK
./gradlew assembleDebug
```
The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

---

## 5. First Hardware Verification Procedure (Read-Only)

Follow `DGP_BPC02_Modbus_Research_Findings.txt` (§8) and `modbuscloud-rs485-modbus-checklist-en.pdf` before connecting to a live site:

1. Verify RS485 wiring (`A`, `B`, `GND`, 120 Ω termination at bus ends, shield grounded at one end only).
2. Confirm the ENTES ETMO-10 serial port is configured to **9600 8N1** and note its LAN IP, TCP port, and working mode (MODBUS vs. Transparent).
3. Confirm the BPC02 slave address in the controller's `Hardware → Network` menu.
4. In the app's **Gateway** tab, switch to **Live gateway**, enter IP, port, Unit ID, and framing, and tap **Save & Connect**.
5. Open the **Diag** tab and run the read-only presets:
   - Read **Word 2** with `FC03` (try `FC04` if Exception `01`/`02` occurs) and verify `raw * 0.01 bar` matches the controller LCD pressure.
   - Read **Word 9–10** and verify `Word 9 * 0.01 Hz` matches mains frequency; record `Word 10` raw value alongside the LCD drive frequency to determine its scaling factor.
   - Read **Bit 19** (`qty 6`) with `FC01` (or `FC02`) and compare with the running pumps.
   - Read **Word 80** and verify normal (`0x0000`) or known alarm state.
6. Record the raw TX/RX frames from the transaction log to finalize the verified protocol mapping before any future work on BPC04 or approved write commands.

---

## 6. Reference Documentation in Repository

All original documentation files are preserved untouched in the repository root:
- `BPC-02.pdf` — DGP BPC-02 User Manual (primary controller reference)
- `DGP_BPC02_Modbus_Research_Findings.txt` — Detailed Modbus protocol audit & open questions for DGP
- `ETOR-4_EN.pdf` — General ENTES Ethernet/Serial gateway reference (selected hardware is ENTES ETMO-10)
- `Modbus register map .txt` — General Modbus addressing and register-type reference
- `modbuscloud-rs485-modbus-checklist-en.pdf` — Field RS485 wiring and commissioning checklist
