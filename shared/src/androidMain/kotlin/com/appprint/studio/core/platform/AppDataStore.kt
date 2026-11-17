package com.appprint.studio.core.platform

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import com.appprint.studio.core.model.AppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Compose-First settings store — Jetpack DataStore (official), file `app_settings.preferences_pb`.
 *
 * - Asynchronous reads/writes via DataStore coroutines (never blocks the UI thread).
 * - A synchronous in-memory [ConcurrentHashMap] mirror gives 0-cost reads for the UI
 *   (loadTheme() reads the cache, no runBlocking on the main thread).
 * - Automatic one-shot migration of the legacy `tabati_prefs` SharedPreferences so no
 *   user setting is lost on upgrade.
 */
object AppDataStore {

    private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
        name = "app_settings",
        produceMigrations = { ctx ->
            // Migrate the legacy "theme" key from tabati_prefs → app_settings automatically.
            listOf(SharedPreferencesMigration(ctx, LEGACY_PREFS, setOf(LEGACY_THEME_KEY)))
        },
    )

    private const val LEGACY_PREFS = "tabati_prefs"
    private const val LEGACY_THEME_KEY = "theme"
    private val THEME_KEY = stringPreferencesKey("theme")

    /** Synchronous in-memory cache — atomic reads, no coroutine needed on the UI thread. */
    private val cache = ConcurrentHashMap<String, String>()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var started = false

    /** Called once from MainActivity.onCreate before setContent. */
    fun init(context: Context) {
        if (started) return
        started = true
        val ds = context.applicationContext.settingsDataStore
        scope.launch {
            ds.data
                .map { it[THEME_KEY] }
                .distinctUntilChanged()
                .collect { theme ->
                    if (theme != null) cache[THEME_KEY.name] = theme
                    else cache.putIfAbsent(THEME_KEY.name, AppTheme.SYSTEM.name)
                }
        }
        // Prime the cache synchronously-async: first read populates, then the collector keeps it fresh.
        scope.launch {
            val first = ds.data.first()[THEME_KEY] ?: AppTheme.SYSTEM.name
            cache[THEME_KEY.name] = first
        }
    }

    /** 0-cost synchronous read from the in-memory mirror. */
    fun loadThemeSync(): AppTheme {
        val raw = cache[THEME_KEY.name] ?: AppTheme.SYSTEM.name
        return runCatching { AppTheme.valueOf(raw) }.getOrDefault(AppTheme.SYSTEM)
    }

    /** Asynchronous write to DataStore; updates the mirror immediately for 0-latency UI. */
    fun saveTheme(theme: AppTheme) {
        cache[THEME_KEY.name] = theme.name
        scope.launch {
            AndroidPlatform.appContext.settingsDataStore.edit { it[THEME_KEY] = theme.name }
        }
    }
}
