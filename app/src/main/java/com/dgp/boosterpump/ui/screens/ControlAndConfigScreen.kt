package com.dgp.boosterpump.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dgp.boosterpump.controller.RegisterReading
import com.dgp.boosterpump.controller.register.AccessPolicy
import com.dgp.boosterpump.controller.register.Bpc02RegisterMap
import com.dgp.boosterpump.controller.register.RegisterDefinition
import com.dgp.boosterpump.controller.register.VendorTable
import com.dgp.boosterpump.session.ConnectionState
import com.dgp.boosterpump.session.WriteResult
import com.dgp.boosterpump.ui.PersianLabels
import com.dgp.boosterpump.ui.components.BadgeTone
import com.dgp.boosterpump.ui.components.DgpGoldButton
import com.dgp.boosterpump.ui.components.DgpSegmentedControl
import com.dgp.boosterpump.ui.components.LtrText
import com.dgp.boosterpump.ui.components.NoticeBox
import com.dgp.boosterpump.ui.components.ScadaPanel
import com.dgp.boosterpump.ui.components.StatusBadge
import com.dgp.boosterpump.ui.components.formatTime
import com.dgp.boosterpump.ui.theme.LocalScadaPalette
import com.dgp.boosterpump.ui.theme.MonoStyle
import com.dgp.boosterpump.viewmodel.MonitoringUiState
import kotlin.math.roundToInt

/** Pending edit state driving one of the Persian confirmation dialogs. */
private sealed interface PendingEdit {
    data class CoilConfirm(val def: RegisterDefinition, val currentValue: Int?, val targetValue: Int) : PendingEdit
    data class WordNumberEdit(val def: RegisterDefinition, val currentValue: Int?) : PendingEdit
    data class WordEnumEdit(val def: RegisterDefinition, val currentValue: Int?) : PendingEdit
    data class ControlModeConfirm(val currentValue: Int?, val targetValue: Int) : PendingEdit
    data object ResetErrorsConfirm : PendingEdit
}

