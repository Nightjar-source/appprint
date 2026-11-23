pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx\\..*")
                includeGroup("com.android.application")
                includeGroup("com.android.library")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        // Kotlin/Wasm downloads Node.js from here for the browser toolchain.
        ivy { url = uri("https://nodejs.org/dist") }
    }
}

rootProject.name = "AppPrint"
include(":androidApp")
include(":shared")
include(":desktopApp")
include(":webApp")
