# DGP BPC02 — Complete Hardware-Verified Modbus Register Map

> **Sources:** `BPC-02.pdf` (User Manual V1, pages 16, 19–21, 25–28), early hardware captures (`BPC02_TEST_RESULTS.md`, `REMAINING 01`, `REMAINING 03`), and the complete automated bench audits in **`BPC02_FULL_READ_RESULTS.txt`** (all 14 read blocks) and **`BPC02_WRITE_TEST_RESULTS.txt`** (FC05/FC06/FC15/FC16 write verification) on COM4 (`9600 8N1`, Slave ID `1`).
>
> **Selected RS485-to-Ethernet Gateway:** **Samin ES 487** (مبدل مدباس RS485 به مدباس اترنت مدل ES 487 — `5–48 V DC`, Web UI configuration, Server/Client, TCP/UDP, DHCP/Static IP, Modbus RTU ↔ Modbus TCP/IP).
>
> **App Access & Safety Posture:**
> - **READ (`FC01`, `FC03`):** Fully verified across all live telemetry and configuration blocks.
> - **CONFIG WRITE (`FC05`, `FC06`):** Hardware-verified on **22 hardware configuration registers** (`4096–4101`, `4106–4110`, `4175`, `4177–4186`), enabled in the Android app with 2-step confirmation, range validation, and immediate read-back verification.
> - **PROTECTED READ-ONLY (`4173`, `4195`):** Kept read-only in the app UI for operational safety (disabling `4173` stops the system; changing `4195` immediately severs Modbus comms).
> - **MANUAL CONTROL (`99`, `50–59`, `50–51`):** Strictly **LOCKED** — bench write testing returned Modbus Exception `0x02` / `0x03` (pending DGP technical clarification on `021-77797006`).

---

## 1. Confirmed Communication & Complete 8-Function-Code Support Matrix

| Parameter | Value | Status |
|---|---|---|
| Controller Model | DGP BPC02 Booster Pump Controller | **VERIFIED** |
| Serial Protocol | Modbus RTU over RS485 (2-wire A/B + GND) | **VERIFIED** |
| Serial Settings | `9600` baud, `8` data bits, `No` parity, `1` stop bit (`9600 8N1`) | **VERIFIED** |
| Tested Slave / Unit ID | `1` (configurable in BPC02 `Hardware → Network`, verified at Word `4195 = 1`) | **VERIFIED** |
| Addressing Base | **Zero-based PDU addressing (`offset = 0`)** — printed decimal address = wire address | **VERIFIED** |
| Byte Order (16-bit words) | Big-endian (MSB first), standard Modbus | **VERIFIED** |
| Signedness | Not declared in manual; treated as unsigned `UINT16` (`0–65535`) | Documented |
| Multi-Register Reads | Verified up to `qty 34` (`FC01`) and `qty 12` (`FC03`) | **VERIFIED** |
| Ethernet Gateway | **Samin ES 487** (Modbus RTU ↔ Modbus TCP/IP, TCP Server mode, `9600 8N1`) | Selected Hardware |

### Complete Function Code Audit (All 8 Standard Modbus Function Codes Tested)

| Function Code | Purpose | BPC02 Hardware Result | Exact Bench Evidence |
|---|---|---|---|
| **`FC01` (`0x01`)** | Read Coils (Bit Table) | **SUPPORTED** | All coil blocks (`0 qty 34`, `50 qty 10`, `4096 qty 6`, `4106 qty 5`) OK, CRC valid |
| **`FC03` (`0x03`)** | Read Holding Registers (Word Table) | **SUPPORTED** | All word blocks (`0`, `2 qty 9`, `34 qty 12`, `50 qty 2`, `80`, `99`, `4173`, `4175`, `4177 qty 10`, `4195`) OK |
| **`FC05` (`0x05`)** | Write Single Coil | **SUPPORTED (Config Coils)** | Coil `4101` `1 → 0 → 1` (`01 05 10 05 00 00 D9 0B`) verified with `FC01` read-back (`0xFF → 0xDF → 0xFF`) |
| **`FC06` (`0x06`)** | Write Single Holding Register | **SUPPORTED (Config Words)** | Word `4175` `40 → 41 → 40` (`01 06 10 4F 00 29 7D 03`) verified with `FC03` read-back (`41 → 40`) |
| **`FC02` (`0x02`)** | Read Discrete Inputs | **NOT SUPPORTED** | Non-standard 4-byte valid-CRC rejection: `01 02 81 E1` |
| **`FC04` (`0x04`)** | Read Input Registers | **NOT SUPPORTED** | Non-standard 4-byte valid-CRC rejection: `01 04 01 E3` |
| **`FC15` (`0x0F`)** | Write Multiple Coils | **NOT SUPPORTED** | Non-standard 4-byte valid-CRC rejection: `01 0F 40 24` |
| **`FC16` (`0x10`)** | Write Multiple Registers | **NOT SUPPORTED** | Non-standard 4-byte valid-CRC rejection: `01 10 01 EC` |

