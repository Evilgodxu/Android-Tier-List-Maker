package com.tdds.jh.model.tierlist.video

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

/**
 * 无感录制器
 * 用户正常编辑榜单时自动记录动作，不保留时间戳
 */
class ActionRecorder {

    private val _actions = mutableStateListOf<RecordedAction>()
    val actions: SnapshotStateList<RecordedAction> = _actions

    /** 是否启用录制 */
    var isRecording: Boolean = true

    /** 清空所有录制 */
    fun clear() = _actions.clear()

    /** 记录图片放置 */
    fun recordImagePlaced(tierImageId: String, tierLabel: String) {
        if (!isRecording) return
        _actions.add(RecordedAction.ImagePlaced(tierImageId, tierLabel))
    }

    /** 记录图片命名 */
    fun recordImageNamed(tierImageId: String, name: String) {
        if (!isRecording) return
        if (name.isBlank()) return
        _actions.add(RecordedAction.ImageNamed(tierImageId, name))
    }

    /** 记录小图标添加 */
    fun recordBadgeAdded(tierImageId: String, slotIndex: Int) {
        if (!isRecording) return
        require(slotIndex in 0..2) { "slotIndex must be 0..2" }
        _actions.add(RecordedAction.BadgeAdded(tierImageId, slotIndex))
    }

    /**
     * 获取某个图片的所有动作，按录制顺序
     */
    fun actionsForImage(tierImageId: String): List<RecordedAction> {
        return _actions.filter { it.tierImageId == tierImageId }
    }

    /**
     * 获取所有被录制过的图片 ID（按首次出现顺序）
     */
    fun recordedImageIds(): List<String> {
        return _actions.map { it.tierImageId }.distinct()
    }
}
