// Android application — thin launcher over :shared (KMP + Compose Multiplatform).
// No org.jetbrains.kotlin.android plugin: AGP 9+ ships built-in Kotlin.
// Clean slate: the app is being rebuilt from scratch from the new design (HTML → Compose).
import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
}

android {
    namespace = "com.appprint.studio"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.appprint.studio"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
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
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// No Compose in this module yet — the UI lives in :shared once the new design lands.
// core-splashscreen keeps the brand Splash theme working; :shared stays on the classpath.
dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.core.splashscreen)
}
