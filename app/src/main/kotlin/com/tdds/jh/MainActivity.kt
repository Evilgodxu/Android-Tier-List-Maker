package com.tdds.jh

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import com.tdds.jh.ui.theme.MyApplicationTheme
import com.tdds.jh.ui.theme.ThemeManager
import com.tdds.jh.ui.theme.ThemeMode
import java.util.Locale

class MainActivity : ComponentActivity() {

    private var saveDraftCallback: (() -> Unit)? = null
    private var isSkippingDraftSave = false

    /** 外部导入意图流，用于 onNewIntent 时通知 Composable 处理 */
    val externalIntentFlow = MutableStateFlow<Intent?>(null)

    private val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(applicationContext)
    }
    private lateinit var languageManager: LanguageManager
    private lateinit var themeManager: ThemeManager
    private lateinit var windowInsetsController: WindowInsetsControllerCompat

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences(SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
        val languageCode = prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
        val locale = resolveLocale(languageCode)
        val config = Configuration(newBase.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            config.setLocale(locale)
        }
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

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
        enableEdgeToEdge()
        setupSystemBars()

        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val versionName = packageInfo.versionName ?: ""
        @Suppress("UNUSED_VARIABLE")
        val versionCode = packageInfo.longVersionCode.toInt()

        ResourceManager.cleanupLogFiles(this)

        languageManager = LanguageManager(this, userPreferencesRepository)
        themeManager = ThemeManager(userPreferencesRepository)

        // 将初始 intent 发送到流中，由 Composable 统一处理外部导入
        externalIntentFlow.value = intent

        val scope = lifecycleScope
        val appRes = resources
        val savedLanguage = getSharedPreferences(SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
        setContent {
            // 首次启动语言检测
            LaunchedEffect(Unit) {
                val firstLaunch = userPreferencesRepository.isFirstLaunch.first()
                if (firstLaunch) {
                    val systemLocale = appRes.configuration.locales[0]
                    val autoLanguage = matchSystemLanguage(systemLocale.language)
                    getSharedPreferences(SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
                        .edit().putString(KEY_LANGUAGE, autoLanguage).commit()
                    // 更新 App 层 Resources 使首次启动即用正确语言
                    languageManager.applyAppLocale(resolveLocale(autoLanguage))
                    userPreferencesRepository.setLanguage(autoLanguage)
                    userPreferencesRepository.setFirstLaunch(false)
                    userPreferencesRepository.setShowLanguageOnFirstLaunch(true)
                }
            }

            val themeMode by userPreferencesRepository.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val disableCustomFont by userPreferencesRepository.disableCustomFont.collectAsStateWithLifecycle(initialValue = true)
            val currentLanguage by userPreferencesRepository.language.collectAsStateWithLifecycle(initialValue = savedLanguage)

            val systemDark = isSystemInDarkTheme()
            val isDarkTheme = themeManager.resolveDarkThemeStatic(themeMode, systemDark)
            val followSystemTheme = themeMode == ThemeMode.SYSTEM

            ProvideLocalizedContext(languageManager) {
                ProvideWindowSizeClass {
                    MyApplicationTheme(
                        darkTheme = isDarkTheme,
                        disableCustomFont = disableCustomFont
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background,
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
                                    getSharedPreferences(SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
                                        .edit().putString(KEY_LANGUAGE, languageCode).commit()
                                    languageManager.applyAppLocale(resolveLocale(languageCode))
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
    }

    private fun setupSystemBars() {
        windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        updateSystemBarsVisibility()
    }

    private fun updateSystemBarsVisibility() {
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
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
        // 其他配置变化时重新应用语言（旋转时 Activity 会重建，不需要处理）
        if (::languageManager.isInitialized) {
            val languageCode = getSharedPreferences(SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
            languageManager.applyAppLocale(resolveLocale(languageCode))
        }
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

    companion object {
        private const val SETTINGS_PREFS_NAME = "app_settings"
        private const val KEY_LANGUAGE = "language"
        private const val DEFAULT_LANGUAGE = "zh"

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

        fun resolveLocale(languageCode: String): Locale {
            return when (languageCode) {
                "zh" -> Locale.SIMPLIFIED_CHINESE
                "en" -> Locale.ENGLISH
                "ja" -> Locale.JAPANESE
                "ko" -> Locale.KOREAN
                "ru" -> Locale.forLanguageTag("ru")
                "de" -> Locale.GERMAN
                "fr" -> Locale.FRENCH
                "es" -> Locale.forLanguageTag("es")
                "ar" -> Locale.forLanguageTag("ar")
                "pt" -> Locale.forLanguageTag("pt")
                else -> Locale.SIMPLIFIED_CHINESE
            }
        }
    }
}
