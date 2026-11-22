# DGP Booster Pump Controller — Persian SCADA Android Application

Industrial Persian (`fa` + RTL) Android SCADA/HMI application (Kotlin, Jetpack Compose, Material 3, MVVM, Coroutines/Flow, DataStore) for monitoring and hardware-verified configuration of **DGP Booster Pump Controllers** over Modbus TCP/IP through a **Samin ES 487** RS485-to-Ethernet Modbus TCP gateway.

- **Target controller:** DGP **BPC02** — 100% hardware-verified on COM4 (`9600 8N1`, Slave ID `1`; see [`BPC02_FULL_READ_RESULTS.txt`](BPC02_FULL_READ_RESULTS.txt), [`BPC02_WRITE_TEST_RESULTS.txt`](BPC02_WRITE_TEST_RESULTS.txt), [`BPC02_REGISTER_MAP.pdf`](BPC02_REGISTER_MAP.pdf), and [`BPC02_REGISTER_MAP.md`](BPC02_REGISTER_MAP.md))
- **Future target:** DGP **BPC04** (architecture ready via `ControllerProfile`; disabled until the BPC04 register map is verified)
- **Language & UI:** Full **Persian (RTL)** industrial SCADA control-room dark UI + 1-tap daylight toggle, bundled **Vazirmatn** typography, custom Jetpack Compose Canvas instruments (240° radial pressure gauge, animated cutaway water tank, 40-sample live pressure trend chart, spinning pump impellers, 3-phase voltage analyzer, PLC LED beads)
- **Verified Read + Safe Config Write Posture:**
  - **Reads:** `FC01` (Coils) and `FC03` (Holding Registers) with zero-based PDU addressing (`offset = 0`).
  - **Hardware Configuration Writes:** `FC05` (Single Coil: `4096–4101`, `4106–4110`) and `FC06` (Single Holding Register: `4175`, `4177–4186`) — verified on hardware with 2-step Persian confirmation dialogs, strict range validation, and immediate read-back verification.
  - **Protected Read-Only:** `4173` (Control Status) and `4195` (ModBus Slave Address) are kept read-only in the app for safety.
  - **Manual Control Locked:** `Addr 99`, `Coils 50–59`, `Words 50–51` remain **LOCKED** (`CONTROL_LOCKED`) because bench testing returned Modbus Exception `0x02` / `0x03` (awaiting DGP technical clarification).

---

## 1. Hardware & Communication Architecture

```text
Android App (Persian SCADA HMI — Modbus TCP Client)
        │  Local Wi-Fi / LAN (TCP/IP)
        ▼
Samin ES 487 Ethernet Gateway (Modbus TCP/IP ↔ Modbus RTU, 5–48 V DC)
        │  RS485 (2-wire A/B + GND, 9600 baud, 8N1)
        ▼
DGP BPC02 Booster Pump Controller (Modbus RTU Slave, Unit ID 1)
```

### Samin ES 487 Gateway Notes (مبدل مدباس RS485 به مدباس اترنت مدل ES 487)

- **Selected hardware:** **Samin ES 487** industrial RS485-to-Ethernet Modbus converter:
  - **Power supply:** `5–48 V DC` wide input range (ships with a `12 V DC` adapter).
  - **Configuration:** Built-in web browser interface + IP Finder utility + hardware factory-reset button.
  - **Network modes:** `Server` / `Client`, `TCP` / `UDP`, `DHCP` or `Static IP`.
  - **Protocol conversion:** `Modbus RTU` (RS485) ↔ `Modbus TCP/IP` (Ethernet).
- **Required ES 487 settings for the DGP BPC02:**
  1. **Serial Port (RS485):** `9600` bps, `8` data bits, `No` parity, `1` stop bit (`9600 8N1`).
  2. **Working Mode:** **TCP Server** with **Modbus TCP/IP ↔ Modbus RTU** conversion (`Modbus TCP (MBAP)` framing in the app), or transparent serial-over-TCP (`RTU over TCP` framing in the app).
  3. **IP & TCP Port:** Assign a **Static IP** on the local LAN (or discover the DHCP address via ES 487 IP Finder) and note the configured TCP port (commonly `502`).
  4. **BPC02 Slave / Unit ID:** Verified as `1` (readable at Holding Register `4195`; configurable in BPC02 `Hardware → Network`).

---

## 2. Complete Hardware-Verified BPC02 Protocol Summary

Full tables are published in **[`BPC02_REGISTER_MAP.pdf`](BPC02_REGISTER_MAP.pdf)** and **[`BPC02_REGISTER_MAP.md`](BPC02_REGISTER_MAP.md)** (verified in `BPC02_FULL_READ_RESULTS.txt` & `BPC02_WRITE_TEST_RESULTS.txt`):

