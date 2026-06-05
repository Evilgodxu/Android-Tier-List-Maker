package com.tdds.jh.screens.tierlist

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.tdds.jh.R
import com.tdds.jh.data.tierlist.PresetData
import com.tdds.jh.data.tierlist.PresetManager
import com.tdds.jh.model.tierlist.TierImage
import com.tdds.jh.model.tierlist.TierItem
import com.tdds.jh.model.tierlist.TierListConfig
import com.tdds.jh.screens.tierlist.logic.usecases.ImagePickerHandler
import com.tdds.jh.screens.tierlist.logic.usecases.PackageOperationHandler
import com.tdds.jh.screens.tierlist.logic.usecases.PresetOperationHandler
import com.tdds.jh.screens.tierlist.logic.usecases.rememberImagePickerHandler
import com.tdds.jh.screens.tierlist.logic.usecases.rememberPackageOperationHandler
import com.tdds.jh.screens.tierlist.logic.usecases.rememberPresetOperationHandler
import com.tdds.jh.screens.tierlist.logic.utils.FileUtils
import com.tdds.jh.screens.tierlist.logic.utils.PermissionUtils
import com.tdds.jh.screens.tierlist.logic.utils.withStoragePermission
import com.tdds.jh.screens.tierlist.service.SettingsService
import com.tdds.jh.ui.toast.showToastWithoutIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.io.File

/**
 * ViewModel层：集中管理页面所有状态、Handler、Launcher和副作用
 * 使用 rememberSaveable 保存关键状态，屏幕旋转时不丢失
 */
@Stable
class TierListViewModel(
    val context: Context,
    val scope: CoroutineScope,
    val settingsService: SettingsService,
    val presetManager: PresetManager,
    val defaultTiers: List<TierItem>,
    initialSavedState: TierListSavedState? = null
) {
    // ==================== 核心列表（从 savedState 恢复或初始化） ====================
    val tiers = initialSavedState?.tiers?.map { it.toTierItem() }?.toMutableStateList()
        ?: mutableStateListOf<TierItem>().apply { addAll(defaultTiers) }
    val tierImages = initialSavedState?.tierImages?.map { it.toTierImage() }?.toMutableStateList()
        ?: mutableStateListOf<TierImage>()

    // ==================== 对话框状态（使用 rememberSaveable） ====================
    val dialogState = DialogState()

    // ==================== UI状态（从 savedState 恢复或初始化） ====================
    var disableClickAdd by mutableStateOf(initialSavedState?.disableClickAdd ?: settingsService.disableClickAdd)
    var floatOffsetX by mutableStateOf(initialSavedState?.floatOffsetX ?: settingsService.floatOffsetX)
    var floatOffsetY by mutableStateOf(initialSavedState?.floatOffsetY ?: settingsService.floatOffsetY)
    var externalBadgeEnabled by mutableStateOf(initialSavedState?.externalBadgeEnabled ?: settingsService.externalBadgeEnabled)
    var nameBelowImage by mutableStateOf(initialSavedState?.nameBelowImage ?: settingsService.nameBelowImage)

    var pendingImages by mutableStateOf(initialSavedState?.pendingImages?.map { it.toUri() } ?: emptyList())
    var tierListTitle by mutableStateOf(initialSavedState?.tierListTitle ?: context.getString(R.string.default_title))
    var authorName by mutableStateOf(initialSavedState?.authorName ?: "")

    var selectedImageForDrag by mutableStateOf<TierImage?>(null)
    var draggingTierImage by mutableStateOf<TierImage?>(null)
    var draggingTierImagePosition by mutableStateOf(Offset.Zero)
    var draggingTierImageTarget by mutableStateOf<String?>(null)
    var pendingSectionRect by mutableStateOf<android.graphics.Rect?>(null)
    var tierRowPositions by mutableStateOf<Map<String, android.graphics.Rect>>(emptyMap())
    var tierToDelete by mutableStateOf<TierItem?>(null)
    var isDraggingPendingImage by mutableStateOf(false)
    var draggedPendingImageUri by mutableStateOf<Uri?>(null)

    var isImportingPreset by mutableStateOf(false)
    var isExportingPreset by mutableStateOf(false)
    var isSavingPreset by mutableStateOf(false)
    var isExportingPackage by mutableStateOf(false)

    var draftConfigData by mutableStateOf<PresetData?>(null)
    var skipDraftRestore by mutableStateOf(false)
    var backPressedTime by mutableLongStateOf(0L)

    // ==================== 语言切换标记 ====================
    val shouldShowLanguageOnFirstLaunch = settingsService.showLanguageOnFirstLaunch

    // ==================== 草稿控制回调 ====================
    var onSkipDraftSaveCallback: (() -> Unit)? = null
    var onResumeDraftSaveCallback: (() -> Unit)? = null

    // ==================== Handler（由工厂函数赋值） ====================
    lateinit var imagePickerHandler: ImagePickerHandler
    lateinit var presetOperationHandler: PresetOperationHandler
    lateinit var packageOperationHandler: PackageOperationHandler
    lateinit var dialogHandlers: DialogHandlers

    // ==================== Launcher（由工厂函数赋值） ====================
    lateinit var imagePicker: ManagedActivityResultLauncher<PickVisualMediaRequest, List<@JvmSuppressWildcards Uri>>
    lateinit var addToPendingPicker: ManagedActivityResultLauncher<PickVisualMediaRequest, List<@JvmSuppressWildcards Uri>>
    lateinit var replaceImagePicker: ManagedActivityResultLauncher<PickVisualMediaRequest, Uri?>
    lateinit var badgeImagePicker: ManagedActivityResultLauncher<PickVisualMediaRequest, Uri?>
    lateinit var badgeImagePickerMultiple: ManagedActivityResultLauncher<PickVisualMediaRequest, List<@JvmSuppressWildcards Uri>>
    lateinit var permissionLauncher: ManagedActivityResultLauncher<String, Boolean>
    lateinit var presetFilePicker: ManagedActivityResultLauncher<String, Uri?>
    lateinit var presetExportLauncher: ManagedActivityResultLauncher<String, Uri?>
    lateinit var packageExportLauncher: ManagedActivityResultLauncher<String, Uri?>

    /**
     * 导出当前状态用于保存
     */
    fun exportSavedState(): TierListSavedState = TierListSavedState(
        tiers = tiers.map { it.toState() },
        tierImages = tierImages.map { it.toState() },
        pendingImages = pendingImages.map { it.toString() },
        tierListTitle = tierListTitle,
        authorName = authorName,
        disableClickAdd = disableClickAdd,
        floatOffsetX = floatOffsetX,
        floatOffsetY = floatOffsetY,
        externalBadgeEnabled = externalBadgeEnabled,
        nameBelowImage = nameBelowImage
    )

}

