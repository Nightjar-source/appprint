package com.appprint.studio.core.model

/**
 * Core models for «طبعتي — استوديو الطباعة الذكي» — UI-free, pure Kotlin,
 * ported 1:1 from the approved HTML design (CARD_TYPES / PAPER_SIZES /
 * DEFAULT_ENHANCE / DEFAULT_FRAME). Safe to unit-test on the JVM.
 *
 * All physical dimensions are millimetres, origin = sheet top-left.
 */

/** Official Iraqi card catalogue. Dimensions in millimetres. */
enum class CardType(
    val key: String,
    val labelAr: String,
    val shortAr: String,
    val widthMm: Float,
    val heightMm: Float,
    val brandColorHex: Long,
) {
    NATIONAL("national", "البطاقة الوطنية الموحدة", "وطنية", 85.6f, 54f, 0xFF006C47L),
    PASSPORT("passport", "جواز السفر", "جواز", 88f, 125f, 0xFF16324FL),
    RESIDENCE("residence", "بطاقة السكن", "سكن", 100f, 70f, 0xFF8A5B12L),
    RATION("ration", "البطاقة التموينية", "تموينية", 140f, 100f, 0xFF910824L),
    DRIVING("driving", "إجازة السوق", "سوق", 85.6f, 54f, 0xFF1F4B7AL),
    VOTER("voter", "بطاقة الناخب", "ناخب", 85.6f, 54f, 0xFF4B2D73L),
    STUDENT("student", "هوية الطالب", "طالب", 85.6f, 54f, 0xFF276B68L),
    CUSTOM("custom", "قياس مخصص", "مخصص", 90f, 60f, 0xFF3F4943L);

    companion object {
        val PRINTABLE: List<CardType> = entries
        fun fromKey(key: String): CardType = entries.firstOrNull { it.key == key } ?: CUSTOM
    }
}

/** Guide-tab reference per card type (from the design's CARD_GUIDE_INFO). */
data class CardGuideInfo(
    val useAr: String,
    val issuerAr: String,
    val sizeNoteAr: String,
    val tipsAr: List<String>,
)

val CardGuides: Map<CardType, CardGuideInfo> = mapOf(
    CardType.NATIONAL to CardGuideInfo(
        "إثبات الهوية الشخصية وتُستخدم في جميع المعاملات الرسمية.",
        "مديرية الأحوال المدنية والجوازات والإقامة",
        "85.6 × 54 مم — قياس ID-1",
        listOf("امسح الوجهين بنفس الدقة", "لا تقص داخل الحافة الملونة", "استخدم ورق مقوّى 250 غرام"),
    ),
    CardType.PASSPORT to CardGuideInfo(
        "وثيقة سفر رسمية تُطبع نسخة واضحة منها للمعاملات.",
        "مديرية الجوازات",
        "88 × 125 مم — قياس B7",
        listOf("حافظ على صفحة البيانات كاملة", "لا تغطِّ رقم الجواز", "اطبع على ورق أبيض غير لامع"),
    ),
    CardType.RESIDENCE to CardGuideInfo(
        "إثبات عنوان السكن الحالي للمعاملات الحكومية.",
        "مكتب المعلومات / المجلس المحلي",
        "100 × 70 مم — قياس شائع",
        listOf("تأكد من وضوح العنوان والختم", "قصّ على الخط المتقطع تماماً"),
    ),
    CardType.RATION to CardGuideInfo(
        "صرف المفردات التموينية وتدقيق بيانات العائلة.",
        "وزارة التجارة",
        "140 × 100 مم — قياس شائع",
        listOf("اطبع الوجهين بصفحة واحدة إن أمكن", "لا تقص جدول المواد"),
    ),
    CardType.DRIVING to CardGuideInfo(
        "إثبات أهلية قيادة المركبات.",
        "مديرية المرور العامة",
        "85.6 × 54 مم — قياس ID-1",
        listOf("تأكد من وضوح فئة الإجازة", "تجنب الانعكاس عند التصوير"),
    ),
    CardType.VOTER to CardGuideInfo(
        "المشاركة في الانتخابات والتحقق من بيانات الناخب.",
        "المفوضية العليا المستقلة للانتخابات",
        "85.6 × 54 مم — قياس ID-1",
        listOf("حافظ على الباركود واضحاً", "لا تطوِ البطاقة بعد القص"),
    ),
    CardType.STUDENT to CardGuideInfo(
        "التعريف بالهوية الجامعية أو المدرسية.",
        "الجامعة / وزارة التربية",
        "85.6 × 54 مم — قياس ID-1",
        listOf("استخدم صورة حديثة وواضحة", "اطبع على ورق مقوّى"),
    ),
    CardType.CUSTOM to CardGuideInfo(
        "أي بطاقة أو مستند بأبعاد خاصة.",
        "حسب نوع المستند",
        "حسب القياس المدخل بالملمتر",
        listOf("قس البطاقة بالمسطرة قبل الإدخال", "جرّب طباعة تجريبية أولاً"),
    ),
)

