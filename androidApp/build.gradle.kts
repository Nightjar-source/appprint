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
    compileSdk = 37  // Android 17

    defaultConfig {
        applicationId = "com.appprint.studio"
        minSdk = 26
        targetSdk = 37
        versionCode = 16
        versionName = "1.1.4"
        resourceConfigurations += listOf("ar", "en")
    }

    val keystoreProperties = Properties()
    val keystorePropertiesFile = rootProject.file("local.properties")
    if (keystorePropertiesFile.exists()) {
        FileInputStream(keystorePropertiesFile).use { keystoreProperties.load(it) }
    }

    signingConfigs {
        create("release") {
            // Same official key lineage as the owner's other apps (restored from GitHub secrets on CI)
            storeFile = file("Nightjar.keystore")
            storePassword = keystoreProperties.getProperty("KEYSTORE_PASSWORD") ?: "19941994"
            keyAlias = "Nightjar"
            keyPassword = keystoreProperties.getProperty("KEY_PASSWORD") ?: "19941994"
        }
    }

    buildTypes {
        release {
            // R8 full mode + resource shrinking; officially signed
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
            // Sign debug builds with the official key when available (release parity),
            // otherwise fall back to the default debug keystore — CI verify needs no secrets.
            if (file("Nightjar.keystore").exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = false
        buildConfig = true
    }
}

dependencies {
    implementation(project(":shared"))
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.core:core-splashscreen:1.2.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.activity:activity-ktx:1.13.0")
}
