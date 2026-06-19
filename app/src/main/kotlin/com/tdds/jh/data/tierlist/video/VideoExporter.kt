package com.tdds.jh.data.tierlist.video

import android.content.Context
import com.tdds.jh.model.tierlist.TierImage
import com.tdds.jh.model.tierlist.TierItem
import com.tdds.jh.model.tierlist.video.VideoGenerationConfig
import com.tdds.jh.model.tierlist.video.frame.FrameSequenceGenerator
import com.tdds.jh.model.tierlist.video.frame.VideoFrame
import com.tdds.jh.model.tierlist.video.timeline.TimelineBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 视频导出器
 *
 * 封装时间线构建、音频合成、帧渲染与视频编码的完整流程。
 */
class VideoExporter(private val context: Context) {

    /**
     * 导出视频
     *
     * 首次尝试使用配置分辨率导出；若编码器失败且分辨率大于 640x360，
     * 则自动将分辨率减半后重试一次。
     *
     * @param outputFile 输出 MP4 文件
     * @param onProgress 进度回调（progress, renderedFrames, totalFrames）
     * @param onCancel 取消检查
     */
    suspend fun export(
        tiers: List<TierItem>,
        tierImages: List<TierImage>,
        config: VideoGenerationConfig,
        isDarkTheme: Boolean,
        disableCustomFont: Boolean,
        externalBadgeEnabled: Boolean,
        nameBelowImage: Boolean,
        title: String,
        authorName: String,
        outputFile: File,
        onProgress: (Float, Int, Int) -> Unit = { _, _, _ -> },
        onCancel: () -> Boolean = { false }
    ): Boolean = withContext(Dispatchers.IO) {
        val success = exportInternal(
            tiers, tierImages, config,
            isDarkTheme, disableCustomFont, externalBadgeEnabled, nameBelowImage,
            title, authorName, outputFile, onProgress, onCancel
        )
        if (success) return@withContext true

        if (config.outputWidth > 640 && config.outputHeight > 360) {
            val fallbackConfig = config.copy(
                outputWidth = config.outputWidth / 2,
                outputHeight = config.outputHeight / 2
            )
            return@withContext exportInternal(
                tiers, tierImages, fallbackConfig,
                isDarkTheme, disableCustomFont, externalBadgeEnabled, nameBelowImage,
                title, authorName, outputFile, onProgress, onCancel
            )
        }
        false
    }

    private suspend fun exportInternal(
        tiers: List<TierItem>,
        tierImages: List<TierImage>,
        config: VideoGenerationConfig,
        isDarkTheme: Boolean,
        disableCustomFont: Boolean,
        externalBadgeEnabled: Boolean,
        nameBelowImage: Boolean,
        title: String,
        authorName: String,
        outputFile: File,
        onProgress: (Float, Int, Int) -> Unit,
        onCancel: () -> Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        onProgress(0f, 0, 0)

        val timeline = TimelineBuilder(
            config,
            AudioDurationProvider(context)
        ).build(tiers, tierImages)

        if (timeline.totalDuration <= 0f) {
            onProgress(1f, 0, 0)
            return@withContext false
        }

        val audioFile = File(context.cacheDir, "export_audio_${System.currentTimeMillis()}.mp4")
        val audioSuccess = AudioMixer(context).mix(
            timeline = timeline,
            config = config,
            outputFile = audioFile,
            progressCallback = { progress ->
                onProgress(0.2f * progress, 0, 0)
            }
        )

        if (onCancel()) {
            audioFile.delete()
            return@withContext false
        }

        val keyFrames = FrameSequenceGenerator().generateKeyFrames(timeline, config)
        val renderer = BitmapFrameRenderer(context)
        var lastKeyFrameIndex = -1
        var cachedBitmap: android.graphics.Bitmap? = null

        if (onCancel()) {
            cachedBitmap?.recycle()
            audioFile.delete()
            return@withContext false
        }

        val encoder = VideoEncoder(
            width = config.outputWidth,
            height = config.outputHeight,
            frameRate = 30,
            bitRate = config.outputWidth * config.outputHeight * 4
        )
        val videoSuccess = encoder.encode(
            frames = keyFrames,
            frameProvider = { frame ->
                val index = keyFrames.indexOf(frame)
                if (index != lastKeyFrameIndex || cachedBitmap == null) {
                    cachedBitmap?.recycle()
                    cachedBitmap = renderer.renderFrame(
                        tiers = tiers,
                        tierImages = tierImages,
                        config = config,
                        timeline = timeline,
                        currentTime = frame.timeSeconds,
                        isDarkTheme = isDarkTheme,
                        disableCustomFont = disableCustomFont,
                        externalBadgeEnabled = externalBadgeEnabled,
                        nameBelowImage = nameBelowImage,
                        title = title,
                        authorName = authorName
                    )
                    lastKeyFrameIndex = index
                }
                cachedBitmap ?: throw IllegalStateException("Frame bitmap not rendered")
            },
            audioFile = if (audioSuccess) audioFile else null,
            outputFile = outputFile,
            progressCallback = { progress ->
                onProgress(0.2f + 0.8f * progress, keyFrames.size, keyFrames.size)
            },
            onCancel = onCancel
        )

        cachedBitmap?.recycle()
        audioFile.delete()

        onProgress(1f, keyFrames.size, keyFrames.size)
        return@withContext videoSuccess
    }
}
