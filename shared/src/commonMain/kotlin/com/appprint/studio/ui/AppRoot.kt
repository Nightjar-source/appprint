package com.appprint.studio.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateBottomPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.calculateTopPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.appprint.studio.ui.components.AppIcons
import com.appprint.studio.ui.components.Chrome
import com.appprint.studio.ui.components.FlagCardGlyph
import com.appprint.studio.ui.components.GlassContainer
import com.appprint.studio.ui.components.GlassIconButton
import com.appprint.studio.ui.screens.GuideScreen
import com.appprint.studio.ui.screens.SettingsScreen
import com.appprint.studio.ui.screens.StudioScreen
import com.appprint.studio.ui.theme.AppPrintTheme
import com.appprint.studio.ui.strings.Str
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

/**
 * App root — full-bleed «Liquid Glass» chrome, Material 3 Expressive:
 *  - One unified background role; every screen draws edge-to-edge behind floating chrome.
 *  - Floating oval top bar (brand glyph + title) with separate round action buttons.
 *  - Floating bottom pill bar (phones) or a clipped 80dp vertical rail (≥700dp),
 *    always visible labels, animated M3 pill indicators with Expressive springs.
 *  - Bottom/edge snackbar always sits above the floating bar.
 */
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

        @Composable
        fun content() {
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull(); Unit },
                entryProvider = entryProvider {
                    entry<AppRoute.Studio> { StudioScreen(viewModel = viewModel) }
                    entry<AppRoute.Guide> { GuideScreen() }
                    entry<AppRoute.Settings> { SettingsScreen(viewModel = viewModel) }
                },
            )
        }

        val items = listOf(
            TopLevelItem(AppRoute.Studio, Str.nav_studio, AppIcons.Print),
            TopLevelItem(AppRoute.Guide, Str.nav_guide, AppIcons.Guide),
            TopLevelItem(AppRoute.Settings, Str.nav_settings, AppIcons.Settings),
        )

        val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val cutoutStart = WindowInsets.displayCutout.asPaddingValues().calculateStartPadding(LocalLayoutDirection.current)

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val railMode = maxWidth >= 700.dp

            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to MaterialTheme.colorScheme.surfaceContainerLowest,
                            0.55f to MaterialTheme.colorScheme.background,
                            1f to MaterialTheme.colorScheme.surfaceContainerLowest,
                        )
                    ),
            ) {
                // ---- screens fill the whole window, edge to edge, behind the floating chrome
                content()

                // ---- floating oval top bar (56 dp)
                FloatingTopBar(
                    theme = state.theme,
                    onTheme = viewModel::setTheme,
                    onPrint = { requestNotifications(); viewModel.printPdf() },
                    onExportPdf = { requestNotifications(); viewModel.sharePdf() },
                    exporting = state.processing,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(start = Chrome.EdgeInset + cutoutStart, end = Chrome.EdgeInset, top = statusTop + Chrome.TopBarGap),
                )

                if (railMode) {
                    FloatingRail(
                        items = items,
                        current = current,
                        onSelect = { route -> navigateTopLevel(backStack, route) },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = cutoutStart + Chrome.EdgeInset),
                    )
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 24.dp, vertical = 24.dp),
                    )
                } else {
                    FloatingNavBar(
                        items = items,
                        current = current,
                        onSelect = { route -> navigateTopLevel(backStack, route) },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(start = Chrome.EdgeInset + cutoutStart, end = Chrome.EdgeInset, bottom = navBottom + Chrome.BottomBarGap),
                    )
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 20.dp, bottom = navBottom + Chrome.BottomBarGap + Chrome.BottomBarHeight + 12.dp),
                    )
                }
            }
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
    theme: AppTheme,
    onTheme: (AppTheme) -> Unit,
    onPrint: () -> Unit,
    onExportPdf: () -> Unit,
    exporting: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.height(Chrome.TopBarHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // oval brand container
        GlassContainer(
            shape = RoundedCornerShape(percent = 50),
            modifier = Modifier.weight(1f).height(Chrome.TopBarHeight),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FlagCardGlyph(sizeDp = 34.dp)
                Column(Modifier.weight(1f)) {
                    Text(
                        text = Str.app_eyebrow,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, letterSpacing = TextUnit(1.4f, TextUnitType.Sp)),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = Str.app_title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // separate round actions
        TopThemeMenu(theme = theme, onTheme = onTheme)
        GlassIconButton(
            onClick = onPrint,
            contentDescription = Str.btn_print,
            enabled = !exporting,
        ) {
            Icon(AppIcons.Print, contentDescription = null, modifier = Modifier.size(20.dp))
        }
        GlassIconButton(
            onClick = onExportPdf,
            contentDescription = Str.btn_export_pdf,
            enabled = !exporting,
        ) {
            if (exporting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(AppIcons.Pdf, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun TopThemeMenu(theme: AppTheme, onTheme: (AppTheme) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        GlassIconButton(onClick = { expanded = true }, contentDescription = Str.appearance) {
            Icon(AppIcons.Theme, contentDescription = null, modifier = Modifier.size(20.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            val options = listOf(
                AppTheme.SYSTEM to Str.theme_auto,
                AppTheme.LIGHT to Str.theme_light,
                AppTheme.DARK to Str.theme_dark,
            )
            options.forEach { (option, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onTheme(option)
                        expanded = false
                    },
                    trailingIcon = {
                        if (theme == option) {
                            Icon(AppIcons.Check, contentDescription = null)
                        }
                    },
                )
            }
        }
    }
}

// ---------------------------------------------------------------- floating navigation

@Composable
private fun NavPillItem(
    item: TopLevelItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pillColor = MaterialTheme.colorScheme.secondaryContainer
    val contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by androidx.compose.foundation.interaction.collectIsPressedAsState(interaction)
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 500f),
        label = "navItemPress",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.14f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f),
        label = "navIcon",
    )
    val pillWidth by animateDpAsState(
        targetValue = if (selected) 56.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f),
        label = "navPillWidth",
    )
    Column(
        modifier = modifier
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .clip(RoundedCornerShape(percent = 50))
            .clickable(onClick = onClick, interactionSource = interaction)
            .semantics { role = Role.Tab }
            .padding(horizontal = 2.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(
            modifier = Modifier
                .height(30.dp)
                .width(56.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (pillWidth > 0.5.dp) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(pillColor)
                        .height(30.dp)
                        .width(pillWidth),
                )
            }
            Icon(
                item.icon,
                contentDescription = item.label,
                tint = contentColor,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer { scaleX = iconScale; scaleY = iconScale },
            )
        }
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun FloatingNavBar(
    items: List<TopLevelItem>,
    current: AppRoute,
    onSelect: (AppRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassContainer(
        shape = RoundedCornerShape(percent = 50),
        modifier = modifier.height(Chrome.BottomBarHeight),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                NavPillItem(
                    item = item,
                    selected = current == item.route,
                    onClick = { onSelect(item.route) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun FloatingRail(
    items: List<TopLevelItem>,
    current: AppRoute,
    onSelect: (AppRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassContainer(
        shape = RoundedCornerShape(28.dp),
        modifier = modifier.width(76.dp),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 4.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items.forEach { item ->
                NavPillItem(
                    item = item,
                    selected = current == item.route,
                    onClick = { onSelect(item.route) },
                )
            }
        }
    }
}
