package com.tdds.jh.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

// 用户偏好设置仓库，统一管理主题和语言偏好
class UserPreferencesRepository(private val context: Context) {
    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val DISABLE_CUSTOM_FONT = booleanPreferencesKey("disable_custom_font")
        val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
        val SHOW_LANGUAGE_ON_FIRST_LAUNCH = booleanPreferencesKey("show_language_on_first_launch")
    }

    val themeMode: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.THEME_MODE] ?: "system"
        }

    val language: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.LANGUAGE] ?: "system"
        }

    val disableCustomFont: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.DISABLE_CUSTOM_FONT] ?: true
        }

    val isFirstLaunch: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.IS_FIRST_LAUNCH] ?: true
        }

    val showLanguageOnFirstLaunch: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SHOW_LANGUAGE_ON_FIRST_LAUNCH] ?: true
        }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode
        }
    }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LANGUAGE] = language
        }
    }

    suspend fun setDisableCustomFont(disabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DISABLE_CUSTOM_FONT] = disabled
        }
    }

    suspend fun setFirstLaunch(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_FIRST_LAUNCH] = value
        }
    }

    suspend fun setShowLanguageOnFirstLaunch(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_LANGUAGE_ON_FIRST_LAUNCH] = value
        }
    }
}
