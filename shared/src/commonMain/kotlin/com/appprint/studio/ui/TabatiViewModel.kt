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
import com.appprint.studio.core.model.Orientation
import com.appprint.studio.core.model.PaperPreset
import com.appprint.studio.core.model.SheetBackground
import com.appprint.studio.core.model.SheetFrame
import com.appprint.studio.core.model.SheetSettings
import com.appprint.studio.core.platform.PlatformActions
import com.appprint.studio.ui.strings.Str
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** One-shot message routed to the snackbar. */
data class TabatiToast(val message: String)

/** Editor sections — mirror the design's `activeSection`. */
enum class StudioSection {
    SHEET, PAPER, CARDS, ENHANCE, BORDERS, GUIDE;

    val isEditingTab: Boolean get() = this != SHEET
}

/** Mergeable document state — single source of truth (UDF). */
data class TabatiUiState(
    val theme: AppTheme = AppTheme.SYSTEM,
    val section: StudioSection = StudioSection.SHEET,
    val settings: SheetSettings = SheetSettings(),
    val cards: List<CardItem> = emptyList(),
    val selectedCardId: Long? = null,
    val activePage: Int = 0,
    /** Extra explicitly-added empty sheets kept until removed. */
    val forcedPages: Int = 0,
    /** Zoom mode for the canvas: FIT (auto) or MANUAL (percent). */
    val zoomMode: ZoomMode = ZoomMode.FIT,
    val manualZoomPercent: Float = 100f,
    /** Enhance target scope: single card or all cards with images. */
    val enhanceScope: EnhanceScope = EnhanceScope.SINGLE,
    /** Non-null when applying enhancement with scope=ALL needs confirmation. */
    val pendingBulkPatch: EnhanceSettings? = null,
    /** Card long-pressed for the quick context sheet (430 ms hold). */
    val longPressSheetCardId: Long? = null,
    /** True while a bulk enhance pixel pass is running. */
    val processing: Boolean = false,
    val toast: TabatiToast? = null,
    /** Compare (un-enhanced) preview toggle. */
    val compareBefore: Boolean = false,
) {
    val selectedCard: CardItem? get() = cards.firstOrNull { it.id == selectedCardId }
    val pageCount: Int get() = maxOf((cards.maxOfOrNull { it.page } ?: -1) + 1, forcedPages).coerceAtLeast(1)
    val cardsOnPage: List<CardItem> get() = cards.filter { it.page == activePage }
}

enum class ZoomMode { FIT, MANUAL }
enum class EnhanceScope { SINGLE, ALL }

/**
 * «طبعتي — استوديو الطباعة الذكي» — حالة التطبيق كاملة.
 * كل الطفرات تمر عبر pushUndo() (ما عدا إعادة التعبئة التلقائية وعمليات السحب).
 * النوافذ الجانبية/الحوارات تقرأ من هنا — لا توجد حالة محلية مزدوجة.
 */
class TabatiViewModel : ViewModel() {

    private var idCounter = 100L
    private fun nextId(): Long = idCounter++

    private val _state = MutableStateFlow(TabatiUiState(theme = PlatformActions.loadTheme()))
    val state: StateFlow<TabatiUiState> = _state.asStateFlow()

    private val undoStack = ArrayDeque<TabatiUiState>()
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private fun pushUndo() {
        undoStack.addLast(_state.value.copy(toast = null))
        while (undoStack.size > 50) undoStack.removeFirst()
        _canUndo.value = true
    }

    fun undo() {
        val prev = undoStack.removeLastOrNull() ?: return
        _state.update { current ->
            prev.copy(theme = current.theme, section = current.section, toast = null)
        }
        _canUndo.value = undoStack.isNotEmpty()
    }

    private fun setProcessing(value: Boolean) = _state.update { it.copy(processing = value) }
    private fun toast(message: String) = _state.update { it.copy(toast = TabatiToast(message)) }
    fun consumeToast() = _state.update { it.copy(toast = null) }

    private fun repack(cards: List<CardItem>, strategy: String = "smart", preserveManual: Boolean = true): List<CardItem> =
        SheetArranger.arrange(cards, _state.value.settings, strategy, preserveManual).cards

    // ------------------------------------------------------------------ theme

