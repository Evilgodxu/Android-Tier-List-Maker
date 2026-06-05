package com.tdds.jh.screens.tierlist.components

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.edit
import com.tdds.jh.data.tierlist.PresetManager
import com.tdds.jh.R
import com.tdds.jh.ui.CustomVerticalScrollbar
import com.tdds.jh.ui.theme.LocalExtendedColors
import java.io.File
import java.io.FileOutputStream

/**
 * 裁切状态数据类
 * 用于保存和恢复裁切框的位置、缩放、比例等状态
 *
 * @param positionX 裁切框水平位置（0.0f = 左边缘, 1.0f = 右边缘, 0.5f = 居中）
 * @param positionY 裁切框垂直位置（0.0f = 上边缘, 1.0f = 下边缘, 0.5f = 居中）
 * @param scale 缩放比例
 * @param cropRatio 裁剪比例: 1f = 1:1, 0.75f = 3:4, 1.33f = 4:3
 * @param useCustomCrop 是否使用自定义裁切尺寸
 * @param customCropWidth 自定义裁切宽度
 * @param customCropHeight 自定义裁切高度
 */
data class CropState(
    val positionX: Float = 0.5f,
    val positionY: Float = 0.5f,
    val scale: Float = 1.0f,
    val cropRatio: Float = 1f,
    val useCustomCrop: Boolean = false,
    val customCropWidth: Int = 0,
    val customCropHeight: Int = 0
)

/**
 * 图片裁切对话框
 * 提供可视化的图片裁切功能，支持手势拖动裁切框、比例选择和自定义尺寸
 *
 * @param imageUri 待裁切图片的URI
 * @param initialCropState 初始裁切状态，用于恢复之前的裁切设置
 * @param onDismiss 关闭对话框回调
 * @param onCrop 裁切完成回调，返回裁切后的图片URI和裁切状态
 * @param presetManager 预设管理器，用于保存裁切后的图片到工作目录
 * @param onApplyToAll 应用到所有图片的回调，用于批量应用相同的裁切设置
 *                 参数: CropState - 当前裁切状态, Int - 当前图片宽度, Int - 当前图片高度
 */
