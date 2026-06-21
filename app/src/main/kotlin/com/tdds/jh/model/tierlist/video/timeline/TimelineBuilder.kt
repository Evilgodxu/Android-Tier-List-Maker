package com.tdds.jh.model.tierlist.video.timeline

import com.tdds.jh.data.tierlist.video.AudioDurationProvider
import com.tdds.jh.model.tierlist.TierImage
import com.tdds.jh.model.tierlist.TierItem
import com.tdds.jh.model.tierlist.video.ArrangementGranularity
import com.tdds.jh.model.tierlist.video.AudioIntervalSource
import com.tdds.jh.model.tierlist.video.NameDisplayMode
import com.tdds.jh.model.tierlist.video.NarrationOrder
import com.tdds.jh.model.tierlist.video.VideoActionType
import com.tdds.jh.model.tierlist.video.VideoGenerationConfig
import kotlin.math.max

/**
 * 时间线构建引擎
 *
 * 根据当前 tierImages 顺序与视频配置，生成带时间戳的 Timeline。
 */
class TimelineBuilder(
    private val config: VideoGenerationConfig,
    private val audioDurationProvider: AudioDurationProvider
) {

    /**
     * 构建时间线
     */
    fun build(tiers: List<TierItem>, tierImages: List<TierImage>): Timeline {
        val actions = mutableListOf<TimelineAction>()
        val audioSegments = mutableListOf<AudioSegment>()
        var currentTime = config.initialPauseSeconds

        when (config.granularity) {
            ArrangementGranularity.PER_IMAGE -> buildByImageOrder(tierImages, actions, audioSegments, currentTime)
            ArrangementGranularity.PER_TYPE -> buildByTierOrder(tiers, tierImages, actions, audioSegments, currentTime)
        }

        val maxActionEnd = actions.maxOfOrNull { it.endTime } ?: 0f
        val maxAudioEnd = audioSegments.maxOfOrNull { it.startTime + it.duration } ?: 0f
        val totalDuration = maxOf(maxActionEnd, maxAudioEnd)

        return Timeline(actions, audioSegments, totalDuration)
    }

    /** 按图片顺序构建：直接按 tierImages 列表顺序逐张播放 */
    private fun buildByImageOrder(
        tierImages: List<TierImage>,
        actions: MutableList<TimelineAction>,
        audioSegments: MutableList<AudioSegment>,
        initialTime: Float
    ): Float {
        return buildSequentialImages(tierImages, actions, audioSegments, initialTime)
    }

    /** 按层级顺序构建：按 tier 分组，每层内图片按 tierImages 顺序播放 */
    private fun buildByTierOrder(
        tiers: List<TierItem>,
        tierImages: List<TierImage>,
        actions: MutableList<TimelineAction>,
        audioSegments: MutableList<AudioSegment>,
        initialTime: Float
    ): Float {
        var currentTime = initialTime
        tiers.forEach { tier ->
            val imagesInTier = tierImages.filter { it.tierLabel == tier.label }
            currentTime = buildSequentialImages(imagesInTier, actions, audioSegments, currentTime)
        }
        return currentTime
    }

    /**
     * 逐张播放图片：单张图片的 Place、内容动作与解说音频全部完成后，再切换到下一张。
     */
    private fun buildSequentialImages(
        images: List<TierImage>,
        actions: MutableList<TimelineAction>,
        audioSegments: MutableList<AudioSegment>,
        initialTime: Float
    ): Float {
        var currentTime = initialTime
        images.forEach { image ->
            val imageStartTime = currentTime

            // 图片先放置，保证后续音频/动作都有画面
            actions.add(TimelineAction.Place(image.id, imageStartTime, PLACE_DURATION, image.tierLabel))

            val audioDuration = image.audioUri?.let { audioDurationProvider.getDurationSeconds(it) } ?: 0f
            val contentTypes = config.actionOrder.filter { it != VideoActionType.PLACE }
            val contentDuration = contentTypes.totalContentDuration(image)

            val contentStart: Float
            val audioStart: Float

            if (audioDuration > 0f) {
                if (config.narrationOrder == NarrationOrder.BEFORE_CONTENT) {
                    // 先放解说，再放内容动作
                    audioStart = imageStartTime + PLACE_DURATION
                    contentStart = audioStart + audioDuration + config.extraAudioOffset
                } else {
                    // 先放内容动作，后放解说
                    contentStart = imageStartTime + PLACE_DURATION
                    audioStart = contentStart + contentDuration + config.extraAudioOffset
                }
                audioSegments.add(AudioSegment(image.id, image.audioUri!!, audioStart, audioDuration))
            } else {
                contentStart = imageStartTime + PLACE_DURATION
                audioStart = contentStart + contentDuration
            }

            // 添加内容动作
            var contentCursor = contentStart
            contentTypes.forEachIndexed { index, actionType ->
                if (index > 0) {
                    contentCursor += config.crossTypePause
                }
                val typeActions = createActionsForImageAndType(image, actionType, contentCursor)
                actions.addAll(typeActions)
                contentCursor += typeActions.totalDuration()
            }

            val contentEnd = contentStart + contentDuration
            val audioEnd = audioStart + audioDuration
            val minImageDuration = if (audioDuration > 0f) {
                audioDuration + contentDuration + config.extraAudioOffset
            } else {
                minImageIntervalWithoutAudio()
            }

            currentTime = maxOf(contentEnd, audioEnd, imageStartTime + minImageDuration) + config.crossImagePause
        }
        return currentTime
    }

    private fun createActionsForImageAndType(
        image: TierImage,
        actionType: VideoActionType,
        startTime: Float
    ): List<TimelineAction> {
        return when (actionType) {
            VideoActionType.PLACE -> listOf(
                TimelineAction.Place(image.id, startTime, PLACE_DURATION, image.tierLabel)
            )

            VideoActionType.NAME -> {
                if (image.name.isBlank()) {
                    emptyList()
                } else {
                    val duration = when (config.nameDisplayMode) {
                        NameDisplayMode.ONCE -> NAME_ONCE_DURATION
                        NameDisplayMode.PER_CHAR -> image.name.length * config.nameCharInterval
                    }
                    listOf(TimelineAction.Name(image.id, startTime, duration, image.name))
                }
            }

            VideoActionType.BADGE -> {
                val badgeUris = listOfNotNull(image.badgeUri, image.badgeUri2, image.badgeUri3)
                badgeUris.mapIndexed { index, _ ->
                    TimelineAction.Badge(
                        tierImageId = image.id,
                        startTime = startTime + index * config.badgeInterval,
                        duration = BADGE_DURATION,
                        slotIndex = index
                    )
                }
            }
        }
    }

    private fun List<VideoActionType>.totalContentDuration(image: TierImage): Float {
        var duration = 0f
        forEachIndexed { index, actionType ->
            if (index > 0) duration += config.crossTypePause
            duration += when (actionType) {
                VideoActionType.PLACE -> PLACE_DURATION
                VideoActionType.NAME -> if (image.name.isBlank()) 0f else when (config.nameDisplayMode) {
                    NameDisplayMode.ONCE -> NAME_ONCE_DURATION
                    NameDisplayMode.PER_CHAR -> image.name.length * config.nameCharInterval
                }
                VideoActionType.BADGE -> {
                    val badgeCount = listOfNotNull(image.badgeUri, image.badgeUri2, image.badgeUri3).size
                    if (badgeCount > 0) (badgeCount - 1) * config.badgeInterval + BADGE_DURATION else 0f
                }
            }
        }
        return duration
    }

    private fun minImageIntervalWithoutAudio(): Float {
        return when (config.imageIntervalSource) {
            AudioIntervalSource.AUDIO_DURATION,
            AudioIntervalSource.FIXED -> config.fixedImageInterval
        }
    }

    private fun List<TimelineAction>.totalDuration(): Float {
        if (isEmpty()) return 0f
        val start = minOf { it.startTime }
        val end = maxOf { it.endTime }
        return end - start
    }

    companion object {
        const val PLACE_DURATION = 0.3f
        const val NAME_ONCE_DURATION = 0.3f
        const val BADGE_DURATION = 0.3f
    }
}