    fun cycleTheme() = _state.update { s ->
        val next = when (s.theme) {
            AppTheme.SYSTEM -> AppTheme.LIGHT
            AppTheme.LIGHT -> AppTheme.DARK
            AppTheme.DARK -> AppTheme.SYSTEM
        }
        PlatformActions.saveTheme(next)
        s.copy(theme = next)
    }

    fun setSection(section: StudioSection) = _state.update { it.copy(section = section) }

    // ------------------------------------------------------------------ sheet config

    private fun updateSettings(transform: (SheetSettings) -> SheetSettings) {
        _state.update { s ->
            val next = transform(s.settings)
            val repackWith = next != s.settings
            s.copy(
                settings = next,
                cards = if (s.cards.isEmpty() || !repackWith) s.cards else repack(s.cards),
                activePage = 0,
            )
        }
    }

    fun setPaperPreset(key: String) = updateSettings { it.copy(presetKey = key) }

    fun setCustomPaper(widthMm: Float, heightMm: Float) =
        updateSettings { it.copy(presetKey = PaperPreset.CUSTOM.key, customWidthMm = widthMm, customHeightMm = heightMm) }

    fun setOrientation(orientation: Orientation) = updateSettings { it.copy(orientation = orientation) }
    fun setMargin(value: Float) = updateSettings { it.copy(marginMm = value.coerceIn(0f, 60f)) }
    fun setGap(value: Float) = updateSettings { it.copy(gapMm = value.coerceIn(0f, 30f)) }
    fun setMode(mode: ArrangeMode) = updateSettings { it.copy(mode = mode) }
    fun setGroupSmart(enabled: Boolean) = updateSettings { it.copy(groupSmart = enabled) }
    fun setAutoArrange(enabled: Boolean) = _state.update { it.copy(settings = it.settings.copy(autoArrangeOnAdd = enabled)) }
    fun setAllowMultiPage(enabled: Boolean) = _state.update { it.copy(settings = it.settings.copy(allowMultiPage = enabled)) }

    fun setFrame(transform: (SheetFrame) -> SheetFrame) =
        _state.update { it.copy(settings = it.settings.copy(frame = transform(it.settings.frame))) }

    fun setFrameBackground(background: SheetBackground) = setFrame { it.copy(background = background) }
    fun setFrameThickness(value: Float) = setFrame { it.copy(thickness = value.coerceIn(0.5f, 5f)) }
    fun setFrameColor(hex: Long) = setFrame { it.copy(colorHex = hex) }
    fun setFrameStyle(style: FrameStyle) = setFrame { it.copy(style = style) }
    fun toggleAllFrames() = setFrame { frame -> if (frame.allFramesOff) frame.copy(sheet = true, card = true, guides = true, ruler = true) else frame.copy(sheet = false, card = false, guides = false, ruler = false) }

    // ------------------------------------------------------------------ add / import

    /** Import image URIs — auto-detect the card type from aspect ratio (النمط العربي). */
    fun importImages(sources: List<String>) {
        if (sources.isEmpty()) return
        pushUndo()
        viewModelScope.launch {
            setProcessing(true)
            val imported = withContext(Dispatchers.IO) {
                sources.mapNotNull { source -> PlatformActions.importImage(source) }
            }
            if (imported.isEmpty()) {
                setProcessing(false)
                toast(Str.images_only)
                return@launch
            }
            val newCards = imported.map { img ->
                val detection = SheetArranger.detectFromRatio(img.widthPx.toFloat(), img.heightPx.toFloat())
                CardItem(
                    id = nextId(),
                    typeKey = detection.typeKey,
                    name = detection.name,
                    baseWidthMm = detection.widthMm,
                    baseHeightMm = detection.heightMm,
                    widthMm = detection.widthMm,
                    heightMm = detection.heightMm,
                    imageUri = img.privatePath,
                    enhance = EnhanceSettings.IDENTITY,
                    detectedLabel = detection.label,
                )
            }
            val combined = _state.value.cards + newCards
            val arranged = if (_state.value.settings.autoArrangeOnAdd) {
                SheetArranger.arrange(combined, _state.value.settings, "smart", preserveManual = true).cards
            } else combined
            _state.update {
                it.copy(
                    cards = arranged,
                    selectedCardId = newCards.first().id,
                    activePage = arranged.lastOrNull()?.page ?: 0,
                    section = StudioSection.SHEET,
                    processing = false,
                    toast = TabatiToast(Str.added_images.format(newCards.size)),
                )
            }
        }
    }

