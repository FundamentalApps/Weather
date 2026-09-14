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
        // LineageOS gradle-generatebp: resolves the org.lineageos.generatebp plugin used to
        // emit Android.bp + vendored libs/ for the in-tree (Soong) build.
        maven("https://raw.githubusercontent.com/lineage-next/gradle-generatebp/v1.32/.m2")
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Weather"
include(":app")
