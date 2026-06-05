package com.tdds.jh.screens.tierlist

import android.net.Uri
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import com.tdds.jh.R
import com.tdds.jh.data.tierlist.generateTierListBitmap
import com.tdds.jh.model.tierlist.TierImage
import com.tdds.jh.model.tierlist.TierItem
import com.tdds.jh.model.tierlist.TierListConfig
import com.tdds.jh.screens.tierlist.components.AddTierButton
import com.tdds.jh.screens.tierlist.components.AuthorInfoSection
import com.tdds.jh.screens.tierlist.components.DraftRestoreDialog
import com.tdds.jh.screens.tierlist.components.FloatingDragImage
import com.tdds.jh.screens.tierlist.components.LoadingDialog
import com.tdds.jh.screens.tierlist.components.PendingImagesSection
import com.tdds.jh.screens.tierlist.components.SwipeableTierRow
import com.tdds.jh.screens.tierlist.components.TierListDialogs
import com.tdds.jh.screens.tierlist.components.ZipPasswordDialog
import com.tdds.jh.screens.tierlist.logic.utils.ColorUtils
import com.tdds.jh.screens.tierlist.logic.utils.ImageOperationUtils
import com.tdds.jh.screens.tierlist.logic.utils.PermissionUtils
import com.tdds.jh.screens.tierlist.logic.utils.withStoragePermission
import com.tdds.jh.ui.adaptive.rememberWindowSizeClass
import com.tdds.jh.ui.theme.LocalExtendedColors
import androidx.window.core.layout.WindowSizeClass
import com.tdds.jh.ui.toast.showToastWithoutIcon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.io.File
import java.util.UUID