    fun importCapture(source: String) = importImages(listOf(source))

    /** One sample of every official card type — the «أمثلة تعليمية» strip. */
    fun addSamples() {
        pushUndo()
        val samples = listOf(
            CardType.NATIONAL, CardType.PASSPORT, CardType.RESIDENCE, CardType.RATION,
            CardType.DRIVING, CardType.VOTER, CardType.STUDENT,
        ).map { type ->
            CardItem(
                id = nextId(),
                typeKey = type.key,
                name = type.labelAr,
                baseWidthMm = type.widthMm,
                baseHeightMm = type.heightMm,
                widthMm = type.widthMm,
                heightMm = type.heightMm,
                isSample = true,
            )
        }
        _state.update {
            val combined = it.cards + samples
            val arranged = SheetArranger.arrange(combined, it.settings, "smart", preserveManual = true).cards
            it.copy(
                cards = arranged,
                selectedCardId = samples.first().id,
                section = StudioSection.SHEET,
                toast = TabatiToast(Str.added_samples),
            )
        }
    }

    fun addSampleOfType(type: CardType) = addSamplesOfType(listOf(type))

    fun addSamplesOfType(types: List<CardType>) {
        pushUndo()
        val cards = types.map { type ->
            CardItem(
                id = nextId(),
                typeKey = type.key,
                name = type.labelAr,
                baseWidthMm = type.widthMm,
                baseHeightMm = type.heightMm,
                widthMm = type.widthMm,
                heightMm = type.heightMm,
                isSample = true,
            )
        }
        _state.update {
            val arranged = SheetArranger.arrange(it.cards + cards, it.settings, "smart", preserveManual = true).cards
            it.copy(cards = arranged, selectedCardId = cards.first().id, section = StudioSection.SHEET)
        }
    }

    /** Add one (empty) sheet — kept until the user deletes it. */
    /** Auto-crop white scanner borders of the selected card's image, rewriting it in place. */
    fun autoCropSelected() {
        val id = _state.value.selectedCardId ?: return
        val path = _state.value.cards.firstOrNull { it.id == id }?.imageUri ?: return
        viewModelScope.launch {
            setProcessing(true)
            val newPath = withContext(Dispatchers.IO) { PlatformActions.autoCropImage(path, id) }
            setProcessing(false)
            if (newPath != null) {
                _state.update { s ->
                    s.copy(
                        cards = s.cards.map { if (it.id == id) it.copy(imageUri = newPath, enhancedImageUri = newPath) else it },
                        toast = TabatiToast(Str.crop_success),
                    )
                }
            } else {
                toast(Str.cropped_none)
            }
        }
    }

    fun addSheet() {
        pushUndo()
        _state.update {
            val next = it.pageCount
            it.copy(forcedPages = it.forcedPages + 1, activePage = next, toast = TabatiToast(Str.new_sheet))
        }
    }

    // ------------------------------------------------------------------ selection & editing

    fun selectCard(id: Long?) = _state.update { it.copy(selectedCardId = id, longPressSheetCardId = null) }
    /** Patch the selected card directly (e.g. border/guides toggles). */
    fun updateSelectedCard(transform: (CardItem) -> CardItem) = _state.update { s ->
        val id = s.selectedCardId ?: return@update s
        s.copy(cards = s.cards.map { if (it.id == id) transform(it) else it })
    }
    fun currentPage(page: Int) = _state.update { it.copy(activePage = page.coerceIn(0, (it.pageCount - 1).coerceAtLeast(0)), selectedCardId = null) }
    fun openLongPressSheet(id: Long) = _state.update { it.copy(longPressSheetCardId = id) }
    fun dismissLongPressSheet() = _state.update { it.copy(longPressSheetCardId = null) }
    fun toggleCompareBefore() = _state.update { it.copy(compareBefore = !it.compareBefore) }
    fun setZoomMode(mode: ZoomMode) = _state.update { it.copy(zoomMode = mode) }
    fun setManualZoom(percent: Float) = _state.update { it.copy(manualZoomPercent = percent.coerceIn(50f, 200f)) }
    fun zoomFit() = _state.update { it.copy(zoomMode = ZoomMode.FIT, manualZoomPercent = 100f) }
    fun zoomStep(deltaPercent: Float) = _state.update {
        it.copy(zoomMode = ZoomMode.MANUAL, manualZoomPercent = (it.manualZoomPercent + deltaPercent).coerceIn(50f, 200f))
    }

