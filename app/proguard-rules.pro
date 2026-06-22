# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

# ===========================
# General Android Rules
# ===========================
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep custom views
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelables
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ===========================
# App-Specific Models
# ===========================
# Keep all model classes (used with Firebase, Gson, etc.)
-keep class com.haset.hasetapp.models.** { *; }
-keep class com.haset.hasetapp.database.entities.** { *; }

# ===========================
# Firebase
# ===========================
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Firebase Authentication
-keep class com.google.firebase.auth.** { *; }

# Firebase Realtime Database
-keep class com.google.firebase.database.** { *; }
-keepclassmembers class com.haset.hasetapp.models.** {
    *;
}

# Firebase Cloud Messaging
-keep class com.google.firebase.messaging.** { *; }
-keep class com.google.firebase.iid.** { *; }

# Firebase Storage
-keep class com.google.firebase.storage.** { *; }

# ===========================
# Retrofit & OkHttp
# ===========================
# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class okio.** { *; }

# OkHttp Logging Interceptor
-keep class okhttp3.logging.** { *; }

# ===========================
# Gson
# ===========================
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Prevent stripping of generic signatures
-keepattributes Signature

# Application classes that will be serialized/deserialized over Gson
-keep class com.haset.hasetapp.models.** { <fields>; }

# ===========================
# Room Database
# ===========================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep DAO methods
-keep interface * extends androidx.room.Dao {
    *;
}

# Keep database classes
-keep class com.haset.hasetapp.database.** { *; }

# ===========================
# Glide
# ===========================
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
    <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
    *** rewind();
}

# ===========================
# Cloudinary
# ===========================
-keep class com.cloudinary.** { *; }
-keep interface com.cloudinary.** { *; }
-dontwarn com.cloudinary.**
-dontwarn org.apache.commons.**

# ===========================
# ZXing (QR Codes)
# ===========================
-keep class com.google.zxing.** { *; }
-keep interface com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

-keep class com.journeyapps.barcodescanner.** { *; }
-dontwarn com.journeyapps.barcodescanner.**

# ===========================
# CircleImageView
# ===========================
-keep class de.hdodenhof.circleimageview.** { *; }

# ===========================
# Facebook Shimmer
# ===========================
-keep class com.facebook.shimmer.** { *; }

# ===========================
# Material Components
# ===========================
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# ===========================
# AndroidX
# ===========================
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-dontwarn androidx.**

# Lifecycle
-keep class androidx.lifecycle.** { *; }
-dontwarn androidx.lifecycle.**

# Navigation
-keep class androidx.navigation.** { *; }
-dontwarn androidx.navigation.**

# ===========================
# Google Play Services
# ===========================
# Location
-keep class com.google.android.gms.location.** { *; }

# Auth (Google Sign-In)
-keep class com.google.android.gms.auth.** { *; }

# ===========================
# Remove Logging (Optional - for production)
# ===========================
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# ===========================
# Debugging
# ===========================
# Keep source file names and line numbers for better crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile