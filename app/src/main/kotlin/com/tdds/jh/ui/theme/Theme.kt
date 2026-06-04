package com.tdds.jh.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * 扩展颜色数据类
 * 包含应用特有的额外颜色定义,包括层级颜色和UI组件颜色
 */
data class ExtendedColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val cardBackground: Color,
    val accentColor: Color,
    val tierSColor: Color = Color(0xFFFF6B6B),
    val tierAColor: Color = Color(0xFFFFB347),
    val tierBColor: Color = Color(0xFFFFFACD),
    val tierCColor: Color = Color(0xFFB8E6B8),
    val tierDColor: Color = Color(0xFF87CEEB),
    val tierEColor: Color = Color(0xFFDDA0DD),
    val buttonContainer: Color,
    val buttonContent: Color,
    val navigationBar: Color,
    val statusBar: Color
)

/**
 * 本地提供的扩展颜色CompositionLocal
 * 用于在Compose组件树中提供扩展颜色
 */
val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        background = Color.Unspecified,
        surface = Color.Unspecified,
        surfaceVariant = Color.Unspecified,
        cardBackground = Color.Unspecified,
        accentColor = Color.Unspecified,
        buttonContainer = Color.Unspecified,
        buttonContent = Color.Unspecified,
        navigationBar = Color.Unspecified,
        statusBar = Color.Unspecified
    )
}

/**
 * 获取扩展颜色 - 只支持深色和浅色模式
 */
@Composable
fun getExtendedColors(isDarkTheme: Boolean): ExtendedColors {
    return if (isDarkTheme) {
        ExtendedColors(
            background = Color(0xFF1C1B1F),
            surface = Color(0xFF2C2C2C),
            surfaceVariant = Color(0xFF2C2C2C),
            cardBackground = Color(0xFF2C2C2C),
            accentColor = Color(0xFFAAAAAA),
            buttonContainer = Color(0xFFAAAAAA),
            buttonContent = Color(0xFF1C1B1F),
            navigationBar = Color(0xFF1C1B1F),
            statusBar = Color(0xFF1C1B1F)
        )
    } else {
        ExtendedColors(
            background = Color(0xFFFAF8F5),
            surface = Color(0xFFFAF8F5),
            surfaceVariant = Color(0xFFDFDFDF),
            cardBackground = Color(0xFFDFDFDF),
            accentColor = Color(0xFF888888),
            buttonContainer = Color(0xFF888888),
            buttonContent = Color(0xFFFAF8F5),
            navigationBar = Color(0xFFFAF8F5),
            statusBar = Color(0xFFFAF8F5)
        )
    }
}

// ==================== 深色主题配色 ====================
// 使用棕色系配色，与浅色模式保持一致的视觉风格
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),  // 柔和的紫色作为主色
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    error = Color(0xFFCF6679),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
    inverseSurface = Color(0xFFE6E1E5),
    inverseOnSurface = Color(0xFF1C1B1F),
    inversePrimary = Color(0xFF6750A4),
    surfaceTint = Color(0xFFD0BCFF)
)

// ==================== 浅色主题配色 ====================
// 米黄色配色，模拟纸张颜色更护眼，适合长时间编辑
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6B5B4F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF0E8E0),
    onPrimaryContainer = Color(0xFF2A1F18),
    secondary = Color(0xFF7A6B5F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF0EAE4),
    onSecondaryContainer = Color(0xFF2A2420),
    tertiary = Color(0xFF6B7A5F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE8F0E0),
    onTertiaryContainer = Color(0xFF1F2A18),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFFAF8F5),
    onBackground = Color(0xFF3D3630),
    surface = Color(0xFFFAF8F5),
    onSurface = Color(0xFF3D3630),
    surfaceVariant = Color(0xFFF0EDE8),
    onSurfaceVariant = Color(0xFF6B655C),
    outline = Color(0xFF9E958C),
    outlineVariant = Color(0xFFD9D4CC),
    inverseSurface = Color(0xFF3D3630),
    inverseOnSurface = Color(0xFFFAF8F5),
    inversePrimary = Color(0xFFE8DCD4),
    surfaceTint = Color(0xFF6B5B4F)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    disableCustomFont: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val extendedColors = getExtendedColors(darkTheme)

    // 根据用户设置选择字体配置，禁用自定义字体可减少内存占用
    val typography = if (disableCustomFont) SystemTypography else Typography

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}
