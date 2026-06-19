package com.tdds.jh.data.tierlist.video

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri

/**
 * 音频时长查询器
 */
class AudioDurationProvider(private val context: Context) {

    fun getDurationSeconds(uri: Uri): Float {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val durationMs = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: 0L
            durationMs / 1000f
        } catch (_: Exception) {
            0f
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }
}
