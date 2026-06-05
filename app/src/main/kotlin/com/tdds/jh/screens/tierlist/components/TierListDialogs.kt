package com.tdds.jh.screens.tierlist.components

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.tdds.jh.data.tierlist.PresetData
import com.tdds.jh.data.tierlist.PresetManager
import com.tdds.jh.R
import com.tdds.jh.model.tierlist.TierImage
import com.tdds.jh.model.tierlist.TierItem
import com.tdds.jh.data.tierlist.generateTierListBitmap
import com.tdds.jh.data.tierlist.PackageItem
import com.tdds.jh.data.tierlist.ImportTarget
import com.tdds.jh.data.tierlist.PackageManager
import com.tdds.jh.data.tierlist.PresetInfo
import com.tdds.jh.screens.tierlist.components.dialogs.DonateDialog
import com.tdds.jh.screens.tierlist.components.dialogs.PreviewDialog
import com.tdds.jh.screens.tierlist.components.dialogs.ColorPickerDialog
import com.tdds.jh.screens.tierlist.components.dialogs.EditAuthorDialog
import com.tdds.jh.screens.tierlist.components.dialogs.EditTierNameDialog
import com.tdds.jh.screens.tierlist.components.dialogs.EditTitleDialog
import com.tdds.jh.screens.tierlist.components.dialogs.LanguageSelectionDialog
import com.tdds.jh.screens.tierlist.components.dialogs.ProgramSettingsDialog
import com.tdds.jh.ui.theme.LocalExtendedColors
import com.tdds.jh.screens.tierlist.service.SettingsService
import com.tdds.jh.screens.tierlist.DialogHandlers
import com.tdds.jh.screens.tierlist.DialogState
import com.tdds.jh.screens.tierlist.logic.utils.ImageOperationUtils
import com.tdds.jh.screens.tierlist.logic.utils.saveBitmapToGallery
import com.tdds.jh.screens.tierlist.logic.utils.shareBitmap
import com.tdds.jh.ui.toast.showToastWithoutIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 层级列表对话框集合
 * 集中管理所有对话框的渲染
 *
 * @param dialogState 对话框状态
 * @param handlers 对话框事件处理器
 * @param context 上下文
 * @param scope 协程作用域
 * @param settingsService 设置服务
 * @param presetManager 预设管理器
 * @param tierImages 层级图片列表
 * @param tiers 层级列表
 * @param tierListTitle 标题
 * @param authorName 作者名
 * @param pendingImages 待分级图片
 * @param defaultTiers 默认层级
 * @param tierRowPositions 层级位置
 * @param disableClickAdd 禁用点击添加
 * @param floatOffsetX 浮动偏移X
 * @param floatOffsetY 浮动偏移Y
 * @param externalBadgeEnabled 外置小图
 * @param followSystemTheme 跟随系统主题
 * @param disableCustomFont 禁用自定义字体
 * @param nameBelowImage 名称在图片下方
 * @param isDarkTheme 深色主题
 * @param currentLanguage 当前语言
 * @param onTitleChange 标题变更回调
 * @param onAuthorChange 作者变更回调
 * @param onTiersChange 层级变更回调
 * @param onTierImagesChange 层级图片变更回调
 * @param onPendingImagesChange 待分级图片变更回调
 * @param onTierRowPositionsChange 层级位置变更回调
 * @param onDisableClickAddChange 禁用点击添加变更回调
 * @param onFloatOffsetXChange 浮动偏移X变更回调
 * @param onFloatOffsetYChange 浮动偏移Y变更回调
 * @param onExternalBadgeChange 外置小图变更回调
 * @param onFollowSystemThemeChange 跟随系统主题变更回调
 * @param onDisableCustomFontChange 禁用自定义字体变更回调
 * @param onNameBelowImageChange 名称在图片下方变更回调
 * @param onLanguageChange 语言变更回调
 * @param onSkipDraftSave 跳过草稿保存回调
 * @param onResumeDraftSave 恢复草稿保存回调
 * @param presetExportLauncher 预设导出启动器
 * @param packageExportLauncher 图包导出启动器
 * @param presetFilePicker 预设文件选择器
 */