@Composable
fun ImageCropDialog(
    imageUri: Uri,
    initialCropState: CropState = CropState(),
    onDismiss: () -> Unit,
    onCrop: (Uri, CropState) -> Unit,
    presetManager: PresetManager? = null,
    onApplyToAll: ((CropState, Int, Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val extendedColors = LocalExtendedColors.current
    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    // 从初始状态获取裁剪比例
    val savedCustomSizeWidth = if (initialCropState.customCropWidth > 0) initialCropState.customCropWidth else 0
    val savedCustomSizeHeight = if (initialCropState.customCropHeight > 0) initialCropState.customCropHeight else 0
    val hasValidCustomSize = savedCustomSizeWidth > 0 && savedCustomSizeHeight > 0
    val savedUseCustomSize = initialCropState.useCustomCrop && hasValidCustomSize

    // 计算初始裁剪比例
    val initialRatio = when {
        savedUseCustomSize && hasValidCustomSize ->
            savedCustomSizeWidth.toFloat() / savedCustomSizeHeight.toFloat()
        initialCropState.cropRatio > 0 -> initialCropState.cropRatio
        else -> 1f
    }

    var selectedRatio by remember { mutableStateOf(initialRatio) }
    var cropPositionX by remember { mutableStateOf(initialCropState.positionX) }
    var cropPositionY by remember { mutableStateOf(initialCropState.positionY) }
    var cropScale by remember { mutableStateOf(initialCropState.scale) }

    // 自定义裁切框大小状态
    var showCustomSizeDialog by remember { mutableStateOf(false) }
    var customCropWidth by remember { mutableStateOf(if (savedCustomSizeWidth > 0) savedCustomSizeWidth.toString() else "") }
    var customCropHeight by remember { mutableStateOf(if (savedCustomSizeHeight > 0) savedCustomSizeHeight.toString() else "") }
    var useCustomSize by remember { mutableStateOf(savedUseCustomSize) }
    var customSizeWidth by remember { mutableStateOf(savedCustomSizeWidth) }
    var customSizeHeight by remember { mutableStateOf(savedCustomSizeHeight) }

    // 加载原图（带尺寸限制防止OOM）
    LaunchedEffect(imageUri) {
        loadBitmapWithSampling(context, imageUri) { loadedBitmap ->
            bitmap = loadedBitmap
            loadedBitmap?.let { bmp ->
                restoreCropStateFromPrefs(prefs, bmp.width, bmp.height) { x, y, useCustom, customW, customH ->
                    cropPositionX = x
                    cropPositionY = y
                    if (useCustom && customW > 0 && customW <= bmp.width && customH > 0 && customH <= bmp.height) {
                        useCustomSize = true
                        customSizeWidth = customW
                        customSizeHeight = customH
                    }
                }
            }
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(0.95f)
                .then(if (isLandscape) Modifier.heightIn(max = 560.dp) else Modifier),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = extendedColors.cardBackground)
        ) {
            Column {
                // 标题
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.select_crop_area),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // 内容区域
                if (isLandscape) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(scrollState)
                                .padding(horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CropDialogContent(
                                bitmap = bitmap,
                                selectedRatio = selectedRatio,
                                useCustomSize = useCustomSize,
                                customSizeWidth = customSizeWidth,
                                customSizeHeight = customSizeHeight,
                                cropPositionX = cropPositionX,
                                cropPositionY = cropPositionY,
                                onPositionChange = { x, y -> cropPositionX = x; cropPositionY = y },
                                onRatioSelect = { r -> selectedRatio = r; useCustomSize = false },
                                onCustomSizeClick = {
                                    showCustomSizeDialog = true
                                    if (customSizeWidth > 0 && customSizeHeight > 0) {
                                        customCropWidth = customSizeWidth.toString()
                                        customCropHeight = customSizeHeight.toString()
                                    }
                                    useCustomSize = true
                                }
                            )
                        }
                        CustomVerticalScrollbar(
                            scrollState = scrollState,
                            modifier = Modifier.fillMaxHeight()
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CropDialogContent(
                            bitmap = bitmap,
                            selectedRatio = selectedRatio,
                            useCustomSize = useCustomSize,
                            customSizeWidth = customSizeWidth,
                            customSizeHeight = customSizeHeight,
                            cropPositionX = cropPositionX,
                            cropPositionY = cropPositionY,
                            onPositionChange = { x, y -> cropPositionX = x; cropPositionY = y },
                            onRatioSelect = { r -> selectedRatio = r; useCustomSize = false },
                            onCustomSizeClick = {
                                showCustomSizeDialog = true
                                if (customSizeWidth > 0 && customSizeHeight > 0) {
                                    customCropWidth = customSizeWidth.toString()
                                    customCropHeight = customSizeHeight.toString()
                                }
                                useCustomSize = true
                            }
                        )
                    }
                }

                // 按钮区域
                val currentCropState = CropState(
                    positionX = cropPositionX,
                    positionY = cropPositionY,
                    scale = cropScale,
                    cropRatio = if (useCustomSize && customSizeWidth > 0 && customSizeHeight > 0) {
                        customSizeWidth.toFloat() / customSizeHeight.toFloat()
                    } else {
                        selectedRatio
                    },
                    useCustomCrop = useCustomSize,
                    customCropWidth = if (useCustomSize) customSizeWidth else 0,
                    customCropHeight = if (useCustomSize) customSizeHeight else 0
                )
                val currentImageWidth = bitmap?.width ?: 0
                val currentImageHeight = bitmap?.height ?: 0
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    CropDialogButtons(
                        bitmap = bitmap,
                        currentCropState = currentCropState,
                        currentImageWidth = currentImageWidth,
                        currentImageHeight = currentImageHeight,
                        onApplyToAll = onApplyToAll,
                        onReset = { onCrop(imageUri, CropState(cropRatio = 0f)) },
                        onConfirm = {
                            performCrop(
                                bitmap = bitmap,
                                imageUri = imageUri,
                                useCustomSize = useCustomSize,
                                customSizeWidth = customSizeWidth,
                                customSizeHeight = customSizeHeight,
                                selectedRatio = selectedRatio,
                                cropPositionX = cropPositionX,
                                cropPositionY = cropPositionY,
                                cropScale = cropScale,
                                context = context,
                                presetManager = presetManager,
                                prefs = prefs,
                                onCrop = onCrop,
                                onDismiss = onDismiss
                            )
                        }
                    )
                }
            }
        }
    }

    // 自定义裁切框大小对话框
    if (showCustomSizeDialog) {
        CustomCropSizeDialog(
            bitmap = bitmap,
            customCropWidth = customCropWidth,
            customCropHeight = customCropHeight,
            onWidthChange = { customCropWidth = it },
            onHeightChange = { customCropHeight = it },
            onDismiss = { showCustomSizeDialog = false },
            onConfirm = { width, height ->
                customSizeWidth = width
                customSizeHeight = height
                useCustomSize = true
                saveCustomSizeToPrefs(prefs, width, height)
                cropPositionX = 0.5f
                cropPositionY = 0.5f
                showCustomSizeDialog = false
                customCropWidth = ""
                customCropHeight = ""
            }
        )
    }
}

