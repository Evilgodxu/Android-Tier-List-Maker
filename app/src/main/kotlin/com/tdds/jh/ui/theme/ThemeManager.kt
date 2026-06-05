package com.tdds.jh.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.activity.ComponentActivity
import com.tdds.jh.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// 主题模式常量
object ThemeMode {
    const val SYSTEM = "system"
    const val LIGHT = "light"
    const val DARK = "dark"
}

// 主题管理器，基于 UserPreferencesRepository 的 Flow 驱动
class ThemeManager(
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    val themeModeFlow: Flow<String> = userPreferencesRepository.themeMode

    // 解析为 Boolean（是否深色），需要在 Composable 中用 collectAsState
    @Composable
    fun resolveDarkTheme(themeMode: String): Boolean {
        val systemDark = isSystemInDarkTheme()
        return when (themeMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            else -> systemDark
        }
    }

    // 解析当前是否为深色主题（由外部传入系统深色状态）
    fun resolveDarkThemeStatic(themeMode: String, systemDark: Boolean): Boolean {
        return when (themeMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            else -> systemDark
        }
    }

    val isSystemModeFlow: Flow<Boolean> = themeModeFlow.map { it == ThemeMode.SYSTEM }

    suspend fun setThemeMode(mode: String) {
        userPreferencesRepository.setThemeMode(mode)
    }
}

// 应用状态栏和导航栏主题
@Composable
fun ApplyStatusBarTheme(isDarkTheme: Boolean) {
    val view = LocalView.current
    val window = (view.context as? ComponentActivity)?.window
    val configuration = LocalView.current.context.resources.configuration
    DisposableEffect(isDarkTheme) {
        window?.let {
            WindowCompat.getInsetsController(it, view).apply {
                isAppearanceLightStatusBars = !isDarkTheme
                isAppearanceLightNavigationBars = !isDarkTheme
                // 根据方向显示或隐藏状态栏
                if (configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
                    hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                } else {
                    show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                }
            }
        }
        onDispose {}
    }
}
