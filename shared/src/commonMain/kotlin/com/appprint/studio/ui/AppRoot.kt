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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.appprint.studio.core.model.AppTheme
import com.appprint.studio.core.platform.rememberNotificationPermissionRequester
import com.appprint.studio.ui.components.AppIcons
import com.appprint.studio.ui.components.IraqiLogo
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

private val InkHeader = Color(0xFF151816)
private val InkText = Color(0xFFFDFAF1)
private val InkMuted = Color(0xFFB9B6AC)

/**
 * App root — brand-ink header + Material 3 Expressive content +
 * compact centered bottom pill (visible on every width).
 *
 * No floating «glass» chrome anymore: the header is a solid brand bar,
 * the navigation is a small centered pill, and every screen draws edge to edge.
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
            TopLevelItem(AppRoute.Studio, Str.nav_studio, AppIcons.Print),
            TopLevelItem(AppRoute.Guide, Str.nav_guide, AppIcons.Guide),
            TopLevelItem(AppRoute.Settings, Str.nav_settings, AppIcons.Settings),
        )

        val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            val wide = maxWidth >= 1000.dp
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                BrandHeader(
                    onPrint = { requestNotifications(); viewModel.printPdf() },
                    onExportPdf = { requestNotifications(); viewModel.sharePdf() },
                    exporting = state.processing,
                    modifier = Modifier.padding(top = statusTop),
                )
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    content()
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = navBottom + 68.dp),
                    )
                }
                BottomNavPill(
                    items = items,
                    current = current,
                    onSelect = { route -> navigateTopLevel(backStack, route) },
                    modifier = Modifier
                        .padding(bottom = if (wide) 10.dp else navBottom + 10.dp),
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

// ---------------------------------------------------------------- header

@Composable
private fun BrandHeader(
    onPrint: () -> Unit,
    onExportPdf: () -> Unit,
    exporting: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(color = InkHeader, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IraqiLogo(size = 38.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    text = Str.app_title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = InkText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = Str.app_eyebrow,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, letterSpacing = 1.3.sp),
                    color = InkMuted,
                    maxLines = 1,
                )
            }
            HeaderIconButton(
                onClick = onExportPdf,
                contentDescription = Str.btn_export_pdf,
                enabled = !exporting,
            ) {
                if (exporting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = InkText)
                } else {
                    Icon(AppIcons.Pdf, contentDescription = null, tint = InkText, modifier = Modifier.size(20.dp))
                }
            }
            HeaderIconButton(
                onClick = onPrint,
                contentDescription = Str.btn_print,
                enabled = !exporting,
            ) {
                Icon(AppIcons.Print, contentDescription = null, tint = InkText, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun HeaderIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = Color.White.copy(alpha = if (enabled) 0.12f else 0.06f),
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(percent = 50))
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            content()
        }
    }
}

// ---------------------------------------------------------------- bottom navigation

@Composable
private fun BottomNavPill(
    items: List<TopLevelItem>,
    current: AppRoute,
    onSelect: (AppRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        modifier = modifier.width(300.dp).height(60.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                BottomNavItem(
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
private fun BottomNavItem(
    item: TopLevelItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant
    val pillColor = MaterialTheme.colorScheme.secondaryContainer
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .clickable(onClick = onClick)
            .semantics { role = Role.Tab }
            .padding(horizontal = 2.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(percent = 50))
                .background(if (selected) pillColor else Color.Transparent)
                .height(28.dp)
                .width(52.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                item.icon,
                contentDescription = item.label,
                tint = contentColor,
                modifier = Modifier.size(20.dp),
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
