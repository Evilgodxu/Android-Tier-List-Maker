package com.tdds.jh.data.tierlist.video

import android.content.Context
import android.net.Uri
import java.io.InputStream
import kotlin.math.min

/**
 * WAV 文件解码器
 *
 * 直接解析 WAV 文件头并读取 PCM 数据，不依赖 MediaExtractor/MediaCodec，
 * 用于兼容原生 MediaExtractor 无法读取 WAV 的设备。
 */
object WavDecoder {

    fun decodeToPcm(
        context: Context,
        uri: Uri,
        targetSampleRate: Int,
        targetChannels: Int,
        maxDurationSeconds: Float? = null
    ): ShortArray? {
        val inputStream = try {
            when (uri.scheme) {
                "file" -> java.io.File(uri.path!!).inputStream()
                else -> context.contentResolver.openInputStream(uri)
            }
        } catch (_: Exception) {
            return null
        } ?: return null

        return inputStream.use { stream ->
            decodeStream(stream, targetSampleRate, targetChannels, maxDurationSeconds)
        }
    }

    private fun decodeStream(
        stream: InputStream,
        targetSampleRate: Int,
        targetChannels: Int,
        maxDurationSeconds: Float?
    ): ShortArray? {
        val reader = LittleEndianReader(stream)

        // RIFF header
        if (!reader.readFourCc().contentEquals(riffBytes)) return null
        reader.readInt() // chunk size
        if (!reader.readFourCc().contentEquals(waveBytes)) return null

        var audioFormat = 0
        var sourceChannels = 0
        var sourceSampleRate = 0
        var bitsPerSample = 0
        var dataSize = 0
        var dataOffset = 0
        var foundData = false

        while (true) {
            val chunkId = reader.readFourCc()
            if (chunkId.size < 4) break
            val chunkSize = reader.readInt()
            if (chunkSize < 0) break

            when {
                chunkId.contentEquals(fmtBytes) -> {
                    if (chunkSize < 16) return null
                    audioFormat = reader.readShort()
                    sourceChannels = reader.readShort()
                    sourceSampleRate = reader.readInt()
                    reader.readInt() // byte rate
                    reader.readShort() // block align
                    bitsPerSample = reader.readShort()
                    if (chunkSize > 16) reader.skip(chunkSize - 16)
                }
                chunkId.contentEquals(dataBytes) -> {
                    dataSize = chunkSize
                    dataOffset = reader.bytesRead
                    foundData = true
                    break
                }
                else -> reader.skip(chunkSize)
            }
        }

        if (!foundData || dataSize <= 0) return null
        if (sourceChannels != 1 && sourceChannels != 2) return null
        if (bitsPerSample != 8 && bitsPerSample != 16 && bitsPerSample != 24 && bitsPerSample != 32) return null

        val bytesPerSample = bitsPerSample / 8
        val sourceSampleCount = dataSize / bytesPerSample / sourceChannels

        val maxSourceSamples = maxDurationSeconds?.let { seconds ->
            (seconds * sourceSampleRate * sourceChannels).toInt()
        } ?: Int.MAX_VALUE

        val samplesToRead = min(sourceSampleCount, maxSourceSamples / sourceChannels)
        if (samplesToRead <= 0) return ShortArray(0)

        // Read raw PCM into ShortArray with source format
        val rawPcm = ShortArray(samplesToRead * sourceChannels)
        for (i in rawPcm.indices) {
            rawPcm[i] = readSample(reader, audioFormat, bitsPerSample)
        }

        // Resample and mix channels
        return AudioDecoderHelper.resampleAndMixChannels(
            rawPcm,
            sourceSampleRate,
            sourceChannels,
            targetSampleRate,
            targetChannels
        )
    }

    private fun readSample(reader: LittleEndianReader, audioFormat: Int, bitsPerSample: Int): Short {
        return when (audioFormat) {
            1 -> readPcmSample(reader, bitsPerSample)
            3 -> readFloatSample(reader, bitsPerSample)
            else -> 0
        }.toShort()
    }

    private fun readPcmSample(reader: LittleEndianReader, bitsPerSample: Int): Int {
        return when (bitsPerSample) {
            8 -> reader.readByte() - 128
            16 -> reader.readShort()
            24 -> reader.readInt24()
            32 -> reader.readInt()
            else -> 0
        }
    }

    private fun readFloatSample(reader: LittleEndianReader, bitsPerSample: Int): Int {
        val floatValue: Double = when (bitsPerSample) {
            32 -> java.lang.Float.intBitsToFloat(reader.readInt()).toDouble()
            64 -> java.lang.Double.longBitsToDouble(reader.readLong())
            else -> 0.0
        }
        return (floatValue * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
    }

    private val riffBytes = byteArrayOf('R'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), 'F'.code.toByte())
    private val waveBytes = byteArrayOf('W'.code.toByte(), 'A'.code.toByte(), 'V'.code.toByte(), 'E'.code.toByte())
    private val fmtBytes = byteArrayOf('f'.code.toByte(), 'm'.code.toByte(), 't'.code.toByte(), ' '.code.toByte())
    private val dataBytes = byteArrayOf('d'.code.toByte(), 'a'.code.toByte(), 't'.code.toByte(), 'a'.code.toByte())

    private class LittleEndianReader(private val stream: InputStream) {
        var bytesRead = 0
            private set

        fun readFourCc(): ByteArray {
            val bytes = ByteArray(4)
            val read = stream.read(bytes)
            bytesRead += read.coerceAtLeast(0)
            return bytes
        }

        fun readByte(): Int {
            val value = stream.read()
            bytesRead += 1
            return value
        }

        fun readShort(): Int {
            val lo = readByte()
            val hi = readByte()
            return ((hi shl 8) or lo)
        }

        fun readInt24(): Int {
            val b0 = readByte()
            val b1 = readByte()
            val b2 = readByte()
            var value = (b2 shl 16) or (b1 shl 8) or b0
            if (value and 0x800000 != 0) value = value or -0x1000000
            return value
        }

        fun readInt(): Int {
            val b0 = readByte()
            val b1 = readByte()
            val b2 = readByte()
            val b3 = readByte()
            return (b3 shl 24) or (b2 shl 16) or (b1 shl 8) or b0
        }

        fun readLong(): Long {
            var value = 0L
            repeat(8) { i ->
                value = value or ((readByte().toLong() and 0xFF) shl (i * 8))
            }
            return value
        }

        fun skip(count: Int) {
            var remaining = count
            while (remaining > 0) {
                val skipped = stream.skip(remaining.toLong())
                if (skipped <= 0) break
                bytesRead += skipped.toInt()
                remaining -= skipped.toInt()
            }
        }
    }
}

/**
 * 音频解码辅助函数，供 WavDecoder 使用
 */
internal object AudioDecoderHelper {
    fun resampleAndMixChannels(
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
                ((left + right) / 2)
            }

            if (targetChannels == 1) {
                output[i] = sample.toShort()
            } else {
                output[i * 2] = sample.toShort()
                output[i * 2 + 1] = sample.toShort()
            }
        }
        return output
    }
}
