// surum: 0.2.0 - cocuk (app) + ebeveyn modulleri
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AileKontrolCocuk"
include(":app")
include(":ebeveyn")
