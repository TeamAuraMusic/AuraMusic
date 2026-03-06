# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Ignore missing service warnings
-dontwarn javax.script.**
-dontwarn META-INF/services/**
-dontwarn com.google.re2j.**
-dontwarn java.beans.**

# Keep Kotlin metadata
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep Kotlin Serialization
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep data classes for serialization
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    <fields>;
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep custom exceptions
-keep class com.auramusic.app.** { *; }
