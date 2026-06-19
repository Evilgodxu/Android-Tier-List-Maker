package com.tdds.jh.model.tierlist.video.frame

import com.tdds.jh.model.tierlist.video.NameDisplayMode
import com.tdds.jh.model.tierlist.video.VideoGenerationConfig
import com.tdds.jh.model.tierlist.video.timeline.Timeline
import com.tdds.jh.model.tierlist.video.timeline.TimelineAction

/**
 * 根据时间线计算每张图片在某一时刻的显示状态
 */
object FrameStateComputer {

    fun computeFrameStates(
        timeline: Timeline,
        currentTime: Float,
        config: VideoGenerationConfig
    ): Map<String, FrameState> {
        val states = mutableMapOf<String, FrameState>()
        timeline.actions.forEach { action ->
            if (action.startTime > currentTime) return@forEach
            val id = action.tierImageId
            val state = states[id] ?: FrameState()
            states[id] = when (action) {
                is TimelineAction.Place -> state.copy(placed = true)
                is TimelineAction.Name -> {
                    val visibleChars = when (config.nameDisplayMode) {
                        NameDisplayMode.ONCE -> action.name.length
                        NameDisplayMode.PER_CHAR -> {
                            if (action.duration <= 0f) {
                                action.name.length
                            } else {
                                val progress = ((currentTime - action.startTime) / action.duration)
                                    .coerceIn(0f, 1f)
                                (action.name.length * progress).toInt()
                                    .coerceIn(0, action.name.length)
                            }
                        }
                    }
                    state.copy(nameVisibleChars = visibleChars)
                }
                is TimelineAction.Badge -> {
                    state.copy(visibleBadgeSlots = state.visibleBadgeSlots + action.slotIndex)
                }
            }
        }
        return states
    }
}
