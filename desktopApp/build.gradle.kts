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
            packageVersion = "2.1.27"
            // jpackage/MSI metadata must stay ASCII: non-ASCII values break
            // jpackage's args-file decoding on GitHub-hosted Windows runners.
            description = "AppPrint Studio"
            vendor = "Nightjar"
            copyright = "Copyright 2026 Nightjar"
            windows {
                menuGroup = "AppPrint"
                // Must be a valid 8-4-4-4-12 hexadecimal UUID; jpackage/MSI rejects anything else.
                upgradeUuid = "8F3C2A1E-6D4B-4F0A-9C21-2026AABBCCDD"
            }
        }
    }
}
