package com.tdds.jh.model.tierlist.video

/**
 * 编排预设
 *
 * 与具体榜单数据解耦，仅保存视频生成配置。
 */
data class VideoPreset(
    val id: String,
    val name: String,
    val group: String,
    val isFavorite: Boolean,
    val createTime: Long,
    val config: VideoGenerationConfig
)
