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
        // Maven Central mirror first: some build hosts block repo.maven.apache.org.
        maven { url = uri("https://maven-central.storage-download.googleapis.com/maven2/") }
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/") }
        maven { url = uri("https://s01.oss.sonatype.org/content/repositories/releases/") }
        google()
    }
}

rootProject.name = "AuraMusic"

include(":app")
include(":desktop")
include(":innertube-jvm")
include(":timber-jvm")
include(":previewprobe")
include(":innertube")
include(":auravideo")
include(":kugou")
include(":lrclib")
include(":kizzy")
include(":lastfm")
include(":betterlyrics")
include(":simpmusic")
include(":shazamkit")
include(":rush")
include(":paxsenix")
include(":musixmatch")
