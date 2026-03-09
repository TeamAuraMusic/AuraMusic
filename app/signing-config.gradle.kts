// Release Signing Configuration for AuraMusic
// This file contains the release signing configuration
// Include this in build.gradle.kts: apply(from = "signing-config.gradle.kts")

// Load local properties for signing credentials
import java.io.FileInputStream
import java.util.*

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {
    signingConfigs {
        // Debug signing config - loads from local.properties
        create("persistentDebug") {
            storeFile = file(localProperties.getProperty("debug.storeFile", "keystore/persistent-debug.keystore"))
            storePassword = localProperties.getProperty("debug.storePassword", "android")
            keyAlias = localProperties.getProperty("debug.keyAlias", "androiddebugkey")
            keyPassword = localProperties.getProperty("debug.keyPassword", "android")
        }

        // Release signing config - loads from local.properties
        create("release") {
            storeFile = file(localProperties.getProperty("release.keystore.path", ""))
            storePassword = localProperties.getProperty("release.keystore.password", "")
            keyAlias = localProperties.getProperty("release.key.alias", "")
            keyPassword = localProperties.getProperty("release.key.password", "")
        }

        // Default debug signing config
        getByName("debug") {
            storeFile = file(localProperties.getProperty("debug.storeFile", "keystore/debug.keystore"))
            storePassword = localProperties.getProperty("debug.storePassword", "android")
            keyAlias = localProperties.getProperty("debug.keyAlias", "auramusicdebug")
            keyPassword = localProperties.getProperty("debug.keyPassword", "android")
        }
    }
}

// Firebase OAuth Configuration for Google Sign-In
// Add these configurations to Firebase Console for proper OAuth validation

val firebaseWebClientId = localProperties.getProperty("firebase.web.client.id") ?: 
    System.getenv("FIREBASE_WEB_CLIENT_ID") ?: ""

val firebaseAndroidClientId = localProperties.getProperty("firebase.android.client.id") ?: 
    System.getenv("FIREBASE_ANDROID_CLIENT_ID") ?: ""

if (firebaseWebClientId.isNotEmpty() || firebaseAndroidClientId.isNotEmpty()) {
    println("✓ Firebase OAuth credentials configured")
    println("  Web Client ID: ${if (firebaseWebClientId.isNotEmpty()) "✓ Set" else "✗ Not set"}")
    println("  Android Client ID: ${if (firebaseAndroidClientId.isNotEmpty()) "✓ Set" else "✗ Not set"}")
}

// SHA Certificate Fingerprints for Google Play Services
// These will be displayed when you build with the release keystore
// Add these to Firebase Console -> Project Settings -> Your Apps -> Android

val releaseKeystorePath = localProperties.getProperty("release.keystore.path")
if (releaseKeystorePath != null && releaseKeystorePath.isNotEmpty()) {
    println("""
        ╔════════════════════════════════════════════════════════════════════════╗
        ║  Release Keystore Found: $releaseKeystorePath                         
        ║                                                                        ║
        ║  To get SHA-1 and SHA-256 fingerprints, run:                         ║
        ║  keytool -list -v -keystore "$releaseKeystorePath" -alias auramusic
        ║                                                                        ║
        ║  Add these fingerprints to:                                          ║
        ║  - Firebase Console -> Project Settings -> Android App                 ║
        ║  - Google Play Console -> App integrity -> SHA-1 & SHA-256           ║
        ╚════════════════════════════════════════════════════════════════════════╝
    """.trimIndent())
}
