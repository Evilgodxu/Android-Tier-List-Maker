package com.tdds.jh.screens.tierlist

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tdds.jh.model.tierlist.TierImage
import com.tdds.jh.model.tierlist.TierItem
import com.tdds.jh.data.tierlist.PackageItem
import com.tdds.jh.data.tierlist.PresetManager
import com.tdds.jh.model.tierlist.PresetOperation

/**
 * 对话框状态管理类
 * 集中管理所有对话框的显示状态和关联数据
 */
@Stable
class DialogState {
    // ==================== 设置菜单对话框 ====================
    var showSettingsMenu by mutableStateOf(false)
    var showProgramSettingsDialog by mutableStateOf(false)
    var showResourceManageDialog by mutableStateOf(false)

    // ==================== 信息对话框 ====================
    var showAboutDialog by mutableStateOf(false)
    var showInstructionsDialog by mutableStateOf(false)

    // ==================== 图包管理对话框 ====================
    var showManagePackagesDialog by mutableStateOf(false)
    var showPackageConfirmDialog by mutableStateOf(false)
    var showImportPackageDialog by mutableStateOf(false)
    var selectedPackage by mutableStateOf<PackageItem.Imported?>(null)
    var selectedPackageImageCount by mutableIntStateOf(0)

    // ==================== 标题/作者编辑对话框 ====================
    var showEditTitleDialog by mutableStateOf(false)
    var showEditAuthorDialog by mutableStateOf(false)

    // ==================== 语言选择对话框 ====================
    var showLanguageDialog by mutableStateOf(false)

    // ==================== 层级编辑对话框 ====================
    var editingTier by mutableStateOf<TierItem?>(null)
    var showEditNameDialog by mutableStateOf(false)
    var showColorPickerDialog by mutableStateOf(false)

    // ==================== 图片操作对话框 ====================
    var selectedImageForAction by mutableStateOf<TierImage?>(null)
    var showImageActionDialog by mutableStateOf(false)
    var showImageViewDialog by mutableStateOf(false)
    var showCropDialog by mutableStateOf(false)
    var showMoveImageDialog by mutableStateOf(false)
    var showEditImageNameDialog by mutableStateOf(false)

    // ==================== 小图标设置对话框 ====================
    var imageForBadge by mutableStateOf<TierImage?>(null)
    var showSetBadgeDialog by mutableStateOf(false)
    var badgeSelectionTarget by mutableIntStateOf(0)
    var badgeDialogRefreshKey by mutableIntStateOf(0)

    // ==================== 预设管理对话框 ====================
    var showManagePresetsDialog by mutableStateOf(false)
    var showPresetNameDialog by mutableStateOf(false)
    var showPresetListDialog by mutableStateOf(false)
    var showPresetOverwriteConfirmDialog by mutableStateOf(false)
    var showImportOverwriteDialog by mutableStateOf(false)
    var presetOperation by mutableStateOf<PresetOperation?>(null)
    var pendingPresetName by mutableStateOf("")
    var pendingImportResult by mutableStateOf<PresetManager.ImportResult?>(null)
    // 导入预设时选择新建预设的标志，用于区分保存和导入流程
    var isImportCreatingNewPreset by mutableStateOf(false)

    // ==================== 图片预览对话框 ====================
    var showPreviewDialog by mutableStateOf(false)
    var previewBitmap by mutableStateOf<Bitmap?>(null)
    var previewIsDarkTheme by mutableStateOf(false)

    // ==================== 加载状态对话框 ====================
    var isImportingPreset by mutableStateOf(false)
    var isExportingPreset by mutableStateOf(false)
    var isSavingPreset by mutableStateOf(false)
    var isExportingPackage by mutableStateOf(false)
    var isImportingPackage by mutableStateOf(false)
    var isSavingChart by mutableStateOf(false)
    var isSharingChart by mutableStateOf(false)

    // ==================== 防重复点击状态 ====================
    var isBadgePickerLaunching by mutableStateOf(false)
    var isImagePickerLaunching by mutableStateOf(false)
    var isResetting by mutableStateOf(false)

