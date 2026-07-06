# Skald ProGuard Rules

# OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-dontwarn okio.**

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# AndroidX
-keep class androidx.** { *; }
-dontwarn androidx.**

# Skald internal
-keep class com.skald.app.model.** { *; }
-keep class com.skald.app.api.** { *; }

# Accessibility Service
-keep class com.skald.app.service.SkaldAccessibilityService { *; }
