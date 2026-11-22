package com.dgp.boosterpump.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dgp.boosterpump.controller.RegisterReading
import com.dgp.boosterpump.session.ConnectionState
import com.dgp.boosterpump.ui.PersianLabels
import com.dgp.boosterpump.ui.components.BadgeTone
import com.dgp.boosterpump.ui.components.LtrText
import com.dgp.boosterpump.ui.components.NoticeBox
import com.dgp.boosterpump.ui.components.ScadaPanel
import com.dgp.boosterpump.ui.components.StatusBadge
import com.dgp.boosterpump.ui.components.formatTime
import com.dgp.boosterpump.ui.theme.LocalScadaPalette
import com.dgp.boosterpump.ui.theme.MonoStyle
import com.dgp.boosterpump.viewmodel.MonitoringUiState

/**
 * Persian Industrial Control & Hardware Parameters screen.
 *
 * Manual control operations (Address 99, Coils 50-59, Words 50-51) are visually previewed
 * in a strictly DISABLED/LOCKED state until bench write tests (BPC02_WRITE_TESTS.txt) are
 * completed and verified. Hardware configuration registers (4096-4110, 4173-4195) can be
 * queried on demand in read-only mode.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ControlAndConfigScreen(
    uiState: MonitoringUiState,
    onReadHardwareConfig: () -> Unit,
) {
    val p = LocalScadaPalette.current
    val cfgSnap = uiState.configSnapshot
    val isConnected = uiState.connectionState is ConnectionState.Connected

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScadaPanel(
                title = "کنترل دستی و فرمان‌های نوشتن (قفل ایمنی)",
                subtitle = "رجیستر ۹۹ (حالت دستی) • کویل ۵۰–۵۹ (رله‌های دستی) • ورد ۵۰–۵۱ (فرکانس آنالوگ)",
                icon = Icons.Outlined.AdminPanelSettings,
                accentColor = p.warning,
                borderColor = p.warning.copy(alpha = 0.55f),
                trailing = { StatusBadge("غیرفعال (ایمن)", BadgeTone.WARNING) },
            ) {
                NoticeBox(
                    text = "دستورات نوشتن مدباس (FC05 / FC06 / FC15 / FC16) در این نسخه به صورت کامل قفل شده‌اند. " +
                        "پس از انجام تست‌های ایزوله نوشتن مطابق فایل BPC02_WRITE_TESTS.txt و تایید مهندسی، پنل کنترل دستی و ویرایش پارامترها فعال خواهد شد.",
                    tone = BadgeTone.WARNING,
                )

                // Disabled Manual Mode preview panel
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(0.55f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(p.panelElevated)
                        .border(1.dp, p.panelBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Icon(Icons.Outlined.Lock, contentDescription = null, tint = p.warning, modifier = Modifier.size(15.dp))
                                Text("ورود به حالت دستی (Manual Mode)", style = MaterialTheme.typography.titleSmall, color = p.textPrimary)
                            }
                            Text(
                                "نوشتن مقدار 1234 در آدرس ۹۹ (بازگشت به اتوماتیک: مقدار 0)",
                                style = MaterialTheme.typography.bodySmall,
                                color = p.textSecondary,
                            )
                        }
                        Switch(checked = false, onCheckedChange = null, enabled = false)
                    }

                    HorizontalDivider(color = p.panelBorder)

                    Text("خروجی‌های دستی رله (کویل ۵۰ تا ۵۹ — در انتظار تایید FC05 / FC15)", style = MaterialTheme.typography.labelMedium, color = p.textSecondary)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        for (i in 0..9) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(p.panel)
                                    .border(1.dp, p.panelBorder, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(Icons.Outlined.Lock, contentDescription = null, tint = p.standby, modifier = Modifier.size(12.dp))
                                LtrText("OUT $i", style = MonoStyle.copy(fontSize = 11.sp), color = p.textSecondary)
                            }
                        }
                    }

                    HorizontalDivider(color = p.panelBorder)

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("سرعت آنالوگ درایو ۱ (ورد ۵۰ — ۰ تا ۵۰.۰ هرتز)", style = MaterialTheme.typography.labelMedium, color = p.textSecondary)
                            LtrText("0.0 Hz", style = MonoStyle, color = p.textSecondary)
                        }
                        Slider(value = 0f, onValueChange = {}, enabled = false)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("سرعت آنالوگ درایو ۲ (ورد ۵۱ — ۰ تا ۵۰.۰ هرتز)", style = MaterialTheme.typography.labelMedium, color = p.textSecondary)
                            LtrText("0.0 Hz", style = MonoStyle, color = p.textSecondary)
                        }
                        Slider(value = 0f, onValueChange = {}, enabled = false)
                    }
                }
            }
        }

        item {
            ScadaPanel(
                title = "تنظیمات سخت‌افزار کنترلر (خواندن درخواستی)",
                subtitle = "کویل ۴۰۹۶–۴۱۱۰ (FC01) و ورد ۴۱۷۳–۴۱۹۵ (FC03) — فقط خواندنی و ایمن",
                icon = Icons.Outlined.Tune,
                accentColor = p.cyan,
                trailing = {
                    Button(
                        onClick = onReadHardwareConfig,
                        enabled = isConnected && !cfgSnap.loading,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        if (cfgSnap.loading) {
                            CircularProgressIndicator(modifier = Modifier.size(15.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(6.dp))
                        } else {
                            Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                        }
                        Text("خواندن تنظیمات")
                    }
                },
            ) {
                NoticeBox(
                    text = "تنظیمات سخت‌افزار به صورت خودکار در هر ثانیه خوانده نمی‌شوند. با زدن دکمه «خواندن تنظیمات»، تمام ۲۴ پارامتر پیکربندی در ۶ بلوک تاییدشده (شامل ۴۱۷۸ و ۴۱۸۰) به صورت فقط‌خواندنی دریافت می‌شوند.",
                    tone = BadgeTone.INFO,
                )

                if (cfgSnap.timestampMillis != null) {
                    Text(
                        "آخرین خوانش تنظیمات در ساعت ${formatTime(cfgSnap.timestampMillis)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = p.cyan,
                    )
                }
                if (cfgSnap.errors.isNotEmpty()) {
                    NoticeBox(
                        text = "خطا در خواندن برخی بلوک‌ها: ${cfgSnap.errors.take(3).joinToString(" | ")}",
                        tone = BadgeTone.FAULT,
                    )
                }

                if (cfgSnap.readings.isEmpty()) {
                    Text(
                        if (isConnected) "برای دریافت وضعیت فعال‌سازی پمپ‌ها، Sleep Gap، اتو سرویس، MFI/MFO و آدرس مدباس دکمه «خواندن تنظیمات» را بزنید."
                        else "ابتدا به کنترلر یا حالت شبیه‌سازی متصل شوید تا امکان خواندن تنظیمات سخت‌افزار فعال شود.",
                        style = MaterialTheme.typography.bodySmall,
                        color = p.textSecondary,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        cfgSnap.readings.forEachIndexed { idx, reading ->
                            if (idx > 0) HorizontalDivider(color = p.panelBorder)
                            ConfigParameterRow(reading)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfigParameterRow(reading: RegisterReading) {
    val p = LocalScadaPalette.current
    val def = reading.definition
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = PersianLabels.registerLabel(def),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = p.textPrimary,
            )
            LtrText(
                text = "${PersianLabels.tableLabel(def.table)} • Addr ${def.documentedAddress} (0x%04X)".format(def.documentedAddress),
                style = MonoStyle.copy(fontSize = 10.5.sp),
                color = p.textSecondary,
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = reading.raw?.let { PersianLabels.formatRegisterValue(def, it) } ?: "—",
            style = MaterialTheme.typography.titleSmall,
            color = if (reading.raw != null) p.cyan else p.textSecondary,
        )
    }
}
