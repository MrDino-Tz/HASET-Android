# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve line number information for debugging stack traces.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ===========================
# General Android Rules
# ===========================
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes Exceptions

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep views inflated from XML by class name (incl. library views)
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
# Used with Firebase RTDB (DataSnapshot.getValue maps JSON to fields by NAME
# reflectively) and Gson — field names must not be obfuscated.
-keep class com.haset.hasetapp.models.** { *; }
-keep class com.haset.hasetapp.database.entities.** { *; }

# Keep the Firebase-reflected banner model
-keep class com.haset.hasetapp.adapters.PatientBannerAdapter$BannerItem {
    <fields>;
    <init>();
}

# ===========================
# Retrofit
# ===========================
# Annotated service methods must keep their signatures for dynamic proxies.
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn sun.misc.**

# ===========================
# Gson
# ===========================
# Custom adapters/factories (if any) must survive shrinking.
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ===========================
# Room Database
# ===========================
# Keep the app's generated database classes
-keep class com.haset.hasetapp.database.** { *; }
-dontwarn androidx.room.paging.**

# ===========================
# Glide
# ===========================
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
    <init>(...);
}

# ===========================
# Cloudinary (media upload)
# ===========================
-keep class com.cloudinary.** { *; }
-keep interface com.cloudinary.** { *; }
-dontwarn com.cloudinary.**
-dontwarn org.apache.commons.**

# ===========================
# zxing (QR codes & MFA enrolment)
# ===========================
-keep class com.google.zxing.** { *; }
-keep interface com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

-keep class com.journeyapps.barcodescanner.** { *; }
-dontwarn com.journeyapps.barcodescanner.**

# ===========================
# Firebase / Google Play Services
# ===========================
# All Firebase & GMS artifacts ship their own consumer rules — no blanket
# keeps here (previously -keep class androidx.**/firebase.**/gms.** { *; }
# blocked ~65% of R8 shrinking/obfuscation).
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ===========================
# Remove Logging (for production)
# ===========================
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
