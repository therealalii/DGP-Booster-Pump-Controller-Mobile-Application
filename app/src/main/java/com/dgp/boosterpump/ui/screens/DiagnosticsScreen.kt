package com.dgp.boosterpump.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dgp.boosterpump.controller.RegisterReading
import com.dgp.boosterpump.controller.register.AccessPolicy
import com.dgp.boosterpump.controller.register.Bpc02RegisterMap
import com.dgp.boosterpump.controller.register.RegisterGroup
import com.dgp.boosterpump.controller.register.Verification
import com.dgp.boosterpump.modbus.ReadFunction
import com.dgp.boosterpump.modbus.TransactionRecord
import com.dgp.boosterpump.session.ConnectionState
import com.dgp.boosterpump.ui.PersianLabels
import com.dgp.boosterpump.ui.components.BadgeTone
import com.dgp.boosterpump.ui.components.DgpGoldButton
import com.dgp.boosterpump.ui.components.DgpPillChip
import com.dgp.boosterpump.ui.components.DgpSegmentedControl
import com.dgp.boosterpump.ui.components.LtrText
import com.dgp.boosterpump.ui.components.NoticeBox
import com.dgp.boosterpump.ui.components.ScadaPanel
import com.dgp.boosterpump.ui.components.StatusBadge
import com.dgp.boosterpump.ui.components.formatTime
import com.dgp.boosterpump.ui.theme.LocalScadaPalette
import com.dgp.boosterpump.ui.theme.MonoStyle
import com.dgp.boosterpump.viewmodel.MonitoringUiState

private enum class DiagTab(val label: String) {
    PROBE("تست خواندن"),
    LOG("لاگ TX/RX"),
    MAP("نقشه رجیستر"),
}

/**
 * Admin-only Engineering Console with segmented tabs for read-only Modbus probe,
 * live TX/RX frame log, and searchable BPC02 register directory.
 */
@Composable
fun DiagnosticsScreen(
    uiState: MonitoringUiState,
    canRunProbe: Boolean,
    onRunProbe: (ReadFunction, Int, Int) -> Unit,
    onClearLog: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
    val p = LocalScadaPalette.current
    var activeTab by remember { mutableStateOf(if (canRunProbe) DiagTab.PROBE else DiagTab.MAP) }

    Column(modifier = Modifier.fillMaxSize()) {
        if (onBack != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Surface(
                    onClick = onBack,
                    color = p.panel,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, p.panelBorder),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = "بازگشت",
                            tint = p.gold,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "بازگشت به اتصال",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = p.textPrimary,
                        )
                    }
                }
                StatusBadge("ابزار فنی مدیر", BadgeTone.GOLD)
            }
        }
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            DgpSegmentedControl(
                items = DiagTab.entries,
                selected = activeTab,
                label = { tab ->
                    when (tab) {
                        DiagTab.LOG -> "${tab.label} (${uiState.transactions.size})"
                        DiagTab.MAP -> "${tab.label} (${Bpc02RegisterMap.all.size})"
                        DiagTab.PROBE -> tab.label
                    }
                },
                onSelect = { activeTab = it },
                height = 42.dp,
            )
        }

        when (activeTab) {
            DiagTab.PROBE -> ProbeTabContent(uiState = uiState, canRunProbe = canRunProbe, onRunProbe = onRunProbe)
            DiagTab.LOG -> TransactionLogTabContent(uiState = uiState, onClearLog = onClearLog)
            DiagTab.MAP -> RegisterMapTabContent(uiState = uiState)
        }
    }
}

