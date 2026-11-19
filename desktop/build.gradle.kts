import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    alias(libs.plugins.compose.desktop)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

// Same version and build number as the Android app; see gradle.properties.
val desktopVersionName = (findProperty("auramusic.versionName") as? String)?.takeIf { it.isNotBlank() } ?: "3.2.0"
val desktopVersionCode = (findProperty("auramusic.versionCode") as? String)?.toIntOrNull() ?: 29

kotlin {
    jvmToolchain(21)
}

val generateDesktopBuildInfo by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/sources/desktopBuildInfo")
    outputs.dir(outputDir)
    doLast {
        val packageDir = outputDir.get().asFile.resolve("com/auramusic/desktop")
        packageDir.mkdirs()
        packageDir.resolve("DesktopBuildInfo.kt").writeText(
            """
            package com.auramusic.desktop

            object DesktopBuildInfo {
                const val VERSION_NAME = "$desktopVersionName"
                const val VERSION_CODE = $desktopVersionCode
                const val VERSION_LABEL = "$desktopVersionName ($desktopVersionCode)"
            }
            """.trimIndent(),
        )
    }
}

kotlin.sourceSets.main {
    kotlin.srcDir(generateDesktopBuildInfo.map { it.outputs.files.singleFile })
}
tasks.named("compileKotlin") {
    dependsOn(generateDesktopBuildInfo)
}

dependencies {
    implementation(project(":innertube-jvm"))
    // NewPipe types appear in the stream-resolution code, so it must be on this
    // module's compile classpath too (:innertube-jvm exposes it as implementation).
    implementation(libs.newpipeextractor)

    implementation(compose.desktop.currentOs)
    implementation(compose.foundation)
    implementation(compose.material3)

    implementation(libs.jna)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.swing)
}

compose.desktop.application {
    mainClass = "com.auramusic.desktop.MainKt"

    nativeDistributions {
        // Put mpv-2.dll in desktop/resources/windows before packaging.
        appResourcesRootDir.set(layout.projectDirectory.dir("resources"))
        targetFormats(TargetFormat.Exe, TargetFormat.Msi)
        packageName = "AuraMusic Desktop"
        packageVersion = desktopVersionName
        vendor = "AuraMusic"
        copyright = "GPL-3.0"

        windows {
            menu = true
            shortcut = true
            dirChooser = true
            upgradeUuid = "6B5A84F0-6A3C-4D2E-9E11-6A6B4E2A1F7C"
        }
    }

    buildTypes.release.proguard {
        isEnabled = false
    }
}