    // ==================== 图片替换状态 ====================
    var imageToReplace by mutableStateOf<TierImage?>(null)

    // ==================== 图包导出状态 ====================
    var packageToExport by mutableStateOf<com.tdds.jh.data.tierlist.PackageItem.Imported?>(null)

    // ==================== 外部图包导入状态 ====================
    var showExternalPackagePasswordDialog by mutableStateOf(false)
    var externalPackageUri by mutableStateOf<Uri?>(null)
    var externalPackageFileName by mutableStateOf("")
    var externalPackagePassword by mutableStateOf<String?>(null)
    var externalPackagePasswordError by mutableStateOf(false)

    // ==================== 草稿恢复对话框 ====================
    var showDraftRestoreDialog by mutableStateOf(false)
    var showDraftLoadingDialog by mutableStateOf(false)

    // ==================== 快速方法 ====================

    /**
     * 关闭所有对话框
     */
    fun dismissAll() {
        showSettingsMenu = false
        showProgramSettingsDialog = false
        showResourceManageDialog = false
        showAboutDialog = false
        showInstructionsDialog = false
        showManagePackagesDialog = false
        showPackageConfirmDialog = false
        showImportPackageDialog = false
        showEditTitleDialog = false
        showEditAuthorDialog = false
        showLanguageDialog = false
        showEditNameDialog = false
        showColorPickerDialog = false
        showImageActionDialog = false
        showImageViewDialog = false
        showCropDialog = false
        showMoveImageDialog = false
        showEditImageNameDialog = false
        showSetBadgeDialog = false
        showManagePresetsDialog = false
        showPresetNameDialog = false
        showPresetListDialog = false
        showPresetOverwriteConfirmDialog = false
        showImportOverwriteDialog = false
        showPreviewDialog = false
        isImportCreatingNewPreset = false
        showExternalPackagePasswordDialog = false
        externalPackageUri = null
        externalPackageFileName = ""
        externalPackagePassword = null
        externalPackagePasswordError = false
    }

    /**
     * 显示图片操作对话框
     */
    fun showImageAction(image: TierImage) {
        selectedImageForAction = image
        showImageActionDialog = true
    }

    /**
     * 关闭图片操作相关对话框并清理状态
     */
    fun dismissImageAction() {
        showImageActionDialog = false
        showImageViewDialog = false
        showCropDialog = false
        showMoveImageDialog = false
        showEditImageNameDialog = false
        selectedImageForAction = null
    }

    /**
     * 显示层级编辑对话框
     */
    fun showTierEdit(tier: TierItem) {
        editingTier = tier
        showEditNameDialog = true
    }

    /**
     * 显示层级颜色选择器
     */
    fun showTierColorPicker(tier: TierItem) {
        editingTier = tier
        showColorPickerDialog = true
    }

    /**
     * 显示小图标设置对话框
     */
    fun showBadgeSettings(image: TierImage) {
        imageForBadge = image
        showSetBadgeDialog = true
    }

    /**
     * 显示预设覆盖确认对话框
     */
    fun showPresetOverwrite(name: String, operation: PresetOperation) {
        pendingPresetName = name
        presetOperation = operation
        showPresetOverwriteConfirmDialog = true
    }

    /**
     * 检查是否有加载中的操作
     */
    fun isLoading(): Boolean = isImportingPreset || isExportingPreset ||
            isSavingPreset || isExportingPackage || isImportingPackage

    /**
     * 获取加载提示文本的资源ID
     */
    fun getLoadingMessageResId(): Int {
        return when {
            isImportingPreset -> com.tdds.jh.R.string.loading_resources
            isExportingPreset -> com.tdds.jh.R.string.loading_resources
            isSavingPreset -> com.tdds.jh.R.string.loading_resources
            isExportingPackage -> com.tdds.jh.R.string.loading_resources
            isImportingPackage -> com.tdds.jh.R.string.loading_resources
            else -> com.tdds.jh.R.string.loading_resources
        }
    }
}
