# Kotlinx serialization — keep serializers for the model classes.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keepclassmembers class com.gigapingu.neon.core.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.gigapingu.neon.core.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.gigapingu.neon.**$$serializer { *; }

# OkHttp
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
