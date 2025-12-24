# Add project specific ProGuard rules here.
# Keep MSAL classes
-keep class com.microsoft.identity.** { *; }
-keep class com.microsoft.aad.** { *; }

# Keep Gson classes
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }

# Keep OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