/**
 * ViewModel Saver 用于 rememberSaveable
 */
private val TierListViewModelSaver = Saver<TierListViewModel, TierListSavedState>(
    save = { it.exportSavedState() },
    restore = { null } // 恢复逻辑在工厂函数中处理
)

/**
 * ViewModel工厂函数
 * 使用 rememberSaveable 保存关键状态，屏幕旋转时自动恢复
 */
@Composable
fun rememberTierListViewModel(
    externalIntentFlow: Flow<Intent?>,
    isDarkTheme: Boolean,
    followSystemTheme: Boolean,
    disableCustomFont: Boolean,
    onDisableCustomFontChange: ((Boolean) -> Unit)?,
    onThemeChange: (Boolean) -> Unit,
    onFollowSystemThemeChange: ((Boolean) -> Unit)?,
    onRegisterSaveDraftCallback: ((() -> Unit) -> Unit)?,
    onSkipDraftSave: (() -> Unit)?,
    onResumeDraftSave: (() -> Unit)?,
    onExitApp: (() -> Unit)?,
): TierListViewModel {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsService = remember { SettingsService(context) }
    val presetManager = remember { PresetManager(context) }
    val defaultTiers = TierListConfig.getDefaultTiers(context.resources.configuration.locales[0].language == "zh")

    // 使用 rememberSaveable 保存和恢复状态
    var savedState by rememberSaveable(stateSaver = TierListSavedStateSaver) {
        mutableStateOf<TierListSavedState?>(null)
    }

    val vm = remember {
        TierListViewModel(context, scope, settingsService, presetManager, defaultTiers, savedState)
    }

    // 当状态变化时保存到 savedState（用于屏幕旋转恢复）
    LaunchedEffect(vm.tiers.size, vm.tierImages.size, vm.pendingImages.size, vm.tierListTitle, vm.authorName) {
        savedState = vm.exportSavedState()
    }

    // ==================== 注入草稿控制回调 ====================
    vm.onSkipDraftSaveCallback = onSkipDraftSave
    vm.onResumeDraftSaveCallback = onResumeDraftSave

    // ==================== Handler ====================
    vm.imagePickerHandler = rememberImagePickerHandler(
        scope = scope, dialogState = vm.dialogState, presetManager = presetManager,
        tierImages = vm.tierImages,
        onPendingImagesChange = { vm.pendingImages = it },
        onResumeDraftSave = onResumeDraftSave
    )
    vm.presetOperationHandler = rememberPresetOperationHandler(
        scope = scope, dialogState = vm.dialogState, presetManager = presetManager,
        settingsService = settingsService, tiers = vm.tiers, tierImages = vm.tierImages,
        onPendingImagesChange = { vm.pendingImages = it },
        onTitleChange = { vm.tierListTitle = it }, onAuthorChange = { vm.authorName = it },
        onTierRowPositionsReset = { vm.tierRowPositions = emptyMap() },
        onResumeDraftSave = onResumeDraftSave, onSkipDraftSave = onSkipDraftSave,
        showToast = { message, duration -> showToastWithoutIcon(context, message, duration) }
    )
    vm.packageOperationHandler = rememberPackageOperationHandler(
        scope = scope, dialogState = vm.dialogState, presetManager = presetManager,
        onPendingImagesChange = { vm.pendingImages = it },
        onSkipDraftSave = onSkipDraftSave, onResumeDraftSave = onResumeDraftSave,
        showToast = { message, duration -> showToastWithoutIcon(context, message, duration) }
    )
    vm.imagePickerHandler.setPendingImagesProvider { vm.pendingImages }

    // ==================== 外部文件打开副作用 ====================
    // 通过 Flow 驱动，支持 onNewIntent 时重新触发（配合 singleTask launchMode）
    val activity = context as? ComponentActivity
    LaunchedEffect(externalIntentFlow) {
        externalIntentFlow.distinctUntilChanged().collect { intent ->
            if (intent == null) return@collect
            when {
                intent.action == Intent.ACTION_VIEW -> handleViewIntent(vm, activity, intent)
                intent.action == Intent.ACTION_SEND || intent.action == Intent.ACTION_SEND_MULTIPLE -> handleSendIntent(vm, activity, intent)
            }
        }
    }

    /** 检查页面是否有用户编辑内容 */
    fun hasContent(defaultTitle: String): Boolean =
        vm.tierListTitle != defaultTitle || vm.authorName.isNotEmpty() ||
        vm.tierImages.isNotEmpty() || vm.pendingImages.isNotEmpty() ||
        vm.tiers.size != defaultTiers.size || vm.tiers.zip(defaultTiers).any { (c, d) -> c.label != d.label || c.color != d.color }

    // ==================== 草稿恢复副作用 ====================
    LaunchedEffect(Unit) {
        if (vm.skipDraftRestore) return@LaunchedEffect
        if (presetManager.hasDraft()) {
            if (!hasContent(context.getString(R.string.default_title))) {
                val draftConfig = presetManager.readDraftConfig()
                if (draftConfig != null) { vm.draftConfigData = draftConfig; vm.dialogState.showDraftRestoreDialog = true }
            } else presetManager.cleanupDraft()
        }
    }

    // ==================== 草稿保存注册 ====================
    DisposableEffect(Unit) {
        onRegisterSaveDraftCallback?.invoke {
            if (hasContent(context.getString(R.string.default_title))) vm.presetOperationHandler.saveDraft(vm.tierListTitle, vm.authorName, vm.pendingImages)
            else presetManager.cleanupDraft()
        }
        onDispose { }
    }

    // ==================== Launcher ====================
    vm.presetFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        vm.presetOperationHandler.handleImportPreset(uri, vm.pendingImages)
    }
    vm.presetExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        vm.presetOperationHandler.handleExportPreset(uri, vm.dialogState.pendingPresetName, vm.tierListTitle, vm.authorName, vm.pendingImages)
    }
    vm.packageExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        vm.packageOperationHandler.handleExportPackage(uri, vm.dialogState.packageToExport)
    }
    vm.imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(maxItems = 50)) { uris ->
        vm.imagePickerHandler.handleImagePickerResult(uris, vm.pendingImages)
    }
    vm.addToPendingPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(maxItems = 50)) { uris ->
        vm.imagePickerHandler.handleAddToPendingResult(uris, vm.pendingImages)
    }
    vm.replaceImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        vm.imagePickerHandler.handleReplaceImageResult(uri, vm.dialogState.imageToReplace)
    }
    vm.badgeImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        vm.imagePickerHandler.handleBadgeImagePickerResult(uri, vm.dialogState.badgeSelectionTarget, vm.dialogState.imageForBadge) { vm.dialogState.badgeDialogRefreshKey++ }
    }
    vm.badgeImagePickerMultiple = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20)) { uris ->
        vm.imagePickerHandler.handleBadgeImagePickerMultipleResult(uris) { vm.dialogState.badgeDialogRefreshKey++ }
    }
    vm.permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
        if (vm.shouldShowLanguageOnFirstLaunch) { vm.dialogState.showLanguageDialog = true; settingsService.showLanguageOnFirstLaunch = false }
        vm.dialogState.isBadgePickerLaunching = false; vm.dialogState.isImagePickerLaunching = false
    }

    // ==================== 权限检查副作用 ====================
    LaunchedEffect(Unit) {
        if (!PermissionUtils.hasStoragePermission(context)) PermissionUtils.requestStoragePermission(vm.permissionLauncher)
        else if (vm.shouldShowLanguageOnFirstLaunch) { vm.dialogState.showLanguageDialog = true; settingsService.showLanguageOnFirstLaunch = false }
    }

    // ==================== DialogHandlers ====================
    vm.dialogHandlers = remember {
        DialogHandlers(
            context = context, dialogState = vm.dialogState, scope = scope,
            settingsService = settingsService, presetManager = presetManager, tierImages = vm.tierImages,
            onTierImagesChange = {}, onPendingImagesChange = { vm.pendingImages = it },
            onSkipDraftSave = onSkipDraftSave, onResumeDraftSave = onResumeDraftSave,
            imagePicker = vm.imagePicker, replaceImagePicker = vm.replaceImagePicker,
            launchBadgePicker = { target ->
                if (!vm.dialogState.isBadgePickerLaunching) {
                    vm.dialogState.isBadgePickerLaunching = true; vm.dialogState.badgeSelectionTarget = target
                    withStoragePermission(context, vm.permissionLauncher, onSkipDraftSave) { vm.badgeImagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                }
            },
            launchBadgePickerForAdding = {
                if (!vm.dialogState.isBadgePickerLaunching) {
                    vm.dialogState.isBadgePickerLaunching = true; vm.dialogState.badgeSelectionTarget = 0
                    withStoragePermission(context, vm.permissionLauncher, onSkipDraftSave) { vm.badgeImagePickerMultiple.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                }
            },
            deleteBadge = { target ->
                val idx = vm.tierImages.indexOfFirst { it.id == vm.dialogState.imageForBadge?.id }
                if (idx != -1) {
                    vm.tierImages[idx] = when (target) { 1 -> vm.tierImages[idx].copy(badgeUri = null); 2 -> vm.tierImages[idx].copy(badgeUri2 = null); 3 -> vm.tierImages[idx].copy(badgeUri3 = null); else -> vm.tierImages[idx] }
                    vm.dialogState.imageForBadge = vm.tierImages[idx]
                }
            },
            deleteBadgeFile = { badgeUri, _ ->
                try { val f = File(badgeUri.path ?: return@DialogHandlers false); if (f.exists()) f.delete() else false } catch (_: Exception) { false }
            }
        )
    }

    return vm
}

private fun handleViewIntent(vm: TierListViewModel, activity: ComponentActivity?, intent: Intent) {
    val dataUri = intent.data ?: return
    val fileName = FileUtils.getFileNameFromUri(vm.context, dataUri)
    val uriString = dataUri.toString()
    val isTdds = (fileName?.endsWith(".tdds", ignoreCase = true) == true) || uriString.endsWith(".tdds", ignoreCase = true) || uriString.contains(".tdds", ignoreCase = true)
    val isZip = (fileName?.endsWith(".zip", ignoreCase = true) == true) || uriString.endsWith(".zip", ignoreCase = true) || uriString.contains(".zip", ignoreCase = true)

    if (isTdds) { vm.isImportingPreset = true; vm.skipDraftRestore = true; vm.presetOperationHandler.handleExternalPresetImport(dataUri) { isLoading -> vm.isImportingPreset = isLoading; if (!isLoading && activity != null) activity.intent = null } }
    if (isZip) { vm.skipDraftRestore = true; vm.packageOperationHandler.handleExternalPackageImport(dataUri, fileName) { isLoading -> vm.isImportingPreset = isLoading; if (!isLoading && activity != null) activity.intent = null } }
}

private fun handleSendIntent(vm: TierListViewModel, activity: ComponentActivity?, intent: Intent) {
    val dataUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
    else @Suppress("DEPRECATION")
        intent.getParcelableExtra(Intent.EXTRA_STREAM) ?: return
    if (dataUri == null) return
    val fileName = FileUtils.getFileNameFromUri(vm.context, dataUri); val uriString = dataUri.toString()
    val isTdds = (fileName?.endsWith(".tdds", ignoreCase = true) == true) || uriString.endsWith(".tdds", ignoreCase = true) || uriString.contains(".tdds", ignoreCase = true)
    val isZip = (fileName?.endsWith(".zip", ignoreCase = true) == true) || uriString.endsWith(".zip", ignoreCase = true) || uriString.contains(".zip", ignoreCase = true)

    if (isTdds) { vm.isImportingPreset = true; vm.skipDraftRestore = true; vm.presetOperationHandler.handleExternalPresetImport(dataUri) { isLoading -> vm.isImportingPreset = isLoading; if (!isLoading && activity != null) activity.intent = null } }
    if (isZip) { vm.skipDraftRestore = true; vm.packageOperationHandler.handleExternalPackageImport(dataUri, fileName) { isLoading -> vm.isImportingPreset = isLoading; if (!isLoading && activity != null) activity.intent = null } }
}
