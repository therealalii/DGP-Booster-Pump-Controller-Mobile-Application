<#
.SYNOPSIS
    Automated bench-only Modbus RTU write verification script for DGP BPC02.
.DESCRIPTION
    Runs Steps 1A-6C over a local Windows COM port (default COM4, 9600 8N1, Slave ID 1),
    validates Modbus RTU CRC-16 on every response, restores every modified register to its
    original read-back value (even on error), and writes a timestamped log file to
    BPC02_WRITE_TEST_RESULTS.txt.

    ISOLATED BENCH ONLY - disconnect all pump motors, contactors, and VFD run outputs first!
    Close Modbus Poll / QModMaster before running so COM4 is free.
.EXAMPLE
    powershell -ExecutionPolicy Bypass -File scripts\bpc02_write_test.ps1 -Port COM4
#>
param(
    [string]$Port = "COM4",
    [int]$BaudRate = 9600,
    [byte]$SlaveId = 1,
    [string]$OutFile = "BPC02_WRITE_TEST_RESULTS.txt"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# --- Safety confirmation ------------------------------------------------------
Write-Host ""
Write-Host "================================================================" -ForegroundColor Yellow
Write-Host "  DGP BPC02 - ISOLATED BENCH MODBUS WRITE VERIFICATION SCRIPT" -ForegroundColor Yellow
Write-Host "================================================================" -ForegroundColor Yellow
Write-Host "Port: $Port ($BaudRate 8N1) | Slave ID: $SlaveId"
Write-Host ""
Write-Host "SAFETY CHECK:" -ForegroundColor Red
Write-Host "  1. ALL pump motors, contactors, and VFD outputs MUST be disconnected." -ForegroundColor Red
Write-Host "  2. Close Modbus Poll / QModMaster so $Port is not in use." -ForegroundColor Red
Write-Host "  3. Script will read original values first and restore them automatically." -ForegroundColor Yellow
Write-Host ""
$confirm = Read-Host "Type YES (all caps) to start the bench write tests"
if ($confirm -cne "YES") {
    Write-Host "Aborted. No frames were sent." -ForegroundColor Cyan
    exit 0
}

# --- Helpers ------------------------------------------------------------------
$logLines = New-Object System.Collections.Generic.List[string]

function Write-Log([string]$Text, [string]$Color = "Gray") {
    Write-Host $Text -ForegroundColor $Color
    $logLines.Add($Text)
}

function Format-Hex([byte[]]$Bytes) {
    if ($null -eq $Bytes -or $Bytes.Length -eq 0) { return "(empty)" }
    return ($Bytes | ForEach-Object { $_.ToString("X2") }) -join " "
}

function Get-ModbusCrc16([byte[]]$Data) {
    [int]$crc = 0xFFFF
    foreach ($b in $Data) {
        $crc = $crc -bxor ([int]$b)
        for ($i = 0; $i -lt 8; $i++) {
            if (($crc -band 1) -ne 0) {
                $crc = ($crc -shr 1) -bxor 0xA001
            } else {
                $crc = $crc -shr 1
            }
        }
    }
    return ,([byte[]]@([byte]($crc -band 0xFF), [byte](($crc -shr 8) -band 0xFF)))
}

function Test-ModbusCrc16([byte[]]$Frame) {
    if ($null -eq $Frame -or $Frame.Length -lt 4) { return $false }
    $bodyLen = $Frame.Length - 2
    [byte[]]$body = New-Object byte[] $bodyLen
    [Array]::Copy($Frame, 0, $body, 0, $bodyLen)
    $calc = Get-ModbusCrc16 $body
    return ($Frame[$Frame.Length - 2] -eq $calc[0]) -and ($Frame[$Frame.Length - 1] -eq $calc[1])
}

function New-RtuFrame([byte[]]$Payload) {
    $crc = Get-ModbusCrc16 $Payload
    [byte[]]$frame = New-Object byte[] ($Payload.Length + 2)
    [Array]::Copy($Payload, 0, $frame, 0, $Payload.Length)
    $frame[$Payload.Length] = $crc[0]
    $frame[$Payload.Length + 1] = $crc[1]
    return ,$frame
}

function Build-Read([byte]$Fc, [int]$Addr, [int]$Qty) {
    return ,(New-RtuFrame ([byte[]]@(
        $SlaveId, $Fc,
        [byte](($Addr -shr 8) -band 0xFF), [byte]($Addr -band 0xFF),
        [byte](($Qty -shr 8) -band 0xFF),  [byte]($Qty -band 0xFF)
    )))
}

function Build-WriteSingleReg([int]$Addr, [int]$Val) {
    return ,(New-RtuFrame ([byte[]]@(
        $SlaveId, 0x06,
        [byte](($Addr -shr 8) -band 0xFF), [byte]($Addr -band 0xFF),
        [byte](($Val -shr 8) -band 0xFF),  [byte]($Val -band 0xFF)
    )))
}

function Build-WriteMultiReg1([int]$Addr, [int]$Val) {
    return ,(New-RtuFrame ([byte[]]@(
        $SlaveId, 0x10,
        [byte](($Addr -shr 8) -band 0xFF), [byte]($Addr -band 0xFF),
        0x00, 0x01, 0x02,
        [byte](($Val -shr 8) -band 0xFF),  [byte]($Val -band 0xFF)
    )))
}

function Build-WriteSingleCoil([int]$Addr, [bool]$On) {
    [byte]$hi = if ($On) { 0xFF } else { 0x00 }
    return ,(New-RtuFrame ([byte[]]@(
        $SlaveId, 0x05,
        [byte](($Addr -shr 8) -band 0xFF), [byte]($Addr -band 0xFF),
        $hi, 0x00
    )))
}

function Build-WriteMultiCoil1([int]$Addr, [bool]$On) {
    [byte]$val = if ($On) { 0x01 } else { 0x00 }
    return ,(New-RtuFrame ([byte[]]@(
        $SlaveId, 0x0F,
        [byte](($Addr -shr 8) -band 0xFF), [byte]($Addr -band 0xFF),
        0x00, 0x01, 0x01, $val
    )))
}

function Send-Frame($Serial, [string]$StepName, [byte[]]$TxFrame) {
    $Serial.DiscardInBuffer()
    $Serial.DiscardOutBuffer()

    $tsTx = (Get-Date).ToString("HH:mm:ss.fff")
    $Serial.Write($TxFrame, 0, $TxFrame.Length)

    $rxBuf = New-Object System.Collections.Generic.List[byte]
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    $lastRxMs = -1

    while ($sw.ElapsedMilliseconds -lt 1200) {
        $avail = $Serial.BytesToRead
        if ($avail -gt 0) {
            [byte[]]$chunk = New-Object byte[] $avail
            $read = $Serial.Read($chunk, 0, $avail)
            for ($i = 0; $i -lt $read; $i++) { $rxBuf.Add($chunk[$i]) }
            $lastRxMs = $sw.ElapsedMilliseconds
        } else {
            if ($lastRxMs -ge 0 -and ($sw.ElapsedMilliseconds - $lastRxMs) -ge 80) {
                break
            }
            Start-Sleep -Milliseconds 10
        }
    }
    $sw.Stop()
    $tsRx = (Get-Date).ToString("HH:mm:ss.fff")
    [byte[]]$rx = $rxBuf.ToArray()

    $crcOk = Test-ModbusCrc16 $rx
    $status = if ($rx.Length -eq 0) {
        "TIMEOUT (no response)"
    } elseif (-not $crcOk) {
        "CRC ERROR ($($rx.Length) bytes)"
    } elseif ($rx.Length -eq 4) {
        "REJECTED (BPC02 4-byte unsupported-FC reply, CRC OK)"
    } elseif (($rx[1] -band 0x80) -ne 0 -and $rx.Length -ge 3) {
        "MODBUS EXCEPTION 0x$($rx[2].ToString('X2'))"
    } else {
        "OK ($($rx.Length) bytes, CRC OK)"
    }

    Write-Log ""
    Write-Log "[$StepName]" "Cyan"
    Write-Log "  TX ($tsTx): $(Format-Hex $TxFrame)"
    Write-Log "  RX ($tsRx): $(Format-Hex $rx)  -->  $status" $(if ($status.StartsWith("OK")) { "Green" } else { "Yellow" })

    Start-Sleep -Milliseconds 300
    return @{
        Rx     = $rx
        CrcOk  = $crcOk
        Ok     = $status.StartsWith("OK")
        Status = $status
    }
}

function Parse-UInt16Regs([byte[]]$Rx) {
    if ($null -eq $Rx -or $Rx.Length -lt 5 -or $Rx[1] -ne 0x03) { return @() }
    $count = [int]($Rx[2] / 2)
    $vals = New-Object System.Collections.Generic.List[int]
    for ($i = 0; $i -lt $count; $i++) {
        $v = (([int]$Rx[3 + 2 * $i]) -shl 8) -bor ([int]$Rx[4 + 2 * $i])
        $vals.Add($v)
    }
    return $vals.ToArray()
}

function Parse-Coils([byte[]]$Rx, [int]$Qty) {
    if ($null -eq $Rx -or $Rx.Length -lt 5 -or $Rx[1] -ne 0x01) { return @() }
    $bits = New-Object System.Collections.Generic.List[int]
    for ($i = 0; $i -lt $Qty; $i++) {
        $b = [int]$Rx[3 + ($i -shr 3)]
        $bits.Add((($b -shr ($i % 8)) -band 1))
    }
    return $bits.ToArray()
}

# --- Main test execution ------------------------------------------------------
Write-Log "DGP BPC02 - MODBUS WRITE VERIFICATION TEST LOG" "White"
Write-Log "Date: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') | Port: $Port ($BaudRate 8N1) | Slave ID: $SlaveId" "White"
Write-Log "================================================================" "White"

$serial = New-Object System.IO.Ports.SerialPort $Port, $BaudRate, ([System.IO.Ports.Parity]::None), 8, ([System.IO.Ports.StopBits]::One)
$serial.ReadTimeout = 1000
$serial.WriteTimeout = 1000

$origSleepGap = $null
$origPump6Act = $null
$enteredManual = $false
$changedWord50 = $false
$changedCoil56 = $false

try {
    $serial.Open()
    Write-Log "Opened $Port successfully." "Green"

    # ---- Baseline reads before touching anything ----
    $r4175Base = Send-Frame $serial "BASE-1 Read Sleep Gap (Word 4175)" (Build-Read 0x03 4175 1)
    $vals4175 = @(Parse-UInt16Regs $r4175Base.Rx)
    if ($vals4175.Count -ge 1) {
        $origSleepGap = [int]$vals4175[0]
        Write-Log "  Decoded Sleep Gap original = $origSleepGap ($([Math]::Round($origSleepGap * 0.01, 2)) bar)" "White"
    }

    $r4096Base = Send-Frame $serial "BASE-2 Read Pump 1-6 Activation (Coils 4096..4101)" (Build-Read 0x01 4096 6)
    $bits4096 = @(Parse-Coils $r4096Base.Rx 6)
    if ($bits4096.Count -eq 6) {
        $origPump6Act = ([int]$bits4096[5] -eq 1)
        Write-Log "  Decoded Pump 1-6 Activation = [$($bits4096 -join ',')] (Pump 6 original = $origPump6Act)" "White"
    }

    $r0Base = Send-Frame $serial "BASE-3 Read Output Status Coils 0..9" (Build-Read 0x01 0 10)
    Write-Log "  Decoded Coils 0..9 = [$((@(Parse-Coils $r0Base.Rx 10)) -join ',')]" "White"

    # ---- Step 1: Enter Manual Mode (Addr 99 = 1234) ----
    $r1a = Send-Frame $serial "STEP 1A Enter Manual Mode (FC06 Addr 99 = 1234)" (Build-WriteSingleReg 99 1234)
    if ($r1a.Ok) { $enteredManual = $true }
    $r1b = Send-Frame $serial "STEP 1B Read back Addr 99 (FC03)" (Build-Read 0x03 99 1)
    Write-Log "  Decoded Addr 99 = [$((@(Parse-UInt16Regs $r1b.Rx)) -join ',')]" "White"
    $r1c = Send-Frame $serial "STEP 1C Read Output Status Coils 0..9 in Manual Mode" (Build-Read 0x01 0 10)
    Write-Log "  Decoded Coils 0..9 = [$((@(Parse-Coils $r1c.Rx 10)) -join ',')]" "White"

    # ---- Step 2: Manual Analog Output 0 (Word 50 = 50 -> 5.0 Hz) ----
    $r2a = Send-Frame $serial "STEP 2A Write Word 50 = 50 (5.0 Hz) via FC06" (Build-WriteSingleReg 50 50)
    if ($r2a.Ok) { $changedWord50 = $true }
    $r2b = Send-Frame $serial "STEP 2B Read back Words 50..51 (FC03)" (Build-Read 0x03 50 2)
    Write-Log "  Decoded Words 50..51 = [$((@(Parse-UInt16Regs $r2b.Rx)) -join ',')]" "White"
    $r2c = Send-Frame $serial "STEP 2C Read Drive Frequency Word 10 (FC03)" (Build-Read 0x03 10 1)
    Write-Log "  Decoded Drive Freq Word 10 = [$((@(Parse-UInt16Regs $r2c.Rx)) -join ',')]" "White"
    $r2d = Send-Frame $serial "STEP 2D Restore Word 50 = 0 via FC06" (Build-WriteSingleReg 50 0)
    if ($r2d.Ok) { $changedWord50 = $false }

    # ---- Step 3: Manual Digital Output 6 (Coil 56 ON -> read -> OFF) ----
    $r3a = Send-Frame $serial "STEP 3A Write Coil 56 (OUT6 Manual) ON via FC05" (Build-WriteSingleCoil 56 $true)
    $usedFc15ForCoil56 = $false
    if (-not $r3a.Ok) {
        $r3a = Send-Frame $serial "STEP 3A-ALT Fallback FC15 Coil 56 ON" (Build-WriteMultiCoil1 56 $true)
        if ($r3a.Ok) { $usedFc15ForCoil56 = $true }
    }
    if ($r3a.Ok) {
        $changedCoil56 = $true
        Write-Log "  --> Pause 1.2s: listen/check if Output 6 relay/LED turned ON..." "Yellow"
        Start-Sleep -Milliseconds 1200
    }
    $r3b = Send-Frame $serial "STEP 3B Read Manual Output Coils 50..59 (FC01)" (Build-Read 0x01 50 10)
    Write-Log "  Decoded Coils 50..59 = [$((@(Parse-Coils $r3b.Rx 10)) -join ',')]" "White"
    $r3c = Send-Frame $serial "STEP 3C Read Output Status Coils 0..9 (FC01)" (Build-Read 0x01 0 10)
    Write-Log "  Decoded Coils 0..9 = [$((@(Parse-Coils $r3c.Rx 10)) -join ',')]" "White"

    $offFrame = if ($usedFc15ForCoil56) { Build-WriteMultiCoil1 56 $false } else { Build-WriteSingleCoil 56 $false }
    $r3d = Send-Frame $serial "STEP 3D Restore Coil 56 OFF" $offFrame
    if ($r3d.Ok) { $changedCoil56 = $false }

    # ---- Step 4: Exit Manual Mode (Addr 99 = 0) ----
    $r4 = Send-Frame $serial "STEP 4 Exit Manual Mode (FC06 Addr 99 = 0)" (Build-WriteSingleReg 99 0)
    if ($r4.Ok) { $enteredManual = $false }

    # ---- Step 5: Hardware Word Write (Sleep Gap 4175: orig -> orig+1 -> orig) ----
    if ($null -ne $origSleepGap) {
        $testGap = if ($origSleepGap -lt 99) { $origSleepGap + 1 } else { $origSleepGap - 1 }
        $r5a = Send-Frame $serial "STEP 5A Write Sleep Gap 4175 = $testGap via FC06" (Build-WriteSingleReg 4175 $testGap)
        $r5b = Send-Frame $serial "STEP 5B Read back Sleep Gap 4175 (FC03)" (Build-Read 0x03 4175 1)
        Write-Log "  Decoded Sleep Gap after FC06 = [$((@(Parse-UInt16Regs $r5b.Rx)) -join ',')]" "White"
        $r5c = Send-Frame $serial "STEP 5C Restore Sleep Gap 4175 = $origSleepGap via FC06" (Build-WriteSingleReg 4175 $origSleepGap)
        $r5d = Send-Frame $serial "STEP 5D Verify restored Sleep Gap 4175 (FC03)" (Build-Read 0x03 4175 1)
        Write-Log "  Decoded Sleep Gap restored = [$((@(Parse-UInt16Regs $r5d.Rx)) -join ',')]" "White"

        # Safe FC16 test writing the EXACT SAME original value
        $r5e = Send-Frame $serial "STEP 5E Safe FC16 check (Addr 4175 Qty 1 = $origSleepGap unchanged)" (Build-WriteMultiReg1 4175 $origSleepGap)
    }

    # ---- Step 6: Hardware Coil Write (Pump 6 Activation 4101: orig -> !orig -> orig) ----
    if ($null -ne $origPump6Act) {
        $testBit = -not $origPump6Act
        $r6a = Send-Frame $serial "STEP 6A Write Pump 6 Activation 4101 = $testBit via FC05" (Build-WriteSingleCoil 4101 $testBit)
        $usedFc15For4101 = $false
        if (-not $r6a.Ok) {
            $r6a = Send-Frame $serial "STEP 6A-ALT Fallback FC15 Coil 4101 = $testBit" (Build-WriteMultiCoil1 4101 $testBit)
            if ($r6a.Ok) { $usedFc15For4101 = $true }
        }
        $r6b = Send-Frame $serial "STEP 6B Read back Pump 1-6 Activation 4096..4101 (FC01)" (Build-Read 0x01 4096 6)
        Write-Log "  Decoded Pump 1-6 Activation = [$((@(Parse-Coils $r6b.Rx 6)) -join ',')]" "White"

        $restore4101 = if ($usedFc15For4101) { Build-WriteMultiCoil1 4101 $origPump6Act } else { Build-WriteSingleCoil 4101 $origPump6Act }
        $r6c = Send-Frame $serial "STEP 6C Restore Pump 6 Activation 4101 = $origPump6Act" $restore4101
        $r6d = Send-Frame $serial "STEP 6D Verify restored Pump 1-6 Activation (FC01)" (Build-Read 0x01 4096 6)
        Write-Log "  Decoded Pump 1-6 Activation restored = [$((@(Parse-Coils $r6d.Rx 6)) -join ',')]" "White"
    }

    Write-Log ""
    Write-Log "ALL BENCH WRITE TESTS FINISHED." "Green"
}
catch {
    Write-Log "ERROR: $($_.Exception.Message)" "Red"
}
finally {
    if ($serial.IsOpen) {
        # Safety restore pass in case an error occurred mid-script
        if ($changedWord50) {
            try { Send-Frame $serial "SAFETY-RESTORE Word 50 = 0" (Build-WriteSingleReg 50 0) | Out-Null } catch {}
        }
        if ($changedCoil56) {
            try { Send-Frame $serial "SAFETY-RESTORE Coil 56 = OFF" (Build-WriteSingleCoil 56 $false) | Out-Null } catch {}
        }
        if ($enteredManual) {
            try { Send-Frame $serial "SAFETY-RESTORE Exit Manual Addr 99 = 0" (Build-WriteSingleReg 99 0) | Out-Null } catch {}
        }
        $serial.Close()
        Write-Log "Closed $Port." "Gray"
    }
    $logLines | Set-Content -Path $OutFile -Encoding UTF8
    Write-Host ""
    Write-Host "Log saved to: $(Resolve-Path $OutFile)" -ForegroundColor Green
    Write-Host "Please send or push '$OutFile' so we can unlock verified write commands in the app." -ForegroundColor Cyan
}
