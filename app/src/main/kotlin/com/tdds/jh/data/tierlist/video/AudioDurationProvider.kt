package com.tdds.jh.data.tierlist.video

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.io.File
import java.io.InputStream

/**
 * 音频时长查询器
 *
 * 使用 MediaExtractor 直接从文件格式读取时长，比 MediaMetadataRetriever 更稳定，
 * 尤其对应用私有目录中的 file:// URI 更可靠。
 * WAV 文件使用独立解析，绕过原生 MediaExtractor 对 WAV 支持不一致的问题。
 */
class AudioDurationProvider(private val context: Context) {

    fun getDurationSeconds(uri: Uri): Float {
        if (isWavUri(uri)) {
            return getWavDurationSeconds(uri)
        }

        val extractor = MediaExtractor()
        return try {
            when (uri.scheme) {
                "file" -> extractor.setDataSource(File(uri.path!!).absolutePath)
                else -> extractor.setDataSource(context, uri, null)
            }

            val trackIndex = (0 until extractor.trackCount).firstOrNull { i ->
                extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return 0f

            val format = extractor.getTrackFormat(trackIndex)
            val durationUs = format.getLong(MediaFormat.KEY_DURATION)
            durationUs / 1_000_000f
        } catch (_: Exception) {
            0f
        } finally {
            extractor.release()
        }
    }

    private fun isWavUri(uri: Uri): Boolean {
        val path = uri.path ?: uri.toString()
        return path.lowercase().endsWith(".wav")
    }

    private fun getWavDurationSeconds(uri: Uri): Float {
        val inputStream = try {
            when (uri.scheme) {
                "file" -> File(uri.path!!).inputStream()
                else -> context.contentResolver.openInputStream(uri)
            }
        } catch (_: Exception) {
            return 0f
        } ?: return 0f

        return inputStream.use { stream ->
            parseWavDuration(stream)
        }
    }

    private fun parseWavDuration(stream: InputStream): Float {
        val reader = LittleEndianReader(stream)

        if (!reader.readFourCc().contentEquals(riffBytes)) return 0f
        reader.readInt()
        if (!reader.readFourCc().contentEquals(waveBytes)) return 0f

        var sampleRate = 0
        var channels = 0
        var bitsPerSample = 0
        var dataSize = 0

        while (true) {
            val chunkId = reader.readFourCc()
            if (chunkId.size < 4) break
            val chunkSize = reader.readInt()
            if (chunkSize < 0) break

            when {
                chunkId.contentEquals(fmtBytes) -> {
                    if (chunkSize < 16) return 0f
                    reader.readShort() // audio format
                    channels = reader.readShort()
                    sampleRate = reader.readInt()
                    reader.readInt()
                    reader.readShort()
                    bitsPerSample = reader.readShort()
                    if (chunkSize > 16) reader.skip(chunkSize - 16)
                }
                chunkId.contentEquals(dataBytes) -> {
                    dataSize = chunkSize
                    break
                }
                else -> reader.skip(chunkSize)
            }
        }

        if (sampleRate <= 0 || channels <= 0 || bitsPerSample <= 0 || dataSize <= 0) return 0f
        val bytesPerSecond = sampleRate * channels * bitsPerSample / 8
        return dataSize.toFloat() / bytesPerSecond
    }

    private class LittleEndianReader(private val stream: InputStream) {
        fun readFourCc(): ByteArray {
            val bytes = ByteArray(4)
            stream.read(bytes)
            return bytes
        }

        fun readShort(): Int {
            val lo = stream.read()
            val hi = stream.read()
            return ((hi shl 8) or lo)
        }

        fun readInt(): Int {
            val b0 = stream.read()
            val b1 = stream.read()
            val b2 = stream.read()
            val b3 = stream.read()
            return (b3 shl 24) or (b2 shl 16) or (b1 shl 8) or b0
        }

        fun skip(count: Int) {
            var remaining = count
            while (remaining > 0) {
                val skipped = stream.skip(remaining.toLong())
                if (skipped <= 0) break
                remaining -= skipped.toInt()
            }
        }
    }

    companion object {
        private val riffBytes = byteArrayOf('R'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), 'F'.code.toByte())
        private val waveBytes = byteArrayOf('W'.code.toByte(), 'A'.code.toByte(), 'V'.code.toByte(), 'E'.code.toByte())
        private val fmtBytes = byteArrayOf('f'.code.toByte(), 'm'.code.toByte(), 't'.code.toByte(), ' '.code.toByte())
        private val dataBytes = byteArrayOf('d'.code.toByte(), 'a'.code.toByte(), 't'.code.toByte(), 'a'.code.toByte())
    }
}