    /** Drag-to-move (mm), clamped inside the sheet. Does NOT push undo (matches design). */
    fun moveCard(id: Long, xMm: Float, yMm: Float) = _state.update { s ->
        val bound = if (s.settings.frame.guides) s.settings.marginMm else 0f
        s.copy(
            cards = s.cards.map { c ->
                if (c.id != id) c
                else c.copy(
                    xMm = xMm.coerceIn(bound, maxOf(bound, s.settings.sheetWidthMm - bound - c.widthMm)),
                    yMm = yMm.coerceIn(bound, maxOf(bound, s.settings.sheetHeightMm - bound - c.heightMm)),
                    manualPlacement = true,
                )
            },
        )
    }

    fun setCardAngle360(id: Long, rawAngle: Float, magneticSnap: Boolean = true): Float {
        var deg = ((rawAngle % 360f) + 360f) % 360f
        if (magneticSnap) {
            val snaps = listOf(0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f, 360f)
            for (snap in snaps) {
                if (kotlin.math.abs(deg - snap) <= 4.5f) {
                    deg = if (snap == 360f) 0f else snap
                    break
                }
            }
        }
        _state.update { s ->
            s.copy(
                cards = s.cards.map { c ->
                    if (c.id != id) return@map c
                    val baseW = c.baseWidthMm
                    val baseH = c.baseHeightMm
                    when (deg.toInt()) {
                        0, 180 -> c.copy(widthMm = baseW, heightMm = baseH, rotation = deg.toInt(), freeRotation = 0f, autoRotated = false)
                        90, 270 -> c.copy(widthMm = baseH, heightMm = baseW, rotation = deg.toInt(), freeRotation = 0f, autoRotated = false)
                        else -> c.copy(widthMm = baseW, heightMm = baseH, rotation = 0, freeRotation = deg, autoRotated = false)
                    }
                },
            )
        }
        return deg
    }

    /** Resize handle (mm), minimum 20 mm, never beyond sheet. No undo per event. */
    fun resizeCard(id: Long, widthMm: Float, heightMm: Float) = _state.update { s ->
        val bound = if (s.settings.frame.guides) s.settings.marginMm else 0f
        val maxW = maxOf(20f, s.settings.sheetWidthMm - bound - (s.cards.firstOrNull { it.id == id }?.xMm ?: 0f))
        val maxH = maxOf(20f, s.settings.sheetHeightMm - bound - (s.cards.firstOrNull { it.id == id }?.yMm ?: 0f))
        s.copy(
            cards = s.cards.map { c ->
                if (c.id != id) c
                else {
                    val w = widthMm.coerceIn(20f, maxW)
                    val h = heightMm.coerceIn(20f, maxH)
                    c.copy(baseWidthMm = w, baseHeightMm = h, widthMm = w, heightMm = h)
                }
            },
        )
    }

    fun renameCard(id: Long, name: String) {
        pushUndo()
        _state.update { s -> s.copy(cards = s.cards.map { if (it.id == id) it.copy(name = name.trim().ifEmpty { it.name }.take(80)) else it }) }
    }

    fun deleteCard(id: Long) {
        pushUndo()
        _state.update { s ->
            val next = s.cards.filterNot { it.id == id }
            val nextId = next.firstOrNull()?.id
            s.copy(
                cards = next,
                selectedCardId = if (s.selectedCardId == id) nextId else s.selectedCardId,
                longPressSheetCardId = if (s.longPressSheetCardId == id) null else s.longPressSheetCardId,
                activePage = if (next.isEmpty()) 0 else s.activePage.coerceIn(0, maxOf(0, next.maxOf { it.page })),
                forcedPages = if (next.isEmpty()) 0 else s.forcedPages,
            )
        }
    }

    fun duplicateCard(id: Long) {
        pushUndo()
        _state.update { s ->
            val src = s.cards.firstOrNull { it.id == id } ?: return@update s
            val copy = src.copy(id = nextId(), manualPlacement = false)
            val combined = s.cards + copy
            val arranged = if (s.settings.autoArrangeOnAdd) SheetArranger.arrange(combined, s.settings, "smart", preserveManual = true).cards else combined
            s.copy(cards = arranged, selectedCardId = copy.id)
        }
    }

