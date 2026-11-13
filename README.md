# مطبعة البطاقات العراقية · AppPrint

> **IRAQI ID PRINT STUDIO** — قصّ، رتّب، اطبع بقياس حقيقي **1:1** — بدون ما يتغيّر حجم البطاقة.

تطبيق احترافي لترتيب الهويات العراقية وبطاقاتها (وطنية، جواز، إجازة سوق، بطاقة ناخب، سكن، تموينية، هوية طالب) على صفحة الطباعة، وطباعتها **بقياس حقيقي** داخل **ملف PDF** جاهز للطباعة، بجودة **300 DPI** ومقاسات حقيقية بالملمتر — حرفياً كما طُبعت البطاقة الأصلية.

**بطاقات:** ID-1 ‏(85.6 × 54 مم) · جواز (88 × 125 مم، B7) · سكن (100 × 70) · تموينية (140 × 100) · مخصص بأي قياس تدخله.

## المزايا

- 🃏 **٨ أنواع بطاقات عراقية** بمقاساتها الرسمية والجهة المصدرة وطريقة التعامل مع كل واحدة (شرح مدمج).
- 📐 **قياس حقيقي 1:1**: Sheet = ورقة A4/A5/A3/Letter/Legal/مخصص (طولي/عرضي)، هوامش وفراغات وشبكة أو مريح + تجميع ذكي حسب النوع وإعادة تجميع تلقائية وترتيب حسب الحجم.
- 🖼️ **تحسين احترافي للصور**: سطوع/تباين/تشبع/حدة/أبيض وأسود + تحسين تلقائي بنقرة وإزالة الحواف البيضاء (سكانر) تلقائياً + مقارنة قبل/بعد، تطبيقه على الكل.
- 🖐️ **سحب أي بطاقة** ووضعها في مكانها يدوياً، تدوير 90°، تكرار، حذف، تحديد.
- 🖨️ **طباعة أصلية**: PDF يُنشأ داخل التطبيق (hand-rolled writer) بجودة 300 DPI حقيقي، PNG لكل ورقة، طباعة نظامية عبر PrintManager بقياس 1:1، مشاركة عبر FileProvider.
- 🔎 **معاينة حية تفاعلية**: تكبير بالإصبع + تحريك + خطوط قص متقطعة حمراء + إطارات وقوالب قابلة للتخصيص (لون/سماكة/نمط متصل/متقطع/منقّط) + خلفية بيضاء/أوف وايت/شفافة.
- 🌓 **ثيم داكن/فاتح/تلقائي** (Material 3 Expressive + خطوتان في المظهر) — مصمم من لوحة: حبر مطبعة عميق + أخضر وأحمر عراقي + ذهبي عتيق + ورق دافئ.
- 🌍 **RTL عربي كامل** + نسخة إنجليزية (`values-en`) · شاشات متكيفة: هاتف (bottom bar + bottom sheets) / لوح (rail) / سطح مكتب (3 أعمدة: إعدادات | لوح | مفتش).
- 🔒 **خصوصية كاملة**: كل شيء داخل جهازك — لا إنترنت إطلاقاً، لا حسابات، لا إعلانات، لا تتبّع. لا صلاحيات ملفات: الصور عبر Photo Picker، التصدير عبر SAF/طباعة/FileProvider.

## البنية (Kotlin Multiplatform + Compose Multiplatform 1.12.2)