### Key BPC02 Firmware Findings Discovered on Hardware
1. **Non-standard 4-byte rejection frame for unsupported function codes:**  
   For `FC02`, `FC04`, `FC15 (0x0F)`, and `FC16 (0x10)`, the BPC02 replies with a 4-byte `[ID] [FC] [CRC_L] [CRC_H]` frame whose CRC-16 is valid (`01 02 81 E1`, `01 04 01 E3`, `01 0F 40 24`, `01 10 01 EC`). Never send `FC02`, `FC04`, `FC15`, or `FC16` through the Samin ES 487 gateway.
2. **Unmasked trailing padding bits on `FC01`:**  
   When `quantity` is not a multiple of 8 (e.g., `4096 qty 6 → 0xFF`, `4106 qty 5 → 0xD0`), the BPC02 leaves unused high bits unmasked. Clients must decode strictly `0 until quantity` LSB-first. Specifically, `0xD0 = 0b1101_0000` for `4106..4110` decodes to `4106=0, 4107=0, 4108=0, 4109=0 (Disable), 4110=1 (NC)`.
3. **PDF print typos resolved (`417B` → `4178`, `41B0` → `4180`):**  
   A single contiguous `FC03` read at `4177 qty 10` (`01 03 10 51 00 0A 90 DC`) returned all 10 words (`4177..4186`) cleanly (`159 h`, `33 = 3.3 s`, `11 min`, and `0` for `MFI1..4`/`MFO1..3`).
4. **Manual Mode bench test findings (`BPC02_WRITE_TEST_RESULTS.txt`):**  
   `FC06 Addr 99 = 1234` echoed OK (`01 06 00 63 04 D2 FB 49`), while read-back of `Addr 99` stayed `0`. Subsequent `FC06 Word 50 = 50` and `FC05 Coil 56 = ON` returned **Modbus Exception `0x02` (Illegal Data Address)** (`01 86 02 C3 A1`, `01 85 02 C3 51`), and `FC06 Addr 99 = 0` returned **Modbus Exception `0x03` (Illegal Data Value)** (`01 86 03 02 61`). Manual-mode writes therefore remain strictly **LOCKED** until DGP confirms the firmware sequence.

---

## 2. Bit Address Table — Coils (`FC01` Read / `FC05` Verified Config Write)

### 2.1 Live Status Bits (Polled — Single Block Read: `FC01`, Addr `0`, Qty `34`)

Verified in `BPC02_FULL_READ_RESULTS.txt` (`01 01 00 00 00 22 BC 13` → `01 01 05 3F 00 F8 01 00 05 36`):

