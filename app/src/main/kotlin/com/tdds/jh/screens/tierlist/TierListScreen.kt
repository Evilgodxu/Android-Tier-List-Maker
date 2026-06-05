package com.tdds.jh.screens.tierlist

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast

import java.io.File
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background

import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberDraggableState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.compose.runtime.key
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import kotlin.math.abs
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import com.tdds.jh.ui.theme.LocalExtendedColors
import com.tdds.jh.data.tierlist.PackageItem
import com.tdds.jh.data.tierlist.PresetManager
import com.tdds.jh.data.tierlist.PresetData
import com.tdds.jh.screens.tierlist.components.ManagePresetsDialog
import com.tdds.jh.screens.tierlist.components.PresetNameDialog
import com.tdds.jh.screens.tierlist.components.PresetListDialog
import com.tdds.jh.screens.tierlist.components.LoadingDialog
import com.tdds.jh.screens.tierlist.components.ImportOverwriteDialog
import com.tdds.jh.screens.tierlist.components.DraftRestoreDialog
import com.tdds.jh.model.tierlist.TierImage
import com.tdds.jh.model.tierlist.TierItem
import com.tdds.jh.data.tierlist.generateTierListBitmap
import com.tdds.jh.R

import com.tdds.jh.screens.tierlist.components.dialogs.AboutDialog
import com.tdds.jh.screens.tierlist.components.dialogs.PreviewDialog
import com.tdds.jh.screens.tierlist.components.dialogs.ColorPickerDialog
import com.tdds.jh.screens.tierlist.components.dialogs.EditAuthorDialog
import com.tdds.jh.screens.tierlist.components.dialogs.EditTierNameDialog
import com.tdds.jh.screens.tierlist.components.dialogs.EditTitleDialog
import com.tdds.jh.screens.tierlist.components.dialogs.ProgramSettingsDialog
import com.tdds.jh.screens.tierlist.components.dialogs.LanguageSelectionDialog
import com.tdds.jh.screens.tierlist.components.ResourceManageDialog
import com.tdds.jh.screens.tierlist.components.SettingsMenuDialog
import com.tdds.jh.screens.tierlist.components.ManagePackagesDialog
import com.tdds.jh.screens.tierlist.components.PackageConfirmDialog
import com.tdds.jh.screens.tierlist.components.ImportPackageDialog
import com.tdds.jh.screens.tierlist.components.ZipPasswordDialog
import com.tdds.jh.screens.tierlist.components.ImportTargetDialog
import com.tdds.jh.screens.tierlist.components.InstructionsDialog
import com.tdds.jh.screens.tierlist.components.ImageActionDialog
import com.tdds.jh.screens.tierlist.components.ImageViewDialog
import com.tdds.jh.screens.tierlist.components.MoveImageDialog
import com.tdds.jh.screens.tierlist.components.EditImageNameDialog
import com.tdds.jh.screens.tierlist.components.SetBadgeDialog
import com.tdds.jh.screens.tierlist.components.BadgePreviewArea
import com.tdds.jh.screens.tierlist.components.ImageCropDialog
import com.tdds.jh.screens.tierlist.components.CropState
import com.tdds.jh.screens.tierlist.components.SwipeableTierRow
import com.tdds.jh.screens.tierlist.components.DraggableImage
import com.tdds.jh.screens.tierlist.components.DraggablePendingImageItem
import com.tdds.jh.screens.tierlist.components.FloatingDragImage
import com.tdds.jh.screens.tierlist.components.AddTierButton
import com.tdds.jh.screens.tierlist.components.AuthorInfoSection
import com.tdds.jh.screens.tierlist.components.TierListDialogs
import com.tdds.jh.screens.tierlist.state.DialogState
import com.tdds.jh.screens.tierlist.state.DialogHandlers
import com.tdds.jh.screens.tierlist.logic.usecases.ImagePickerHandler
import com.tdds.jh.screens.tierlist.logic.usecases.PresetOperationHandler
import com.tdds.jh.screens.tierlist.logic.usecases.PackageOperationHandler
import com.tdds.jh.screens.tierlist.logic.usecases.rememberImagePickerHandler
import com.tdds.jh.screens.tierlist.logic.usecases.rememberPresetOperationHandler
import com.tdds.jh.screens.tierlist.logic.usecases.rememberPackageOperationHandler
import com.tdds.jh.screens.tierlist.components.PendingImagesSection
import com.tdds.jh.model.tierlist.PresetOperation
import com.tdds.jh.model.tierlist.TierListConfig
import com.tdds.jh.screens.tierlist.service.SettingsService
import com.tdds.jh.screens.tierlist.logic.utils.saveBitmapToGallery
import com.tdds.jh.screens.tierlist.logic.utils.shareBitmap
import com.tdds.jh.screens.tierlist.logic.utils.saveTierListImage
import com.tdds.jh.screens.tierlist.logic.utils.PermissionUtils
import com.tdds.jh.screens.tierlist.logic.utils.withStoragePermission
import com.tdds.jh.screens.tierlist.logic.utils.ImageOperationUtils
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.ArrowDropDown
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.FilterChip
import androidx.compose.ui.graphics.BlendMode

import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.LocalTextStyle
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat

import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.CachePolicy
import com.tdds.jh.ui.theme.MyApplicationTheme
import com.tdds.jh.ui.toast.showToastWithoutIcon
import com.tdds.jh.screens.tierlist.logic.utils.ColorUtils
import com.tdds.jh.screens.tierlist.logic.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.roundToInt

