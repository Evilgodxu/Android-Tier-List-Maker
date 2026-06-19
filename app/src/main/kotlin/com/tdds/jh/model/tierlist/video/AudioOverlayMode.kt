package com.tdds.jh.model.tierlist.video

import androidx.annotation.StringRes
import com.tdds.jh.R

/**
 * 解说音频叠加方式
 */
enum class AudioOverlayMode(@StringRes val labelRes: Int) {
    OVERLAY(R.string.audio_overlay_overlay),
    KEEP_SFX(R.string.audio_overlay_keep_sfx),
    REPLACE(R.string.audio_overlay_replace)
}