    fun swapCardsPositions(idA: Long, idB: Long) {
        if (idA == idB) return
        pushUndo()
        _state.update { s ->
            val a = s.cards.firstOrNull { it.id == idA } ?: return@update s
            val b = s.cards.firstOrNull { it.id == idB } ?: return@update s
            s.copy(
                cards = s.cards.map { c ->
                    when (c.id) {
                        idA -> c.copy(xMm = b.xMm, yMm = b.yMm, page = b.page, manualPlacement = true)
                        idB -> c.copy(xMm = a.xMm, yMm = a.yMm, page = a.page, manualPlacement = true)
                        else -> c
                    }
                },
                activePage = b.page,
                longPressSheetCardId = null,
            )
        }
    }

    fun moveCardToPage(id: Long, page: Int) {
        pushUndo()
        _state.update { s ->
            val card = s.cards.firstOrNull { it.id == id } ?: return@update s
            val target = page.coerceIn(0, (s.pageCount - 1).coerceAtLeast(0) + 1)
            val onTarget = s.cards.filter { it.page == target && it.id != id }
            val lowestBottom = onTarget.maxOfOrNull { it.yMm + it.heightMm } ?: s.settings.marginMm
            val nextY = (lowestBottom + s.settings.effectiveGapMm).coerceAtMost(maxOf(s.settings.marginMm, s.settings.sheetHeightMm - s.settings.marginMm - card.heightMm))
            s.copy(
                cards = s.cards.map { if (it.id == id) it.copy(page = target, xMm = s.settings.marginMm, yMm = nextY, manualPlacement = true) else it },
                activePage = target,
                longPressSheetCardId = null,
            )
        }
    }

    fun replaceCardImage(id: Long, uri: String) {
        pushUndo()
        _state.update { s ->
            s.copy(
                cards = s.cards.map {
                    if (it.id == id) it.copy(imageUri = uri, enhancedImageUri = uri, enhance = EnhanceSettings.IDENTITY) else it
                },
                longPressSheetCardId = null,
            )
        }
    }

    // ------------------------------------------------------------------ enhance

    fun setEnhanceScope(scope: EnhanceScope) = _state.update { it.copy(enhanceScope = scope) }

    /**
     * Update the selected card's enhance via [patch]. When scope is ALL and more
     * than one image card exists, the whole patch is deferred behind the
     * confirmation dialog (then applied to every image card identically).
     */
    fun updateSelectedEnhance(patch: (EnhanceSettings) -> EnhanceSettings) {
        val imageCards = _state.value.cards.count { it.imageUri != null }
        if (_state.value.enhanceScope == EnhanceScope.ALL && imageCards > 1) {
            val base = _state.value.selectedCard?.enhance ?: EnhanceSettings.IDENTITY
            _state.update { it.copy(pendingBulkPatch = patch(base)) }
            return
        }
        val selectedId = _state.value.selectedCardId ?: return
        _state.update { s ->
            s.copy(cards = s.cards.map { if (it.id == selectedId) it.copy(enhance = patch(it.enhance)) else it })
        }
    }

    fun confirmPendingBulkEnhance() {
        val patch = _state.value.pendingBulkPatch ?: return
        _state.update { it.copy(pendingBulkPatch = null) }
        _state.update { s -> s.copy(cards = s.cards.map { if (it.imageUri != null) it.copy(enhance = patch) else it }) }
        // Real pixel pass happens on export; the preview ColorFilter approximates it instantly.
        toast(Str.enhance_applied_all.format(_state.value.cards.count { it.imageUri != null }))
    }

    fun dismissPendingBulk() = _state.update { it.copy(pendingBulkPatch = null) }

    /** «تحسين تلقائي» — scanner preset on top of the selected card. */
    fun autoEnhanceSelected() = updateSelectedEnhance { current ->
        current.copy(
            brightness = EnhanceSettings.AUTO.brightness,
            contrast = EnhanceSettings.AUTO.contrast,
            saturation = EnhanceSettings.AUTO.saturation,
            sharpness = EnhanceSettings.AUTO.sharpness,
        )
    }

    fun resetSelectedEnhance() = updateSelectedEnhance { EnhanceSettings.IDENTITY }

    fun resetAllEnhance() {
        _state.update { s -> s.copy(cards = s.cards.map { it.copy(enhance = EnhanceSettings.IDENTITY) }, toast = TabatiToast(Str.enhance_reset_all)) }
    }

