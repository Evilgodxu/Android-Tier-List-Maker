package com.tdds.jh.model.tierlist.video

import androidx.annotation.StringRes
import com.tdds.jh.R

/**
 * 视频动作类型
 */
enum class VideoActionType(@StringRes val labelRes: Int) {
    PLACE(R.string.action_place),
    NAME(R.string.action_name),
    BADGE(R.string.action_badge)
}
