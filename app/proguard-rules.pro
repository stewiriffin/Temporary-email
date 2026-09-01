# ── TempBox ProGuard / R8 Rules ──────────────────────────────────

# Maintain layout tags, signatures, and type attributes for serialized validation mapping
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*, PermittedSubclasses

# Protect Gson model serialization identifiers across internal structural endpoints
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Enforce absolute package retention for explicit network model definitions block
-keep class com.rank.tempbox.MailTmModels.** { *; }

# Keep the entire app package — covers data models, ApiService interface, ViewModel, etc.
-keep class com.rank.tempbox.** { *; }
-keep interface com.rank.tempbox.** { *; }

# Retrofit does reflection on generic parameters and method/parameter annotations
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes EnclosingMethod, InnerClasses

# Gson @SerializedName and @Expose — keep these specific annotation attributes explicitly
-keepattributes Expose
-keepattributes SerializedName

# Keep Gson model class fields with @SerializedName — redundant with the wildcard above,
# but belt-and-suspenders for R8's optimizer
-keepclassmembers class com.rank.tempbox.** {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keepclassmembers class com.rank.tempbox.** {
    @com.google.gson.annotations.SerializedName <methods>;
}
-keepclassmembers class com.rank.tempbox.** {
    @com.google.gson.annotations.Expose <fields>;
}

# Keep data class constructors — Gson needs them for reflection-based instantiation
-keepclassmembers class com.rank.tempbox.** {
    <init>(...);
}

# Keep Kotlin Metadata annotation values — Retrofit coroutine support reads return types from it
-keep class kotlin.Metadata { *; }
-keepattributes RuntimeVisibleAnnotations

# Keep Retrofit HTTP-method-annotated methods (e.g. @GET, @POST)
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Gson
-keep class com.google.gson.** { *; }

# OkHttp / Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class okio.** { *; }

# Kotlin coroutines
# Prevent R8 from stripping Continuation's type argument from method signatures.
# Retrofit reads Continuation<ResponseType> to determine suspend function return types.
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# Start.io / StartApp SDK
-keep class com.startapp.** { *; }
-keep class com.truenet.** { *; }
-keepattributes Exceptions, InnerClasses, Signature, Deprecated, SourceFile, LineNumberTable, *Annotation*, EnclosingMethod
-dontwarn android.webkit.JavascriptInterface
-dontwarn com.startapp.**
-dontwarn org.jetbrains.annotations.**

