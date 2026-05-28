# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

# Keep application class
-keep class com.personaltracker.** { *; }

# Hilt
-keepclassmembers class * {
    @com.google.dagger.hilt.android.lifecycle.HiltViewModel *;
}

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

# SQLCipher
-keep class net.sqlcipher.** { *; }
-dontwarn net.sqlcipher.**

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.personaltracker.**$$serializer { *; }
-keepclassmembers class com.personaltracker.** {
    *** Companion;
}
-keepclasseswithmembers class com.personaltracker.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# WorkManager
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker

# Vico charts
-keep class com.patrykandpatrick.vico.** { *; }

# Coil
-keep class io.coil.kt.** { *; }

# Prevent obfuscation of security-sensitive classes
-keep class com.personaltracker.security.** { *; }
