package com.tdds.jh.screens.tierlist.utils

import androidx.compose.runtime.snapshots.SnapshotStateList
import com.tdds.jh.data.bitmap.TierImage

/**
 * 图片操作工具类
 * 提供图片交换、移动等操作的通用方法
 */
object ImageOperationUtils {

    /**
     * 交换两张图片的内容（保留各自的 ID 和层级标签）
     * 用于双击交换或拖拽交换场景
     *
     * @param tierImages 图片列表
     * @param fromId 源图片 ID
     * @param toId 目标图片 ID
     * @param onImageForActionUpdate 更新操作对话框中图片的回调（可选）
     * @param onImageToReplaceUpdate 更新替换图片的回调（可选）
     * @param onImageForBadgeUpdate 更新小图标对话框中图片的回调（可选）
     * @return 是否成功交换
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

        // 交换图片的 uri（内容）、name（命名）、badgeUri（小图标）、originalUri（原图）和裁剪信息
        // 保持各自的 id 和 tierLabel 不变，这样图片在各自层级中的位置不变
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

        // 更新相关引用，确保操作对话框、替换、小图标等使用正确的图片数据
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
     *
     * @param tierImages 图片列表
     * @param imageId 要移动的图片 ID
     * @param targetTierLabel 目标层级标签
     * @return 是否成功移动
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

        // 从原位置移除图片，并修改层级标签
        val movedImage = tierImages.removeAt(index).copy(tierLabel = targetTierLabel)
        // 将图片添加到新层级的末尾
        tierImages.add(movedImage)

        return true
    }

    /**
     * 将图片移到当前层级的第一位
     *
     * @param tierImages 图片列表
     * @param imageId 要移动的图片 ID
     * @return 是否成功移动
     */
    fun moveImageToFirst(
        tierImages: SnapshotStateList<TierImage>,
        imageId: String
    ): Boolean {
        val currentIndex = tierImages.indexOfFirst { it.id == imageId }
        if (currentIndex == -1) return false

        val currentTier = tierImages[currentIndex].tierLabel
        // 获取当前层级的所有图片
        val tierImagesList = tierImages.filter { it.tierLabel == currentTier }
        if (tierImagesList.size <= 1) return false

        // 找到当前层级第一张图片的索引
        val firstIndex = tierImages.indexOfFirst { it.tierLabel == currentTier }
        if (currentIndex == firstIndex) return false

        // 移动位置
        val image = tierImages.removeAt(currentIndex)
        tierImages.add(firstIndex, image)

        return true
    }

    /**
     * 将图片移到当前层级的最后一位
     *
     * @param tierImages 图片列表
     * @param imageId 要移动的图片 ID
     * @return 是否成功移动
     */
    fun moveImageToLast(
        tierImages: SnapshotStateList<TierImage>,
        imageId: String
    ): Boolean {
        val currentIndex = tierImages.indexOfFirst { it.id == imageId }
        if (currentIndex == -1) return false

        val currentTier = tierImages[currentIndex].tierLabel
        // 获取当前层级的所有图片
        val tierImagesList = tierImages.filter { it.tierLabel == currentTier }
        if (tierImagesList.size <= 1) return false

        // 找到当前层级最后一张图片的索引
        val lastIndex = tierImages.indexOfLast { it.tierLabel == currentTier }
        if (currentIndex == lastIndex) return false

        // 移除图片
        val image = tierImages.removeAt(currentIndex)
        // 重新计算最后位置（因为移除后索引变了）
        val newLastIndex = tierImages.indexOfLast { it.tierLabel == currentTier }
        tierImages.add(newLastIndex + 1, image)

        return true
    }

    /**
     * 将图片向左移动一位（与左边图片交换位置）
     *
     * @param tierImages 图片列表
     * @param imageId 要移动的图片 ID
     * @return 是否成功移动
     */
    fun moveImageLeft(
        tierImages: SnapshotStateList<TierImage>,
        imageId: String
    ): Boolean {
        val currentIndex = tierImages.indexOfFirst { it.id == imageId }
        if (currentIndex == -1) return false

        val currentTier = tierImages[currentIndex].tierLabel
        // 获取当前层级的所有图片索引
        val tierIndices = tierImages.withIndex()
            .filter { it.value.tierLabel == currentTier }
            .map { it.index }
        val currentPosition = tierIndices.indexOf(currentIndex)

        // 如果不是第一个，则与左边图片交换
        if (currentPosition <= 0) return false

        val leftIndex = tierIndices[currentPosition - 1]
        // 交换两个图片的位置
        val temp = tierImages[currentIndex]
        tierImages[currentIndex] = tierImages[leftIndex]
        tierImages[leftIndex] = temp

        return true
    }

    /**
     * 将图片向右移动一位（与右边图片交换位置）
     *
     * @param tierImages 图片列表
     * @param imageId 要移动的图片 ID
     * @return 是否成功移动
     */
    fun moveImageRight(
        tierImages: SnapshotStateList<TierImage>,
        imageId: String
    ): Boolean {
        val currentIndex = tierImages.indexOfFirst { it.id == imageId }
        if (currentIndex == -1) return false

        val currentTier = tierImages[currentIndex].tierLabel
        // 获取当前层级的所有图片索引
        val tierIndices = tierImages.withIndex()
            .filter { it.value.tierLabel == currentTier }
            .map { it.index }
        val currentPosition = tierIndices.indexOf(currentIndex)

        // 如果不是最后一个，则与右边图片交换
        if (currentPosition >= tierIndices.size - 1) return false

        val rightIndex = tierIndices[currentPosition + 1]
        // 交换两个图片的位置
        val temp = tierImages[currentIndex]
        tierImages[currentIndex] = tierImages[rightIndex]
        tierImages[rightIndex] = temp

        return true
    }
}