/**
 * 裁切对话框可滚动内容
 */
@Composable
private fun CropDialogContent(
    bitmap: Bitmap?,
    selectedRatio: Float,
    useCustomSize: Boolean,
    customSizeWidth: Int,
    customSizeHeight: Int,
    cropPositionX: Float,
    cropPositionY: Float,
    onPositionChange: (Float, Float) -> Unit,
    onRatioSelect: (Float) -> Unit,
    onCustomSizeClick: () -> Unit
) {
    CropPreviewArea(
        bitmap = bitmap,
        selectedRatio = selectedRatio,
        useCustomSize = useCustomSize,
        customSizeWidth = customSizeWidth,
        customSizeHeight = customSizeHeight,
        cropPositionX = cropPositionX,
        cropPositionY = cropPositionY,
        onPositionChange = onPositionChange
    )

    Spacer(modifier = Modifier.height(16.dp))

    bitmap?.let {
        Text(
            text = "${it.width} x ${it.height}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
    }

    Text(text = stringResource(R.string.crop_ratio))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        CropRatioChip(
            selected = selectedRatio == 1f && !useCustomSize,
            label = stringResource(R.string.ratio_1_1),
            onClick = { onRatioSelect(1f) }
        )
        CropRatioChip(
            selected = selectedRatio == 0.75f && !useCustomSize,
            label = stringResource(R.string.ratio_3_4),
            onClick = { onRatioSelect(0.75f) }
        )
        CropRatioChip(
            selected = useCustomSize,
            label = stringResource(R.string.custom),
            onClick = onCustomSizeClick
        )
    }
}

/**
 * 裁切预览区域组件
 */
@Composable
private fun CropPreviewArea(
    bitmap: Bitmap?,
    selectedRatio: Float,
    useCustomSize: Boolean,
    customSizeWidth: Int,
    customSizeHeight: Int,
    cropPositionX: Float,
    cropPositionY: Float,
    onPositionChange: (Float, Float) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        bitmap?.let { originalBitmap ->
            val imageWidth = originalBitmap.width
            val imageHeight = originalBitmap.height

            val (cropWidth, cropHeight) = calculateCropDimensions(
                imageWidth, imageHeight, selectedRatio, useCustomSize, customSizeWidth, customSizeHeight
            )

            val maxXOffset = imageWidth - cropWidth
            val maxYOffset = imageHeight - cropHeight
            val xOffset = (maxXOffset * cropPositionX).toInt()
            val yOffset = (maxYOffset * cropPositionY).toInt()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(bitmap, selectedRatio, useCustomSize, customSizeWidth, customSizeHeight) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            handleCropDrag(
                                change = change,
                                size = size,
                                bitmap = bitmap,
                                selectedRatio = selectedRatio,
                                useCustomSize = useCustomSize,
                                customSizeWidth = customSizeWidth,
                                customSizeHeight = customSizeHeight,
                                onPositionChange = onPositionChange
                            )
                        }
                    }
            ) {
                Image(
                    bitmap = originalBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                // 绘制裁切框和遮罩
                CropOverlay(
                    imageWidth = imageWidth,
                    imageHeight = imageHeight,
                    cropWidth = cropWidth,
                    cropHeight = cropHeight,
                    xOffset = xOffset,
                    yOffset = yOffset
                )
            }

            // 显示提示文字
            Text(
                text = stringResource(R.string.drag_to_move_crop),
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
            )
        }
    }
}

/**
 * 裁切框遮罩和边框绘制
 */