private enum class GestureType { LongPress, VerticalDrag, HorizontalSwipe }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TierListMakerApp(
    externalIntentFlow: kotlinx.coroutines.flow.Flow<android.content.Intent?> = kotlinx.coroutines.flow.emptyFlow(),
    isDarkTheme: Boolean = false,
    followSystemTheme: Boolean = true,
    disableCustomFont: Boolean = false,
    currentLanguage: String = "system",
    onDisableCustomFontChange: ((Boolean) -> Unit)? = null,
    onThemeChange: (Boolean) -> Unit = {},
    onFollowSystemThemeChange: ((Boolean) -> Unit)? = null,
    onLanguageChange: ((String) -> Unit)? = null,
    onRegisterSaveDraftCallback: ((() -> Unit) -> Unit)? = null,
    onSaveDraftForResourceManager: (() -> Unit)? = null,
    onSkipDraftSave: (() -> Unit)? = null,
    onResumeDraftSave: (() -> Unit)? = null,
    onExitApp: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val extendedColors = LocalExtendedColors.current
    val windowSizeClass = rememberWindowSizeClass()
    val isExpanded = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

    val vm = rememberTierListViewModel(
        externalIntentFlow,
        isDarkTheme, followSystemTheme, disableCustomFont,
        onDisableCustomFontChange, onThemeChange, onFollowSystemThemeChange,
        onRegisterSaveDraftCallback, onSkipDraftSave, onResumeDraftSave, onExitApp
    )

    // 双击返回退出
    var backPressedTime by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    BackHandler {
        val currentTime = System.currentTimeMillis()
        if (currentTime - backPressedTime > 2000) backPressedTime = currentTime
        else onExitApp?.invoke()
    }

    // 草稿恢复对话框
    if (vm.dialogState.showDraftRestoreDialog && vm.draftConfigData != null) {
        DraftRestoreDialog(
            title = vm.draftConfigData!!.title,
            author = vm.draftConfigData!!.author,
            onDismiss = { vm.presetManager.cleanupDraft(); vm.dialogState.showDraftRestoreDialog = false; vm.draftConfigData = null },
            onRestore = {
                vm.dialogState.showDraftRestoreDialog = false; vm.dialogState.showDraftLoadingDialog = true
                vm.presetOperationHandler.restoreDraft(vm.draftConfigData!!) { isLoading ->
                    vm.dialogState.showDraftLoadingDialog = isLoading
                    if (!isLoading) vm.draftConfigData = null
                }
            }
        )
    }
    if (vm.dialogState.showDraftLoadingDialog) LoadingDialog(message = stringResource(R.string.loading_resources))

    Scaffold(
        containerColor = extendedColors.background,
        topBar = {
            Box(
                modifier = Modifier.fillMaxWidth().background(extendedColors.background)
                    .windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = 4.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(modifier = Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onThemeChange(!isDarkTheme) }) {
                        Icon(painter = painterResource(id = if (isDarkTheme) R.drawable.ic_sun_light else R.drawable.ic_moon_light),
                            contentDescription = if (isDarkTheme) stringResource(R.string.switch_to_light_theme) else stringResource(R.string.switch_to_dark_theme),
                            modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
                Text(text = "-${vm.tierListTitle}-", modifier = Modifier.clickable { vm.dialogState.showEditTitleDialog = true },
                    color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge)
                Row(modifier = Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { vm.dialogState.showResourceManageDialog = true }) {
                        Icon(painterResource(id = R.drawable.ic_resource_manage), stringResource(R.string.resource_management), Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurface)
                    }
                    IconButton(onClick = { vm.dialogState.showSettingsMenu = true }) {
                        Icon(painterResource(id = R.drawable.ic_menu_light), stringResource(R.string.settings), Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().background(extendedColors.background)
                    .padding(horizontal = 16.dp, vertical = 12.dp).windowInsetsPadding(WindowInsets.navigationBars),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            ) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            vm.dialogState.previewIsDarkTheme = isDarkTheme
                            vm.dialogState.previewBitmap = generateTierListBitmap(context, vm.tiers, vm.tierImages, vm.tierListTitle, vm.authorName, vm.dialogState.previewIsDarkTheme, vm.externalBadgeEnabled, disableCustomFont, vm.nameBelowImage)
                            vm.dialogState.showPreviewDialog = true
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                ) { Text(stringResource(R.string.save), fontSize = 16.sp, fontWeight = FontWeight.Medium) }
                OutlinedButton(
                    onClick = {
                        if (vm.dialogState.isResetting) return@OutlinedButton
                        val currentDefaultTiers = TierListConfig.getDefaultTiers(resources.configuration.locales[0].language == "zh")
                        val isDefault = vm.tierImages.isEmpty() && vm.tiers.size == currentDefaultTiers.size &&
                                vm.tiers.zip(currentDefaultTiers).all { (c, d) -> c.label == d.label && c.color == d.color } &&
                                vm.tierListTitle == context.getString(R.string.default_title) && vm.authorName.isEmpty()
                        if (isDefault) return@OutlinedButton
                        vm.dialogState.isResetting = true
                        val imagesToReturn = vm.tierImages.map { it.originalUri ?: it.uri }
                        vm.tiers.clear(); vm.tiers.addAll(currentDefaultTiers); vm.tierImages.clear()
                        vm.tierRowPositions = emptyMap(); vm.settingsService.clearCropSettings()
                        if (imagesToReturn.isNotEmpty()) vm.pendingImages = vm.pendingImages + imagesToReturn
                        vm.tierListTitle = context.getString(R.string.default_title); vm.authorName = ""
                        showToastWithoutIcon(context, context.getString(R.string.reset_success))
                        scope.launch { delay(500); vm.dialogState.isResetting = false }
                    },
                    modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                ) { Text(stringResource(R.string.reset), fontSize = 16.sp, fontWeight = FontWeight.Medium) }
            }
        }
    ) { innerPadding ->
        val pendingSection = @Composable {
            PendingImagesSection(
                images = vm.pendingImages, tiers = vm.tiers, tierRowPositions = vm.tierRowPositions,
                onClear = {
                    vm.pendingImages.forEach { uri ->
                        val fileName = uri.lastPathSegment
                        if (fileName != null && (fileName.startsWith("imported_") || fileName.startsWith("builtin_"))) {
                            val f = File(context.filesDir, fileName); if (f.exists()) f.delete()
                        }
                    }
                    vm.pendingImages = emptyList()
                },
                onAdd = {
                    if (!vm.dialogState.isImagePickerLaunching) {
                        vm.dialogState.isImagePickerLaunching = true
                        withStoragePermission(context, vm.permissionLauncher, onSkipDraftSave) {
                            vm.addToPendingPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    }
                },
                onDragStart = { uri -> vm.isDraggingPendingImage = true; vm.draggedPendingImageUri = uri },
                onDragEnd = { vm.isDraggingPendingImage = false; vm.draggedPendingImageUri = null },
                onDropOnTier = { uri, tierLabel ->
                    vm.tierImages.add(TierImage(UUID.randomUUID().toString(), tierLabel, uri))
                    val idx = vm.pendingImages.indexOfFirst { it == uri }
                    if (idx != -1) vm.pendingImages = vm.pendingImages.toMutableList().apply { removeAt(idx) }
                },
                onDeleteImage = { uri ->
                    val idx = vm.pendingImages.indexOfFirst { it == uri }
                    if (idx != -1) vm.pendingImages = vm.pendingImages.toMutableList().apply { removeAt(idx) }
                    try {
                        val fileName = uri.lastPathSegment
                        if (fileName != null && (fileName.startsWith("imported_") || fileName.startsWith("builtin_"))) {
                            val f = File(context.filesDir, fileName); if (f.exists()) f.delete()
                        }
                    } catch (_: Exception) {}
                },
                floatOffsetX = vm.floatOffsetX, floatOffsetY = vm.floatOffsetY,
                onPositionUpdate = { rect -> vm.pendingSectionRect = rect }
            )
        }

        val listState = rememberLazyListState()
        val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
            if (from.index in vm.tiers.indices && to.index in vm.tiers.indices) {
                val tier = vm.tiers.removeAt(from.index); vm.tiers.add(to.index, tier)
            }
        }

        val tierListSection = @Composable { modifier: Modifier ->
            LazyColumn(state = listState, modifier = modifier) {
                itemsIndexed(vm.tiers, key = { _, t -> t.label }) { _, tier ->
                    val tierImageList = vm.tierImages.filter { it.tierLabel == tier.label }
                    ReorderableItem(reorderableState, key = tier.label) { isDragging ->
                        SwipeableTierRow(
                            tier = tier, isDragging = isDragging, images = tierImageList,
                            pendingImages = if (vm.isDraggingPendingImage) emptyList() else vm.pendingImages,
                            onTierClick = { vm.dialogState.editingTier = tier; vm.dialogState.showEditNameDialog = true },
                            onTierLongClick = {},
                            onTierDoubleClick = { vm.dialogState.editingTier = tier; vm.dialogState.showColorPickerDialog = true },
                            onAddImage = { uri ->
                                vm.tierImages.add(TierImage(UUID.randomUUID().toString(), tier.label, uri))
                                val idx = vm.pendingImages.indexOfFirst { it == uri }
                                if (idx != -1) vm.pendingImages = vm.pendingImages.toMutableList().apply { removeAt(idx) }
                            },
                            onPositionUpdate = { label, rect -> vm.tierRowPositions = vm.tierRowPositions + (label to rect) },
                            selectedImageForDrag = vm.selectedImageForDrag,
                            onImageClick = { image, _ ->
                                if (vm.selectedImageForDrag == null) {
                                    vm.dialogState.selectedImageForAction = image; vm.dialogState.showImageActionDialog = true
                                } else if (vm.selectedImageForDrag!!.id == image.id) vm.selectedImageForDrag = null
                                else {
                                    ImageOperationUtils.swapImageContents(vm.tierImages, vm.selectedImageForDrag!!.id, image.id,
                                        { updated -> if (vm.dialogState.selectedImageForAction?.id == updated.id) vm.dialogState.selectedImageForAction = updated },
                                        { updated -> if (vm.dialogState.imageToReplace?.id == updated.id) vm.dialogState.imageToReplace = updated },
                                        { updated -> if (vm.dialogState.imageForBadge?.id == updated.id) vm.dialogState.imageForBadge = updated })
                                    vm.selectedImageForDrag = null
                                }
                            },
                            onImageLongClick = { _, _ -> },
                            onImageDoubleClick = { image, _ ->
                                if (vm.selectedImageForDrag == null) vm.selectedImageForDrag = image
                                else if (vm.selectedImageForDrag!!.id == image.id) vm.selectedImageForDrag = null
                                else {
                                    ImageOperationUtils.swapImageContents(vm.tierImages, vm.selectedImageForDrag!!.id, image.id,
                                        { updated -> if (vm.dialogState.selectedImageForAction?.id == updated.id) vm.dialogState.selectedImageForAction = updated },
                                        { updated -> if (vm.dialogState.imageToReplace?.id == updated.id) vm.dialogState.imageToReplace = updated },
                                        { updated -> if (vm.dialogState.imageForBadge?.id == updated.id) vm.dialogState.imageForBadge = updated })
                                    vm.selectedImageForDrag = null
                                }
                            },
                            onDeleteTier = {
                                val images = vm.tierImages.filter { it.tierLabel == tier.label }
                                if (images.isNotEmpty()) vm.pendingImages = vm.pendingImages + images.map { it.originalUri ?: it.uri }
                                val curIdx = vm.tiers.indexOfFirst { it.label == tier.label }
                                if (curIdx != -1) { vm.tiers.removeAt(curIdx); vm.tierImages.removeAll { it.tierLabel == tier.label } }
                                vm.tierRowPositions = vm.tierRowPositions - tier.label
                            },
                            onPickImage = {
                                if (!vm.dialogState.isImagePickerLaunching) {
                                    vm.dialogState.isImagePickerLaunching = true
                                    withStoragePermission(context, vm.permissionLauncher, onSkipDraftSave) { vm.imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                                }
                            },
                            disableClickAdd = vm.disableClickAdd, isDraggingPendingImage = vm.isDraggingPendingImage,
                            onMoveSelectedImageToTier = {
                                vm.selectedImageForDrag?.let { img ->
                                    ImageOperationUtils.moveImageToTier(vm.tierImages, img.id, tier.label); vm.selectedImageForDrag = null
                                }
                            },
                            tierRowPositions = vm.tierRowPositions, draggingTierImage = vm.draggingTierImage,
                            onTierImageDragStart = { img, center ->
                                val latest = vm.tierImages.find { it.id == img.id } ?: img
                                vm.draggingTierImage = latest; vm.draggingTierImagePosition = center
                            },
                            onTierImageDrag = { center, target -> vm.draggingTierImagePosition = center; vm.draggingTierImageTarget = target },
                            onTierImageDragEnd = { img, target ->
                                if (target != null && target != img.tierLabel) ImageOperationUtils.moveImageToTier(vm.tierImages, img.id, target)
                                else if (target == null) {
                                    val rect = vm.pendingSectionRect
                                    if (rect != null) {
                                        val center = vm.draggingTierImagePosition
                                        if (center.x.toInt() in rect.left..rect.right && center.y.toInt() in rect.top..rect.bottom) {
                                            val idx = vm.tierImages.indexOfFirst { it.id == img.id }
                                            if (idx != -1) {
                                                val removed = vm.tierImages.removeAt(idx); vm.pendingImages = vm.pendingImages + (removed.originalUri ?: removed.uri)
                                            }
                                        }
                                    }
                                }
                                vm.draggingTierImage = null; vm.draggingTierImageTarget = null
                            },
                            listState = listState
                        )
                    }
                }
                item { AddTierButton(onClick = { vm.tiers.add(TierItem(ColorUtils.generateNextLabel(vm.tiers.map { it.label }), ColorUtils.generateRandomColor())) }) }
                item { AuthorInfoSection(authorName = vm.authorName, onClick = { vm.dialogState.showEditAuthorDialog = true }) }
            }
        }

        if (!isExpanded) {
            // Compact: 手机竖屏 — 待分级图片在顶部，层级列表在下方垂直排列
            Column(Modifier.fillMaxSize().background(extendedColors.background).padding(innerPadding)) {
                pendingSection()
                tierListSection(Modifier.fillMaxWidth().weight(1f))
            }
        } else {
            // Expanded: 平板/横屏 — 左右双栏布局
            Row(Modifier.fillMaxSize().background(extendedColors.background).padding(innerPadding)) {
                Column(
                    modifier = Modifier.weight(0.35f).fillMaxSize()
                        .padding(start = 4.dp, end = 4.dp, top = 4.dp)
                ) {
                    pendingSection()
                    Spacer(modifier = Modifier.height(8.dp))
                }
                tierListSection(Modifier.weight(0.65f).fillMaxSize())
            }
        }

        if (vm.draggingTierImage != null) FloatingDragImage(vm.draggingTierImage!!.uri, vm.draggingTierImagePosition, vm.draggingTierImageTarget, vm.floatOffsetX, vm.floatOffsetY)

        TierListDialogs(
            dialogState = vm.dialogState, handlers = vm.dialogHandlers, context = context, scope = scope,
            settingsService = vm.settingsService, presetManager = vm.presetManager,
            tierImages = vm.tierImages, tiers = vm.tiers,
            tierListTitle = vm.tierListTitle, authorName = vm.authorName,
            pendingImages = vm.pendingImages, defaultTiers = vm.defaultTiers,
            tierRowPositions = vm.tierRowPositions,
            disableClickAdd = vm.disableClickAdd, floatOffsetX = vm.floatOffsetX, floatOffsetY = vm.floatOffsetY,
            externalBadgeEnabled = vm.externalBadgeEnabled, followSystemTheme = followSystemTheme,
            disableCustomFont = disableCustomFont, nameBelowImage = vm.nameBelowImage,
            isDarkTheme = isDarkTheme, currentLanguage = currentLanguage,
            onTitleChange = { vm.tierListTitle = it }, onAuthorChange = { vm.authorName = it },
            onTiersChange = {}, onTierImagesChange = {},
            onPendingImagesChange = { vm.pendingImages = it }, onTierRowPositionsChange = { vm.tierRowPositions = it },
            onDisableClickAddChange = { vm.disableClickAdd = it }, onFloatOffsetXChange = { vm.floatOffsetX = it },
            onFloatOffsetYChange = { vm.floatOffsetY = it }, onExternalBadgeChange = { vm.externalBadgeEnabled = it },
            onFollowSystemThemeChange = onFollowSystemThemeChange,
            onDisableCustomFontChange = onDisableCustomFontChange,
            onNameBelowImageChange = { vm.nameBelowImage = it },
            onLanguageChange = onLanguageChange ?: {},
            onSkipDraftSave = onSkipDraftSave, onResumeDraftSave = onResumeDraftSave,
            presetExportLauncher = vm.presetExportLauncher, packageExportLauncher = vm.packageExportLauncher,
            presetFilePicker = vm.presetFilePicker
        )

        if (vm.dialogState.showExternalPackagePasswordDialog) {
            ZipPasswordDialog(
                showError = vm.dialogState.externalPackagePasswordError,
                onDismiss = {
                    vm.dialogState.showExternalPackagePasswordDialog = false; vm.dialogState.externalPackageUri = null
                    vm.dialogState.externalPackageFileName = ""; vm.dialogState.externalPackagePassword = null
                    vm.dialogState.externalPackagePasswordError = false; vm.isImportingPreset = false
                },
                onConfirm = { password -> vm.packageOperationHandler.continueExternalPackageImportWithPassword(password) { vm.isImportingPreset = it } }
            )
        }

        if (vm.isImportingPreset || vm.isExportingPreset || vm.isSavingPreset || vm.isExportingPackage)
            LoadingDialog(message = stringResource(R.string.loading_resources))
    }
}
