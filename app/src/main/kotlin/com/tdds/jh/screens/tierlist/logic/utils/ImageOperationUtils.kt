package com.tdds.jh.screens.tierlist.logic.utils

import androidx.compose.runtime.snapshots.SnapshotStateList
import com.tdds.jh.model.tierlist.TierImage

/**
 * 图片操作工具类
 * 提供图片交换、移动等操作的通用方法
 */
object ImageOperationUtils {

    /**
     * 交换两张图片的内容（保留各自的 ID 和层级标签）
     * 用于双击交换或拖拽交换场景
     */
    fun swapImageContents(
        tierImages: SnapshotStateList<TierImage>,
        fromId: String,
        toId: String,
        onImageForActionUpdate: ((TierImage) -> Unit)? = null,
        onImageToReplaceUpdate: ((TierImage) -> Unit)? = null,
        onImageForBadgeUpdate: ((TierImage) -> Unit)? = null
    ): Boolean {
        val fromIndex = tierImages.indexOfFirst { it.id == fromId }
        val toIndex = tierImages.indexOfFirst { it.id == toId }

        if (fromIndex == -1 || toIndex == -1 || fromIndex == toIndex) {
            return false
        }

        val fromImage = tierImages[fromIndex]
        val toImage = tierImages[toIndex]

        val newFromImage = fromImage.copy(
            uri = toImage.uri,
            name = toImage.name,
            badgeUri = toImage.badgeUri,
            badgeUri2 = toImage.badgeUri2,
            badgeUri3 = toImage.badgeUri3,
            originalUri = toImage.originalUri,
            cropPositionX = toImage.cropPositionX,
            cropPositionY = toImage.cropPositionY,
            cropScale = toImage.cropScale,
            isCropped = toImage.isCropped,
            cropRatio = toImage.cropRatio,
            useCustomCrop = toImage.useCustomCrop,
            customCropWidth = toImage.customCropWidth,
            customCropHeight = toImage.customCropHeight
        )
        val newToImage = toImage.copy(
            uri = fromImage.uri,
            name = fromImage.name,
            badgeUri = fromImage.badgeUri,
            badgeUri2 = fromImage.badgeUri2,
            badgeUri3 = fromImage.badgeUri3,
            originalUri = fromImage.originalUri,
            cropPositionX = fromImage.cropPositionX,
            cropPositionY = fromImage.cropPositionY,
            cropScale = fromImage.cropScale,
            isCropped = fromImage.isCropped,
            cropRatio = fromImage.cropRatio,
            useCustomCrop = fromImage.useCustomCrop,
            customCropWidth = fromImage.customCropWidth,
            customCropHeight = fromImage.customCropHeight
        )

        tierImages[fromIndex] = newFromImage
        tierImages[toIndex] = newToImage

        onImageForActionUpdate?.invoke(newFromImage)
        onImageForActionUpdate?.invoke(newToImage)
        onImageToReplaceUpdate?.invoke(newFromImage)
        onImageToReplaceUpdate?.invoke(newToImage)
        onImageForBadgeUpdate?.invoke(newFromImage)
        onImageForBadgeUpdate?.invoke(newToImage)

        return true
    }

    /**
     * 将图片移动到指定层级的末尾
     */
    fun moveImageToTier(
        tierImages: SnapshotStateList<TierImage>,
        imageId: String,
        targetTierLabel: String
    ): Boolean {
        val index = tierImages.indexOfFirst { it.id == imageId }
        if (index == -1) return false

        val oldTier = tierImages[index].tierLabel
        if (oldTier == targetTierLabel) return false

        val movedImage = tierImages.removeAt(index).copy(tierLabel = targetTierLabel)
        tierImages.add(movedImage)

        return true
    }

    fun moveImageToFirst(
        tierImages: SnapshotStateList<TierImage>,
        imageId: String
    ): Boolean {
        val currentIndex = tierImages.indexOfFirst { it.id == imageId }
        if (currentIndex == -1) return false

        val currentTier = tierImages[currentIndex].tierLabel
        val tierImagesList = tierImages.filter { it.tierLabel == currentTier }
        if (tierImagesList.size <= 1) return false

        val firstIndex = tierImages.indexOfFirst { it.tierLabel == currentTier }
        if (currentIndex == firstIndex) return false

        val image = tierImages.removeAt(currentIndex)
        tierImages.add(firstIndex, image)

        return true
    }

    fun moveImageToLast(
        tierImages: SnapshotStateList<TierImage>,
        imageId: String
    ): Boolean {
        val currentIndex = tierImages.indexOfFirst { it.id == imageId }
        if (currentIndex == -1) return false

        val currentTier = tierImages[currentIndex].tierLabel
        val tierImagesList = tierImages.filter { it.tierLabel == currentTier }
        if (tierImagesList.size <= 1) return false

        val lastIndex = tierImages.indexOfLast { it.tierLabel == currentTier }
        if (currentIndex == lastIndex) return false

        val image = tierImages.removeAt(currentIndex)
        val newLastIndex = tierImages.indexOfLast { it.tierLabel == currentTier }
        tierImages.add(newLastIndex + 1, image)

        return true
    }

    fun moveImageLeft(
        tierImages: SnapshotStateList<TierImage>,
        imageId: String
    ): Boolean {
        val currentIndex = tierImages.indexOfFirst { it.id == imageId }
        if (currentIndex == -1) return false

        val currentTier = tierImages[currentIndex].tierLabel
        val tierIndices = tierImages.withIndex()
            .filter { it.value.tierLabel == currentTier }
            .map { it.index }
        val currentPosition = tierIndices.indexOf(currentIndex)

        if (currentPosition <= 0) return false

        val leftIndex = tierIndices[currentPosition - 1]
        val temp = tierImages[currentIndex]
        tierImages[currentIndex] = tierImages[leftIndex]
        tierImages[leftIndex] = temp

        return true
    }

    fun moveImageRight(
        tierImages: SnapshotStateList<TierImage>,
        imageId: String
    ): Boolean {
        val currentIndex = tierImages.indexOfFirst { it.id == imageId }
        if (currentIndex == -1) return false

        val currentTier = tierImages[currentIndex].tierLabel
        val tierIndices = tierImages.withIndex()
            .filter { it.value.tierLabel == currentTier }
            .map { it.index }
        val currentPosition = tierIndices.indexOf(currentIndex)

        if (currentPosition >= tierIndices.size - 1) return false

        val rightIndex = tierIndices[currentPosition + 1]
        val temp = tierImages[currentIndex]
        tierImages[currentIndex] = tierImages[rightIndex]
        tierImages[rightIndex] = temp

        return true
    }
}