/** Official paper sizes (mm). `custom` uses [SheetSettings.customWidthMm]/[customHeightMm]. */
enum class PaperPreset(val key: String, val label: String, val widthMm: Float, val heightMm: Float) {
    A4("A4", "A4", 210f, 297f),
    A5("A5", "A5", 148f, 210f),
    A3("A3", "A3", 297f, 420f),
    LETTER("Letter", "Letter", 215.9f, 279.4f),
    LEGAL("Legal", "Legal", 215.9f, 355.6f),
    CUSTOM("custom", "مخصص", 210f, 297f);

    companion object {
        val PRESETS: List<PaperPreset> = entries
        fun fromKey(key: String): PaperPreset = entries.firstOrNull { it.key == key } ?: A4
    }
}

enum class Orientation { PORTRAIT, LANDSCAPE }

/** Theme selection for the whole app (design: auto/light/dark, kept as «تلقائي/فاتح/داكن»). */
enum class AppTheme { SYSTEM, LIGHT, DARK }

/** Packing strategy from the design. COMFORTABLE = smart pocket packing (default), COMPACT = dense, GRID = uniform cells. */
enum class ArrangeMode { COMFORTABLE, COMPACT, GRID }

enum class FrameStyle { SOLID, DASHED, DOTTED }

enum class SheetBackground { WHITE, OFFWHITE, TRANSPARENT }

/** Enhancement mode — from DEFAULT_ENHANCE (color / gray / bw document modes). */
enum class EnhanceMode { COLOR, GRAY, BW }

/**
 * Scanner-like enhance, values in *percent of identity* (100 = untouched) —
 * matches the HTML DEFAULT_ENHANCE contract exactly.
 */
data class EnhanceSettings(
    val brightness: Float = 100f,
    val contrast: Float = 100f,
    val saturation: Float = 100f,
    val sharpness: Float = 100f,
    val mode: EnhanceMode = EnhanceMode.COLOR,
    val shadowLift: Boolean = false,
    /** Small tilt/deskew, degrees. */
    val tilt: Float = 0f,
    /** Percent of area removed by auto-crop (display only). */
    val cropPercent: Int = 0,
) {
    val isIdentity: Boolean get() =
        brightness == 100f && contrast == 100f && saturation == 100f && sharpness == 100f &&
            mode == EnhanceMode.COLOR && !shadowLift && tilt == 0f && cropPercent == 0

    companion object {
        val IDENTITY = EnhanceSettings()
        /** «تحسين تلقائي» scanner preset from the design. */
        val AUTO = EnhanceSettings(brightness = 108f, contrast = 118f, saturation = 106f, sharpness = 110f)
    }
}

