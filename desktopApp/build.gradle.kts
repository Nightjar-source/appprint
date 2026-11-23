// Windows / Linux / macOS native program — Compose Desktop host for the shared UI.
// Uses the exact same shared/src/jvmMain PlatformActions actuals (file I/O, AWT picker,
// javax.print fallback) — no WebView, no browser wrapper.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutines.swing)
}

compose.desktop {
    application {
        mainClass = "com.appprint.studio.DesktopAppKt"
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
            )
            packageName = "AppPrint"
            packageVersion = "2.1.21"
            description = "طبعتي — استوديو الطباعة الذكي 1:1"
            vendor = "Nightjar"
            copyright = "© 2026 Nightjar"
            windows {
                menuGroup = "طبعتي"
                upgradeUuid = "8F3C2A1E-6D4B-4F0A-9C21-APPPRINT2026"
            }
        }
    }
}
