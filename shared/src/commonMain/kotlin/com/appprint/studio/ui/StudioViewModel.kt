package com.appprint.studio.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appprint.studio.core.arrange.SheetArranger
import com.appprint.studio.core.model.AppTheme
import com.appprint.studio.core.model.ArrangeMode
import com.appprint.studio.core.model.CardItem
import com.appprint.studio.core.model.CardType
import com.appprint.studio.core.model.EnhanceSettings
import com.appprint.studio.core.model.FrameStyle
import com.appprint.studio.core.model.SheetBackground
import com.appprint.studio.core.model.SheetSettings
import com.appprint.studio.core.platform.PlatformActions
import com.appprint.studio.shared.resources.Res
import com.appprint.studio.shared.resources.msg_applied_all
import com.appprint.studio.shared.resources.msg_arranged
import com.appprint.studio.shared.resources.msg_camera_denied
import com.appprint.studio.shared.resources.msg_camera_unavailable
import com.appprint.studio.shared.resources.msg_cleared
import com.appprint.studio.shared.resources.msg_export_failed
import com.appprint.studio.shared.resources.msg_import_failed
import com.appprint.studio.shared.resources.msg_imported
import com.appprint.studio.shared.resources.msg_no_cards
import com.appprint.studio.shared.resources.msg_pdf_error
import com.appprint.studio.shared.resources.msg_pdf_saved
import com.appprint.studio.shared.resources.msg_png_saved
import com.appprint.studio.shared.resources.msg_print_sent
import com.appprint.studio.shared.resources.msg_print_unavailable
import com.appprint.studio.shared.resources.msg_samples
import com.appprint.studio.shared.resources.msg_share_unavailable
import com.appprint.studio.shared.resources.msg_sorted
import com.appprint.studio.shared.resources.notif_export_body
import com.appprint.studio.shared.resources.notif_export_title
import com.appprint.studio.shared.resources.notif_print_body
import com.appprint.studio.shared.resources.notif_print_title
import org.jetbrains.compose.resources.getString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random
import org.jetbrains.compose.resources.StringResource

/** One-shot user feedback routed to the global snackbar. */
data class UiToast(val resource: StringResource, val args: List<Any> = emptyList())

data class StudioUiState(
    val settings: SheetSettings = SheetSettings(),
    val cards: List<CardItem> = emptyList(),
    val selectedId: Long? = null,
    val currentPage: Int = 0,
    val theme: AppTheme = AppTheme.SYSTEM,
    val processing: Boolean = false,
    val toast: UiToast? = null,
) {
    val pageCount: Int get() = (cards.maxOfOrNull { it.page } ?: 0) + 1
    val selectedCard: CardItem? get() = cards.firstOrNull { it.id == selectedId }
    val cardsOnPage: List<CardItem> get() = cards.filter { it.page == currentPage }
}

/**
 * Single source of truth (UDF). State flows down, events go up.
 * All platform work (files, camera, print, sharing) is delegated to PlatformActions.
 */
class StudioViewModel : ViewModel() {

    private var idCounter = Random.nextLong(1_000_000_000L, Long.MAX_VALUE / 2)
    private fun nextId(): Long = idCounter++

    private val _state = MutableStateFlow(StudioUiState(theme = PlatformActions.loadTheme()))
    val state: StateFlow<StudioUiState> = _state.asStateFlow()

    // ------------------------------------------------------------- theme

    fun setTheme(theme: AppTheme) {
        PlatformActions.saveTheme(theme)
        _state.update { it.copy(theme = theme) }
    }

    // ------------------------------------------------------------- cards IO

