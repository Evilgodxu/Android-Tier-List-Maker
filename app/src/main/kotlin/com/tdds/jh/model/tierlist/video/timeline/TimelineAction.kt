package com.tdds.jh.model.tierlist.video.timeline

/**
 * 时间线上的动作，包含开始时间与持续时长
 */
sealed class TimelineAction(
    open val tierImageId: String,
    open val startTime: Float,
    open val duration: Float
) {
    val endTime: Float get() = startTime + duration

    /**
     * 图片放置动作
     */
    data class Place(
        override val tierImageId: String,
        override val startTime: Float,
        override val duration: Float = 0.3f,
        val tierLabel: String
    ) : TimelineAction(tierImageId, startTime, duration)

    /**
     * 图片命名动作
     */
    data class Name(
        override val tierImageId: String,
        override val startTime: Float,
        override val duration: Float,
        val name: String
    ) : TimelineAction(tierImageId, startTime, duration)

    /**
     * 小图标添加动作
     */
    data class Badge(
        override val tierImageId: String,
        override val startTime: Float,
        override val duration: Float = 0.3f,
        val slotIndex: Int
    ) : TimelineAction(tierImageId, startTime, duration)
}
