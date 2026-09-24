# ProGuard rules for Samsung Health Bridge
# https://developer.android.com/guide/developing/tools/proguard.html

# Keep Samsung Health Data SDK classes
-keep class com.samsung.android.sdk.healthdata.** { *; }
-dontwarn com.samsung.android.sdk.healthdata.**

# Keep our data models for Gson serialization
-keep class com.selfmanagement.samsungbridge.models.** { *; }
-keepclassmembers class com.selfmanagement.samsungbridge.models.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Gson
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
