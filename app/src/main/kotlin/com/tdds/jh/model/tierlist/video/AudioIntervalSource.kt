package com.tdds.jh.model.tierlist.video

import androidx.annotation.StringRes
import com.tdds.jh.R

/**
 * 图片间隔来源
 */
enum class AudioIntervalSource(@StringRes val labelRes: Int) {
    FIXED(R.string.interval_source_fixed),
    AUDIO_DURATION(R.string.interval_source_audio_duration)
}
