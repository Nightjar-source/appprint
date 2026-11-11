package com.appprint.studio.core.model

/**
 * UI-free core models. No Compose, no Android UI types — safe to unit-test on the JVM.
 * Catalog data (dimensions, issuers, usage notes, tips) is ported 1:1 from
 * «ستوديو الطباعة.html.html» to keep the print measurements legally accurate.
 */

/** Official Iraqi card catalogue. All dimensions in millimetres. */
enum class CardType(
    val key: String,
    val labelAr: String,
    val shortAr: String,
    val widthMm: Float,
    val heightMm: Float,
    val brandColorHex: Long,
    val useAr: String,
    val issuerAr: String,
    val sizeNoteAr: String,
    val tipsAr: List<String>,
) {
    NATIONAL(
        "national", "البطاقة الوطنية الموحدة", "وطنية", 85.6f, 54f, 0xFF0D5B3AL,
        "إثبات الهوية والجنسية العراقية، وتُستخدم بكل المعاملات الرسمية.",
        "مديرية الأحوال المدنية والجوازات والإقامة",
        "85.6 × 54 مم — قياس ID-1",
        listOf("امسح الوجهين بنفس الدقة", "لا تقص داخل الحافة الملونة", "استخدم ورق مقوّى 250 غرام"),
    ),
    PASSPORT(
        "passport", "جواز السفر", "جواز", 88f, 125f, 0xFF16324FL,
        "وثيقة سفر رسمية، وتُطبع نسخة واضحة عند الحاجة للمعاملات.",
        "مديرية الجوازات",
        "88 × 125 مم — قياس B7",
        listOf("حافظ على صفحة البيانات كاملة", "لا تغطِّ رقم الجواز", "اطبع على ورق أبيض غير لامع"),
    ),
    RESIDENCE(
        "residence", "بطاقة السكن", "سكن", 100f, 70f, 0xFF8A5B12L,
        "إثبات عنوان السكن الحالي، وتُطلب بكثير من المعاملات.",
        "المجلس المحلي / الجهة المختصة",
        "100 × 70 مم — قياس تقريبي شائع",
        listOf("تأكد من وضوح العنوان", "قص على الخط المتقطع تماماً"),
    ),
    RATION(
        "ration", "البطاقة التموينية", "تموينية", 140f, 100f, 0xFF7A2440L,
        "صرف المواد الغذائية والتحقق من بيانات العائلة.",
        "وزارة التجارة",
        "140 × 100 مم — قياس تقريبي شائع",
        listOf("اطبع الوجهين بصفحة واحدة إن أمكن", "لا تقص جدول المواد"),
    ),
    DRIVING(
        "driving", "إجازة السوق", "سوق", 85.6f, 54f, 0xFF1F4B7AL,
        "إثبات أهلية قيادة المركبات داخل العراق.",
        "مديرية المرور العامة",
        "85.6 × 54 مم — قياس ID-1",
        listOf("تأكد من وضوح فئة الإجازة", "تجنب الانعكاس عند التصوير"),
    ),
    VOTER(
        "voter", "بطاقة الناخب", "ناخب", 85.6f, 54f, 0xFF4B2D73L,
        "المشاركة بالانتخابات والتحقق من مركز الاقتراع.",
        "المفوضية العليا المستقلة للانتخابات",
        "85.6 × 54 مم — قياس ID-1",
        listOf("حافظ على الباركود واضحاً", "لا تطوِ البطاقة بعد القص"),
    ),
    STUDENT(
        "student", "هوية الطالب", "طالب", 85.6f, 54f, 0xFF276B68L,
        "إثبات صفة الطالب داخل المدرسة أو الجامعة.",
        "المدرسة / الجامعة",
        "85.6 × 54 مم — قياس ID-1",
        listOf("استخدم صورة حديثة وواضحة", "اطبع على ورق مقوّى"),
    ),
    CUSTOM(
        "custom", "قياس مخصص", "مخصص", 90f, 60f, 0xFF333333L,
        "أي بطاقة أو مستند آخر تريد ترتيبه بنفس الطريقة.",
        "حسب نوع المستند",
        "حسب القياس الذي تدخله",
        listOf("قس البطاقة بالمسطرة قبل الإدخال", "جرّب طباعة تجريبية أولاً"),
    );

    companion object {
        val PRINTABLE: List<CardType> = entries
        fun fromKey(key: String): CardType = entries.firstOrNull { it.key == key } ?: CUSTOM
    }
}

