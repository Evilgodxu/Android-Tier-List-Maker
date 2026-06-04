# ============================================================
# 基础属性保留
# ============================================================
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# 保留源文件属性用于调试
-renamesourcefileattribute SourceFile

# ============================================================
# Kotlin
# ============================================================
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ============================================================
# Compose
# ============================================================
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# ============================================================
# 数据类保留
# ============================================================
-keep class com.tdds.jh.data.** { *; }

# ============================================================
# R 文件
# ============================================================
-keepclassmembers class **.R$* {
    public static <fields>;
}

# ============================================================
# Coil 3
# ============================================================
-keep class coil3.** { *; }
-keep interface coil3.** { *; }

# ============================================================
# Kotlin Coroutines
# ============================================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ============================================================
# Apache Commons Compress
# ============================================================
-keep class org.apache.commons.compress.** { *; }
-dontwarn org.apache.commons.compress.**
-dontwarn org.objectweb.asm.**
-dontwarn org.tukaani.xz.**

# ============================================================
# Zip4j
# ============================================================
-keep class net.lingala.zip4j.** { *; }

# ============================================================
# 测试相关
# ============================================================
-keep class * extends org.junit.** { *; }
-keep class androidx.test.** { *; }

# ============================================================
# 移除调试日志（发布版本）
# ============================================================
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
