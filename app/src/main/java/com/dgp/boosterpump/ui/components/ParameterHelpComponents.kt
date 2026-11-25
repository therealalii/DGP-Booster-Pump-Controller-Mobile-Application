package com.dgp.boosterpump.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dgp.boosterpump.controller.Bpc02Catalog
import com.dgp.boosterpump.ui.theme.LocalScadaPalette

/**
 * Compact, unobtrusive `?` help icon button placed directly next to an English parameter name.
 * Tapping opens [ParameterHelpBottomSheet] with a clear Persian explanation of the parameter.
 */
@Composable
fun ParameterHelpIcon(
    contentDescription: String = "راهنمای پارامتر",
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = LocalScadaPalette.current
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(p.cyanSoft)
                .border(0.75.dp, p.cyan.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                contentDescription = contentDescription,
                tint = p.cyan,
                modifier = Modifier.size(13.dp),
            )
        }
    }
}

/**
 * Clean, calm Material 3 bottom sheet displaying:
 *  - Official English parameter name & BPC-02 menu path
 *  - Optional current value, allowed range, and editability badge
 *  - Concise, user-friendly Persian explanation derived from `BPC-02.pdf`
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ParameterHelpBottomSheet(
    help: Bpc02Catalog.ParameterHelp,
    currentValue: String? = null,
    onDismiss: () -> Unit,
) {
    val p = LocalScadaPalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = p.panel,
        contentColor = p.textPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header: English Official Parameter Name + BPC-02 Menu Breadcrumb
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(p.cyanSoft)
                        .border(0.9.dp, p.cyan.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = p.cyan,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    LtrText(
                        text = help.fullEnglishName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = p.gold,
                    )
                    LtrText(
                        text = help.menuPath,
                        style = MaterialTheme.typography.labelSmall,
                        color = p.textSecondary,
                    )
                }
            }

            // Metadata chips (Current value, Allowed range, Editability)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (!currentValue.isNullOrBlank()) {
                    HelpMetaChip(label = "Current: $currentValue", accent = p.cyan)
                }
                if (!help.rangeHint.isNullOrBlank()) {
                    HelpMetaChip(label = "Range: ${help.rangeHint}", accent = p.gold)
                }
                HelpMetaChip(
                    label = if (help.isWritable) "Editable" else "Read-Only",
                    accent = if (help.isWritable) p.ok else p.textSecondary,
                )
            }

            // Clear Persian User Explanation Card
            Surface(
                color = p.panelElevated,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(0.9.dp, p.panelBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "توضیح کاربرد پارامتر",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = p.cyan,
                    )
                    Text(
                        text = help.helpFa,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 24.sp),
                        color = p.textPrimary,
                    )
                }
            }

            Spacer(Modifier.height(2.dp))

            DgpGoldButton(
                text = "متوجه شدم",
                onClick = onDismiss,
                height = 46.dp,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun HelpMetaChip(label: String, accent: androidx.compose.ui.graphics.Color) {
    val p = LocalScadaPalette.current
    Surface(
        color = accent.copy(alpha = if (p.isDark) 0.13f else 0.10f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(0.8.dp, accent.copy(alpha = 0.40f)),
    ) {
        LtrText(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = accent,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