@Composable
private fun CropOverlay(
    imageWidth: Int,
    imageHeight: Int,
    cropWidth: Int,
    cropHeight: Int,
    xOffset: Int,
    yOffset: Int
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        val (drawWidth, drawHeight, drawX, drawY) = calculateImageDrawRect(
            canvasWidth, canvasHeight, imageWidth.toFloat() / imageHeight
        )

        val scaleX = drawWidth / imageWidth
        val scaleY = drawHeight / imageHeight

        val cropRectLeft = drawX + xOffset * scaleX
        val cropRectTop = drawY + yOffset * scaleY
        val cropRectWidth = cropWidth * scaleX
        val cropRectHeight = cropHeight * scaleY

        // 绘制半透明遮罩
        drawCropMask(canvasWidth, canvasHeight, cropRectLeft, cropRectTop, cropRectWidth, cropRectHeight)

        // 绘制裁切框边框
        drawCropBorder(cropRectLeft, cropRectTop, cropRectWidth, cropRectHeight)

        // 绘制四角标记
        drawCropCorners(cropRectLeft, cropRectTop, cropRectWidth, cropRectHeight)
    }
}

/**
 * 计算图片在Canvas中的绘制区域
 */
private fun calculateImageDrawRect(
    canvasWidth: Float,
    canvasHeight: Float,
    bitmapAspect: Float
): Quadruple<Float, Float, Float, Float> {
    val canvasAspect = canvasWidth / canvasHeight

    return if (bitmapAspect > canvasAspect) {
        val drawWidth = canvasWidth
        val drawHeight = canvasWidth / bitmapAspect
        val drawX = 0f
        val drawY = (canvasHeight - drawHeight) / 2f
        Quadruple(drawWidth, drawHeight, drawX, drawY)
    } else {
        val drawHeight = canvasHeight
        val drawWidth = canvasHeight * bitmapAspect
        val drawX = (canvasWidth - drawWidth) / 2f
        val drawY = 0f
        Quadruple(drawWidth, drawHeight, drawX, drawY)
    }
}

/**
 * 四元组数据类
 */
private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

/**
 * 计算裁切尺寸
 */
private fun calculateCropDimensions(
    imageWidth: Int,
    imageHeight: Int,
    selectedRatio: Float,
    useCustomSize: Boolean,
    customSizeWidth: Int,
    customSizeHeight: Int
): Pair<Int, Int> {
    return if (useCustomSize) {
        customSizeWidth.coerceIn(1, imageWidth) to customSizeHeight.coerceIn(1, imageHeight)
    } else {
        val aspectRatio = selectedRatio
        if (imageWidth.toFloat() / imageHeight > aspectRatio) {
            val cropHeight = imageHeight
            val cropWidth = (imageHeight * aspectRatio).toInt()
            cropWidth to cropHeight
        } else {
            val cropWidth = imageWidth
            val cropHeight = (imageWidth / aspectRatio).toInt()
            cropWidth to cropHeight
        }
    }
}

/**
 * 处理裁切框拖动
 */
private fun handleCropDrag(
    change: androidx.compose.ui.input.pointer.PointerInputChange,
    size: androidx.compose.ui.unit.IntSize,
    bitmap: Bitmap?,
    selectedRatio: Float,
    useCustomSize: Boolean,
    customSizeWidth: Int,
    customSizeHeight: Int,
    onPositionChange: (Float, Float) -> Unit
) {
    bitmap?.let { originalBitmap ->
        val imageWidth = originalBitmap.width
        val imageHeight = originalBitmap.height

        val (cropWidth, cropHeight) = calculateCropDimensions(
            imageWidth, imageHeight, selectedRatio, useCustomSize, customSizeWidth, customSizeHeight
        )

        val maxXOffset = (imageWidth - cropWidth).toFloat()
        val maxYOffset = (imageHeight - cropHeight).toFloat()

        val touchX = change.position.x
        val touchY = change.position.y

        val canvasWidth = size.width.toFloat()
        val canvasHeight = size.height.toFloat()
        val bitmapAspect = imageWidth.toFloat() / imageHeight

        val (drawWidth, drawHeight, drawX, drawY) = calculateImageDrawRect(
            canvasWidth, canvasHeight, bitmapAspect
        )

        val scaleX = drawWidth / imageWidth
        val scaleY = drawHeight / imageHeight

        val cropRectWidth = cropWidth * scaleX
        val cropRectHeight = cropHeight * scaleY

        val relativeX = touchX - drawX
        val relativeY = touchY - drawY

        val cropCenterX = relativeX - cropRectWidth / 2
        val cropCenterY = relativeY - cropRectHeight / 2

        val newXOffset = (cropCenterX / scaleX).coerceIn(0f, maxXOffset)
        val newYOffset = (cropCenterY / scaleY).coerceIn(0f, maxYOffset)

        val newPositionX = if (maxXOffset > 0) newXOffset / maxXOffset else 0.5f
        val newPositionY = if (maxYOffset > 0) newYOffset / maxYOffset else 0.5f

        onPositionChange(newPositionX, newPositionY)
    }
}

