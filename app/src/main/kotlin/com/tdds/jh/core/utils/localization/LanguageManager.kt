package com.tdds.jh.core.utils.localization

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import com.tdds.jh.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

// 语言管理器，处理应用语言切换（支持 10 种语言）
class LanguageManager(
    private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    // Activity 必须实现 ActivityResultRegistryOwner
    val registryOwner: ActivityResultRegistryOwner = context as ActivityResultRegistryOwner

    val languageFlow: Flow<String> = userPreferencesRepository.language

    val localeFlow: Flow<Locale> = languageFlow.map { languageCode ->
        resolveLocale(languageCode)
    }

    companion object {
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
                else -> Locale.getDefault()
            }
        }
    }

    fun createLocalizedContext(locale: Locale): Context {
        val config = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            config.setLocale(locale)
        }
        return context.createConfigurationContext(config)
    }

    // 同步更新 App 层 Resources，使 Dialog 窗口继承正确的 locale
    fun applyAppLocale(locale: Locale) {
        val appConfig = Configuration(context.applicationContext.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            appConfig.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            appConfig.setLocale(locale)
        }
        @Suppress("DEPRECATION")
        context.applicationContext.resources.updateConfiguration(appConfig, context.applicationContext.resources.displayMetrics)

        val activityConfig = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            activityConfig.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            activityConfig.setLocale(locale)
        }
        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(activityConfig, context.resources.displayMetrics)

        @Suppress("DEPRECATION")
        Locale.setDefault(locale)
    }
}

// 提供本地化 Context 的 Composable，语言切换无需 recreate
@Composable
fun ProvideLocalizedContext(
    languageManager: LanguageManager,
    content: @Composable () -> Unit,
) {
    val locale by languageManager.localeFlow.collectAsState(
        initial = LocalLocale.current.platformLocale,
    )
    val localizedContext = languageManager.createLocalizedContext(locale)
    // 透传 ActivityResultRegistryOwner:
    // LocalContext 替换后父级 CompositionLocal 会丢失，从 Activity 补全
    val registryOwner = LocalActivityResultRegistryOwner.current
        ?: languageManager.registryOwner

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalActivityResultRegistryOwner provides registryOwner,
    ) {
        content()
    }
}