/** Frame + print chrome of the sheet (from DEFAULT_FRAME). */
data class SheetFrame(
    val sheet: Boolean = true,
    val card: Boolean = true,
    val guides: Boolean = true,
    val ruler: Boolean = true,
    val background: SheetBackground = SheetBackground.WHITE,
    /** Border thickness multiplier (strike is `thickness * 4` px at 300 DPI). */
    val thickness: Float = 1f,
    val colorHex: Long = 0xFF111111L,
    val style: FrameStyle = FrameStyle.SOLID,
) {
    val allFramesOff: Boolean get() = !sheet && !card && !guides && !ruler
}

/** A single card placed (or to place) on a sheet. Coordinates in millimetres. */
data class CardItem(
    val id: Long,
    val typeKey: String,
    val name: String,
    val baseWidthMm: Float,
    val baseHeightMm: Float,
    val widthMm: Float,
    val heightMm: Float,
    val xMm: Float = 0f,
    val yMm: Float = 0f,
    val page: Int = 0,
    /** Right-angle orientation only: 0 / 90 / 180 / 270. */
    val rotation: Int = 0,
    /** Free 360° rotation (magnetic-snapped). */
    val freeRotation: Float = 0f,
    val imageUri: String? = null,
    val enhancedImageUri: String? = null,
    val manualPlacement: Boolean = false,
    val autoRotated: Boolean = false,
    val autoShrunk: Boolean = false,
    val border: Boolean = true,
    val guides: Boolean = true,
    val enhance: EnhanceSettings = EnhanceSettings.IDENTITY,
    val isSample: Boolean = false,
    /** Scanner auto-detected type label (non-empty when detection ran). */
    val detectedLabel: String = "",
) {
    val type: CardType get() = CardType.fromKey(typeKey)

    /** Total rotation for rendering (right-angle part). */
    val totalRotation: Float get() = (((rotation + freeRotation) % 360f) + 360f) % 360f

    /** Footprint width/height after right-angle rotation. */
    val footprintWidth: Float get() = if (rotation % 180 != 0) baseHeightMm else baseWidthMm
    val footprintHeight: Float get() = if (rotation % 180 != 0) baseWidthMm else baseHeightMm

    /** Effective footprint used for packing (considers free rotation = axis-aligned box). */
    val effectiveWidth: Float get() = widthMm
    val effectiveHeight: Float get() = heightMm
}

/** Sheet (paper) configuration. */
data class SheetSettings(
    val presetKey: String = PaperPreset.A4.key,
    val customWidthMm: Float = 210f,
    val customHeightMm: Float = 297f,
    val orientation: Orientation = Orientation.PORTRAIT,
    val marginMm: Float = 10f,
    val gapMm: Float = 4f,
    val mode: ArrangeMode = ArrangeMode.COMPACT,
    val groupSmart: Boolean = true,
    val autoArrangeOnAdd: Boolean = true,
    val allowMultiPage: Boolean = true,
    val frame: SheetFrame = SheetFrame(),
) {
    private val base: PaperPreset get() = PaperPreset.fromKey(presetKey)

    val baseWidthMm: Float get() = if (presetKey == PaperPreset.CUSTOM.key) customWidthMm else base.widthMm
    val baseHeightMm: Float get() = if (presetKey == PaperPreset.CUSTOM.key) customHeightMm else base.heightMm

    val sheetWidthMm: Float get() = if (orientation == Orientation.PORTRAIT) minOf(baseWidthMm, baseHeightMm) else maxOf(baseWidthMm, baseHeightMm)
    val sheetHeightMm: Float get() = if (orientation == Orientation.PORTRAIT) maxOf(baseWidthMm, baseHeightMm) else minOf(baseWidthMm, baseHeightMm)

    /** Comfortable packing enforces a roomier gap; compact packs denser. */
    val effectiveGapMm: Float get() = when (mode) {
        ArrangeMode.COMFORTABLE -> maxOf(gapMm, 8f)
        ArrangeMode.COMPACT -> gapMm
        ArrangeMode.GRID -> gapMm
    }
}
