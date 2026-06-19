package com.tdds.jh.data.tierlist.video

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri

/**
 * 音频时长查询器
 *
 * 使用 MediaExtractor 直接从文件格式读取时长，比 MediaMetadataRetriever 更稳定，
 * 尤其对应用私有目录中的 file:// URI 更可靠。
 */
class AudioDurationProvider(private val context: Context) {

    fun getDurationSeconds(uri: Uri): Float {
        val extractor = MediaExtractor()
        return try {
            when (uri.scheme) {
                "file" -> extractor.setDataSource(uri.path!!)
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
}
