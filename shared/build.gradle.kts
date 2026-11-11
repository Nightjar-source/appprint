// Shared UI-free core + Compose Multiplatform UI shared by Android (shipping),
// Desktop JVM (verification target) and iOS (targets declared; wire on macOS).
// Follows the Compose Multiplatform 1.11 conventions (AGP 9 built-in Kotlin).
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "com.appprint.studio.shared"
        compileSdk = 37
        minSdk = 26
        withHostTest { }
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
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.ui.graphics)
            implementation(libs.compose.material3)
            implementation(libs.compose.material.icons.core)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.ui.tooling.preview)

            implementation(libs.lifecycle.viewmodel)
            implementation(libs.lifecycle.runtime.compose)
            implementation(libs.lifecycle.viewmodel.navigation3)
            implementation(libs.navigation3.ui.multiplatform)

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
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.compose.ui.tooling)
        }
        jvmMain.dependencies {
            // Skia-backed Compose for Desktop comes transitively via the Compose plugin
        }
        iosMain.dependencies {
            // iOS actuals are placeholders — enable the iosApp on macOS
        }
    }
}

compose {
    resources {
        packageOfResClass = "com.appprint.studio.shared.resources"
        publicResClass = true
    }
}
