-keepattributes *Annotation*, InnerClasses
-keepclassmembers class com.samupdater.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.samupdater.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-dontwarn okhttp3.internal.platform.**
