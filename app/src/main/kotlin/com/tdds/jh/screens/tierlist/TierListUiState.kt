package com.tdds.jh.screens.tierlist

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.net.toUri
import com.tdds.jh.model.tierlist.TierImage
import com.tdds.jh.model.tierlist.TierItem
import com.tdds.jh.data.tierlist.PackageItem
import com.tdds.jh.data.tierlist.PresetManager
import com.tdds.jh.model.tierlist.PresetOperation
import com.tdds.jh.model.tierlist.video.VideoGenerationConfig

// ==================== Parcelable 数据类用于状态保存 ====================

data class TierItemState(
    val label: String,
    val colorArgb: Int
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(label)
        parcel.writeInt(colorArgb)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<TierItemState> {
        override fun createFromParcel(parcel: Parcel): TierItemState = TierItemState(parcel)
        override fun newArray(size: Int): Array<TierItemState?> = arrayOfNulls(size)
    }
}

data class TierImageState(
    val id: String,
    val tierLabel: String,
    val uriString: String,
    val originalUriString: String?,
    val name: String?,
    val badgeUriString: String?,
    val badgeUri2String: String?,
    val badgeUri3String: String?,
    val audioUriString: String? = null
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(tierLabel)
        parcel.writeString(uriString)
        parcel.writeString(originalUriString)
        parcel.writeString(name)
        parcel.writeString(badgeUriString)
        parcel.writeString(badgeUri2String)
        parcel.writeString(badgeUri3String)
        parcel.writeString(audioUriString)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<TierImageState> {
        override fun createFromParcel(parcel: Parcel): TierImageState = TierImageState(parcel)
        override fun newArray(size: Int): Array<TierImageState?> = arrayOfNulls(size)
    }
}

fun TierItem.toState() = TierItemState(label, color.toArgb())
fun TierItemState.toTierItem() = TierItem(label, Color(colorArgb))
fun TierImage.toState() = TierImageState(
    id = id,
    tierLabel = tierLabel,
    uriString = uri.toString(),
    originalUriString = originalUri?.toString(),
    name = name.takeIf { it.isNotEmpty() },
    badgeUriString = badgeUri?.toString(),
    badgeUri2String = badgeUri2?.toString(),
    badgeUri3String = badgeUri3?.toString(),
    audioUriString = audioUri?.toString()
)
fun TierImageState.toTierImage() = TierImage(
    id = id,
    tierLabel = tierLabel,
    uri = uriString.toUri(),
    name = name ?: "",
    badgeUri = badgeUriString?.toUri(),
    badgeUri2 = badgeUri2String?.toUri(),
    badgeUri3 = badgeUri3String?.toUri(),
    originalUri = originalUriString?.toUri(),
    audioUri = audioUriString?.toUri()
)

/**
 * 可保存的 UI 状态数据类
 */
data class TierListSavedState(
    val tiers: List<TierItemState>,
    val tierImages: List<TierImageState>,
    val pendingImages: List<String>,
    val tierListTitle: String,
    val authorName: String,
    val disableClickAdd: Boolean,
    val floatOffsetX: Float,
    val floatOffsetY: Float,
    val externalBadgeEnabled: Boolean,
    val nameBelowImage: Boolean,
    val videoGenerationConfig: VideoGenerationConfig = VideoGenerationConfig()
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.createTypedArrayList(TierItemState.CREATOR) ?: emptyList(),
        parcel.createTypedArrayList(TierImageState.CREATOR) ?: emptyList(),
        parcel.createStringArrayList() ?: emptyList(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readByte() != 0.toByte(),
        parcel.readFloat(),
        parcel.readFloat(),
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
        parcel.readParcelable(VideoGenerationConfig::class.java.classLoader) ?: VideoGenerationConfig()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeTypedList(tiers)
        parcel.writeTypedList(tierImages)
        parcel.writeStringList(pendingImages)
        parcel.writeString(tierListTitle)
        parcel.writeString(authorName)
        parcel.writeByte(if (disableClickAdd) 1 else 0)
        parcel.writeFloat(floatOffsetX)
        parcel.writeFloat(floatOffsetY)
        parcel.writeByte(if (externalBadgeEnabled) 1 else 0)
        parcel.writeByte(if (nameBelowImage) 1 else 0)
        parcel.writeParcelable(videoGenerationConfig, flags)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<TierListSavedState> {
        override fun createFromParcel(parcel: Parcel): TierListSavedState = TierListSavedState(parcel)
        override fun newArray(size: Int): Array<TierListSavedState?> = arrayOfNulls(size)
    }
}

/**
 * TierListSavedState 的 Saver 用于 rememberSaveable
 */
val TierListSavedStateSaver = Saver<TierListSavedState?, Bundle>(
    save = { state ->
        if (state == null) Bundle()
        else Bundle().apply {
            putParcelable("saved_state", state)
        }
    },
    restore = { bundle ->
        @Suppress("DEPRECATION")
        bundle.getParcelable("saved_state")
    }
)

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
    var showVideoGenerationConfigDialog by mutableStateOf(false)
    var showImageAudioDialog by mutableStateOf(false)
    var selectedImageForAudio by mutableStateOf<com.tdds.jh.model.tierlist.TierImage?>(null)
    var showVideoPreviewDialog by mutableStateOf(false)
    var showVideoExportDialog by mutableStateOf(false)

    // ==================== 信息对话框 ====================
    var showDonateDialog by mutableStateOf(false)
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
        showDonateDialog = false
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

}
