package com.tdds.jh.model.tierlist.video.frame

/**
 * 单张图片在某一帧的显示状态
 *
 * @param placed 是否已放置显示
 * @param nameVisibleChars 名字可见字符数（0 表示不显示）
 * @param visibleBadgeSlots 已显示的小图标槽位索引集合
 */
data class FrameState(
    val placed: Boolean = false,
    val nameVisibleChars: Int = 0,
    val visibleBadgeSlots: Set<Int> = emptySet()
)