@Composable
fun TierListDialogs(
    dialogState: DialogState,
    handlers: DialogHandlers,
    context: Context,
    scope: CoroutineScope,
    settingsService: SettingsService,
    presetManager: PresetManager,
    tierImages: MutableList<TierImage>,
    tiers: MutableList<TierItem>,
    tierListTitle: String,
    authorName: String,
    pendingImages: List<Uri>,
    defaultTiers: List<TierItem>,
    tierRowPositions: Map<String, android.graphics.Rect>,
    disableClickAdd: Boolean,
    floatOffsetX: Float,
    floatOffsetY: Float,
    externalBadgeEnabled: Boolean,
    followSystemTheme: Boolean,
    disableCustomFont: Boolean,
    nameBelowImage: Boolean,
    isDarkTheme: Boolean,
    currentLanguage: String,
    onTitleChange: (String) -> Unit,
    onAuthorChange: (String) -> Unit,
    onTiersChange: () -> Unit,
    onTierImagesChange: () -> Unit,
    onPendingImagesChange: (List<Uri>) -> Unit,
    onTierRowPositionsChange: (Map<String, android.graphics.Rect>) -> Unit,
    onDisableClickAddChange: (Boolean) -> Unit,
    onFloatOffsetXChange: (Float) -> Unit,
    onFloatOffsetYChange: (Float) -> Unit,
    onExternalBadgeChange: (Boolean) -> Unit,
    onFollowSystemThemeChange: ((Boolean) -> Unit)?,
    onDisableCustomFontChange: ((Boolean) -> Unit)?,
    onNameBelowImageChange: (Boolean) -> Unit,
    onLanguageChange: (String) -> Unit,
    onSkipDraftSave: (() -> Unit)?,
    onResumeDraftSave: (() -> Unit)?,
    presetExportLauncher: ActivityResultLauncher<String>,
    packageExportLauncher: ActivityResultLauncher<String>,
    presetFilePicker: ActivityResultLauncher<String>
) {
    val extendedColors = LocalExtendedColors.current

    // ==================== 图片操作对话框 ====================
    if (dialogState.showImageActionDialog && dialogState.selectedImageForAction != null) {
        ImageActionDialog(
            onDismiss = handlers::onImageActionDismiss,
            onSetBadge = handlers::onImageSetBadge,
            onReplace = handlers::onImageReplace,
            onMove = handlers::onImageMove,
            onRename = handlers::onImageRename,
            onView = handlers::onImageView,
            onCrop = handlers::onImageCrop
        )
    }

    // ==================== 图片查看对话框 ====================
    if (dialogState.showImageViewDialog && dialogState.selectedImageForAction != null) {
        ImageViewDialog(
            imageUri = dialogState.selectedImageForAction!!.uri,
            onDismiss = handlers::onImageViewDismiss
        )
    }

    // ==================== 图片裁剪对话框 ====================
    if (dialogState.showCropDialog && dialogState.selectedImageForAction != null) {
        val currentImageIndex = tierImages.indexOfFirst { it.id == dialogState.selectedImageForAction!!.id }
        val currentImageForCrop = if (currentImageIndex != -1) tierImages[currentImageIndex] else null
        val imageUriToCrop = currentImageForCrop?.originalUri ?: dialogState.selectedImageForAction!!.uri
        val initialCropState = if (currentImageForCrop != null) {
            CropState(
                positionX = currentImageForCrop.cropPositionX,
                positionY = currentImageForCrop.cropPositionY,
                scale = currentImageForCrop.cropScale,
                cropRatio = currentImageForCrop.cropRatio,
                useCustomCrop = currentImageForCrop.useCustomCrop,
                customCropWidth = currentImageForCrop.customCropWidth,
                customCropHeight = currentImageForCrop.customCropHeight
            )
        } else {
            CropState()
        }

        ImageCropDialog(
            imageUri = imageUriToCrop,
            initialCropState = initialCropState,
            onDismiss = handlers::onCropDismiss,
            onCrop = { resultUri, cropState ->
                handleCropResult(
                    dialogState = dialogState,
                    tierImages = tierImages,
                    resultUri = resultUri,
                    cropState = cropState,
                    context = context,
                    presetManager = presetManager,
                    onTierImagesChange = onTierImagesChange
                )
            },
            presetManager = presetManager,
            onApplyToAll = { currentCropState, sourceImageWidth, sourceImageHeight ->
                handleApplyToAll(
                    context = context,
                    tierImages = tierImages,
                    currentCropState = currentCropState,
                    sourceImageWidth = sourceImageWidth,
                    sourceImageHeight = sourceImageHeight,
                    presetManager = presetManager,
                    onTierImagesChange = onTierImagesChange
                )
            }
        )
    }

    // ==================== 设置小图标对话框 ====================
    if (dialogState.showSetBadgeDialog && dialogState.imageForBadge != null) {
        val currentImage by remember(dialogState.imageForBadge!!.id) {
            derivedStateOf {
                val currentImageIndex = tierImages.indexOfFirst { it.id == dialogState.imageForBadge!!.id }
                (if (currentImageIndex != -1) tierImages[currentImageIndex] else dialogState.imageForBadge)!!
            }
        }
        SetBadgeDialog(
            badgeUri1 = currentImage.badgeUri,
            badgeUri2 = currentImage.badgeUri2,
            badgeUri3 = currentImage.badgeUri3,
            presetManager = presetManager,
            onDismiss = handlers::onSetBadgeDismiss,
            onSelectBadge1 = handlers::onSelectBadge1,
            onSelectBadge2 = handlers::onSelectBadge2,
            onSelectBadge3 = handlers::onSelectBadge3,
            onDeleteBadge1 = handlers::onDeleteBadge1,
            onDeleteBadge2 = handlers::onDeleteBadge2,
            onDeleteBadge3 = handlers::onDeleteBadge3,
            onAddBadge = handlers::onAddBadge,
            onSelectBadgeFromPreview = handlers::onSelectBadgeFromPreview,
            onDeleteBadgeFromPreview = { badgeUri ->
                val success = handlers.onDeleteBadgeFromPreview(badgeUri)
                if (!success) {
                    showToastWithoutIcon(
                        context,
                        context.getString(R.string.badge_in_use_message),
                        android.widget.Toast.LENGTH_LONG
                    )
                }
                success
            },
            externalRefreshKey = dialogState.badgeDialogRefreshKey
        )
    }

    // ==================== 移动图片对话框 ====================
    if (dialogState.showMoveImageDialog && dialogState.selectedImageForAction != null) {
        MoveImageDialog(
            tiers = tiers,
            currentTierLabel = dialogState.selectedImageForAction!!.tierLabel,
            onDismiss = handlers::onMoveImageDismiss,
            onMoveToTier = handlers::onMoveToTier,
            onMoveToFirst = handlers::onMoveToFirst,
            onMoveToLast = handlers::onMoveToLast,
            onMoveLeft = handlers::onMoveLeft,
            onMoveRight = handlers::onMoveRight
        )
    }

    // ==================== 编辑图片名称对话框 ====================
    if (dialogState.showEditImageNameDialog && dialogState.selectedImageForAction != null) {
        EditImageNameDialog(
            currentName = dialogState.selectedImageForAction!!.name,
            onDismiss = handlers::onEditImageNameDismiss,
            onConfirm = { newName ->
                handlers.onEditImageNameConfirm(newName)
            }
        )
    }

    // ==================== 编辑标题对话框 ====================
    if (dialogState.showEditTitleDialog) {
        EditTitleDialog(
            currentTitle = tierListTitle,
            onDismiss = handlers::onEditTitleDismiss,
            onConfirm = { newTitle ->
                handlers.onEditTitleConfirm(newTitle, onTitleChange)
            }
        )
    }

    // ==================== 编辑作者对话框 ====================
    if (dialogState.showEditAuthorDialog) {
        EditAuthorDialog(
            currentAuthor = authorName,
            onDismiss = handlers::onEditAuthorDismiss,
            onConfirm = { newAuthor ->
                handlers.onEditAuthorConfirm(newAuthor, onAuthorChange)
            }
        )
    }

    // ==================== 设置菜单对话框 ====================
    if (dialogState.showSettingsMenu) {
        SettingsMenuDialog(
            onDismiss = handlers::onSettingsMenuDismiss,
            onShowInstructions = handlers::onShowInstructions,
            onShowFeedback = handlers::onShowFeedback,
            onImagePackage = handlers::onImagePackage,
            onShowProgramSettings = handlers::onShowProgramSettings,
            onManagePresets = handlers::onManagePresets
        )
    }

    // ==================== 程序设置对话框 ====================
    if (dialogState.showProgramSettingsDialog) {
        ProgramSettingsDialog(
            onDismiss = handlers::onProgramSettingsDismiss,
            disableClickAdd = disableClickAdd,
            onToggleDisableClickAdd = { newValue ->
                handlers.onToggleDisableClickAdd(newValue, onDisableClickAddChange)
            },
            floatOffsetX = floatOffsetX,
            onFloatOffsetXChange = { newValue ->
                handlers.onFloatOffsetXChange(newValue, onFloatOffsetXChange)
            },
            floatOffsetY = floatOffsetY,
            onFloatOffsetYChange = { newValue ->
                handlers.onFloatOffsetYChange(newValue, onFloatOffsetYChange)
            },
            externalBadgeEnabled = externalBadgeEnabled,
            onToggleExternalBadge = { newValue ->
                handlers.onToggleExternalBadge(newValue, onExternalBadgeChange)
            },
            followSystemTheme = followSystemTheme,
            onToggleFollowSystemTheme = { newValue ->
                handlers.onToggleFollowSystemTheme(newValue, onFollowSystemThemeChange)
            },
            onShowLanguageDialog = handlers::onShowLanguageDialog,
            disableCustomFont = disableCustomFont,
            onToggleDisableCustomFont = { newValue ->
                handlers.onToggleDisableCustomFont(newValue, onDisableCustomFontChange)
            },
            nameBelowImage = nameBelowImage,
            onToggleNameBelowImage = { newValue ->
                handlers.onToggleNameBelowImage(newValue, onNameBelowImageChange)
            }
        )
    }

    // ==================== 资源管理对话框 ====================
    if (dialogState.showResourceManageDialog) {
        ResourceManageDialog(
            onDismiss = handlers::onResourceManageDismiss,
            presetManager = presetManager,
            onResettiermaster = {
                handlers.onResetTierMaster(
                    defaultTiers = defaultTiers,
                    onTitleReset = { onTitleChange(context.getString(R.string.default_title)) },
                    onAuthorReset = { onAuthorChange("") },
                    onTiersReset = {
                        tiers.clear()
                        tiers.addAll(defaultTiers)
                        onTiersChange()
                    },
                    onTierImagesReset = onTierImagesChange,
                    onTierRowPositionsReset = { onTierRowPositionsChange(emptyMap()) },
                    onPendingImagesReset = onPendingImagesChange
                )
            }
        )
    }

    // ==================== 管理图包对话框 ====================
    if (dialogState.showManagePackagesDialog) {
        ManagePackagesDialog(
            context = context,
            presetManager = presetManager,
            onDismiss = handlers::onManagePackagesDismiss,
            onImportPackage = handlers::onImportPackage,
            onPackageSelected = { packageItem ->
                handlers.onPackageSelected(packageItem) { count ->
                    dialogState.selectedPackageImageCount = count
                }
            },
            onExportPackage = { packageItem ->
                dialogState.packageToExport = packageItem
                handlers.onExportPackage(packageItem) {
                    packageExportLauncher.launch("${packageItem.name}.zip")
                }
            }
        )
    }

    // ==================== 图包确认对话框 ====================
    if (dialogState.showPackageConfirmDialog && dialogState.selectedPackage != null) {
        PackageConfirmDialog(
            packageName = dialogState.selectedPackage!!.name,
            imageCount = dialogState.selectedPackageImageCount,
            isImporting = dialogState.isImportingPackage,
            onDismiss = handlers::onPackageConfirmDismiss,
            onConfirm = { target ->
                handlePackageImport(
                    context = context,
                    dialogState = dialogState,
                    target = target,
                    pendingImages = pendingImages,
                    presetManager = presetManager,
                    onPendingImagesChange = onPendingImagesChange
                )
            }
        )
    }

    // ==================== 导入图包对话框 ====================
    if (dialogState.showImportPackageDialog) {
        ImportPackageDialog(
            context = context,
            presetManager = presetManager,
            pendingImages = pendingImages,
            onPendingImagesChanged = onPendingImagesChange,
            onDismiss = handlers::onImportPackageDismiss,
            onSkipDraftSave = onSkipDraftSave,
            onResumeDraftSave = onResumeDraftSave
        )
    }

    // ==================== 语言选择对话框 ====================
    if (dialogState.showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = currentLanguage,
            onDismiss = handlers::onLanguageDialogDismiss,
            onLanguageSelected = { language ->
                handlers.onLanguageSelected(language, currentLanguage, onLanguageChange)
            }
        )
    }

    // ==================== 使用说明对话框 ====================
    if (dialogState.showInstructionsDialog) {
        InstructionsDialog(onDismiss = handlers::onInstructionsDismiss)
    }

    // ==================== 捐赠打赏对话框 ====================
    if (dialogState.showDonateDialog) {
        DonateDialog(
            onDismiss = handlers::onDonateDismiss
        )
    }

    // ==================== 管理预设对话框 ====================
    if (dialogState.showManagePresetsDialog) {
        ManagePresetsDialog(
            onDismiss = handlers::onManagePresetsDismiss,
            onExportPreset = {
                handlers.onExportPreset(
                    defaultTitle = context.getString(R.string.default_title),
                    currentTitle = tierListTitle,
                    onLaunchExport = {
                        presetExportLauncher.launch("$tierListTitle.tdds")
                    }
                )
            },
            onImportPreset = {
                handlers.onImportPreset {
                    presetFilePicker.launch("*/*")
                }
            },
            onSavePreset = {
                handlers.onSavePreset(
                    defaultTitle = context.getString(R.string.default_title),
                    currentTitle = tierListTitle
                )
            },
            onManagePresetList = handlers::onManagePresetList
        )
    }

    // ==================== 预设名称输入对话框 ====================
    if (dialogState.showPresetNameDialog) {
        // 根据是否是导入时新建预设，决定默认名称
        val defaultName = when {
            dialogState.isImportCreatingNewPreset && dialogState.pendingImportResult != null -> {
                // 导入时新建预设，使用导入预设的标题作为默认值
                dialogState.pendingImportResult?.presetData?.title ?: ""
            }
            tierListTitle == context.getString(R.string.default_title) -> ""
            else -> tierListTitle
        }
        PresetNameDialog(
            defaultName = defaultName,
            onDismiss = handlers::onPresetNameDialogDismiss,
            onConfirm = { name ->
                if (dialogState.isImportCreatingNewPreset) {
                    // 导入时新建预设：使用新名称导入并应用预设
                    handleImportWithNewName(
                        dialogState = dialogState,
                        newName = name,
                        context = context,
                        scope = scope,
                        settingsService = settingsService,
                        presetManager = presetManager,
                        tierImages = tierImages,
                        tiers = tiers,
                        onTierImagesChange = onTierImagesChange,
                        onTiersChange = onTiersChange,
                        onPendingImagesChange = onPendingImagesChange,
                        onTitleChange = onTitleChange,
                        onAuthorChange = onAuthorChange,
                        onTierRowPositionsChange = onTierRowPositionsChange
                    )
                } else {
                    handlers.onPresetNameConfirm(name) {
                        presetExportLauncher.launch("$name.tdds")
                    }
                }
            }
        )
    }

    // ==================== 预设覆盖确认对话框 ====================
    if (dialogState.showPresetOverwriteConfirmDialog) {
        PresetOverwriteConfirmDialog(
            dialogState = dialogState,
            handlers = handlers,
            context = context,
            scope = scope,
            settingsService = settingsService,
            presetManager = presetManager,
            tierImages = tierImages,
            tiers = tiers,
            pendingImages = pendingImages,
            authorName = authorName,
            extendedColors = extendedColors
        )
    }

    // ==================== 加载资源中对话框 ====================
    if (dialogState.isLoading()) {
        LoadingDialog(message = stringResource(R.string.loading_resources))
    }

    // ==================== 导入预设覆盖确认对话框 ====================
    if (dialogState.showImportOverwriteDialog && dialogState.pendingImportResult != null) {
        ImportOverwriteDialog(
            presetName = dialogState.pendingImportResult?.presetData?.title ?: "",
            onDismiss = handlers::onImportOverwriteDismiss,
            onOverwrite = {
                handleImportOverwrite(
                    dialogState = dialogState,
                    context = context,
                    scope = scope,
                    settingsService = settingsService,
                    presetManager = presetManager,
                    tierImages = tierImages,
                    tiers = tiers,
                    onTierImagesChange = onTierImagesChange,
                    onTiersChange = onTiersChange,
                    onPendingImagesChange = onPendingImagesChange,
                    onTitleChange = onTitleChange,
                    onAuthorChange = onAuthorChange,
                    onTierRowPositionsChange = onTierRowPositionsChange
                )
            },
            onCreateNew = handlers::onImportCreateNewPreset
        )
    }

    // ==================== 预设列表对话框 ====================
    if (dialogState.showPresetListDialog) {
        PresetListDialog(
            presetManager = presetManager,
            onDismiss = handlers::onPresetListDismiss,
            onApplyPreset = { presetInfo ->
                handleApplyPreset(
                    presetInfo = presetInfo,
                    presetManager = presetManager,
                    tiers = tiers,
                    tierImages = tierImages,
                    onTiersChange = onTiersChange,
                    onTierImagesChange = onTierImagesChange,
                    onPendingImagesChange = onPendingImagesChange,
                    onTitleChange = onTitleChange,
                    onAuthorChange = onAuthorChange,
                    onTierRowPositionsChange = onTierRowPositionsChange,
                    settingsService = settingsService
                )
            }
        )
    }

    // ==================== 编辑层级名称对话框 ====================
    if (dialogState.showEditNameDialog && dialogState.editingTier != null) {
        EditTierNameDialog(
            currentName = dialogState.editingTier!!.label,
            existingNames = tiers.map { it.label },
            onDismiss = {
                dialogState.showEditNameDialog = false
                dialogState.editingTier = null
            },
            onConfirm = { newName ->
                val tier = dialogState.editingTier
                if (tier != null && newName != tier.label) {
                    val index = tiers.indexOfFirst { it.label == tier.label }
                    if (index != -1) {
                        // 更新层级名称
                        tiers[index] = tiers[index].copy(label = newName)
                        // 更新该层级下的所有图片的 tierLabel
                        val imagesToUpdate = tierImages.filter { it.tierLabel == tier.label }
                        imagesToUpdate.forEach { image ->
                            val imageIndex = tierImages.indexOfFirst { it.id == image.id }
                            if (imageIndex != -1) {
                                tierImages[imageIndex] = image.copy(tierLabel = newName)
                            }
                        }
                        // 更新层级位置信息
                        val oldRect = tierRowPositions[tier.label]
                        if (oldRect != null) {
                            onTierRowPositionsChange(tierRowPositions - tier.label + (newName to oldRect))
                        }
                    }
                }
                dialogState.showEditNameDialog = false
                dialogState.editingTier = null
            }
        )
    }

    // ==================== 层级颜色选择器对话框 ====================
    if (dialogState.showColorPickerDialog && dialogState.editingTier != null) {
        ColorPickerDialog(
            currentColor = dialogState.editingTier!!.color,
            onDismiss = {
                dialogState.showColorPickerDialog = false
                dialogState.editingTier = null
            },
            onConfirm = { newColor ->
                val tier = dialogState.editingTier
                if (tier != null) {
                    val index = tiers.indexOfFirst { it.label == tier.label }
                    if (index != -1) {
                        tiers[index] = tiers[index].copy(color = newColor)
                    }
                }
                dialogState.showColorPickerDialog = false
                dialogState.editingTier = null
            }
        )
    }

    // ==================== 图片预览对话框 ====================
    if (dialogState.showPreviewDialog && dialogState.previewBitmap != null) {
        var previewIsDarkTheme by remember { mutableStateOf(isDarkTheme) }
        PreviewDialog(
            bitmap = dialogState.previewBitmap!!,
            isSaving = dialogState.isSavingChart,
            isSharing = dialogState.isSharingChart,
            onDismiss = handlers::onPreviewDismiss,
            onSave = {
                handlers.onPreviewSave {
                    saveBitmapToGallery(context, dialogState.previewBitmap!!, tierListTitle)
                }
            },
            onShare = {
                handlers.onPreviewShare {
                    shareBitmap(context, dialogState.previewBitmap!!, tierListTitle)
                }
            },
            onThemeToggle = {
                handlers.onPreviewThemeToggle {
                    previewIsDarkTheme = !previewIsDarkTheme
                    dialogState.previewBitmap = generateTierListBitmap(
                        context, tiers, tierImages, tierListTitle, authorName,
                        previewIsDarkTheme, externalBadgeEnabled, disableCustomFont, nameBelowImage
                    )
                }
            },
            isDarkTheme = isDarkTheme,
            appDarkTheme = previewIsDarkTheme
        )
    }
}

