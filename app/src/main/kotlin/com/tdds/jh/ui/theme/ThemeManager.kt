package com.tdds.jh.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.activity.ComponentActivity

/**
 * 主题管理器
 * 负责管理应用的深色/浅色模式切换逻辑
 */
object ThemeManager {

    private const val PREFS_NAME = "app_settings"
    private const val KEY_FOLLOW_SYSTEM_THEME = "follow_system_theme"
    private const val KEY_DARK_THEME = "dark_theme"

    /**
     * 主题状态数据类
     */
    data class ThemeState(
        val isDarkTheme: Boolean,
        val followSystemTheme: Boolean
    )

    /**
     * 获取 SharedPreferences
     */
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 加载主题设置
     * @param context 上下文
     * @param systemDarkTheme 系统当前是否为深色主题
     * @return 主题状态
     */
    fun loadThemeState(context: Context, systemDarkTheme: Boolean): ThemeState {
        val prefs = getPrefs(context)
        val followSystemTheme = prefs.getBoolean(KEY_FOLLOW_SYSTEM_THEME, true)
        val isDarkTheme = if (followSystemTheme) {
            systemDarkTheme
        } else {
            prefs.getBoolean(KEY_DARK_THEME, systemDarkTheme)
        }
        return ThemeState(isDarkTheme, followSystemTheme)
    }

    /**
     * 保存主题设置
     * @param context 上下文
     * @param isDarkTheme 是否为深色主题
     * @param followSystemTheme 是否跟随系统主题
     */
    fun saveThemeState(context: Context, isDarkTheme: Boolean, followSystemTheme: Boolean) {
        val prefs = getPrefs(context)
        prefs.edit()
            .putBoolean(KEY_DARK_THEME, isDarkTheme)
            .putBoolean(KEY_FOLLOW_SYSTEM_THEME, followSystemTheme)
            .apply()
    }

    /**
     * 保存深色主题设置
     * @param context 上下文
     * @param isDarkTheme 是否为深色主题
     */
    fun saveDarkTheme(context: Context, isDarkTheme: Boolean) {
        val prefs = getPrefs(context)
        prefs.edit()
            .putBoolean(KEY_DARK_THEME, isDarkTheme)
            .apply()
    }

    /**
     * 保存跟随系统主题设置
     * @param context 上下文
     * @param followSystemTheme 是否跟随系统主题
     */
    fun saveFollowSystemTheme(context: Context, followSystemTheme: Boolean) {
        val prefs = getPrefs(context)
        prefs.edit()
            .putBoolean(KEY_FOLLOW_SYSTEM_THEME, followSystemTheme)
            .apply()
    }

    /**
     * 切换主题
     * @param context 上下文
     * @param currentState 当前主题状态
     * @return 新的主题状态
     */
    fun toggleTheme(context: Context, currentState: ThemeState): ThemeState {
        val newDarkTheme = !currentState.isDarkTheme
        val newFollowSystem = false
        saveThemeState(context, newDarkTheme, newFollowSystem)
        return ThemeState(newDarkTheme, newFollowSystem)
    }

    /**
     * 设置跟随系统主题
     * @param context 上下文
     * @param followSystem 是否跟随系统
     * @param systemDarkTheme 系统当前是否为深色主题
     * @return 新的主题状态
     */
    fun setFollowSystemTheme(
        context: Context,
        followSystem: Boolean,
        systemDarkTheme: Boolean
    ): ThemeState {
        val isDarkTheme = if (followSystem) {
            systemDarkTheme
        } else {
            getPrefs(context).getBoolean(KEY_DARK_THEME, systemDarkTheme)
        }
        saveThemeState(context, isDarkTheme, followSystem)
        return ThemeState(isDarkTheme, followSystem)
    }

    /**
     * 获取系统主题状态
     * @return 系统是否为深色主题
     */
    @Composable
    fun getSystemInDarkTheme(): Boolean {
        return isSystemInDarkTheme()
    }

    /**
     * 应用状态栏主题
     * @param isDarkTheme 是否为深色主题
     */
    @Composable
    fun ApplyStatusBarTheme(isDarkTheme: Boolean) {
        val view = LocalView.current
        val window = (view.context as? ComponentActivity)?.window
        DisposableEffect(isDarkTheme) {
            window?.let {
                WindowCompat.getInsetsController(it, view).apply {
                    isAppearanceLightStatusBars = !isDarkTheme
                    isAppearanceLightNavigationBars = !isDarkTheme
                }
            }
            onDispose {}
        }
    }

    /**
     * 创建主题状态的可观察对象
     * @param context 上下文
     * @return 主题状态的 MutableState
     */
    @Composable
    fun rememberThemeState(context: Context): MutableState<ThemeState> {
        val systemInDarkTheme = getSystemInDarkTheme()
        val initialState = remember {
            loadThemeState(context, systemInDarkTheme)
        }
        val themeState = remember { mutableStateOf(initialState) }

        // 监听系统主题变化（仅在跟随系统时）
        if (themeState.value.followSystemTheme) {
            val currentSystemTheme = getSystemInDarkTheme()
            if (currentSystemTheme != themeState.value.isDarkTheme) {
                themeState.value = themeState.value.copy(isDarkTheme = currentSystemTheme)
            }
        }

        return themeState
    }
}
