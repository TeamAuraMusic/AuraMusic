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
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/") }
        maven { url = uri("https://s01.oss.sonatype.org/content/repositories/releases/") }
        google()
    }
}

rootProject.name = "AuraMusic"

include(":app")
include(":innertube")
include(":flow")
include(":kugou")
include(":lrclib")
include(":kizzy")
include(":lastfm")
include(":betterlyrics")
include(":simpmusic")
include(":shazamkit")
include(":rush")