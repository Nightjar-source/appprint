// Android application — thin launcher over :shared (KMP + Compose Multiplatform).
// No org.jetbrains.kotlin.android plugin: AGP 9+ ships built-in Kotlin.
import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.appprint.studio"
    // Compose 1.13: AndroidX stable through BOM 2026.11.01 (API 37 extension 1).
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.appprint.studio"
        minSdk = 26
        targetSdk = 37
        versionCode = 92
        versionName = "2.1.65"
        resourceConfigurations += listOf("ar", "en")
    }

    val keystoreProperties = Properties()
    val keystorePropertiesFile = rootProject.file("local.properties")
    if (keystorePropertiesFile.exists()) {
        FileInputStream(keystorePropertiesFile).use { keystoreProperties.load(it) }
    }

    signingConfigs {
        create("release") {
            // Restored from GitHub secrets on CI. NO fallback passwords: missing keys
            // stay null here and the *release packaging* fails loudly (CI verify doesn't).
            storeFile = file("Nightjar.keystore")
            storePassword = keystoreProperties.getProperty("KEYSTORE_PASSWORD")
            keyAlias = "Nightjar"
            keyPassword = keystoreProperties.getProperty("KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// Compose Lives in :shared — this module wires the launcher, edge-to-edge and the splash.
dependencies {
    implementation(project(":shared"))
    // Jetpack Compose November '26 stable BOM (Compose 1.13.0 / Material 3 1.5.0).
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.material3)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.activity.ktx)
    // AdMob (Android-only) — monetization via Google Mobile Ads SDK; TEST ad unit ids until the real units are issued.
    implementation(libs.play.services.ads)
}
