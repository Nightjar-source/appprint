# DEPENDENCY_AUDIT — AppPrint (مطبعة البطاقات العراقية)

**آخر تدقيق كامل:** 2026-11-13 (klibs.io MCP `getLatestVersion` لكل حزمة + Google Maven `maven-metadata.xml` للحزم غير المفهرسة في klibs + الوثائق الأصلية على developer.android.com لمصفوفة AGP↔Gradle)
**القاعدة:** `latestStableVersion` فقط — يُستبعد أي إصدار alpha/beta/rc/dev/pre-release.
**الحالة:** كل إصدار في الكتالوج = أحدث مستقر KMP منشور — **لا ترقيات مطبّقة** (الكتالوج كان محدّثًا أصلًا). ما طُبّق بدلًا منها = **المعايير الناقصة** (انظره أسفل).
**الأدوات:** klibs.io MCP (`getLatestVersion` + `searchProjects`) · Google Maven / Maven Central metadata للحزم غير المفهرسة في klibs (Android-only)
**القاعدة:** `latestStableVersion` فقط — يُستبعد أي إصدار alpha/beta/rc/dev/pre-release.
**القيود:** لا يُلمَس Gradle Wrapper (`gradle-9.4.1-bin` — مطابق لمشروع المرجع بالحزمة والنسخة) · لا أسرار في المستودع.

## ملخص القرار