/**
 * 绘制裁切遮罩
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCropMask(
    canvasWidth: Float,
    canvasHeight: Float,
    cropRectLeft: Float,
    cropRectTop: Float,
    cropRectWidth: Float,
    cropRectHeight: Float
) {
    // 上
    drawRect(
        color = Color.Black.copy(alpha = 0.5f),
        topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
        size = Size(canvasWidth, cropRectTop)
    )
    // 下
    drawRect(
        color = Color.Black.copy(alpha = 0.5f),
        topLeft = androidx.compose.ui.geometry.Offset(0f, cropRectTop + cropRectHeight),
        size = Size(canvasWidth, canvasHeight - cropRectTop - cropRectHeight)
    )
    // 左
    drawRect(
        color = Color.Black.copy(alpha = 0.5f),
        topLeft = androidx.compose.ui.geometry.Offset(0f, cropRectTop),
        size = Size(cropRectLeft, cropRectHeight)
    )
    // 右
    drawRect(
        color = Color.Black.copy(alpha = 0.5f),
        topLeft = androidx.compose.ui.geometry.Offset(cropRectLeft + cropRectWidth, cropRectTop),
        size = Size(canvasWidth - cropRectLeft - cropRectWidth, cropRectHeight)
    )
}

/**
 * 绘制裁切边框
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCropBorder(
    cropRectLeft: Float,
    cropRectTop: Float,
    cropRectWidth: Float,
    cropRectHeight: Float
) {
    drawRect(
        color = Color.White,
        topLeft = androidx.compose.ui.geometry.Offset(cropRectLeft, cropRectTop),
        size = Size(cropRectWidth, cropRectHeight),
        style = Stroke(width = 3f)
    )
}

/**
 * 绘制四角标记
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCropCorners(
    cropRectLeft: Float,
    cropRectTop: Float,
    cropRectWidth: Float,
    cropRectHeight: Float
) {
    val cornerSize = 20f
    val corners = listOf(
        // 左上角
        Triple(cropRectLeft, cropRectTop, listOf(
            androidx.compose.ui.geometry.Offset(cropRectLeft, cropRectTop + cornerSize) to
                androidx.compose.ui.geometry.Offset(cropRectLeft, cropRectTop),
            androidx.compose.ui.geometry.Offset(cropRectLeft, cropRectTop) to
                androidx.compose.ui.geometry.Offset(cropRectLeft + cornerSize, cropRectTop)
        )),
        // 右上角
        Triple(cropRectLeft + cropRectWidth, cropRectTop, listOf(
            androidx.compose.ui.geometry.Offset(cropRectLeft + cropRectWidth - cornerSize, cropRectTop) to
                androidx.compose.ui.geometry.Offset(cropRectLeft + cropRectWidth, cropRectTop),
            androidx.compose.ui.geometry.Offset(cropRectLeft + cropRectWidth, cropRectTop) to
                androidx.compose.ui.geometry.Offset(cropRectLeft + cropRectWidth, cropRectTop + cornerSize)
        )),
        // 左下角
        Triple(cropRectLeft, cropRectTop + cropRectHeight, listOf(
            androidx.compose.ui.geometry.Offset(cropRectLeft, cropRectTop + cropRectHeight - cornerSize) to
                androidx.compose.ui.geometry.Offset(cropRectLeft, cropRectTop + cropRectHeight),
            androidx.compose.ui.geometry.Offset(cropRectLeft, cropRectTop + cropRectHeight) to
                androidx.compose.ui.geometry.Offset(cropRectLeft + cornerSize, cropRectTop + cropRectHeight)
        )),
        // 右下角
        Triple(cropRectLeft + cropRectWidth, cropRectTop + cropRectHeight, listOf(
            androidx.compose.ui.geometry.Offset(cropRectLeft + cropRectWidth - cornerSize, cropRectTop + cropRectHeight) to
                androidx.compose.ui.geometry.Offset(cropRectLeft + cropRectWidth, cropRectTop + cropRectHeight),
            androidx.compose.ui.geometry.Offset(cropRectLeft + cropRectWidth, cropRectTop + cropRectHeight - cornerSize) to
                androidx.compose.ui.geometry.Offset(cropRectLeft + cropRectWidth, cropRectTop + cropRectHeight)
        ))
    )

    corners.forEach { (_, _, lines) ->
        lines.forEach { (start, end) ->
            drawLine(color = Color.White, start = start, end = end, strokeWidth = 3f)
        }
    }
}

/**
 * 比例选择芯片
 */
