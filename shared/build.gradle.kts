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
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.material3)
            implementation("org.jetbrains.compose.material:material-icons-extended:1.5.12")
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

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

compose.resources {
    publicResClass = true
    packageOfResClass = "com.appprint.studio.shared.resources"
    generateResClass = always
}
