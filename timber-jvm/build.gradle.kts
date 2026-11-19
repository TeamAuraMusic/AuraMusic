// JVM stand-in for Timber: the Android Timber artifact is AAR-only, so the
// desktop build provides the few log calls InnerTube actually uses.
plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(21)
}