@Composable
private fun ProbeTabContent(
    uiState: MonitoringUiState,
    canRunProbe: Boolean,
    onRunProbe: (ReadFunction, Int, Int) -> Unit,
) {
    val p = LocalScadaPalette.current
    var selectedFn by remember { mutableStateOf(ReadFunction.READ_HOLDING_REGISTERS) }
    var addrText by remember { mutableStateOf("2") }
    var qtyText by remember { mutableStateOf("9") }
    var inputError by remember { mutableStateOf<String?>(null) }
    val isConnected = uiState.connectionState is ConnectionState.Connected

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            ScadaPanel(
                title = "تست خواندن مستقیم رجیستر (فقط خواندنی)",
                subtitle = "ارسال یک درخواست تکی روی ارتباط فعال برای بررسی پاسخ خام کنترلر",
                icon = Icons.Outlined.BugReport,
                accentColor = p.gold,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ReadFunction.entries.forEach { fn ->
                        DgpPillChip(
                            label = PersianLabels.readFunctionShort(fn),
                            selected = fn == selectedFn,
                            onClick = { selectedFn = fn },
                            accent = if (fn == ReadFunction.READ_DISCRETE_INPUTS || fn == ReadFunction.READ_INPUT_REGISTERS) p.warning else p.gold,
                        )
                    }
                }

                if (selectedFn == ReadFunction.READ_DISCRETE_INPUTS || selectedFn == ReadFunction.READ_INPUT_REGISTERS) {
                    NoticeBox(
                        text = "توجه: در تست سخت‌افزاری BPC02 مشخص شد که توابع FC02 و FC04 پشتیبانی نمی‌شوند (پاسخ ۴ بایتی غیراستاندارد). برای BPC02 از FC01 و FC03 استفاده کنید.",
                        tone = BadgeTone.WARNING,
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("پیش‌تنظیم‌های سریع BPC02", style = MaterialTheme.typography.labelMedium, color = p.textSecondary)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        DgpPillChip("فشار و ولتاژ (W2..9)", selected = false, accent = p.cyan, onClick = {
                            selectedFn = ReadFunction.READ_HOLDING_REGISTERS; addrText = "2"; qtyText = "8"
                        })
                        DgpPillChip("فرکانس درایو (W25..26)", selected = false, accent = p.cyan, onClick = {
                            selectedFn = ReadFunction.READ_HOLDING_REGISTERS; addrText = "25"; qtyText = "2"
                        })
                        DgpPillChip("فعال‌سازی پمپ (B4096..4102)", selected = false, accent = p.cyan, onClick = {
                            selectedFn = ReadFunction.READ_COILS; addrText = "4096"; qtyText = "7"
                        })
                        DgpPillChip("فشار هدف (W4096)", selected = false, accent = p.cyan, onClick = {
                            selectedFn = ReadFunction.READ_HOLDING_REGISTERS; addrText = "4096"; qtyText = "1"
                        })
                        DgpPillChip("خطاها (W80..81)", selected = false, accent = p.cyan, onClick = {
                            selectedFn = ReadFunction.READ_HOLDING_REGISTERS; addrText = "80"; qtyText = "2"
                        })
                        DgpPillChip("حالت کنترل (W4173)", selected = false, accent = p.cyan, onClick = {
                            selectedFn = ReadFunction.READ_HOLDING_REGISTERS; addrText = "4173"; qtyText = "1"
                        })
                        DgpPillChip("تایمر پمپ ۱–۴ (W34)", selected = false, accent = p.cyan, onClick = {
                            selectedFn = ReadFunction.READ_HOLDING_REGISTERS; addrText = "34"; qtyText = "12"
                        })
                        DgpPillChip("کل بیت‌ها (B0..33)", selected = false, accent = p.cyan, onClick = {
                            selectedFn = ReadFunction.READ_COILS; addrText = "0"; qtyText = "34"
                        })
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = addrText,
                        onValueChange = { addrText = it; inputError = null },
                        label = { Text("آدرس PDU (مبنای صفر)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { qtyText = it; inputError = null },
                        label = { Text("تعداد") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.62f),
                    )
                }

                if (inputError != null) {
                    Text(inputError!!, style = MaterialTheme.typography.bodySmall, color = p.fault)
                }

                if (!canRunProbe) {
                    NoticeBox(
                        text = "ارسال مستقیم درخواست تست خواندن (Probe) برای سطح دسترسی اپراتور محدود شده است. می‌توانید از تب‌های «لاگ TX/RX» و «نقشه رجیستر» استفاده کنید.",
                        tone = BadgeTone.WARNING,
                    )
                }

                DgpGoldButton(
                    text = when {
                        !canRunProbe -> "مخصوص ادمین، نصاب و سرویس‌کار"
                        isConnected -> "ارسال درخواست خواندن"
                        else -> "ابتدا به مبدل یا شبیه‌ساز متصل شوید"
                    },
                    icon = Icons.AutoMirrored.Outlined.Send,
                    enabled = isConnected && canRunProbe,
                    onClick = {
                        val addr = addrText.trim().toIntOrNull()
                        val qty = qtyText.trim().toIntOrNull()
                        when {
                            addr == null || addr !in 0..65535 -> inputError = "آدرس PDU باید بین ۰ تا ۶۵۵۳۵ باشد"
                            qty == null || qty !in 1..selectedFn.maxQuantity ->
                                inputError = "تعداد باید بین ۱ تا ${selectedFn.maxQuantity} باشد"
                            addr + qty > 65536 -> inputError = "مجموع آدرس و تعداد از ۶۵۵۳۵ بیشتر است"
                            else -> {
                                inputError = null
                                onRunProbe(selectedFn, addr, qty)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                uiState.probeResult?.let { res ->
                    HorizontalDivider(color = p.hairline)
                    if (res.error != null) {
                        NoticeBox(
                            text = "خطا در FC%02X آدرس %d (تعداد %d): %s".format(res.function.code, res.pduAddress, res.quantity, res.error),
                            tone = BadgeTone.FAULT,
                        )
                    } else if (res.values != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(p.terminalBg)
                                .border(0.9.dp, p.ok.copy(alpha = 0.42f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                "پاسخ دریافتی (${formatTime(res.timestampMillis)})",
                                style = MaterialTheme.typography.labelMedium,
                                color = p.ok,
                            )
                            val formatted = res.values.mapIndexed { idx, v ->
                                "[%d]=%d (0x%04X)".format(res.pduAddress + idx, v, v)
                            }.joinToString("  ")
                            LtrText(
                                text = "FC%02X @ %d → %s".format(res.function.code, res.pduAddress, formatted),
                                style = MonoStyle,
                                color = p.terminalText,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionLogTabContent(
    uiState: MonitoringUiState,
    onClearLog: () -> Unit,
) {
    val p = LocalScadaPalette.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        item {
            ScadaPanel(
                title = "ترمینال فریم‌های مدباس (${uiState.transactions.size})",
                subtitle = "مشاهده زنده فریم‌های خام TX و RX (جدیدترین در بالا)",
                icon = Icons.Outlined.Terminal,
                accentColor = p.cyan,
                trailing = {
                    TextButton(onClick = onClearLog, enabled = uiState.transactions.isNotEmpty()) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("پاک کردن")
                    }
                },
            ) {
                if (uiState.transactions.isEmpty()) {
                    Text(
                        "هنوز تراکنشی ثبت نشده است. با برقراری اتصال، تمام فریم‌های TX/RX در اینجا نمایش داده می‌شوند.",
                        style = MaterialTheme.typography.bodySmall,
                        color = p.textSecondary,
                    )
                }
            }
        }

        items(uiState.transactions, key = { "${it.timestampMillis}-${it.functionCode}-${it.pduAddress}-${it.durationMs}-${it.requestHex}" }) { tx ->
            TerminalTxCard(tx)
        }
    }
}

@Composable
private fun TerminalTxCard(tx: TransactionRecord) {
    val p = LocalScadaPalette.current
    val ok = tx.error == null
    val gutterColor = when {
        !ok -> p.fault
        tx.isWrite -> p.gold
        else -> p.cyan
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(p.terminalBg)
            .border(0.8.dp, if (ok) p.panelBorder else p.fault.copy(alpha = 0.5f), RoundedCornerShape(13.dp)),
    ) {
        // Color-coded protocol edge gutter (gold = write, cyan = read, crimson = error)
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(74.dp)
                .background(gutterColor),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LtrText(
                    text = if (tx.isWrite) {
                        "${formatTime(tx.timestampMillis)}  •  ID ${tx.unitId}  •  FC%02X @ %d".format(
                            tx.functionCode,
                            tx.pduAddress,
                        )
                    } else {
                        "${formatTime(tx.timestampMillis)}  •  ID ${tx.unitId}  •  FC%02X @ %d (×%d)".format(
                            tx.functionCode,
                            tx.pduAddress,
                            tx.quantity,
                        )
                    },
                    style = MonoStyle.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                    color = p.textPrimary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (tx.isWrite) {
                        StatusBadge("نوشتن", BadgeTone.GOLD)
                    }
                    StatusBadge(if (ok) "${tx.durationMs} ms" else "خطا", if (ok) BadgeTone.OK else BadgeTone.FAULT)
                }
            }
            LtrText("TX: ${tx.requestHex}", style = MonoStyle.copy(fontSize = 11.sp), color = if (tx.isWrite) p.gold else p.cyan)
            tx.responseHex?.let { rx ->
                LtrText("RX: $rx", style = MonoStyle.copy(fontSize = 11.sp), color = p.ok)
            }
            tx.error?.let { err ->
                Text(err, style = MaterialTheme.typography.bodySmall, color = p.fault)
            }
        }
    }
}

@Composable
private fun RegisterMapTabContent(uiState: MonitoringUiState) {
    val p = LocalScadaPalette.current
    var query by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf<RegisterGroup?>(null) }
    var activeHelp by remember { mutableStateOf<Pair<com.dgp.boosterpump.controller.Bpc02Catalog.ParameterHelp, String?>?>(null) }

    activeHelp?.let { (help, curVal) ->
        com.dgp.boosterpump.ui.components.ParameterHelpBottomSheet(
            help = help,
            currentValue = curVal,
            onDismiss = { activeHelp = null },
        )
    }

    val liveMap: Map<String, Int?> = remember(uiState.snapshot, uiState.configSnapshot) {
        buildMap {
            uiState.snapshot?.readings?.forEach { put(it.definition.key, it.raw) }
            uiState.configSnapshot.readings.forEach { put(it.definition.key, it.raw) }
        }
    }

    val filtered = remember(query, selectedGroup) {
        val q = query.trim().lowercase()
        Bpc02RegisterMap.all.filter { def ->
            val groupOk = selectedGroup == null || def.group == selectedGroup
            val searchOk = q.isEmpty() ||
                com.dgp.boosterpump.controller.Bpc02Catalog.fullParameterName(def).lowercase().contains(q) ||
                def.label.lowercase().contains(q) ||
                def.documentedAddress.toString().contains(q)
            groupOk && searchOk
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            ScadaPanel(
                title = "BPC-02 Register Map",
                subtitle = "${Bpc02RegisterMap.all.size} Registers (Coil FC01/FC05 & Holding FC03/FC06, Offset 0)",
                icon = Icons.AutoMirrored.Outlined.ListAlt,
                accentColor = p.gold,
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search by English name or address (e.g. 2, 80, Sleep, Pump)…") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(13.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    DgpPillChip(
                        label = "All",
                        selected = selectedGroup == null,
                        onClick = { selectedGroup = null },
                    )
                    val groups = listOf(
                        RegisterGroup.MEASUREMENT to "Measurements",
                        RegisterGroup.PUMP_STATUS to "Pumps",
                        RegisterGroup.RUN_TIME to "Work Time",
                        RegisterGroup.ALARM to "Errors",
                        RegisterGroup.DIGITAL_IO to "Digital I/O",
                        RegisterGroup.CONFIGURATION to "Hardware",
                        RegisterGroup.COMMAND to "Command (RW)",
                        RegisterGroup.MANUAL_CONTROL to "Manual (R)",
                    )
                    groups.forEach { (g, title) ->
                        DgpPillChip(
                            label = title,
                            selected = selectedGroup == g,
                            onClick = { selectedGroup = if (selectedGroup == g) null else g },
                        )
                    }
                }
            }
        }

        items(filtered, key = { it.key }) { def ->
            val raw = liveMap[def.key]
            RegisterDirectoryCard(
                reading = RegisterReading(def, raw),
                onHelpClick = {
                    val cur = raw?.let { com.dgp.boosterpump.controller.Bpc02Catalog.formatValue(def, it) }
                    activeHelp = com.dgp.boosterpump.controller.Bpc02Catalog.helpFor(def) to cur
                },
            )
        }
    }
}

@Composable
private fun RegisterDirectoryCard(
    reading: RegisterReading,
    onHelpClick: () -> Unit,
) {
    val p = LocalScadaPalette.current
    val def = reading.definition
    val locked = def.access == AccessPolicy.CONTROL_LOCKED

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(p.panel)
            .border(0.8.dp, if (locked) p.warning.copy(alpha = 0.35f) else p.panelBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                LtrText(
                    text = com.dgp.boosterpump.controller.Bpc02Catalog.fullParameterName(def),
                    style = MaterialTheme.typography.titleSmall,
                    color = p.textPrimary,
                )
                com.dgp.boosterpump.ui.components.ParameterHelpIcon(onClick = onHelpClick)
            }
            Spacer(Modifier.width(8.dp))
            LtrText(
                text = reading.raw?.let { com.dgp.boosterpump.controller.Bpc02Catalog.formatValue(def, it) } ?: "—",
                style = MaterialTheme.typography.titleSmall,
                color = if (reading.raw != null) p.cyan else p.textSecondary,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LtrText(
                text = "${def.table.name}  •  Dec ${def.documentedAddress} (0x%04X)".format(def.documentedAddress) +
                    (reading.raw?.let { "  •  raw=$it" } ?: ""),
                style = MonoStyle.copy(fontSize = 10.sp),
                color = p.textMuted,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                if (def.isWritable) {
                    StatusBadge("RW", BadgeTone.GOLD)
                }
                if (locked) {
                    StatusBadge("Locked", BadgeTone.WARNING)
                } else {
                    val (vLabel, tone) = when (def.verification) {
                        Verification.HARDWARE_VERIFIED -> "Verified" to BadgeTone.OK
                        Verification.DOCUMENTED -> "Catalog" to BadgeTone.NEUTRAL
                        Verification.AMBIGUOUS -> "Check" to BadgeTone.WARNING
                    }
                    StatusBadge(vLabel, tone)
                }
            }
        }
    }
}