    // ------------------------------------------------------------------ export / print / share

    /** «زر PDF» — builds the 300 DPI PDF and saves it straight to the user's Downloads folder. */
    fun downloadPdf() {
        viewModelScope.launch {
            val s = _state.value
            if (s.cards.isEmpty()) { toast(Str.empty_for_export); return@launch }
            setProcessing(true)
            val bytes = withContext(Dispatchers.IO) {
                PlatformActions.exportPdf(s.cards, s.settings, s.pageCount)
            }
            setProcessing(false)
            if (bytes == null || !PlatformActions.downloadToDownloads("طبعتي-${s.settings.presetKey}.pdf", bytes)) {
                toast(Str.empty_for_export)
                return@launch
            }
            toast(Str.pdf_saved.format(s.pageCount, s.settings.sheetWidthMm.toInt(), s.settings.sheetHeightMm.toInt()))
        }
    }

    /** «طباعة مباشرة» — hands the PDF straight to the system print service. */
    fun printPdf() {
        viewModelScope.launch {
            val s = _state.value
            if (s.cards.isEmpty()) { toast(Str.empty_for_export); return@launch }
            setProcessing(true)
            val bytes = withContext(Dispatchers.IO) {
                PlatformActions.exportPdf(s.cards, s.settings, s.pageCount)
            }
            if (bytes == null) { setProcessing(false); toast(Str.empty_for_export); return@launch }
            val accepted = PlatformActions.printPdf(
                pdfBytes = bytes,
                pageCount = s.pageCount,
                sheetWidthMm = s.settings.sheetWidthMm,
                sheetHeightMm = s.settings.sheetHeightMm,
                jobName = "طبعتي-${s.settings.presetKey}",
            )
            setProcessing(false)
            if (accepted) {
                toast(Str.print_accepted)
            } else {
                // print service unavailable — save instead so the user still gets the file
                withContext(Dispatchers.IO) {
                    PlatformActions.downloadToDownloads("طبعتي-${s.settings.presetKey}.pdf", bytes)
                }
                toast(Str.print_fallback)
            }
        }
    }

    /** «مشاركة للتطبيقات» — the PDF as a file through the system share sheet. */
    fun sharePdf() {
        viewModelScope.launch {
            val s = _state.value
            if (s.cards.isEmpty()) { toast(Str.empty_for_export); return@launch }
            setProcessing(true)
            val bytes = withContext(Dispatchers.IO) {
                PlatformActions.exportPdf(s.cards, s.settings, s.pageCount)
            }
            if (bytes == null) { setProcessing(false); toast(Str.empty_for_export); return@launch }
            val path = withContext(Dispatchers.IO) {
                PlatformActions.writeExport("طبعتي-${s.settings.presetKey}.pdf", bytes)
            } ?: run { setProcessing(false); toast(Str.empty_for_export); return@launch }
            setProcessing(false)
            withContext(Dispatchers.IO) {
                PlatformActions.shareFile(path, "application/pdf", subject = "طبعتي — ملف جاهز للطباعة")
            }
        }
    }

    /** Save one page as PNG into Downloads. */
    fun downloadPng(page: Int) {
        viewModelScope.launch {
            val s = _state.value
            if (s.cards.isEmpty()) { toast(Str.empty_for_export); return@launch }
            setProcessing(true)
            val bytes = withContext(Dispatchers.IO) {
                PlatformActions.exportPng(page, s.cards, s.settings)
            }
            setProcessing(false)
            if (bytes == null) { toast(Str.png_saved.format(0)); return@launch }
            val ok = withContext(Dispatchers.IO) {
                PlatformActions.downloadToDownloads("طبعتي-ورقة-${page + 1}.png", bytes)
            }
            toast(if (ok) Str.png_saved.format(page + 1) else Str.empty_for_export)
        }
    }

    /** Save every page as PNG into Downloads (one toast, no chooser spam). */
    fun downloadAllPngs() {
        viewModelScope.launch {
            val s = _state.value
            if (s.cards.isEmpty()) { toast(Str.empty_for_export); return@launch }
            setProcessing(true)
            var saved = 0
            for (page in 0 until s.pageCount) {
                val bytes = withContext(Dispatchers.IO) {
                    PlatformActions.exportPng(page, s.cards, s.settings)
                } ?: continue
                val ok = withContext(Dispatchers.IO) {
                    PlatformActions.downloadToDownloads("طبعتي-ورقة-${page + 1}.png", bytes)
                }
                if (ok) saved++
            }
            setProcessing(false)
            toast(if (saved > 0) Str.png_all_saved.format(saved) else Str.empty_for_export)
        }
    }

