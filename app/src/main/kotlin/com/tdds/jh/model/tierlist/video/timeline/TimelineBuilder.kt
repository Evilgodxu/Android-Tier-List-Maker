package com.tdds.jh.model.tierlist.video.timeline

import com.tdds.jh.data.tierlist.video.AudioDurationProvider
import com.tdds.jh.model.tierlist.TierImage
import com.tdds.jh.model.tierlist.video.ArrangementGranularity
import com.tdds.jh.model.tierlist.video.AudioIntervalSource
import com.tdds.jh.model.tierlist.video.GranularityMode
import com.tdds.jh.model.tierlist.video.NameDisplayMode
import com.tdds.jh.model.tierlist.video.VideoActionType
import com.tdds.jh.model.tierlist.video.VideoGenerationConfig

/**
 * 时间线构建引擎
 *
 * 根据当前 tierImages 顺序、视频配置与录制动作，生成带时间戳的 Timeline。
 */
class TimelineBuilder(
    private val config: VideoGenerationConfig,
    private val audioDurationProvider: AudioDurationProvider
) {

    /**
     * 构建时间线
     */
    fun build(tierImages: List<TierImage>): Timeline {
        val actions = mutableListOf<TimelineAction>()
        var currentTime = 0f

        when (config.granularity) {
            ArrangementGranularity.PER_IMAGE -> buildPerImage(tierImages, actions, currentTime)
            ArrangementGranularity.PER_TYPE -> buildPerType(tierImages, actions, currentTime)
            ArrangementGranularity.MIXED -> buildMixed(tierImages, actions, currentTime)
        }

        val audioSegments = buildAudioSegments(tierImages, actions)
        val maxActionEnd = actions.maxOfOrNull { it.endTime } ?: 0f
        val maxAudioEnd = audioSegments.maxOfOrNull { it.startTime + it.duration } ?: 0f
        val totalDuration = maxOf(maxActionEnd, maxAudioEnd)

        return Timeline(actions, audioSegments, totalDuration)
    }

    private fun buildPerImage(
        tierImages: List<TierImage>,
        actions: MutableList<TimelineAction>,
        initialTime: Float
    ): Float {
        var currentTime = initialTime
        tierImages.forEachIndexed { imageIndex, image ->
            if (imageIndex > 0) {
                currentTime += imageIntervalFor(image)
            }
            config.actionOrder.forEachIndexed { typeIndex, actionType ->
                if (typeIndex > 0) {
                    currentTime += config.crossTypePause
                }
                val imageActions = createActionsForImageAndType(image, actionType, currentTime)
                actions.addAll(imageActions)
                currentTime += imageActions.totalDuration()
            }
        }
        return currentTime
    }

    private fun buildPerType(
        tierImages: List<TierImage>,
        actions: MutableList<TimelineAction>,
        initialTime: Float
    ): Float {
        var currentTime = initialTime
        config.actionOrder.forEachIndexed { typeIndex, actionType ->
            if (typeIndex > 0) {
                currentTime += config.crossTypePause
            }
            tierImages.forEach { image ->
                val imageActions = createActionsForImageAndType(image, actionType, currentTime)
                actions.addAll(imageActions)
                currentTime += imageActions.totalDuration()
            }
        }
        return currentTime
    }

    private fun buildMixed(
        tierImages: List<TierImage>,
        actions: MutableList<TimelineAction>,
        initialTime: Float
    ): Float {
        var currentTime = initialTime
        config.actionOrder.forEachIndexed { typeIndex, actionType ->
            if (typeIndex > 0) {
                currentTime += config.crossTypePause
            }
            val mode = config.mixedGranularity[actionType] ?: GranularityMode.PER_IMAGE
            if (mode == GranularityMode.PER_IMAGE) {
                tierImages.forEachIndexed { imageIndex, image ->
                    if (imageIndex > 0) {
                        currentTime += config.crossImagePause
                    }
                    val imageActions = createActionsForImageAndType(image, actionType, currentTime)
                    actions.addAll(imageActions)
                    currentTime += imageActions.totalDuration()
                }
            } else {
                tierImages.forEach { image ->
                    val imageActions = createActionsForImageAndType(image, actionType, currentTime)
                    actions.addAll(imageActions)
                    currentTime += imageActions.totalDuration()
                }
            }
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

    private fun imageIntervalFor(image: TierImage): Float {
        val base = when (config.imageIntervalSource) {
            AudioIntervalSource.AUDIO_DURATION -> {
                image.audioUri?.let { audioDurationProvider.getDurationSeconds(it) }
                    ?: config.fixedImageInterval
            }

            AudioIntervalSource.FIXED -> config.fixedImageInterval
        }
        return base + config.extraAudioOffset + config.crossImagePause
    }

    private fun buildAudioSegments(
        tierImages: List<TierImage>,
        actions: List<TimelineAction>
    ): List<AudioSegment> {
        val placeActions = actions.filterIsInstance<TimelineAction.Place>()
        val segments = mutableListOf<AudioSegment>()
        tierImages.forEach { image ->
            val uri = image.audioUri ?: return@forEach
            val place = placeActions.find { it.tierImageId == image.id } ?: return@forEach
            val duration = audioDurationProvider.getDurationSeconds(uri)
            if (duration > 0f) {
                segments.add(AudioSegment(image.id, uri, place.startTime, duration))
            }
        }
        return segments
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
