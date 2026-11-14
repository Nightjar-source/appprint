package com.appprint.studio.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
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
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
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
        val requestNotifications = com.appprint.studio.core.platform.rememberNotificationPermissionRequester()

        var shareSheetOpen by remember { mutableStateOf(false) }

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
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceContainerLow,
                                MaterialTheme.colorScheme.background,
                            ),
                        ),
                    ),
            ) {
                StudioScreen(
                    viewModel = viewModel,
                    onAddImages = pickImages,
                    onCamera = onCamera,
                    onReplaceImage = onReplaceImage,
                )
                FloatingTopBar(
                    state = state,
                    onBackToSheet = { viewModel.setSection(StudioSection.SHEET) },
                    onCycleTheme = viewModel::cycleTheme,
                    onShare = { shareSheetOpen = true },
                    onExportPdf = { requestNotifications(); viewModel.exportPdf() },
                    onZoomIn = { viewModel.zoomStep(+15f) },
                    onZoomOut = { viewModel.zoomStep(-15f) },
                    onZoomFit = viewModel::zoomFit,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
                val isCompact = Chrome.isCompact()
                if (isCompact) {
                    FloatingNavPill(
                        current = state.section,
                        onSelect = viewModel::setSection,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                } else {
                    FloatingRail(
                        current = state.section,
                        onSelect = viewModel::setSection,
                        onAddSheet = viewModel::addSheet,
                        modifier = Modifier.align(Alignment.CenterStart),
                    )
                }
                // toast at the top, under the floating bar
                toast?.let { t ->
                    AppSnackbar(
                        message = t.message,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = Chrome.topReserve()),
                    )
                }
            }
        }

        // ---------------------------------------------------- share sheet
        if (shareSheetOpen) {
            ModalBottomSheet(
                onDismissRequest = { shareSheetOpen = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                ShareSheet(
                    onExportPdf = { requestNotifications(); viewModel.exportPdf() },
                    onExportPngCurrent = { viewModel.exportPng(state.activePage) },
                    onExportAllPngs = { repeat(state.pageCount) { page -> viewModel.exportPng(page) } },
                    onExportHtml = viewModel::exportHtml,
                    pageCount = state.pageCount,
                    onDismiss = { shareSheetOpen = false },
                )
            }
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
                Icon(Icons.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }

        // wordmark capsule / zoom controls — crossfade per section
        if (!isEditing) {
            Row(
                modifier = Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(scheme.surfaceContainerHigh.copy(alpha = 0.92f))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                com.appprint.studio.ui.components.TabatiLogoMark(size = 22.dp)
                Text(
                    text = "طبعتي",
                    fontFamily = com.appprint.studio.ui.theme.ReemKufiFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = androidx.compose.ui.unit.TextUnit(20f, androidx.compose.ui.unit.TextUnitType.Sp),
                    color = scheme.onSurface,
                )
                Spacer(Modifier.width(2.dp))
                SurfaceChip(
                    text = "${state.settings.presetKey} · ${state.cards.size}",
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
                CircleActionButton(onClick = onZoomOut, contentDescription = Str.zoom_out, size = 40.dp, container = scheme.surfaceContainerHigh, tint = scheme.onSurface) {
                    Icon(Icons.Filled.ZoomOut, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                CircleActionButton(onClick = onZoomFit, contentDescription = Str.zoom_fit, size = 40.dp, container = scheme.surfaceContainerHigh, tint = scheme.onSurface) {
                    Icon(Icons.Filled.FitScreen, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                CircleActionButton(onClick = onZoomIn, contentDescription = Str.zoom_in, size = 40.dp, container = scheme.surfaceContainerHigh, tint = scheme.onSurface) {
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
            container = scheme.surfaceContainerHigh,
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
            if (state.processing) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = scheme.onPrimary,
                )
            } else {
                Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
            }
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
            fontWeight = FontWeight.Black,
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
                if (elevated) Modifier.shadow(6.dp, RoundedCornerShape(percent = 50), clip = false) else Modifier,
            )
            .clip(RoundedCornerShape(percent = 50))
            .background(container.copy(alpha = if (container == MaterialTheme.colorScheme.primary) 1f else 0.92f))
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

/** Floating pill (compact) — 6 destinations, animated oval indicator. */
@Composable
private fun FloatingNavPill(
    current: StudioSection,
    onSelect: (StudioSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val bottomPad = androidx.compose.foundation.layout.WindowInsets.navigationBars
        .asPaddingValues().calculateBottomPadding() + 8.dp
    Box(
        modifier = modifier
            .padding(start = Chrome.EdgeInset, top = 0.dp, end = Chrome.EdgeInset, bottom = bottomPad)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(percent = 50),
            color = scheme.surfaceContainerHigh.copy(alpha = 0.92f),
            shadowElevation = 10.dp,
            tonalElevation = 2.dp,
            modifier = Modifier.height(Chrome.NavBarDp).widthIn(max = 440.dp),
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
    onSelect: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pillWidth by animateDpAsState(
        targetValue = if (selected) 46.dp else 20.dp,
        animationSpec = Motion.fastDp(),
        label = "pillIndicator",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
        animationSpec = tween(180),
        label = "pillContentColor",
    )
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .clickable(interactionSource = interaction, indication = null, onClick = onSelect)
            .semantics { role = Role.Tab }
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(
            modifier = Modifier
                .height(28.dp)
                .width(pillWidth)
                .clip(RoundedCornerShape(percent = 50))
                .background(if (selected) scheme.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                destination.icon,
                contentDescription = destination.label,
                tint = contentColor,
                modifier = Modifier.size(if (selected) 18.dp else 19.dp),
            )
        }
        Text(
            text = destination.label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp),
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
    onAddSheet: () -> Unit,
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
            shape = RoundedCornerShape(percent = 50),
            color = scheme.surfaceContainerHigh.copy(alpha = 0.92f),
            shadowElevation = 12.dp,
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 2.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                // add sheet — the one accented action
                val interaction = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(6.dp, RoundedCornerShape(percent = 50), clip = false)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(scheme.primary)
                        .clickable(interactionSource = interaction, indication = null, onClick = onAddSheet)
                        .semantics { role = Role.Button },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = Str.add_sheet, tint = scheme.onPrimary, modifier = Modifier.size(22.dp))
                }
                DividerHair()
                DESTINATIONS.forEach { destination ->
                    RailItem(
                        destination = destination,
                        selected = destination.section == current,
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
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
    )
}

@Composable
private fun RailItem(
    destination: Destination,
    selected: Boolean,
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
            .clip(RoundedCornerShape(percent = 50))
            .clickable(interactionSource = interaction, indication = null, onClick = onSelect)
            .semantics { role = Role.Tab }
            .padding(horizontal = 6.dp, vertical = 5.dp)
            .widthIn(min = 52.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Icon(
            destination.icon,
            contentDescription = destination.label,
            tint = contentColor,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = destination.label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
            color = contentColor,
            maxLines = 1,
        )
    }
}