/** Paper presets. Millimetres. */
enum class PaperPreset(val key: String, val label: String, val widthMm: Float, val heightMm: Float) {
    A4("A4", "A4", 210f, 297f),
    A5("A5", "A5", 148f, 210f),
    A3("A3", "A3", 297f, 420f),
    LETTER("letter", "Letter", 215.9f, 279.4f),
    LEGAL("legal", "Legal", 215.9f, 355.6f);

    companion object {
        fun fromKey(key: String): PaperPreset = entries.firstOrNull { it.key == key } ?: A4
    }
}

enum class ArrangeMode { GRID, COMFORTABLE }
enum class SheetBackground { WHITE, OFFWHITE, TRANSPARENT }
enum class FrameStyle { SOLID, DASHED, DOTTED }
enum class AppTheme { SYSTEM, LIGHT, DARK }

/** Image enhancement, values in [-100, 100] except flags. */
data class EnhanceSettings(
    val brightness: Int = 0,
    val contrast: Int = 0,
    val saturation: Int = 0,
    val grayscale: Boolean = false,
    val sharpness: Int = 0,
) {
    val isIdentity: Boolean get() =
        brightness == 0 && contrast == 0 && saturation == 0 && !grayscale && sharpness == 0

    companion object {
        val IDENTITY = EnhanceSettings()
        /** Scanner-like preset from the HTML «تحسين تلقائي». */
        val AUTO = EnhanceSettings(brightness = 8, contrast = 14, saturation = 6, sharpness = 10)
    }
}

/** A card placed (or to place) on a sheet. Coordinates in millimetres, origin = sheet top-left. */
data class CardItem(
    val id: Long,
    val typeKey: String,
    val name: String,
    val widthMm: Float,
    val heightMm: Float,
    val xMm: Float = 0f,
    val yMm: Float = 0f,
    val page: Int = 0,
    val rotation: Int = 0,          // 0 or 90
    val imageUri: String? = null,
    val enhance: EnhanceSettings = EnhanceSettings.IDENTITY,
    val border: Boolean = true,
    val guides: Boolean = true,
    val isSample: Boolean = false,
) {
    val type: CardType get() = CardType.fromKey(typeKey)

    /** Effective footprint after user rotation. */
    val effectiveWidth: Float get() = if (rotation == 90) heightMm else widthMm
    val effectiveHeight: Float get() = if (rotation == 90) widthMm else heightMm
}

/** Sheet (page) configuration. */
data class SheetSettings(
    val presetKey: String = PaperPreset.A4.key,
    val customWidthMm: Float = 210f,
    val customHeightMm: Float = 297f,
    val landscape: Boolean = false,
    val marginMm: Float = 10f,
    val gapMm: Float = 6f,
    val mode: ArrangeMode = ArrangeMode.COMFORTABLE,
    val smartGroup: Boolean = true,
    val background: SheetBackground = SheetBackground.WHITE,
    val showGuides: Boolean = true,
    val showFrames: Boolean = true,
    val frameStyle: FrameStyle = FrameStyle.DASHED,
    val frameColorHex: Long = 0xFF151816L,
    val frameWidthDp: Float = 1.5f,
) {
    private val base: PaperPreset?
        get() = PaperPreset.entries.firstOrNull { it.key == presetKey }

    val baseWidthMm: Float get() = base?.widthMm ?: customWidthMm
    val baseHeightMm: Float get() = base?.heightMm ?: customHeightMm

    /** Effective sheet size in mm for the current orientation. */
    val sheetWidthMm: Float get() = if (landscape) maxOf(baseWidthMm, baseHeightMm) else minOf(baseWidthMm, baseHeightMm)
    val sheetHeightMm: Float get() = if (landscape) minOf(baseWidthMm, baseHeightMm) else maxOf(baseWidthMm, baseHeightMm)

    /** Comfortable mode enforces a roomier gap. */
    val effectiveGapMm: Float get() = if (mode == ArrangeMode.COMFORTABLE) maxOf(gapMm, 8f) else gapMm

    val isCustom: Boolean get() = base == null
}
