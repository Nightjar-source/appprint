package com.appprint.studio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.SavedStateConfiguration
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateTopPadding
import androidx.compose.foundation.layout.calculateBottomPadding
import androidx.compose.foundation.layout.calculateStartPadding
import com.appprint.studio.core.model.AppTheme
import com.appprint.studio.core.platform.rememberNotificationPermissionRequester
import com.appprint.studio.ui.components.AppIcons
import com.appprint.studio.ui.components.FlagCardGlyph
import com.appprint.studio.ui.components.GlassContainer
import com.appprint.studio.ui.components.GlassIconButton
import com.appprint.studio.ui.screens.GuideScreen
import com.appprint.studio.ui.screens.SettingsScreen
import com.appprint.studio.ui.screens.StudioScreen
import com.appprint.studio.ui.theme.AppPrintTheme
import com.appprint.studio.ui.strings.Str
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.serializersModule
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
    val icon: ImageVector,
)

/**
 * App root — M3 Expressive «Liquid Glass» chrome:
 *  - Edge-to-edge window (enableEdgeToEdge in MainActivity), system insets honored everywhere.
 *  - Floating oval top bar: brand glyph + titles inside a frosted pill, with
 *    *separate* circular action buttons (theme / print / export).
 *  - Floating pill navigation: centered bottom pill (phones) or vertical floating dock
 *    (≥700dp), M3 Expressive active pill indicator — like One UI / Liquid Glass.
 */
@Composable
fun AppRoot(viewModel: StudioViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AppPrintTheme(theme = state.theme) {
        val context = LocalContext.current
        val snackbarHostState = remember { SnackbarHostState() }

        // state flows down, events go up — toast text arrives pre-built from the ViewModel
        val toast = state.toast
        LaunchedEffect(toast) {
            if (toast != null) {
                snackbarHostState.showSnackbar(toast.message)
                viewModel.consumeToast()
            }
        }

        val savedStateConfig = remember {
            SavedStateConfiguration {
                serializersModule = serializersModule {
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
                onBack = { backStack.removeLastOrNull() },
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

        val statusBarsInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val navBarsInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val cutoutStart = WindowInsets.displayCutout.asPaddingValues().calculateStartPadding(LocalLayoutDirection.current)

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val railMode = maxWidth >= 700.dp

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to MaterialTheme.colorScheme.surfaceContainerLowest,
                            1f to MaterialTheme.colorScheme.background,
                        )
                    ),
            ) {
                Column(Modifier.fillMaxSize()) {
                    // ---- floating oval top bar
                    FloatingTopBar(
                        theme = state.theme,
                        onTheme = viewModel::setTheme,
                        onExportPdf = { requestNotifications(); viewModel.sharePdf() },
                        onPrint = { requestNotifications(); viewModel.printPdf() },
                        exporting = state.processing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(start = cutoutStart)
                            .padding(top = statusBarsInset + 8.dp, bottom = 8.dp),
                    )

                    // ---- content + floating navigation
                    if (railMode) {
                        Row(Modifier.weight(1f).fillMaxWidth()) {
                            FloatingDock(
                                items = items,
                                current = current,
                                onSelect = { route -> navigateTopLevel(backStack, route) },
                                modifier = Modifier
                                    .padding(start = cutoutStart)
                                    .padding(start = 12.dp, top = 8.dp, bottom = 8.dp)
                                    .fillMaxHeight(),
                            )
                            Box(Modifier.weight(1f).fillMaxHeight()) {
                                content()
                            }
                        }
                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 24.dp, vertical = 20.dp),
                        )
                    } else {
                        Box(Modifier.weight(1f).fillMaxWidth()) { content() }
                        FloatingNavPill(
                            items = items,
                            current = current,
                            onSelect = { route -> navigateTopLevel(backStack, route) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(start = cutoutStart)
                                .padding(bottom = navBarsInset + 12.dp, top = 4.dp),
                        )
                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 24.dp, bottom = navBarsInset + 112.dp),
                        )
                    }
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
    onExportPdf: () -> Unit,
    onPrint: () -> Unit,
    exporting: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // oval brand container
        GlassContainer(
            shape = RoundedCornerShape(percent = 50),
            modifier = Modifier.weight(1f).height(64.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FlagCardGlyph(sizeDp = 38.dp)
                Column {
                    Text(
                        text = Str.app_eyebrow,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = TextUnit(1.7f, TextUnitType.Sp),
                        maxLines = 1,
                    )
                    Text(
                        text = Str.app_title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
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
            Icon(AppIcons.Print, contentDescription = null, modifier = Modifier.size(22.dp))
        }
        GlassIconButton(
            onClick = onExportPdf,
            contentDescription = Str.btn_export_pdf,
            enabled = !exporting,
        ) {
            if (exporting) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                Icon(AppIcons.Pdf, contentDescription = null, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun TopThemeMenu(theme: AppTheme, onTheme: (AppTheme) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        GlassIconButton(onClick = { expanded = true }, contentDescription = Str.appearance) {
            Icon(AppIcons.Theme, contentDescription = null, modifier = Modifier.size(22.dp))
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
) {
    val pillColor = MaterialTheme.colorScheme.secondaryContainer
    val contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .clickable(onClick = onClick)
            .semantics { role = Role.Tab }
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(percent = 50))
                .background(if (selected) pillColor else Color.Transparent)
                .height(32.dp)
                .width(56.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                item.icon,
                contentDescription = item.label,
                tint = contentColor,
                modifier = Modifier.size(23.dp),
            )
        }
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Black else FontWeight.Medium,
            color = contentColor,
            maxLines = 1,
        )
    }
}

@Composable
private fun FloatingNavPill(
    items: List<TopLevelItem>,
    current: AppRoute,
    onSelect: (AppRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        GlassContainer(shape = RoundedCornerShape(percent = 50)) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
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
}

@Composable
private fun FloatingDock(
    items: List<TopLevelItem>,
    current: AppRoute,
    onSelect: (AppRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        GlassContainer(shape = RoundedCornerShape(percent = 50), modifier = Modifier.fillMaxHeight()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.Center,
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
}