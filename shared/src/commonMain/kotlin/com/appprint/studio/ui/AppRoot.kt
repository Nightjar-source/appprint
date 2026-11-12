package com.appprint.studio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.appprint.studio.ui.platform.PlatformBackHandler
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.appprint.studio.core.platform.rememberNotificationPermissionRequester
import com.appprint.studio.ui.components.IraqiLogo
import com.appprint.studio.ui.screens.GuideScreen
import com.appprint.studio.ui.screens.SettingsScreen
import com.appprint.studio.ui.screens.StudioScreen
import com.appprint.studio.ui.theme.AppPrintTheme
import com.appprint.studio.ui.theme.contrastingTextColor
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

private fun routeName(route: AppRoute): String = when (route) {
    AppRoute.Studio -> Str.nav_studio
    AppRoute.Guide -> Str.nav_guide
    AppRoute.Settings -> Str.nav_settings
}

/**
 * App root — unified edge-to-edge chrome (Material 3 Expressive):
 *  - Fixed top bar shares the content tone with a hairline divider — no color blocks, no floats.
 *  - Title shows the current screen name, with circular action buttons.
 *  - One unified export menu button (PDF / PNG / HTML / share) + direct print button.
 *  - Bottom bar matches the scaffold tone with a hairline divider.
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
                modifier = Modifier.fillMaxSize(),
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

        // Back: leave secondary screens to the studio; at the studio, history handled inside the screen.
        PlatformBackHandler(enabled = true) {
            if (current != AppRoute.Studio) navigateTopLevel(backStack, AppRoute.Studio)
        }

        BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            val wide = maxWidth >= 1000.dp
            Column(Modifier.fillMaxSize()) {
                TopBar(
                    title = routeName(current),
                    onExportPdf = viewModel::sharePdf,
                    onExportPng = { viewModel.sharePng(state.currentPage) },
                    onExportHtml = viewModel::shareHtml,
                    onShare = { viewModel.sharePng(state.currentPage) },
                    onPrint = { requestNotifications(); viewModel.printPdf() },
                    exporting = state.processing,
                    modifier = Modifier.padding(top = statusTop),
                )
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    content()
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = navBottom + 96.dp),
                    )
                }
                BottomNavBar(
                    items = items,
                    current = current,
                    onSelect = { route -> navigateTopLevel(backStack, route) },
                    showLabels = !wide,
                )
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

// ---------------------------------------------------------------- top bar

@Composable
private fun TopBar(
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

    Surface(color = scheme.background, modifier = modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // circular brand button (also goes Back→studio when pressed on a child screen)
                IconButton(
                    onClick = { /* handled by system back — no navigation needed here */ },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(scheme.surfaceContainerHigh),
                ) {
                    IraqiLogo(size = 30.dp)
                }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(
                        text = Str.app_eyebrow,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, letterSpacing = 1.4.sp),
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // unified export menu — PDF / PNG / HTML / share
                Box {
                    IconButton(
                        onClick = { exportMenuOpen = true },
                        enabled = !exporting,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(if (exportMenuOpen) scheme.secondaryContainer else scheme.surfaceContainerHigh),
                    ) {
                        if (exporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = contrastingTextColor(scheme.secondaryContainer),
                            )
                        } else {
                            Icon(
                                Icons.Filled.FileUpload,
                                contentDescription = Str.export_menu,
                                tint = if (exportMenuOpen) contrastingTextColor(scheme.secondaryContainer) else scheme.onSurface,
                                modifier = Modifier.size(22.dp),
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
                }

                // direct print button
                IconButton(
                    onClick = onPrint,
                    enabled = !exporting,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(scheme.surfaceContainerHigh),
                ) {
                    Icon(
                        Icons.Filled.Print,
                        contentDescription = Str.btn_print,
                        tint = scheme.onSurface,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.45f))
        }
    }
}

// ---------------------------------------------------------------- bottom navigation (fixed)

@Composable
private fun BottomNavBar(
    items: List<TopLevelItem>,
    current: AppRoute,
    onSelect: (AppRoute) -> Unit,
    showLabels: Boolean,
) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEach { item ->
                    NavItem(
                        item = item,
                        selected = current == item.route,
                        onClick = { onSelect(item.route) },
                        showLabel = showLabels,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun NavItem(
    item: TopLevelItem,
    selected: Boolean,
    onClick: () -> Unit,
    showLabel: Boolean,
    modifier: Modifier = Modifier,
) {
    val pill = MaterialTheme.colorScheme.secondaryContainer
    val content = if (selected) contrastingTextColor(pill) else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .semantics { role = Role.Tab }
            .padding(horizontal = 2.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(percent = 50))
                .background(if (selected) pill else Color.Transparent)
                .height(28.dp)
                .width(52.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(item.icon, contentDescription = item.label, tint = content, modifier = Modifier.size(20.dp))
        }
        if (showLabel) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                color = content,
                maxLines = 1,
            )
        }
    }
}
