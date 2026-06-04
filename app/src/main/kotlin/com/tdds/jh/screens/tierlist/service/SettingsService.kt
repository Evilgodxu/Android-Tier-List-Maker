package com.tdds.jh.screens.tierlist.service

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * 设置管理服务
 * 集中管理应用设置的读写操作
 */
class SettingsService(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    // 点击添加开关
    var disableClickAdd: Boolean
        get() = prefs.getBoolean(KEY_DISABLE_CLICK_ADD, true)
        set(value) = prefs.edit { putBoolean(KEY_DISABLE_CLICK_ADD, value) }

    // 浮显水平偏移
    var floatOffsetX: Float
        get() = prefs.getFloat(KEY_FLOAT_OFFSET_X, DEFAULT_FLOAT_OFFSET_X)
        set(value) = prefs.edit { putFloat(KEY_FLOAT_OFFSET_X, value) }

    // 浮显垂直偏移
    var floatOffsetY: Float
        get() = prefs.getFloat(KEY_FLOAT_OFFSET_Y, DEFAULT_FLOAT_OFFSET_Y)
        set(value) = prefs.edit { putFloat(KEY_FLOAT_OFFSET_Y, value) }

    // 外置小图开关
    var externalBadgeEnabled: Boolean
        get() = prefs.getBoolean(KEY_EXTERNAL_BADGE_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_EXTERNAL_BADGE_ENABLED, value) }

    // 下置命名开关
    var nameBelowImage: Boolean
        get() = prefs.getBoolean(KEY_NAME_BELOW_IMAGE, false)
        set(value) = prefs.edit { putBoolean(KEY_NAME_BELOW_IMAGE, value) }

    // 当前语言
    var currentLanguage: String
        get() = prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
        set(value) = prefs.edit { putString(KEY_LANGUAGE, value) }

    // 是否首次启动显示语言选择
    var showLanguageOnFirstLaunch: Boolean
        get() = prefs.getBoolean(KEY_SHOW_LANGUAGE_ON_FIRST_LAUNCH, true)
        set(value) = prefs.edit { putBoolean(KEY_SHOW_LANGUAGE_ON_FIRST_LAUNCH, value) }

    // 裁剪位置X
    var cropPositionX: Float
        get() = prefs.getFloat(KEY_CROP_POSITION_X, 0.5f)
        set(value) = prefs.edit { putFloat(KEY_CROP_POSITION_X, value) }

    // 裁剪位置Y
    var cropPositionY: Float
        get() = prefs.getFloat(KEY_CROP_POSITION_Y, 0.5f)
        set(value) = prefs.edit { putFloat(KEY_CROP_POSITION_Y, value) }

    // 自定义裁剪宽度
    var customCropWidth: Int
        get() = prefs.getInt(KEY_CUSTOM_CROP_WIDTH, 0)
        set(value) = prefs.edit { putInt(KEY_CUSTOM_CROP_WIDTH, value) }

    // 自定义裁剪高度
    var customCropHeight: Int
        get() = prefs.getInt(KEY_CUSTOM_CROP_HEIGHT, 0)
        set(value) = prefs.edit { putInt(KEY_CUSTOM_CROP_HEIGHT, value) }

    // 使用自定义裁剪尺寸
    var useCustomCropSize: Boolean
        get() = prefs.getBoolean(KEY_USE_CUSTOM_CROP_SIZE, false)
        set(value) = prefs.edit { putBoolean(KEY_USE_CUSTOM_CROP_SIZE, value) }

    // 裁剪比例
    var cropRatio: Float
        get() = prefs.getFloat(KEY_CROP_RATIO, 1f)
        set(value) = prefs.edit { putFloat(KEY_CROP_RATIO, value) }

    /**
     * 清除所有裁剪相关设置
     */
    fun clearCropSettings() {
        prefs.edit {
            remove(KEY_CROP_RATIO)
            remove(KEY_CUSTOM_CROP_WIDTH)
            remove(KEY_CUSTOM_CROP_HEIGHT)
            remove(KEY_USE_CUSTOM_CROP_SIZE)
        }
    }

    /**
     * 保存语言设置（独立方法，便于扩展）
     */
    fun saveLanguage(language: String) {
        prefs.edit {
            putString(KEY_LANGUAGE, language)
        }
    }

    companion object {
        private const val PREFS_NAME = "app_settings"

        // Key常量
        private const val KEY_DISABLE_CLICK_ADD = "disable_click_add"
        private const val KEY_FLOAT_OFFSET_X = "float_offset_x"
        private const val KEY_FLOAT_OFFSET_Y = "float_offset_y"
        private const val KEY_EXTERNAL_BADGE_ENABLED = "external_badge_enabled"
        private const val KEY_NAME_BELOW_IMAGE = "name_below_image"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_SHOW_LANGUAGE_ON_FIRST_LAUNCH = "show_language_on_first_launch"
        private const val KEY_CROP_POSITION_X = "crop_position_x"
        private const val KEY_CROP_POSITION_Y = "crop_position_y"
        private const val KEY_CUSTOM_CROP_WIDTH = "custom_crop_width"
        private const val KEY_CUSTOM_CROP_HEIGHT = "custom_crop_height"
        private const val KEY_USE_CUSTOM_CROP_SIZE = "use_custom_crop_size"
        private const val KEY_CROP_RATIO = "crop_ratio"

        // 默认值
        const val DEFAULT_FLOAT_OFFSET_X = 125f
        const val DEFAULT_FLOAT_OFFSET_Y = 85f
        const val DEFAULT_LANGUAGE = "zh"
    }
}
