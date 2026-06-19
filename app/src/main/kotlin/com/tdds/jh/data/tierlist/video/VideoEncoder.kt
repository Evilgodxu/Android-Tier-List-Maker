package com.tdds.jh.data.tierlist.video

import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import com.tdds.jh.model.tierlist.video.frame.VideoFrame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

/**
 * 视频编码器
 *
 * 将关键帧序列与 AAC 音频轨道封装为 MP4（H.264 + AAC）。
 */
class VideoEncoder(
    private val width: Int,
    private val height: Int,
    private val frameRate: Int = 30,
    private val bitRate: Int = 8_000_000,
    private val iFrameInterval: Int = 1
) {

    private val bufferInfo = MediaCodec.BufferInfo()

    /**
     * 编码视频
     *
     * @param frames 关键帧列表（已按时间排序）
     * @param frameProvider 根据关键帧渲染 Bitmap
     * @param audioFile 可选的 AAC 音频文件
     * @param outputFile 输出 MP4 文件
     * @param progressCallback 进度回调 0~1
     * @param onCancel 取消检查
     */
    suspend fun encode(
        frames: List<VideoFrame>,
        frameProvider: suspend (VideoFrame) -> Bitmap,
        audioFile: File?,
        outputFile: File,
        progressCallback: (Float) -> Unit = {},
        onCancel: () -> Boolean = { false }
    ): Boolean = withContext(Dispatchers.IO) {
        if (frames.isEmpty() || !isActive) {
            progressCallback(1f)
            return@withContext false
        }

        val totalDuration = frames.last().timeSeconds
        val totalFrames = (totalDuration * frameRate).toInt() + 1

        val codec = try {
            MediaCodec.createEncoderByType("video/avc")
        } catch (_: Exception) {
            progressCallback(1f)
            return@withContext false
        }

        val colorFormat = selectColorFormat(codec)
        val isPlanar = colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar

        val format = MediaFormat.createVideoFormat("video/avc", width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInterval)
        }

        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()

        val muxer = try {
            MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        } catch (_: Exception) {
            codec.release()
            progressCallback(1f)
            return@withContext false
        }

        var muxerStarted = false
        var videoTrackIndex = -1
        var audioTrackIndex = -1
        var presentationTimeUs = 0L
        var outputDone = false
        var inputDone = false
        var frameIndex = 0

        var lastKeyFrameIndex = -1
        var lastBitmap: Bitmap? = null

        val audioExtractor = audioFile?.let { createAudioExtractor(it) }

        try {
            while (!outputDone) {
                if (onCancel()) {
                    return@withContext false
                }

                if (!inputDone) {
                    val inputBufferId = codec.dequeueInputBuffer(TIMEOUT_US)
                    if (inputBufferId >= 0) {
                        if (frameIndex < totalFrames) {
                            val currentTime = frameIndex / frameRate.toFloat()
                            val keyFrameIndex = findKeyFrameIndex(frames, currentTime)

                            val bitmap = if (keyFrameIndex == lastKeyFrameIndex) {
                                lastBitmap
                            } else {
                                lastBitmap?.recycle()
                                val rendered = frameProvider(frames[keyFrameIndex])
                                lastBitmap = rendered
                                rendered
                            }
                            lastKeyFrameIndex = keyFrameIndex

                            val inputBuffer = codec.getInputBuffer(inputBufferId) ?: continue
                            inputBuffer.clear()

                            if (bitmap != null) {
                                val scaledBitmap = if (bitmap.width != width || bitmap.height != height) {
                                    Bitmap.createScaledBitmap(bitmap, width, height, true)
                                } else {
                                    bitmap
                                }
                                val yuv = if (isPlanar) {
                                    BitmapToYuvConverter.argbToYuv420p(scaledBitmap)
                                } else {
                                    BitmapToYuvConverter.argbToNv12(scaledBitmap)
                                }
                                if (scaledBitmap != bitmap) scaledBitmap.recycle()
                                inputBuffer.put(yuv)

                                presentationTimeUs = frameIndex * 1_000_000L / frameRate
                                codec.queueInputBuffer(
                                    inputBufferId,
                                    0,
                                    yuv.size,
                                    presentationTimeUs,
                                    0
                                )
                            } else {
                                codec.queueInputBuffer(
                                    inputBufferId,
                                    0,
                                    0,
                                    presentationTimeUs,
                                    0
                                )
                            }
                            frameIndex++
                        } else {
                            codec.queueInputBuffer(
                                inputBufferId,
                                0,
                                0,
                                0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            inputDone = true
                        }
                    }
                }

                val outputBufferId = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                when {
                    outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        if (!muxerStarted) {
                            videoTrackIndex = muxer.addTrack(codec.outputFormat)
                            audioTrackIndex = audioExtractor?.let { muxer.addTrack(it.format) } ?: -1
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
                            muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                        }
                        codec.releaseOutputBuffer(outputBufferId, false)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            outputDone = true
                        }
                    }
                }

                if (muxerStarted && audioExtractor != null) {
                    writeAudioSamples(muxer, audioExtractor, audioTrackIndex)
                }

                progressCallback(0.5f + 0.5f * frameIndex / totalFrames.coerceAtLeast(1))
            }

            if (muxerStarted && audioExtractor != null) {
                drainAudio(muxer, audioExtractor, audioTrackIndex)
            }
        } catch (_: Exception) {
            return@withContext false
        } finally {
            if (muxerStarted) {
                muxer.stop()
            }
            muxer.release()
            codec.stop()
            codec.release()
            audioExtractor?.release()
            lastBitmap?.recycle()
        }

        progressCallback(1f)
        return@withContext true
    }

    private fun selectColorFormat(codec: MediaCodec): Int {
        val capabilities = try {
            codec.codecInfo.getCapabilitiesForType("video/avc")
        } catch (_: Exception) {
            return MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
        }
        val preferred = listOf(
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar
        )
        for (format in preferred) {
            if (format in capabilities.colorFormats) return format
        }
        return capabilities.colorFormats.firstOrNull()
            ?: MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
    }

    private fun findKeyFrameIndex(frames: List<VideoFrame>, currentTime: Float): Int {
        var index = frames.binarySearch { it.timeSeconds.compareTo(currentTime) }
        if (index < 0) {
            index = (-index - 2).coerceAtLeast(0)
        }
        return index
    }

    private fun createAudioExtractor(audioFile: File): AudioExtractorWrapper? {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(audioFile.absolutePath)
            val trackIndex = (0 until extractor.trackCount).firstOrNull { i ->
                extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            }
            if (trackIndex == null) {
                extractor.release()
                return null
            }
            extractor.selectTrack(trackIndex)
            AudioExtractorWrapper(extractor, extractor.getTrackFormat(trackIndex))
        } catch (_: Exception) {
            extractor.release()
            null
        }
    }

    private fun writeAudioSamples(
        muxer: MediaMuxer,
        extractorWrapper: AudioExtractorWrapper,
        audioTrackIndex: Int
    ) {
        val extractor = extractorWrapper.extractor
        val buffer = ByteBuffer.allocate(64 * 1024)
        val info = MediaCodec.BufferInfo()
        val sampleSize = extractor.readSampleData(buffer, 0)
        if (sampleSize >= 0) {
            info.offset = 0
            info.size = sampleSize
            info.presentationTimeUs = extractor.sampleTime
            info.flags = mapExtractorFlagsToBufferFlags(extractor.sampleFlags)
            muxer.writeSampleData(audioTrackIndex, buffer, info)
            extractor.advance()
        }
    }

    private fun drainAudio(
        muxer: MediaMuxer,
        extractorWrapper: AudioExtractorWrapper,
        audioTrackIndex: Int
    ) {
        val extractor = extractorWrapper.extractor
        val buffer = ByteBuffer.allocate(64 * 1024)
        val info = MediaCodec.BufferInfo()
        while (true) {
            val sampleSize = extractor.readSampleData(buffer, 0)
            if (sampleSize < 0) break
            info.offset = 0
            info.size = sampleSize
            info.presentationTimeUs = extractor.sampleTime
            info.flags = mapExtractorFlagsToBufferFlags(extractor.sampleFlags)
            muxer.writeSampleData(audioTrackIndex, buffer, info)
            extractor.advance()
        }
    }

    private fun mapExtractorFlagsToBufferFlags(extractorFlags: Int): Int {
        var flags = 0
        if (extractorFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) {
            flags = flags or MediaCodec.BUFFER_FLAG_KEY_FRAME
        }
        if (extractorFlags and MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME != 0) {
            flags = flags or MediaCodec.BUFFER_FLAG_PARTIAL_FRAME
        }
        return flags
    }

    private data class AudioExtractorWrapper(
        val extractor: MediaExtractor,
        val format: MediaFormat
    ) {
        fun release() {
            extractor.release()
        }
    }

    companion object {
        private const val TIMEOUT_US = 10_000L
    }
}