    fun importImages(sources: List<String>) {
        if (sources.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(processing = true) }
            val imported = withContext(Dispatchers.IO) {
                sources.mapNotNull { source -> importOne(source) }
            }
            if (imported.isEmpty()) {
                _state.update { it.copy(processing = false, toast = UiToast(Res.string.msg_import_failed)) }
                return@launch
            }
            _state.update { s ->
                s.copy(
                    cards = arrangeNow(s.cards + imported, s.settings),
                    processing = false,
                    toast = UiToast(Res.string.msg_imported, listOf(imported.size)),
                )
            }
        }
    }

    fun importCapture(source: String) = importImages(listOf(source))

    private fun importOne(source: String): CardItem? {
        val imported = PlatformActions.importImage(source) ?: return null
        val imgAspect = imported.widthPx.toFloat() / imported.heightPx.coerceAtLeast(1)
        val (wMm, hMm) = if (kotlin.math.abs(imgAspect - 85.6f / 54f) < 0.08f) {
            85.6f to 54f
        } else {
            val w = 100f
            w to (w / imgAspect).coerceIn(30f, 220f)
        }
        return CardItem(
            id = nextId(),
            typeKey = CardType.CUSTOM.key,
            name = imported.displayName.ifBlank { CardType.CUSTOM.labelAr },
            widthMm = wMm,
            heightMm = hMm,
            imageUri = imported.privatePath,
            isSample = false,
        )
    }

    /** Adds one sample card of every Iraqi card type (تعليمية). */
    fun addSamples() {
        val samples = listOf(
            CardType.NATIONAL, CardType.DRIVING, CardType.VOTER, CardType.STUDENT,
            CardType.RESIDENCE, CardType.RATION, CardType.PASSPORT,
        ).map { type ->
            CardItem(
                id = nextId(),
                typeKey = type.key,
                name = type.labelAr,
                widthMm = type.widthMm,
                heightMm = type.heightMm,
                isSample = true,
            )
        }
        _state.update { s ->
            s.copy(
                cards = arrangeNow(s.cards + samples, s.settings),
                toast = UiToast(Res.string.msg_samples),
            )
        }
    }

    // ------------------------------------------------------------- arrangement

    private fun arrangeNow(cards: List<CardItem>, settings: SheetSettings): List<CardItem> =
        SheetArranger.arrange(cards, settings).cards

    fun arrangeSmart() {
        _state.update { s ->
            if (s.cards.isEmpty()) s.copy(toast = UiToast(Res.string.msg_no_cards))
            else s.copy(
                cards = arrangeNow(s.cards, s.settings),
                toast = UiToast(Res.string.msg_arranged),
            )
        }
    }

    fun sortBySize() {
        _state.update { s ->
            if (s.cards.isEmpty()) s.copy(toast = UiToast(Res.string.msg_no_cards))
            else s.copy(
                cards = SheetArranger.arrange(s.cards, s.settings.copy(smartGroup = false)).cards,
                toast = UiToast(Res.string.msg_sorted),
            )
        }
    }

    fun clearAll() {
        _state.update {
            it.copy(cards = emptyList(), selectedId = null, currentPage = 0, toast = UiToast(Res.string.msg_cleared))
        }
    }

    fun updateSettings(transform: (SheetSettings) -> SheetSettings) {
        _state.update { s ->
            val newSettings = transform(s.settings)
            s.copy(
                settings = newSettings,
                cards = if (s.cards.isEmpty()) s.cards else arrangeNow(s.cards, newSettings),
                currentPage = 0,
            )
        }
    }

    fun setPaperPreset(key: String?) = updateSettings { s ->
        if (key == null) s.copy(presetKey = "custom") else s.copy(presetKey = key)
    }

    fun setCustomPaper(widthMm: Float, heightMm: Float) = updateSettings { s ->
        s.copy(presetKey = "custom", customWidthMm = widthMm, customHeightMm = heightMm)
    }

    fun setLandscape(landscape: Boolean) = updateSettings { it.copy(landscape = landscape) }
    fun setMargin(mm: Float) = updateSettings { it.copy(marginMm = mm) }
    fun setGap(mm: Float) = updateSettings { it.copy(gapMm = mm) }
    fun setMode(mode: ArrangeMode) = updateSettings { it.copy(mode = mode) }
    fun setSmartGroup(enabled: Boolean) = updateSettings { it.copy(smartGroup = enabled) }
    fun setBackground(bg: SheetBackground) = updateSettings { it.copy(background = bg) }
    fun setShowGuides(enabled: Boolean) = updateSettings { it.copy(showGuides = enabled) }
    fun setShowFrames(enabled: Boolean) = updateSettings { it.copy(showFrames = enabled) }
    fun setFrameStyle(style: FrameStyle) = updateSettings { it.copy(frameStyle = style) }
    fun setFrameColor(hex: Long) = updateSettings { it.copy(frameColorHex = hex) }
    fun setFrameWidth(dp: Float) = updateSettings { it.copy(frameWidthDp = dp) }

    // ------------------------------------------------------------- selection & editing

    fun selectCard(id: Long?) = _state.update { it.copy(selectedId = id) }

    fun currentPage(page: Int) = _state.update { s ->
        s.copy(currentPage = page.coerceIn(0, (s.pageCount - 1).coerceAtLeast(0)), selectedId = null)
    }

    /** Drag-to-move in mm coordinates, clamped inside the sheet. */
    fun moveCard(id: Long, xMm: Float, yMm: Float) {
        _state.update { s ->
            val settings = s.settings
            val clamped = s.cards.map { card ->
                if (card.id != id) card
                else {
                    val maxX = (settings.sheetWidthMm - card.effectiveWidth).coerceAtLeast(0f)
                    val maxY = (settings.sheetHeightMm - card.effectiveHeight).coerceAtLeast(0f)
                    card.copy(xMm = xMm.coerceIn(0f, maxX), yMm = yMm.coerceIn(0f, maxY))
                }
            }
            s.copy(cards = clamped)
        }
    }

    fun deleteSelected() {
        _state.update { s ->
            val id = s.selectedId ?: return@update s
            s.copy(cards = s.cards.filterNot { it.id == id }, selectedId = null, currentPage = 0)
        }
    }

    fun duplicateSelected() {
        _state.update { s ->
            val card = s.selectedCard ?: return@update s
            val copy = card.copy(id = nextId(), xMm = card.xMm + 12f, yMm = card.yMm + 12f)
            s.copy(cards = arrangeNow(s.cards + copy, s.settings), selectedId = copy.id)
        }
    }

    fun rotateSelected() {
        _state.update { s ->
            val id = s.selectedId ?: return@update s
            val updated = s.cards.map {
                if (it.id == id) it.copy(rotation = if (it.rotation == 90) 0 else 90) else it
            }
            s.copy(cards = arrangeNow(updated, s.settings))
        }
    }

    // ------------------------------------------------------------- enhance

    fun updateEnhance(transform: (EnhanceSettings) -> EnhanceSettings) {
        _state.update { s ->
            val id = s.selectedId ?: return@update s
            s.copy(cards = s.cards.map { if (it.id == id) it.copy(enhance = transform(it.enhance)) else it })
        }
    }

    fun autoEnhanceSelected() = updateEnhance { EnhanceSettings.AUTO }
    fun resetEnhanceSelected() = updateEnhance { EnhanceSettings.IDENTITY }

    fun applyEnhanceToAll() {
        _state.update { s ->
            val source = s.selectedCard?.enhance ?: return@update s
            s.copy(cards = s.cards.map { it.copy(enhance = source) }, toast = UiToast(Res.string.msg_applied_all))
        }
    }

    fun autoEnhanceAll() {
        _state.update { s -> s.copy(cards = s.cards.map { it.copy(enhance = EnhanceSettings.AUTO) }) }
    }

    fun resetEnhanceAll() {
        _state.update { s -> s.copy(cards = s.cards.map { it.copy(enhance = EnhanceSettings.IDENTITY) }) }
    }

    // ------------------------------------------------------------- export / print / share

    fun sharePdf() = exportAndShare("iraqi-cards-print.pdf", "application/pdf", page = null)

    fun sharePng(page: Int) = exportAndShare("sheet-${page + 1}.png", "image/png", page = page)

    private fun exportAndShare(fileName: String, mimeType: String, page: Int?) {
        viewModelScope.launch {
            val s = _state.value
            if (s.cards.isEmpty()) {
                _state.update { it.copy(toast = UiToast(Res.string.msg_no_cards)) }
                return@launch
            }
            _state.update { it.copy(processing = true) }
            val result = withContext(Dispatchers.IO) {
                val bytes = if (page == null) {
                    PlatformActions.exportPdf(s.cards, s.settings, s.pageCount)
                } else {
                    PlatformActions.exportPng(page, s.cards, s.settings)
                }
                bytes?.let { PlatformActions.writeExport(fileName, it) }
            }
            _state.update { it.copy(processing = false) }
            if (result == null) {
                _state.update { it.copy(toast = UiToast(Res.string.msg_export_failed)) }
                return@launch
            }
            val shared = withContext(Dispatchers.IO) {
                PlatformActions.shareFile(result, mimeType, subject = fileName)
            }
            if (!shared) {
                _state.update { it.copy(toast = UiToast(Res.string.msg_share_unavailable, listOf(fileName))) }
            } else if (page != null) {
                _state.update { it.copy(toast = UiToast(Res.string.msg_png_saved)) }
            } else {
                _state.update { it.copy(toast = UiToast(Res.string.msg_pdf_saved, listOf(_state.value.pageCount))) }
            }
            // system notification (auto-requests handled by the UI right before export)
            val title = getString(Res.string.notif_export_title)
            val body = getString(Res.string.notif_export_body, fileName)
            PlatformActions.showExportNotification(title, body, result)
        }
    }

    fun printPdf() {
        viewModelScope.launch {
            val s = _state.value
            if (s.cards.isEmpty()) {
                _state.update { it.copy(toast = UiToast(Res.string.msg_no_cards)) }
                return@launch
            }
            _state.update { it.copy(processing = true) }
            val bytes = withContext(Dispatchers.IO) {
                PlatformActions.exportPdf(s.cards, s.settings, s.pageCount)
            }
            _state.update { it.copy(processing = false) }
            if (bytes == null) {
                _state.update { it.copy(toast = UiToast(Res.string.msg_pdf_error)) }
                return@launch
            }
            val accepted = PlatformActions.printPdf(
                pdfBytes = bytes,
                pageCount = s.pageCount,
                sheetWidthMm = s.settings.sheetWidthMm,
                sheetHeightMm = s.settings.sheetHeightMm,
                jobName = "iraqi-id-cards",
            )
            _state.update {
                it.copy(toast = UiToast(if (accepted) Res.string.msg_print_sent else Res.string.msg_print_unavailable))
            }
            if (accepted) {
                val title = getString(Res.string.notif_print_title)
                val body = getString(Res.string.notif_print_body, "iraqi-id-cards.pdf")
                PlatformActions.showExportNotification(title, body, null)
            }
        }
    }

    fun notifyCameraDenied() {
        _state.update { it.copy(toast = UiToast(Res.string.msg_camera_denied)) }
    }

    fun notifyCameraUnavailable() {
        _state.update { it.copy(toast = UiToast(Res.string.msg_camera_unavailable)) }
    }

    fun consumeToast() = _state.update { it.copy(toast = null) }
}