| Dec Addr | Hex Addr | Row | Name | Values (`0` / `1`) | Observed | Access | Status |
|---:|---:|---:|---|---|---|---|---|
| 0–5 | `0x0000`–`0x0005` | 1–6 | Output 0–5 Status | `0: OFF` / `1: ON` | `1` (ON) | Read (`FC01`) | **HARDWARE VERIFIED** |
| 6–9 | `0x0006`–`0x0009` | 7–10 | Output 6–9 Status | `0: OFF` / `1: ON` | `0` (OFF) | Read (`FC01`) | **HARDWARE VERIFIED** |
| 10 | `0x000A` | 11 | DR Status | `0: OFF` / `1: ON` | `0` (OFF) | Read (`FC01`) | **HARDWARE VERIFIED** |
| 11–14 | `0x000B`–`0x000E` | 12–15 | PR1–PR4 Input Status | `0: OFF` / `1: ON` | `0` (OFF) | Read (`FC01`) | **HARDWARE VERIFIED** *(contactor feedback, p.8)* |
| 15–18 | `0x000F`–`0x0012` | 16–19 | MFI1–MFI4 Input Status | `0: OFF` / `1: ON` | `0` (OFF) | Read (`FC01`) | **HARDWARE VERIFIED** *(type at Reg 4180–4183)* |
| 19–24 | `0x0013`–`0x0018` | 20–25 | Pump 1–6 Status | `0: OFF` / `1: ON` | `1` (ON) | Read (`FC01`) | **HARDWARE VERIFIED** |
| 25 | `0x0019` | 26 | Drive Fail | `0: Normal` / `1: Failed` | `0` (Normal) | Read (`FC01`) | **HARDWARE VERIFIED** |
| 26–31 | `0x001A`–`0x001F` | 27–32 | Pump 1–6 Fail | `0: Normal` / `1: Failed` | `0` (Normal) | Read (`FC01`) | **HARDWARE VERIFIED** |
| 32 | `0x0020` | 33 | Floater L Level | `0: Not Conn.` / `1: Conn.` | `0` | Read (`FC01`) | **HARDWARE VERIFIED** |
| 33 | `0x0021` | 34 | Floater H Level | `0: Not Conn.` / `1: Conn.` | `0` | Read (`FC01`) | **HARDWARE VERIFIED** |

### 2.2 Manual Output Bits (`50–59` — LOCKED for Writes)

| Dec Addr | Hex Addr | Row | Name | Values | Access in App | Bench Evidence |
|---:|---:|---:|---|---|---|---|
| 50–59 | `0x0032`–`0x003B` | 35–44 | Output 0–9 Manual | `0: OFF` / `1: ON` | **CONTROL_LOCKED** | `FC01` reads `0`; `FC05` Coil 56 → Exception `0x02`; `FC15` → `01 0F 40 24` |

### 2.3 Hardware Configuration Bits (`4096–4101` & `4106–4110` — Verified `FC01` Read + `FC05` Write)

**Block A (`FC01`, Addr `4096`, Qty `6` → `0xFF`):**

| Dec Addr | Hex Addr | Row | Name | Values | Observed | Access in App | Status |
|---:|---:|---:|---|---|---|---|---|
| 4096 | `0x1000` | 45 | Pump 1 Activation | `0: Disable` / `1: Enable` | `1` (Enable) | **R/W (`FC01`/`FC05`)** | **HARDWARE VERIFIED** |
| 4097 | `0x1001` | 46 | Pump 2 Activation | `0: Disable` / `1: Enable` | `1` (Enable) | **R/W (`FC01`/`FC05`)** | **HARDWARE VERIFIED** |
| 4098 | `0x1002` | 47 | Pump 3 Activation | `0: Disable` / `1: Enable` | `1` (Enable) | **R/W (`FC01`/`FC05`)** | **HARDWARE VERIFIED** |
| 4099 | `0x1003` | 48 | Pump 4 Activation | `0: Disable` / `1: Enable` | `1` (Enable) | **R/W (`FC01`/`FC05`)** | **HARDWARE VERIFIED** |
| 4100 | `0x1004` | 49 | Pump 5 Activation | `0: Disable` / `1: Enable` | `1` (Enable) | **R/W (`FC01`/`FC05`)** | **HARDWARE VERIFIED** |
| 4101 | `0x1005` | 50 | Pump 6 Activation | `0: Disable` / `1: Enable` | `1` (`1→0→1` tested!) | **R/W (`FC01`/`FC05`)** | **FC05 WRITE VERIFIED** |

**Block B (`FC01`, Addr `4106`, Qty `5` → `0xD0 = 0b1101_0000`):**

