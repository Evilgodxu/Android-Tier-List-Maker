package com.tdds.jh.screens.tierlist

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * ViewModel层：集中管理页面所有状态、Handler、Launcher和副作用
 */
@Stable
class TierListViewModel(
    val context: Context,
    val scope: CoroutineScope,
    val settingsService: SettingsService,
    val presetManager: PresetManager,
    val defaultTiers: List<TierItem>,
) {
    // ==================== 核心列表 ====================
    val tiers = mutableStateListOf<TierItem>().apply { addAll(defaultTiers) }
    val tierImages = mutableStateListOf<TierImage>()

    // ==================== 对话框状态 ====================
    val dialogState = DialogState()

    // ==================== UI状态 ====================
    var disableClickAdd by mutableStateOf(settingsService.disableClickAdd)
    var floatOffsetX by mutableStateOf(settingsService.floatOffsetX)
    var floatOffsetY by mutableStateOf(settingsService.floatOffsetY)
    var externalBadgeEnabled by mutableStateOf(settingsService.externalBadgeEnabled)
    var nameBelowImage by mutableStateOf(settingsService.nameBelowImage)

    var pendingImages by mutableStateOf<List<Uri>>(emptyList())
    var tierListTitle by mutableStateOf(context.getString(R.string.default_title))
    var authorName by mutableStateOf("")

    var currentLanguage by mutableStateOf(settingsService.currentLanguage)
    var languageChanged by mutableStateOf(false)

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

    /** 应用导入的预设数据到当前页面 */
    fun applyPresetData(result: PresetManager.ApplyPresetResult) {
        tiers.clear(); tiers.addAll(result.tiers.map { TierItem(it.label, try { Color(android.graphics.Color.parseColor("#${it.color}")) } catch (_: Exception) { Color.Gray }) })
        tierImages.clear(); tierImages.addAll(result.tierImages.map { TierImage(it.id, it.tierLabel, it.uri, it.name, it.badgeUri, it.badgeUri2, it.badgeUri3, it.cropPositionX, it.cropPositionY, it.cropScale, it.isCropped, it.originalUri, it.cropRatio, it.useCustomCrop, it.customCropWidth, it.customCropHeight) })
        pendingImages = result.pendingImages; tierListTitle = result.title; authorName = result.author
        settingsService.cropPositionX = result.cropPositionX; settingsService.cropPositionY = result.cropPositionY
        settingsService.customCropWidth = result.customCropWidth; settingsService.customCropHeight = result.customCropHeight
        settingsService.useCustomCropSize = result.useCustomCropSize; settingsService.cropRatio = result.cropRatio
        tierRowPositions = emptyMap()
    }
}

/**
 * ViewModel工厂函数
 */
@Composable
fun rememberTierListViewModel(
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

    val vm = remember {
        TierListViewModel(context, scope, settingsService, presetManager, defaultTiers)
    }

    // ==================== 语言切换副作用 ====================
    LaunchedEffect(vm.languageChanged) {
        if (vm.languageChanged) {
            vm.languageChanged = false
            vm.settingsService.saveLanguage(vm.currentLanguage)
            (context as ComponentActivity).recreate()
        }
    }

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
    LaunchedEffect(Unit) {
        val activity = context as? ComponentActivity ?: return@LaunchedEffect
        val intent = activity.intent ?: return@LaunchedEffect
        when {
            intent.action == Intent.ACTION_VIEW -> handleViewIntent(vm, activity, intent)
            intent.action == Intent.ACTION_SEND || intent.action == Intent.ACTION_SEND_MULTIPLE -> handleSendIntent(vm, activity, intent)
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

private fun handleViewIntent(vm: TierListViewModel, activity: ComponentActivity, intent: Intent) {
    val dataUri = intent.data ?: return
    val fileName = FileUtils.getFileNameFromUri(vm.context, dataUri)
    val uriString = dataUri.toString()
    val isTdds = (fileName?.endsWith(".tdds", ignoreCase = true) == true) || uriString.endsWith(".tdds", ignoreCase = true) || uriString.contains(".tdds", ignoreCase = true)
    val isZip = (fileName?.endsWith(".zip", ignoreCase = true) == true) || uriString.endsWith(".zip", ignoreCase = true) || uriString.contains(".zip", ignoreCase = true)

    if (isTdds) { vm.isImportingPreset = true; vm.skipDraftRestore = true; importTddsFile(vm, dataUri); activity.intent = null; return }
    if (isZip) { vm.skipDraftRestore = true; vm.packageOperationHandler.handleExternalPackageImport(dataUri, fileName) { isLoading -> vm.isImportingPreset = isLoading; if (!isLoading) activity.intent = null } }
}

private fun handleSendIntent(vm: TierListViewModel, activity: ComponentActivity, intent: Intent) {
    val dataUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
    else @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_STREAM) ?: return
    if (dataUri == null) return
    val fileName = FileUtils.getFileNameFromUri(vm.context, dataUri); val uriString = dataUri.toString()
    val isTdds = (fileName?.endsWith(".tdds", ignoreCase = true) == true) || uriString.endsWith(".tdds", ignoreCase = true) || uriString.contains(".tdds", ignoreCase = true)
    val isZip = (fileName?.endsWith(".zip", ignoreCase = true) == true) || uriString.endsWith(".zip", ignoreCase = true) || uriString.contains(".zip", ignoreCase = true)

    if (isTdds) { vm.isImportingPreset = true; vm.skipDraftRestore = true; vm.presetOperationHandler.handleExternalPresetImport(dataUri) { isLoading -> vm.isImportingPreset = isLoading; if (!isLoading) activity.intent = null } }
    if (isZip) { vm.skipDraftRestore = true; vm.packageOperationHandler.handleExternalPackageImport(dataUri, fileName) { isLoading -> vm.isImportingPreset = isLoading; if (!isLoading) activity.intent = null } }
}

private fun importTddsFile(vm: TierListViewModel, dataUri: Uri) {
    vm.scope.launch {
        try {
            val importResult = withContext(Dispatchers.IO) { vm.presetManager.importPreset(dataUri) }
            when (importResult.status) {
                PresetManager.ImportStatus.SUCCESS, PresetManager.ImportStatus.ALREADY_EXISTS -> {
                    val result = withContext(Dispatchers.IO) { vm.presetManager.applyPreset(importResult.presetFile) }
                    vm.applyPresetData(result); vm.presetManager.cleanupDraftOnly()
                    showToastWithoutIcon(vm.context, vm.context.getString(R.string.preset_import_success), Toast.LENGTH_SHORT)
                }
                PresetManager.ImportStatus.NEEDS_OVERWRITE -> { vm.dialogState.pendingImportResult = importResult; vm.dialogState.showImportOverwriteDialog = true }
            }
        } catch (e: Exception) { showToastWithoutIcon(vm.context, vm.context.getString(R.string.preset_import_failed, e.message), Toast.LENGTH_SHORT) }
        finally { vm.isImportingPreset = false }
    }
}
