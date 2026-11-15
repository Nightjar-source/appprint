package com.appprint.studio.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BorderOuter
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.appprint.studio.core.model.AppTheme
import com.appprint.studio.ui.components.AppSnackbar
import com.appprint.studio.ui.components.Chrome
import com.appprint.studio.ui.components.ShareSheet
import com.appprint.studio.ui.components.pressScale
import com.appprint.studio.ui.platform.PlatformBackHandler
import com.appprint.studio.ui.screens.StudioScreen
import com.appprint.studio.ui.strings.Str
import com.appprint.studio.ui.theme.Motion
import com.appprint.studio.ui.theme.TabatiTheme
import kotlinx.coroutines.delay

/**
 * «طبعتي — استوديو الطباعة الذكي 1:1» root:
 * edge-to-edge screens + floating chrome (Reem Kufi wordmark top bar, nav pill/rail),
 * Navigation-free section switching per the approved HTML design.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(viewModel: TabatiViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TabatiTheme(theme = state.theme) {
        val pickImages = com.appprint.studio.core.platform.rememberImagePickerLauncher(maxItems = 100) { uris ->
            viewModel.importImages(uris)
        }
        val onCamera = com.appprint.studio.core.platform.rememberCameraCaptureLauncher { path ->
            path?.let(viewModel::importCapture)
        }
        val replaceRequest = remember { mutableStateOf<Long?>(null) }
        val replaceLauncher = com.appprint.studio.core.platform.rememberImagePickerLauncher(maxItems = 1) { uris ->
            val id = replaceRequest.value
            replaceRequest.value = null
            if (id != null && uris.isNotEmpty()) viewModel.replaceCardImage(id, uris.first())
        }
        val onReplaceImage: (Long) -> Unit = { id ->
            replaceRequest.value = id
            replaceLauncher()
        }
        // (notification permission reserved for export toast on Android 13+ — used by the print path)

        var shareSheetOpen by remember { mutableStateOf(false) }
        var importMenuOpen by remember { mutableStateOf(false) }

        // edge-to-edge: status + navigation bar icons always contrast the current surface
        val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
        val barDark = when (state.theme) {
            AppTheme.DARK -> true
            AppTheme.LIGHT -> false
            AppTheme.SYSTEM -> systemDark
        }
        LaunchedEffect(barDark) {
            com.appprint.studio.core.platform.PlatformUi.syncSystemBars(barDark)
        }

        // one-shot toast → global snackbar, auto-expires
        val toast = state.toast
        LaunchedEffect(toast?.message) {
            if (toast != null) {
                delay(3200)
                viewModel.consumeToast()
            }
        }

        PlatformBackHandler(enabled = state.section != StudioSection.SHEET) {
            viewModel.setSection(StudioSection.SHEET)
        }

        Surface(
            color = androidx.compose.ui.graphics.Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (barDark) com.appprint.studio.ui.theme.TabatiCanvasDark
                        else com.appprint.studio.ui.theme.TabatiCanvasLight,
                    ),
            ) {
        StudioScreen(
                    viewModel = viewModel,
                    onImportMenu = { importMenuOpen = true },
                    onReplaceImage = onReplaceImage,
                    onPickGallery = pickImages,
                    onCaptureCamera = onCamera,
                )
                FloatingTopBar(
                    state = state,
                    onBackToSheet = { viewModel.setSection(StudioSection.SHEET) },
                    onCycleTheme = viewModel::cycleTheme,
                    onShare = { shareSheetOpen = true },
                    onExportPdf = viewModel::downloadPdf,
                    onZoomIn = { viewModel.zoomStep(+15f) },
                    onZoomOut = { viewModel.zoomStep(-15f) },
                    onZoomFit = viewModel::zoomFit,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
                val showRail = Chrome.railVisible()
                if (!showRail) {
                    FloatingNavPill(
                        current = state.section,
                        onSelect = viewModel::setSection,
                        onAdd = { importMenuOpen = true },
                        cardsCount = state.cards.size,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                } else {
                    FloatingRail(
                        current = state.section,
                        onSelect = viewModel::setSection,
                        onAdd = { importMenuOpen = true },
                        cardsCount = state.cards.size,
                        modifier = Modifier.align(Alignment.CenterStart),
                    )
                }
                // toast at the top, under the floating bar
                toast?.let { t ->
                    AppSnackbar(
                        message = t.message,
                        busy = state.processing,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = Chrome.topReserve()),
                    )
                }
            }
        }

        // ---------------------------------------------------- import menu sheet
        if (importMenuOpen) {
            com.appprint.studio.ui.components.ImportMenuSheet(
                onPickFromGallery = pickImages,
                onCapturePhoto = onCamera,
                onAddDemoCards = viewModel::addSamples,
                onDismiss = { importMenuOpen = false },
            )
        }

        // ---------------------------------------------------- share dialog
        if (shareSheetOpen) {
            com.appprint.studio.ui.components.ShareSheet(
                pageCount = state.pageCount,
                currentPage = state.activePage,
                onDirectPrint = viewModel::printPdf,
                onSavePngCurrent = { viewModel.downloadPng(state.activePage) },
                onSaveAllPngs = viewModel::downloadAllPngs,
                onSaveHtml = viewModel::downloadHtml,
                onSharePdf = viewModel::sharePdf,
                onDismiss = { shareSheetOpen = false },
            )
        }
    }
}

// ---------------------------------------------------------- top bar

@Composable
private fun FloatingTopBar(
    state: com.appprint.studio.ui.TabatiUiState,
    onBackToSheet: () -> Unit,
    onCycleTheme: () -> Unit,
    onShare: () -> Unit,
    onExportPdf: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onZoomFit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val isEditing = state.section != StudioSection.SHEET
    val barHeight = Chrome.topBarHeight()
    val actionTouch = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .widthIn(max = 1024.dp)
            .padding(start = Chrome.EdgeInset, top = statusTopSafe() + Chrome.TopBarGapDp, end = Chrome.EdgeInset)
            .height(barHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (isEditing) {
            CircleActionButton(
                onClick = onBackToSheet,
                contentDescription = Str.back_to_sheet,
                size = 40.dp,
                container = scheme.secondaryContainer,
                tint = scheme.onSecondaryContainer,
            ) {
                Icon(Icons.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(19.dp))
            }
        }

        // wordmark capsule / zoom controls — crossfade per section
        if (!isEditing) {
            Row(
                modifier = Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(scheme.surfaceContainerLow.copy(alpha = 0.9f))
                    .border(1.dp, scheme.outlineVariant, RoundedCornerShape(percent = 50))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                com.appprint.studio.ui.components.TabatiLogoMark(size = 22.dp)
                Text(
                    text = "طبعتي",
                    fontFamily = com.appprint.studio.ui.theme.rememberReemKufiFamily(),
                    fontWeight = FontWeight.Bold,
                    fontSize = androidx.compose.ui.unit.TextUnit(17f, androidx.compose.ui.unit.TextUnitType.Sp),
                    letterSpacing = androidx.compose.ui.unit.TextUnit(0.4f, androidx.compose.ui.unit.TextUnitType.Sp),
                    color = scheme.onSurface,
                    maxLines = 1,
                )
                SurfaceChip(
                    text = "${state.settings.presetKey} (${state.cards.size})",
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CircleActionButton(onClick = onZoomOut, contentDescription = Str.zoom_out, size = 40.dp, container = scheme.surfaceContainerLow, tint = scheme.onSurface) {
                    Icon(Icons.Filled.ZoomOut, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                CircleActionButton(
                    onClick = onZoomFit,
                    contentDescription = Str.zoom_fit,
                    size = 40.dp,
                    container = if (state.zoomMode == com.appprint.studio.ui.ZoomMode.FIT) scheme.primary else scheme.surfaceContainerLow,
                    tint = if (state.zoomMode == com.appprint.studio.ui.ZoomMode.FIT) scheme.onPrimary else scheme.onSurface,
                ) {
                    Icon(Icons.Filled.FitScreen, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                CircleActionButton(onClick = onZoomIn, contentDescription = Str.zoom_in, size = 40.dp, container = scheme.surfaceContainerLow, tint = scheme.onSurface) {
                    Icon(Icons.Filled.ZoomIn, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // theme cycle — تلقائي / فاتح / داكن
        CircleActionButton(
            onClick = onCycleTheme,
            contentDescription = themeDescription(state.theme),
            size = 40.dp,
            container = scheme.surfaceContainerLow,
            tint = scheme.onSurface,
        ) {
            Icon(
                if (state.theme == AppTheme.DARK) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        }
        // share — glass circle
        CircleActionButton(
            onClick = onShare,
            contentDescription = Str.share,
            size = 40.dp,
            container = scheme.secondaryContainer,
            tint = scheme.onSecondaryContainer,
        ) {
            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        // export PDF — the one primary action
        CircleActionButton(
            onClick = onExportPdf,
            contentDescription = Str.export_pdf,
            size = 40.dp,
            container = scheme.primary,
            tint = scheme.onPrimary,
            elevated = true,
        ) {
            Icon(
                Icons.Filled.PictureAsPdf,
                contentDescription = null,
                tint = androidx.compose.ui.graphics.Color.White.copy(alpha = if (state.processing) 0.35f else 1f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun statusTopSafe(): androidx.compose.ui.unit.Dp =
    androidx.compose.foundation.layout.WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

private fun themeDescription(theme: AppTheme): String = when (theme) {
    AppTheme.SYSTEM -> "المظهر: ${Str.theme_auto}"
    AppTheme.LIGHT -> "المظهر: ${Str.theme_light}"
    AppTheme.DARK -> "المظهر: ${Str.theme_dark}"
}

@Composable
private fun SurfaceChip(text: String) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .height(22.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(scheme.secondaryContainer)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 10.sp,
            color = scheme.onSecondaryContainer,
            maxLines = 1,
        )
    }
}

/** Glass action circle — 40/44dp, elevated optional primary. */
@Composable
private fun CircleActionButton(
    onClick: () -> Unit,
    contentDescription: String,
    size: androidx.compose.ui.unit.Dp,
    container: androidx.compose.ui.graphics.Color,
    tint: androidx.compose.ui.graphics.Color,
    elevated: Boolean = false,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(size)
            .then(
                if (elevated) Modifier.shadow(10.dp, RoundedCornerShape(percent = 50), clip = false) else Modifier,
            )
            .clip(RoundedCornerShape(percent = 50))
            .background(container.copy(alpha = if (container == MaterialTheme.colorScheme.primary) 1f else 0.9f))
            .border(
                if (container == MaterialTheme.colorScheme.primary) 0.dp else 1.dp,
                if (container == MaterialTheme.colorScheme.primary) androidx.compose.ui.graphics.Color.Transparent else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(percent = 50),
            )
            .pressScale(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides tint) {
            content()
        }
    }
}

