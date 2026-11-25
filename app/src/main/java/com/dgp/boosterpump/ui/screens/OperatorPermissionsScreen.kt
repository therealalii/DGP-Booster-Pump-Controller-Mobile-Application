package com.dgp.boosterpump.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.LockReset
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.SettingsEthernet
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dgp.boosterpump.auth.OperatorPermission
import com.dgp.boosterpump.auth.OperatorPermissions
import com.dgp.boosterpump.auth.PermissionGroup
import com.dgp.boosterpump.ui.components.BadgeTone
import com.dgp.boosterpump.ui.components.DgpOutlinedButton
import com.dgp.boosterpump.ui.components.LtrText
import com.dgp.boosterpump.ui.components.NoticeBox
import com.dgp.boosterpump.ui.components.ScadaPanel
import com.dgp.boosterpump.ui.components.StatusBadge
import com.dgp.boosterpump.ui.theme.LocalScadaPalette
import com.dgp.boosterpump.ui.theme.MonoStyle

/**
 * Reusable Admin-only entry card for «دسترسی های اپراتور», embedded inside Admin Panel screens
 * so administrators can immediately see and open operator access controls.
 */
@Composable
fun OperatorPermissionsAdminCard(
    permissions: OperatorPermissions,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = LocalScadaPalette.current
    val writeCount = permissions.grantedWriteCount
    val summaryLabel = if (writeCount == 0) {
        "فقط خواندنی"
    } else {
        "$writeCount مجوز تغییر"
    }
    val summaryTone = if (writeCount == 0) BadgeTone.INFO else BadgeTone.GOLD

    Surface(
        onClick = onClick,
        color = p.panel,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.95.dp, p.gold.copy(alpha = 0.42f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(p.goldSoft)
                    .border(0.85.dp, p.gold.copy(alpha = 0.45f), RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.ManageAccounts,
                    contentDescription = null,
                    tint = p.gold,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = "دسترسی های اپراتور",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = p.textPrimary,
                )
                Text(
                    text = "مدیریت مجوزهای حساب کاربری اپراتور (مخصوص مدیر سیستم)",
                    style = MaterialTheme.typography.labelSmall,
                    color = p.textSecondary,
                )
                StatusBadge(
                    text = summaryLabel,
                    tone = summaryTone,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = "باز کردن دسترسی های اپراتور",
                tint = p.gold,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/**
 * Admin-only management screen («دسترسی های اپراتور») allowing the Administrator to
 * individually grant or revoke every feature and write operation for the OPERATOR account.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OperatorPermissionsScreen(
    permissions: OperatorPermissions,
    onTogglePermission: (OperatorPermission, Boolean) -> Unit,
    onApplyPreset: (OperatorPermissions) -> Unit,
    onResetToReadOnly: () -> Unit,
    onBack: () -> Unit,
) {
    val p = LocalScadaPalette.current
    val isDefaultReadOnly = permissions == OperatorPermissions.READ_ONLY_DEFAULT
    val isDailyPreset = permissions == OperatorPermissions.DAILY_OPERATOR_PRESET

    val viewPerms = OperatorPermission.entries.filter { it.group == PermissionGroup.VIEW }
    val grantedViews = viewPerms.count { it in permissions }

    Column(modifier = Modifier.fillMaxSize()) {
        // Navigation & Admin-Only Header Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Surface(
                onClick = onBack,
                color = p.panel,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(0.8.dp, p.panelBorder),
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
                        text = "بازگشت",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = p.textPrimary,
                    )
                }
            }
            StatusBadge("مخصوص مدیر سیستم", BadgeTone.GOLD)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // 1. Overview & Quick Presets Panel
            item {
                ScadaPanel(
                    title = "دسترسی های اپراتور",
                    subtitle = "تعیین مجوزهای حساب کاربری اپراتور (OPERATOR) به صورت جداگانه",
                    icon = Icons.Outlined.AdminPanelSettings,
                    accentColor = p.gold,
                    borderColor = p.gold.copy(alpha = 0.42f),
                ) {
                    NoticeBox(
                        text = "حساب «مدیر سیستم (ADMIN)» همواره دارای دسترسی کامل است. به صورت پیش‌فرض، اپراتور فقط دسترسی مشاهده (فقط خواندنی) دارد و نمی‌تواند هیچ تغییری ایجاد کند؛ مگر مجوز آن را در زیر فعال کنید.",
                        tone = BadgeTone.INFO,
                    )

                    // Live summary badges
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        StatusBadge(
                            text = if (permissions.grantedWriteCount == 0) {
                                "حالت فعلی: فقط خواندنی"
                            } else {
                                "مجوزهای ویرایش: ${permissions.grantedWriteCount} از ${permissions.totalWriteCount}"
                            },
                            tone = if (permissions.grantedWriteCount == 0) BadgeTone.INFO else BadgeTone.GOLD,
                        )
                        StatusBadge(
                            text = "بخش‌های مشاهده: $grantedViews از ${viewPerms.size}",
                            tone = BadgeTone.OK,
                        )
                    }

                    // Quick Preset Buttons (every permission remains individually editable below)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DgpOutlinedButton(
                            text = "بازنشانی به فقط خواندنی (پیش‌فرض)",
                            icon = Icons.Outlined.LockReset,
                            accentColor = p.gold,
                            enabled = !isDefaultReadOnly,
                            onClick = onResetToReadOnly,
                            height = 44.dp,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        DgpOutlinedButton(
                            text = "الگوی عملیات روزانه (فشار، پمپ، درایو، رفع خطا)",
                            icon = Icons.Outlined.Shield,
                            accentColor = p.cyan,
                            enabled = !isDailyPreset,
                            onClick = { onApplyPreset(OperatorPermissions.DAILY_OPERATOR_PRESET) },
                            height = 44.dp,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(p.okBg)
                            .border(0.75.dp, p.ok.copy(alpha = 0.28f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircleOutline,
                            contentDescription = null,
                            tint = p.ok,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "تمامی تغییرات بلافاصله و به صورت خودکار روی دستگاه ذخیره می‌شوند.",
                            style = MaterialTheme.typography.labelSmall,
                            color = p.ok,
                        )
                    }
                }
            }

            // 2. Grouped Individual Permission Toggles
            PermissionGroup.entries.forEach { group ->
                val groupItems = OperatorPermission.entries.filter { it.group == group }
                val activeCount = groupItems.count { it in permissions }
                item(key = group.name) {
                    PermissionGroupPanel(
                        group = group,
                        items = groupItems,
                        permissions = permissions,
                        activeCount = activeCount,
                        onTogglePermission = onTogglePermission,
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionGroupPanel(
    group: PermissionGroup,
    items: List<OperatorPermission>,
    permissions: OperatorPermissions,
    activeCount: Int,
    onTogglePermission: (OperatorPermission, Boolean) -> Unit,
) {
    val p = LocalScadaPalette.current
    val (icon, accent) = groupStyle(group, p.cyan, p.gold, p.warning)

    ScadaPanel(
        title = group.titleFa,
        subtitle = group.subtitleFa,
        icon = icon,
        accentColor = accent,
        trailing = {
            StatusBadge(
                text = "$activeCount / ${items.size}",
                tone = when {
                    activeCount == 0 -> BadgeTone.NEUTRAL
                    group == PermissionGroup.VIEW -> BadgeTone.INFO
                    else -> BadgeTone.GOLD
                },
            )
        },
    ) {
        if (group == PermissionGroup.HARDWARE_SETTINGS && !permissions.canViewHardwareMenu && permissions.hasAnyHardwareMenuWrite) {
            NoticeBox(
                text = "به دلیل فعال بودن حداقل یک مجوز ویرایش در این بخش، منوی Hardware Menu به صورت خودکار برای اپراتور قابل مشاهده خواهد بود.",
                tone = BadgeTone.INFO,
            )
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            items.forEachIndexed { idx, perm ->
                if (idx > 0) HorizontalDivider(color = p.hairline)
                PermissionToggleRow(
                    permission = perm,
                    checked = perm in permissions,
                    onCheckedChange = { enabled -> onTogglePermission(perm, enabled) },
                )
            }
        }
    }
}

@Composable
private fun PermissionToggleRow(
    permission: OperatorPermission,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val p = LocalScadaPalette.current
    val tagColor = if (permission.isWriteOperation) p.warning else p.cyan
    val tagBg = if (permission.isWriteOperation) p.warningBg else p.cyanSoft
    val tagText = if (permission.isWriteOperation) "ویرایش / فرمان" else "خواندنی"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(vertical = 10.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = permission.titleFa,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = if (checked) p.textPrimary else p.textPrimary.copy(alpha = 0.85f),
            )
            LtrText(
                text = permission.titleEn,
                style = MonoStyle.copy(fontSize = 10.5.sp),
                color = if (checked) p.gold else p.textSecondary,
            )
            Text(
                text = permission.descriptionFa,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                color = p.textSecondary,
            )
            Surface(
                color = tagBg,
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(0.7.dp, tagColor.copy(alpha = 0.35f)),
                modifier = Modifier.padding(top = 2.dp),
            ) {
                Text(
                    text = tagText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = tagColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Switch(
            checked = checked,
            onCheckedChange = null, // Handled by parent toggleable Row for unified tap target
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF0A0702),
                checkedTrackColor = p.gold,
                checkedBorderColor = p.goldBright,
                uncheckedThumbColor = p.textSecondary,
                uncheckedTrackColor = p.panelElevated,
                uncheckedBorderColor = p.panelBorder,
            ),
        )
    }
}

private fun groupStyle(
    group: PermissionGroup,
    cyan: Color,
    gold: Color,
    warning: Color,
): Pair<ImageVector, Color> = when (group) {
    PermissionGroup.VIEW -> Icons.Outlined.Visibility to cyan
    PermissionGroup.DAILY_CONTROL -> Icons.Outlined.PowerSettingsNew to gold
    PermissionGroup.HARDWARE_SETTINGS -> Icons.Outlined.Tune to warning
    PermissionGroup.SYSTEM_TOOLS -> Icons.Outlined.SettingsEthernet to cyan
}