```
appprint/
├── androidApp/                  # المضيف الأندرويد (AGP 9 · built-in Kotlin · splash · أيقونة Nightjar adapt.)
│   └── src/main/                #   MainActivity · AndroidManifest (CAMERA + POST_NOTIFICATIONS) · res (icon/theme/colors)
├── shared/                      # الوحدة متعددة المنصات
│   └── src/
│       ├── commonMain/          #   منطق + واجهة مشتركة بالكامل (expect/actual للفجوات المنصاتية)
│       │   ├── core/model       #   كتالوج البطاقات الرسمي + إعدادات الورقة (UI-free)
│       │   ├── core/arrange     #   محرك التداخل والتوزيع الذكي (UI-free، مختبر)
│       │   ├── core/image       #   بكسلات + مصفوفات تحسين (UI-free)
│       │   ├── core/pdf         #   كاتب PDF يدوي (بدون مكتبات خارجية)
│       │   ├── core/platform    #   expect: منصات (تصدير/طباعة/مشاركة/إشعار/صور/تقاط)
│       │   ├── ui/theme         #   Material 3 Expressive داكن/فاتح — لوحة العلامة
│       │   ├── ui/AppRoot.kt    #   Nav 3 (state-driven back stack) + NavigationBar/Rail
│       │   └── ui/screens/      #   Studio · الشرح والنصائح · الإعدادات
│       ├── androidMain/         #   actual: أندرويد (Canvas 300DPI · PrintManager · Notification · FileProvider)
│       ├── jvmMain/             #   actual: سطح مكتب (AWT/Skia — هدف التحقق الثاني)
│       └── iosMain/             #   actual مؤقت — يُفعّل على macOS (المالك طلب تأجيل iOS)
└── .github/workflows/release.yml  # verify (compile+tests) ثم APK موقّع رسمياً → Artifacts (+GitHub Release على tags)
```

**المعمارية:** `UI-Free Core` (الكتالوج + التداخل + البكسلات + PDF بلا أي Composable) ← مصرف في `commonMain`؛ الواجهة عبر **UDF**: `StudioViewModel : StateFlow<StudioUiState>` تصب إلى الأسفل، والأحداث تُحيل للأعلى؛ **Navigation 3** بـ `rememberNavBackStack` (مكدّس يملكه المستخدم، مفاتيح `@Serializable`)؛ شاشات تستهلك بالكامل عبر `collectAsStateWithLifecycle()`.

## الصلاحيات

| الصلاحية | لماذا | متى تُطلب |
|---|---|---|
| `CAMERA` | تصوير البطاقة داخل التطبيق | عند أول استخدام لزر «صوّر بطاقة» فقط (يمكن رفضها والاكتفاء برفع الصور) |
| `POST_NOTIFICATIONS` | «تم إنشاء الملف» / «أُرسلت مهمة الطباعة» | قبل أول تصدير/طباعة (Android 13+) |

**لا شيء غيرهما** — لا ملفات، لا موقع، لا نت، لا كاميرا مكررة.

## البناء والتشغيل

> جهاز التطوير ضعيف — التحقق الإلزامي عبر GitHub Actions (انظر تبويب **Actions**). البناء المحلي فقط إن كان جهازك قوياً:

```bash
./gradlew :androidApp:assembleDebug          # APK للتجربة
./gradlew :androidApp:assembleRelease        # APK موقّع (يتطلب Nightjar.keystore في androidApp/ + local.properties)
./gradlew :shared:jvmTest                    # اختبارات محرك الترتيب
./gradlew :androidApp:assembleDebug          # فحص الكومبيلشن + APK للتجربة
```

APK الرسمي الموقّع يُبنى عبر CI: **Actions → Build & Release → Artifacts** (أو تلقائياً على وسوم `v*` مع GitHub Release باسم الإصدار).

## الثيم والأيقونة

- الأيقونة: adaptive icon (foreground/background/monochrome — تُعادى لكل الأقنعة) + نسخة `artwork/icon-1024.png` للمتجر والعرض.
- الألوان: `#151816` حبر · `#007A3D` أخضر عراقي · `#C8102E` أحمر عراقي · `#D9A51F` ذهبي · `#FDFAF1` ورق · `#17365A` كحلي الجواز.

## التراخيص

Apache-2.0 للمشروع. الاعتمادات وتراخيصها تظهر داخل التطبيق (الإعدادات ← الاعتماديات والتراخيص) وقائمتها في [DEPENDENCY_AUDIT.md](DEPENDENCY_AUDIT.md) — وهو أيضاً سجل تدقيق klibs.io لكل تبعية (لا إصدارات pre-release أبداً).
