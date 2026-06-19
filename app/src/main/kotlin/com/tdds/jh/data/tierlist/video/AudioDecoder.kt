package com.tdds.jh.data.tierlist.video

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.nio.ByteBuffer

/**
 * 音频解码器
 *
 * 将音频文件解码为 PCM 16bit 单声道/指定采样率的 ShortArray。
 */
object AudioDecoder {

    private const val TIMEOUT_US = 10_000L

    /**
     * 解码音频为 PCM
     *
     * @param targetSampleRate 目标采样率，如 44100
     * @param targetChannels 目标声道数，当前仅支持 1 或 2
     * @return 16bit PCM 样本数组，失败返回 null
     */
    fun decodeToPcm(
        context: Context,
        uri: Uri,
        targetSampleRate: Int = 44100,
        targetChannels: Int = 2,
        maxDurationSeconds: Float? = null
    ): ShortArray? {
        val extractor = MediaExtractor()
        try {
            when (uri.scheme) {
                "file" -> extractor.setDataSource(uri.path!!)
                else -> extractor.setDataSource(context, uri, null)
            }
        } catch (_: Exception) {
            extractor.release()
            return null
        }

        val trackIndex = (0 until extractor.trackCount).firstOrNull { i ->
            extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        }
        if (trackIndex == null) {
            extractor.release()
            return null
        }

        extractor.selectTrack(trackIndex)
        val format = extractor.getTrackFormat(trackIndex)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: run {
            extractor.release()
            return null
        }

        val codec = try {
            MediaCodec.createDecoderByType(mime)
        } catch (_: Exception) {
            extractor.release()
            return null
        }

        codec.configure(format, null, null, 0)
        codec.start()

        val samples = mutableListOf<Short>()
        val bufferInfo = MediaCodec.BufferInfo()
        var sawInputEOS = false
        var sawOutputEOS = false

        val sourceSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val sourceChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

        try {
            while (!sawOutputEOS) {
                if (!sawInputEOS) {
                    val inputBufferIndex = codec.dequeueInputBuffer(TIMEOUT_US)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputBufferIndex) ?: continue
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            sawInputEOS = true
                        } else {
                            codec.queueInputBuffer(inputBufferIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                when {
                    outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {}
                    outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {}
                    outputBufferIndex >= 0 -> {
                        val outputBuffer = codec.getOutputBuffer(outputBufferIndex) ?: continue
                        val chunk = ByteArray(bufferInfo.size)
                        outputBuffer.get(chunk)
                        outputBuffer.clear()

                        val decodedShorts = bytesToShorts(chunk)
                        val resampled = resampleAndMixChannels(
                            decodedShorts,
                            sourceSampleRate,
                            sourceChannels,
                            targetSampleRate,
                            targetChannels
                        )
                        samples.addAll(resampled.toList())

                        val maxSamples = maxDurationSeconds?.let { seconds ->
                            (seconds * targetSampleRate * targetChannels).toInt()
                        }
                        if (maxSamples != null && samples.size >= maxSamples) {
                            sawOutputEOS = true
                        }

                        codec.releaseOutputBuffer(outputBufferIndex, false)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            sawOutputEOS = true
                        }
                    }
                }
            }
        } catch (_: Exception) {
            return null
        } finally {
            codec.stop()
            codec.release()
            extractor.release()
        }

        return samples.toShortArray()
    }

    private fun bytesToShorts(bytes: ByteArray): ShortArray {
        val shorts = ShortArray(bytes.size / 2)
        for (i in shorts.indices) {
            val lo = bytes[i * 2].toInt() and 0xFF
            val hi = bytes[i * 2 + 1].toInt()
            shorts[i] = ((hi shl 8) or lo).toShort()
        }
        return shorts
    }

    private fun resampleAndMixChannels(
        input: ShortArray,
        sourceSampleRate: Int,
        sourceChannels: Int,
        targetSampleRate: Int,
        targetChannels: Int
    ): ShortArray {
        if (sourceSampleRate == targetSampleRate && sourceChannels == targetChannels) {
            return input
        }

        val sampleCount = input.size / sourceChannels
        val outputSampleCount = (sampleCount * targetSampleRate.toDouble() / sourceSampleRate).toInt()
        val output = ShortArray(outputSampleCount * targetChannels)

        for (i in 0 until outputSampleCount) {
            val srcIndex = (i * sourceSampleRate.toDouble() / targetSampleRate).toInt()
                .coerceIn(0, sampleCount - 1)
            val sample = if (sourceChannels == 1) {
                input[srcIndex]
            } else {
                val left = input[srcIndex * 2].toInt()
                val right = input[srcIndex * 2 + 1].toInt()
                ((left + right) / 2).toShort()
            }

            if (targetChannels == 1) {
                output[i] = sample
            } else {
                output[i * 2] = sample
                output[i * 2 + 1] = sample
            }
        }
        return output
    }
}
