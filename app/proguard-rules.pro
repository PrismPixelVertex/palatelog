# Add project specific ProGuard rules here.
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-dontwarn okio.**
-dontwarn javax.annotation.**

# Room Database
-keepclassmembers class * {
    @androidx.room.TypeConverter *;
}
-keep class * extends androidx.room.RoomDatabase

# kotlinx.serialization
-keep,allowobfuscation,allowshrinking @kotlinx.serialization.Serializable class *
-keepclassmembers class * {
    *** Companion;
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclassmembers class * {
    @kotlinx.serialization.Required <fields>;
}
-keepclassmembers class * {
    @kotlinx.serialization.Transient <fields>;
}

# osmdroid map engine
-dontwarn org.osmdroid.**

# Mappls Map Engine
-keep class com.mappls.** { *; }
-keep interface com.mappls.** { *; }
-dontwarn com.mappls.**

