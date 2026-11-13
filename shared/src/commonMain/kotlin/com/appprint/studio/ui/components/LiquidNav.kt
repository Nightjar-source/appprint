package com.appprint.studio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.LocalIndication
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appprint.studio.ui.theme.Motion
import com.appprint.studio.ui.theme.contrastingTextColor

/**
 * Liquid glass navigation — the floating droplet that glides between destinations.
 *
 * Behaviour mirrors the reference «شريط تنقل مائي عائم»:
 *  - a glass droplet indicator measures the selected item and *slides* to it
 *    (position + size) with a slightly bouncy spring, instead of a static
 *    per-item background;
 *  - the selected item expands (icon + full label) while the others keep a
 *    tiny sub-label, so the bar grows around the selection without shifting;
 *  - the active icon pops (0.86 → 1) like a water drop settling.
 */
@Composable
fun LiquidNavGroup(
    items: List<NavItemModel>,
    selectedKey: String,
    onSelect: (NavItemModel) -> Unit,
    vertical: Boolean,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val activeIndex = items.indexOfFirst { it.key == selectedKey }.coerceAtLeast(0)

    var hostOrigin by remember { mutableStateOf(Offset.Zero) }
    val bounds = remember { mutableStateMapOf<Int, NavBounds>() }

    val target = bounds[activeIndex]
    val dropletSpec: FiniteAnimationSpec<Dp> = spring(dampingRatio = 0.55f, stiffness = 420f)

    val targetOffset: Offset = target?.position ?: Offset.Zero
    val targetSize: IntSize = target?.size ?: IntSize.Zero

    val offsetX by animateDpAsState(
        targetValue = with(density) { (targetOffset.x - hostOrigin.x).toDp() },
        animationSpec = dropletSpec,
        label = "dropletX",
    )
    val offsetY by animateDpAsState(
        targetValue = with(density) { (targetOffset.y - hostOrigin.y).toDp() },
        animationSpec = dropletSpec,
        label = "dropletY",
    )
    val dropletW by animateDpAsState(
        targetValue = with(density) { targetSize.width.toDp() },
        animationSpec = dropletSpec,
        label = "dropletW",
    )
    val dropletH by animateDpAsState(
        targetValue = with(density) { targetSize.height.toDp() },
        animationSpec = dropletSpec,
        label = "dropletH",
    )
    val dropletAlpha by animateFloatAsState(
        targetValue = if (target != null) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "dropletAlpha",
    )

    Box(modifier = modifier) {
        // the gliding droplet
        if (target != null) {
            Box(
                modifier = Modifier
                    .offset(x = offsetX, y = offsetY)
                    .size(dropletW, dropletH)
                    .graphicsLayer { alpha = dropletAlpha }
                    .clip(RoundedCornerShape(percent = 50))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                scheme.primaryContainer.copy(alpha = 0.95f),
                                scheme.primaryContainer.copy(alpha = 0.60f),
                                scheme.primaryContainer.copy(alpha = 0.88f),
                            ),
                        ),
                    )
                    .border(
                        width = 1.dp,
                        color = scheme.onPrimaryContainer.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(percent = 50),
                    ),
            )
        }

        @Composable
        fun ItemSlot(index: Int, item: NavItemModel) {
            Box(
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    bounds[index] = NavBounds(
                        position = coordinates.positionInParent(),
                        size = coordinates.size,
                    )
                },
            ) {
                LiquidNavItem(
                    item = item,
                    selected = index == activeIndex,
                    vertical = vertical,
                    onClick = { onSelect(item) },
                )
            }
        }

        if (vertical) {
            Column(
                modifier = Modifier.onGloballyPositioned { hostOrigin = it.positionInParent() },
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                items.forEachIndexed { index, item -> ItemSlot(index, item) }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { hostOrigin = it.positionInParent() },
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEachIndexed { index, item -> ItemSlot(index, item) }
            }
        }
    }
}

/** Simple model so the navigation stays decoupled from routes. */
data class NavItemModel(
    val key: String,
    val label: String,
    val icon: ImageVector,
)

private data class NavBounds(
    val position: Offset,
    val size: IntSize,
)

@Composable
private fun LiquidNavItem(
    item: NavItemModel,
    selected: Boolean,
    vertical: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val indication = LocalIndication.current
    val pop = remember { Animatable(1f) }

    LaunchedEffect(selected) {
        if (selected) {
            pop.snapTo(0.86f)
            pop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 700f))
        }
    }

    val iconColor by animateColorAsState(
        targetValue = if (selected) contrastingTextColor(scheme.primaryContainer) else scheme.onSurfaceVariant,
        animationSpec = Motion.effects(),
        label = "liquidNavIconColor",
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) contrastingTextColor(scheme.primaryContainer) else scheme.onSurfaceVariant,
        animationSpec = Motion.effects(),
        label = "liquidNavLabelColor",
    )

    if (vertical) {
        Column(
            modifier = Modifier
                .pressScale(interaction, pressedScale = 0.92f)
                .clip(RoundedCornerShape(percent = 50))
                .clickable(interactionSource = interaction, indication = indication, onClick = onClick)
                .semantics { role = Role.Tab }
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .widthIn(min = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = iconColor,
                modifier = Modifier
                    .size(21.dp)
                    .graphicsLayer {
                        scaleX = pop.value
                        scaleY = pop.value
                    },
            )
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    } else {
        Column(
            modifier = Modifier
                .pressScale(interaction, pressedScale = 0.92f)
                .clip(RoundedCornerShape(percent = 50))
                .clickable(interactionSource = interaction, indication = indication, onClick = onClick)
                .semantics { role = Role.Tab }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    tint = iconColor,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer {
                            scaleX = pop.value
                            scaleY = pop.value
                        },
                )
                AnimatedVisibility(
                    visible = selected,
                    enter = fadeIn(tween(180)) + expandHorizontally(animationSpec = Motion.spatialFast()),
                    exit = fadeOut(tween(140)) + shrinkHorizontally(animationSpec = Motion.effectsFast()),
                ) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = labelColor,
                        maxLines = 1,
                    )
                }
            }
            if (!selected) {
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                    color = labelColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Spacer(Modifier.height(11.dp))
            }
        }
    }
}
