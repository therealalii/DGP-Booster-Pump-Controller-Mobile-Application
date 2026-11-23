package com.dgp.boosterpump.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.SettingsEthernet
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dgp.boosterpump.controller.ControllerModel
import com.dgp.boosterpump.gateway.ConfigValidation
import com.dgp.boosterpump.gateway.DataSourceMode
import com.dgp.boosterpump.gateway.ProtocolMapping
import com.dgp.boosterpump.modbus.Framing
import com.dgp.boosterpump.modbus.ReadFunction
import com.dgp.boosterpump.ui.PersianLabels
import com.dgp.boosterpump.ui.components.BadgeTone
import com.dgp.boosterpump.ui.components.DgpGoldButton
import com.dgp.boosterpump.ui.components.DgpPillChip
import com.dgp.boosterpump.ui.components.DgpSegmentedControl
import com.dgp.boosterpump.ui.components.LtrText
import com.dgp.boosterpump.ui.components.NoticeBox
import com.dgp.boosterpump.ui.components.ScadaPanel
import com.dgp.boosterpump.ui.components.StatusBadge
import com.dgp.boosterpump.ui.theme.LocalScadaPalette
import com.dgp.boosterpump.ui.theme.MonoStyle
import com.dgp.boosterpump.viewmodel.GatewayFormState
import com.dgp.boosterpump.viewmodel.GatewaySettingsViewModel

