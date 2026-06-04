package com.tdds.jh.screens.tierlist.handler

import android.content.Context
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.platform.LocalContext
import com.tdds.jh.data.bitmap.TierImage
import com.tdds.jh.screens.tierlist.logic.utils.FileUtils
import com.tdds.jh.data.manager.ImageResourceManager
import com.tdds.jh.data.manager.PresetManager
import com.tdds.jh.screens.tierlist.state.DialogState
import com.tdds.jh.ui.toast.showToastWithoutIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 图片选择器处理类
 * 封装所有图片选择器的创建和回调逻辑
 */
class ImagePickerHandler(
    private val context: Context,
    private val scope: CoroutineScope,
    private val dialogState: DialogState,
    private val presetManager: PresetManager,
    private val tierImages: SnapshotStateList<TierImage>,
    private val onPendingImagesChange: (List<Uri>) -> Unit,
    private val onResumeDraftSave: (() -> Unit)?
) {

    // ==================== 图片选择器回调处理 ====================

    /**
     * 处理初始图片选择结果（多选）
     */
    fun handleImagePickerResult(
        uris: List<Uri>,
        currentPendingImages: List<Uri>
    ) {
        if (uris.isEmpty()) {
            resetPickerState()
            return
        }

        scope.launch {
            val importedUris = processImageImport(uris, currentPendingImages, isAppend = false)
            onPendingImagesChange(importedUris)
            resetPickerState()
        }
    }

    /**
     * 处理添加到待分级区域的图片选择结果（多选）
     */
    fun handleAddToPendingResult(
        uris: List<Uri>,
        currentPendingImages: List<Uri>
    ) {
        if (uris.isEmpty()) {
            resetPickerState()
            return
        }

        scope.launch {
            val importedUris = processImageImport(uris, currentPendingImages, isAppend = true)
            onPendingImagesChange(currentPendingImages + importedUris)

            if (importedUris.isNotEmpty()) {
                showToastWithoutIcon(context, "已添加 ${importedUris.size} 张图片")
            }
            resetPickerState()
        }
    }

    /**
     * 处理图片替换结果（单选）
     */
    fun handleReplaceImageResult(
        uri: Uri?,
        imageToReplace: TierImage?
    ) {
        if (uri == null || imageToReplace == null) {
            dialogState.imageToReplace = null
            return
        }

        val index = tierImages.indexOfFirst { it.id == imageToReplace.id }
        if (index == -1) {
            dialogState.imageToReplace = null
            return
        }

        val oldImage = tierImages[index]

        scope.launch {
            try {
                val newUri = copyImageToWorkDir(uri, PresetManager.IMAGES_FOLDER_NAME, "replaced_${System.currentTimeMillis()}.webp")

                // 返回原图到待分级区域
                val uriToReturn = oldImage.originalUri ?: oldImage.uri
                onPendingImagesChange(getCurrentPendingImages() + uriToReturn)

                // 替换为新图片
                tierImages[index] = tierImages[index].copy(
                    uri = newUri,
                    originalUri = newUri,
                    cropPositionX = 0.5f,
                    cropPositionY = 0.5f,
                    cropScale = 1.0f,
                    isCropped = false,
                    cropRatio = 0f,
                    useCustomCrop = false,
                    customCropWidth = 0,
                    customCropHeight = 0
                )
            } catch (e: Exception) {
                // 回退到原始方式
                val uriToReturn = oldImage.originalUri ?: oldImage.uri
                onPendingImagesChange(getCurrentPendingImages() + uriToReturn)
                tierImages[index] = tierImages[index].copy(
                    uri = uri,
                    originalUri = uri,
                    cropPositionX = 0.5f,
                    cropPositionY = 0.5f,
                    cropScale = 1.0f,
                    isCropped = false,
                    cropRatio = 0f,
                    useCustomCrop = false,
                    customCropWidth = 0,
                    customCropHeight = 0
                )
            }
            dialogState.imageToReplace = null
        }
    }

    /**
     * 处理小图标选择结果（单选）
     */
    fun handleBadgeImagePickerResult(
        uri: Uri?,
        badgeSelectionTarget: Int,
        imageForBadge: TierImage?,
        onBadgeDialogRefresh: () -> Unit
    ) {
        if (uri == null) {
            resetBadgeState()
            return
        }

        when {
            badgeSelectionTarget == 0 -> {
                // 添加小图标到工作目录
                scope.launch {
                    try {
                        copyImageToWorkDir(uri, PresetManager.BADGES_FOLDER_NAME, getBadgeFileName(uri))
                        onBadgeDialogRefresh()
                        showToastWithoutIcon(context, "小图标已添加")
                    } catch (e: Exception) {
                    } finally {
                        resetBadgeState()
                    }
                }
            }
            imageForBadge != null -> {
                // 为图片设置小图标
                scope.launch {
                    try {
                        val workUri = copyImageToWorkDir(uri, PresetManager.BADGES_FOLDER_NAME, getBadgeFileName(uri))
                        val index = tierImages.indexOfFirst { it.id == imageForBadge.id }
                        if (index != -1) {
                            tierImages[index] = when (badgeSelectionTarget) {
                                1 -> tierImages[index].copy(badgeUri = workUri)
                                2 -> tierImages[index].copy(badgeUri2 = workUri)
                                else -> tierImages[index].copy(badgeUri3 = workUri)
                            }
                            dialogState.imageForBadge = tierImages[index]
                        }
                        onBadgeDialogRefresh()
                    } catch (e: Exception) {
                        // 回退到原始URI
                        val index = tierImages.indexOfFirst { it.id == imageForBadge.id }
                        if (index != -1) {
                            tierImages[index] = when (badgeSelectionTarget) {
                                1 -> tierImages[index].copy(badgeUri = uri)
                                2 -> tierImages[index].copy(badgeUri2 = uri)
                                else -> tierImages[index].copy(badgeUri3 = uri)
                            }
                            dialogState.imageForBadge = tierImages[index]
                        }
                        onBadgeDialogRefresh()
                    } finally {
                        resetBadgeState()
                    }
                }
            }
            else -> resetBadgeState()
        }
    }

    /**
     * 处理批量小图标选择结果（多选）
     */
    fun handleBadgeImagePickerMultipleResult(
        uris: List<Uri>,
        onBadgeDialogRefresh: () -> Unit
    ) {
        if (uris.isEmpty()) {
            resetBadgeState()
            return
        }

        scope.launch {
            var successCount = 0
            var failCount = 0

            uris.forEachIndexed { index, uri ->
                try {
                    val fileName = FileUtils.getFileNameFromUri(context, uri)
                        ?: "${System.currentTimeMillis()}_$index.png"
                    copyImageToWorkDir(uri, PresetManager.BADGES_FOLDER_NAME, fileName)
                    successCount++
                } catch (e: Exception) {
                    failCount++
                }
            }

            onBadgeDialogRefresh()

            if (successCount > 0) {
                showToastWithoutIcon(context, "已添加 $successCount 张小图标")
            }
            resetBadgeState()
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 处理图片导入（支持WebP转换和哈希查重）
     */
    private suspend fun processImageImport(
        uris: List<Uri>,
        currentPendingImages: List<Uri>,
        isAppend: Boolean
    ): List<Uri> = withContext(Dispatchers.IO) {
        val workImagesDir = presetManager.getWorkImagesDirectory()
        val imagesDir = File(workImagesDir, "images")
        imagesDir.mkdirs()

        // 构建已有文件的哈希映射表
        val existingHashes = buildExistingHashMap(imagesDir)
        val importedUris = mutableListOf<Uri>()
        var convertedCount = 0
        var reusedCount = 0

        uris.forEach { uri ->
            // 如果是追加模式，检查URI是否已存在
            if (isAppend && uri in currentPendingImages) {
                reusedCount++
                return@forEach
            }

            val result = ImageResourceManager.importImageWithWebPAndHash(
                context,
                uri,
                imagesDir,
                existingHashes,
                convertToWebP = true
            )

            if (result.fileName != null) {
                val file = File(imagesDir, result.fileName)
                importedUris.add(Uri.fromFile(file))
                if (result.isConvertedToWebP) convertedCount++
                if (result.isReused) reusedCount++
            }
        }

        importedUris
    }

    /**
     * 构建已有文件的哈希映射表
     */
    private fun buildExistingHashMap(imagesDir: File): MutableMap<String, File> {
        val existingHashes = mutableMapOf<String, File>()
        imagesDir.listFiles()?.forEach { file ->
            if (file.isFile) {
                try {
                    val hash = ImageResourceManager.calculateQuickHash(file)
                    existingHashes[hash] = file
                } catch (e: Exception) {
                    // 忽略计算失败的文件
                }
            }
        }
        return existingHashes
    }

    /**
     * 复制图片到工作目录
     */
    private suspend fun copyImageToWorkDir(
        uri: Uri,
        folderName: String,
        fileName: String
    ): Uri = withContext(Dispatchers.IO) {
        val workDir = File(presetManager.getWorkImagesDirectory(), folderName)
        workDir.mkdirs()
        val destFile = File(workDir, fileName)

        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }

        Uri.fromFile(destFile)
    }

    /**
     * 获取小图标文件名
     */
    private fun getBadgeFileName(uri: Uri): String {
        return FileUtils.getFileNameFromUri(context, uri)
            ?: "${System.currentTimeMillis()}.png"
    }

    /**
     * 获取当前待分级图片列表（需要从外部状态获取）
     */
    private var currentPendingImagesProvider: (() -> List<Uri>)? = null

    fun setPendingImagesProvider(provider: () -> List<Uri>) {
        currentPendingImagesProvider = provider
    }

    private fun getCurrentPendingImages(): List<Uri> {
        return currentPendingImagesProvider?.invoke() ?: emptyList()
    }

    /**
     * 重置选择器状态
     */
    private fun resetPickerState() {
        dialogState.isImagePickerLaunching = false
        onResumeDraftSave?.invoke()
    }

    /**
     * 重置小图标选择器状态
     */
    private fun resetBadgeState() {
        dialogState.badgeSelectionTarget = 0
        dialogState.isBadgePickerLaunching = false
        onResumeDraftSave?.invoke()
    }
}

/**
 * 创建并记住 ImagePickerHandler 实例
 */
@Composable
fun rememberImagePickerHandler(
    scope: CoroutineScope,
    dialogState: DialogState,
    presetManager: PresetManager,
    tierImages: SnapshotStateList<TierImage>,
    onPendingImagesChange: (List<Uri>) -> Unit,
    onResumeDraftSave: (() -> Unit)?
): ImagePickerHandler {
    val context = LocalContext.current
    return remember(scope, dialogState, presetManager) {
        ImagePickerHandler(
            context = context,
            scope = scope,
            dialogState = dialogState,
            presetManager = presetManager,
            tierImages = tierImages,
            onPendingImagesChange = onPendingImagesChange,
            onResumeDraftSave = onResumeDraftSave
        )
    }
}
