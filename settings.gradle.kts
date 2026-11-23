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
    // Kotlin/Wasm adds a specially laid-out Ivy repository for its Node.js
    // distribution at configuration time. PREFER_SETTINGS/FAIL_ON_PROJECT_REPOS
    // reject it, and declaring a plain Ivy repo here uses the wrong layout.
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AppPrint"
include(":androidApp")
include(":shared")
include(":desktopApp")
include(":webApp")