// 手势类型枚举
private enum class GestureType {
    LongPress,      // 长按
    VerticalDrag,   // 垂直拖动
    HorizontalSwipe // 水平滑动
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TierListMakerApp(
    isDarkTheme: Boolean = false,
    followSystemTheme: Boolean = true,
    disableCustomFont: Boolean = false,
    onDisableCustomFontChange: ((Boolean) -> Unit)? = null,
    onThemeChange: (Boolean) -> Unit = {},
    onFollowSystemThemeChange: ((Boolean) -> Unit)? = null,
    onRegisterSaveDraftCallback: ((() -> Unit) -> Unit)? = null,
    onSaveDraftForResourceManager: (() -> Unit)? = null,
    onSkipDraftSave: (() -> Unit)? = null,
    onResumeDraftSave: (() -> Unit)? = null,
    onExitApp: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()

    // 设置服务
    val settingsService = remember { SettingsService(context) }

    // 默认梯度模板 - 根据语言选择
    val currentLocale = context.resources.configuration.locales[0]
    val isChinese = currentLocale.language == "zh"

    val defaultTiers = TierListConfig.getDefaultTiers(isChinese)

    // 当前梯度列表
    val tiers = remember { mutableStateListOf<TierItem>().apply { addAll(defaultTiers) } }

    // 每个梯度的图片
    val tierImages = remember { mutableStateListOf<TierImage>() }

    // 程序设置状态
    var disableClickAdd by remember { mutableStateOf(settingsService.disableClickAdd) }
    // 调节浮显：水平偏移 0-300dp（默认125），垂直偏移 0-150dp（默认85）
    var floatOffsetX by remember { mutableStateOf(settingsService.floatOffsetX) }
    var floatOffsetY by remember { mutableStateOf(settingsService.floatOffsetY) }
    // 外置小图开关：启用时小图标显示在图片右侧
    var externalBadgeEnabled by remember { mutableStateOf(settingsService.externalBadgeEnabled) }
    // 下置命名开关：启用时图片命名显示在图片下方
    var nameBelowImage by remember { mutableStateOf(settingsService.nameBelowImage) }

    // 待添加的图片（允许重复URI，通过哈希查重实现文件复用）
    var pendingImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    
    // ZIP导入的待添加图片
    var zipPendingImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var pendingZipUri by remember { mutableStateOf<Uri?>(null) }
    
    // 图包管理状态
    var selectedPackage by remember { mutableStateOf<PackageItem.Imported?>(null) }
    var selectedPackageImageCount by remember { mutableStateOf(0) }
    var isImportingPackage by remember { mutableStateOf(false) }  // 防止重复导入
    var isExportingPackage by remember { mutableStateOf(false) }  // 图包导出状态

    // 标题状态
    var tierListTitle by remember { mutableStateOf(context.getString(R.string.default_title)) }

    // 作者信息状态
    var authorName by remember { mutableStateOf("") }

    // 语言设置状态
    val shouldShowLanguageOnFirstLaunch = settingsService.showLanguageOnFirstLaunch
    var currentLanguage by remember { mutableStateOf(settingsService.currentLanguage) }
    var languageChanged by remember { mutableStateOf(false) }

    // 语言切换 - 使用 recreate 重启 Activity 来应用新语言
    LaunchedEffect(languageChanged) {
        if (languageChanged) {
            languageChanged = false
            // 保存语言设置
            settingsService.saveLanguage(currentLanguage)
            // 重启 Activity 以应用新语言
            (context as ComponentActivity).recreate()
        }
    }

    // 对话框状态（使用新的 DialogState 集中管理）
    val dialogState = remember { DialogState() }

    // 拖拽选中状态
    var selectedImageForDrag by remember { mutableStateOf<TierImage?>(null) }
    
    // 层级图片拖拽状态
    var draggingTierImage by remember { mutableStateOf<TierImage?>(null) }
    var draggingTierImagePosition by remember { mutableStateOf(Offset.Zero) }
    var draggingTierImageTarget by remember { mutableStateOf<String?>(null) }
    
    // 待分级区位置
    var pendingSectionRect by remember { mutableStateOf<android.graphics.Rect?>(null) }

    // 层级位置跟踪（用于拖放）- 必须在 presetFilePicker 之前定义
    var tierRowPositions by remember { mutableStateOf<Map<String, android.graphics.Rect>>(emptyMap()) }

    // 删除层级对话框状态
    var tierToDelete by remember { mutableStateOf<TierItem?>(null) }

    // 预设管理器
    val presetManager = remember { PresetManager(context) }

    // 预设管理状态
    var pendingPresetName by remember { mutableStateOf("") }
    var isExportingPreset by remember { mutableStateOf(false) }
    var isImportingPreset by remember { mutableStateOf(false) }
    var isSavingPreset by remember { mutableStateOf(false) }
    var presetOperation by remember { mutableStateOf<PresetOperation?>(null) }

    // 草稿恢复状态
    // 只存储草稿配置数据，不解压图片，等待用户确认后再解压
    var draftConfigData by remember { mutableStateOf<PresetData?>(null) }
    // 是否跳过草稿恢复（用于外部导入预设时）
    var skipDraftRestore by remember { mutableStateOf(false) }

    // 初始化 Handler 类（必须在 LaunchedEffect 之前）
    val imagePickerHandler = rememberImagePickerHandler(
        scope = scope,
        dialogState = dialogState,
        presetManager = presetManager,
        tierImages = tierImages,
        onPendingImagesChange = { pendingImages = it },
        onResumeDraftSave = onResumeDraftSave
    )

    val presetOperationHandler = rememberPresetOperationHandler(
        scope = scope,
        dialogState = dialogState,
        presetManager = presetManager,
        settingsService = settingsService,
        tiers = tiers,
        tierImages = tierImages,
        onPendingImagesChange = { pendingImages = it },
        onTitleChange = { tierListTitle = it },
        onAuthorChange = { authorName = it },
        onTierRowPositionsReset = { tierRowPositions = emptyMap() },
        onResumeDraftSave = onResumeDraftSave,
        onSkipDraftSave = onSkipDraftSave,
        showToast = { message, duration -> showToastWithoutIcon(context, message, duration) }
    )

    val packageOperationHandler = rememberPackageOperationHandler(
        scope = scope,
        dialogState = dialogState,
        presetManager = presetManager,
        onPendingImagesChange = { pendingImages = it },
        onSkipDraftSave = onSkipDraftSave,
        onResumeDraftSave = onResumeDraftSave,
        showToast = { message, duration -> showToastWithoutIcon(context, message, duration) }
    )

    // 设置待分级图片提供者
    imagePickerHandler.setPendingImagesProvider { pendingImages }

    // 检查是否是从外部打开 .tdds 或 .zip 文件
    LaunchedEffect(Unit) {
        val activity = context as? ComponentActivity
        activity?.intent?.let { intent ->
            // 处理 VIEW 动作（文件管理器等）
            if (intent.action == Intent.ACTION_VIEW) {
                val dataUri = intent.data
                if (dataUri != null) {
                    // 使用 FileUtils 获取文件名来检查扩展名
                    val fileName = FileUtils.getFileNameFromUri(context, dataUri)
                    val uriString = dataUri.toString()
                    val isTddsFile = (fileName?.endsWith(".tdds", ignoreCase = true) == true) ||
                            uriString.endsWith(".tdds", ignoreCase = true) ||
                            uriString.contains(".tdds", ignoreCase = true)
                    val isZipFile = (fileName?.endsWith(".zip", ignoreCase = true) == true) ||
                            uriString.endsWith(".zip", ignoreCase = true) ||
                            uriString.contains(".zip", ignoreCase = true)
                    
                    if (isTddsFile) {
                        isImportingPreset = true
                        skipDraftRestore = true
                        try {
                            val importResult = withContext(Dispatchers.IO) {
                                presetManager.importPreset(dataUri)
                            }
                            when (importResult.status) {
                                PresetManager.ImportStatus.SUCCESS,
                                PresetManager.ImportStatus.ALREADY_EXISTS -> {
                                    val result = withContext(Dispatchers.IO) {
                                        presetManager.applyPreset(importResult.presetFile)
                                    }
                                    tiers.clear()
                                    tiers.addAll(result.tiers.map { tierData ->
                                        TierItem(tierData.label, try {
                                            Color(android.graphics.Color.parseColor("#${tierData.color}"))
                                        } catch (e: Exception) { Color.Gray })
                                    })
                                    tierImages.removeAll { true }
                                    result.tierImages.forEach { appliedImage ->
                                        tierImages.add(TierImage(
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
                                        ))
                                    }
                                    pendingImages = result.pendingImages
                                    tierListTitle = result.title
                                    authorName = result.author
                                    settingsService.cropPositionX = result.cropPositionX
                                    settingsService.cropPositionY = result.cropPositionY
                                    settingsService.customCropWidth = result.customCropWidth
                                    settingsService.customCropHeight = result.customCropHeight
                                    settingsService.useCustomCropSize = result.useCustomCropSize
                                    settingsService.cropRatio = result.cropRatio
                                    // 清理草稿文件（保留工作目录中的图片）
                                    presetManager.cleanupDraftOnly()
                                    showToastWithoutIcon(
                                        context,
                                        context.getString(R.string.preset_import_success),
                                        Toast.LENGTH_SHORT
                                    )

                                }
                                PresetManager.ImportStatus.NEEDS_OVERWRITE -> {
                                    dialogState.pendingImportResult = importResult
                                    dialogState.showImportOverwriteDialog = true
                                }
                            }
                        } catch (e: Exception) {
                            showToastWithoutIcon(
                                context,
                                context.getString(R.string.preset_import_failed, e.message),
                                Toast.LENGTH_SHORT
                            )
                        } finally {
                            isImportingPreset = false
                        }
                        // 清除 intent 避免重复处理
                        activity.intent = null
                        return@LaunchedEffect
                    }
                    
                    // 处理 .zip 图包文件
                    if (isZipFile) {
                        skipDraftRestore = true
                        packageOperationHandler.handleExternalPackageImport(dataUri, fileName) { isLoading ->
                            isImportingPreset = isLoading
                            if (!isLoading) {
                                activity.intent = null
                            }
                        }
                        return@LaunchedEffect
                    }
                }
            }

            // 处理 SEND 动作（QQ等应用分享）
            if (intent.action == Intent.ACTION_SEND || intent.action == Intent.ACTION_SEND_MULTIPLE) {
                val dataUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                if (dataUri != null) {
                    // 使用 FileUtils 获取文件名来检查扩展名
                    val fileName = FileUtils.getFileNameFromUri(context, dataUri)
                    val uriString = dataUri.toString()
                    val isTddsFile = (fileName?.endsWith(".tdds", ignoreCase = true) == true) ||
                            uriString.endsWith(".tdds", ignoreCase = true) ||
                            uriString.contains(".tdds", ignoreCase = true)
                    val isZipFile = (fileName?.endsWith(".zip", ignoreCase = true) == true) ||
                            uriString.endsWith(".zip", ignoreCase = true) ||
                            uriString.contains(".zip", ignoreCase = true)
                    
                    if (isTddsFile) {
                        isImportingPreset = true
                        skipDraftRestore = true
                        presetOperationHandler.handleExternalPresetImport(dataUri) { isLoading ->
                            isImportingPreset = isLoading
                            if (!isLoading) {
                                activity.intent = null
                            }
                        }
                        return@LaunchedEffect
                    }

                    // 处理 .zip 图包文件
                    if (isZipFile) {
                        skipDraftRestore = true
                        packageOperationHandler.handleExternalPackageImport(dataUri, fileName) { isLoading ->
                            isImportingPreset = isLoading
                            if (!isLoading) {
                                activity.intent = null
                            }
                        }
                        return@LaunchedEffect
                    }
                }
            }
        }
    }

    // 检查是否存在草稿（仅在当前没有编辑内容时显示恢复对话框）
    LaunchedEffect(Unit) {
        // 如果跳过草稿恢复标志已设置，直接返回
        if (skipDraftRestore) {
            return@LaunchedEffect
        }
        if (presetManager.hasDraft()) {
            // 检查当前是否已经有编辑内容
            val hasCurrentContent = tierListTitle != context.getString(R.string.default_title) ||
                    authorName.isNotEmpty() ||
                    tierImages.isNotEmpty() ||
                    pendingImages.isNotEmpty() ||
                    tiers.size != defaultTiers.size ||
                    tiers.zip(defaultTiers).any { (current, default) ->
                        current.label != default.label || current.color != default.color
                    }

            // 只有当前没有编辑内容时才显示恢复对话框
            if (!hasCurrentContent) {
                // 只读取草稿配置，不解压图片（等待用户确认后再解压）
                val draftConfig = presetManager.readDraftConfig()
                if (draftConfig != null) {
                    draftConfigData = draftConfig
                    dialogState.showDraftRestoreDialog = true
                }
            } else {
                // 清理草稿，因为用户正在编辑新内容
                presetManager.cleanupDraft()
            }
        }
    }

    // 注册草稿保存回调到 Activity
    DisposableEffect(Unit) {
        onRegisterSaveDraftCallback?.invoke {
            // 检查是否有编辑内容（非默认状态）
            val hasContent = tierListTitle != context.getString(R.string.default_title) ||
                    authorName.isNotEmpty() ||
                    tierImages.isNotEmpty() ||
                    pendingImages.isNotEmpty() ||
                    tiers.size != defaultTiers.size ||
                    tiers.zip(defaultTiers).any { (current, default) ->
                        current.label != default.label || current.color != default.color
                    }

            if (hasContent) {
                // 使用 Handler 保存草稿
                presetOperationHandler.saveDraft(tierListTitle, authorName, pendingImages)
            } else {
                // 没有内容，清理草稿
                presetManager.cleanupDraft()
            }
        }
        onDispose { }
    }

    // 预设文件选择器
    val presetFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        presetOperationHandler.handleImportPreset(uri, pendingImages)
    }