### Confirmed Function Codes (`9600 8N1`, Slave ID `1`, Zero-Based PDU `offset = 0`)
- **Supported (only 4 FCs):** `FC01` (Read Coils), `FC03` (Read Holding Registers), `FC05` (Write Single Coil), `FC06` (Write Single Holding Register).
- **Not Supported (all 4 return BPC02's non-standard 4-byte valid-CRC rejection frame):**
  - `FC02` → `01 02 81 E1`
  - `FC04` → `01 04 01 E3`
  - `FC15 (0x0F)` → `01 0F 40 24`
  - `FC16 (0x10)` → `01 10 01 EC`

### Verified Register Blocks & Quirks
- **Live Polled Reads (5 gap-aware blocks):**
  - `FC01 0 qty 34`: Outputs `0–9`, `DR` (`10`), `PR1–4` (`11–14`), `MFI1–4` (`15–18`), Pumps `1–6` run (`19–24`), Drive fail (`25`), Pumps `1–6` fail (`26–31`), Floaters L/H (`32–33`).
  - `FC03 0 qty 1`: Displayed LCD page number.
  - `FC03 2 qty 9`: System pressure (`2`, `0.01 bar`), Voltages `Vrn/Vsn/Vtn/Vrs/Vst/Vtr` (`3–8`, `V`), Mains frequency (`9`, `0.01 Hz` — hardware read `5410 → 54.10 Hz`), Drive frequency (`10`, `0–1600`, read `1000` — displayed raw until scale is confirmed).
  - `FC03 34 qty 12`: Pump `1–4` work times (`s`/`min`/`h` — 1 Hz tick and 60 s rollover verified on hardware).
  - `FC03 80 qty 1`: System Error bitfield (bits `3–10` documented).
- **Hardware Configuration (6 on-demand read blocks + 22 verified `FC05`/`FC06` writable settings):**
  - `FC01`/`FC05` `4096–4101`: Pump `1–6` Activation (Coil `4101` `1 → 0 → 1` bench-verified).
  - `FC01`/`FC05` `4106–4110`: Control Phase, OFF Changing, Time Changing, Reserve Auto Activation, Reserve Auto Type (`NO`/`NC`).
  - `FC03`/`FC06` `4175`: Sleep Gap (`1–100` = `0.01–1.00 bar`; `40 → 41 → 40` bench-verified).
  - `FC03`/`FC06` `4177–4186` (verified single `qty 10` block; confirms PDF `417B`/`41B0` typos are `4178`/`4180`): Auto Service Off/On/Interval, `MFI1–4` Type, `MFO1–3` Type.
  - `FC03` read-only protected: `4173` (Control Status) and `4195` (ModBus Address = `1`).
- **Unmasked trailing coil bits:** For non-multiple-of-8 `FC01` reads (`4096 qty 6 → 0xFF`, `4106 qty 5 → 0xD0`), unused upper bits are not masked by BPC02; `ModbusPdu.parseBitsResponse` strictly decodes `0 until quantity` LSB-first.

---

## 3. Application Architecture

```text
app/src/main/java/com/dgp/boosterpump/
├── modbus/          # ReadFunction (FC01–FC04), WriteFunction (FC05/FC06 only), ModbusPdu,
│                    # Crc16, ModbusTransport, TcpModbusTransport (MBAP + RTU-over-TCP),
│                    # ModbusReadClient, ModbusWriteClient
├── controller/      # ControllerProfile (BPC02 + BPC04 extension point), ControllerReader,
│   └── register/    # ControllerSnapshot, SystemErrorBit, RegisterDefinition, Bpc02RegisterMap
├── gateway/         # GatewayConfig, ProtocolMapping (default FC01 + FC03 + offset 0), DataStore
├── simulation/      # SimulatedBpc02Transport (5 live scenarios + persistent FC05/FC06 config state)
├── session/         # ControllerSession (polling, reconnect backoff, TX/RX log, verified FC05/FC06
│                    # writes with immediate read-back, single read probe)
├── viewmodel/       # MonitoringViewModel, GatewaySettingsViewModel
└── ui/              # Persian RTL theme (Vazirmatn), PersianLabels, Canvas SCADA instruments,
                     # 5 screens (Dashboard, Pumps & I/O, Control & Config, Diagnostics, Gateway)
```

---

## 4. Build, Unit Tests & Windows Bench Scripts

### Android Build & Unit Tests
```bash
# Run all unit tests (Modbus codec, FC05/FC06 hardware vectors, TCP/RTU transport, register map, session)
./gradlew testDebugUnitTest

# Build debug APK
./gradlew assembleDebug

# If dl.google.com is restricted on your network (HTTP 403/404), put Aliyun/Huawei mirrors first:
./gradlew testDebugUnitTest assembleDebug -PuseMirrors=true
```
The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.
*(Fallback mirrors — Aliyun & Huawei Cloud — are also configured automatically in `settings.gradle.kts`.)*

### Windows Hardware Audit Scripts (`scripts/`)
- **`scripts/run_read_tests.bat`** (`scripts/bpc02_full_read_test.ps1`): Strictly read-only full audit of all 14 BPC02 blocks over Serial RS485 (`COM4`) or Samin ES 487 (`Modbus TCP`).
- **`scripts/run_write_tests.bat`** (`scripts/bpc02_write_test.ps1`): Isolated bench write verification script (reads original values first and restores them automatically).

---

## 5. Repository Documentation

- [`BPC02_REGISTER_MAP.pdf`](BPC02_REGISTER_MAP.pdf) & [`BPC02_REGISTER_MAP.md`](BPC02_REGISTER_MAP.md) — Complete hardware-verified BPC02 Modbus register map & Samin ES 487 setup
- [`BPC02_FULL_READ_RESULTS.txt`](BPC02_FULL_READ_RESULTS.txt) — Complete 14-block read-only hardware capture on COM4
- [`BPC02_WRITE_TEST_RESULTS.txt`](BPC02_WRITE_TEST_RESULTS.txt) — Bench write verification capture (`FC05`/`FC06` verified, `FC15`/`FC16` rejected, Manual Mode `0x02`/`0x03`)
- [`BPC02_WRITE_TESTS.txt`](BPC02_WRITE_TESTS.txt) — Isolated bench write verification protocol
- [`BPC02_TEST_RESULTS.md`](BPC02_TEST_RESULTS.md), `REMAINING 01 ... .txt`, `REMAINING 03 ... .txt` — Initial COM4 hardware captures
- `BPC-02.pdf`, `DGP_BPC02_Modbus_Research_Findings.txt`, `ETOR-4_EN.pdf`, `Modbus register map .txt`, `modbuscloud-rs485-modbus-checklist-en.pdf` — Original controller manual and reference documents
