# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Ignore missing service warnings
-dontwarn javax.script.**
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

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# VOSK
-keep class org.vosk.** { *; }
-keep class com.alphacephei.** { *; }
-dontwarn org.vosk.**

# Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Jsoup
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**

# Coil
-keep class coil3.** { *; }

# Protobuf
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }

# Kuromoji loads dictionary .bin files via Class.getResourceAsStream with
# package-relative paths. If R8 obfuscates these classes/packages, release
# builds look for the dictionaries under the obfuscated package and crash.
-keep class com.atilika.kuromoji.** { *; }
-dontwarn com.atilika.kuromoji.**

# Innertube / YouTube models
-keep class com.auramusic.innertube.models.** { *; }

# App entities and DAOs
-keep class com.auramusic.app.db.entities.** { *; }
-keep class com.auramusic.app.db.dao.** { *; }
-keep class com.auramusic.app.db.MusicDatabase { *; }
