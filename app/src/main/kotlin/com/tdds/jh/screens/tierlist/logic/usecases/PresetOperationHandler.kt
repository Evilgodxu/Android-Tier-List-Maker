package com.tdds.jh.screens.tierlist.logic.usecases

import android.content.Context
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import com.tdds.jh.model.tierlist.TierImage
import com.tdds.jh.model.tierlist.TierItem
import com.tdds.jh.model.tierlist.video.VideoGenerationConfig
import com.tdds.jh.screens.tierlist.logic.utils.FileUtils
import com.tdds.jh.data.tierlist.PresetManager
import com.tdds.jh.data.tierlist.PresetData
import com.tdds.jh.screens.tierlist.DialogState
import com.tdds.jh.screens.tierlist.service.SettingsService
import com.tdds.jh.ui.toast.showToastWithoutIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File

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
    private val onVideoConfigChange: (VideoGenerationConfig) -> Unit,
    private val onResumeDraftSave: (() -> Unit)?,
    private val onSkipDraftSave: (() -> Unit)?,
    private val showToast: (String, Int) -> Unit
) {

    fun handleImportPreset(uri: Uri?, currentPendingImages: List<Uri>) {
        if (uri == null) { onResumeDraftSave?.invoke(); return }
        dialogState.isImportingPreset = true; onSkipDraftSave?.invoke()
        scope.launch {
            try {
                val importResult = withContext(Dispatchers.IO) { presetManager.importPreset(uri) }
                when (importResult.status) {
                    PresetManager.ImportStatus.SUCCESS, PresetManager.ImportStatus.ALREADY_EXISTS -> applyImportedPreset(importResult, currentPendingImages)
                    PresetManager.ImportStatus.NEEDS_OVERWRITE -> { dialogState.pendingImportResult = importResult; dialogState.showImportOverwriteDialog = true }
                }
            } catch (e: Exception) { showToast("导入预设失败: ${e.message}", android.widget.Toast.LENGTH_LONG) }
            finally { dialogState.isImportingPreset = false; onResumeDraftSave?.invoke() }
        }
    }

    fun handleExternalTddsFile(uri: Uri, onSkipDraftRestore: () -> Unit) {
        dialogState.isImportingPreset = true; onSkipDraftRestore(); onSkipDraftSave?.invoke()
        scope.launch {
            try {
                val importResult = withContext(Dispatchers.IO) { presetManager.importPreset(uri) }
                when (importResult.status) {
                    PresetManager.ImportStatus.SUCCESS, PresetManager.ImportStatus.ALREADY_EXISTS -> applyImportedPreset(importResult, emptyList())
                    PresetManager.ImportStatus.NEEDS_OVERWRITE -> { dialogState.pendingImportResult = importResult; dialogState.showImportOverwriteDialog = true }
                }
            } catch (e: Exception) { showToast("导入预设失败: ${e.message}", android.widget.Toast.LENGTH_LONG) }
            finally { dialogState.isImportingPreset = false; onResumeDraftSave?.invoke() }
        }
    }

    private suspend fun applyImportedPreset(importResult: PresetManager.ImportResult, currentPendingImages: List<Uri>) {
        val result = withContext(Dispatchers.IO) { presetManager.applyPreset(importResult.presetFile) }
        tiers.clear(); tiers.addAll(result.tiers.map { TierItem(it.label, try { Color(android.graphics.Color.parseColor("#${it.color}")) } catch (e: Exception) { Color.Gray }) })
        tierImages.clear(); tierImages.addAll(result.tierImages.map { TierImage(id = it.id, tierLabel = it.tierLabel, uri = it.uri, name = it.name, badgeUri = it.badgeUri, badgeUri2 = it.badgeUri2, badgeUri3 = it.badgeUri3, audioUri = it.audioUri, originalUri = it.originalUri, cropPositionX = it.cropPositionX, cropPositionY = it.cropPositionY, cropScale = it.cropScale, isCropped = it.isCropped, cropRatio = it.cropRatio, useCustomCrop = it.useCustomCrop, customCropWidth = it.customCropWidth, customCropHeight = it.customCropHeight) })
        onPendingImagesChange(result.pendingImages); onTitleChange(importResult.presetData.title); onAuthorChange(importResult.presetData.author)
        settingsService.clearCropSettings(); settingsService.customCropWidth = result.customCropWidth; settingsService.customCropHeight = result.customCropHeight; settingsService.useCustomCropSize = result.useCustomCropSize; settingsService.cropRatio = result.cropRatio
        onVideoConfigChange(result.videoConfig)
        onTierRowPositionsReset()
        val message = when (importResult.status) { PresetManager.ImportStatus.ALREADY_EXISTS -> "预设已加载"; else -> "导入预设成功" }
        showToast(message, android.widget.Toast.LENGTH_SHORT)
    }

    fun handleExportPreset(uri: Uri?, presetName: String, tierListTitle: String, authorName: String, currentPendingImages: List<Uri>, videoConfig: VideoGenerationConfig) {
        if (uri == null) { onResumeDraftSave?.invoke(); return }
        dialogState.isExportingPreset = true; onSkipDraftSave?.invoke()
        scope.launch {
            try {
                yield()
                val presetData = withContext(Dispatchers.IO) { presetManager.createPresetData(title = tierListTitle, author = authorName, tiers = tiers, tierImages = tierImages, pendingImages = currentPendingImages, cropPositionX = settingsService.cropPositionX, cropPositionY = settingsService.cropPositionY, customCropWidth = settingsService.customCropWidth, customCropHeight = settingsService.customCropHeight, useCustomCropSize = settingsService.useCustomCropSize, cropRatio = settingsService.cropRatio, videoConfig = videoConfig) }
                val outputFile = File(context.cacheDir, "$presetName.tdds")
                withContext(Dispatchers.IO) {
                    presetManager.exportPreset(presetData, outputFile)
                    context.contentResolver.openOutputStream(uri, "rwt")?.use { output -> java.io.FileInputStream(outputFile).use { input -> input.copyTo(output) } }
                    outputFile.delete()
                }
                showToast("导出预设成功", android.widget.Toast.LENGTH_SHORT)
            } catch (e: Exception) { showToast("导出预设失败: ${e.message}", android.widget.Toast.LENGTH_LONG) }
            finally { dialogState.isExportingPreset = false; onResumeDraftSave?.invoke() }
        }
    }

    fun checkAndLoadDraft(skipDraftRestore: Boolean, onDraftFound: (PresetData) -> Unit) {
        if (skipDraftRestore) return
        scope.launch {
            try {
                val draftExists = withContext(Dispatchers.IO) { presetManager.hasDraft() }
                if (draftExists) {
                    val draftData = withContext(Dispatchers.IO) { presetManager.readDraftConfig() }
                    if (draftData != null) onDraftFound(draftData)
                }
            } catch (e: Exception) { }
        }
    }

    fun restoreDraft(draftData: PresetData, onLoadingStateChange: (Boolean) -> Unit) {
        onLoadingStateChange(true)
        scope.launch {
            try {
                val draftFile = withContext(Dispatchers.IO) { presetManager.obtainDraftFile() }
                if (draftFile != null) {
                    val result = withContext(Dispatchers.IO) { presetManager.restoreDraft(draftFile) }
                    tiers.clear(); tiers.addAll(result.tiers.map { TierItem(it.label, try { Color(android.graphics.Color.parseColor("#${it.color}")) } catch (e: Exception) { Color.Gray }) })
                    tierImages.clear(); tierImages.addAll(result.tierImages.map { TierImage(id = it.id, tierLabel = it.tierLabel, uri = it.uri, name = it.name, badgeUri = it.badgeUri, badgeUri2 = it.badgeUri2, badgeUri3 = it.badgeUri3, audioUri = it.audioUri, originalUri = it.originalUri, cropPositionX = it.cropPositionX, cropPositionY = it.cropPositionY, cropScale = it.cropScale, isCropped = it.isCropped, cropRatio = it.cropRatio, useCustomCrop = it.useCustomCrop, customCropWidth = it.customCropWidth, customCropHeight = it.customCropHeight) })
                    onPendingImagesChange(result.pendingImages); onTitleChange(draftData.title); onAuthorChange(draftData.author)
                    settingsService.clearCropSettings(); settingsService.customCropWidth = result.customCropWidth; settingsService.customCropHeight = result.customCropHeight; settingsService.useCustomCropSize = result.useCustomCropSize; settingsService.cropRatio = result.cropRatio
                    onVideoConfigChange(result.videoConfig)
                    onTierRowPositionsReset(); showToast("草稿已恢复", android.widget.Toast.LENGTH_SHORT)
                } else throw IllegalStateException("加载草稿失败")
            } catch (e: Exception) { showToast("恢复草稿失败", android.widget.Toast.LENGTH_SHORT) }
            finally { onLoadingStateChange(false) }
        }
    }

    suspend fun saveDraft(tierListTitle: String, authorName: String, currentPendingImages: List<Uri>, videoConfig: VideoGenerationConfig) {
        try {
            val presetData = withContext(Dispatchers.IO) {
                presetManager.createPresetData(
                    title = tierListTitle, author = authorName, tiers = tiers,
                    tierImages = tierImages, pendingImages = currentPendingImages,
                    cropPositionX = settingsService.cropPositionX,
                    cropPositionY = settingsService.cropPositionY,
                    customCropWidth = settingsService.customCropWidth,
                    customCropHeight = settingsService.customCropHeight,
                    useCustomCropSize = settingsService.useCustomCropSize,
                    cropRatio = settingsService.cropRatio,
                    videoConfig = videoConfig
                )
            }
            withContext(Dispatchers.IO) { presetManager.saveDraft(presetData) }
        } catch (_: Exception) { }
    }

    fun handleExternalPresetImport(uri: Uri, onLoadingStateChange: (Boolean) -> Unit) {
        dialogState.isImportingPreset = true; onSkipDraftSave?.invoke()
        scope.launch {
            try {
                val importResult = withContext(Dispatchers.IO) { presetManager.importPreset(uri) }
                when (importResult.status) {
                    PresetManager.ImportStatus.SUCCESS, PresetManager.ImportStatus.ALREADY_EXISTS -> { applyImportedPreset(importResult, emptyList()); presetManager.cleanupDraftOnly(); dialogState.isImportingPreset = false; onResumeDraftSave?.invoke(); onLoadingStateChange(false) }
                    PresetManager.ImportStatus.NEEDS_OVERWRITE -> { dialogState.pendingImportResult = importResult; dialogState.showImportOverwriteDialog = true; dialogState.isImportingPreset = false; onResumeDraftSave?.invoke(); onLoadingStateChange(false) }
                }
            } catch (e: Exception) { dialogState.isImportingPreset = false; onResumeDraftSave?.invoke(); onLoadingStateChange(false); showToast("导入预设失败: ${e.message}", android.widget.Toast.LENGTH_SHORT) }
        }
    }
}