// ==================== 私有辅助函数 ====================

private fun handleCropResult(
    dialogState: DialogState,
    tierImages: MutableList<TierImage>,
    resultUri: Uri,
    cropState: CropState,
    context: Context,
    presetManager: PresetManager,
    onTierImagesChange: () -> Unit
) {
    val index = tierImages.indexOfFirst { it.id == dialogState.selectedImageForAction!!.id }
    if (index != -1) {
        val currentImage = tierImages[index]
        val originalImageUri = currentImage.originalUri ?: currentImage.uri
        val isReset = cropState.cropRatio == 0f

        tierImages[index] = currentImage.copy(
            uri = if (isReset) originalImageUri else resultUri,
            originalUri = if (isReset) null else originalImageUri,
            cropPositionX = cropState.positionX,
            cropPositionY = cropState.positionY,
            cropScale = cropState.scale,
            isCropped = !isReset,
            cropRatio = cropState.cropRatio,
            useCustomCrop = cropState.useCustomCrop,
            customCropWidth = cropState.customCropWidth,
            customCropHeight = cropState.customCropHeight
        )
        onTierImagesChange()

        if (!isReset) {
            try {
                val oldUri = currentImage.uri
                if (oldUri != originalImageUri) {
                    val fileName = oldUri.lastPathSegment
                    if (fileName != null && fileName.startsWith("cropped_")) {
                        val file = File(context.filesDir, fileName)
                        if (file.exists()) {
                            file.delete()
                        }
                    }
                }
            } catch (e: Exception) {
            }
        }
    }
    dialogState.showCropDialog = false
    dialogState.selectedImageForAction = null
}

