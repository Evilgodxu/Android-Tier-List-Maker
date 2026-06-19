package com.tdds.jh.screens.tierlist.components

import android.graphics.Rect
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ArrowDropUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.tdds.jh.R
import com.tdds.jh.model.tierlist.TierImage
import com.tdds.jh.model.tierlist.TierItem
import com.tdds.jh.screens.tierlist.logic.utils.TextUtils
import com.tdds.jh.ui.theme.LocalExtendedColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.roundToInt
import sh.calvin.reorderable.ReorderableCollectionItemScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * 可滑动的层级行组件
 * 支持左右滑动删除、长按拖动排序、图片拖拽等功能
 *
 * @param modifier 修饰符
 * @param tier 层级项数据
 * @param isDragging 是否正在拖动排序
 * @param images 该层级的图片列表
 * @param pendingImages 待分级图片列表
 * @param selectedImageForDrag 当前选中的拖动图片
 * @param onTierClick 层级点击回调
 * @param onTierLongClick 层级长按回调
 * @param onTierDoubleClick 层级双击回调
 * @param onAddImage 添加图片回调
 * @param onImageClick 图片点击回调
 * @param onImageLongClick 图片长按回调
 * @param onImageDoubleClick 图片双击回调
 * @param onDeleteTier 删除层级回调
 * @param onPickImage 选择图片回调
 * @param onPositionUpdate 位置更新回调
 * @param disableClickAdd 是否禁用点击添加
 * @param isDraggingPendingImage 是否有待分级图片正在拖动
 * @param onMoveSelectedImageToTier 移动选中图片到当前层级回调
 * @param tierRowPositions 层级行位置映射
 * @param draggingTierImage 正在拖动的层级图片
 * @param onTierImageDragStart 层级图片拖动开始回调
 * @param onTierImageDrag 层级图片拖动中回调
 * @param onTierImageDragEnd 层级图片拖动结束回调
 * @param listState 列表状态，用于自动滚动
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReorderableCollectionItemScope.SwipeableTierRow(
    modifier: Modifier = Modifier,
    tier: TierItem,
    isDragging: Boolean = false,
    images: List<TierImage>,
    pendingImages: List<Uri>,
    selectedImageForDrag: TierImage?,
    onTierClick: () -> Unit,
    onTierLongClick: () -> Unit,
    onTierDoubleClick: () -> Unit,
    onAddImage: (Uri) -> Unit,
    onImageClick: (TierImage, Int) -> Unit,
    onImageLongClick: (TierImage, Int) -> Unit,
    onImageDoubleClick: (TierImage, Int) -> Unit,
    onDeleteTier: () -> Unit,
    onPickImage: () -> Unit,
    onPositionUpdate: ((String, Rect) -> Unit)? = null,
    disableClickAdd: Boolean = false,
    isDraggingPendingImage: Boolean = false,
    onMoveSelectedImageToTier: (() -> Unit)? = null,
    // 层级图片拖拽相关参数
    tierRowPositions: Map<String, Rect> = emptyMap(),
    draggingTierImage: TierImage? = null,
    onTierImageDragStart: ((TierImage, Offset) -> Unit)? = null,
    onTierImageDrag: ((Offset, String?) -> Unit)? = null,
    onTierImageDragEnd: ((TierImage, String?) -> Unit)? = null,
    // 列表状态用于自动滚动
    listState: LazyListState? = null
) {
    // 固定尺寸配置
    val tierLabelWidth = 70.dp
    val imageSize = 70.dp
    val tierRowHeight = 80.dp
    val labelFontSize = 20.sp
    val tierLabelCornerRadius = 8.dp
    val imageCornerRadius = 4.dp
    // 使用 tier.label 作为 key，确保每个层级有独立的滑动状态
    var offsetX by remember(tier.label) { mutableFloatStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        label = ""
    )

    // 获取屏幕宽高计算滑动阈值和自动滚动
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val maxDragDistance = screenWidth.value * 0.75f // 最大滑动距离为屏幕宽度的75%
    val deleteThreshold = screenWidth.value * 0.5f // 删除阈值为屏幕宽度的50%

    val extendedColors = LocalExtendedColors.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .height(tierRowHeight)
            .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
            .onGloballyPositioned { coordinates ->
                // 报告层级位置
                onPositionUpdate?.let { update ->
                    val bounds = coordinates.boundsInWindow()
                    val rect = Rect(
                        bounds.left.toInt(),
                        bounds.top.toInt(),
                        bounds.right.toInt(),
                        bounds.bottom.toInt()
                    )
                    update(tier.label, rect)
                }
            }
            .pointerInput(tier.label) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (offsetX > deleteThreshold) {
                            // 向右滑动超过阈值（超过屏幕宽度的50%），直接删除
                            offsetX = 0f
                            onDeleteTier()
                        } else {
                            // 滑动未超过阈值，回弹
                            offsetX = 0f
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = offsetX + dragAmount
                        // 限制只能向右滑动，最大为屏幕宽度的75%
                        offsetX = newOffset.coerceIn(0f, maxDragDistance)
                    }
                )
            }
    ) {
        // 左侧层级标签
        val labelScope = rememberCoroutineScope()
        
        // 点击/长按/双击状态
        var lastLabelClickTime by remember { mutableLongStateOf(0L) }
        var lastLabelClickPosition by remember { mutableStateOf(Offset.Zero) }
        val labelDoubleClickThresholdMs = 300L
        
        Box(
            modifier = Modifier
                .width(tierLabelWidth)
                .fillMaxHeight()
                // 如果被拖拽，显示半透明背景
                .background(
                    if (isDragging) tier.color.copy(alpha = 0.5f) else tier.color,
                    RoundedCornerShape(tierLabelCornerRadius)
                )
                // 使用 Reorderable 库的长按拖动
                .longPressDraggableHandle()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown()
                            val startPosition = down.position
                            val downTime = System.currentTimeMillis()
                            
                            down.consume()
                            
                            // 等待手指抬起或超时
                            var isLongPress = false
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                
                                if (change.changedToUpIgnoreConsumed()) {
                                    // 手指抬起，处理点击/双击
                                    if (!isLongPress) {
                                        val upTime = System.currentTimeMillis()
                                        val timeSinceLastClick = upTime - lastLabelClickTime
                                        val distanceFromLastClick = hypot(
                                            startPosition.x - lastLabelClickPosition.x,
                                            startPosition.y - lastLabelClickPosition.y
                                        )
                                        
                                        if (timeSinceLastClick < labelDoubleClickThresholdMs && distanceFromLastClick < 50f) {
                                            onTierDoubleClick()
                                            lastLabelClickTime = 0L
                                        } else {
                                            lastLabelClickTime = upTime
                                            lastLabelClickPosition = startPosition
                                            
                                            labelScope.launch {
                                                delay(labelDoubleClickThresholdMs)
                                                if (lastLabelClickTime == upTime) {
                                                    onTierClick()
                                                }
                                            }
                                        }
                                    }
                                    break
                                }
                                
                                // 检测长按
                                val pressDuration = System.currentTimeMillis() - downTime
                                val currentPosition = change.position
                                val moveDistance = hypot(
                                    currentPosition.x - startPosition.x,
                                    currentPosition.y - startPosition.y
                                )
                                
                                if (pressDuration >= 400L && moveDistance < 25f && !isLongPress) {
                                    isLongPress = true
                                    onTierLongClick()
                                }
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // 如果被拖拽，显示拖动指示器
            if (isDragging) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowDropUp,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                    Icon(
                        imageVector = Icons.Outlined.ArrowDropDown,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Text(
                    text = TextUtils.truncateTierLabel(tier.label),
                    fontSize = labelFontSize,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        // 右侧图片区域
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable {
                    // 如果有选中的图片且不在当前层级，移动图片到当前层级
                    if (selectedImageForDrag != null && selectedImageForDrag.tierLabel != tier.label) {
                        onMoveSelectedImageToTier?.invoke()
                    } else if (selectedImageForDrag == null && pendingImages.isEmpty()) {
                        // 点击空白区域打开图片选择器（有选中图片时禁用）
                        onPickImage()
                    }
                },
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = extendedColors.surfaceVariant
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                if (images.isEmpty() && pendingImages.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isDraggingPendingImage) stringResource(R.string.drop_image_here) else stringResource(R.string.click_to_add_image),
                            color = if (isDraggingPendingImage) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyRow(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        // 已添加的图片
                        itemsIndexed(images, key = { index, image -> "${index}_${image.id}_${image.uri}" }) { index, image ->
                            DraggableImage(
                                image = image,
                                index = index,
                                isSelected = selectedImageForDrag?.id == image.id,
                                isDragging = draggingTierImage?.id == image.id,
                                onClick = { onImageClick(image, index) },
                                onLongClick = { onImageLongClick(image, index) },
                                onDoubleClick = { onImageDoubleClick(image, index) },
                                onDragStart = { center -> onTierImageDragStart?.invoke(image, center) },
                                onDrag = { center, targetTier -> onTierImageDrag?.invoke(center, targetTier) },
                                onDragEnd = { targetTier -> onTierImageDragEnd?.invoke(image, targetTier) },
                                tierRowPositions = tierRowPositions,
                                listState = listState
                            )
                        }

                        // 待添加的图片（仅在未禁用加添且未拖拽层级图片时显示）
                        if (!disableClickAdd && draggingTierImage == null) {
                            items(pendingImages) { uri ->
                                Box(
                                    modifier = Modifier
                                        .size(imageSize)
                                        .clickable { onAddImage(uri) }
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(uri)
                                            .crossfade(true)
                                            .diskCachePolicy(CachePolicy.DISABLED)
                                            .build(),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(imageCornerRadius)),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(imageCornerRadius)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = stringResource(R.string.click_to_add_image),
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
