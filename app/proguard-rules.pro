# 基础属性保留
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# 保留源文件属性用于调试
-renamesourcefileattribute SourceFile

# Kotlin
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Compose
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# 数据类保留
-keep class com.tdds.jh.data.** { *; }

# R 文件
-keepclassmembers class **.R$* {
    public static <fields>;
}

# Coil 3
-keep class coil3.** { *; }
-keep interface coil3.** { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Apache Commons Compress
-keep class org.apache.commons.compress.** { *; }
-dontwarn org.apache.commons.compress.**
-dontwarn org.objectweb.asm.**
-dontwarn org.tukaani.xz.**

# Zip4j
-keep class net.lingala.zip4j.** { *; }

# DataStore Preferences（内部使用 protobuf，需保留 descriptor）
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite* {
    <fields>;
}