@Composable
private fun CropRatioChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) }
    )
}

/**
 * 裁切对话框按钮区域
 */
@Composable
private fun CropDialogButtons(
    bitmap: Bitmap?,
    currentCropState: CropState,
    currentImageWidth: Int,
    currentImageHeight: Int,
    onApplyToAll: ((CropState, Int, Int) -> Unit)?,
    onReset: () -> Unit,
    onConfirm: () -> Unit
) {
    var isApplyingToAll by remember { mutableStateOf(false) }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (onApplyToAll != null) {
            TextButton(
                onClick = {
                    isApplyingToAll = true
                    onApplyToAll(currentCropState, currentImageWidth, currentImageHeight)
                    isApplyingToAll = false
                },
                enabled = !isApplyingToAll && currentImageWidth > 0 && currentImageHeight > 0
            ) {
                if (isApplyingToAll) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(R.string.apply_to_all))
                }
            }
        }

        TextButton(onClick = onReset) {
            Text(stringResource(R.string.reset))
        }

        TextButton(onClick = onConfirm) {
            Text(stringResource(R.string.confirm))
        }
    }
}

/**
 * 自定义裁切尺寸对话框
 */
@Composable
private fun CustomCropSizeDialog(
    bitmap: Bitmap?,
    customCropWidth: String,
    customCropHeight: String,
    onWidthChange: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val extendedColors = LocalExtendedColors.current
    val maxWidth = bitmap?.width ?: 9999
    val maxHeight = bitmap?.height ?: 9999

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = extendedColors.cardBackground,
        title = { Text(stringResource(R.string.custom_crop_size)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.image_dimensions, maxWidth, maxHeight),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = customCropWidth,
                    onValueChange = { onWidthChange(filterCropInput(it, maxWidth)) },
                    label = { Text(stringResource(R.string.crop_width, maxWidth)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = customCropHeight,
                    onValueChange = { onHeightChange(filterCropInput(it, maxHeight)) },
                    label = { Text(stringResource(R.string.crop_height, maxHeight)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val width = customCropWidth.toIntOrNull()
                    val height = customCropHeight.toIntOrNull()
                    if (width != null && height != null && width > 0 && height > 0) {
                        onConfirm(
                            width.coerceIn(1, maxWidth),
                            height.coerceIn(1, maxHeight)
                        )
                    }
                }
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * 过滤裁切输入
 */
private fun filterCropInput(input: String, maxValue: Int): String {
    val filtered = input.filter { it.isDigit() }
    val num = filtered.toIntOrNull()
    return when {
        num == null -> ""
        num > maxValue -> maxValue.toString()
        else -> filtered
    }
}

/**
 * 加载图片（带采样防止OOM）
 */
private fun loadBitmapWithSampling(
    context: Context,
    imageUri: Uri,
    onLoaded: (Bitmap?) -> Unit
) {
    try {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(imageUri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }

        val maxDimension = 2048
        var sampleSize = 1
        while (options.outWidth / sampleSize > maxDimension ||
            options.outHeight / sampleSize > maxDimension) {
            sampleSize *= 2
        }

        context.contentResolver.openInputStream(imageUri)?.use { stream ->
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            val loadedBitmap = BitmapFactory.decodeStream(stream, null, decodeOptions)
            onLoaded(loadedBitmap)
        }
    } catch (_: Exception) {
        onLoaded(null)
    }
}

/**
 * 从SharedPreferences恢复裁切状态
 */
private fun restoreCropStateFromPrefs(
    prefs: android.content.SharedPreferences,
    imageWidth: Int,
    imageHeight: Int,
    onRestored: (Float, Float, Boolean, Int, Int) -> Unit
) {
    val savedImageWidth = prefs.getInt("last_crop_image_width", 0)
    val savedImageHeight = prefs.getInt("last_crop_image_height", 0)
    val savedCropPositionX = prefs.getFloat("crop_position_x", 0.5f)
    val savedCropPositionY = prefs.getFloat("crop_position_y", 0.5f)
    val savedCustomCropWidth = prefs.getInt("custom_crop_width", 0)
    val savedCustomCropHeight = prefs.getInt("custom_crop_height", 0)
    val savedUseCustomSize = prefs.getBoolean("use_custom_crop_size", false)

    if (imageWidth == savedImageWidth && imageHeight == savedImageHeight) {
        val useCustom = savedUseCustomSize &&
            savedCustomCropWidth > 0 && savedCustomCropWidth <= imageWidth &&
            savedCustomCropHeight > 0 && savedCustomCropHeight <= imageHeight
        onRestored(savedCropPositionX, savedCropPositionY, useCustom, savedCustomCropWidth, savedCustomCropHeight)
    } else {
        onRestored(0.5f, 0.5f, false, 0, 0)
    }
}

/**
 * 保存自定义尺寸到SharedPreferences
 */
private fun saveCustomSizeToPrefs(
    prefs: android.content.SharedPreferences,
    width: Int,
    height: Int
) {
    prefs.edit {
        putInt("custom_crop_width", width)
        putInt("custom_crop_height", height)
        putBoolean("use_custom_crop_size", true)
    }
}

/**
 * 执行裁切操作
 */
private fun performCrop(
    bitmap: Bitmap?,
    imageUri: Uri,
    useCustomSize: Boolean,
    customSizeWidth: Int,
    customSizeHeight: Int,
    selectedRatio: Float,
    cropPositionX: Float,
    cropPositionY: Float,
    cropScale: Float,
    context: Context,
    presetManager: PresetManager?,
    prefs: android.content.SharedPreferences,
    onCrop: (Uri, CropState) -> Unit,
    onDismiss: () -> Unit
) {
    bitmap?.let { originalBitmap ->
        try {
            val imageWidth = originalBitmap.width
            val imageHeight = originalBitmap.height

            val (cropWidth, cropHeight) = calculateCropDimensions(
                imageWidth, imageHeight, selectedRatio, useCustomSize, customSizeWidth, customSizeHeight
            )

            val maxXOffset = imageWidth - cropWidth
            val maxYOffset = imageHeight - cropHeight
            val xOffset = (maxXOffset * cropPositionX).toInt()
            val yOffset = (maxYOffset * cropPositionY).toInt()

            val croppedBitmap = Bitmap.createBitmap(
                originalBitmap,
                xOffset,
                yOffset,
                cropWidth,
                cropHeight
            )

            // 保存裁切框位置和图片尺寸到缓存
            prefs.edit {
                putFloat("crop_position_x", cropPositionX)
                putFloat("crop_position_y", cropPositionY)
                putInt("last_crop_image_width", imageWidth)
                putInt("last_crop_image_height", imageHeight)
            }

            // 保存到工作目录或缓存目录
            val targetFile = if (presetManager != null) {
                val workDir = File(presetManager.getWorkImagesDirectory(), PresetManager.IMAGES_FOLDER_NAME)
                workDir.mkdirs()
                File(workDir, "cropped_${System.currentTimeMillis()}.webp")
            } else {
                File(context.cacheDir, "cropped_${System.currentTimeMillis()}.webp")
            }
            FileOutputStream(targetFile).use { out ->
                croppedBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 100, out)
            }

            val actualCropRatio = if (useCustomSize && customSizeWidth > 0 && customSizeHeight > 0) {
                customSizeWidth.toFloat() / customSizeHeight.toFloat()
            } else {
                selectedRatio
            }

            onCrop(
                Uri.fromFile(targetFile),
                CropState(
                    positionX = cropPositionX,
                    positionY = cropPositionY,
                    scale = cropScale,
                    cropRatio = actualCropRatio,
                    useCustomCrop = useCustomSize,
                    customCropWidth = if (useCustomSize) customSizeWidth else 0,
                    customCropHeight = if (useCustomSize) customSizeHeight else 0
                )
            )
        } catch (_: Exception) {
            onDismiss()
        }
    } ?: onDismiss()
}
