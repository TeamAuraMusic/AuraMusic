plugins {
    id("com.android.application")
}

android {
    namespace = "com.auramusic.previewprobe"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.auramusic.previewprobe"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation("androidx.tvprovider:tvprovider:1.0.0")
    testImplementation(libs.junit)
}
