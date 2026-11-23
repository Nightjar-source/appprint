package com.appprint.studio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Panel section header — small caps label with optional trailing action. */
@Composable
fun PanelSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

/** Slider with label + live value on the trailing end. */
@Composable
fun PanelSliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    valueText: String,
    modifier: Modifier = Modifier,
    steps: Int = 0,
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = scheme.onSurface,
            )
        }
        val sliderState = rememberSliderState(
            value = value,
            steps = steps,
            trackRange = valueRange,
        )
        // Parent state remains the source of truth; sync it into the stateful slider.
        sliderState.value = value
        Slider(
            state = sliderState,
            onValueChange = onValueChange,
            colors = androidx.compose.material3.SliderDefaults.colors(
                thumbColor = androidx.compose.ui.graphics.Color(0xFF006C47),
                activeTrackColor = androidx.compose.ui.graphics.Color(0xFF006C47),
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Switch row — title (+optional subtitle) leading, switch trailing. */
@Composable
fun PanelToggleRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(scheme.surfaceContainer)
            .clickable(interactionSource = interaction, indication = null, onClick = { onCheckedChange(!checked) })
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        M3Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

/**
 * The design's official M3 Expressive fluid toggle switch:
 * 50×30 track with 2dp border (off: surfaceHighest + outline border, on: primary),
 * thumb 15dp off (outline color) / 22dp on (onPrimary with check glyph).
 */
@Composable
fun M3Switch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val thumbSize by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (checked) 22.dp else 15.dp,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.72f, stiffness = 460f),
        label = "m3SwitchThumbSize",
    )
    val thumbOffset by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (checked) 24.dp else 4.dp,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.72f, stiffness = 460f),
        label = "m3SwitchThumbOffset",
    )
    Box(
        modifier = modifier
            .size(width = 50.dp, height = 30.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(if (checked) scheme.primary else scheme.surfaceContainerHighest)
            .border(2.dp, if (checked) scheme.primary else scheme.outline, RoundedCornerShape(percent = 50))
            .toggleable(
                value = checked,
                interactionSource = interaction,
                indication = null,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
    ) {
        // Offset the thumb itself — padding on a size-constrained box collapsed the
        // checked thumb to zero width (24dp offset inside a 22dp box).
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = thumbOffset)
                .size(thumbSize)
                .clip(RoundedCornerShape(percent = 50))
                .background(if (checked) scheme.onPrimary else scheme.outline),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(14.dp))
            } else {
                Icon(Icons.Filled.Close, contentDescription = null, tint = scheme.surfaceContainerHighest, modifier = Modifier.size(10.dp))
            }
        }
    }
}

/** Chip-styled selectable toggle (e.g. text modes ملون/رمادي/وثيقة) — the design's rounded-2xl option chips. */
@Composable
fun PanelChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    accent: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val selectedBg = if (accent) scheme.secondaryContainer else scheme.primary
    val selectedFg = if (accent) scheme.onSecondaryContainer else scheme.onPrimary
    Row(
        modifier = modifier
            .pressScale(interaction)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) selectedBg else scheme.surfaceContainerHigh)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            SpacerSmall()
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
            color = if (selected) selectedFg else scheme.onSurface,
            maxLines = 1,
        )
    }
}

@Composable
private fun SpacerSmall() {
    androidx.compose.foundation.layout.Spacer(Modifier.width(6.dp))
}
