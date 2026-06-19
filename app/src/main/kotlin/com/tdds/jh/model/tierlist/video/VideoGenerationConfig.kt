package com.tdds.jh.model.tierlist.video

import android.os.Parcel
import android.os.Parcelable

/**
 * 视频生成配置
 * 与榜单数据解耦，可保存为编排预设
 */
data class VideoGenerationConfig(
    /** 三种动作类型的播放顺序 */
    val actionOrder: List<VideoActionType> = DEFAULT_ACTION_ORDER,

    /** 编排粒度 */
    val granularity: ArrangementGranularity = ArrangementGranularity.PER_IMAGE,

    /** 图片间隔来源 */
    val imageIntervalSource: AudioIntervalSource = AudioIntervalSource.AUDIO_DURATION,

    /** 手动固定图片间隔（秒） */
    val fixedImageInterval: Float = 1.5f,

    /** 同一图片内多个图标之间的间隔（秒） */
    val badgeInterval: Float = 0.3f,

    /** 命名显示模式 */
    val nameDisplayMode: NameDisplayMode = NameDisplayMode.ONCE,

    /** 逐字显示时每个字的间隔（秒） */
    val nameCharInterval: Float = 0.05f,

    /** 切换到下一类型动作前的全局等待（秒） */
    val crossTypePause: Float = 0f,

    /** 串行模式下完成一张图片全部动作后的额外停顿（秒，不含音频间隔） */
    val crossImagePause: Float = 0f,

    /** 在音频时长基础上统一增加的偏移（秒） */
    val extraAudioOffset: Float = 0f,

    /** 解说音频叠加方式 */
    val audioOverlayMode: AudioOverlayMode = AudioOverlayMode.KEEP_SFX,

    /** 背景音乐 URI（可为空） */
    val backgroundMusicUri: String? = null,

    /** 背景音乐音量 0~1 */
    val backgroundMusicVolume: Float = 0.3f,

    /** 解说音频音量 0~1 */
    val narrationVolume: Float = 1f,

    /** 动作音效音量 0~1 */
    val sfxVolume: Float = 0.6f,

    /** 输出宽度 */
    val outputWidth: Int = 1920,

    /** 输出高度 */
    val outputHeight: Int = 1080
) : Parcelable {

    constructor(parcel: Parcel) : this(
        actionOrder = mutableListOf<VideoActionType>().apply {
            parcel.readList(this, VideoActionType::class.java.classLoader)
        },
        granularity = ArrangementGranularity.entries[parcel.readInt()],
        imageIntervalSource = AudioIntervalSource.entries[parcel.readInt()],
        fixedImageInterval = parcel.readFloat(),
        badgeInterval = parcel.readFloat(),
        nameDisplayMode = NameDisplayMode.entries[parcel.readInt()],
        nameCharInterval = parcel.readFloat(),
        crossTypePause = parcel.readFloat(),
        crossImagePause = parcel.readFloat(),
        extraAudioOffset = parcel.readFloat(),
        audioOverlayMode = AudioOverlayMode.entries[parcel.readInt()],
        backgroundMusicUri = parcel.readString(),
        backgroundMusicVolume = parcel.readFloat(),
        narrationVolume = parcel.readFloat(),
        sfxVolume = parcel.readFloat(),
        outputWidth = parcel.readInt(),
        outputHeight = parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeList(actionOrder)
        parcel.writeInt(granularity.ordinal)
        parcel.writeInt(imageIntervalSource.ordinal)
        parcel.writeFloat(fixedImageInterval)
        parcel.writeFloat(badgeInterval)
        parcel.writeInt(nameDisplayMode.ordinal)
        parcel.writeFloat(nameCharInterval)
        parcel.writeFloat(crossTypePause)
        parcel.writeFloat(crossImagePause)
        parcel.writeFloat(extraAudioOffset)
        parcel.writeInt(audioOverlayMode.ordinal)
        parcel.writeString(backgroundMusicUri)
        parcel.writeFloat(backgroundMusicVolume)
        parcel.writeFloat(narrationVolume)
        parcel.writeFloat(sfxVolume)
        parcel.writeInt(outputWidth)
        parcel.writeInt(outputHeight)
    }

    override fun describeContents(): Int = 0

    companion object {
        val DEFAULT_ACTION_ORDER = listOf(VideoActionType.PLACE, VideoActionType.NAME, VideoActionType.BADGE)

        @JvmField
        val CREATOR: Parcelable.Creator<VideoGenerationConfig> = object : Parcelable.Creator<VideoGenerationConfig> {
            override fun createFromParcel(parcel: Parcel): VideoGenerationConfig = VideoGenerationConfig(parcel)
            override fun newArray(size: Int): Array<VideoGenerationConfig?> = arrayOfNulls(size)
        }
    }
}
