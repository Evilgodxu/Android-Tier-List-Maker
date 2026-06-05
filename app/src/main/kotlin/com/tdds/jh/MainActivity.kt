package com.tdds.jh

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.tdds.jh.core.utils.localization.LanguageManager
import com.tdds.jh.core.utils.localization.ProvideLocalizedContext
import com.tdds.jh.data.repository.UserPreferencesRepository
import com.tdds.jh.data.tierlist.ResourceManager
import com.tdds.jh.screens.tierlist.TierListMakerApp
import com.tdds.jh.ui.adaptive.ProvideWindowSizeClass
import com.tdds.jh.ui.theme.ApplyStatusBarTheme
import com.tdds.jh.ui.theme.MyApplicationTheme
import com.tdds.jh.ui.theme.ThemeManager
import com.tdds.jh.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {

    private var saveDraftCallback: (() -> Unit)? = null
    private var isSkippingDraftSave = false

    private lateinit var windowInsetsController: WindowInsetsControllerCompat

    /** 外部导入意图流，用于 onNewIntent 时通知 Composable 处理 */
    val externalIntentFlow = MutableStateFlow<Intent?>(null)

    private val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(applicationContext)
    }
    private lateinit var languageManager: LanguageManager
    private lateinit var themeManager: ThemeManager

    private fun skipDraftSaveTemporarily() {
        isSkippingDraftSave = true
    }

    private fun resumeDraftSave() {
        isSkippingDraftSave = false
    }

    override fun onDestroy() {
        saveDraftCallback = null
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val versionName = packageInfo.versionName ?: ""
        @Suppress("UNUSED_VARIABLE")
        val versionCode = packageInfo.longVersionCode.toInt()

        ResourceManager.cleanupLogFiles(this)

        languageManager = LanguageManager(this, userPreferencesRepository)
        themeManager = ThemeManager(userPreferencesRepository)

        enableEdgeToEdge()
        windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        updateSystemBarsVisibility()

        // 将初始 intent 发送到流中，由 Composable 统一处理外部导入
        externalIntentFlow.value = intent

        val scope = lifecycleScope
        val appRes = resources
        setContent {
            // 首次启动语言检测
            LaunchedEffect(Unit) {
                val firstLaunch = userPreferencesRepository.isFirstLaunch.first()
                if (firstLaunch) {
                    val systemLocale = appRes.configuration.locales[0]
                    val autoLanguage = matchSystemLanguage(systemLocale.language)
                    userPreferencesRepository.setLanguage(autoLanguage)
                    userPreferencesRepository.setFirstLaunch(false)
                    userPreferencesRepository.setShowLanguageOnFirstLaunch(true)
                }
            }

            val themeMode by userPreferencesRepository.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val disableCustomFont by userPreferencesRepository.disableCustomFont.collectAsStateWithLifecycle(initialValue = true)
            val currentLanguage by userPreferencesRepository.language.collectAsStateWithLifecycle(initialValue = "system")

            val systemDark = isSystemInDarkTheme()
            val isDarkTheme = themeManager.resolveDarkThemeStatic(themeMode, systemDark)
            val followSystemTheme = themeMode == ThemeMode.SYSTEM

            ApplyStatusBarTheme(isDarkTheme)

            ProvideLocalizedContext(languageManager) {
                ProvideWindowSizeClass {
                    MyApplicationTheme(
                        darkTheme = isDarkTheme,
                        disableCustomFont = disableCustomFont
                    ) {
                        TierListMakerApp(
                        externalIntentFlow = externalIntentFlow,
                        isDarkTheme = isDarkTheme,
                        followSystemTheme = followSystemTheme,
                        disableCustomFont = disableCustomFont,
                        currentLanguage = currentLanguage,
                        onDisableCustomFontChange = { newValue ->
                            scope.launch {
                                userPreferencesRepository.setDisableCustomFont(newValue)
                            }
                        },
                        onThemeChange = { newDarkTheme ->
                            scope.launch {
                                val newMode = if (newDarkTheme) ThemeMode.DARK else ThemeMode.LIGHT
                                themeManager.setThemeMode(newMode)
                            }
                        },
                        onFollowSystemThemeChange = { followSystem ->
                            scope.launch {
                                val newMode = if (followSystem) ThemeMode.SYSTEM
                                else if (isDarkTheme) ThemeMode.DARK else ThemeMode.LIGHT
                                themeManager.setThemeMode(newMode)
                            }
                        },
                        onLanguageChange = { languageCode ->
                            scope.launch {
                                userPreferencesRepository.setLanguage(languageCode)
                            }
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
        }
    }

    fun exitAppWithCleanup() {
        finishAffinity()
        try {
            saveDraftCallback?.invoke()
        } catch (_: Exception) {
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        externalIntentFlow.value = intent
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateSystemBarsVisibility()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (isSkippingDraftSave) return
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    saveDraftCallback?.invoke()
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun updateSystemBarsVisibility() {
        if (!::windowInsetsController.isInitialized) return
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    companion object {
        fun matchSystemLanguage(systemLanguage: String): String {
            return when (systemLanguage) {
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
        }
    }
}
