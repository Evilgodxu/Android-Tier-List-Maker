package com.tdds.jh.data.tierlist.video

import android.content.Context
import android.util.Log
import com.tdds.jh.model.tierlist.video.VideoGenerationConfig
import com.tdds.jh.model.tierlist.video.timeline.Timeline
import com.tdds.jh.model.tierlist.video.timeline.TimelineAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
/**
 * 音频合成引擎
 *
 * 将解说音频按时间线混音为单条 AAC 音轨。
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
            Log.w(TAG, "混音失败: 总时长 <= 0")
            progressCallback(1f)
            return@withContext false
        }

        val totalSamples = (totalDuration * sampleRate * channels).toLong()
        if (totalSamples > Int.MAX_VALUE) {
            Log.w(TAG, "混音失败: 总样本数超过 Int.MAX_VALUE")
            progressCallback(1f)
            return@withContext false
        }

        val mixBuffer = IntArray(totalSamples.toInt())

        mixNarration(timeline, config, mixBuffer, sampleRate, channels, progressCallback)
        progressCallback(0.6f)
        progressCallback(0.9f)

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

    private fun mixPcm(mixBuffer: IntArray, pcm: ShortArray, startSample: Int, volume: Float) {
        if (volume <= 0f) return
        for (i in pcm.indices) {
            val targetIndex = startSample + i
            if (targetIndex < 0 || targetIndex >= mixBuffer.size) continue
            mixBuffer[targetIndex] += (pcm[i] * volume).toInt()
        }
    }

    companion object {
        const val SAMPLE_RATE = 44100
        const val CHANNELS = 2
        private const val TAG = "AudioMixer"
    }
}