// ---------------------------------------------------------- destinations

private data class Destination(
    val section: StudioSection,
    val label: String,
    val icon: ImageVector,
)

private val DESTINATIONS = listOf(
    Destination(StudioSection.SHEET, Str.section_sheet, Icons.Filled.Layers),
    Destination(StudioSection.PAPER, Str.section_paper, Icons.Filled.Description),
    Destination(StudioSection.CARDS, Str.section_cards, Icons.Filled.Dashboard),
    Destination(StudioSection.ENHANCE, Str.section_enhance, Icons.Filled.AutoFixHigh),
    Destination(StudioSection.BORDERS, Str.section_borders, Icons.Filled.BorderOuter),
    Destination(StudioSection.GUIDE, Str.section_guide, Icons.Filled.Info),
)

/** Floating pill (compact) — standalone + circle beside the pill, badge on البطاقات. */
@Composable
private fun FloatingNavPill(
    current: StudioSection,
    onSelect: (StudioSection) -> Unit,
    onAdd: () -> Unit,
    cardsCount: Int,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val navBottom = WindowInsets.navigationBars
        .union(WindowInsets.displayCutout)
        .asPaddingValues()
        .calculateBottomPadding() + Chrome.EdgeInset
    Row(
        modifier = modifier.fillMaxWidth().padding(
            start = Chrome.EdgeInset + cutoutStartPadding(),
            top = 0.dp,
            end = Chrome.EdgeInset,
            bottom = navBottom,
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
    ) {
        // standalone + — outside the pill, the one accented control
        val interaction = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .size(48.dp)
                .shadow(12.dp, RoundedCornerShape(percent = 50), clip = false)
                .clip(RoundedCornerShape(percent = 50))
                .background(scheme.primary)
                .pressScale(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onAdd)
                .semantics { role = Role.Button },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Add, contentDescription = Str.add_images, tint = scheme.onPrimary, modifier = Modifier.size(24.dp))
        }
        Surface(
            shape = RoundedCornerShape(percent = 50),
            color = scheme.surfaceContainerLow.copy(alpha = 0.9f),
            shadowElevation = 10.dp,
            tonalElevation = 2.dp,
            modifier = Modifier
                .height(Chrome.NavBarDp)
                .widthIn(max = 512.dp)
                .border(1.dp, scheme.outlineVariant, RoundedCornerShape(percent = 50)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
            ) {
                DESTINATIONS.forEach { destination ->
                    NavPillItem(
                        destination = destination,
                        selected = destination.section == current,
                        badge = if (destination.section == StudioSection.CARDS && cardsCount > 0) cardsCount.toString() else null,
                        onSelect = { onSelect(destination.section) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NavPillItem(
    destination: Destination,
    selected: Boolean,
    badge: String?,
    onSelect: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val contentColor by animateColorAsState(
        targetValue = if (selected) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
        animationSpec = tween(180),
        label = "pillContentColor",
    )
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .pressScale(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onSelect)
            .semantics { role = Role.Tab }
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .widthIn(min = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(
            modifier = Modifier
                .height(24.dp)
                .width(44.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(if (selected) scheme.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                destination.icon,
                contentDescription = destination.label,
                tint = contentColor,
                modifier = Modifier.size(17.dp),
            )
            if (badge != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 8.dp, y = (-6).dp)
                        .size(14.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(scheme.tertiary)
                        .padding(horizontal = 3.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(badge, style = MaterialTheme.typography.labelSmall, color = scheme.onTertiary, fontSize = 8.5.sp, maxLines = 1)
                }
            }
        }
        Text(
            text = destination.label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold),
            color = contentColor,
            maxLines = 1,
        )
    }
}

// ---------------------------------------------------------- destinations
@Composable
private fun FloatingRail(
    current: StudioSection,
    onSelect: (StudioSection) -> Unit,
    onAdd: () -> Unit,
    cardsCount: Int,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val railWidth = Chrome.railWidth()
    val windowInfo = androidx.compose.ui.platform.LocalWindowInfo.current
    val maxHeightDp = with(androidx.compose.ui.platform.LocalDensity.current) { windowInfo.containerSize.height.toDp() } - 12.dp
    Box(
        modifier = modifier
            .width(railWidth)
            .heightInRail(maxHeightDp)
            .padding(start = Chrome.EdgeInset + cutoutStartPadding(), top = 0.dp, end = 0.dp, bottom = 0.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = scheme.surfaceContainerLow.copy(alpha = 0.9f),
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, scheme.outlineVariant, RoundedCornerShape(32.dp)),
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 2.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // add card/image — the one accented action
                val interaction = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(6.dp, RoundedCornerShape(percent = 50), clip = false)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(scheme.primary)
                        .pressScale(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onAdd)
                        .semantics { role = Role.Button },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = Str.add_images, tint = scheme.onPrimary, modifier = Modifier.size(24.dp))
                }
                DividerHair()
                DESTINATIONS.forEach { destination ->
                    RailItem(
                        destination = destination,
                        selected = destination.section == current,
                        badge = if (destination.section == StudioSection.CARDS && cardsCount > 0) cardsCount.toString() else null,
                        onSelect = { onSelect(destination.section) },
                    )
                }
            }
        }
    }
}

private fun Modifier.heightInRail(max: androidx.compose.ui.unit.Dp): Modifier =
    this.heightIn(max = max)

@Composable
private fun cutoutStartPadding(): androidx.compose.ui.unit.Dp =
    androidx.compose.foundation.layout.WindowInsets.displayCutout.asPaddingValues()
        .calculateStartPadding(LocalLayoutDirection.current)

@Composable
private fun DividerHair() {
    Box(
        modifier = Modifier
            .height(1.dp)
            .width(28.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

@Composable
private fun RailItem(
    destination: Destination,
    selected: Boolean,
    badge: String?,
    onSelect: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val contentColor by animateColorAsState(
        targetValue = if (selected) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
        animationSpec = tween(180),
        label = "railColor",
    )
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .pressScale(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onSelect)
            .semantics { role = Role.Tab }
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .widthIn(min = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(if (selected) scheme.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                destination.icon,
                contentDescription = destination.label,
                tint = contentColor,
                modifier = Modifier.size(19.dp),
            )
            if (badge != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 10.dp, y = (-6).dp)
                        .size(14.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(scheme.tertiary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(badge, color = scheme.onTertiary, fontSize = 8.5.sp, maxLines = 1)
                }
            }
        }
        Text(
            text = destination.label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.ExtraBold),
            color = contentColor,
            maxLines = 1,
        )
    }
}