@Composable
fun rememberPresetOperationHandler(scope: CoroutineScope, dialogState: DialogState, presetManager: PresetManager, settingsService: SettingsService, tiers: SnapshotStateList<TierItem>, tierImages: SnapshotStateList<TierImage>, onPendingImagesChange: (List<Uri>) -> Unit, onTitleChange: (String) -> Unit, onAuthorChange: (String) -> Unit, onTierRowPositionsReset: () -> Unit, onVideoConfigChange: (VideoGenerationConfig) -> Unit, onResumeDraftSave: (() -> Unit)?, onSkipDraftSave: (() -> Unit)?, showToast: (String, Int) -> Unit): PresetOperationHandler {
    val context = LocalContext.current
    return remember(scope, dialogState, presetManager) { PresetOperationHandler(context = context, scope = scope, dialogState = dialogState, presetManager = presetManager, settingsService = settingsService, tiers = tiers, tierImages = tierImages, onPendingImagesChange = onPendingImagesChange, onTitleChange = onTitleChange, onAuthorChange = onAuthorChange, onTierRowPositionsReset = onTierRowPositionsReset, onVideoConfigChange = onVideoConfigChange, onResumeDraftSave = onResumeDraftSave, onSkipDraftSave = onSkipDraftSave, showToast = showToast) }
}
