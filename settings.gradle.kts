pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // MuPDF (docs/decisions/0002-rendering-engine.md) is published only here, not on Maven Central.
        // Exclusive, so nothing else can resolve from it and MuPDF cannot resolve from anywhere else.
        exclusiveContent {
            forRepository { maven("https://maven.ghostscript.com") }
            filter { includeGroup("com.artifex.mupdf") }
        }
    }
}

rootProject.name = "fcat"
include(":app")