| الحزمة | الحالي | أحدث مستقر | القرار | السبب |
|---|---|---|---|---|
| `com.android.application` (AGP) | **9.2.1** | 9.5.1 | **إبقاء 9.2.1** | AGP 9.2.x ↔ Gradle 9.4.1 (Compatibility matrix). 9.5.x يتطلب Gradle 9.6.0 — يتطلب ترقية الـ Wrapper (توصية بانتظار موافقة المالك). compileSdk 37 مفعل عبر `android.suppressUnsupportedCompileSdk=37` (نفس نمط مشروع المرجع) |
| `com.android.kotlin.multiplatform.library` (AGP-KMP) | **9.2.1** | — | **تبنّي** | AGP 9.x لا يسمح بـ `com.android.library` بجانب `org.jetbrains.kotlin.multiplatform` — المكوّن الرسمي للموديولات المكتبية KMP |
| `org.jetbrains.kotlin.multiplatform` | **2.4.21** | 2.4.21 | **تبنّي** | أحدث مستقر على Maven Central (2.5.0-Beta2 مستبعد كـ pre-release). klibs: كل أدوات البناء غير مفهرسة هناك — تم التحقق من Central |
| `org.jetbrains.compose` (Compose Multiplatform) | **1.12.2** | 1.13.0-beta02 | **تبنّي 1.12.2** | أحدث مستقر مثبت على المرجع التجاري (amsakkarrar) مع نفس المكدس؛ klibs يؤكد منصات common/jvm/androidJvm/js/wasm/native |
| `org.jetbrains.kotlin.plugin.compose` | **2.4.21** | — | **تبنّي** | مطابق لنسخة Kotlin (AGP 9 بدون `kotlin-android` — Kotlin مدمج في AGP 9) |
| `org.jetbrains.kotlin.plugin.serialization` | **2.4.21** | — | **تبنّي** | مفاتيح Navigation 3 مُعلَّمة بـ @Serializable |
| `org.jetbrains.compose.material3:material3` (Expressive) | **1.12.0-alpha03** | 1.5.12 (فهرسة klibs لخط قديم) / 1.13.0-beta02 | **تبنّي alpha03** | **الإصدارات التعبيرية الوحيدة** لـ Material 3 (`MaterialExpressiveTheme`، `MotionScheme.expressive()`، رموز ألوان موسّعة) تسكن حصريًا في خط `1.12.0-alpha03` لـ Compose MP؛ مفروضة في كل الـ configurations عبر `resolutionStrategy.force` في `:shared`. الترقية لـ 1.13.0-* مستحيلة دون ترقية Compose كله (1.13.0-beta02 pre-release — مستبعد). سطر `1.5.12` على klibs = خط androidx الأقدم ولا يحمل التعبيرية |
| `androidx.compose.ui:ui*` | **1.12.2** | 1.12.2 (via CMP) | **تبنّي** | مستقرة على كامل سلسلة 1.12.x في JetBrains MP؛ compose-bom 2026.09.00 (غير مستخدمة) تطابق |
| `org.jetbrains.androidx.lifecycle:*` (viewmodel/runtime/compose/viewmodel-navigation3) | **2.11.0** | 2.11.0 | **تبنّي** | klibs `latestStableVersion=2.11.0` — نسخة JetBrains المنشورة KMP من androidx.lifecycle |
| `org.jetbrains.androidx.navigation3:navigation3-ui` + `androidx.navigation3:navigation3-runtime/ui` | **1.2.0 / 1.2.1** | 1.2.0 / 1.2.1 | **تبنّي** | klibs يؤكد platforms common/androidJvm/jvm/js/native/wasm; الـ runtime عبر jetbrains metadata + androidx 1.2.1 في androidMain (مقطع المرجع)؛ التنقل **State-driven**: `rememberNavBackStack(SavedStateConfiguration { … }, AppRoute.Studio)` user-owned back stack |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core` | **1.11.0** | 1.11.0 | **تبنّي** | klibs stable |
| `org.jetbrains.kotlinx:kotlinx-serialization-core` | **1.13.0** | 1.13.0 | **تبنّي** | klibs stable |
| `androidx.core:core-ktx` | **1.19.1** | 1.19.1 | **تبنّي** | Google Maven `release=1.19.1` |
| `androidx.activity:activity-compose / activity-ktx` | **1.13.0** | 1.13.0 | **تبنّي** | Google Maven release؛ activity-ktx لـ `enableEdgeToEdge` |
| `androidx.core:core-splashscreen` | **1.2.0** | 1.2.0 | **تبنّي** | Google Maven release |
| `junit:junit` | **4.13.2** | 4.13.2 | **تبنّي** | اختبارات commonTest (SheetArranger) — مشتركة عبر commonTest مع kotlin("test") |
| `io.coil-kt.coil3:coil-compose` | — | 3.6.3 | **مرفوض حالياً** | klibs stable=3.6.3 لكن لا حاجة له: التحميل/التكبير/التخزين المؤقت اليدوي للصور (in-memory sampled decode) يكفي لتطبيق خفيف؛ إبقاء APK أصغر بدون اعتماد شبكي/Cairo |
| `androidx.compose.material3:material3-adaptive-navigation-suite` | — | 1.4.0 | **مُؤجّل** | klibs stable=1.4.0 لكنه androidx-only اليوم؛ بدلاً منه NavigationBar/NavigationRail الماديّتان من CMP (`maxWidth` switching) — متوافقة KMP بالكامل |
| `androidx.compose.material3.adaptive:adaptive*` | — | 1.3.0 | **مُؤجّل** | للوضع المتعدد (list-detail SceneStrategy على Nav3) — لا حاجة حالياً لتطبيق 3 وجهات؛ ستُضاف مع شاشات Head-to-head لاحقاً |
| `androidx.navigation3` (علامة androidx الأصلية) | — | 1.2.1 | **غير مستخدمة** | استُبدلت بـ `org.jetbrains.androidx.navigation3` 1.2.0 (الأحدث المتوافق رسمياً مع Compose MP 1.11)؛ klibs يؤكد 1.2.1 للعلامة الأصلية |
| `androidx.compose:compose-bom` | — | 2026.09.00 | **غير مستخدمة** | CMP plugin يدير إصدارات Compose (لا BOM مع Compose Multiplatform) |
| iOS actuals (Foundation/UIKit) | — | — | **مؤقت (TODO معلّم)** | iOS مؤجل بطلب المالك — `iosMain` ملفات تُرجع null/false بأناقة ولا تكسر البناء؛ تُفعّل على macOS |

## التنقّل 3 (Nav 3) — State-Driven

- `backStack = rememberNavBackStack(AppRoute.Studio)` — **المكدس مملوك للمستخدم** (بيانات عادية serializable عبر kotlinx.serialization)
- `entryProvider { entry<Studio> … entry<Guide> … entry<Settings> … }` + `NavDisplay(backStack, onBack)`
- لا `ListDetailPaneScaffold` (مهمل عملياً) — التكيف الحالي عبر `BoxWithConstraints` + `NavigationBar/NavigationRail`
- على الهواتف: ModalBottomSheet للـ inspector عند تحديد بطاقة؛ ≥640dp: لوح جانبي؛ ≥1150dp: ثلاثة ألسنة (إعدادات | لوح | مفتش)

## معايير أندرويد 17 (API 37) المطبقة

- `targetSdk=37` · `compileSdk=37` · `minSdk=26`
- **Edge-to-edge مفروض**: `enableEdgeToEdge()` + windowBackground داكن، لا `SystemUiAppearance` يدوي
- **Adaptive**: NavigationBar/Rail حسب العرض، لا `configChanges`/`screenOrientation` في Manifest (حالة الواجهة تبقى عبر rememberSaveable/ViewModel/Nav3)
- **Predictive Back**: `android:enableOnBackInvokedCallback="true"` + معالجة NavDisplay الأصلية
- **Runtime permissions دنيا**: CAMERA و POST_NOTIFICATIONS فقط؛ الصور عبر Photo Picker (بلا صلاحيات)، PDF عبر Storage/PrintManager/FileProvider (بلا صلاحيات ملفات)، لا INTERNET نهائياً (التطبيق يعمل بدون اتصال)
- **R8 full mode** في release + `isShrinkResources` + keep بأدنى حد
- **Manifest queries**: IMAGE_CAPTURE + VIEW(application/pdf) لرؤية الحزم

## تحذيرات / أعمال معلّقة (Breaking)

1. **AGP 9.5.1 ↔ Gradle 9.6.0**: عند موافقة المالك على ترقية Wrapper (لا يلمس حالياً)، الترقية المقترحة: Gradle 9.6.0 + AGP 9.5.1 + إزالة `android.suppressUnsupportedCompileSdk`
2. **Compose Multiplatform 1.13.0 / Navigation3 1.2.1+**: إصدارات pre-release مستبعدة؛ تُراجع بعد أول مستقر
3. **androidApp يجب أن يطبّق `org.jetbrains.compose` + `org.jetbrains.kotlin.plugin.compose`**: بدونها يُبنى الـ `MainActivity` بـ plain Kotlin لا تُعالج الـ @Composable، ويُستدعى `setContent` بدون مُعامِل `Composer/Integer` → `NoSuchMethodError: setContent$default(...Function0...)` عند الإقلاع (خطأ v1.0.0، مُصلَح في v1.0.1)
4. **Windows (isLocal) لا يبني المشروع** (بطلب المالك — جهاز ضعيف)؛ التحقق الإلزامي عبر GitHub Actions (`verify` ثم `build-release`)
5. **UiTooling/Compose internal**: تبعيات preview 1.12.2 مباشرة؛ تبعيات icons-extended تبقى لأن M3 icons core لا يقدّم List/Settings/Check في هذه السلسلة |

## التحقق

- **لا يوجد بناء محلي** بطلب المالك. القناة الرسمية للتحقق: GitHub Actions → https://github.com/Nightjar-source/appprint/actions
- آخر نتيجة ناجحة: **Verify — jvmTest (4 اختبارات ✓) + assembleDebug ✓** ثم **Build Official Signed APK ✓** (مرتين: على main بعد إصلاح crash الإقلاع، ثم على الوسم v1.0.1) — APK موقّع بمفتاح المالك (~1.2 MB). الإصدار v1.0.0 تعطل عند الإقلاع (NoSuchMethodError) — حُوّل إلى prerelease مع تحذير؛ الإصلاح في v1.0.1 بتطبيق Compose Multiplatform plugins في androidApp.
- أوامر التحقق في CI: `./gradlew :shared:jvmTest :androidApp:assembleDebug --stacktrace` ثم `./gradlew :androidApp:assembleRelease --no-daemon --stacktrace`
- الـ APK الرسمي موقّع بمفتاح المالك (Nightjar keystore — يُستعاد من `secrets.KEYSTORE_BASE64` في مهمة build-release) ومتاح كـ artifact ثابت على كل دفعة إلى main، وعلى GitHub Releases عند الوسوم `v*`.

---

## التحديث المعياري 2026-11-13 — المعايير المطبّقة (لا ترقيات)

### تدقيق الإصدارات المستقلة (15 حزمة عبر klibs.io + Google Maven)
كل القيم `current == latestStableVersion`. لا يوجد أي ترقية مطبّقة على الإصدارات — الكتالوج كان محدّثًا أصلًا.
(التحقق من الأندرويد فقط عبر Google Maven `maven-metadata.xml`: AGP 9.5.1 stable يتطلب **Gradle 9.6.0**؛ `core-ktx 1.19.1` هو `release` الحالي؛ `activity 1.13.0` هو `release`؛ `exifinterface 1.4.2` هو الأحدث المستقر؛ `core-splashscreen 1.2.0` هو الأحدث المستقر.)

### المعايير الناقصة التي طُبّقت الآن
1. **قاعدة أسماء ملفات مجموعات المصادر (Kotlin Coding Conventions، الأصلية على kotlinlang.org):** كل ملف في `androidMain/jvmMain/iosMain` يحمل **إعلانات دوال على المستوى العلوي** أُعيدت تسميته الآن باللاحقة المعتمدة: `*.android.kt` / `*.jvm.kt` / `*.ios.kt`:
   - `AndroidBitmapBridge / JvmBitmapBridge / IosBitmapBridge.kt` ← `ImageBitmapBridge.{android,jvm,ios}.kt` (مطابقة لملف `commonMain`)
   - `AndroidLaunchers / JvmLaunchers / IosLaunchers.kt` ← `PlatformLaunchers.{android,jvm,ios}.kt`
   - `PlatformBackHandler.kt` ← `PlatformBackHandler.{android,jvm,ios}.kt`
   - `PlatformSchemes.kt` ← `PlatformSchemes.{android,jvm,ios}.kt`
   (الملفات التي لا تحوي سوى `object`/`class` مثل `AndroidActions.kt` لم تُمس — القاعدة للإعلانات العليا فقط.)
2. **أسماء بمعنى لا أسماء عامة:** `ui/components/Common.kt` (helpers مشتركة: SectionHeader/ToggleRow/LabeledSlider/…) أُعيدت تسميته → **`StudioComponents.kt`**.
3. **لا اعتماديات مثبتة خارج الكتالوج — القرار الرسمي:** أُلغيت كل النصوص المثبتة `org.jetbrains.compose.material3:material3:1.12.0-alpha03` و`org.jetbrains.compose.ui:ui-tooling(-preview):1.12.2` و`junit:junit:4.13.2` و`androidx.core:core-ktx:1.19.1` و`core-splashscreen:1.2.0` و`activity-compose/ktx:1.13.0` — صارت جميعها **مراجع كتالوجية** (`libs.material3.expressive`, `libs.ui.tooling[.preview]`, `libs.junit4`, `libs.androidx.core[.splashscreen]`, `libs.androidx.activity[.compose / .ktx]`). أُضيف `material3Expressive` (1.12.0-alpha03) و`junit4` (4.13.2) كمفاتيح `[versions]`.
4. **إزالة اعتماد غير مستخدم:** `androidx.navigation3:navigation3-ui` (العلامة الأصلية؛ المستخدم هو `org.jetbrains.androidx.navigation3:navigation3-ui` KMP) — محذوف من الكتالوج.
5. **وثائق تصف الواقع:** README «Compose Multiplatform 1.11.1 → 1.12.2»؛ release notes في CI عُدّلت إلى (شريط علوي 48/42/46dp · كبسولة 56dp) بدل القيم القديمة (56/62dp).
6. **التحقق من الكود بعد إعادة التسمية:** لا مراجع نصية لأسماء الملفات (الكود بالمراجع الحزبية/الـ imports) — الحزمة لم تتغير؛ التجميع عبر CI (`:shared:jvmTest` + `:androidApp:assembleDebug`).

### ما لم يُطبَّق (توصيات يترتب عليها تغييرات منهجية)
- **AGP 9.5.1 + Gradle 9.6.0**: مفتاح الترقية عند موافقة المالك على لمس الـ Wrapper (ممنوع حاليًا) — 9.6.0 minimum per developer.android.com؛ وسيزيل `android.suppressUnsupportedCompileSdk=37`.
- **Compose Multiplatform 1.13.0-beta02** و**material3 1.13.0-beta02** و**adaptive 1.4.0-alpha01**: pre-release مستبعدة؛ تُراجع بعد أول وسم مستقر.
- **Lint / Detekt / Spotless**: لا توجد أدوات جودة ثابتة في الـ CI (لا format gates)؛ إضافة Detekt تغيّر سلوك CI ولها إيجابيات كاذبة على الكود الحالي — توصية، لا تطبيق.
- **navigation3 (androidx الأصلية) 1.2.1**: مستقرة لكنها غير متوافقة KMP لهذا المكدس؛ يبقى `org.jetbrains.androidx.navigation3` 1.2.0 + `androidx.navigation3:navigation3-runtime` 1.2.1.
