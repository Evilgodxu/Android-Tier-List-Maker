package com.tdds.jh.screens.tierlist.handler

import android.content.Context
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import com.tdds.jh.data.bitmap.TierImage
import com.tdds.jh.data.bitmap.TierItem
import com.tdds.jh.screens.tierlist.logic.utils.FileUtils
import com.tdds.jh.data.manager.PresetManager
import com.tdds.jh.screens.tierlist.state.DialogState
import com.tdds.jh.screens.tierlist.service.SettingsService
import com.tdds.jh.ui.toast.showToastWithoutIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File

/**
 * 预设操作处理类
 * 封装预设导入、导出和草稿恢复的业务逻辑
 */
class PresetOperationHandler(
    private val context: Context,
    private val scope: CoroutineScope,
    private val dialogState: DialogState,
    private val presetManager: PresetManager,
    private val settingsService: SettingsService,
    private val tiers: SnapshotStateList<TierItem>,
    private val tierImages: SnapshotStateList<TierImage>,
    private val onPendingImagesChange: (List<Uri>) -> Unit,
    private val onTitleChange: (String) -> Unit,
    private val onAuthorChange: (String) -> Unit,
    private val onTierRowPositionsReset: () -> Unit,
    private val onResumeDraftSave: (() -> Unit)?,
    private val onSkipDraftSave: (() -> Unit)?,
    private val showToast: (String, Int) -> Unit
) {

    // ==================== 预设导入 ====================

    /**
     * 处理从文件选择器导入预设
     */
    fun handleImportPreset(
        uri: Uri?,
        currentPendingImages: List<Uri>
    ) {
        if (uri == null) {
            onResumeDraftSave?.invoke()
            return
        }

        dialogState.isImportingPreset = true
        onSkipDraftSave?.invoke()

        scope.launch {
            try {
                val importResult = withContext(Dispatchers.IO) {
                    presetManager.importPreset(uri)
                }

                when (importResult.status) {
                    PresetManager.ImportStatus.SUCCESS,
                    PresetManager.ImportStatus.ALREADY_EXISTS -> {
                        applyImportedPreset(importResult, currentPendingImages)
                    }
                    PresetManager.ImportStatus.NEEDS_OVERWRITE -> {
                        dialogState.pendingImportResult = importResult
                        dialogState.showImportOverwriteDialog = true
                    }
                }
            } catch (e: Exception) {
                showToast("导入预设失败: ${e.message}", android.widget.Toast.LENGTH_LONG)
            } finally {
                dialogState.isImportingPreset = false
                onResumeDraftSave?.invoke()
            }
        }
    }

    /**
     * 处理从外部打开 .tdds 文件
     */
    fun handleExternalTddsFile(
        uri: Uri,
        onSkipDraftRestore: () -> Unit
    ) {
        dialogState.isImportingPreset = true
        onSkipDraftRestore()
        onSkipDraftSave?.invoke()

        scope.launch {
            try {
                val importResult = withContext(Dispatchers.IO) {
                    presetManager.importPreset(uri)
                }

                when (importResult.status) {
                    PresetManager.ImportStatus.SUCCESS,
                    PresetManager.ImportStatus.ALREADY_EXISTS -> {
                        applyImportedPreset(importResult, emptyList())
                    }
                    PresetManager.ImportStatus.NEEDS_OVERWRITE -> {
                        dialogState.pendingImportResult = importResult
                        dialogState.showImportOverwriteDialog = true
                    }
                }
            } catch (e: Exception) {
                showToast("导入预设失败: ${e.message}", android.widget.Toast.LENGTH_LONG)
            } finally {
                dialogState.isImportingPreset = false
                onResumeDraftSave?.invoke()
            }
        }
    }

    /**
     * 应用导入的预设
     */
    private suspend fun applyImportedPreset(
        importResult: PresetManager.ImportResult,
        currentPendingImages: List<Uri>
    ) {
        val result = withContext(Dispatchers.IO) {
            presetManager.applyPreset(importResult.presetFile)
        }

        // 更新层级
        tiers.clear()
        tiers.addAll(result.tiers.map { tierData ->
            TierItem(
                tierData.label,
                try {
                    Color(android.graphics.Color.parseColor("#${tierData.color}"))
                } catch (e: Exception) {
                    Color.Gray
                }
            )
        })

        // 更新层级图片
        tierImages.clear()
        tierImages.addAll(result.tierImages.map { appliedImage ->
            TierImage(
                id = appliedImage.id,
                tierLabel = appliedImage.tierLabel,
                uri = appliedImage.uri,
                name = appliedImage.name,
                badgeUri = appliedImage.badgeUri,
                badgeUri2 = appliedImage.badgeUri2,
                badgeUri3 = appliedImage.badgeUri3,
                originalUri = appliedImage.originalUri,
                cropPositionX = appliedImage.cropPositionX,
                cropPositionY = appliedImage.cropPositionY,
                cropScale = appliedImage.cropScale,
                isCropped = appliedImage.isCropped,
                cropRatio = appliedImage.cropRatio,
                useCustomCrop = appliedImage.useCustomCrop,
                customCropWidth = appliedImage.customCropWidth,
                customCropHeight = appliedImage.customCropHeight
            )
        })

        // 更新待分级图片
        onPendingImagesChange(result.pendingImages)

        // 更新标题和作者
        onTitleChange(importResult.presetData.title)
        onAuthorChange(importResult.presetData.author)

        // 应用裁剪设置
        settingsService.clearCropSettings()
        settingsService.customCropWidth = result.customCropWidth
        settingsService.customCropHeight = result.customCropHeight
        settingsService.useCustomCropSize = result.useCustomCropSize
        settingsService.cropRatio = result.cropRatio

        // 清空层级位置信息
        onTierRowPositionsReset()

        val message = when (importResult.status) {
            PresetManager.ImportStatus.ALREADY_EXISTS -> "预设已加载"
            else -> "导入预设成功"
        }
        showToast(message, android.widget.Toast.LENGTH_SHORT)
    }

    // ==================== 预设导出 ====================

    /**
     * 处理预设导出
     */
    fun handleExportPreset(
        uri: Uri?,
        presetName: String,
        tierListTitle: String,
        authorName: String,
        currentPendingImages: List<Uri>
    ) {
        if (uri == null) {
            onResumeDraftSave?.invoke()
            return
        }

        dialogState.isExportingPreset = true
        onSkipDraftSave?.invoke()

        scope.launch {
            try {
                // 让出时间片，确保UI有时间显示加载对话框
                yield()

                val presetData = withContext(Dispatchers.IO) {
                    presetManager.createPresetData(
                        title = tierListTitle,
                        author = authorName,
                        tiers = tiers,
                        tierImages = tierImages,
                        pendingImages = currentPendingImages,
                        cropPositionX = settingsService.cropPositionX,
                        cropPositionY = settingsService.cropPositionY,
                        customCropWidth = settingsService.customCropWidth,
                        customCropHeight = settingsService.customCropHeight,
                        useCustomCropSize = settingsService.useCustomCropSize,
                        cropRatio = settingsService.cropRatio
                    )
                }

                val outputFile = File(context.cacheDir, "$presetName.tdds")

                withContext(Dispatchers.IO) {
                    presetManager.exportPreset(presetData, outputFile)

                    // 复制到用户选择的位置
                    context.contentResolver.openOutputStream(uri, "rwt")?.use { output ->
                        java.io.FileInputStream(outputFile).use { input ->
                            input.copyTo(output)
                        }
                    }
                    outputFile.delete()
                }

                showToast("导出预设成功", android.widget.Toast.LENGTH_SHORT)

            } catch (e: Exception) {
                showToast("导出预设失败: ${e.message}", android.widget.Toast.LENGTH_LONG)
            } finally {
                dialogState.isExportingPreset = false
                onResumeDraftSave?.invoke()
            }
        }
    }

    // ==================== 草稿恢复 ====================

    /**
     * 检查并加载草稿
     */
    fun checkAndLoadDraft(
        skipDraftRestore: Boolean,
        onDraftFound: (com.tdds.jh.data.manager.PresetData) -> Unit
    ) {
        if (skipDraftRestore) return

        scope.launch {
            try {
                val draftExists = withContext(Dispatchers.IO) {
                    presetManager.hasDraft()
                }

                if (draftExists) {
                    val draftData = withContext(Dispatchers.IO) {
                        presetManager.readDraftConfig()
                    }

                    if (draftData != null) {
                        onDraftFound(draftData)
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

    /**
     * 恢复草稿
     */
    fun restoreDraft(
        draftData: com.tdds.jh.data.manager.PresetData,
        onLoadingStateChange: (Boolean) -> Unit
    ) {
        onLoadingStateChange(true)

        scope.launch {
            try {
                val draftFile = withContext(Dispatchers.IO) {
                    presetManager.obtainDraftFile()
                }

                if (draftFile != null) {
                    val result = withContext(Dispatchers.IO) {
                        presetManager.restoreDraft(draftFile)
                    }

                    // 更新层级
                    tiers.clear()
                    tiers.addAll(result.tiers.map { tierData ->
                        TierItem(
                            tierData.label,
                            try {
                                Color(android.graphics.Color.parseColor("#${tierData.color}"))
                            } catch (e: Exception) {
                                Color.Gray
                            }
                        )
                    })

                    // 更新层级图片
                    tierImages.clear()
                    tierImages.addAll(result.tierImages.map { appliedImage ->
                        TierImage(
                            id = appliedImage.id,
                            tierLabel = appliedImage.tierLabel,
                            uri = appliedImage.uri,
                            name = appliedImage.name,
                            badgeUri = appliedImage.badgeUri,
                            badgeUri2 = appliedImage.badgeUri2,
                            badgeUri3 = appliedImage.badgeUri3,
                            originalUri = appliedImage.originalUri,
                            cropPositionX = appliedImage.cropPositionX,
                            cropPositionY = appliedImage.cropPositionY,
                            cropScale = appliedImage.cropScale,
                            isCropped = appliedImage.isCropped,
                            cropRatio = appliedImage.cropRatio,
                            useCustomCrop = appliedImage.useCustomCrop,
                            customCropWidth = appliedImage.customCropWidth,
                            customCropHeight = appliedImage.customCropHeight
                        )
                    })

                    // 更新待分级图片
                    onPendingImagesChange(result.pendingImages)

                    // 更新标题和作者
                    onTitleChange(draftData.title)
                    onAuthorChange(draftData.author)

                    // 应用裁剪设置
                    settingsService.clearCropSettings()
                    settingsService.customCropWidth = result.customCropWidth
                    settingsService.customCropHeight = result.customCropHeight
                    settingsService.useCustomCropSize = result.useCustomCropSize
                    settingsService.cropRatio = result.cropRatio

                    // 清空层级位置信息
                    onTierRowPositionsReset()

                    showToast("草稿已恢复", android.widget.Toast.LENGTH_SHORT)
                } else {
                    throw IllegalStateException("加载草稿失败")
                }
            } catch (e: Exception) {
                showToast("恢复草稿失败", android.widget.Toast.LENGTH_SHORT)
            } finally {
                onLoadingStateChange(false)
            }
        }
    }

    /**
     * 保存草稿
     */
    fun saveDraft(
        tierListTitle: String,
        authorName: String,
        currentPendingImages: List<Uri>
    ) {
        kotlinx.coroutines.runBlocking {
            try {
                val presetData = presetManager.createPresetData(
                    title = tierListTitle,
                    author = authorName,
                    tiers = tiers,
                    tierImages = tierImages,
                    pendingImages = currentPendingImages,
                    cropPositionX = settingsService.cropPositionX,
                    cropPositionY = settingsService.cropPositionY,
                    customCropWidth = settingsService.customCropWidth,
                    customCropHeight = settingsService.customCropHeight,
                    useCustomCropSize = settingsService.useCustomCropSize,
                    cropRatio = settingsService.cropRatio
                )
                presetManager.saveDraft(presetData)
            } catch (_: Exception) {
            }
        }
    }

    /**
     * 处理外部预设导入
     */
    fun handleExternalPresetImport(
        uri: Uri,
        onLoadingStateChange: (Boolean) -> Unit
    ) {
        scope.launch {
            try {
                val importResult = withContext(Dispatchers.IO) {
                    presetManager.importPreset(uri)
                }

                when (importResult.status) {
                    PresetManager.ImportStatus.SUCCESS,
                    PresetManager.ImportStatus.ALREADY_EXISTS -> {
                        applyImportedPreset(importResult, emptyList())
                        // 清理草稿文件（保留工作目录中的图片）
                        presetManager.cleanupDraftOnly()
                        onLoadingStateChange(false)
                    }
                    PresetManager.ImportStatus.NEEDS_OVERWRITE -> {
                        dialogState.pendingImportResult = importResult
                        dialogState.showImportOverwriteDialog = true
                        onLoadingStateChange(false)
                    }
                }
            } catch (e: Exception) {
                onLoadingStateChange(false)
                showToast("导入预设失败: ${e.message}", android.widget.Toast.LENGTH_SHORT)
            }
        }
    }
}

/**
 * 创建并记住 PresetOperationHandler 实例
 */
@Composable
fun rememberPresetOperationHandler(
    scope: CoroutineScope,
    dialogState: DialogState,
    presetManager: PresetManager,
    settingsService: SettingsService,
    tiers: SnapshotStateList<TierItem>,
    tierImages: SnapshotStateList<TierImage>,
    onPendingImagesChange: (List<Uri>) -> Unit,
    onTitleChange: (String) -> Unit,
    onAuthorChange: (String) -> Unit,
    onTierRowPositionsReset: () -> Unit,
    onResumeDraftSave: (() -> Unit)?,
    onSkipDraftSave: (() -> Unit)?,
    showToast: (String, Int) -> Unit
): PresetOperationHandler {
    val context = LocalContext.current
    return remember(scope, dialogState, presetManager) {
        PresetOperationHandler(
            context = context,
            scope = scope,
            dialogState = dialogState,
            presetManager = presetManager,
            settingsService = settingsService,
            tiers = tiers,
            tierImages = tierImages,
            onPendingImagesChange = onPendingImagesChange,
            onTitleChange = onTitleChange,
            onAuthorChange = onAuthorChange,
            onTierRowPositionsReset = onTierRowPositionsReset,
            onResumeDraftSave = onResumeDraftSave,
            onSkipDraftSave = onSkipDraftSave,
            showToast = showToast
        )
    }
}
