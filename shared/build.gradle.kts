// Shared UI-free core + Compose Multiplatform UI shared by Android (shipping),
// Desktop JVM (verification target) and iOS (targets declared; wire on macOS).
// Layout follows the Kotlin Multiplatform wizard conventions for AGP 9.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

kotlin {
    androidLibrary {
        namespace = "com.appprint.studio.shared"
        compileSdk = 37
        minSdk = 26
        withHostTest { }
    }

    jvm()

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.uiGraphics)
            implementation(compose.material3)
            implementation(compose.materialIconsCore)
            implementation(compose.components.resources)
            implementation(compose.uiToolingPreview)

            implementation(libs.lifecycle.viewmodel)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.lifecycle.runtime.compose)
            implementation(libs.lifecycle.viewmodel.navigation3)
            implementation(libs.navigation3.runtime)
            implementation(libs.navigation3.ui)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        androidMain.dependencies {
            // image picker + camera capture launchers live behind expect composables
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation(compose.uiTooling)
        }
        jvmMain.dependencies {
            // Skia-backed Compose for Desktop comes transitively via the Compose plugin
        }
        iosMain.dependencies {
            // iOS actuals are implemented; wire the iosApp on macOS
        }
    }
}

compose {
    resources {
        packageOfResClass = "com.appprint.studio.shared.resources"
        publicResClass = true
    }
}
