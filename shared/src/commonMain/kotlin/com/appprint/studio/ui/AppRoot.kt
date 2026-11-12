package com.appprint.studio.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.LocalIndication
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.appprint.studio.core.model.AppTheme
import com.appprint.studio.core.platform.rememberNotificationPermissionRequester
import com.appprint.studio.ui.components.AppSnackbarHost
import com.appprint.studio.ui.components.IraqiLogo
import com.appprint.studio.ui.components.pressScale
import com.appprint.studio.ui.components.Chrome
import com.appprint.studio.ui.screens.GuideScreen
import com.appprint.studio.ui.screens.SettingsScreen
import com.appprint.studio.ui.screens.StudioScreen
import com.appprint.studio.ui.theme.AppPrintTheme
import com.appprint.studio.ui.theme.Motion
import com.appprint.studio.ui.theme.contrastingTextColor
import com.appprint.studio.ui.strings.Str
import com.appprint.studio.ui.platform.PlatformBackHandler
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.window.core.layout.WindowSizeClass
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
sealed interface AppRoute : NavKey {
    @Serializable
    data object Studio : AppRoute

    @Serializable
    data object Guide : AppRoute

    @Serializable
    data object Settings : AppRoute
}

private data class TopLevelItem(
    val route: AppRoute,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private fun routeName(route: AppRoute): String = when (route) {
    AppRoute.Studio -> Str.nav_studio
    AppRoute.Guide -> Str.nav_guide
    AppRoute.Settings -> Str.nav_settings
}

/**
 * App root — fully floating chrome on edge-to-edge content, unified on the
 * Material 3 Expressive + Compose Adaptive (`currentWindowAdaptiveInfoV2`) + Navigation 3 trio.
 *  - Floating oval top bar (composite: brand + screen name + circular actions).
 *  - Floating bottom pill (compact) / floating vertical rail (medium+).
 *  - One unified export menu button + direct print button in the top bar.
 *  - System Back wired step-by-step through panels/dialogs/selection.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppRoot(viewModel: StudioViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AppPrintTheme(theme = state.theme) {
        val snackbarHostState = remember { SnackbarHostState() }
        val toast = state.toast
        LaunchedEffect(toast) {
            if (toast != null) {
                snackbarHostState.showSnackbar(toast.message)
                viewModel.consumeToast()
            }
        }

        val savedStateConfig = remember {
            SavedStateConfiguration {
                serializersModule = SerializersModule {
                    polymorphic(NavKey::class) {
                        subclass(AppRoute.Studio::class, AppRoute.Studio.serializer())
                        subclass(AppRoute.Guide::class, AppRoute.Guide.serializer())
                        subclass(AppRoute.Settings::class, AppRoute.Settings.serializer())
                    }
                }
            }
        }
        val backStack = rememberNavBackStack(savedStateConfig, AppRoute.Studio)
        val current = backStack.lastOrNull() as? AppRoute ?: AppRoute.Studio
        val requestNotifications = rememberNotificationPermissionRequester()

        val adaptiveInfo = currentWindowAdaptiveInfoV2()
        val sizeClass = adaptiveInfo.windowSizeClass
        val isCompact = !sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
        val isExpanded = sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

        // Official adaptive navigation widths: 64dp for medium/foldable,
        // 72dp for expanded tablets, Chromebooks and Samsung DeX.
        val railWidth = if (isExpanded) 72.dp else 64.dp
        val animatedRailWidth by animateDpAsState(
            targetValue = railWidth,
            animationSpec = Motion.effects(),
            label = "railWidth",
        )
        // Page content padding follows the rail width so content never hides underneath it.
        val pageContentPadding by animateDpAsState(
            targetValue = if (isCompact) 0.dp else animatedRailWidth + Chrome.EdgeInset,
            animationSpec = Motion.effects(),
            label = "pageContentPadding",
        )

        @Composable
        fun content() {
            NavDisplay(
                backStack = backStack,
                modifier = Modifier.fillMaxSize().padding(start = pageContentPadding),
                onBack = { backStack.removeLastOrNull(); Unit },
                entryProvider = entryProvider {
                    entry<AppRoute.Studio> { StudioScreen(viewModel = viewModel) }
                    entry<AppRoute.Guide> { GuideScreen() }
                    entry<AppRoute.Settings> { SettingsScreen(viewModel = viewModel) }
                },
            )
        }

        val items = listOf(
            TopLevelItem(AppRoute.Studio, Str.nav_studio, Icons.Filled.Print),
            TopLevelItem(AppRoute.Guide, Str.nav_guide, Icons.Filled.MenuBook),
            TopLevelItem(AppRoute.Settings, Str.nav_settings, Icons.Filled.Settings),
        )

        val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val cutoutStart = WindowInsets.displayCutout.asPaddingValues().calculateStartPadding(LocalLayoutDirection.current)

        PlatformBackHandler(enabled = current != AppRoute.Studio) {
            navigateTopLevel(backStack, AppRoute.Studio)
        }

        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceContainerLowest,
                            MaterialTheme.colorScheme.background,
                        ),
                    ),
                ),
        ) {
            // screens fill edge-to-edge, behind the floating chrome
            content()

            FloatingTopBar(
                title = routeName(current),
                onExportPdf = viewModel::sharePdf,
                onExportPng = { viewModel.sharePng(state.currentPage) },
                onExportHtml = viewModel::shareHtml,
                onShare = { viewModel.sharePng(state.currentPage) },
                onPrint = { requestNotifications(); viewModel.printPdf() },
                exporting = state.processing,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(start = Chrome.EdgeInset + cutoutStart, top = statusTop + Chrome.TopBarGap, end = Chrome.EdgeInset, bottom = 0.dp),
            )

            AnimatedVisibility(
                visible = isCompact,
                enter = fadeIn(Motion.effects()) + slideInVertically(Motion.spatial()) { it },
                exit = fadeOut(Motion.effectsFast()) + slideOutVertically(Motion.effectsFast()) { it },
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                FloatingNavPill(
                    items = items,
                    current = current,
                    onSelect = { route -> navigateTopLevel(backStack, route) },
                    modifier = Modifier.padding(
                        start = Chrome.EdgeInset + cutoutStart,
                        top = 0.dp,
                        end = Chrome.EdgeInset,
                        bottom = navBottom + Chrome.BottomBarGap,
                    ),
                )
            }

            AnimatedVisibility(
                visible = !isCompact,
                enter = fadeIn(Motion.effects()) + slideInHorizontally(Motion.spatial()) { -it / 3 },
                exit = fadeOut(Motion.effectsFast()) + slideOutHorizontally(Motion.effectsFast()) { -it / 3 },
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                FloatingRail(
                    items = items,
                    current = current,
                    onSelect = { route -> navigateTopLevel(backStack, route) },
                    railWidth = animatedRailWidth,
                    modifier = Modifier.padding(start = cutoutStart + Chrome.EdgeInset, top = 0.dp, end = 0.dp, bottom = 0.dp),
                )
            }

            AppSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = pageContentPadding / 2,
                        top = 0.dp,
                        end = pageContentPadding / 2,
                        bottom = if (isCompact) {
                            navBottom + Chrome.BottomBarGap + Chrome.BottomBarHeight + 10.dp
                        } else {
                            24.dp
                        },
                    ),
            )
        }
    }
}

private fun navigateTopLevel(backStack: MutableList<NavKey>, route: AppRoute) {
    if (backStack.lastOrNull() != route) {
        backStack.clear()
        backStack.add(route)
    }
}

// ---------------------------------------------------------------- floating top bar

@Composable
private fun FloatingTopBar(
    title: String,
    onExportPdf: () -> Unit,
    onExportPng: () -> Unit,
    onExportHtml: () -> Unit,
    onShare: () -> Unit,
    onPrint: () -> Unit,
    exporting: Boolean,
    modifier: Modifier = Modifier,
) {
    var exportMenuOpen by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    val container = scheme.surfaceContainerHigh.copy(alpha = 0.90f)
    val exportTurns by animateFloatAsState(
        targetValue = if (exportMenuOpen) 135f else 0f,
        animationSpec = Motion.effects(),
        label = "exportTurns",
    )

    Row(
        modifier = modifier
            .height(Chrome.TopBarHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // oval brand container
        Surface(
            shape = RoundedCornerShape(percent = 50),
            color = container,
            shadowElevation = 10.dp,
            modifier = Modifier.weight(1f).height(Chrome.TopBarHeight),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                IraqiLogo(size = 34.dp)
                Column(Modifier.weight(1f)) {
                    Text(
                        text = Str.app_eyebrow,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, letterSpacing = 1.2.sp),
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    AnimatedContent(
                        targetState = title,
                        transitionSpec = {
                            (fadeIn(Motion.effects()) + slideInVertically(Motion.spatialFast()) { it / 3 })
                                .togetherWith(
                                    fadeOut(Motion.effectsFast()) + slideOutVertically(Motion.effectsFast()) { -it / 3 },
                                )
                        },
                        label = "screenTitle",
                    ) { value ->
                        Text(
                            text = value,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = scheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        // unified export menu — circular button opening an elegant dropdown
        CircularIconButton(
            onClick = { exportMenuOpen = true },
            contentDescription = Str.export_menu,
            enabled = !exporting,
            container = container,
        ) {
            if (exporting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = scheme.onSurface,
                )
            } else {
                Icon(
                    Icons.Filled.FileUpload,
                    contentDescription = null,
                    tint = scheme.onSurface,
                    modifier = Modifier.size(21.dp).rotate(exportTurns),
                )
            }
        }
        DropdownMenu(
            expanded = exportMenuOpen,
            onDismissRequest = { exportMenuOpen = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = scheme.surfaceContainerHigh,
        ) {
            DropdownMenuItem(
                text = { Text(Str.btn_export_pdf, maxLines = 1) },
                onClick = { exportMenuOpen = false; onExportPdf() },
                leadingIcon = { Icon(Icons.Filled.PictureAsPdf, contentDescription = null) },
            )
            DropdownMenuItem(
                text = { Text(Str.btn_save_png, maxLines = 1) },
                onClick = { exportMenuOpen = false; onExportPng() },
                leadingIcon = { Icon(Icons.Filled.Image, contentDescription = null) },
            )
            DropdownMenuItem(
                text = { Text(Str.btn_export_html, maxLines = 1) },
                onClick = { exportMenuOpen = false; onExportHtml() },
                leadingIcon = { Icon(Icons.Filled.Code, contentDescription = null) },
            )
            DropdownMenuItem(
                text = { Text(Str.share_page, maxLines = 1) },
                onClick = { exportMenuOpen = false; onShare() },
                leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
            )
        }

        // direct print button — prints the current file immediately
        CircularIconButton(
            onClick = onPrint,
            contentDescription = Str.btn_print,
            enabled = !exporting,
            container = container,
        ) {
            Icon(Icons.Filled.Print, contentDescription = null, tint = scheme.onSurface, modifier = Modifier.size(21.dp))
        }
    }
}

@Composable
private fun CircularIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    enabled: Boolean,
    container: Color,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val indication = LocalIndication.current
    val containerColor by animateColorAsState(
        targetValue = if (enabled) container else container.copy(alpha = 0.5f),
        animationSpec = Motion.effects(),
        label = "circularContainer",
    )
    Box(
        modifier = Modifier
            .size(48.dp)
            .pressScale(interaction)
            .shadow(8.dp, RoundedCornerShape(percent = 50), clip = false)
            .clip(RoundedCornerShape(percent = 50))
            .background(containerColor)
            .clickable(
                interactionSource = interaction,
                indication = indication,
                enabled = enabled,
                onClick = onClick,
            )
            .semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) { content() }
}

// ---------------------------------------------------------------- floating bottom navigation

@Composable
private fun FloatingNavPill(
    items: List<TopLevelItem>,
    current: AppRoute,
    onSelect: (AppRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val container = scheme.surfaceContainerHigh.copy(alpha = 0.90f)
    Surface(
        color = container,
        shadowElevation = 12.dp,
        shape = RoundedCornerShape(percent = 50),
        modifier = modifier.height(Chrome.BottomBarHeight).widthIn(max = 336.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, top = 0.dp, end = 10.dp, bottom = 0.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                NavPillItem(item = item, selected = current == item.route, onClick = { onSelect(item.route) })
            }
        }
    }
}

@Composable
private fun FloatingRail(
    items: List<TopLevelItem>,
    current: AppRoute,
    onSelect: (AppRoute) -> Unit,
    railWidth: Dp,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val container = scheme.surfaceContainerHigh.copy(alpha = 0.90f)
    Surface(
        color = container,
        shadowElevation = 12.dp,
        shape = RoundedCornerShape(percent = 50),
        modifier = modifier.width(railWidth),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 12.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items.forEach { item ->
                NavPillItem(item = item, selected = current == item.route, onClick = { onSelect(item.route) }, vertical = true)
            }
        }
    }
}

@Composable
private fun NavPillItem(
    item: TopLevelItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    vertical: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val indication = LocalIndication.current
    val pill by animateColorAsState(
        targetValue = if (selected) scheme.secondaryContainer else Color.Transparent,
        animationSpec = Motion.effects(),
        label = "navPill",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) contrastingTextColor(scheme.secondaryContainer) else scheme.onSurfaceVariant,
        animationSpec = Motion.effects(),
        label = "navContent",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.09f else 1f,
        animationSpec = Motion.spatial(),
        label = "navIconScale",
    )
    val boxWidth = if (vertical) 44.dp else 52.dp
    val boxHeight = if (vertical) 34.dp else 30.dp
    val iconSize = if (vertical) 21.dp else 20.dp

    Column(
        modifier = modifier
            .pressScale(interaction, pressedScale = 0.92f)
            .clip(RoundedCornerShape(percent = 50))
            .clickable(
                interactionSource = interaction,
                indication = indication,
                onClick = onClick,
            )
            .semantics { role = Role.Tab }
            .padding(horizontal = 2.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(percent = 50))
                .background(pill)
                .height(boxHeight)
                .width(boxWidth),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = contentColor,
                modifier = Modifier
                    .size(iconSize)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    },
            )
        }
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
            color = contentColor,
            maxLines = 1,
        )
    }
}
