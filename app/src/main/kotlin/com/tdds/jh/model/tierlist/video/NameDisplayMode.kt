package com.tdds.jh.model.tierlist.video

import androidx.annotation.StringRes
import com.tdds.jh.R

/**
 * 命名显示模式
 */
enum class NameDisplayMode(@StringRes val labelRes: Int) {
    ONCE(R.string.name_display_once),
    PER_CHAR(R.string.name_display_per_char)
}
