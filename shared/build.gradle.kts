// Shared UI-free core + Compose Multiplatform UI shared by Android (shipping),
// Desktop JVM (verification target) and iOS (targets declared; wire on macOS).
// Follows the Compose Multiplatform 1.12 conventions (AGP 9 built-in Kotlin).
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Keep Material 3 on the expressive (alpha) line everywhere — the Compose
// plugin would otherwise pull the stable material3 back in and the expressive
// theming APIs would not resolve.
configurations.all {
    resolutionStrategy {
        force("org.jetbrains.compose.material3:material3:1.12.0-alpha03")
    }
}

kotlin {
    android {
        namespace = "com.appprint.studio.shared"
        compileSdk = 37
        minSdk = 26
        withHostTest { }
        // Enables Compose Multiplatform resources (Res class + accessors) for the androidLibrary target
        androidResources {
            enable = true
        }
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    jvm()

    // iosX64 غير مدعوم من Compose/Navigation3 في هذه التركيبة — arm64 فقط حتى تُدعم
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "AppPrint"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            // Material 3 Expressive theming (official Compose Multiplatform guidance:
            // "Decoupled Material3 versioning" — the expressive APIs live in the alpha
            // Material3 artifact and need to be requested explicitly).
            implementation("org.jetbrains.compose.material3:material3:1.12.0-alpha03")
            implementation(compose.materialIconsExtended)
            implementation(libs.material3.adaptive)
            implementation("org.jetbrains.compose.ui:ui-tooling-preview:1.12.2")

            implementation(libs.lifecycle.viewmodel)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.lifecycle.runtime.compose)
            implementation(libs.lifecycle.viewmodel.navigation3)
            implementation(libs.navigation3.runtime)
            implementation(libs.navigation3.ui.multiplatform)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("junit:junit:4.13.2")
        }
        androidMain.dependencies {
            // image picker + camera capture launchers live behind expect composables
            implementation(libs.androidx.activity)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.exifinterface)
            implementation("org.jetbrains.compose.ui:ui-tooling:1.12.2")
        }
        jvmMain.dependencies {
            // Skia-backed Compose for Desktop comes transitively via the Compose plugin
        }
        iosMain.dependencies {
            // iOS actuals are placeholders — enable the iosApp on macOS
        }
    }
}


