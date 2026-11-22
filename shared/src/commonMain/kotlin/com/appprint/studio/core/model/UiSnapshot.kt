package com.appprint.studio.core.model

import kotlinx.serialization.Serializable

/**
 * Serializable snapshot of everything the user can change — persisted to DataStore so a
 * sudden exit / process death / background return restores the exact same session
 * (including images already added, their placement and enhancement).
 *
 * Ephemeral values (toast, processing, dialogs) are intentionally NOT stored.
 */
@Serializable
data class UiSnapshot(
    val version: Int = 1,
    val theme: AppTheme = AppTheme.SYSTEM,
    val section: String = "SHEET",
    val settings: SheetSettings = SheetSettings(),
    val cards: List<CardItem> = emptyList(),
    val selectedCardId: Long? = null,
    val activePage: Int = 0,
    val forcedPages: Int = 0,
    val zoomMode: String = "FIT",
    val manualZoomPercent: Float = 100f,
    val enhanceScope: String = "SINGLE",
    val compareBefore: Boolean = false,
    val nextId: Long = 100L,
)
