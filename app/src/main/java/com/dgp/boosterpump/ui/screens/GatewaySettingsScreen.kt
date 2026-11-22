package com.dgp.boosterpump.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.SettingsEthernet
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dgp.boosterpump.controller.ControllerModel
import com.dgp.boosterpump.gateway.ConfigValidation
import com.dgp.boosterpump.gateway.DataSourceMode
import com.dgp.boosterpump.gateway.ProtocolMapping
import com.dgp.boosterpump.modbus.Framing
import com.dgp.boosterpump.modbus.ReadFunction
import com.dgp.boosterpump.ui.PersianLabels
import com.dgp.boosterpump.ui.components.BadgeTone
import com.dgp.boosterpump.ui.components.NoticeBox
import com.dgp.boosterpump.ui.components.ScadaPanel
import com.dgp.boosterpump.ui.components.StatusBadge
import com.dgp.boosterpump.ui.theme.LocalScadaPalette
import com.dgp.boosterpump.viewmodel.GatewayFormState
import com.dgp.boosterpump.viewmodel.GatewaySettingsViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GatewaySettingsScreen(
    form: GatewayFormState,
    viewModel: GatewaySettingsViewModel,
    onSavedAndConnected: () -> Unit,
) {
    val p = LocalScadaPalette.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScadaPanel(
                title = "منبع داده و مدل کنترلر",
                subtitle = "انتخاب بین اتصال واقعی به مبدل سمین ES 487 یا شبیه‌سازی کامل بدون سخت‌افزار",
                icon = Icons.Outlined.Memory,
                accentColor = p.cyan,
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    DataSourceMode.entries.forEach { mode ->
                        FilterChip(
                            selected = form.mode == mode,
                            onClick = { viewModel.updateMode(mode) },
                            label = { Text(PersianLabels.modeLabel(mode)) },
                        )
                    }
                }
                if (form.mode == DataSourceMode.SIMULATION) {
                    NoticeBox(
                        text = "در حالت شبیه‌سازی، داده‌های BPC02 درون حافظه تولید شده و از کل لایه رمزگشایی مدباس عبور می‌کنند. نوار هشدار نارنجی همواره بالای صفحه نمایش داده می‌شود تا داده‌های شبیه‌سازی با دستگاه واقعی اشتباه نشوند.",
                        tone = BadgeTone.WARNING,
                    )
                }

                Text("مدل کنترلر بوستر پمپ", style = MaterialTheme.typography.labelMedium, color = p.textSecondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ControllerModel.entries.forEach { model ->
                        FilterChip(
                            selected = form.controllerModel == model,
                            onClick = { viewModel.updateModel(model) },
                            enabled = model.isSupported,
                            label = { Text(PersianLabels.modelLabel(model)) },
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
                    subtitle = "مبدل صنعتی RS485 به Modbus TCP/IP (تغذیه ۵ تا ۴۸ ولت DC، تنظیمات تحت وب)",
                    icon = Icons.Outlined.Router,
                    accentColor = p.cyan,
                ) {
                    OutlinedTextField(
                        value = form.host,
                        onValueChange = viewModel::updateHost,
                        label = { Text("آدرس IP یا نام میزبان مبدل ES 487") },
                        placeholder = { Text("مثلاً 192.168.1.50 (از نرم‌افزار IP Finder سمین)") },
                        singleLine = true,
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
                            label = { Text("پورت TCP") },
                            supportingText = {
                                Text(
                                    form.errors[ConfigValidation.Field.PORT]?.let {
                                        PersianLabels.validationError(ConfigValidation.Field.PORT, it)
                                    } ?: "پورت وب مبدل (معمولاً 502)",
                                )
                            },
                            singleLine = true,
                            isError = ConfigValidation.Field.PORT in form.errors,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = form.unitIdText,
                            onValueChange = viewModel::updateUnitId,
                            label = { Text("شناسه Slave / Unit ID") },
                            supportingText = {
                                Text(
                                    form.errors[ConfigValidation.Field.UNIT_ID]?.let {
                                        PersianLabels.validationError(ConfigValidation.Field.UNIT_ID, it)
                                    } ?: "منوی Network (تاییدشده: 1)",
                                )
                            },
                            singleLine = true,
                            isError = ConfigValidation.Field.UNIT_ID in form.errors,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Text("حالت فریم‌بندی شبکه (مطابق تنظیمات وب مبدل ES 487)", style = MaterialTheme.typography.labelMedium, color = p.textSecondary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Framing.entries.forEach { framing ->
                            FilterChip(
                                selected = form.framing == framing,
                                onClick = { viewModel.updateFraming(framing) },
                                label = { Text(PersianLabels.framingLabel(framing)) },
                            )
                        }
                    }
                    Text(
                        PersianLabels.framingDescription(form.framing),
                        style = MaterialTheme.typography.bodySmall,
                        color = p.textSecondary,
                    )

                    NoticeBox(
                        text = "در صفحه تنظیمات وب مبدل سمین ES 487 مطمئن شوید: حالت کاری = TCP Server (تبدیل Modbus TCP به Modbus RTU) و پورت RS485 روی نرخ ۹۶۰۰ بیت‌برثانیه، ۸ بیت داده، بدون Parity و ۱ بیت Stop (9600 8N1) تنظیم شده باشد.",
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
                    text = "در تست سخت‌افزاری BPC02 تایید شد که جدول Bit فقط با FC01 و جدول Word فقط با FC03 و آفست 0 کار می‌کند. توابع FC02 و FC04 توسط کنترلر رد می‌شوند — برای BPC02 مقادیر پیش‌فرض (FC01 + FC03 + آفست 0) را حفظ کنید.",
                    tone = BadgeTone.OK,
                )

                Text("تابع خواندن جدول Bit (آدرس‌های ۰–۳۳)", style = MaterialTheme.typography.labelMedium, color = p.textSecondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(ReadFunction.READ_COILS, ReadFunction.READ_DISCRETE_INPUTS).forEach { fn ->
                        FilterChip(
                            selected = form.bitReadFunction == fn,
                            onClick = { viewModel.updateBitFunction(fn) },
                            label = { Text(PersianLabels.readFunctionShort(fn)) },
                        )
                    }
                }

                Text("تابع خواندن جدول Word (آدرس‌های ۰–۸۰)", style = MaterialTheme.typography.labelMedium, color = p.textSecondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(ReadFunction.READ_HOLDING_REGISTERS, ReadFunction.READ_INPUT_REGISTERS).forEach { fn ->
                        FilterChip(
                            selected = form.wordReadFunction == fn,
                            onClick = { viewModel.updateWordFunction(fn) },
                            label = { Text(PersianLabels.readFunctionShort(fn)) },
                        )
                    }
                }

                Text("آفست آدرس PDU", style = MaterialTheme.typography.labelMedium, color = p.textSecondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProtocolMapping.ALLOWED_OFFSETS.forEach { offset ->
                        val label = when (offset) {
                            0 -> "0 (تاییدشده)"
                            -1 -> "-1 (مبنای ۱ به ۰)"
                            else -> "+1"
                        }
                        FilterChip(
                            selected = form.addressOffset == offset,
                            onClick = { viewModel.updateAddressOffset(offset) },
                            label = { Text(label) },
                        )
                    }
                }
            }
        }

        item {
            ScadaPanel(
                title = "زمان‌بندی نمونه‌برداری و Timeout",
                subtitle = "برای جلوگیری از اشباع باس RS485 در نرخ ۹۶۰۰، بازه نمونه‌برداری ≥ ۱۰۰۰ میلی‌ثانیه توصیه می‌شود",
                icon = Icons.Outlined.Timer,
                accentColor = p.cyan,
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = form.pollIntervalText,
                        onValueChange = viewModel::updatePollInterval,
                        label = { Text("بازه خوانش (ms)") },
                        singleLine = true,
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
                            label = { Text("مهلت پاسخ (ms)") },
                            singleLine = true,
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
                        label = { Text("مهلت اتصال سوکت TCP (میلی‌ثانیه)") },
                        singleLine = true,
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                form.statusMessage?.let { msg ->
                    val faMsg = if (msg.contains("Connecting")) "تنظیمات ذخیره شد — در حال اتصال…" else "تنظیمات با موفقیت ذخیره شد."
                    NoticeBox(text = faMsg, tone = BadgeTone.OK)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = { viewModel.save(andConnect = true, onConnected = onSavedAndConnected) },
                        modifier = Modifier.weight(1.2f),
                    ) {
                        Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("ذخیره و اتصال")
                    }
                    OutlinedButton(
                        onClick = { viewModel.save(andConnect = false) },
                        modifier = Modifier.weight(0.8f),
                    ) {
                        Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("فقط ذخیره")
                    }
                }
            }
        }
    }
}
