# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# General Keep Attributes
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable

# FFmpegKit
-keep class com.arthenica.ffmpegkit.** { *; }
-dontwarn com.arthenica.ffmpegkit.**

# Media3 & ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Room Database
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# Moshi
-keep class com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**
-keepclassmembers class * {
    @com.squareup.moshi.* <fields>;
    @com.squareup.moshi.* <methods>;
}

# Retrofit & OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# Coroutines
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { *; }

# Jetpack Compose & Navigation
-dontwarn androidx.compose.**
-dontwarn androidx.navigation.**

# Firebase & Google Services
-dontwarn com.google.firebase.**
-keep class com.google.firebase.** { *; }
-dontwarn com.google.android.gms.**
-keep class com.google.android.gms.** { *; }
-dontwarn androidx.credentials.**
-keep class androidx.credentials.** { *; }

# Coil
-dontwarn coil.**
-keep class coil.** { *; }

# Project Models & Entities
-keep class com.example.data.** { *; }
-keep class com.example.util.** { *; }
-keep class com.example.ui.** { *; }
-keep class com.example.model.** { *; }
-keep class com.example.service.** { *; }


