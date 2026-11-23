# DGP BPC02 — Complete Official & Hardware-Verified Modbus Register Map

> **Authoritative Sources:**
> 1. **`Booster Pump Memory Map.xlsx`** (official DGP complete register map: **56 Bit addresses + 98 Word addresses = 154 registers**, exact `R` vs `RW` permissions, Word 80/81 error bitfields, and Word 99 = `25` Reset Pump Error).
> 2. **Physical Hardware Audits:** `BPC02_FULL_READ_RESULTS.txt` and `BPC02_WRITE_TEST_RESULTS.txt` (COM4 `9600 8N1`, Slave ID `1`) confirming supported function codes (`FC01`, `FC03`, `FC05`, `FC06`), zero-based PDU addressing, and EEPROM write timing (~240–250 ms).
>
> **Selected RS485-to-Ethernet Gateway:** **Samin ES 487** (Modbus TCP/IP ↔ Modbus RTU Server, `9600 8N1`).
>
> **App Access & Safety Posture (Strictly Enforced by Official Excel):**
> - **LIVE POLL (`FC01`, `FC03`):** 9 gap-free contiguous blocks including Drive & Pump 1–6 activation coils (`4096–4102`, detecting manual `S` deactivation live), Drive 1/2 frequencies (`25–26`), System Errors Word 1 & Word 2 (`80–81`), System Pressure Setpoint (`4096`), and Control Status (`4173`).
> - **OFFICIAL RW WRITES (`FC05`, `FC06` — 33 registers total):**
>   - **7 RW Coils (`FC05`):** `4096` (Mode 2,3 Drive Activation), `4097–4102` (Pump 1–6 Activation).
>   - **25 RW Config Words (`FC06`):** `4096`, `4123–4125`, `4127–4129`, `4135–4138`, `4143`, `4149–4151`, `4161–4164`, `4166`, `4173`, `4175`, `4177–4179`.
>   - **1 RW Command Word (`FC06`):** `99 = 25` (**Reset Pump Error**).
> - **OFFICIAL READ-ONLY (`R`):** Coils `4106–4110`, Words `4097–4111`, `4122`, `4131–4134`, `4142`, `4152`, `4156`, `4170`, `4180–4195`, `4206–4210`, and Manual Outputs (`Coils 50–59`, `Words 50–51`) are strictly read-only in firmware — write UI is never offered on `R` registers.

---

## 1. Confirmed Communication & Function Code Support Matrix

| Parameter | Value | Status |
|---|---|---|
| Controller Model | DGP BPC02 Booster Pump Controller | **VERIFIED** |
| Serial Protocol | Modbus RTU over RS485 (2-wire A/B + GND) | **VERIFIED** |
| Serial Settings | `9600` baud, `8` data bits, `No` parity, `1` stop bit (`9600 8N1`) | **VERIFIED** |
| Default Slave / Unit ID | `1` (Word `4195`, read-only over Modbus) | **VERIFIED** |
| Addressing Base | **Zero-based PDU addressing (`offset = 0`)** | **VERIFIED** |
| Byte Order (16-bit words) | Big-endian (MSB first), unsigned `UINT16` | **VERIFIED** |
| Supported Function Codes | **`FC01`** (Read Coils), **`FC03`** (Read Holding), **`FC05`** (Write Single Coil), **`FC06`** (Write Single Register) | **HARDWARE VERIFIED** |
| Unsupported Function Codes | **`FC02`**, **`FC04`**, **`FC15` (`0x0F`)**, **`FC16` (`0x10`)** — return 4-byte valid-CRC rejection frame | **CONFIRMED UNSUPPORTED** |
| EEPROM Write Latency | `~240–250 ms` on direct RS485 (`FC05`/`FC06`); app enforces `150 ms` pre-write quiet gap + `350 ms` post-write settle before read-back | **HARDWARE VERIFIED** |