private fun handleApplyToAll(
    context: Context,
    tierImages: MutableList<TierImage>,
    currentCropState: CropState,
    sourceImageWidth: Int,
    sourceImageHeight: Int,
    presetManager: PresetManager,
    onTierImagesChange: () -> Unit
) {
    var appliedCount = 0
    var skippedCount = 0
    tierImages.forEachIndexed { index, tierImage ->
        if (!tierImage.isCropped) {
            try {
                val originalUri = tierImage.originalUri ?: tierImage.uri
                context.contentResolver.openInputStream(originalUri)?.use { stream ->
                    val options = android.graphics.BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    android.graphics.BitmapFactory.decodeStream(stream, null, options)
                    val imageWidth = options.outWidth
                    val imageHeight = options.outHeight

                    if (imageWidth != sourceImageWidth || imageHeight != sourceImageHeight) {
                        skippedCount++
                        return@use
                    }

                    val cropWidth: Int
                    val cropHeight: Int
                    if (currentCropState.useCustomCrop && currentCropState.customCropWidth > 0 && currentCropState.customCropHeight > 0) {
                        cropWidth = currentCropState.customCropWidth.coerceIn(1, imageWidth)
                        cropHeight = currentCropState.customCropHeight.coerceIn(1, imageHeight)
                    } else {
                        val aspectRatio = currentCropState.cropRatio
                        if (imageWidth.toFloat() / imageHeight > aspectRatio) {
                            cropHeight = imageHeight
                            cropWidth = (imageHeight * aspectRatio).toInt()
                        } else {
                            cropWidth = imageWidth
                            cropHeight = (imageWidth / aspectRatio).toInt()
                        }
                    }

                    val maxXOffset = imageWidth - cropWidth
                    val maxYOffset = imageHeight - cropHeight
                    val xOffset = (maxXOffset * currentCropState.positionX).toInt()
                    val yOffset = (maxYOffset * currentCropState.positionY).toInt()

                    context.contentResolver.openInputStream(originalUri)?.use { cropStream ->
                        val originalBitmap = android.graphics.BitmapFactory.decodeStream(cropStream)
                        if (originalBitmap != null) {
                            val croppedBitmap = android.graphics.Bitmap.createBitmap(
                                originalBitmap,
                                xOffset,
                                yOffset,
                                cropWidth,
                                cropHeight
                            )

                            val workDir = File(presetManager.getWorkImagesDirectory(), PresetManager.IMAGES_FOLDER_NAME)
                            workDir.mkdirs()
                            val targetFile = File(workDir, "cropped_${System.currentTimeMillis()}_${index}.webp")
                            java.io.FileOutputStream(targetFile).use { out ->
                                croppedBitmap.compress(android.graphics.Bitmap.CompressFormat.WEBP_LOSSY, 100, out)
                            }

                            tierImages[index] = tierImage.copy(
                                uri = Uri.fromFile(targetFile),
                                originalUri = originalUri,
                                cropPositionX = currentCropState.positionX,
                                cropPositionY = currentCropState.positionY,
                                cropScale = currentCropState.scale,
                                isCropped = true,
                                cropRatio = currentCropState.cropRatio,
                                useCustomCrop = currentCropState.useCustomCrop,
                                customCropWidth = currentCropState.customCropWidth,
                                customCropHeight = currentCropState.customCropHeight
                            )
                            appliedCount++

                            if (originalBitmap != croppedBitmap) {
                                originalBitmap.recycle()
                            }
                            croppedBitmap.recycle()
                        }
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

    onTierImagesChange()
    showToastWithoutIcon(
        context,
        context.getString(R.string.apply_to_all_success, appliedCount),
        android.widget.Toast.LENGTH_SHORT
    )
}

private fun handlePackageImport(
    context: Context,
    dialogState: DialogState,
    target: ImportTarget,
    pendingImages: List<Uri>,
    presetManager: PresetManager,
    onPendingImagesChange: (List<Uri>) -> Unit
) {
    if (dialogState.isImportingPackage) return
    dialogState.isImportingPackage = true

    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
        try {
            val targetDir = when (target) {
                ImportTarget.PENDING ->
                    File(presetManager.getWorkImagesDirectory(), PresetManager.IMAGES_FOLDER_NAME)
                ImportTarget.BADGES ->
                    File(presetManager.getWorkImagesDirectory(), PresetManager.BADGES_FOLDER_NAME)
            }

            val imageUris = PackageManager.extractImportedPackage(
                context,
                dialogState.selectedPackage!!.file,
                targetDir
            )

            if (imageUris.isNotEmpty()) {
                when (target) {
                    ImportTarget.PENDING -> {
                        onPendingImagesChange(pendingImages + imageUris)
                        showToastWithoutIcon(
                            context,
                            context.getString(R.string.import_success, imageUris.size)
                        )
                    }
                    ImportTarget.BADGES -> {
                        showToastWithoutIcon(context, context.getString(R.string.badge_added))
                    }
                }
            } else {
                showToastWithoutIcon(
                    context,
                    context.getString(R.string.no_images_in_zip)
                )
            }
        } catch (e: Exception) {
            showToastWithoutIcon(
                context,
                context.getString(R.string.import_failed, e.message),
                android.widget.Toast.LENGTH_LONG
            )
        }
        dialogState.isImportingPackage = false
        dialogState.showPackageConfirmDialog = false
        dialogState.selectedPackage = null
    }
}

private fun handleImportOverwrite(
    dialogState: DialogState,
    context: Context,
    scope: CoroutineScope,
    settingsService: SettingsService,
    presetManager: PresetManager,
    tierImages: MutableList<TierImage>,
    tiers: MutableList<TierItem>,
    onTierImagesChange: () -> Unit,
    onTiersChange: () -> Unit,
    onPendingImagesChange: (List<Uri>) -> Unit,
    onTitleChange: (String) -> Unit,
    onAuthorChange: (String) -> Unit,
    onTierRowPositionsChange: (Map<String, android.graphics.Rect>) -> Unit
) {
    val importResult = dialogState.pendingImportResult
    dialogState.showImportOverwriteDialog = false
    dialogState.pendingImportResult = null

    importResult?.let { result ->
        scope.launch {
            try {
                val newPresetFile = result.existingPresetFile?.let { existingFile ->
                    presetManager.overwritePreset(result.presetFile, existingFile)
                } ?: result.presetFile

                val applyResult = presetManager.applyPreset(newPresetFile)

                tiers.clear()
                tiers.addAll(applyResult.tiers.map { tierData ->
                    TierItem(tierData.label, try {
                        androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor("#${tierData.color}"))
                    } catch (e: Exception) { androidx.compose.ui.graphics.Color.Gray })
                })
                onTiersChange()

                tierImages.clear()
                tierImages.addAll(applyResult.tierImages.map { appliedImage ->
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
                onTierImagesChange()

                onPendingImagesChange(applyResult.pendingImages)
                onTitleChange(result.presetData.title)
                onAuthorChange(result.presetData.author)

                settingsService.clearCropSettings()
                settingsService.customCropWidth = applyResult.customCropWidth
                settingsService.customCropHeight = applyResult.customCropHeight
                settingsService.useCustomCropSize = applyResult.useCustomCropSize
                settingsService.cropRatio = applyResult.cropRatio

                onTierRowPositionsChange(emptyMap())

                showToastWithoutIcon(context, context.getString(R.string.preset_overwrite_success))

                withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val workImagesDir = presetManager.getWorkImagesDirectory()
                        val newPresetData = presetManager.createPresetData(
                            title = result.presetData.title,
                            author = result.presetData.author,
                            tiers = tiers,
                            tierImages = tierImages,
                            pendingImages = applyResult.pendingImages,
                            cropPositionX = settingsService.cropPositionX,
                            cropPositionY = settingsService.cropPositionY,
                            customCropWidth = settingsService.customCropWidth,
                            customCropHeight = settingsService.customCropHeight,
                            useCustomCropSize = settingsService.useCustomCropSize,
                            cropRatio = settingsService.cropRatio
                        )
                        presetManager.exportPreset(
                            presetName = result.presetData.title,
                            presetData = newPresetData,
                            tempDir = workImagesDir,
                            outputFile = newPresetFile
                        )
                    } catch (e: Exception) {
                    }
                }
            } catch (e: Exception) {
                showToastWithoutIcon(
                    context,
                    context.getString(R.string.preset_overwrite_failed, e.message),
                    android.widget.Toast.LENGTH_LONG
                )
            }
        }
    }
}

/**
 * 处理导入预设时使用新名称的情况
 * 将预设以新名称保存并应用
 */
private fun handleImportWithNewName(
    dialogState: DialogState,
    newName: String,
    context: Context,
    scope: CoroutineScope,
    settingsService: SettingsService,
    presetManager: PresetManager,
    tierImages: MutableList<TierImage>,
    tiers: MutableList<TierItem>,
    onTierImagesChange: () -> Unit,
    onTiersChange: () -> Unit,
    onPendingImagesChange: (List<Uri>) -> Unit,
    onTitleChange: (String) -> Unit,
    onAuthorChange: (String) -> Unit,
    onTierRowPositionsChange: (Map<String, android.graphics.Rect>) -> Unit
) {
    val importResult = dialogState.pendingImportResult
    dialogState.showPresetNameDialog = false
    dialogState.isImportCreatingNewPreset = false
    dialogState.isImportingPreset = true

    importResult?.let { result ->
        scope.launch {
            try {
                // 使用新名称保存预设：复制文件内容但更新标题
                // 如果名称已存在，saveImportedPresetAsNew 会覆盖现有文件
                val newPresetFile = presetManager.saveImportedPresetAsNew(
                    result.presetFile,
                    newName
                )

                // 应用预设
                val applyResult = presetManager.applyPreset(newPresetFile)

                tiers.clear()
                tiers.addAll(applyResult.tiers.map { tierData ->
                    TierItem(tierData.label, try {
                        androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor("#${tierData.color}"))
                    } catch (e: Exception) { androidx.compose.ui.graphics.Color.Gray })
                })
                onTiersChange()

                tierImages.clear()
                tierImages.addAll(applyResult.tierImages.map { appliedImage ->
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
                onTierImagesChange()

                onPendingImagesChange(applyResult.pendingImages)
                onTitleChange(newName)
                onAuthorChange(result.presetData.author)

                settingsService.clearCropSettings()
                settingsService.customCropWidth = applyResult.customCropWidth
                settingsService.customCropHeight = applyResult.customCropHeight
                settingsService.useCustomCropSize = applyResult.useCustomCropSize
                settingsService.cropRatio = applyResult.cropRatio

                onTierRowPositionsChange(emptyMap())

                showToastWithoutIcon(context, context.getString(R.string.preset_import_success))

                // 清理临时文件
                result.presetFile.delete()
                dialogState.pendingImportResult = null
            } catch (e: Exception) {
                showToastWithoutIcon(
                    context,
                    context.getString(R.string.preset_import_failed, e.message),
                    android.widget.Toast.LENGTH_LONG
                )
            } finally {
                dialogState.isImportingPreset = false
            }
        }
    }
}

private fun handleApplyPreset(
    presetInfo: PresetInfo,
    presetManager: PresetManager,
    tiers: MutableList<TierItem>,
    tierImages: MutableList<TierImage>,
    onTiersChange: () -> Unit,
    onTierImagesChange: () -> Unit,
    onPendingImagesChange: (List<Uri>) -> Unit,
    onTitleChange: (String) -> Unit,
    onAuthorChange: (String) -> Unit,
    onTierRowPositionsChange: (Map<String, android.graphics.Rect>) -> Unit,
    settingsService: SettingsService
) {
    val applyResult = presetManager.applyPreset(presetInfo.file)

    tiers.clear()
    tiers.addAll(applyResult.tiers.map { tierData ->
        TierItem(tierData.label, try {
            androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor("#${tierData.color}"))
        } catch (e: Exception) { androidx.compose.ui.graphics.Color.Gray })
    })
    onTiersChange()

    tierImages.clear()
    tierImages.addAll(applyResult.tierImages.map { appliedImage ->
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
    onTierImagesChange()

    onPendingImagesChange(applyResult.pendingImages)
    onTitleChange(applyResult.title)
    onAuthorChange(applyResult.author)
    onTierRowPositionsChange(emptyMap())

    settingsService.clearCropSettings()
    settingsService.cropPositionX = applyResult.cropPositionX
    settingsService.cropPositionY = applyResult.cropPositionY
    settingsService.customCropWidth = applyResult.customCropWidth
    settingsService.customCropHeight = applyResult.customCropHeight
    settingsService.useCustomCropSize = applyResult.useCustomCropSize
    settingsService.cropRatio = applyResult.cropRatio
}

@Composable
private fun PresetOverwriteConfirmDialog(
    dialogState: DialogState,
    handlers: DialogHandlers,
    context: Context,
    scope: CoroutineScope,
    settingsService: SettingsService,
    presetManager: PresetManager,
    tierImages: MutableList<TierImage>,
    tiers: MutableList<TierItem>,
    pendingImages: List<Uri>,
    authorName: String,
    extendedColors: com.tdds.jh.ui.theme.ExtendedColors
) {
    val isNameExists = presetManager.isPresetNameExists(dialogState.pendingPresetName)

    if (isNameExists) {
        Dialog(onDismissRequest = handlers::onPresetOverwriteConfirmDismiss) {
            Card(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth(0.85f)
                    .padding(16.dp),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = extendedColors.cardBackground
                )
            ) {
                androidx.compose.foundation.layout.Column(
                    modifier = androidx.compose.ui.Modifier.padding(vertical = 20.dp, horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.preset_name_exists),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.preset_name_exists_message, dialogState.pendingPresetName),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(24.dp))

                    androidx.compose.foundation.layout.Column(
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                dialogState.showPresetOverwriteConfirmDialog = false
                                dialogState.isSavingPreset = true
                                scope.launch {
                                    kotlinx.coroutines.yield()
                                    try {
                                        val presetData = withContext(kotlinx.coroutines.Dispatchers.IO) {
                                            presetManager.createPresetData(
                                                title = dialogState.pendingPresetName,
                                                author = authorName,
                                                tiers = tiers,
                                                tierImages = tierImages,
                                                pendingImages = pendingImages,
                                                cropPositionX = settingsService.cropPositionX,
                                                cropPositionY = settingsService.cropPositionY,
                                                customCropWidth = settingsService.customCropWidth,
                                                customCropHeight = settingsService.customCropHeight,
                                                useCustomCropSize = settingsService.useCustomCropSize,
                                                cropRatio = settingsService.cropRatio
                                            )
                                        }
                                        withContext(kotlinx.coroutines.Dispatchers.IO) {
                                            presetManager.savePreset(dialogState.pendingPresetName, presetData)
                                        }

                                        showToastWithoutIcon(context, context.getString(R.string.preset_save_success))
                                    } catch (e: Exception) {
                                        showToastWithoutIcon(
                                            context,
                                            context.getString(R.string.preset_save_failed, e.message),
                                            android.widget.Toast.LENGTH_LONG
                                        )
                                    }
                                    dialogState.isSavingPreset = false
                                    dialogState.pendingPresetName = ""
                                }
                            },
                            modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = extendedColors.buttonContainer,
                                contentColor = extendedColors.buttonContent
                            )
                        ) {
                            Text(stringResource(R.string.overwrite))
                        }

                        OutlinedButton(
                            onClick = handlers::onCreateNewPreset,
                            modifier = androidx.compose.ui.Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.create_new_preset))
                        }
                    }
                }
            }
        }
    } else {
        dialogState.showPresetOverwriteConfirmDialog = false
        dialogState.isSavingPreset = true
        scope.launch {
            kotlinx.coroutines.yield()
            try {
                val presetData = presetManager.createPresetData(
                    title = dialogState.pendingPresetName,
                    author = authorName,
                    tiers = tiers,
                    tierImages = tierImages,
                    pendingImages = pendingImages,
                    cropPositionX = settingsService.cropPositionX,
                    cropPositionY = settingsService.cropPositionY,
                    customCropWidth = settingsService.customCropWidth,
                    customCropHeight = settingsService.customCropHeight,
                    useCustomCropSize = settingsService.useCustomCropSize,
                    cropRatio = settingsService.cropRatio
                )
                presetManager.savePreset(dialogState.pendingPresetName, presetData)

                showToastWithoutIcon(context, context.getString(R.string.preset_save_success))
            } catch (e: Exception) {
                showToastWithoutIcon(
                    context,
                    context.getString(R.string.preset_save_failed, e.message),
                    android.widget.Toast.LENGTH_LONG
                )
            }
            dialogState.isSavingPreset = false
            dialogState.pendingPresetName = ""
        }
    }
}
