package com.tdds.jh.model.tierlist.video

/**
 * 录制下来的编辑动作
 * 不保留时间戳，仅保留动作类型与关联数据
 */
sealed class RecordedAction(
    open val tierImageId: String
) {
    /**
     * 图片被放置到某个层级
     */
    data class ImagePlaced(
        override val tierImageId: String,
        val tierLabel: String
    ) : RecordedAction(tierImageId)

    /**
     * 图片被命名
     */
    data class ImageNamed(
        override val tierImageId: String,
        val name: String
    ) : RecordedAction(tierImageId)

    /**
     * 图片被添加了第 slotIndex 个小图标（0-based）
     */
    data class BadgeAdded(
        override val tierImageId: String,
        val slotIndex: Int
    ) : RecordedAction(tierImageId)
}
