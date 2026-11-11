package com.appprint.studio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.SavedStateConfiguration
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.appprint.studio.core.model.AppTheme
import com.appprint.studio.core.platform.rememberNotificationPermissionRequester
import com.appprint.studio.shared.resources.Res
import com.appprint.studio.shared.resources.app_eyebrow
import com.appprint.studio.shared.resources.app_title
import com.appprint.studio.shared.resources.appearance
import com.appprint.studio.shared.resources.btn_export_pdf
import com.appprint.studio.shared.resources.btn_print
import com.appprint.studio.shared.resources.nav_guide
import com.appprint.studio.shared.resources.nav_settings
import com.appprint.studio.shared.resources.nav_studio
import com.appprint.studio.shared.resources.theme_auto
import com.appprint.studio.shared.resources.theme_dark
import com.appprint.studio.shared.resources.theme_light
import com.appprint.studio.ui.components.AppIcons
import com.appprint.studio.ui.components.FlagCardGlyph
import com.appprint.studio.ui.screens.GuideScreen
import com.appprint.studio.ui.screens.SettingsScreen
import com.appprint.studio.ui.screens.StudioScreen
import com.appprint.studio.ui.theme.AppPrintTheme
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.serializersModule
import kotlinx.serialization.modules.subclass
import org.jetbrains.compose.resources.stringResource

@Serializable
sealed interface AppRoute : NavKey {
    @Serializable
    data object Studio : AppRoute

    @Serializable
    data object Guide : AppRoute

    @Serializable
    data object Settings : AppRoute
}

/**
 * App root: three top-level destinations with an adaptive navigation shell
 * (bottom bar on phones, navigation rail on larger screens) and a brand header.
 * UDF: state flows from StudioViewModel; Navigation 3 owns the back stack as state.
 */
@Composable
fun AppRoot(viewModel: StudioViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AppPrintTheme(theme = state.theme) {
        val snackbarHostState = remember { SnackbarHostState() }

        // resolve toast text in composition (stringResource is composable)
        val toast = state.toast
        val toastMessage: String? = when {
            toast == null -> null
            toast.args.isEmpty() -> stringResource(toast.resource)
            else -> stringResource(toast.resource, *toast.args.toTypedArray())
        }
        LaunchedEffect(toast, toastMessage) {
            if (toastMessage != null) {
                snackbarHostState.showSnackbar(toastMessage)
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

        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Column(Modifier.fillMaxSize()) {
                AppHeader(
                    theme = state.theme,
                    onTheme = viewModel::setTheme,
                    onExportPdf = {
                        requestNotifications()
                        viewModel.sharePdf()
                    },
                    onPrint = {
                        requestNotifications()
                        viewModel.printPdf()
                    },
                    exporting = state.processing,
                )
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                    val useRail = maxWidth >= 600.dp
                    if (useRail) {
                        Row(Modifier.fillMaxSize()) {
                            NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                                railItem(AppRoute.Studio, current, backStack, stringResource(Res.string.nav_studio)) {
                                    Icon(AppIcons.Print, contentDescription = null)
                                }
                                railItem(AppRoute.Guide, current, backStack, stringResource(Res.string.nav_guide)) {
                                    Icon(androidx.compose.material.icons.Icons.Filled.List, contentDescription = null)
                                }
                                railItem(AppRoute.Settings, current, backStack, stringResource(Res.string.nav_settings)) {
                                    Icon(androidx.compose.material.icons.Icons.Filled.Settings, contentDescription = null)
                                }
                            }
                            Box(Modifier.weight(1f).fillMaxHeight()) {
                                content()
                            }
                        }
                    } else {
                        Column(Modifier.fillMaxSize()) {
                            Box(Modifier.weight(1f).fillMaxWidth()) {
                                content()
                            }
                            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                                barItem(AppRoute.Studio, current, backStack, stringResource(Res.string.nav_studio)) {
                                    Icon(AppIcons.Print, contentDescription = null)
                                }
                                barItem(AppRoute.Guide, current, backStack, stringResource(Res.string.nav_guide)) {
                                    Icon(androidx.compose.material.icons.Icons.Filled.List, contentDescription = null)
                                }
                                barItem(AppRoute.Settings, current, backStack, stringResource(Res.string.nav_settings)) {
                                    Icon(androidx.compose.material.icons.Icons.Filled.Settings, contentDescription = null)
                                }
                            }
                        }
                    }
                }
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
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

@Composable
private fun railItem(
    route: AppRoute,
    current: AppRoute,
    backStack: MutableList<NavKey>,
    label: String,
    icon: @Composable () -> Unit,
) {
    NavigationRailItem(
        selected = current == route,
        onClick = { navigateTopLevel(backStack, route) },
        icon = icon,
        label = { Text(label, maxLines = 1) },
        alwaysShowLabel = true,
    )
}

@Composable
private fun barItem(
    route: AppRoute,
    current: AppRoute,
    backStack: MutableList<NavKey>,
    label: String,
    icon: @Composable () -> Unit,
) {
    NavigationBarItem(
        selected = current == route,
        onClick = { navigateTopLevel(backStack, route) },
        icon = icon,
        label = { Text(label, maxLines = 1) },
        alwaysShowLabel = true,
    )
}

@Composable
private fun AppHeader(
    theme: AppTheme,
    onTheme: (AppTheme) -> Unit,
    onExportPdf: () -> Unit,
    onPrint: () -> Unit,
    exporting: Boolean,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FlagCardGlyph(sizeDp = 44.dp)
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.app_eyebrow),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = TextUnit(1.6f, TextUnitType.Sp),
                        maxLines = 1,
                    )
                    Text(
                        text = stringResource(Res.string.app_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                }

                ThemeMenu(theme = theme, onTheme = onTheme)

                IconButton(onClick = onPrint, enabled = !exporting) {
                    Icon(AppIcons.Print, contentDescription = stringResource(Res.string.btn_print))
                }
                FilledIconButton(onClick = onExportPdf, enabled = !exporting) {
                    if (exporting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(AppIcons.Pdf, contentDescription = stringResource(Res.string.btn_export_pdf))
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun ThemeMenu(theme: AppTheme, onTheme: (AppTheme) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(AppIcons.Theme, contentDescription = stringResource(Res.string.appearance))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            val options = listOf(
                AppTheme.SYSTEM to Res.string.theme_auto,
                AppTheme.LIGHT to Res.string.theme_light,
                AppTheme.DARK to Res.string.theme_dark,
            )
            options.forEach { (option, labelRes) ->
                DropdownMenuItem(
                    text = { Text(stringResource(labelRes)) },
                    onClick = {
                        onTheme(option)
                        expanded = false
                    },
                    trailingIcon = {
                        if (theme == option) {
                            Icon(androidx.compose.material.icons.Icons.Filled.Check, contentDescription = null)
                        }
                    },
                )
            }
        }
    }
}