/**
 * Flagship Persian Operational Control & Hardware Parameters screen, strictly governed by
 * DGP's official Booster Pump Memory Map.xlsx (R vs RW permissions, FC05/FC06 single writes,
 * 2-step confirmation, RS485/EEPROM settle timing, and read-back verification).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ControlAndConfigScreen(
    uiState: MonitoringUiState,
    canWriteConfig: Boolean,
    onReadHardwareConfig: () -> Unit,
    onWriteParameter: (RegisterDefinition, Int) -> Unit,
    onResetPumpErrors: () -> Unit,
    onDismissWriteNotice: () -> Unit,
) {
    val p = LocalScadaPalette.current
    val cfgSnap = uiState.configSnapshot
    val liveSnap = uiState.snapshot
    val isConnected = uiState.connectionState is ConnectionState.Connected
    val isWriting = cfgSnap.writingKey != null
    val canEditNow = canWriteConfig && isConnected && !isWriting

    var pendingEdit by remember { mutableStateOf<PendingEdit?>(null) }
    var showReadOnlyParams by remember { mutableStateOf(false) }

    when (val edit = pendingEdit) {
        is PendingEdit.CoilConfirm -> CoilWriteConfirmDialog(
            def = edit.def,
            currentValue = edit.currentValue,
            targetValue = edit.targetValue,
            onDismiss = { pendingEdit = null },
            onConfirm = {
                pendingEdit = null
                onWriteParameter(edit.def, edit.targetValue)
            },
        )
        is PendingEdit.WordNumberEdit -> WordNumberEditDialog(
            def = edit.def,
            currentValue = edit.currentValue,
            onDismiss = { pendingEdit = null },
            onConfirm = { newVal ->
                pendingEdit = null
                onWriteParameter(edit.def, newVal)
            },
        )
        is PendingEdit.WordEnumEdit -> WordEnumEditDialog(
            def = edit.def,
            currentValue = edit.currentValue,
            onDismiss = { pendingEdit = null },
            onConfirm = { newVal ->
                pendingEdit = null
                onWriteParameter(edit.def, newVal)
            },
        )
        is PendingEdit.ControlModeConfirm -> ControlModeConfirmDialog(
            currentValue = edit.currentValue,
            targetValue = edit.targetValue,
            onDismiss = { pendingEdit = null },
            onConfirm = {
                pendingEdit = null
                onWriteParameter(Bpc02RegisterMap.controlStatusDef, edit.targetValue)
            },
        )
        PendingEdit.ResetErrorsConfirm -> ResetErrorsConfirmDialog(
            onDismiss = { pendingEdit = null },
            onConfirm = {
                pendingEdit = null
                onResetPumpErrors()
            },
        )
        null -> Unit
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 0. Write Result Notification Banner (visible immediately after any write)
        if (cfgSnap.lastWrite != null) {
            item {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically { -it / 3 },
                    exit = fadeOut(),
                ) {
                    WriteResultBanner(result = cfgSnap.lastWrite, onDismiss = onDismissWriteNotice)
                }
            }
        }

        // 1. Live Operational Controls (Control Status 4173, Setpoint 4096, Reset Error 99=25, Drive/Pump 1..6 4096..4102)
        item {
            val liveSetpointRaw = liveSnap?.readings?.firstOrNull { it.definition.key == Bpc02RegisterMap.SYSTEM_PRESSURE_SETPOINT }?.raw
                ?: cfgSnap.readings.firstOrNull { it.definition.key == Bpc02RegisterMap.SYSTEM_PRESSURE_SETPOINT }?.raw
            val liveControlStatus = liveSnap?.controlStatus
                ?: cfgSnap.readings.firstOrNull { it.definition.key == Bpc02RegisterMap.CONTROL_STATUS }?.raw
            val liveDriveEnabled = liveSnap?.driveEnabled?.let { if (it) 1 else 0 }
                ?: cfgSnap.readings.firstOrNull { it.definition.key == Bpc02RegisterMap.DRIVE_ACTIVATION }?.raw

            ScadaPanel(
                title = "کنترل عملیاتی زنده ایستگاه",
                subtitle = "حالت کنترل (۴۱۷۳) • فشار هدف (۴۰۹۶) • ریست خطا (۹۹) • فعال‌سازی پمپ‌ها (۴۰۹۶–۴۱۰۲)",
                icon = Icons.Outlined.PowerSettingsNew,
                accentColor = p.gold,
                trailing = {
                    if (isWriting) {
                        StatusBadge("در حال ارسال…", BadgeTone.GOLD, pulsing = true)
                    } else if (!canWriteConfig) {
                        StatusBadge("فقط خواندنی", BadgeTone.WARNING)
                    } else {
                        StatusBadge("FC05 / FC06", BadgeTone.OK)
                    }
                },
            ) {
                if (!canWriteConfig) {
                    NoticeBox(
                        text = "حساب کاربری فعلی فقط‌خواندنی است. برای اعمال فرمان یا تغییر پارامترهای RW با حساب «مدیر سیستم» یا «نصاب» وارد شوید.",
                        tone = BadgeTone.WARNING,
                    )
                }

                // Control Status (Word 4173: 0=Disable, 1=PID, 2=ON/OFF)
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("حالت عملکرد کنترلر", style = MaterialTheme.typography.titleSmall, color = p.textPrimary)
                            LtrText("FC06 • Word 4173 (0x104D)", style = MonoStyle.copy(fontSize = 10.sp), color = p.textMuted)
                        }
                        if (cfgSnap.writingKey == Bpc02RegisterMap.CONTROL_STATUS) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = p.gold)
                        } else {
                            StatusBadge(
                                PersianLabels.controlStatusShort(liveControlStatus),
                                when (liveControlStatus) {
                                    1, 2 -> BadgeTone.OK
                                    0 -> BadgeTone.WARNING
                                    else -> BadgeTone.NEUTRAL
                                },
                            )
                        }
                    }
                    DgpSegmentedControl(
                        items = listOf(0, 1, 2),
                        selected = liveControlStatus ?: -1,
                        label = { mode ->
                            when (mode) {
                                0 -> "توقف (Disable)"
                                1 -> "PID"
                                else -> "ON / OFF"
                            }
                        },
                        onSelect = { target ->
                            if (liveControlStatus != target) {
                                pendingEdit = PendingEdit.ControlModeConfirm(liveControlStatus, target)
                            }
                        },
                        enabled = canEditNow && liveControlStatus != null,
                        height = 40.dp,
                    )
                }

                HorizontalDivider(color = p.hairline)

                // System Pressure Setpoint (Word 4096: 0~6000 = 0.00-60.00 bar)
                WordActionRow(
                    reading = RegisterReading(Bpc02RegisterMap.pressureSetpointDef, liveSetpointRaw),
                    actionLabel = "تنظیم فشار",
                    enabled = canEditNow,
                    isWritingThis = cfgSnap.writingKey == Bpc02RegisterMap.SYSTEM_PRESSURE_SETPOINT,
                    onClickEdit = {
                        pendingEdit = PendingEdit.WordNumberEdit(Bpc02RegisterMap.pressureSetpointDef, liveSetpointRaw)
                    },
                )

                HorizontalDivider(color = p.hairline)

                // Reset Pump Error (Word 99 = 25)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("ریست خطای پمپ‌ها", style = MaterialTheme.typography.titleSmall, color = p.textPrimary)
                        LtrText("FC06 • Word 99 = 25 (Reset Pump Error)", style = MonoStyle.copy(fontSize = 10.sp), color = p.textMuted)
                    }
                    Spacer(Modifier.width(8.dp))
                    if (cfgSnap.writingKey == Bpc02RegisterMap.MULTI_FUNCTION_REGISTER) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = p.gold)
                    } else {
                        DgpGoldButton(
                            text = "ریست خطا",
                            icon = Icons.Outlined.RestartAlt,
                            enabled = canEditNow,
                            onClick = { pendingEdit = PendingEdit.ResetErrorsConfirm },
                            height = 38.dp,
                        )
                    }
                }

                HorizontalDivider(color = p.hairline)

                // Live Drive & Pump 1..6 Activation Toggles (Coils 4096..4102)
                ConfigSectionHeader("فعال / غیرفعال کردن درایو و پمپ‌ها (S)", "کویل ۴۰۹۶–۴۱۰۲ • FC05")
                ConfigGroupBox {
                    CoilSwitchRow(
                        reading = RegisterReading(Bpc02RegisterMap.driveActivationDef, liveDriveEnabled),
                        enabled = canEditNow,
                        isWritingThis = cfgSnap.writingKey == Bpc02RegisterMap.DRIVE_ACTIVATION,
                        disabledBadgeText = "غیرفعال (S)",
                        onRequestToggle = { target ->
                            pendingEdit = PendingEdit.CoilConfirm(Bpc02RegisterMap.driveActivationDef, liveDriveEnabled, target)
                        },
                    )
                    Bpc02RegisterMap.pumpActivationDefs.forEachIndexed { idx, def ->
                        HorizontalDivider(color = p.hairline)
                        val rawVal = liveSnap?.pumps?.getOrNull(idx)?.enabled?.let { if (it) 1 else 0 }
                            ?: cfgSnap.readings.firstOrNull { it.definition.key == def.key }?.raw
                        CoilSwitchRow(
                            reading = RegisterReading(def, rawVal),
                            enabled = canEditNow,
                            isWritingThis = cfgSnap.writingKey == def.key,
                            disabledBadgeText = "غیرفعال (S)",
                            onRequestToggle = { target ->
                                pendingEdit = PendingEdit.CoilConfirm(def, rawVal, target)
                            },
                        )
                    }
                }
            }
        }

        // 2. Full Hardware Parameters Panel (On-demand read of all 4096..4210 official parameters)
        item {
            ScadaPanel(
                title = "پارامترهای کامل کنترلر (۴۰۹۶ تا ۴۲۱۰)",
                subtitle = "ویرایش امن ۲۳ پارامتر RW تاییدشده و مشاهده تمام پارامترهای خواندنی (R)",
                icon = Icons.Outlined.Tune,
                accentColor = p.cyan,
                trailing = {
                    val canRefresh = isConnected && !cfgSnap.loading && !isWriting
                    Surface(
                        onClick = onReadHardwareConfig,
                        enabled = canRefresh,
                        color = if (canRefresh) p.goldSoft else p.panelElevated,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.9.dp, if (canRefresh) p.gold.copy(alpha = 0.5f) else p.panelBorder),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            if (cfgSnap.loading) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = p.gold)
                            } else {
                                Icon(
                                    Icons.Outlined.Refresh,
                                    contentDescription = null,
                                    tint = if (canRefresh) p.gold else p.textMuted,
                                    modifier = Modifier.size(15.dp),
                                )
                            }
                            Text(
                                text = if (cfgSnap.readings.isEmpty()) "خواندن همه" else "بروزرسانی",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (canRefresh) p.gold else p.textMuted,
                            )
                        }
                    }
                },
            ) {
                if (cfgSnap.timestampMillis != null) {
                    Text(
                        "آخرین همگام‌سازی پارامترها: ساعت ${formatTime(cfgSnap.timestampMillis)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = p.cyan,
                    )
                }
                if (cfgSnap.errors.isNotEmpty()) {
                    NoticeBox(
                        text = "برخی بلوک‌های پارامتر پاسخ ندادند: ${cfgSnap.errors.take(3).joinToString(" | ")}",
                        tone = BadgeTone.WARNING,
                    )
                }

                if (cfgSnap.readings.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(p.panelElevated)
                            .border(0.8.dp, p.panelBorder, RoundedCornerShape(14.dp))
                            .padding(14.dp),
                    ) {
                        Text(
                            if (isConnected) {
                                "برای خواندن و ویرایش پارامترهای PID، خواب و بیداری، کنترل فاز، فلوتر، جاکی، اتو سرویس و تنظیمات فقط‌خواندنی کنترلر دکمه «خواندن همه» را بزنید."
                            } else {
                                "ابتدا به کنترلر (مبدل سمین ES 487) یا حالت شبیه‌سازی متصل شوید تا امکان خواندن پارامترها فعال شود."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = p.textSecondary,
                        )
                    }
                } else {
                    val byAddrWord = cfgSnap.readings.filter { it.definition.table == VendorTable.WORD }
                    val pidAndCalib = byAddrWord.filter {
                        it.definition.documentedAddress in setOf(4136, 4137, 4138, 4143, 4166)
                    }
                    val sleepAndTiming = byAddrWord.filter {
                        it.definition.documentedAddress in setOf(4175, 4150, 4151, 4149, 4164, 4162, 4163, 4161)
                    }
                    val phaseFloaterJockey = byAddrWord.filter {
                        it.definition.documentedAddress in setOf(4123, 4124, 4125, 4127, 4128, 4129, 4135)
                    }
                    val autoService = byAddrWord.filter {
                        it.definition.documentedAddress in 4177..4179
                    }
                    val readOnlyAll = cfgSnap.readings.filter { it.definition.access == AccessPolicy.READ }

                    if (pidAndCalib.isNotEmpty()) {
                        ConfigSectionHeader("ضرایب PID و کالیبراسیون", "ورد ۴۱۳۶–۴۱۳۸، ۴۱۴۳، ۴۱۶۶ • FC06")
                        ConfigGroupBox {
                            pidAndCalib.forEachIndexed { idx, reading ->
                                if (idx > 0) HorizontalDivider(color = p.hairline)
                                WordActionRow(
                                    reading = reading,
                                    actionLabel = "ویرایش",
                                    enabled = canEditNow,
                                    isWritingThis = cfgSnap.writingKey == reading.definition.key,
                                    onClickEdit = {
                                        pendingEdit = PendingEdit.WordNumberEdit(reading.definition, reading.raw)
                                    },
                                )
                            }
                        }
                    }

                    if (sleepAndTiming.isNotEmpty()) {
                        ConfigSectionHeader("خواب، بیداری و تاخیر پمپ‌ها", "ورد ۴۱۴۹–۴۱۵۱، ۴۱۶۱–۴۱۶۴، ۴۱۷۵ • FC06")
                        ConfigGroupBox {
                            sleepAndTiming.forEachIndexed { idx, reading ->
                                if (idx > 0) HorizontalDivider(color = p.hairline)
                                WordActionRow(
                                    reading = reading,
                                    actionLabel = "ویرایش",
                                    enabled = canEditNow,
                                    isWritingThis = cfgSnap.writingKey == reading.definition.key,
                                    onClickEdit = {
                                        pendingEdit = PendingEdit.WordNumberEdit(reading.definition, reading.raw)
                                    },
                                )
                            }
                        }
                    }

                    if (phaseFloaterJockey.isNotEmpty()) {
                        ConfigSectionHeader("کنترل فاز، فلوتر و جاکی", "ورد ۴۱۲۳–۴۱۲۹، ۴۱۳۵ • FC06")
                        ConfigGroupBox {
                            phaseFloaterJockey.forEachIndexed { idx, reading ->
                                if (idx > 0) HorizontalDivider(color = p.hairline)
                                val isEnum = reading.definition.enumLabels.isNotEmpty()
                                WordActionRow(
                                    reading = reading,
                                    actionLabel = if (isEnum) "انتخاب" else "ویرایش",
                                    enabled = canEditNow,
                                    isWritingThis = cfgSnap.writingKey == reading.definition.key,
                                    onClickEdit = {
                                        pendingEdit = if (isEnum) {
                                            PendingEdit.WordEnumEdit(reading.definition, reading.raw)
                                        } else {
                                            PendingEdit.WordNumberEdit(reading.definition, reading.raw)
                                        }
                                    },
                                )
                            }
                        }
                    }

                    if (autoService.isNotEmpty()) {
                        ConfigSectionHeader("تنظیمات اتو سرویس", "ورد ۴۱۷۷–۴۱۷۹ • FC06")
                        ConfigGroupBox {
                            autoService.forEachIndexed { idx, reading ->
                                if (idx > 0) HorizontalDivider(color = p.hairline)
                                WordActionRow(
                                    reading = reading,
                                    actionLabel = "ویرایش",
                                    enabled = canEditNow,
                                    isWritingThis = cfgSnap.writingKey == reading.definition.key,
                                    onClickEdit = {
                                        pendingEdit = PendingEdit.WordNumberEdit(reading.definition, reading.raw)
                                    },
                                )
                            }
                        }
                    }

                    if (readOnlyAll.isNotEmpty()) {
                        Surface(
                            onClick = { showReadOnlyParams = !showReadOnlyParams },
                            color = p.panelElevated,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(0.8.dp, p.warning.copy(alpha = 0.30f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(Icons.Outlined.Shield, contentDescription = null, tint = p.warning, modifier = Modifier.size(16.dp))
                                    Column {
                                        Text(
                                            "پارامترهای فقط خواندنی کنترلر (${readOnlyAll.size} مورد)",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = p.textPrimary,
                                        )
                                        Text(
                                            "کویل ۴۱۰۶–۴۱۱۰ • فشارهای پمپ ۱–۴ • سنسور • MFI/MFO • سریال و آدرس مدباس",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = p.textSecondary,
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = if (showReadOnlyParams) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                    contentDescription = null,
                                    tint = p.gold,
                                )
                            }
                        }

                        AnimatedVisibility(visible = showReadOnlyParams) {
                            ConfigGroupBox(borderColor = p.warning.copy(alpha = 0.28f)) {
                                readOnlyAll.forEachIndexed { idx, reading ->
                                    if (idx > 0) HorizontalDivider(color = p.hairline)
                                    ReadOnlyRegisterRow(reading)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Manual Outputs Information Panel (R-only in official DGP firmware)
        item {
            ScadaPanel(
                title = "خروجی‌های دستی (فقط خواندنی در فریمور)",
                subtitle = "کویل ۵۰–۵۹ (رله‌های دستی) • ورد ۵۰–۵۱ (آنالوگ دستی)",
                icon = Icons.Outlined.AdminPanelSettings,
                accentColor = p.warning,
                borderColor = p.warning.copy(alpha = 0.36f),
                trailing = { StatusBadge("فقط خواندنی (R)", BadgeTone.WARNING) },
            ) {
                NoticeBox(
                    text = "مطابق فایل رسمی Booster Pump Memory Map.xlsx شرکت DGP، رجیسترهای خروجی دستی (کویل ۵۰ تا ۵۹ و ورد ۵۰–۵۱) در فریمور کنترلر فقط خواندنی (R) تعریف شده‌اند (و در تست واقعی نیز خطای Modbus 0x02 برگرداندند). همچنین رجیستر ۹۹ فقط فرمان ۲۵ = ریست خطای پمپ دارد که در بخش کنترل زنده بالا فعال شده است.",
                    tone = BadgeTone.WARNING,
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(0.55f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(p.panelElevated)
                        .border(0.8.dp, p.panelBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("خروجی‌های دستی رله — فقط خواندنی در فریمور (کویل ۵۰ تا ۵۹)", style = MaterialTheme.typography.labelMedium, color = p.textSecondary)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        for (i in 0..9) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(p.panel)
                                    .border(0.8.dp, p.panelBorder, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(Icons.Outlined.Lock, contentDescription = null, tint = p.standby, modifier = Modifier.size(11.dp))
                                LtrText("OUT $i", style = MonoStyle.copy(fontSize = 10.5.sp), color = p.textSecondary)
                            }
                        }
                    }

                    HorizontalDivider(color = p.hairline)

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("خروجی آنالوگ دستی ۰ (ورد ۵۰ — R)", style = MaterialTheme.typography.labelMedium, color = p.textSecondary)
                            LtrText("0.0 Hz", style = MonoStyle, color = p.textSecondary)
                        }
                        Slider(value = 0f, onValueChange = {}, enabled = false)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("خروجی آنالوگ دستی ۱ (ورد ۵۱ — R)", style = MaterialTheme.typography.labelMedium, color = p.textSecondary)
                            LtrText("0.0 Hz", style = MonoStyle, color = p.textSecondary)
                        }
                        Slider(value = 0f, onValueChange = {}, enabled = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfigGroupBox(
    borderColor: Color? = null,
    content: @Composable () -> Unit,
) {
    val p = LocalScadaPalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(p.panelElevated)
            .border(0.8.dp, borderColor ?: p.panelBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 4.dp),
    ) {
        content()
    }
}

@Composable
private fun ConfigSectionHeader(title: String, tag: String) {
    val p = LocalScadaPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = p.textPrimary,
        )
        LtrText(
            text = tag,
            style = MonoStyle.copy(fontSize = 10.sp),
            color = p.gold,
        )
    }
}

@Composable
private fun WriteResultBanner(result: WriteResult, onDismiss: () -> Unit) {
    val p = LocalScadaPalette.current
    val fg = if (result.success) p.ok else p.fault
    val bg = if (result.success) p.okBg else p.faultBg
    val def = result.definition
    val label = PersianLabels.registerLabel(def)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.9.dp, fg.copy(alpha = 0.42f), RoundedCornerShape(12.dp)),
        color = bg,
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Icon(
                imageVector = if (result.success) Icons.Outlined.CheckCircleOutline else Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(19.dp),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (result.success) {
                        "تغییر «$label» اعمال و تایید شد (${formatTime(result.timestampMillis)})"
                    } else {
                        "خطا در ثبت «$label» (آدرس ${def.documentedAddress})"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = fg,
                )
                Text(
                    text = if (result.success) {
                        val confirmed = result.readBackValue?.let { PersianLabels.formatRegisterValue(def, it) } ?: "—"
                        "آدرس ${def.documentedAddress}  •  مقدار تاییدشده: $confirmed"
                    } else {
                        result.error ?: "خطای نامشخص در ارتباط مدباس"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = fg,
                )
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Outlined.Close, contentDescription = "بستن", tint = fg, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun CoilSwitchRow(
    reading: RegisterReading,
    enabled: Boolean,
    isWritingThis: Boolean,
    disabledBadgeText: String = "غیرفعال",
    onRequestToggle: (Int) -> Unit,
) {
    val p = LocalScadaPalette.current
    val def = reading.definition
    val isOn = reading.raw == 1

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = PersianLabels.registerLabel(def),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = p.textPrimary,
            )
            LtrText(
                text = "FC05 • Coil ${def.documentedAddress} (0x%04X)".format(def.documentedAddress),
                style = MonoStyle.copy(fontSize = 10.sp),
                color = p.textMuted,
            )
        }
        Spacer(Modifier.width(10.dp))
        if (isWritingThis) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = p.gold)
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when (reading.raw) {
                    1 -> StatusBadge("فعال", BadgeTone.OK)
                    0 -> StatusBadge(disabledBadgeText, BadgeTone.WARNING)
                    else -> Text("—", style = MaterialTheme.typography.labelMedium, color = p.textMuted)
                }
                Switch(
                    checked = isOn,
                    onCheckedChange = { checked -> onRequestToggle(if (checked) 1 else 0) },
                    enabled = enabled && reading.raw != null,
                )
            }
        }
    }
}

@Composable
private fun WordActionRow(
    reading: RegisterReading,
    actionLabel: String,
    enabled: Boolean,
    isWritingThis: Boolean,
    onClickEdit: () -> Unit,
) {
    val p = LocalScadaPalette.current
    val def = reading.definition

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = PersianLabels.registerLabel(def),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = p.textPrimary,
            )
            LtrText(
                text = "FC06 • Word ${def.documentedAddress} (0x%04X)".format(def.documentedAddress),
                style = MonoStyle.copy(fontSize = 10.sp),
                color = p.textMuted,
            )
            Text(
                text = reading.raw?.let { PersianLabels.formatRegisterValue(def, it) } ?: "—",
                style = MaterialTheme.typography.titleSmall,
                color = if (reading.raw != null) p.cyan else p.textSecondary,
            )
        }
        Spacer(Modifier.width(8.dp))
        if (isWritingThis) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = p.gold)
        } else {
            val active = enabled && reading.raw != null
            Surface(
                onClick = onClickEdit,
                enabled = active,
                color = if (active) p.goldSoft else p.panel,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(0.8.dp, if (active) p.gold.copy(alpha = 0.45f) else p.panelBorder),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = null,
                        tint = if (active) p.gold else p.textMuted,
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        actionLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (active) p.gold else p.textMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadOnlyRegisterRow(reading: RegisterReading) {
    val p = LocalScadaPalette.current
    val def = reading.definition
    val addrKind = if (def.table == VendorTable.BIT) "Coil" else "Word"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = PersianLabels.registerLabel(def),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = p.textPrimary,
            )
            LtrText(
                text = "Read-Only (R) • $addrKind ${def.documentedAddress} (0x%04X)".format(def.documentedAddress),
                style = MonoStyle.copy(fontSize = 10.sp),
                color = p.textMuted,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = reading.raw?.let { PersianLabels.formatRegisterValue(def, it) } ?: "—",
            style = MaterialTheme.typography.titleSmall,
            color = if (reading.raw != null) p.textPrimary else p.textMuted,
        )
    }
}

@Composable
private fun ControlModeConfirmDialog(
    currentValue: Int?,
    targetValue: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val p = LocalScadaPalette.current
    val def = Bpc02RegisterMap.controlStatusDef
    val currentText = currentValue?.let { PersianLabels.enumOptionLabel(def, it) } ?: "—"
    val targetText = PersianLabels.enumOptionLabel(def, targetValue)
    val isDisabling = targetValue == 0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = p.panel,
        shape = RoundedCornerShape(22.dp),
        icon = {
            Icon(
                Icons.Outlined.PowerSettingsNew,
                contentDescription = null,
                tint = if (isDisabling) p.fault else p.gold,
            )
        },
        title = { Text("تایید تغییر حالت عملکرد کنترلر", style = MaterialTheme.typography.titleMedium, color = p.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                LtrText("FC06 • Word 4173 (0x104D) = $targetValue", style = MonoStyle.copy(fontSize = 11.sp), color = p.cyan)
                Surface(
                    color = p.panelElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.8.dp, p.panelBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("حالت فعلی: $currentText", style = MaterialTheme.typography.bodySmall, color = p.textSecondary)
                        Text(
                            "حالت جدید: $targetText",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isDisabling) p.fault else p.gold,
                        )
                    }
                }
                if (isDisabling) {
                    NoticeBox(
                        text = "هشدار ایمنی: انتخاب «توقف (Disable)» باعث خاموش شدن کل ایستگاه بوستر پمپ و قطع تامین فشار می‌شود.",
                        tone = BadgeTone.FAULT,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDisabling) p.fault else p.gold,
                    contentColor = if (isDisabling) Color.White else Color(0xFF0E0A03),
                ),
            ) {
                Text("تایید و ارسال (FC06)", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف", color = p.textSecondary) }
        },
    )
}

@Composable
private fun ResetErrorsConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val p = LocalScadaPalette.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = p.panel,
        shape = RoundedCornerShape(22.dp),
        icon = { Icon(Icons.Outlined.RestartAlt, contentDescription = null, tint = p.gold) },
        title = { Text("تایید ریست خطای پمپ‌ها", style = MaterialTheme.typography.titleMedium, color = p.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LtrText("FC06 • Word 99 (0x0063) = 25", style = MonoStyle.copy(fontSize = 11.sp), color = p.cyan)
                Text(
                    "فرمان رسمی ریست خطای پمپ‌ها (Reset Pump Error) به کنترلر BPC02 ارسال می‌شود. پیش از اجرا از رفع عامل خطا اطمینان حاصل کنید.",
                    style = MaterialTheme.typography.bodySmall,
                    color = p.textSecondary,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = p.gold, contentColor = Color(0xFF0E0A03)),
            ) {
                Text("ارسال ریست (کد ۲۵)", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف", color = p.textSecondary) }
        },
    )
}

@Composable
private fun CoilWriteConfirmDialog(
    def: RegisterDefinition,
    currentValue: Int?,
    targetValue: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val p = LocalScadaPalette.current
    val currentText = currentValue?.let { if (it == 1) "فعال" else "غیرفعال (S)" } ?: "—"
    val targetText = if (targetValue == 1) "فعال" else "غیرفعال (S)"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = p.panel,
        shape = RoundedCornerShape(22.dp),
        icon = { Icon(Icons.Outlined.Tune, contentDescription = null, tint = p.gold) },
        title = { Text("تایید تغییر تنظیم سخت‌افزاری", style = MaterialTheme.typography.titleMedium, color = p.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text(
                    PersianLabels.registerLabel(def),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = p.textPrimary,
                )
                LtrText(
                    text = "FC05 (Write Single Coil)  •  Addr ${def.documentedAddress} (0x%04X)".format(def.documentedAddress),
                    style = MonoStyle.copy(fontSize = 11.sp),
                    color = p.cyan,
                )
                Surface(
                    color = p.panelElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.8.dp, p.panelBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text("مقدار فعلی: $currentText", style = MaterialTheme.typography.bodySmall, color = p.textSecondary)
                        Text("مقدار جدید: $targetText", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = p.gold)
                    }
                }
                Text(
                    "پس از ارسال فرمان FC05 و ذخیره در EEPROM، مقدار جدید مجدداً بازخوانی و تایید می‌شود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = p.textSecondary,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = p.gold, contentColor = Color(0xFF0E0A03)),
            ) {
                Text("تایید و نوشتن (FC05)", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف", color = p.textSecondary) }
        },
    )
}

@Composable
private fun WordNumberEditDialog(
    def: RegisterDefinition,
    currentValue: Int?,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val p = LocalScadaPalette.current
    val min = def.minValue ?: 0
    val max = def.maxValue ?: 65535
    val scale = def.scale ?: 1.0
    val usesDecimalInput = scale < 1.0

    val initialText = remember(def.key, currentValue) {
        if (currentValue == null) {
            ""
        } else if (usesDecimalInput) {
            val decimals = if (scale >= 0.1) 1 else 2
            "%.${decimals}f".format(java.util.Locale.US, currentValue * scale)
        } else {
            currentValue.toString()
        }
    }
    var text by remember(def.key, currentValue) { mutableStateOf(initialText) }

    val parsedRaw: Int? = remember(text, scale) {
        val normalized = text.trim().replace(',', '.')
        if (normalized.isEmpty()) return@remember null
        if (usesDecimalInput) {
            normalized.toDoubleOrNull()?.let { (it / scale).roundToInt() }
        } else {
            normalized.toIntOrNull()
        }
    }
    val inRange = parsedRaw != null && parsedRaw in min..max
    val changed = parsedRaw != null && parsedRaw != currentValue

    val rangeSummary = if (usesDecimalInput) {
        "محدوده مجاز: ${PersianLabels.formatRegisterValue(def, min)} تا ${PersianLabels.formatRegisterValue(def, max)}"
    } else {
        "محدوده مجاز: $min تا $max${def.unit?.let { " $it" }.orEmpty()}"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = p.panel,
        shape = RoundedCornerShape(22.dp),
        icon = { Icon(Icons.Outlined.Edit, contentDescription = null, tint = p.gold) },
        title = { Text("ویرایش پارامتر سخت‌افزاری", style = MaterialTheme.typography.titleMedium, color = p.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    PersianLabels.registerLabel(def),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = p.textPrimary,
                )
                LtrText(
                    text = "FC06 (Write Single Register)  •  Addr ${def.documentedAddress} (0x%04X)".format(def.documentedAddress),
                    style = MonoStyle.copy(fontSize = 11.sp),
                    color = p.cyan,
                )
                Text(rangeSummary, style = MaterialTheme.typography.bodySmall, color = p.textSecondary)
                currentValue?.let { cur ->
                    Text(
                        "مقدار فعلی: ${PersianLabels.formatRegisterValue(def, cur)} (خام: $cur)",
                        style = MaterialTheme.typography.bodySmall,
                        color = p.textSecondary,
                    )
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { input ->
                        text = if (usesDecimalInput) {
                            input.filter { it.isDigit() || it == '.' || it == ',' }
                        } else {
                            input.filter { it.isDigit() }
                        }
                    },
                    label = {
                        Text(if (usesDecimalInput) "مقدار جدید (${def.unit ?: ""})" else "مقدار جدید ($min..$max)")
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    isError = text.isNotEmpty() && !inRange,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (usesDecimalInput) KeyboardType.Decimal else KeyboardType.Number,
                    ),
                    supportingText = {
                        when {
                            text.isEmpty() -> Text(rangeSummary)
                            !inRange -> Text("خارج از محدوده مجاز ($rangeSummary)", color = p.fault)
                            else -> Text(
                                "ثبت خواهد شد: ${PersianLabels.formatRegisterValue(def, parsedRaw!!)}  (مقدار خام مدباس: $parsedRaw)",
                                color = p.ok,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "پس از ارسال فرمان FC06 و ذخیره در EEPROM، مقدار جدید مجدداً بازخوانی و تایید می‌شود.",
                    style = MaterialTheme.typography.labelSmall,
                    color = p.warning,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (inRange) parsedRaw?.let(onConfirm) },
                enabled = inRange && changed,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = p.gold, contentColor = Color(0xFF0E0A03)),
            ) {
                Text("تایید و نوشتن (FC06)", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف", color = p.textSecondary) }
        },
    )
}

@Composable
private fun WordEnumEditDialog(
    def: RegisterDefinition,
    currentValue: Int?,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val p = LocalScadaPalette.current
    val options = remember(def.key) { def.enumLabels.keys.sorted() }
    var selected by remember(def.key, currentValue) {
        mutableIntStateOf(currentValue ?: options.firstOrNull() ?: 0)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = p.panel,
        shape = RoundedCornerShape(22.dp),
        icon = { Icon(Icons.Outlined.Tune, contentDescription = null, tint = p.gold) },
        title = { Text("انتخاب عملکرد ${PersianLabels.registerLabel(def)}", style = MaterialTheme.typography.titleMedium, color = p.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LtrText(
                    text = "FC06 (Write Single Register)  •  Addr ${def.documentedAddress} (0x%04X)".format(def.documentedAddress),
                    style = MonoStyle.copy(fontSize = 11.sp),
                    color = p.cyan,
                )
                Spacer(Modifier.height(4.dp))
                options.forEach { code ->
                    val isSel = selected == code
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSel) p.goldSoft else Color.Transparent)
                            .clickable { selected = code }
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        RadioButton(selected = isSel, onClick = { selected = code })
                        Text(
                            text = "$code — ${PersianLabels.enumOptionLabel(def, code)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSel) p.gold else p.textPrimary,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "پس از تایید، فرمان FC06 ارسال شده و مقدار ثبت‌شده پس از ذخیره در EEPROM بازخوانی می‌شود.",
                    style = MaterialTheme.typography.labelSmall,
                    color = p.warning,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selected) },
                enabled = selected != currentValue,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = p.gold, contentColor = Color(0xFF0E0A03)),
            ) {
                Text("تایید و نوشتن (FC06)", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف", color = p.textSecondary) }
        },
    )
}
