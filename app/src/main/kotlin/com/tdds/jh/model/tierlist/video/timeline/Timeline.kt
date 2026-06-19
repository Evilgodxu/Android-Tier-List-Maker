package com.tdds.jh.model.tierlist.video.timeline

/**
 * 视频时间线
 *
 * @param actions 动作时间戳列表
 * @param audioSegments 音频段列表
 * @param totalDuration 总时长（秒）
 */
data class Timeline(
    val actions: List<TimelineAction>,
    val audioSegments: List<AudioSegment>,
    val totalDuration: Float
)
