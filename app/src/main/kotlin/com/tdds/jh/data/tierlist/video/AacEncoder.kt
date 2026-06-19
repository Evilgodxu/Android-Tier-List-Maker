package com.tdds.jh.data.tierlist.video

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer

/**
 * AAC 编码器
 *
 * 将 16bit PCM 编码为 AAC 音频轨道并写入 MP4 文件。
 */
class AacEncoder(
    private val sampleRate: Int = 44100,
    private val channels: Int = 2,
    private val bitRate: Int = 128_000
) {

    private val bufferInfo = MediaCodec.BufferInfo()

    /**
     * 编码 PCM 数据到指定 MP4 文件
     *
     * @param pcmData 16bit PCM 样本，声道交错排列（L,R,L,R...）
     * @param outputFile 输出 MP4 文件
     */
    fun encode(pcmData: ShortArray, outputFile: File): Boolean {
        val codec = try {
            MediaCodec.createEncoderByType("audio/mp4a-latm")
        } catch (_: Exception) {
            return false
        }

        val format = MediaFormat.createAudioFormat("audio/mp4a-latm", sampleRate, channels).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
        }

        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()

        val muxer = try {
            MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        } catch (_: Exception) {
            codec.release()
            return false
        }

        var muxerStarted = false
        var audioTrackIndex = -1
        var presentationTimeUs = 0L
        val bytesPerSample = 2
        val frameBytes = 1024 * channels * bytesPerSample
        val inputBufferSize = frameBytes

        var inputDone = false
        var outputDone = false
        var inputIndex = 0

        try {
            while (!outputDone) {
                if (!inputDone) {
                    val inputBufferId = codec.dequeueInputBuffer(TIMEOUT_US)
                    if (inputBufferId >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputBufferId) ?: continue
                        inputBuffer.clear()

                        val samplesToRead = minOf(inputBufferSize / bytesPerSample, pcmData.size - inputIndex)
                        if (samplesToRead > 0) {
                            for (i in 0 until samplesToRead) {
                                inputBuffer.putShort(pcmData[inputIndex + i])
                            }
                            val sampleTimeUs = (inputIndex.toDouble() / channels / sampleRate * 1_000_000).toLong()
                            codec.queueInputBuffer(inputBufferId, 0, samplesToRead * bytesPerSample, sampleTimeUs, 0)
                            inputIndex += samplesToRead
                        } else {
                            codec.queueInputBuffer(inputBufferId, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        }
                    }
                }

                val outputBufferId = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                when {
                    outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        if (!muxerStarted) {
                            audioTrackIndex = muxer.addTrack(codec.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                    }

                    outputBufferId == MediaCodec.INFO_TRY_AGAIN_LATER -> {}
                    outputBufferId >= 0 -> {
                        val encodedData = codec.getOutputBuffer(outputBufferId) ?: continue
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                            bufferInfo.size = 0
                        }
                        if (bufferInfo.size > 0 && muxerStarted) {
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            bufferInfo.presentationTimeUs = presentationTimeUs
                            muxer.writeSampleData(audioTrackIndex, encodedData, bufferInfo)
                            presentationTimeUs += (1024L * 1_000_000L / sampleRate)
                        }
                        codec.releaseOutputBuffer(outputBufferId, false)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            outputDone = true
                        }
                    }
                }
            }
        } catch (_: Exception) {
            return false
        } finally {
            if (muxerStarted) {
                muxer.stop()
            }
            muxer.release()
            codec.stop()
            codec.release()
        }

        return true
    }

    companion object {
        private const val TIMEOUT_US = 10_000L
    }
}
