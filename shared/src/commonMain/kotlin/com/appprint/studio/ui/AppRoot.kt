package com.appprint.studio.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.AutoMode
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appprint.studio.core.model.AppTheme
import com.appprint.studio.ui.components.AppSnackbar
import com.appprint.studio.ui.components.Chrome
import com.appprint.studio.ui.components.ShareSheet
import com.appprint.studio.ui.components.overlayVerticalScrollbar
import com.appprint.studio.ui.components.pressScale
import com.appprint.studio.ui.platform.PlatformBackHandler
import com.appprint.studio.ui.screens.StudioScreen
import com.appprint.studio.ui.strings.Str
import com.appprint.studio.ui.theme.Motion
import com.appprint.studio.ui.theme.TabatiTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * «طبعتي — استوديو الطباعة الذكي 1:1» root:
 * edge-to-edge screens + floating chrome (Reem Kufi wordmark top bar, nav pill/rail),
 * Navigation-free section switching per the approved HTML design.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(
    viewModel: TabatiViewModel = remember { TabatiViewModel() },
    // AdMob banner slot (Android-only implementation supplied by the launcher activity).
    adBanner: @Composable (Modifier) -> Unit = {},
    // Shows a full-screen interstitial, then runs the action when the ad is closed.
    showInterstitial: ((onClosed: () -> Unit) -> Unit)? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Persist the full session (settings, cards, placement, zoom) after any change — the
    // debounce keeps DataStore writes cheap while guaranteeing a sudden exit restores it.
    LaunchedEffect(
        state.settings, state.cards, state.activePage, state.section, state.theme,
        state.zoomMode, state.manualZoomPercent, state.selectedCardId, state.forcedPages,
        state.enhanceScope, state.compareBefore,
    ) {
        delay(400)
        viewModel.persistState()
    }
    TabatiTheme(theme = state.theme) {
        val pickImages = com.appprint.studio.core.platform.rememberImagePickerLauncher(maxItems = 100) { uris ->
            viewModel.importImages(uris)
        }
        val onCamera = com.appprint.studio.core.platform.rememberCameraCaptureLauncher { path ->
            if (path != null) viewModel.importCapture(path) else viewModel.toastCameraUnavailable()
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
        // Export-completion notifications: ask for POST_NOTIFICATIONS every time an export
        // starts while it is still denied (Android 13+). The system dialog therefore appears
        // on each download, exactly as required; when granted, the notification opens the file.
        val notificationPermissionRequester = com.appprint.studio.core.platform.rememberNotificationPermissionRequester()

        var shareSheetOpen by remember { mutableStateOf(false) }
        var importMenuOpen by remember { mutableStateOf(false) }
        // Dialog/sheet state lives here so one Back handler can close them in strict order.
        var deletePageIdx by remember { mutableStateOf<Int?>(null) }
        var swapFromId by remember { mutableStateOf<Long?>(null) }
        var clearAllOpen by remember { mutableStateOf(false) }

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

        // The platform splash is kept until content is laid out — no artificial wait or blank.

        // one-shot toast → global snackbar, auto-expires; the snackbar itself animates in/out
        // fluidly (`.m3-float-enter` — scale/lift spring) via its `visible` state, like the
        // design's M3 Expressive Island motion.
        val toast = state.toast
        var toastVisible by remember { mutableStateOf(false) }
        var toastMsg by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(toast) {
            if (toast == null) {
                toastVisible = false
                toastMsg = null
                return@LaunchedEffect
            }
            toastMsg = toast.message
            toastVisible = true
            delay(3200)
            viewModel.consumeToast()
        }

        Surface(
            color = androidx.compose.ui.graphics.Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // Adaptive backdrop from the active Material 3 scheme.
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest),
            ) {
                // interstitial helper — the action always runs exactly once: after the ad closes
                // (or immediately when no interstitial is wired, e.g. desktop/iOS/tests).
                val withInterstitial: (() -> Unit) -> Unit = { action ->
                    val show = showInterstitial
                    if (show != null) show(action) else action()
                }

                StudioScreen(
                    viewModel = viewModel,
                    onImportMenu = { importMenuOpen = true },
                    onReplaceImage = onReplaceImage,
                    onPickGallery = pickImages,
                    onCaptureCamera = onCamera,
                    deletePageIdx = deletePageIdx,
                    onDeletePageIdxChange = { deletePageIdx = it },
                    swapFromId = swapFromId,
                    onSwapFromIdChange = { swapFromId = it },
                    clearAllOpen = clearAllOpen,
                    onClearAllOpenChange = { clearAllOpen = it },
                    adBanner = adBanner,
                )
                // No status-bar scrim: the system bars are transparent and show the same
                // canvas background; content is offset with insets instead of a color band.
                FloatingTopBar(
                    state = state,
                    onBackToSheet = { viewModel.setSection(StudioSection.SHEET) },
                    onCycleTheme = viewModel::cycleTheme,
                    onShare = { withInterstitial { shareSheetOpen = true } },
                    onExportPdf = {
                        withInterstitial {
                            notificationPermissionRequester()
                            viewModel.downloadPdf()
                        }
                    },
                    onZoomIn = { viewModel.zoomStep(+15f) },
                    onZoomOut = { viewModel.zoomStep(-15f) },
                    onZoomFit = viewModel::zoomFit,
                    onZoomToggle = {
                        if (state.zoomMode == com.appprint.studio.ui.ZoomMode.FIT) {
                            viewModel.setZoomMode(com.appprint.studio.ui.ZoomMode.MANUAL)
                            viewModel.setManualZoom(130f)
                        } else {
                            viewModel.zoomFit()
                        }
                    },
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
                    // The add action is a standalone floating circle, isolated from the rail
                    // capsule on phones, tablets, foldables and split-screen alike.
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(
                                start = Chrome.EdgeInset + cutoutStartPadding(),
                                top = Chrome.topReserve(),
                                bottom = 16.dp,
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CircleActionButton(
                            onClick = { importMenuOpen = true },
                            contentDescription = Str.add_images,
                            size = 48.dp,
                            container = MaterialTheme.colorScheme.primary,
                            tint = MaterialTheme.colorScheme.onPrimary,
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(24.dp))
                        }
                        FloatingRail(
                            current = state.section,
                            onSelect = viewModel::setSection,
                            cardsCount = state.cards.size,
                        )
                    }
                }
                // toast at the top, under the floating bar (design: fixed top-16 = 64dp + safe-top)
                if (toastMsg != null && toastVisible) {
                    AppSnackbar(
                        message = toastMsg ?: "",
                        busy = state.processing,
                        visible = toastVisible,
                        onDismiss = viewModel::consumeToast,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .padding(
                                start = 16.dp,
                                top = statusTopSafe() + 64.dp,
                                end = if (showRail) Chrome.railWidth() + 24.dp else 0.dp,
                            ),
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
                hasCards = state.cards.isNotEmpty(),
            )
        }

        // ---------------------------------------------------- share dialog
        if (shareSheetOpen) {
            com.appprint.studio.ui.components.ShareSheet(
                pageCount = state.pageCount,
                currentPage = state.activePage,
                onDirectPrint = { notificationPermissionRequester(); viewModel.printPdf() },
                onSavePngCurrent = { notificationPermissionRequester(); viewModel.downloadPng(state.activePage) },
                onSaveAllPngs = { notificationPermissionRequester(); viewModel.downloadAllPngs() },
                onSaveHtml = { notificationPermissionRequester(); viewModel.downloadHtml() },
                onSharePdf = viewModel::sharePdf,
                onSharePreview = { notificationPermissionRequester(); viewModel.sharePreview() },
                onDismiss = { shareSheetOpen = false },
            )
        }

        // Single, LAST-registered Back handler: it wins over the sheets' own handlers and
        // dismisses strictly topmost-first. Without this, ModalBottomSheet could close
        // visually while our flag stayed true, so the next Back exited the app.
        val anyOverlayOpen = importMenuOpen || shareSheetOpen || deletePageIdx != null ||
            swapFromId != null || clearAllOpen || state.longPressSheetCardId != null ||
            state.section != StudioSection.SHEET || state.selectedCardId != null
        PlatformBackHandler(enabled = anyOverlayOpen) {
            when {
                importMenuOpen -> importMenuOpen = false
                shareSheetOpen -> shareSheetOpen = false
                deletePageIdx != null -> deletePageIdx = null
                swapFromId != null -> swapFromId = null
                clearAllOpen -> clearAllOpen = false
                state.longPressSheetCardId != null -> viewModel.dismissLongPressSheet()
                state.section != StudioSection.SHEET -> viewModel.setSection(StudioSection.SHEET)
                state.selectedCardId != null -> viewModel.selectCard(null)
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
    onZoomToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val isEditing = state.section != StudioSection.SHEET
    val barHeight = Chrome.topBarHeight()
    val actionTouch = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .widthIn(max = 1024.dp)
            .padding(start = Chrome.EdgeInset + cutoutStartPadding(), top = statusTopSafe() + Chrome.TopBarGapDp, end = Chrome.EdgeInset)
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(19.dp))
            }
        }

        // M3 Expressive: the wordmark morphs fluidly into the zoom cluster and back,
        // spring-based shared transition instead of a hard swap.
        AnimatedContent(
            targetState = isEditing,
            transitionSpec = {
                (fadeIn(animationSpec = Motion.effects()) + scaleIn(initialScale = 0.86f, animationSpec = Motion.default())) togetherWith
                    (fadeOut(animationSpec = Motion.fast()) + scaleOut(targetScale = 0.86f, animationSpec = Motion.fast()))
            },
            label = "wordmark-zoom-morph",
        ) { editing ->
            if (!editing) {
                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .shadow(18.dp, RoundedCornerShape(percent = 50), clip = false, ambientColor = androidx.compose.ui.graphics.Color(0x38000000), spotColor = androidx.compose.ui.graphics.Color(0x38000000))
                        .shadow(5.dp, RoundedCornerShape(percent = 50), clip = false, ambientColor = androidx.compose.ui.graphics.Color(0x1C000000), spotColor = androidx.compose.ui.graphics.Color(0x1C000000))
                        .clip(RoundedCornerShape(percent = 50))
                        .background(androidx.compose.ui.graphics.Color.Transparent)
                        .border(1.dp, scheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(percent = 50))
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
                    SurfaceChip(text = "${state.settings.presetKey} (${state.cards.size})")
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
        }

        Spacer(Modifier.weight(1f))

        // design: fit↔130% zoom toggle — glass circle, FIRST in the left action group (sheet tab only)
        if (!isEditing) {
            CircleActionButton(
                onClick = onZoomToggle,
                contentDescription = if (state.zoomMode == com.appprint.studio.ui.ZoomMode.FIT) Str.zoom_in else Str.zoom_fit,
                size = 40.dp,
                container = scheme.surfaceContainerLow,
                tint = scheme.onSurface,
            ) {
                Icon(
                    if (state.zoomMode == com.appprint.studio.ui.ZoomMode.FIT) Icons.Filled.ZoomIn else Icons.Filled.FitScreen,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        // theme cycle — تلقائي / فاتح / داكن (dedicated M3 Expressive «Auto» icon per state)
        val themeIcon = when (state.theme) {
            AppTheme.SYSTEM -> Icons.Filled.AutoMode
            AppTheme.DARK -> Icons.Filled.LightMode
            AppTheme.LIGHT -> Icons.Filled.DarkMode
        }
        CircleActionButton(
            onClick = onCycleTheme,
            contentDescription = themeDescription(state.theme),
            size = 40.dp,
            container = scheme.surfaceContainerLow,
            tint = scheme.onSurface,
        ) {
            Icon(
                themeIcon,
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
        // export PDF — primary action; icon stays brand-green on the transparent bar.
        CircleActionButton(
            onClick = onExportPdf,
            contentDescription = Str.export_pdf,
            size = 40.dp,
            container = scheme.primary,
            tint = scheme.primary,
            elevated = false,
        ) {
            Icon(
                Icons.Filled.PictureAsPdf,
                contentDescription = null,
                tint = scheme.onPrimary.copy(alpha = if (state.processing) 0.35f else 1f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun statusTopSafe(): androidx.compose.ui.unit.Dp =
    androidx.compose.foundation.layout.WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

/**
 * DESIGN glass pill: CSS `color-mix(surface-low 90%, transparent)` + `backdrop-filter: blur(22px)`
 * + white top sheen. Compose has no backdrop-filter, so we approximate with a translucent
 * vertical gradient (bright top → tinted bottom) + hairline border + the design's 2-layer shadow.
 */
private fun glassBrush(base: androidx.compose.ui.graphics.Color): Brush = Brush.verticalGradient(
    colors = listOf(
        base.copy(alpha = 0.16f),
        base.copy(alpha = 0.20f),
        base.copy(alpha = 0.24f),
    ),
)

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
                if (elevated) {
                    Modifier.shadow(10.dp, RoundedCornerShape(percent = 50), clip = false)
                } else {
                    // design: 0 12px 30px -6px rgba(0,0,0,.22) + 0 3px 10px -2px rgba(0,0,0,.11)
                    Modifier
                        .shadow(18.dp, RoundedCornerShape(percent = 50), clip = false, ambientColor = androidx.compose.ui.graphics.Color(0x38000000), spotColor = androidx.compose.ui.graphics.Color(0x38000000))
                        .shadow(5.dp, RoundedCornerShape(percent = 50), clip = false, ambientColor = androidx.compose.ui.graphics.Color(0x1C000000), spotColor = androidx.compose.ui.graphics.Color(0x1C000000))
                },
            )
            .clip(RoundedCornerShape(percent = 50))
            // Glass icons float on the bar; a primary container is a real selected/primary
            // state and must actually paint, otherwise its onPrimary icon disappears.
            .background(if (elevated || container == MaterialTheme.colorScheme.primary) container else androidx.compose.ui.graphics.Color.Transparent)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                RoundedCornerShape(percent = 50),
            )
            .pressScale(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .semantics { role = Role.Button; this.contentDescription = contentDescription },
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
    val density = androidx.compose.ui.platform.LocalDensity.current
    val navBottom = WindowInsets.navigationBars
        .union(WindowInsets.displayCutout)
        .asPaddingValues()
        .calculateBottomPadding() + Chrome.EdgeInset
    // Same fluid, measured spring indicator as the side rail — unified M3 Expressive motion.
    val indicatorX = remember { Animatable(0f) }
    val indicatorY = remember { Animatable(0f) }
    val indicatorW = remember { Animatable(0f) }
    val indicatorH = remember { Animatable(0f) }
    var indicatorBounds by remember { mutableStateOf<FloatArray?>(null) }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    LaunchedEffect(indicatorBounds, rtl) {
        indicatorBounds?.let { b ->
            val x = if (rtl) -b[0] else b[0]
            launch { indicatorX.animateTo(x, Motion.default()) }
            launch { indicatorY.animateTo(b[1], Motion.default()) }
            launch { indicatorW.animateTo(b[2], Motion.default()) }
            launch { indicatorH.animateTo(b[3], Motion.default()) }
        }
    }
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
            color = androidx.compose.ui.graphics.Color.Transparent,
            shadowElevation = 14.dp,
            tonalElevation = 0.dp,
            modifier = Modifier
                .weight(1f)
                .height(Chrome.NavBarDp)
                .widthIn(max = 512.dp)
                .background(androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(percent = 50))
                .border(1.dp, scheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(percent = 50)),
        ) {
            Box {
                Box(
                    modifier = Modifier
                        .width(with(density) { indicatorW.value.toDp() })
                        .height(with(density) { indicatorH.value.toDp() })
                        .offset { IntOffset(indicatorX.value.roundToInt(), indicatorY.value.roundToInt()) }
                        .clip(RoundedCornerShape(percent = 50))
                        .background(scheme.secondaryContainer),
                )
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterHorizontally),
                ) {
                    DESTINATIONS.forEach { destination ->
                        NavPillItem(
                            destination = destination,
                            selected = destination.section == current,
                            badge = if (destination.section == StudioSection.CARDS && cardsCount > 0) cardsCount.toString() else null,
                            onSelect = { onSelect(destination.section) },
                            showSelectedPill = false,
                            modifier = Modifier
                                .weight(1f)
                                .onGloballyPositioned { coords ->
                                    if (destination.section == current) {
                                        val p = coords.positionInParent()
                                        indicatorBounds = floatArrayOf(
                                            p.x, p.y,
                                            coords.size.width.toFloat(), coords.size.height.toFloat(),
                                        )
                                    }
                                },
                        )
                    }
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
    showSelectedPill: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val contentColor by animateColorAsState(
        targetValue = if (selected) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 460f),
        label = "pillContentColor",
    )
    Column(
        modifier = modifier
            .pressScale(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onSelect)
            .semantics { role = Role.Tab }
            .padding(vertical = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp)
                .background(if (selected && showSelectedPill) scheme.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(percent = 50)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                destination.icon,
                contentDescription = destination.label,
                tint = contentColor,
                modifier = Modifier.size(17.dp),
            )
            if (badge != null) {
                // HTML: min-w-16px px-1 — grows with the count and is never clipped.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-1).dp)
                        .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(scheme.tertiary)
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onTertiary,
                        fontSize = 8.5.sp,
                        lineHeight = 9.sp,
                        maxLines = 1,
                    )
                }
            }
        }
        Text(
            text = destination.label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 10.sp),
            color = contentColor,
            maxLines = 2,
            textAlign = TextAlign.Center,
        )
    }
}

// ---------------------------------------------------------- destinations
@Composable
private fun FloatingRail(
    current: StudioSection,
    onSelect: (StudioSection) -> Unit,
    cardsCount: Int,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val density = androidx.compose.ui.platform.LocalDensity.current
    val windowInfo = androidx.compose.ui.platform.LocalWindowInfo.current
    val safeMaxH = with(density) { windowInfo.containerSize.height.toDp() } - Chrome.topReserve() - 16.dp
    // One shared, physics-driven indicator. Its bounds are MEASURED from the destination,
    // so it fits the whole icon+label in both vertical and horizontal rail orientations.
    val indicatorX = remember { Animatable(0f) }
    val indicatorY = remember { Animatable(0f) }
    val indicatorW = remember { Animatable(0f) }
    val indicatorH = remember { Animatable(0f) }
    var indicatorBounds by remember { mutableStateOf<FloatArray?>(null) }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    LaunchedEffect(indicatorBounds, rtl) {
        indicatorBounds?.let { b ->
            // Modifier.offset { IntOffset } is RTL-aware: negate x in Arabic so the
            // capsule travels toward the destination the user actually taps.
            val x = if (rtl) -b[0] else b[0]
            launch { indicatorX.animateTo(x, Motion.default()) }
            launch { indicatorY.animateTo(b[1], Motion.default()) }
            launch { indicatorW.animateTo(b[2], Motion.default()) }
            launch { indicatorH.animateTo(b[3], Motion.default()) }
        }
    }
    Box(
        modifier = modifier
            .widthIn(min = Chrome.railWidth(), max = Chrome.railWidth())
            .heightInRail(safeMaxH.coerceAtLeast(240.dp)),
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = androidx.compose.ui.graphics.Color.Transparent,
            shadowElevation = 14.dp,
            modifier = Modifier
                .fillMaxWidth()
                // Solid surface: no white paper can shine through as a straight line.
                .background(scheme.surfaceContainerLow, RoundedCornerShape(32.dp))
                .border(1.dp, scheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(32.dp)),
        ) {
            Box {
                // Liquid M3 Expressive selection: a single spring-animated capsule.
                Box(
                    modifier = Modifier
                        .width(with(density) { indicatorW.value.toDp() })
                        .height(with(density) { indicatorH.value.toDp() })
                        .offset {
                            IntOffset(indicatorX.value.roundToInt(), indicatorY.value.roundToInt())
                        }
                        .clip(RoundedCornerShape(20.dp))
                        .background(scheme.secondaryContainer),
                )
                val railScroll = rememberScrollState()
                Column(
                    modifier = Modifier
                        .verticalScroll(railScroll),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    DESTINATIONS.forEach { destination ->
                        RailItem(
                            destination = destination,
                            selected = destination.section == current,
                            badge = if (destination.section == StudioSection.CARDS && cardsCount > 0) cardsCount.toString() else null,
                            onSelect = { onSelect(destination.section) },
                            showSelectedPill = false,
                            modifier = Modifier.onGloballyPositioned { coords ->
                                if (destination.section == current) {
                                    val p = coords.positionInParent()
                                    indicatorBounds = floatArrayOf(
                                        p.x, p.y,
                                        coords.size.width.toFloat(), coords.size.height.toFloat(),
                                    )
                                }
                            },
                        )
                    }
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
private fun RailItem(
    destination: Destination,
    selected: Boolean,
    badge: String?,
    onSelect: () -> Unit,
    showSelectedPill: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val contentColor by animateColorAsState(
        targetValue = if (selected) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 460f),
        label = "railColor",
    )
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .pressScale(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onSelect)
            .semantics { role = Role.Tab }
            .padding(vertical = 1.dp)
            .widthIn(min = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(if (selected && showSelectedPill) scheme.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent),
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
                        .offset(x = 1.dp, y = (-2).dp)
                        .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(scheme.tertiary)
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        badge,
                        color = scheme.onTertiary,
                        fontSize = 8.5.sp,
                        lineHeight = 9.sp,
                        maxLines = 1,
                    )
                }
            }
        }
        Text(
            text = destination.label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.ExtraBold),
            color = contentColor,
            textAlign = TextAlign.Center,
            // Arabic rail labels are longer than Latin ones — allow two wrapped lines
            // instead of clipping them at the fixed capsule width.
            maxLines = 2,
            modifier = Modifier.width(68.dp),
        )
    }
}