@Composable
fun GatewaySettingsScreen(
    form: GatewayFormState,
    viewModel: GatewaySettingsViewModel,
    canEditGateway: Boolean,
    onSavedAndConnected: () -> Unit,
) {
    val p = LocalScadaPalette.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (!canEditGateway) {
            item {
                NoticeBox(
                    text = "مشاهده فقط‌خواندنی: تغییر آدرس IP، پورت و پروتکل مبدل سمین ES 487 مخصوص «مدیر سیستم» و «نصاب» است.",
                    tone = BadgeTone.WARNING,
                )
            }
        }

        item {
            ScadaPanel(
                title = "منبع داده و مدل کنترلر",
                subtitle = "انتخاب بین اتصال واقعی به مبدل سمین ES 487 یا شبیه‌سازی کامل",
                icon = Icons.Outlined.Memory,
                accentColor = p.gold,
            ) {
                DgpSegmentedControl(
                    items = DataSourceMode.entries,
                    selected = form.mode,
                    label = { mode ->
                        when (mode) {
                            DataSourceMode.LIVE -> "اتصال واقعی (ES 487)"
                            DataSourceMode.SIMULATION -> "حالت شبیه‌سازی"
                        }
                    },
                    onSelect = viewModel::updateMode,
                    enabled = canEditGateway,
                )

                if (form.mode == DataSourceMode.SIMULATION) {
                    NoticeBox(
                        text = "در حالت شبیه‌سازی، داده‌های BPC02 درون حافظه تولید شده و از کل لایه رمزگشایی مدباس عبور می‌کنند.",
                        tone = BadgeTone.WARNING,
                    )
                }

                Text("مدل کنترلر بوستر پمپ", style = MaterialTheme.typography.labelMedium, color = p.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ControllerModel.entries.forEach { model ->
                        DgpPillChip(
                            label = PersianLabels.modelLabel(model),
                            selected = form.controllerModel == model,
                            onClick = { viewModel.updateModel(model) },
                            enabled = model.isSupported && canEditGateway,
                        )
                    }
                }
                Text(
                    PersianLabels.modelNote(form.controllerModel),
                    style = MaterialTheme.typography.bodySmall,
                    color = p.textSecondary,
                )
            }
        }

        if (form.mode == DataSourceMode.LIVE) {
            item {
                ScadaPanel(
                    title = "تنظیمات مبدل مدباس سمین ES 487",
                    subtitle = "مبدل صنعتی RS485 به Modbus TCP/IP (تغذیه ۵ تا ۴۸ ولت DC)",
                    icon = Icons.Outlined.Router,
                    accentColor = p.cyan,
                ) {
                    // Clean visual architecture path pill
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(p.panelElevated)
                            .border(0.8.dp, p.panelBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("اپلیکیشن DGP", style = MaterialTheme.typography.labelSmall, color = p.gold, fontWeight = FontWeight.Bold)
                        LtrText("TCP/IP", style = MonoStyle.copy(fontSize = 9.5.sp), color = p.cyan)
                        Text("سمین ES 487", style = MaterialTheme.typography.labelSmall, color = p.textPrimary, fontWeight = FontWeight.Bold)
                        LtrText("RS485 9600", style = MonoStyle.copy(fontSize = 9.5.sp), color = p.ok)
                        Text("BPC02", style = MaterialTheme.typography.labelSmall, color = p.gold, fontWeight = FontWeight.Bold)
                    }

                    OutlinedTextField(
                        value = form.host,
                        onValueChange = viewModel::updateHost,
                        readOnly = !canEditGateway,
                        label = { Text("آدرس IP یا نام میزبان مبدل ES 487") },
                        placeholder = { Text("مثلاً 192.168.1.50 (از IP Finder سمین)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        isError = ConfigValidation.Field.HOST in form.errors,
                        supportingText = form.errors[ConfigValidation.Field.HOST]?.let { err ->
                            { Text(PersianLabels.validationError(ConfigValidation.Field.HOST, err)) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = form.portText,
                            onValueChange = viewModel::updatePort,
                            readOnly = !canEditGateway,
                            label = { Text("پورت TCP") },
                            supportingText = {
                                Text(
                                    form.errors[ConfigValidation.Field.PORT]?.let {
                                        PersianLabels.validationError(ConfigValidation.Field.PORT, it)
                                    } ?: "پورت وب (معمولاً 502)",
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            isError = ConfigValidation.Field.PORT in form.errors,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = form.unitIdText,
                            onValueChange = viewModel::updateUnitId,
                            readOnly = !canEditGateway,
                            label = { Text("شناسه Slave ID") },
                            supportingText = {
                                Text(
                                    form.errors[ConfigValidation.Field.UNIT_ID]?.let {
                                        PersianLabels.validationError(ConfigValidation.Field.UNIT_ID, it)
                                    } ?: "منوی Network (تاییدشده: 1)",
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            isError = ConfigValidation.Field.UNIT_ID in form.errors,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Text("حالت فریم‌بندی شبکه (مطابق تنظیمات وب مبدل)", style = MaterialTheme.typography.labelMedium, color = p.textSecondary)
                    DgpSegmentedControl(
                        items = Framing.entries,
                        selected = form.framing,
                        label = PersianLabels::framingLabel,
                        onSelect = viewModel::updateFraming,
                        enabled = canEditGateway,
                    )
                    Text(
                        PersianLabels.framingDescription(form.framing),
                        style = MaterialTheme.typography.bodySmall,
                        color = p.textSecondary,
                    )

                    NoticeBox(
                        text = "در صفحه تنظیمات وب مبدل سمین ES 487 مطمئن شوید: حالت کاری = Use server MODBUS TCP/IP، پورت RS485 روی 9600 8N1، و مقدار Waiting for slave reply(ms) حداقل 1000ms (برای پایداری نوشتن در EEPROM کنترلر) تنظیم شده باشد.",
                        tone = BadgeTone.INFO,
                    )
                }
            }
        }

        item {
            ScadaPanel(
                title = "پروفایل پروتکل مدباس BPC02",
                subtitle = "توابع تاییدشده روی کنترلر واقعی — قابل تغییر برای عیب‌یابی",
                icon = Icons.Outlined.SettingsEthernet,
                accentColor = p.ok,
                trailing = { StatusBadge("تایید سخت‌افزاری", BadgeTone.OK) },
            ) {
                NoticeBox(
                    text = "در تست سخت‌افزاری BPC02 تایید شد که فقط توابع FC01 (خواندن کویل)، FC03 (خواندن ورد)، FC05 (نوشتن تکی کویل) و FC06 (نوشتن تکی ورد) با آفست 0 پشتیبانی می‌شوند. مقادیر پیش‌فرض را حفظ کنید.",
                    tone = BadgeTone.OK,
                )

                Text("تابع خواندن جدول Bit (آدرس‌های ۰–۳۳)", style = MaterialTheme.typography.labelMedium, color = p.textSecondary)
                DgpSegmentedControl(
                    items = listOf(ReadFunction.READ_COILS, ReadFunction.READ_DISCRETE_INPUTS),
                    selected = form.bitReadFunction,
                    label = PersianLabels::readFunctionShort,
                    onSelect = viewModel::updateBitFunction,
                    enabled = canEditGateway,
                    height = 38.dp,
                )

                Text("تابع خواندن جدول Word (آدرس‌های ۰–۸۰)", style = MaterialTheme.typography.labelMedium, color = p.textSecondary)
                DgpSegmentedControl(
                    items = listOf(ReadFunction.READ_HOLDING_REGISTERS, ReadFunction.READ_INPUT_REGISTERS),
                    selected = form.wordReadFunction,
                    label = PersianLabels::readFunctionShort,
                    onSelect = viewModel::updateWordFunction,
                    enabled = canEditGateway,
                    height = 38.dp,
                )

                Text("آفست آدرس PDU", style = MaterialTheme.typography.labelMedium, color = p.textSecondary)
                DgpSegmentedControl(
                    items = ProtocolMapping.ALLOWED_OFFSETS,
                    selected = form.addressOffset,
                    label = { offset ->
                        when (offset) {
                            0 -> "0 (تاییدشده)"
                            -1 -> "-1 (مبنای ۱ به ۰)"
                            else -> "+1"
                        }
                    },
                    onSelect = viewModel::updateAddressOffset,
                    enabled = canEditGateway,
                    height = 38.dp,
                )
            }
        }

        item {
            ScadaPanel(
                title = "زمان‌بندی نمونه‌برداری و Timeout",
                subtitle = "برای پایداری باس RS485 در نرخ ۹۶۰۰، بازه خوانش ≥ ۱۰۰۰ میلی‌ثانیه توصیه می‌شود",
                icon = Icons.Outlined.Timer,
                accentColor = p.cyan,
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = form.pollIntervalText,
                        onValueChange = viewModel::updatePollInterval,
                        readOnly = !canEditGateway,
                        label = { Text("بازه خوانش (ms)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        isError = ConfigValidation.Field.POLL_INTERVAL in form.errors,
                        supportingText = form.errors[ConfigValidation.Field.POLL_INTERVAL]?.let { e ->
                            { Text(PersianLabels.validationError(ConfigValidation.Field.POLL_INTERVAL, e)) }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    if (form.mode == DataSourceMode.LIVE) {
                        OutlinedTextField(
                            value = form.responseTimeoutText,
                            onValueChange = viewModel::updateResponseTimeout,
                            readOnly = !canEditGateway,
                            label = { Text("مهلت پاسخ (ms)") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            isError = ConfigValidation.Field.RESPONSE_TIMEOUT in form.errors,
                            supportingText = form.errors[ConfigValidation.Field.RESPONSE_TIMEOUT]?.let { e ->
                                { Text(PersianLabels.validationError(ConfigValidation.Field.RESPONSE_TIMEOUT, e)) }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                if (form.mode == DataSourceMode.LIVE) {
                    OutlinedTextField(
                        value = form.connectTimeoutText,
                        onValueChange = viewModel::updateConnectTimeout,
                        readOnly = !canEditGateway,
                        label = { Text("مهلت اتصال سوکت TCP (میلی‌ثانیه)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        isError = ConfigValidation.Field.CONNECT_TIMEOUT in form.errors,
                        supportingText = form.errors[ConfigValidation.Field.CONNECT_TIMEOUT]?.let { e ->
                            { Text(PersianLabels.validationError(ConfigValidation.Field.CONNECT_TIMEOUT, e)) }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                form.statusMessage?.let { msg ->
                    val faMsg = if (msg.contains("Connecting")) "تنظیمات ذخیره شد — در حال اتصال…" else "تنظیمات با موفقیت ذخیره شد."
                    NoticeBox(text = faMsg, tone = BadgeTone.OK)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DgpGoldButton(
                        text = "ذخیره و اتصال",
                        icon = Icons.Outlined.Check,
                        enabled = canEditGateway,
                        onClick = { viewModel.save(andConnect = true, onConnected = onSavedAndConnected) },
                        height = 48.dp,
                        modifier = Modifier.weight(1.25f),
                    )
                    OutlinedButton(
                        onClick = { viewModel.save(andConnect = false) },
                        enabled = canEditGateway,
                        shape = RoundedCornerShape(13.dp),
                        border = BorderStroke(0.9.dp, if (canEditGateway) p.panelHighlight else p.panelBorder),
                        modifier = Modifier
                            .weight(0.75f)
                            .height(48.dp),
                    ) {
                        Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("ذخیره")
                    }
                }
            }
        }
    }
}
