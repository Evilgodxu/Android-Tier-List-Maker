package com.tdds.jh.model.tierlist.video.frame

import android.graphics.Bitmap

/**
 * 视频帧
 *
 * @param timeSeconds 该帧对应的时间点（秒）
 * @param stateSnapshot 该时间点下所有图片的显示状态快照
 * @param bitmap 已渲染的位图（可选，延迟填充）
 */
data class VideoFrame(
    val timeSeconds: Float,
    val stateSnapshot: Map<String, FrameState>,
    var bitmap: Bitmap? = null
)