| Dec Addr | Hex Addr | Row | Name | Values | Observed (`0xD0` LSB-first) | Access in App | Status |
|---:|---:|---:|---|---|---|---|---|
| 4106 | `0x100A` | 51 | Control Phase Activation | `0: Disable` / `1: Enable` | `0` (Disable) | **R/W (`FC01`/`FC05`)** | **HARDWARE VERIFIED** |
| 4107 | `0x100B` | 52 | OFF Changing Activation | `0: Disable` / `1: Enable` | `0` (Disable) | **R/W (`FC01`/`FC05`)** | **HARDWARE VERIFIED** |
| 4108 | `0x100C` | 53 | Time Changing Activation | `0: Disable` / `1: Enable` | `0` (Disable) | **R/W (`FC01`/`FC05`)** | **HARDWARE VERIFIED** |
| 4109 | `0x100D` | 54 | Reserve Auto Activation | `0: Disable` / `1: Enable` | `0` (Disable) | **R/W (`FC01`/`FC05`)** | **HARDWARE VERIFIED** |
| 4110 | `0x100E` | 55 | Reserve Auto Type | `0: NO` / `1: NC` | `1` (NC) | **R/W (`FC01`/`FC05`)** | **HARDWARE VERIFIED** |

*(Bits 6–7 of `0xFF` and Bits 5–7 of `0xD0` are unmasked padding bits beyond `quantity` and are ignored by LSB-first decoding.)*

---

## 3. Word Address Table — Holding Registers (`FC03` Read / `FC06` Verified Config Write)

### 3.1 Live Measurements, Run Times & Errors (Polled via 4 Gap-Aware `FC03` Blocks)

Poll blocks (`Bpc02RegisterMap.pollBlocks`): `0 qty 1`, `2 qty 9`, `34 qty 12`, `80 qty 1`.

| Dec Addr | Hex Addr | Row | Name | Type | Scale / Unit | Observed (`FULL_READ`) | Verification Status |
|---:|---:|---:|---|---|---|---|---|
| 0 | `0x0000` | 1 | Displayed Page Number | UINT16 | `1` (index) | `0` | **HARDWARE VERIFIED** |
| 2 | `0x0002` | 2 | System Pressure | UINT16 | `× 0.01 bar` | `0` (`0.00 bar`, bench idle) | **FC03 VERIFIED** *(0.01 bar documented)* |
| 3–5 | `0x0003`–`0x0005` | 3–5 | Voltages `Vrn`, `Vsn`, `Vtn` | UINT16 | `× 1 V` | `0 V` *(Coil 4106 OFF)* | **FC03 VERIFIED** *(Volt documented)* |
| 6–8 | `0x0006`–`0x0008` | 6–8 | Voltages `Vrs`, `Vst`, `Vtr` | UINT16 | `× 1 V` | `0 V` *(Coil 4106 OFF)* | **FC03 VERIFIED** *(Volt documented)* |
| 9 | `0x0009` | 9 | Mains Frequency | UINT16 | `× 0.01 Hz` | **`5410` (`54.10 Hz`)** | **HARDWARE VERIFIED** *(live non-zero!)* |
| 10 | `0x000A` | 10 | Drive Frequency | UINT16 | **UNVERIFIED** | `1000` (`0x03E8`, range `0–1600`) | **AMBIGUOUS** *(displayed raw until scale confirmed)* |
| 34–36 | `0x0022`–`0x0024` | 11–13 | Pump 1 Work Time (`s`, `min`, `h`) | UINT16 | `1 s / 1 min / 1 h` | `2 h 44 m 40 s → 45 s` (+5 s) | **HARDWARE VERIFIED** |
| 37–39 | `0x0025`–`0x0027` | 14–16 | Pump 2 Work Time (`s`, `min`, `h`) | UINT16 | `1 s / 1 min / 1 h` | `2 h 42 m 57 s → 43 m 02 s` (rollover) | **HARDWARE VERIFIED** |
| 40–42 | `0x0028`–`0x002A` | 17–19 | Pump 3 Work Time (`s`, `min`, `h`) | UINT16 | `1 s / 1 min / 1 h` | `2 h 41 m 53 s → 58 s` (+5 s) | **HARDWARE VERIFIED** |
| 43–45 | `0x002B`–`0x002D` | 20–22 | Pump 4 Work Time (`s`, `min`, `h`) | UINT16 | `1 s / 1 min / 1 h` | `2 h 40 m 59 s → 41 m 04 s` (rollover) | **HARDWARE VERIFIED** |
| 80 | `0x0050` | 25 | System Error Bitfield | BITFIELD16 | See §3.2 | `0x0000` (all clear) | **HARDWARE VERIFIED** |

