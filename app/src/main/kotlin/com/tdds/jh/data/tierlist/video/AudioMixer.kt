package com.tdds.jh.data.tierlist.video

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.tdds.jh.model.tierlist.video.AudioOverlayMode
import com.tdds.jh.model.tierlist.video.VideoGenerationConfig
import com.tdds.jh.model.tierlist.video.timeline.Timeline
import com.tdds.jh.model.tierlist.video.timeline.TimelineAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.sin

/**
 * 音频合成引擎
 *
 * 将解说音频、背景音乐、动作音效按时间线混音为单条 AAC 音轨。
 */
class AudioMixer(private val context: Context) {

    /**
     * 混音并输出到文件
     *
     * @param outputFile 输出 MP4/AAC 文件路径
     * @param progressCallback 进度回调 0~1
     */
    suspend fun mix(
        timeline: Timeline,
        config: VideoGenerationConfig,
        outputFile: File,
        progressCallback: (Float) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        val sampleRate = SAMPLE_RATE
        val channels = CHANNELS
        val totalDuration = timeline.totalDuration
        if (totalDuration <= 0f) {
            progressCallback(1f)
            return@withContext false
        }

        val totalSamples = (totalDuration * sampleRate * channels).toLong()
        if (totalSamples > Int.MAX_VALUE) {
            progressCallback(1f)
            return@withContext false
        }

        val mixBuffer = IntArray(totalSamples.toInt())

        mixNarration(timeline, config, mixBuffer, sampleRate, channels, progressCallback)
        mixBackgroundMusic(config, mixBuffer, sampleRate, channels, progressCallback)
        mixSoundEffects(timeline, config, mixBuffer, sampleRate, channels, progressCallback)

        val finalPcm = ShortArray(mixBuffer.size) { i ->
            mixBuffer[i].coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        progressCallback(1f)
        AacEncoder(sampleRate, channels).encode(finalPcm, outputFile)
    }

    private suspend fun mixNarration(
        timeline: Timeline,
        config: VideoGenerationConfig,
        mixBuffer: IntArray,
        sampleRate: Int,
        channels: Int,
        progressCallback: (Float) -> Unit
    ) {
        val segments = timeline.audioSegments
        if (segments.isEmpty()) {
            progressCallback(0.3f)
            return
        }
        segments.forEachIndexed { index, segment ->
            val pcm = AudioDecoder.decodeToPcm(context, segment.uri, sampleRate, channels)
            if (pcm != null) {
                val startSample = (segment.startTime * sampleRate * channels).toInt()
                mixPcm(mixBuffer, pcm, startSample, config.narrationVolume)
            }
            progressCallback(0.3f * (index + 1) / segments.size)
        }
    }

    private suspend fun mixBackgroundMusic(
        config: VideoGenerationConfig,
        mixBuffer: IntArray,
        sampleRate: Int,
        channels: Int,
        progressCallback: (Float) -> Unit
    ) {
        val bgmUriString = config.backgroundMusicUri
        if (bgmUriString.isNullOrBlank() || config.backgroundMusicVolume <= 0f) {
            progressCallback(0.6f)
            return
        }
        val bgmUri = bgmUriString.toUri()
        val pcm = AudioDecoder.decodeToPcm(context, bgmUri, sampleRate, channels)
        if (pcm != null && pcm.isNotEmpty()) {
            mixLoopPcm(mixBuffer, pcm, 0, config.backgroundMusicVolume)
        }
        progressCallback(0.6f)
    }

    private suspend fun mixSoundEffects(
        timeline: Timeline,
        config: VideoGenerationConfig,
        mixBuffer: IntArray,
        sampleRate: Int,
        channels: Int,
        progressCallback: (Float) -> Unit
    ) {
        if (config.audioOverlayMode == AudioOverlayMode.REPLACE || config.sfxVolume <= 0f) {
            progressCallback(0.9f)
            return
        }

        val placeSfx = generateBeepPcm(0.15f, 800.0, sampleRate, channels)
        val nameSfx = generateBeepPcm(0.1f, 1200.0, sampleRate, channels)
        val badgeSfx = generateBeepPcm(0.1f, 1600.0, sampleRate, channels)

        timeline.actions.forEach { action ->
            val sfx = when (action) {
                is TimelineAction.Place -> placeSfx
                is TimelineAction.Name -> nameSfx
                is TimelineAction.Badge -> badgeSfx
            }
            val startSample = (action.startTime * sampleRate * channels).toInt()
            mixPcm(mixBuffer, sfx, startSample, config.sfxVolume)
        }
        progressCallback(0.9f)
    }

    private fun mixPcm(mixBuffer: IntArray, pcm: ShortArray, startSample: Int, volume: Float) {
        if (volume <= 0f) return
        for (i in pcm.indices) {
            val targetIndex = startSample + i
            if (targetIndex < 0 || targetIndex >= mixBuffer.size) continue
            mixBuffer[targetIndex] += (pcm[i] * volume).toInt()
        }
    }

    private fun mixLoopPcm(mixBuffer: IntArray, pcm: ShortArray, startSample: Int, volume: Float) {
        if (pcm.isEmpty() || volume <= 0f) return
        for (targetIndex in mixBuffer.indices) {
            val sourceIndex = (targetIndex - startSample).mod(pcm.size)
            mixBuffer[targetIndex] += (pcm[sourceIndex] * volume).toInt()
        }
    }

    private fun generateBeepPcm(
        durationSeconds: Float,
        frequency: Double,
        sampleRate: Int,
        channels: Int
    ): ShortArray {
        val numSamples = (durationSeconds * sampleRate).toInt()
        return ShortArray(numSamples * channels) { i ->
            val sampleIndex = i / channels
            val t = sampleIndex / sampleRate.toDouble()
            val value = sin(2 * Math.PI * frequency * t) * 32767 * 0.3
            value.toInt().toShort()
        }
    }

    companion object {
        const val SAMPLE_RATE = 44100
        const val CHANNELS = 2
    }
}