    /** Write the standalone HTML print file into Downloads. */
    fun downloadHtml() {
        viewModelScope.launch {
            val s = _state.value
            if (s.cards.isEmpty()) { toast(Str.empty_for_export); return@launch }
            val html = withContext(Dispatchers.IO) {
                val pages = (0 until s.pageCount).map { p -> s.cards.filter { it.page == p } }
                com.appprint.studio.core.export.HtmlExport.build(pages, s.settings.sheetWidthMm, s.settings.sheetHeightMm, s.settings.marginMm, s.settings.frame) { card ->
                    card.imageUri?.let { PlatformActions.imageAsDataUri(it) } ?: ""
                }
            }
            val ok = withContext(Dispatchers.IO) {
                PlatformActions.downloadToDownloads("طبعتي-للطباعة.html", html)
            }
            toast(if (ok) Str.html_saved else Str.empty_for_export)
        }
    }

    // ------------------------------------------------------------------ pages

    fun deletePageKeepCards(page: Int) {
        pushUndo()
        _state.update { s ->
            val target = page.coerceIn(0, s.pageCount - 1)
            val countOnPage = s.cards.count { it.page == target }
            val shifted = s.cards.map { c ->
                when {
                    c.page == target -> c.copy(page = maxOf(0, target - 1), manualPlacement = false)
                    c.page > target -> c.copy(page = c.page - 1)
                    else -> c
                }
            }
            val repacked = SheetArranger.arrange(shifted, s.settings, "smart", preserveManual = true).cards
            if (countOnPage == 0) {
                s.copy(
                    cards = s.cards,
                    forcedPages = maxOf(0, s.forcedPages - 1),
                    activePage = if (target <= s.activePage) maxOf(0, s.activePage - 1) else s.activePage,
                    toast = TabatiToast(Str.deleted_page_empty),
                )
            } else {
                s.copy(
                    cards = repacked,
                    activePage = if (target <= s.activePage) maxOf(0, s.activePage - 1) else s.activePage,
                    toast = TabatiToast(Str.deleted_page_keep.format(countOnPage)),
                )
            }
        }
    }

    fun deletePageWithCards(page: Int) {
        pushUndo()
        _state.update { s ->
            val target = page.coerceIn(0, s.pageCount - 1)
            val countOnPage = s.cards.count { it.page == target }
            val remaining = s.cards
                .filterNot { it.page == target }
                .map { if (it.page > target) it.copy(page = it.page - 1) else it }
            s.copy(
                cards = remaining,
                forcedPages = if (remaining.isEmpty()) 0 else s.forcedPages,
                activePage = if (target <= s.activePage) maxOf(0, s.activePage - 1) else s.activePage.coerceAtLeast(0),
                selectedCardId = null,
                toast = TabatiToast(Str.deleted_page_cards.format(countOnPage)),
            )
        }
    }

    fun clearAll() {
        pushUndo()
        _state.update {
            it.copy(
                cards = emptyList(),
                selectedCardId = null,
                activePage = 0,
                forcedPages = 0,
                zoomMode = ZoomMode.FIT,
                manualZoomPercent = 100f,
                section = StudioSection.SHEET,
                toast = TabatiToast(Str.cleared),
            )
        }
    }

    fun arrangeSmart() {
        pushUndo()
        _state.update { s ->
            if (s.cards.isEmpty()) s.copy(toast = TabatiToast(Str.empty_for_export))
            else {
                val arranged = SheetArranger.arrange(s.cards, s.settings, "smart", preserveManual = false).cards
                s.copy(cards = arranged, toast = TabatiToast(Str.arrange_done.format(arranged.maxOf { it.page } + 1)))
            }
        }
    }

    fun sortBySize() {
        pushUndo()
        _state.update { s ->
            if (s.cards.isEmpty()) s.copy(toast = TabatiToast(Str.empty_for_export))
            else {
                val arranged = SheetArranger.arrange(s.cards, s.settings, "size", preserveManual = false).cards
                s.copy(cards = arranged, toast = TabatiToast(Str.arrange_size.format(arranged.maxOf { it.page } + 1)))
            }
        }
    }
}
