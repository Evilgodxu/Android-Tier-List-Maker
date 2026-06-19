package com.tdds.jh.model.tierlist.video

import androidx.annotation.StringRes
import com.tdds.jh.R

/**
 * 解说音频播放时机
 */
enum class NarrationOrder(@StringRes val labelRes: Int) {
    BEFORE_CONTENT(R.string.narration_before_content),
    AFTER_CONTENT(R.string.narration_after_content)
}
