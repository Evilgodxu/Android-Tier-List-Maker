package com.tdds.jh.model.tierlist.video.frame

import com.tdds.jh.model.tierlist.video.NameDisplayMode
import com.tdds.jh.model.tierlist.video.VideoGenerationConfig
import com.tdds.jh.model.tierlist.video.timeline.Timeline
import com.tdds.jh.model.tierlist.video.timeline.TimelineAction

/**
 * 帧序列生成器
 *
 * 根据 Timeline 生成关键帧时间点，仅在动作状态发生变化时产生新帧，
 * 静态停留期可由外部复用上一关键帧位图。
 */
class FrameSequenceGenerator {

    /**
     * 生成关键帧列表
     */
    fun generateKeyFrames(
        timeline: Timeline,
        config: VideoGenerationConfig
    ): List<VideoFrame> {
        val times = sortedSetOf<Float>()
        times.add(0f)
        if (timeline.totalDuration > 0f) {
            times.add(timeline.totalDuration)
        }

        timeline.actions.forEach { action ->
            times.add(action.startTime)
            times.add(action.endTime)

            if (action is TimelineAction.Name && config.nameDisplayMode == NameDisplayMode.PER_CHAR) {
                for (i in 1 until action.name.length) {
                    times.add(action.startTime + i * config.nameCharInterval)
                }
            }
        }

        return times.map { time ->
            VideoFrame(
                timeSeconds = time,
                stateSnapshot = FrameStateComputer.computeFrameStates(timeline, time, config)
            )
        }
    }
}
