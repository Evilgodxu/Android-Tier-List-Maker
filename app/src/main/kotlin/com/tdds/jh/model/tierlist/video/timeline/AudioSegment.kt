package com.tdds.jh.model.tierlist.video.timeline

import android.net.Uri

/**
 * 时间线上的音频段
 *
 * @param tierImageId 关联图片 ID
 * @param uri 音频文件 URI
 * @param startTime 开始时间（秒）
 * @param duration 持续时长（秒）
 */
data class AudioSegment(
    val tierImageId: String,
    val uri: Uri,
    val startTime: Float,
    val duration: Float
)