*(Pumps 5 and 6 have no work-time registers documented in `BPC-02.pdf`.)*

### 3.2 System Error Bitfield — Holding Register `80` (`0x0050`)

Standard LSB = bit 0 (`(raw ushr bit) and 1`). Any set bit outside `3–10` is surfaced as an undocumented-bit warning.

| Bit | Mask | Error Description (Manual p. 27) | Troubleshooting Summary (Manual p. 23) |
|---:|---:|---|---|
| 0–2 | `0x0007` | *(Undocumented)* | Flagged if non-zero |
| 3 | `0x0008` | Floater Error | Storage tank water level below limit |
| 4 | `0x0010` | Max Pressure Error | Water pressure exceeded configured maximum |
| 5 | `0x0020` | Pressure Sensor Disconnected | Sensor cable/connection open |
| 6 | `0x0040` | Control Phase Sensitivity Error | R/S/T phase imbalance exceeded limit |
| 7 | `0x0080` | Control Phase Max Voltage Error | Input voltage above upper limit |
| 8 | `0x0100` | Control Phase Min Voltage Error | Input voltage below lower limit |
| 9 | `0x0200` | Control Phase Sequence Error | Input phases R/S/T swapped |
| 10 | `0x0400` | Pump 1 Error | Listed as `Pump1 Error` *(Pumps 1–6 also have Coil fail bits `26–31`)* |
| 11–15 | `0xF800` | *(Undocumented)* | Flagged if non-zero |

### 3.3 Manual Control Words (`50–51`, `99` — LOCKED for Writes)

| Dec Addr | Hex Addr | Ref | Name | Scale / Values | Observed | Access in App | Bench Evidence |
|---:|---:|---|---|---|---|---|---|
| 50 | `0x0032` | Row 23 | Analog Output 0 Manual | `0–500` (`× 0.1 Hz`) | `0` | **CONTROL_LOCKED** | `FC03` OK; `FC06 = 50` → Exception `0x02` |
| 51 | `0x0033` | Row 24 | Analog Output 1 Manual | `0–500` (`× 0.1 Hz`) | `0` | **CONTROL_LOCKED** | `FC03` OK; locked with Word 50 |
| 99 | `0x0063` | p. 28 | Multi Function Register | `1234`: Manual, `0`: Auto | `0` | **CONTROL_LOCKED** | `FC06 = 1234` echoed OK; `FC06 = 0` → Exception `0x03` |

### 3.4 Hardware Configuration Words (`4173`, `4175`, `4177–4186`, `4195`)

Verified `FC03` contiguous blocks (`Bpc02RegisterMap.configurationBlocks`): `4173 (qty 1)`, `4175 (qty 1)`, **`4177 (qty 10)`**, `4195 (qty 1)`.

