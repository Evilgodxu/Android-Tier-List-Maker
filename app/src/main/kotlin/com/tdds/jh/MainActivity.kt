package com.tdds.jh

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import com.tdds.jh.data.tierlist.PresetManager
import com.tdds.jh.data.tierlist.ResourceManager
import com.tdds.jh.ui.theme.MyApplicationTheme
import com.tdds.jh.ui.theme.ThemeManager
import com.tdds.jh.screens.tierlist.TierListMakerApp
import com.tdds.jh.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    // 草稿保存回调，用于在双击退出时触发保存
    private var saveDraftCallback: (() -> Unit)? = null

    // 标记是否正在执行不需要保存草稿的操作（如打开图片选择器、文件选择器等）
    // 当此标记为 true 时，onUserLeaveHint 不会触发草稿保存
    private var isSkippingDraftSave = false

    // 临时禁用草稿保存，用于执行特定操作时
    private fun skipDraftSaveTemporarily() {
        isSkippingDraftSave = true
    }

    // 恢复草稿保存
    private fun resumeDraftSave() {
        isSkippingDraftSave = false
    }

    override fun onDestroy() {
        saveDraftCallback = null
        super.onDestroy()
    }

    override fun attachBaseContext(newBase: android.content.Context) {
        val prefs = newBase.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        val isFirstLaunch = prefs.getBoolean("is_first_launch", true)
        val language = if (isFirstLaunch) {
            val systemLocale = newBase.resources.configuration.locales[0]
            val systemLanguage = systemLocale.language
            val autoLanguage = when (systemLanguage) {
                "zh" -> "zh"
                "en" -> "en"
                "ja" -> "ja"
                "ko" -> "ko"
                "ru" -> "ru"
                "de" -> "de"
                "fr" -> "fr"
                "es" -> "es"
                "ar" -> "ar"
                "pt" -> "pt"
                else -> "zh"
            }
            prefs.edit()
                .putString("language", autoLanguage)
                .putBoolean("is_first_launch", false)
                .putBoolean("show_language_on_first_launch", true)
                .apply()
            autoLanguage
        } else {
            prefs.getString("language", "zh") ?: "zh"
        }
        val locale = java.util.Locale.forLanguageTag(language)
        val config = android.content.res.Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val versionName = packageInfo.versionName ?: ""
        val versionCode = packageInfo.longVersionCode.toInt()

        // 启动时仅清理7天前的日志文件，保留其他缓存以提高启动速度
        ResourceManager.cleanupLogFiles(this)

        enableEdgeToEdge()
        setContent {
            val themeState = ThemeManager.rememberThemeState(this)
            val isDarkTheme = themeState.value.isDarkTheme
            val systemInDarkTheme = ThemeManager.getSystemInDarkTheme()

            val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            var disableCustomFont by remember { mutableStateOf(prefs.getBoolean("disable_custom_font", true)) }

            ThemeManager.ApplyStatusBarTheme(isDarkTheme)

            MyApplicationTheme(
                darkTheme = isDarkTheme,
                disableCustomFont = disableCustomFont
            ) {
                TierListMakerApp(
                    isDarkTheme = isDarkTheme,
                    followSystemTheme = themeState.value.followSystemTheme,
                    disableCustomFont = disableCustomFont,
                    onDisableCustomFontChange = { newValue ->
                        disableCustomFont = newValue
                        prefs.edit().putBoolean("disable_custom_font", newValue).apply()
                    },
                    onThemeChange = { newTheme ->
                        val newState = ThemeManager.toggleTheme(this, themeState.value)
                        themeState.value = newState
                    },
                    onFollowSystemThemeChange = { newValue ->
                        val newState = ThemeManager.setFollowSystemTheme(this, newValue, systemInDarkTheme)
                        themeState.value = newState
                    },
                    onRegisterSaveDraftCallback = { callback ->
                        saveDraftCallback = callback
                    },
                    onSaveDraftForResourceManager = {
                        saveDraftCallback?.invoke()
                    },
                    onSkipDraftSave = {
                        skipDraftSaveTemporarily()
                    },
                    onResumeDraftSave = {
                        resumeDraftSave()
                    },
                    onExitApp = {
                        exitAppWithCleanup()
                    }
                )
            }
        }
    }

    /**
     * 双击退出时立即返回桌面，同步保存草稿，后台清理资源
     */
    fun exitAppWithCleanup() {
        finishAffinity()

        try {
            saveDraftCallback?.invoke()
        } catch (_: Exception) {
        }
    }

    /**
     * 当用户离开Activity时调用（如按Home键、切换到其他应用）
     */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()

        if (isSkippingDraftSave) {
            return
        }

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    saveDraftCallback?.invoke()
                }
            } catch (_: Exception) {
            }
        }
    }
}
