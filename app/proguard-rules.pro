# Keep Moshi generated adapters and model classes used for (de)serialization.
-keep class com.medfusion.ai.data.remote.dto.** { *; }
-keep class com.medfusion.ai.domain.model.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-dontwarn okhttp3.**
-dontwarn okio.**
# Firebase
-keepattributes *Annotation*