| Dec Addr | Hex Addr | Row | Name | Scale / Enum Values | Range | Observed | Access in App | Status |
|---:|---:|---:|---|---|---|---|---|---|
| 4173 | `0x104D` | 56 | Control Status | `0–1` *(p.16: disabling stops system)* | `0–1` | `0` | **Read-Only (Protected)** | **HARDWARE VERIFIED** |
| 4175 | `0x104F` | 57 | Sleep Gap | `× 0.01 bar` | `1–100` | `40` (`0.40 bar`, `40→41→40`!) | **R/W (`FC03`/`FC06`)** | **FC06 WRITE VERIFIED** |
| 4177 | `0x1051` | 58 | Auto Service Off Time | `× 1 h` | `1–9999` | `159` (`159 h`) | **R/W (`FC03`/`FC06`)** | **HARDWARE VERIFIED** |
| 4178 | `0x1052` | 59 *(PDF: `417B`)* | Auto Service On Time | `× 0.1 s` | `1–999` | `33` (`3.3 s`) | **R/W (`FC03`/`FC06`)** | **HARDWARE VERIFIED** |
| 4179 | `0x1053` | 60 | Auto Service Interval | `× 1 min` | `1–99` | `11` (`11 min`) | **R/W (`FC03`/`FC06`)** | **HARDWARE VERIFIED** |
| 4180 | `0x1054` | 61 *(PDF: `41B0`)* | MFI1 Type | `0:Disable, 1:PS, 2:MP, 3:EMG, 4:CP, 5:FL` | `0–5` | `0` (`Disable`) | **R/W (`FC03`/`FC06`)** | **HARDWARE VERIFIED** |
| 4181 | `0x1055` | 62 | MFI2 Type | Same as MFI1 | `0–5` | `0` (`Disable`) | **R/W (`FC03`/`FC06`)** | **HARDWARE VERIFIED** |
| 4182 | `0x1056` | 63 | MFI3 Type | Same as MFI1 | `0–5` | `0` (`Disable`) | **R/W (`FC03`/`FC06`)** | **HARDWARE VERIFIED** |
| 4183 | `0x1057` | 64 | MFI4 Type | Same as MFI1 | `0–5` | `0` (`Disable`) | **R/W (`FC03`/`FC06`)** | **HARDWARE VERIFIED** |
| 4184 | `0x1058` | 65 | MFO1 Type | `0:Disable, 1:E Valve, 2:Fan, 3:Alarm, 4:DC3, 5:MC3` | `0–5` | `0` (`Disable`) | **R/W (`FC03`/`FC06`)** | **HARDWARE VERIFIED** |
| 4185 | `0x1059` | 66 | MFO2 Type | Same as MFO1 | `0–5` | `0` (`Disable`) | **R/W (`FC03`/`FC06`)** | **HARDWARE VERIFIED** |
| 4186 | `0x105A` | 67 | MFO3 Type | Same as MFO1 | `0–5` | `0` (`Disable`) | **R/W (`FC03`/`FC06`)** | **HARDWARE VERIFIED** |
| 4195 | `0x1063` | 68 | ModBus Address | Slave ID (`1–247`) | `1–247` | `1` | **Read-Only (Protected)** | **HARDWARE VERIFIED** |

---

## 4. Undocumented Registers Observed During Exploratory Scans (DO NOT USE)

| Dec Addr | Observed Raw Values | Notes |
|---:|---|---|
| `1` | `0` | Gap between Page Number (`0`) and System Pressure (`2`) |
| `11` | `1000` (`0x03E8`) | Constant, matches Word `10` (`1000`) — unconfirmed |
| `12–14` | `18655–20447`, `6658–9002` | Live varying internal values; undocumented |
| `15–22`, `24`, `27–29` | `0` | Constant zero during scan |
| `23` | `10` (`0x000A`) | Constant |
| `25–26` | `5000`, `5000` (`0x1388`) | Possibly 50.00 Hz internal parameter; unconfirmed |
| `30–33` | `~3217–3254` | Live varying internal values; undocumented |

---

## 5. Samin ES 487 Gateway Configuration Summary

| ES 487 Setting | Required Value for DGP BPC02 |
|---|---|
| Power Supply | `5–48 V DC` (12 V DC adapter included with ES 487) |
| RS485 Wiring | 2-wire `A (+)` to BPC02 `A`, `B (-)` to BPC02 `B`, plus common `GND`; `120 Ω` termination at bus ends |
| Serial Parameters | `9600` bps, `8` data bits, `No` parity, `1` stop bit (`9600 8N1`) |
| Serial Protocol | `Modbus RTU` |
| Network Mode | `TCP Server` |
| Protocol Conversion | `Modbus TCP/IP ↔ Modbus RTU` (recommended) or `Transparent` (`RTU over TCP` in app) |
| IP Configuration | `Static IP` recommended on plant LAN (or `DHCP` discovered via ES 487 IP Finder / Web UI) |
| TCP Port | Configured TCP server port in ES 487 Web UI (commonly `502`) |
| Target Unit / Slave ID | `1` (or the Slave ID configured in BPC02 `Hardware → Network`) |