### Key Firmware Findings & Corrections vs Older Printed `BPC-02.pdf`
1. **Coil `4096` vs `4097–4102` Off-by-1 Fix:** `Booster Pump Memory Map.xlsx` confirms Coil **`4096`** is **`Mode 2,3 Drive Activation`** (`RW`), and **`Pump 1–6 Activation`** are **`4097–4102`** (`RW`). Polling `FC01 4096 qty 7` live immediately detects manually deactivated pumps (`S` on controller LCD).
2. **Coils `4106–4110` & Words `4180–4187` are `R` (Read-Only):** Attempting writes to these read-only addresses caused timeouts/disconnects; the app strictly blocks writes to all `R` registers. Also `4110` (`Reserve Automatic Type`) is **`0 = NC, 1 = NO`**.
3. **Manual Outputs (`Coils 50–59`, `Words 50–51`) are `R` (Read-Only):** Matches physical `Exception 0x02` bench tests. **`Word 99`** only defines `0: No Function` and **`25: Reset Pump Error`** (pulse command that auto-clears to `0`).
4. **Drive Frequencies (`Words 25–26`) & System Error Bitfields (`Words 80–81`):** `Words 25` and `26` are official `Drive1 Frequency` and `Drive2 Frequency` (`0~5000`, `0.01 Hz`), replacing the older undocumented Word `10`. Word `80` defines all 16 bits (`0..15`, starting at `Bit 0 = FLOATER_ERROR`), and Word `81` defines bits `0..7` (`MODE3_*`, `FULL_LOAD`, `SYSTEM_STOPPED`, `EMG_STOPPED`, `BATTERY_VOLTAGE`).

---

## 2. Complete Bit Address Table — Coils (56 Addresses: `FC01` Read / `FC05` for `RW`)

### 2.1 Live Status Coils (Polled — `FC01` Addr `0`, Qty `34`)

| Dec Addr | Hex Addr | Description | Values (`0` / `1`) | Access |
|---:|---:|---|---|---|
| `0–9` | `0x0000`–`0x0009` | Output `0..9` Status | `0: OFF` / `1: ON` | `R` |
| `10` | `0x000A` | DR Status | `0: OFF` / `1: ON` | `R` |
| `11–14` | `0x000B`–`0x000E` | PR1–PR4 Status (Contactor Feedback) | `0: OFF` / `1: ON` | `R` |
| `15–18` | `0x000F`–`0x0012` | MFI1–MFI4 Status | `0: OFF` / `1: ON` | `R` |
| `19–24` | `0x0013`–`0x0018` | Pump 1–6 Running Status | `0: OFF` / `1: ON` | `R` |
| `25` | `0x0019` | Drive Fail | `0: Normal` / `1: Failed` | `R` |
| `26–31` | `0x001A`–`0x001F` | Pump 1–6 Fail | `0: Normal` / `1: Failed` | `R` |
| `32` | `0x0020` | Floater L Level | `0: Not Connected` / `1: Connected` | `R` |
| `33` | `0x0021` | Floater H Level | `0: Not Connected` / `1: Connected` | `R` |

### 2.2 Manual Output Coils (`50–59` — Read-Only in Firmware)

| Dec Addr | Hex Addr | Description | Values | Access |
|---:|---:|---|---|---|
| `50–59` | `0x0032`–`0x003B` | Output `0..9` Manual | `0: OFF` / `1: ON` | `R` *(Locked — `0x02` on write)* |

### 2.3 Activation & Configuration Coils (`4096–4102` Live Polled `RW`; `4106–4110` Config `R`)

| Dec Addr | Hex Addr | Description | Values | Access |
|---:|---:|---|---|---|
| `4096` | `0x1000` | Mode 2,3 Drive Activation | `0: Disable` / `1: Enable` | **`RW` (`FC05`)** *(Live Polled)* |
| `4097` | `0x1001` | Pump 1 Activation (`0` = `S` on LCD) | `0: Disable` / `1: Enable` | **`RW` (`FC05`)** *(Live Polled)* |
| `4098` | `0x1002` | Pump 2 Activation (`0` = `S` on LCD) | `0: Disable` / `1: Enable` | **`RW` (`FC05`)** *(Live Polled)* |
| `4099` | `0x1003` | Pump 3 Activation (`0` = `S` on LCD) | `0: Disable` / `1: Enable` | **`RW` (`FC05`)** *(Live Polled)* |
| `4100` | `0x1004` | Pump 4 Activation (`0` = `S` on LCD) | `0: Disable` / `1: Enable` | **`RW` (`FC05`)** *(Live Polled)* |
| `4101` | `0x1005` | Pump 5 Activation (`0` = `S` on LCD) | `0: Disable` / `1: Enable` | **`RW` (`FC05`)** *(Live Polled)* |
| `4102` | `0x1006` | Pump 6 Activation (`0` = `S` on LCD) | `0: Disable` / `1: Enable` | **`RW` (`FC05`)** *(Live Polled)* |
| `4106` | `0x100A` | Control Phase Activation | `0: Disable` / `1: Enable` | `R` |
| `4107` | `0x100B` | Off Changing Activation | `0: Disable` / `1: Enable` | `R` |
| `4108` | `0x100C` | Time Changing Activation | `0: Disable` / `1: Enable` | `R` |
| `4109` | `0x100D` | Reserve Automatic Activation | `0: Disable` / `1: Enable` | `R` |
| `4110` | `0x100E` | Reserve Automatic Type | **`0: NC`** / **`1: NO`** | `R` |