    // 预设导出文件创建器
    val presetExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        presetOperationHandler.handleExportPreset(
            uri = uri,
            presetName = pendingPresetName,
            tierListTitle = tierListTitle,
            authorName = authorName,
            currentPendingImages = pendingImages
        )
    }

    // 图包导出文件创建器
    val packageExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        packageOperationHandler.handleExportPackage(uri, dialogState.packageToExport)
    }

    // 待添加图片拖动状态
    var isDraggingPendingImage by remember { mutableStateOf(false) }
    var draggedPendingImageUri by remember { mutableStateOf<Uri?>(null) }

    // 图片选择器（多选）- 支持WebP转换和哈希查重
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 50)
    ) { uris ->
        imagePickerHandler.handleImagePickerResult(uris, pendingImages)
    }

    // 图片选择器（多选，用于添加到待分级区域）- 支持WebP转换和哈希查重
    val addToPendingPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 50)
    ) { uris ->
        imagePickerHandler.handleAddToPendingResult(uris, pendingImages)
    }

    // 图片选择器（单选，用于替换）
    val replaceImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        imagePickerHandler.handleReplaceImageResult(uri, dialogState.imageToReplace)
    }

    // 图片选择器（单选，用于选择小图标设置到具体槽位）
    val badgeImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        imagePickerHandler.handleBadgeImagePickerResult(
            uri,
            dialogState.badgeSelectionTarget,
            dialogState.imageForBadge
        ) {
            dialogState.badgeDialogRefreshKey++
        }
    }

    // 图片选择器（多选，用于批量添加小图标到工作目录，最多20张）
    val badgeImagePickerMultiple = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20)
    ) { uris ->
        imagePickerHandler.handleBadgeImagePickerMultipleResult(uris) {
            dialogState.badgeDialogRefreshKey++
        }
    }

    // 权限申请（不自动打开图片选择器）
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // 权限申请完成后，如果是首次启动则显示语言选择对话框
        if (shouldShowLanguageOnFirstLaunch) {
            dialogState.showLanguageDialog = true
            // 标记已显示过语言选择对话框
            settingsService.showLanguageOnFirstLaunch = false
        }
        // 重置所有防重复点击状态（权限申请完成后，无论成功与否）
        dialogState.isBadgePickerLaunching = false
        dialogState.isImagePickerLaunching = false
    }

    // 应用启动时检查权限（仅申请权限，不自动打开图片选择器）
    LaunchedEffect(Unit) {
        if (!PermissionUtils.hasStoragePermission(context)) {
            PermissionUtils.requestStoragePermission(permissionLauncher)
        } else {
            // 已有权限，如果是首次启动则显示语言选择对话框
            if (shouldShowLanguageOnFirstLaunch) {
                dialogState.showLanguageDialog = true
                // 标记已显示过语言选择对话框
                settingsService.showLanguageOnFirstLaunch = false
            }
        }
    }

    // 对话框事件处理器（必须在 permissionLauncher 和 imagePicker 等定义之后初始化）
    val dialogHandlers = remember {
        DialogHandlers(
            context = context,
            dialogState = dialogState,
            scope = scope,
            settingsService = settingsService,
            presetManager = presetManager,
            tierImages = tierImages,
            onTierImagesChange = { /* tierImages 是 mutableStateList，变更会自动触发重组 */ },
            onPendingImagesChange = { pendingImages = it },
            onSkipDraftSave = onSkipDraftSave,
            onResumeDraftSave = onResumeDraftSave,
            imagePicker = imagePicker,
            replaceImagePicker = replaceImagePicker,
            launchBadgePicker = { target ->
                if (!dialogState.isBadgePickerLaunching) {
                    dialogState.isBadgePickerLaunching = true
                    dialogState.badgeSelectionTarget = target
                    withStoragePermission(
                        context = context,
                        permissionLauncher = permissionLauncher,
                        onSkipDraftSave = onSkipDraftSave
                    ) {
                        badgeImagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                }
            },
            launchBadgePickerForAdding = {
                if (!dialogState.isBadgePickerLaunching) {
                    dialogState.isBadgePickerLaunching = true
                    dialogState.badgeSelectionTarget = 0 // 0 表示添加到工作目录，不设置到具体槽位
                    withStoragePermission(
                        context = context,
                        permissionLauncher = permissionLauncher,
                        onSkipDraftSave = onSkipDraftSave
                    ) {
                        badgeImagePickerMultiple.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                }
            },
            deleteBadge = { target ->
                val index = tierImages.indexOfFirst { it.id == dialogState.imageForBadge?.id }
                if (index != -1) {
                    tierImages[index] = when (target) {
                        1 -> tierImages[index].copy(badgeUri = null)
                        2 -> tierImages[index].copy(badgeUri2 = null)
                        3 -> tierImages[index].copy(badgeUri3 = null)
                        else -> tierImages[index]
                    }
                    dialogState.imageForBadge = tierImages[index]
                }
            },
            deleteBadgeFile = { badgeUri, pm ->
                try {
                    val path = badgeUri.path ?: return@DialogHandlers false
                    val file = File(path)
                    if (file.exists()) {
                        file.delete()
                    } else {
                        false
                    }
                } catch (e: Exception) {
                    false
                }
            }
        )
    }

    // 双击返回退出应用
    var backPressedTime by remember { mutableLongStateOf(0L) }
    BackHandler {
        val currentTime = System.currentTimeMillis()
        if (currentTime - backPressedTime > 2000) {
            backPressedTime = currentTime
        } else {
            // 双击退出：保存草稿并清理资源
            onExitApp?.invoke()
        }
    }

    val extendedColors = LocalExtendedColors.current

    // 草稿恢复对话框
    if (dialogState.showDraftRestoreDialog && draftConfigData != null) {
        DraftRestoreDialog(
            title = draftConfigData!!.title,
            author = draftConfigData!!.author,
            onDismiss = {
                // 取消恢复，清理草稿
                presetManager.cleanupDraft()
                dialogState.showDraftRestoreDialog = false
                draftConfigData = null
            },
            onRestore = {
                dialogState.showDraftRestoreDialog = false
                dialogState.showDraftLoadingDialog = true
                presetOperationHandler.restoreDraft(draftConfigData!!) { isLoading ->
                    dialogState.showDraftLoadingDialog = isLoading
                    if (!isLoading) {
                        draftConfigData = null
                    }
                }
            }
        )
    }

    // 草稿加载中对话框
    if (dialogState.showDraftLoadingDialog) {
        LoadingDialog(message = stringResource(R.string.loading_resources))
    }

    Scaffold(
        containerColor = extendedColors.background,
        topBar = {
            // 使用自定义布局实现标题真正居中
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(extendedColors.background)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                // 左侧按钮组
                Row(
                    modifier = Modifier.align(Alignment.CenterStart),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 主题切换按钮
                    IconButton(onClick = {
                        val newTheme = !isDarkTheme
                        onThemeChange(newTheme)
                    }) {
                        // 当前深色主题显示太阳(切换到浅色),当前浅色主题显示月亮(切换到深色)
                        val iconRes = if (isDarkTheme) R.drawable.ic_sun_light else R.drawable.ic_moon_light
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = if (isDarkTheme) stringResource(R.string.switch_to_light_theme) else stringResource(R.string.switch_to_dark_theme),
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // 居中的标题
                Text(
                    text = "-$tierListTitle-",
                    modifier = Modifier.clickable { dialogState.showEditTitleDialog = true },
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleLarge
                )

                // 右侧按钮组
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 资源管理按钮
                    IconButton(onClick = { dialogState.showResourceManageDialog = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_resource_manage),
                            contentDescription = stringResource(R.string.resource_management),
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    // 功能菜单按钮
                    IconButton(onClick = { dialogState.showSettingsMenu = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_menu_light),
                            contentDescription = stringResource(R.string.settings),
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        bottomBar = {
            // 底部按钮栏 - 现代化设计
            val buttonHeight = 48.dp
            val buttonFontSize = 16.sp
            val horizontalPadding = 16.dp
            val verticalPadding = 12.dp
            val buttonSpacing = 12.dp
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(extendedColors.background)
                    .padding(horizontal = horizontalPadding, vertical = verticalPadding)
                    .windowInsetsPadding(WindowInsets.navigationBars),
                horizontalArrangement = Arrangement.spacedBy(buttonSpacing, Alignment.CenterHorizontally)
            ) {
                // 保存按钮 - 使用轮廓样式
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            dialogState.previewIsDarkTheme = isDarkTheme
                            val bitmap = generateTierListBitmap(context, tiers, tierImages, tierListTitle, authorName, dialogState.previewIsDarkTheme, externalBadgeEnabled, disableCustomFont, nameBelowImage)
                            dialogState.previewBitmap = bitmap
                            dialogState.showPreviewDialog = true
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(buttonHeight),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.outline
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(
                        text = stringResource(R.string.save),
                        fontSize = buttonFontSize,
                        fontWeight = FontWeight.Medium
                    )
                }

                // 重置按钮 - 使用轮廓样式
                OutlinedButton(
                    onClick = {
                        // 防止重复点击
                        if (dialogState.isResetting) return@OutlinedButton
                        
                        // 检查是否已经是默认状态（默认模板且无图片）
                        val isDefaultState = tierImages.isEmpty() && 
                                            tiers.size == defaultTiers.size &&
                                            tiers.zip(defaultTiers).all { (current, default) ->
                                                current.label == default.label && current.color == default.color
                                            }
                        
                        if (isDefaultState) {
                            // 已经是默认状态,直接返回不执行任何操作
                            return@OutlinedButton
                        }
                        
                        dialogState.isResetting = true
                        
                        // 将层级中的图片返回到待分级区域（原样返回所有图片，包括重复的）
                        val imagesToReturn = tierImages.map { it.originalUri ?: it.uri }
                        
                        // 重置层级为默认模板
                        tiers.clear()
                        tiers.addAll(defaultTiers)
                        tierImages.clear()
                        // 清空层级位置信息,确保使用默认模板的层级标签
                        tierRowPositions = emptyMap()
                        // 清理裁剪设置
                        settingsService.clearCropSettings()

                        // 将层级图片添加到待分级区域（原样返回所有图片，包括重复的）
                        if (imagesToReturn.isNotEmpty()) {
                            pendingImages = pendingImages + imagesToReturn
                        }

                        tierListTitle = context.getString(R.string.default_title)
                        authorName = ""
                        showToastWithoutIcon(context, context.getString(R.string.reset_success))
                        
                        // 延迟重置状态，防止快速连续点击
                        scope.launch {
                            delay(500)
                            dialogState.isResetting = false
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(buttonHeight),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.outline
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(
                        text = stringResource(R.string.reset),
                        fontSize = buttonFontSize,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(extendedColors.background)
                .padding(innerPadding)
        ) {
            // 待添加图片区域（固定显示）
            PendingImagesSection(
                images = pendingImages,
                tiers = tiers,
                tierRowPositions = tierRowPositions,
                onClear = {
                        // 清理待添加图片的资源文件（保留层级中正在使用的图片）
                        val clearedCount = pendingImages.size
                        try {
                            val tierImageUris = tierImages.map { it.uri }
                            var cleanedCount = 0
                            pendingImages.forEach { uri ->
                                // 只清理不在层级中使用的图片
                                if (uri !in tierImageUris) {
                                    val fileName = uri.lastPathSegment
                                    if (fileName != null && (fileName.startsWith("imported_") || fileName.startsWith("builtin_"))) {
                                        val file = File(context.filesDir, fileName)
                                        if (file.exists() && file.delete()) {
                                            cleanedCount++
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                        }
                        pendingImages = emptyList()
                        // 显示清空提示
                        if (clearedCount > 0) {
                            showToastWithoutIcon(context, context.getString(R.string.images_cleared, clearedCount))
                        }
                    },
                onAdd = {
                    // 打开图片选择器添加图片到待分级区域
                    if (!dialogState.isImagePickerLaunching) {
                        dialogState.isImagePickerLaunching = true
                        withStoragePermission(
                            context = context,
                            permissionLauncher = permissionLauncher,
                            onSkipDraftSave = onSkipDraftSave
                        ) {
                            addToPendingPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    }
                },
                onDragStart = { uri ->
                    isDraggingPendingImage = true
                    draggedPendingImageUri = uri
                },
                onDragEnd = {
                    isDraggingPendingImage = false
                    draggedPendingImageUri = null
                },
                onDropOnTier = { uri, tierLabel ->
                    tierImages.add(TierImage(UUID.randomUUID().toString(), tierLabel, uri))
                    // 只移除待分级区域中的一个匹配URI（支持同一URI的多张图片）
                    val index = pendingImages.indexOfFirst { it == uri }
                    if (index != -1) {
                        pendingImages = pendingImages.toMutableList().apply { removeAt(index) }
                    }
                },
                onDeleteImage = { uri ->
                    // 从待选区删除图片（只移除一个匹配项，支持同一URI的多张图片）
                    val index = pendingImages.indexOfFirst { it == uri }
                    if (index != -1) {
                        pendingImages = pendingImages.toMutableList().apply { removeAt(index) }
                    }
                    // 清理资源文件
                    try {
                        val fileName = uri.lastPathSegment
                        if (fileName != null && (fileName.startsWith("imported_") || fileName.startsWith("builtin_"))) {
                            val file = File(context.filesDir, fileName)
                            if (file.exists()) {
                                file.delete()
                            }
                        }
                    } catch (e: Exception) {
                    }
                },
                floatOffsetX = floatOffsetX,
                floatOffsetY = floatOffsetY,
                onPositionUpdate = { rect ->
                    pendingSectionRect = rect
                }
            )
            
            // 梯度表主体
            val listState = rememberLazyListState()
            val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
                // 交换层级位置
                val fromIndex = from.index
                val toIndex = to.index
                if (fromIndex in tiers.indices && toIndex in tiers.indices) {
                    val tier = tiers.removeAt(fromIndex)
                    tiers.add(toIndex, tier)
                }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                itemsIndexed(tiers, key = { _, tier -> tier.label }) { index, tier ->
                    val tierImageList = tierImages.filter { it.tierLabel == tier.label }
                    ReorderableItem(reorderableState, key = tier.label) { isDragging ->
                        SwipeableTierRow(
                            tier = tier,
                            isDragging = isDragging,
                            images = tierImageList,
                        pendingImages = if (isDraggingPendingImage) emptyList() else pendingImages,
                        onTierClick = {
                            dialogState.editingTier = tier
                            dialogState.showEditNameDialog = true
                        },
                        onTierLongClick = {
                            // 长按功能未实现，仅作为占位回调
                        },
                        onTierDoubleClick = {
                            dialogState.editingTier = tier
                            dialogState.showColorPickerDialog = true
                        },
                        onAddImage = { uri ->
                            // 正常点击模式
                            tierImages.add(TierImage(UUID.randomUUID().toString(), tier.label, uri))
                            // 只移除待分级区域中的一个匹配URI（支持同一URI的多张图片）
                            val index = pendingImages.indexOfFirst { it == uri }
                            if (index != -1) {
                                pendingImages = pendingImages.toMutableList().apply { removeAt(index) }
                            }
                        },
                        onPositionUpdate = { tierLabel, rect ->
                            tierRowPositions = tierRowPositions + (tierLabel to rect)
                        },
                        selectedImageForDrag = selectedImageForDrag,
                        onImageClick = { image, imgIndex ->
                            if (selectedImageForDrag == null) {
                                // 没有选中图片，单击打开操作对话框
                                dialogState.selectedImageForAction = image
                                dialogState.showImageActionDialog = true
                            } else if (selectedImageForDrag!!.id == image.id) {
                                // 单击已选中的图片，取消选中
                                selectedImageForDrag = null
                            } else {
                                // 已选中图片，单击其他图片交换位置
                                ImageOperationUtils.swapImageContents(
                                    tierImages = tierImages,
                                    fromId = selectedImageForDrag!!.id,
                                    toId = image.id,
                                    onImageForActionUpdate = { updatedImage ->
                                        if (dialogState.selectedImageForAction?.id == updatedImage.id) {
                                            dialogState.selectedImageForAction = updatedImage
                                        }
                                    },
                                    onImageToReplaceUpdate = { updatedImage ->
                                        if (dialogState.imageToReplace?.id == updatedImage.id) {
                                            dialogState.imageToReplace = updatedImage
                                        }
                                    },
                                    onImageForBadgeUpdate = { updatedImage ->
                                        if (dialogState.imageForBadge?.id == updatedImage.id) {
                                            dialogState.imageForBadge = updatedImage
                                        }
                                    }
                                )
                                // 交换后取消选中状态
                                selectedImageForDrag = null
                            }
                        },
                        onImageLongClick = { image, imgIndex ->
                            // 长按功能未实现，仅作为占位回调
                        },
                        onImageDoubleClick = { image, imgIndex ->
                            if (selectedImageForDrag == null) {
                                // 没有选中图片，双击选中图片
                                selectedImageForDrag = image
                            } else if (selectedImageForDrag!!.id == image.id) {
                                // 双击已选中的图片，取消选中
                                selectedImageForDrag = null
                            } else {
                                // 已选中图片，双击其他图片交换位置并取消选中
                                ImageOperationUtils.swapImageContents(
                                    tierImages = tierImages,
                                    fromId = selectedImageForDrag!!.id,
                                    toId = image.id,
                                    onImageForActionUpdate = { updatedImage ->
                                        if (dialogState.selectedImageForAction?.id == updatedImage.id) {
                                            dialogState.selectedImageForAction = updatedImage
                                        }
                                    },
                                    onImageToReplaceUpdate = { updatedImage ->
                                        if (dialogState.imageToReplace?.id == updatedImage.id) {
                                            dialogState.imageToReplace = updatedImage
                                        }
                                    },
                                    onImageForBadgeUpdate = { updatedImage ->
                                        if (dialogState.imageForBadge?.id == updatedImage.id) {
                                            dialogState.imageForBadge = updatedImage
                                        }
                                    }
                                )
                                selectedImageForDrag = null
                            }
                        },
                        onDeleteTier = {
                            // 使用该层级的标签查找当前索引（避免交换图片后索引过期）
                            val currentIndex = tiers.indexOfFirst { it.label == tier.label }
                            if (currentIndex != -1) {
                                // 将该层级的所有图片返回到待放置区（原样返回所有图片，包括重复的）
                                val imagesToReturn = tierImages.filter { it.tierLabel == tier.label }
                                val returnUris = imagesToReturn.map { it.originalUri ?: it.uri }
                                if (returnUris.isNotEmpty()) {
                                    pendingImages = pendingImages + returnUris
                                }
                                val returnedCount = returnUris.size
                                val deletedCount = imagesToReturn.size
                                tiers.removeAt(currentIndex)
                                tierImages.removeAll { it.tierLabel == tier.label }
                                // 从 tierRowPositions 中移除该层级的位置信息
                                tierRowPositions = tierRowPositions - tier.label
                            }
                        },
                        onPickImage = {
                            // 防止重复打开图片选择器
                            if (!dialogState.isImagePickerLaunching) {
                                dialogState.isImagePickerLaunching = true
                                withStoragePermission(
                                    context = context,
                                    permissionLauncher = permissionLauncher,
                                    onSkipDraftSave = onSkipDraftSave
                                ) {
                                    imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }
                            }
                        },
                        disableClickAdd = disableClickAdd,
                        isDraggingPendingImage = isDraggingPendingImage,
                        onMoveSelectedImageToTier = {
                            // 移动选中的图片到当前层级
                            selectedImageForDrag?.let { selectedImage ->
                                ImageOperationUtils.moveImageToTier(
                                    tierImages = tierImages,
                                    imageId = selectedImage.id,
                                    targetTierLabel = tier.label
                                )
                                selectedImageForDrag = null
                            }
                        },
                        // 层级图片拖拽相关参数
                        tierRowPositions = tierRowPositions,
                        draggingTierImage = draggingTierImage,
                        onTierImageDragStart = { image, center ->
                            // 从 tierImages 列表中查找最新的图片对象,确保获取正确的 URI
                            val latestImage = tierImages.find { it.id == image.id } ?: image
                            draggingTierImage = latestImage
                            draggingTierImagePosition = center
                        },
                        onTierImageDrag = { center, targetTier ->
                            draggingTierImagePosition = center
                            draggingTierImageTarget = targetTier
                        },
                        onTierImageDragEnd = { image, targetTier ->
                            val index = tierImages.indexOfFirst { it.id == image.id }
                            if (index != -1) {
                                if (targetTier != null && targetTier != image.tierLabel) {
                                // 跨层级移动
                                ImageOperationUtils.moveImageToTier(
                                    tierImages = tierImages,
                                    imageId = image.id,
                                    targetTierLabel = targetTier
                                )
                            } else if (targetTier == null) {
                                    // 检查是否拖到了待分级区域
                                    val currentRect = pendingSectionRect
                                    if (currentRect != null) {
                                        val finalCenter = draggingTierImagePosition
                                        if (finalCenter.x >= currentRect.left && finalCenter.x <= currentRect.right &&
                                            finalCenter.y >= currentRect.top && finalCenter.y <= currentRect.bottom) {
                                            // 将图片移回待分级区域
                                            val removedImage = tierImages.removeAt(index)
                                            val uriToReturn = removedImage.originalUri ?: removedImage.uri

                                            // 注意：不删除小图标文件，因为小图标是全局资源，可能被其他图片使用
                                            // 只从当前图片移除小图标引用，让图片恢复为无小图标状态

                                            // 返回图片到待分级区域
                                            pendingImages = pendingImages + uriToReturn
                                        }
                                    }
                                }
                            }
                            draggingTierImage = null
                            draggingTierImageTarget = null
                        },
                        // 列表状态用于自动滚动
                        listState = listState
                    )
                    }
                }

                // 添加新层级按钮
                item {
                    AddTierButton(
                        onClick = {
                            val newLabel = ColorUtils.generateNextLabel(tiers.map { it.label })
                            tiers.add(TierItem(newLabel, ColorUtils.generateRandomColor()))
                        }
                    )
                }

                // 作者信息输入
                item {
                    AuthorInfoSection(
                        authorName = authorName,
                        onClick = { dialogState.showEditAuthorDialog = true }
                    )
                }
            }
        }
        
        // 浮动显示的层级图片拖拽
        if (draggingTierImage != null) {
            FloatingDragImage(
                uri = draggingTierImage!!.uri,
                position = draggingTierImagePosition,
                dropTarget = draggingTierImageTarget,
                floatOffsetX = floatOffsetX,
                floatOffsetY = floatOffsetY
            )
        }
        
        // 对话框集合
        TierListDialogs(
            dialogState = dialogState,
            handlers = dialogHandlers,
            context = context,
            scope = scope,
            settingsService = settingsService,
            presetManager = presetManager,
            tierImages = tierImages,
            tiers = tiers,
            tierListTitle = tierListTitle,
            authorName = authorName,
            pendingImages = pendingImages,
            defaultTiers = defaultTiers,
            tierRowPositions = tierRowPositions,
            disableClickAdd = disableClickAdd,
            floatOffsetX = floatOffsetX,
            floatOffsetY = floatOffsetY,
            externalBadgeEnabled = externalBadgeEnabled,
            followSystemTheme = followSystemTheme,
            disableCustomFont = disableCustomFont,
            nameBelowImage = nameBelowImage,
            isDarkTheme = isDarkTheme,
            currentLanguage = currentLanguage,
            onTitleChange = { tierListTitle = it },
            onAuthorChange = { authorName = it },
            onTiersChange = { /* tiers 是 mutableStateList，变更会自动触发重组 */ },
            onTierImagesChange = { /* tierImages 是 mutableStateList，变更会自动触发重组 */ },
            onPendingImagesChange = { pendingImages = it },
            onTierRowPositionsChange = { tierRowPositions = it },
            onDisableClickAddChange = { disableClickAdd = it },
            onFloatOffsetXChange = { floatOffsetX = it },
            onFloatOffsetYChange = { floatOffsetY = it },
            onExternalBadgeChange = { externalBadgeEnabled = it },
            onFollowSystemThemeChange = onFollowSystemThemeChange,
            onDisableCustomFontChange = onDisableCustomFontChange,
            onNameBelowImageChange = { nameBelowImage = it },
            onLanguageChange = {
                currentLanguage = it
                languageChanged = true
            },
            onSkipDraftSave = onSkipDraftSave,
            onResumeDraftSave = onResumeDraftSave,
            presetExportLauncher = presetExportLauncher,
            packageExportLauncher = packageExportLauncher,
            presetFilePicker = presetFilePicker
        )

        // ==================== 外部图包密码输入对话框 ====================
        if (dialogState.showExternalPackagePasswordDialog) {
            ZipPasswordDialog(
                showError = dialogState.externalPackagePasswordError,
                onDismiss = {
                    dialogState.showExternalPackagePasswordDialog = false
                    dialogState.externalPackageUri = null
                    dialogState.externalPackageFileName = ""
                    dialogState.externalPackagePassword = null
                    dialogState.externalPackagePasswordError = false
                    isImportingPreset = false
                },
                onConfirm = { password ->
                    packageOperationHandler.continueExternalPackageImportWithPassword(password) { isLoading ->
                        isImportingPreset = isLoading
                    }
                }
            )
        }

        // 加载资源中对话框（导入/导出/保存预设/导出图包）
        if (isImportingPreset || isExportingPreset || isSavingPreset || isExportingPackage) {
            LoadingDialog(message = stringResource(R.string.loading_resources))
        }

    }
}

// ImageCropDialog 和 CropState 已移至 components/ImageCropDialog.kt


