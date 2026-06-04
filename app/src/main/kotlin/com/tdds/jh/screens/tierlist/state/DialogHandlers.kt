package com.tdds.jh.screens.tierlist.state

import android.content.Context
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.tdds.jh.data.manager.PresetManager
import com.tdds.jh.data.bitmap.TierImage
import com.tdds.jh.data.bitmap.TierItem
import com.tdds.jh.data.resource.PackageItem
import com.tdds.jh.data.resource.PackageManager
import com.tdds.jh.screens.tierlist.model.PresetOperation
import com.tdds.jh.screens.tierlist.service.SettingsService
import com.tdds.jh.screens.tierlist.utils.ImageOperationUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 对话框事件处理器
 * 集中处理所有对话框的用户交互事件
 */
@Stable
class DialogHandlers(
    private val context: Context,
    private val dialogState: DialogState,
    private val scope: CoroutineScope,
    private val settingsService: SettingsService,
    private val presetManager: PresetManager,
    private val tierImages: SnapshotStateList<TierImage>,
    private val onTierImagesChange: () -> Unit,
    private val onPendingImagesChange: (List<Uri>) -> Unit,
    private val onSkipDraftSave: (() -> Unit)?,
    private val onResumeDraftSave: (() -> Unit)?,
    private val imagePicker: ActivityResultLauncher<PickVisualMediaRequest>,
    private val replaceImagePicker: ActivityResultLauncher<PickVisualMediaRequest>,
    private val launchBadgePicker: (Int) -> Unit,
    private val launchBadgePickerForAdding: () -> Unit,
    private val deleteBadge: (Int) -> Unit,
    private val deleteBadgeFile: (Uri, PresetManager) -> Boolean
) {

    // ==================== 图片操作对话框处理器 ====================

    fun onImageActionDismiss() {
        dialogState.showImageActionDialog = false
    }

    fun onImageSetBadge() {
        dialogState.imageForBadge = dialogState.selectedImageForAction
        dialogState.showImageActionDialog = false
        dialogState.showSetBadgeDialog = true
    }

    fun onImageReplace() {
        dialogState.imageToReplace = dialogState.selectedImageForAction
        dialogState.showImageActionDialog = false
        dialogState.selectedImageForAction = null
        replaceImagePicker.launch(PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    fun onImageMove() {
        dialogState.showImageActionDialog = false
        dialogState.showMoveImageDialog = true
    }

    fun onImageRename() {
        dialogState.showImageActionDialog = false
        dialogState.showEditImageNameDialog = true
    }

    fun onImageView() {
        dialogState.showImageActionDialog = false
        dialogState.showImageViewDialog = true
    }

    fun onImageCrop() {
        dialogState.showImageActionDialog = false
        dialogState.showCropDialog = true
    }

    // ==================== 图片查看对话框处理器 ====================

    fun onImageViewDismiss() {
        dialogState.showImageViewDialog = false
        dialogState.selectedImageForAction = null
    }

    // ==================== 图片裁剪对话框处理器 ====================

    fun onCropDismiss() {
        dialogState.showCropDialog = false
        dialogState.selectedImageForAction = null
    }

    // ==================== 移动图片对话框处理器 ====================

    fun onMoveImageDismiss() {
        dialogState.showMoveImageDialog = false
        dialogState.selectedImageForAction = null
    }

    fun onMoveToTier(targetTierLabel: String) {
        dialogState.selectedImageForAction?.let { image ->
            ImageOperationUtils.moveImageToTier(
                tierImages = tierImages,
                imageId = image.id,
                targetTierLabel = targetTierLabel
            )
            onTierImagesChange()
        }
        onMoveImageDismiss()
    }

    fun onMoveToFirst() {
        dialogState.selectedImageForAction?.let { image ->
            ImageOperationUtils.moveImageToFirst(
                tierImages = tierImages,
                imageId = image.id
            )
            onTierImagesChange()
        }
        onMoveImageDismiss()
    }

    fun onMoveToLast() {
        dialogState.selectedImageForAction?.let { image ->
            ImageOperationUtils.moveImageToLast(
                tierImages = tierImages,
                imageId = image.id
            )
            onTierImagesChange()
        }
        onMoveImageDismiss()
    }

    fun onMoveLeft() {
        dialogState.selectedImageForAction?.let { image ->
            ImageOperationUtils.moveImageLeft(
                tierImages = tierImages,
                imageId = image.id
            )
            onTierImagesChange()
        }
        onMoveImageDismiss()
    }

    fun onMoveRight() {
        dialogState.selectedImageForAction?.let { image ->
            ImageOperationUtils.moveImageRight(
                tierImages = tierImages,
                imageId = image.id
            )
            onTierImagesChange()
        }
        onMoveImageDismiss()
    }

    // ==================== 编辑图片名称对话框处理器 ====================

    fun onEditImageNameDismiss() {
        dialogState.showEditImageNameDialog = false
        dialogState.selectedImageForAction = null
    }

    fun onEditImageNameConfirm(newName: String) {
        dialogState.selectedImageForAction?.let { image ->
            val index = tierImages.indexOfFirst { it.id == image.id }
            if (index != -1) {
                tierImages[index] = tierImages[index].copy(name = newName)
                onTierImagesChange()
            }
        }
        onEditImageNameDismiss()
    }

    // ==================== 小图标设置对话框处理器 ====================

    fun onSetBadgeDismiss() {
        dialogState.showSetBadgeDialog = false
        dialogState.imageForBadge = null
        dialogState.badgeSelectionTarget = 0
    }

    fun onSelectBadge1() = launchBadgePicker(1)
    fun onSelectBadge2() = launchBadgePicker(2)
    fun onSelectBadge3() = launchBadgePicker(3)

    fun onDeleteBadge1() = deleteBadge(1)
    fun onDeleteBadge2() = deleteBadge(2)
    fun onDeleteBadge3() = deleteBadge(3)

    fun onAddBadge() = launchBadgePickerForAdding()

    fun onSelectBadgeFromPreview(badgeUri: Uri, slot: Int) {
        dialogState.imageForBadge?.let { image ->
            val index = tierImages.indexOfFirst { it.id == image.id }
            if (index != -1) {
                tierImages[index] = when (slot) {
                    1 -> tierImages[index].copy(badgeUri = badgeUri)
                    2 -> tierImages[index].copy(badgeUri2 = badgeUri)
                    else -> tierImages[index].copy(badgeUri3 = badgeUri)
                }
                onTierImagesChange()
            }
        }
    }

    fun onDeleteBadgeFromPreview(badgeUri: Uri): Boolean {
        val isInUse = tierImages.any { image ->
            image.badgeUri == badgeUri ||
                    image.badgeUri2 == badgeUri ||
                    image.badgeUri3 == badgeUri
        }
        return if (isInUse) {
            false
        } else {
            deleteBadgeFile(badgeUri, presetManager)
        }
    }

    // ==================== 编辑标题对话框处理器 ====================

    fun onEditTitleDismiss() {
        dialogState.showEditTitleDialog = false
    }

    fun onEditTitleConfirm(newTitle: String, onTitleChange: (String) -> Unit) {
        onTitleChange(newTitle)
        dialogState.showEditTitleDialog = false
    }

    // ==================== 编辑作者对话框处理器 ====================

    fun onEditAuthorDismiss() {
        dialogState.showEditAuthorDialog = false
    }

    fun onEditAuthorConfirm(newAuthor: String, onAuthorChange: (String) -> Unit) {
        onAuthorChange(newAuthor)
        dialogState.showEditAuthorDialog = false
    }

    // ==================== 设置菜单对话框处理器 ====================

    fun onSettingsMenuDismiss() {
        dialogState.showSettingsMenu = false
    }

    fun onShowInstructions() {
        dialogState.showSettingsMenu = false
        dialogState.showInstructionsDialog = true
    }

    fun onShowFeedback() {
        dialogState.showSettingsMenu = false
        dialogState.showAboutDialog = true
    }

    fun onImagePackage() {
        dialogState.showSettingsMenu = false
        dialogState.showManagePackagesDialog = true
    }

    fun onShowProgramSettings() {
        dialogState.showSettingsMenu = false
        dialogState.showProgramSettingsDialog = true
    }

    fun onManagePresets() {
        dialogState.showSettingsMenu = false
        dialogState.showManagePresetsDialog = true
    }

    // ==================== 程序设置对话框处理器 ====================

    fun onProgramSettingsDismiss() {
        dialogState.showProgramSettingsDialog = false
    }

    fun onToggleDisableClickAdd(newValue: Boolean, onValueChange: (Boolean) -> Unit) {
        onValueChange(newValue)
        settingsService.disableClickAdd = newValue
    }

    fun onFloatOffsetXChange(newValue: Float, onValueChange: (Float) -> Unit) {
        onValueChange(newValue)
        settingsService.floatOffsetX = newValue
    }

    fun onFloatOffsetYChange(newValue: Float, onValueChange: (Float) -> Unit) {
        onValueChange(newValue)
        settingsService.floatOffsetY = newValue
    }

    fun onToggleExternalBadge(newValue: Boolean, onValueChange: (Boolean) -> Unit) {
        onValueChange(newValue)
        settingsService.externalBadgeEnabled = newValue
    }

    fun onToggleFollowSystemTheme(newValue: Boolean, onValueChange: ((Boolean) -> Unit)?) {
        onValueChange?.invoke(newValue)
    }

    fun onShowLanguageDialog() {
        dialogState.showProgramSettingsDialog = false
        dialogState.showLanguageDialog = true
    }

    fun onToggleDisableCustomFont(newValue: Boolean, onValueChange: ((Boolean) -> Unit)?) {
        onValueChange?.invoke(newValue)
    }

    fun onToggleNameBelowImage(newValue: Boolean, onValueChange: (Boolean) -> Unit) {
        onValueChange(newValue)
        settingsService.nameBelowImage = newValue
    }

    // ==================== 资源管理对话框处理器 ====================

    fun onResourceManageDismiss() {
        dialogState.showResourceManageDialog = false
    }

    fun onResetTierMaster(
        defaultTiers: List<TierItem>,
        onTitleReset: () -> Unit,
        onAuthorReset: () -> Unit,
        onTiersReset: () -> Unit,
        onTierImagesReset: () -> Unit,
        onTierRowPositionsReset: () -> Unit,
        onPendingImagesReset: (List<Uri>) -> Unit
    ) {
        onTitleReset()
        onAuthorReset()
        onTiersReset()
        tierImages.clear()
        onTierImagesReset()
        onTierRowPositionsReset()
        onPendingImagesReset(emptyList())
        settingsService.clearCropSettings()
    }

    // ==================== 图包管理对话框处理器 ====================

    fun onManagePackagesDismiss() {
        dialogState.showManagePackagesDialog = false
    }

    fun onImportPackage() {
        dialogState.showImportPackageDialog = true
    }

    fun onPackageSelected(packageItem: PackageItem.Imported, onImageCountCalculated: (Int) -> Unit) {
        dialogState.selectedPackage = packageItem
        scope.launch {
            val count = PackageManager.countImagesInImportedPackage(packageItem.file)
            onImageCountCalculated(count)
            dialogState.showManagePackagesDialog = false
            dialogState.showPackageConfirmDialog = true
        }
    }

    fun onExportPackage(packageItem: PackageItem.Imported, onExport: () -> Unit) {
        onSkipDraftSave?.invoke()
        onExport()
    }

    // ==================== 图包确认对话框处理器 ====================

    fun onPackageConfirmDismiss() {
        if (!dialogState.isImportingPackage) {
            dialogState.showPackageConfirmDialog = false
            dialogState.selectedPackage = null
        }
    }

    // ==================== 导入图包对话框处理器 ====================

    fun onImportPackageDismiss() {
        dialogState.showImportPackageDialog = false
    }

    // ==================== 语言选择对话框处理器 ====================

    fun onLanguageDialogDismiss() {
        dialogState.showLanguageDialog = false
    }

    fun onLanguageSelected(language: String, currentLanguage: String, onLanguageChange: (String) -> Unit) {
        if (language != currentLanguage) {
            onLanguageChange(language)
        }
    }

    // ==================== 使用说明对话框处理器 ====================

    fun onInstructionsDismiss() {
        dialogState.showInstructionsDialog = false
    }

    // ==================== 关于对话框处理器 ====================

    fun onAboutDismiss() {
        dialogState.showAboutDialog = false
    }

    // ==================== 管理预设对话框处理器 ====================

    fun onManagePresetsDismiss() {
        dialogState.showManagePresetsDialog = false
    }

    fun onExportPreset(defaultTitle: String, currentTitle: String, onLaunchExport: () -> Unit) {
        dialogState.showManagePresetsDialog = false
        dialogState.presetOperation = PresetOperation.EXPORT
        if (currentTitle == defaultTitle) {
            dialogState.showPresetNameDialog = true
        } else {
            dialogState.pendingPresetName = currentTitle
            onSkipDraftSave?.invoke()
            onLaunchExport()
        }
    }

    fun onImportPreset(onLaunchImport: () -> Unit) {
        dialogState.showManagePresetsDialog = false
        onSkipDraftSave?.invoke()
        onLaunchImport()
    }

    fun onSavePreset(defaultTitle: String, currentTitle: String) {
        dialogState.showManagePresetsDialog = false
        dialogState.presetOperation = PresetOperation.SAVE
        if (currentTitle == defaultTitle) {
            dialogState.showPresetNameDialog = true
        } else {
            dialogState.pendingPresetName = currentTitle
            dialogState.showPresetOverwriteConfirmDialog = true
        }
    }

    fun onManagePresetList() {
        dialogState.showManagePresetsDialog = false
        dialogState.showPresetListDialog = true
    }

    // ==================== 预设名称对话框处理器 ====================

    fun onPresetNameDialogDismiss() {
        dialogState.showPresetNameDialog = false
        // 如果是导入时新建预设流程被取消，清理相关状态
        if (dialogState.isImportCreatingNewPreset) {
            dialogState.isImportCreatingNewPreset = false
            dialogState.pendingImportResult?.presetFile?.delete()
            dialogState.pendingImportResult = null
        }
    }

    fun onPresetNameConfirm(name: String, onLaunchExport: () -> Unit) {
        dialogState.showPresetNameDialog = false
        dialogState.pendingPresetName = name
        when (dialogState.presetOperation) {
            PresetOperation.EXPORT -> {
                onSkipDraftSave?.invoke()
                onLaunchExport()
            }
            PresetOperation.SAVE -> {
                dialogState.showPresetOverwriteConfirmDialog = true
            }
            else -> {}
        }
    }

    // ==================== 预设覆盖确认对话框处理器 ====================

    fun onPresetOverwriteConfirmDismiss() {
        dialogState.showPresetOverwriteConfirmDialog = false
    }

    fun onCreateNewPreset() {
        dialogState.showPresetOverwriteConfirmDialog = false
        dialogState.showPresetNameDialog = true
    }

    // ==================== 导入预设覆盖确认对话框处理器 ====================

    fun onImportOverwriteDismiss() {
        dialogState.showImportOverwriteDialog = false
        dialogState.pendingImportResult?.presetFile?.delete()
        dialogState.pendingImportResult = null
        dialogState.isImportCreatingNewPreset = false
    }

    fun onImportCreateNewPreset() {
        dialogState.showImportOverwriteDialog = false
        dialogState.isImportCreatingNewPreset = true
        dialogState.showPresetNameDialog = true
    }

    // ==================== 预设列表对话框处理器 ====================

    fun onPresetListDismiss() {
        dialogState.showPresetListDialog = false
    }

    // ==================== 图片预览对话框处理器 ====================

    fun onPreviewDismiss() {
        dialogState.showPreviewDialog = false
        dialogState.previewBitmap = null
    }

    fun onPreviewSave(onSave: suspend () -> Unit) {
        dialogState.isSavingChart = true
        scope.launch {
            try {
                onSave()
                dialogState.showPreviewDialog = false
                dialogState.previewBitmap = null
            } finally {
                dialogState.isSavingChart = false
            }
        }
    }

    fun onPreviewShare(onShare: suspend () -> Unit) {
        dialogState.isSharingChart = true
        scope.launch {
            try {
                onShare()
            } finally {
                dialogState.isSharingChart = false
            }
        }
    }

    fun onPreviewThemeToggle(onToggle: suspend () -> Unit) {
        scope.launch {
            onToggle()
        }
    }
}