---

## 3. Complete Word Address Table — Holding Registers (98 Addresses: `FC03` / `FC06`)

### 3.1 Live Telemetry, Manual Analog & Command Words (`0–99`)

| Dec Addr | Hex Addr | Description | Range / Values | Unit / Scale | Access |
|---:|---:|---|---|---|---|
| `0` | `0x0000` | Displayed Page Number | index | `1` | `R` *(Live)* |
| `2` | `0x0002` | System Pressure | `0~6000` | `0.01 bar` | `R` *(Live)* |
| `3–5` | `0x0003`–`0x0005` | `Vrn`, `Vsn`, `Vtn` | phase-neutral | `1 V` | `R` *(Live)* |
| `6–8` | `0x0006`–`0x0008` | `Vrs`, `Vst`, `Vtr` | phase-phase | `1 V` | `R` *(Live)* |
| `9` | `0x0009` | Mains Frequency | `0.01 Hz` | `0.01 Hz` | `R` *(Live)* |
| `25` | `0x0019` | **Drive 1 Frequency** | `0~5000` | `0.01 Hz` | `R` *(Live)* |
| `26` | `0x001A` | **Drive 2 Frequency** | `0~5000` | `0.01 Hz` | `R` *(Live)* |
| `34–45` | `0x0022`–`0x002D` | Pump `1..4` Work Time (`Sec`, `Min`, `Hour` × 4) | `s / min / h` | `1` | `R` *(Live)* |
| `50–51` | `0x0032`–`0x0033` | Analog Output `0..1` Manual | `0~500` | `0.1 Hz` | `R` *(Locked)* |
| `80` | `0x0050` | **System Error Word 1** | 16-bit bitfield (§3.2) | bitmask | `R` *(Live)* |
| `81` | `0x0051` | **System Error Word 2** | 16-bit bitfield (§3.2) | bitmask | `R` *(Live)* |
| `99` | `0x0063` | **Multi Function Register** | `0: No Function`, **`25: Reset Pump Error`** | pulse cmd | **`RW` (`FC06 = 25`)** |

### 3.2 System Error Bitfields — Words `80` & `81`

**Word `80` (`0x0050`):**

| Bit | Mask | Flag | Bit | Mask | Flag |
|---:|---:|---|---:|---:|---|
| `0` | `0x0001` | `FLOATER_ERROR` | `8` | `0x0100` | `PUMP1_ERROR` |
| `1` | `0x0002` | `MAX_PRESSURE_ERROR` | `9` | `0x0200` | `PUMP2_ERROR` |
| `2` | `0x0004` | `SENSOR_DISCONNECT_ERROR` | `10` | `0x0400` | `PUMP3_ERROR` |
| `3` | `0x0008` | `CP_SENSIVITY_ERROR` | `11` | `0x0800` | `PUMP4_ERROR` |
| `4` | `0x0010` | `CP_MAX_ERROR` | `12` | `0x1000` | `PUMP5_ERROR` |
| `5` | `0x0020` | `CP_MIN_ERROR` | `13` | `0x2000` | `PUMP6_ERROR` |
| `6` | `0x0040` | `CP_SQUENCY_ERROR` | `14` | `0x4000` | `ALL_PUMP_ERROR` |
| `7` | `0x0080` | `DRIVE_ERROR` | `15` | `0x8000` | `SYSTEM_STOP_BY_USER` |

**Word `81` (`0x0051`):**

