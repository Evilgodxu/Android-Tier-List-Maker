package com.tdds.jh.data.tierlist.video

import android.content.Context
import android.net.Uri
import com.tdds.jh.model.tierlist.video.ArrangementGranularity
import com.tdds.jh.model.tierlist.video.AudioIntervalSource
import com.tdds.jh.model.tierlist.video.AudioOverlayMode
import com.tdds.jh.model.tierlist.video.GranularityMode
import com.tdds.jh.model.tierlist.video.NameDisplayMode
import com.tdds.jh.model.tierlist.video.VideoActionType
import com.tdds.jh.model.tierlist.video.VideoGenerationConfig
import com.tdds.jh.model.tierlist.video.VideoPreset
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * 编排预设管理器
 *
 * 管理 JSON 格式的视频编排预设，与榜单数据解耦。
 */
class VideoPresetManager(private val context: Context) {

    private val presetsDir: File by lazy {
        File(context.filesDir, PRESETS_FOLDER_NAME).apply { mkdirs() }
    }

    /**
     * 保存预设
     */
    fun save(preset: VideoPreset): Boolean {
        return try {
            val file = File(presetsDir, "${preset.id}$EXTENSION")
            file.writeText(presetToJson(preset).toString())
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 更新预设
     */
    fun update(preset: VideoPreset): Boolean {
        return save(preset)
    }

    /**
     * 删除预设
     */
    fun delete(presetId: String): Boolean {
        return File(presetsDir, "$presetId$EXTENSION").delete()
    }

    /**
     * 加载指定预设
     */
    fun load(presetId: String): VideoPreset? {
        val file = File(presetsDir, "$presetId$EXTENSION")
        if (!file.exists()) return null
        return try {
            presetFromJson(JSONObject(file.readText()))
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 列出所有预设
     */
    fun list(): List<VideoPreset> {
        return presetsDir.listFiles { _, name -> name.endsWith(EXTENSION) }
            ?.mapNotNull { file ->
                try {
                    presetFromJson(JSONObject(file.readText()))
                } catch (_: Exception) {
                    null
                }
            }
            ?.sortedByDescending { it.createTime }
            ?: emptyList()
    }

    /**
     * 重命名
     */
    fun rename(presetId: String, newName: String): Boolean {
        val preset = load(presetId) ?: return false
        return save(preset.copy(name = newName))
    }

    /**
     * 设置分组
     */
    fun setGroup(presetId: String, group: String): Boolean {
        val preset = load(presetId) ?: return false
        return save(preset.copy(group = group))
    }

    /**
     * 切换收藏
     */
    fun toggleFavorite(presetId: String): Boolean {
        val preset = load(presetId) ?: return false
        return save(preset.copy(isFavorite = !preset.isFavorite))
    }

    /**
     * 导出预设到 URI
     */
    fun export(presetId: String, uri: Uri): Boolean {
        val preset = load(presetId) ?: return false
        return try {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(presetToJson(preset).toString().toByteArray())
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 从 URI 导入预设
     */
    fun import(uri: Uri): VideoPreset? {
        return try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader().readText()
            } ?: return null
            val preset = presetFromJson(JSONObject(jsonString)) ?: return null
            val newPreset = preset.copy(id = UUID.randomUUID().toString())
            save(newPreset)
            newPreset
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        const val PRESETS_FOLDER_NAME = "VideoPresets"
        const val EXTENSION = ".tdv"
    }
}

private fun presetToJson(preset: VideoPreset): JSONObject {
    return JSONObject().apply {
        put("id", preset.id)
        put("name", preset.name)
        put("group", preset.group)
        put("isFavorite", preset.isFavorite)
        put("createTime", preset.createTime)
        put("config", configToJson(preset.config))
    }
}

private fun presetFromJson(json: JSONObject): VideoPreset? {
    return try {
        VideoPreset(
            id = json.getString("id"),
            name = json.getString("name"),
            group = json.optString("group", ""),
            isFavorite = json.optBoolean("isFavorite", false),
            createTime = json.optLong("createTime", System.currentTimeMillis()),
            config = configFromJson(json.getJSONObject("config")) ?: return null
        )
    } catch (_: Exception) {
        null
    }
}

private fun configToJson(config: VideoGenerationConfig): JSONObject {
    return JSONObject().apply {
        put("actionOrder", JSONArray(config.actionOrder.map { it.name }))
        put("granularity", config.granularity.name)
        put("mixedGranularity", JSONObject().apply {
            config.mixedGranularity.forEach { (type, mode) ->
                put(type.name, mode.name)
            }
        })
        put("imageIntervalSource", config.imageIntervalSource.name)
        put("fixedImageInterval", config.fixedImageInterval)
        put("badgeInterval", config.badgeInterval)
        put("nameDisplayMode", config.nameDisplayMode.name)
        put("nameCharInterval", config.nameCharInterval)
        put("crossTypePause", config.crossTypePause)
        put("crossImagePause", config.crossImagePause)
        put("extraAudioOffset", config.extraAudioOffset)
        put("audioOverlayMode", config.audioOverlayMode.name)
        put("backgroundMusicUri", config.backgroundMusicUri ?: JSONObject.NULL)
        put("backgroundMusicVolume", config.backgroundMusicVolume)
        put("narrationVolume", config.narrationVolume)
        put("sfxVolume", config.sfxVolume)
        put("outputWidth", config.outputWidth)
        put("outputHeight", config.outputHeight)
    }
}

private fun configFromJson(json: JSONObject): VideoGenerationConfig? {
    return try {
        VideoGenerationConfig(
            actionOrder = json.getJSONArray("actionOrder").let { array ->
                List(array.length()) { index ->
                    VideoActionType.valueOf(array.getString(index))
                }
            },
            granularity = ArrangementGranularity.valueOf(json.getString("granularity")),
            mixedGranularity = json.getJSONObject("mixedGranularity").let { obj ->
                VideoActionType.entries.associateWith { type ->
                    try {
                        GranularityMode.valueOf(obj.getString(type.name))
                    } catch (_: Exception) {
                        GranularityMode.PER_IMAGE
                    }
                }
            },
            imageIntervalSource = AudioIntervalSource.valueOf(json.getString("imageIntervalSource")),
            fixedImageInterval = json.getDouble("fixedImageInterval").toFloat(),
            badgeInterval = json.getDouble("badgeInterval").toFloat(),
            nameDisplayMode = NameDisplayMode.valueOf(json.getString("nameDisplayMode")),
            nameCharInterval = json.getDouble("nameCharInterval").toFloat(),
            crossTypePause = json.getDouble("crossTypePause").toFloat(),
            crossImagePause = json.getDouble("crossImagePause").toFloat(),
            extraAudioOffset = json.getDouble("extraAudioOffset").toFloat(),
            audioOverlayMode = AudioOverlayMode.valueOf(json.getString("audioOverlayMode")),
            backgroundMusicUri = json.optString("backgroundMusicUri").takeIf { it != "null" },
            backgroundMusicVolume = json.getDouble("backgroundMusicVolume").toFloat(),
            narrationVolume = json.getDouble("narrationVolume").toFloat(),
            sfxVolume = json.getDouble("sfxVolume").toFloat(),
            outputWidth = json.getInt("outputWidth"),
            outputHeight = json.getInt("outputHeight")
        )
    } catch (_: Exception) {
        null
    }
}