| Bit | Mask | Flag | Bit | Mask | Flag |
|---:|---:|---|---:|---:|---|
| `0` | `0x0001` | `MODE3_MAIN_PUMP_ERROR` | `4` | `0x0010` | `SYSTEM_STOPPED` |
| `1` | `0x0002` | `MODE3_DRIVE_ERROR` | `5` | `0x0020` | `EMG_STOPPED` |
| `2` | `0x0004` | `FULL_LOAD_ERROR` | `6` | `0x0040` | `RESERVE` |
| `3` | `0x0008` | `RESERVE` | `7` | `0x0080` | `BATTERY_VOLTAGE_ERROR` |

### 3.3 Configuration & Parameter Words (`4096–4210`)

| Dec Addr | Hex Addr | Description | Range / Enum | Unit / Scale | Access |
|---:|---:|---|---|---|---|
| `4096` | `0x1000` | **System Pressure Setpoint** | `0~6000` | `0.01 bar` | **`RW`** *(Live)* |
| `4097` | `0x1001` | On Delay Pump 1 | `0~9999` | `s` | `R` |
| `4098` | `0x1002` | OFF Pressure Pump 1 | `0~6000` | `0.01 bar` | `R` |
| `4099` | `0x1003` | OFF Delay Pump 1 | `0~9999` | `s` | `R` |
| `4100–4103` | `0x1004`–`0x1007` | Pump 2 On Pressure / On Delay / OFF Pressure / OFF Delay | `0~6000` / `0~9999` | `0.01 bar` / `s` | `R` |
| `4104–4107` | `0x1008`–`0x100B` | Pump 3 On Pressure / On Delay / OFF Pressure / OFF Delay | `0~6000` / `0~9999` | `0.01 bar` / `s` | `R` |
| `4108–4111` | `0x100C`–`0x100F` | Pump 4 On Pressure / On Delay / OFF Pressure / OFF Delay | `0~6000` / `0~9999` | `0.01 bar` / `s` | `R` |
| `4122` | `0x101A` | Control Phase Type | `1: 1P`, `3: 3P` | enum | `R` |
| `4123` | `0x101B` | Control Phase Min Voltage | `180~210` | `V` | **`RW`** |
| `4124` | `0x101C` | Control Phase Max Voltage | `240~280` | `V` | **`RW`** |
| `4125` | `0x101D` | Control Phase On/Off Delay | `1~99` | `s` | **`RW`** |
| `4127` | `0x101F` | Control Phase Sensitivity | `5~20` | `%` | **`RW`** |
| `4128` | `0x1020` | Floater Trigger Level | `0~1000` | `kΩ` | **`RW`** |
| `4129` | `0x1021` | Floater On/Off Delay | `0~99` | `s` | **`RW`** |
| `4131` | `0x1023` | Floater Type | `0:Disable, 1:Internal, 2:External` | enum | `R` |
| `4132` | `0x1024` | Pressure Sensor Output Type | `2:4~20mA, 3:0~20mA, 4:0~5V, 5:0~10V, 6:2~10V` | enum | `R` |
| `4133` | `0x1025` | Number Of Pump | `1~4` | count | `R` |
| `4134` | `0x1026` | Number Of Variable Pump | `0~2` | count | `R` |
| `4135` | `0x1027` | Jockey Type | `0:Disable, 1:Jockey On, 2:Jockey Off` | enum | **`RW`** |
| `4136` | `0x1028` | P Value (PID) | `0~999` | `1` | **`RW`** |
| `4137` | `0x1029` | I Value (PID) | `0~999` | `1` | **`RW`** |
| `4138` | `0x102A` | D Value (PID) | `0~999` | `1` | **`RW`** |
| `4142` | `0x102E` | Max Pressure Value | `0~2500` | `0.01 bar` | `R` |
| `4143` | `0x102F` | Sensor Calibration Value | `1~999` | `1` | **`RW`** |
| `4149` | `0x1035` | Wake up Differential | `0~1000` | `0.01 bar` | **`RW`** |
| `4150` | `0x1036` | Sleep Frequency | `0~5000` | `0.01 Hz` | **`RW`** |
| `4151` | `0x1037` | Sleep Off Delay | `0~99` | `s` | **`RW`** |
| `4152` | `0x1038` | Pressure Sensor Input Type | `0:0-6, 1:0-10, 2:0-16, 3:0-25, 4:0-40, 5:0-60 bar, 10:PS` | enum | `R` |
| `4156` | `0x103C` | Mode 3 Switch Time | `2~100` | `0.01 s` | `R` |
| `4161` | `0x1041` | Time Changing Time | `1~9999` | `min` | **`RW`** |
| `4162` | `0x1042` | Add Delay | `0~999` | `0.1 s` | **`RW`** |
| `4163` | `0x1043` | Remove Delay | `0~999` | `0.1 s` | **`RW`** |
| `4164` | `0x1044` | Wake up On Delay | `0~99` | `s` | **`RW`** |
| `4166` | `0x1046` | Constant Pump Percent | `0~100` | `%` | **`RW`** |
| `4170` | `0x104A` | Wiring Mode | `1~3` | enum | `R` |
| `4173` | `0x104D` | **Control Status** | `0:Disable, 1:PID, 2:ON/OFF` | enum | **`RW`** *(Live)* |
| `4175` | `0x104F` | Sleep Gap | `1~100` | `0.01 bar` | **`RW`** *(Verified)* |
| `4177` | `0x1051` | Auto Service Off Time | `1~9999` | `h` | **`RW`** |
| `4178` | `0x1052` | Auto Service On Time | `1~999` | `0.1 s` | **`RW`** |
| `4179` | `0x1053` | Auto Service Interval | `1~99` | `min` | **`RW`** |
| `4180–4183` | `0x1054`–`0x1057` | MFI1–MFI4 Type | `0:DISABLE, 1:PS, 2:TS, 3:MP, 4:EMG, 5:CP, 6:FL, 7:PR5, 8:PR6, 9:RESET, 10:FIX` | enum | `R` |
| `4184–4187` | `0x1058`–`0x105B` | MFO1–MFO4 Type | `0:DISABLE, 1:MC3, 2:DC4, 3:MC4, 4:FAN, 5:ALARM, 6:E_VALVE, 7:RUN, 8:READY, 9:FILLER, 10–13:P1..P4_ERR` | enum | `R` |
| `4188` | `0x105C` | Serial Number | `0~99999` | `1` | `R` |
| `4189` | `0x105D` | Start Frequency | `100~5000` | `0.01 Hz` | `R` |
| `4190` | `0x105E` | Full Load Status | `0:Disable, 1:Enable` | enum | `R` |
| `4191` | `0x105F` | Full Load Pressure | `0~9999` | `0.01 bar` | `R` |
| `4192` | `0x1060` | Full Load Time | `0~9999` | `s` | `R` |
| `4193` | `0x1061` | Stop Frequency | `100~5000` | `0.01 Hz` *(Excel typo `0.01Bar`)* | `R` |
| `4194` | `0x1062` | Control Direction | `0:Down, 1:Up` | enum | `R` |
| `4195` | `0x1063` | ModBus Address (Slave ID) | `1~247` | `1` | `R` |
| `4206–4207` | `0x106E`–`0x106F` | AO1 Type / AO2 Type | `0:Pump1, 1:Pump2` | enum | `R` |
| `4208` | `0x1070` | LCD Contrast | `0~20` | `1` | `R` |
| `4209–4210` | `0x1071`–`0x1072` | AO1 Gain / AO2 Gain | `900~1100` | `× 0.0001` | `R` |

---

## 4. Recommended Samin ES 487 Gateway Settings (Critical for Reliable Writes)

| ES 487 Setting | Recommended Value | Why |
|---|---|---|
| Server or Client | `Use server MODBUS TCP/IP` | Converts Modbus TCP (MBAP) from Android app to RS485 RTU |
| TCP or UDP | `Use TCP` | Standard reliable Modbus TCP transport |
| Listening network port | `502` (or `4001`, matching app Port) | Must match the Port field in the app |
| **Waiting for slave reply (ms)** | **`1000`** *(do NOT leave at `300`!)* | BPC02 EEPROM writes take `~250–350 ms` on RS485; `300 ms` causes ES 487 to drop writes |
| **Number of socket(s)** | **`2`** (or `4`) | Prevents socket exhaustion during quick reconnects |
| Keep alive timeout (sec) | `60` | Keeps idle LAN link alive |
| Inactivity timeout (sec) | `60` | Prevents premature drops |
| RS485 Baud / Frame | `9600`, `8 / NONE / 1` | Required by BPC02 firmware |
| Serial Buffer timeout | `50`–`100` ms (or `Auto buffer timeout`) | Clean frame boundary without adding excess latency |
| App Response Timeout | `2500` ms (new app default) | Exceeds ES 487 slave reply timeout cleanly |
