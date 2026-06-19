package com.tdds.jh.screens.tierlist.components

import android.graphics.Rect
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.tdds.jh.model.tierlist.TierImage
import com.tdds.jh.screens.tierlist.logic.utils.TextUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot

/**
 * 可拖动的层级图片组件
 * 支持点击、长按、双击和拖动操作
 *
 * @param image 层级图片数据
 * @param index 图片索引位置
 * @param isSelected 是否被选中
 * @param isDragging 是否正在拖动
 * @param onClick 点击回调
 * @param onLongClick 长按回调
 * @param onDoubleClick 双击回调
 * @param onDragStart 拖动开始回调，返回图片中心偏移量
 * @param onDrag 拖动中回调，返回当前中心点和目标层级
 * @param onDragEnd 拖动结束回调，返回放置目标层级标签
 * @param tierRowPositions 层级行位置映射，用于计算放置目标
 * @param listState 列表状态，用于自动滚动
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DraggableImage(
    image: TierImage,
    index: Int,
    isSelected: Boolean,
    isDragging: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDoubleClick: () -> Unit,
    onDragStart: ((Offset) -> Unit)? = null,
    onDrag: ((Offset, String?) -> Unit)? = null,
    onDragEnd: ((String?) -> Unit)? = null,
    tierRowPositions: Map<String, Rect> = emptyMap(),
    listState: LazyListState? = null
) {
    // 固定尺寸配置
    val imageSize = 70.dp
    val imageCornerRadius = 4.dp
    val badgeSize = (imageSize.value * 0.22f).dp
    val density = LocalDensity.current
    val imageScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    
    // 记录图片中心点的全局位置
    var itemCenterGlobal by remember { mutableStateOf(Offset.Zero) }
    // 记录拖动偏移量
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    // 本地拖动状态
    var localIsDragging by remember { mutableStateOf(false) }
    // 使用 rememberUpdatedState 确保总是使用最新的位置信息
    val currentTierRowPositions by rememberUpdatedState(tierRowPositions)
    val currentListState by rememberUpdatedState(listState)
    
    // 点击/长按/双击状态
    var lastClickTime by remember { mutableLongStateOf(0L) }
    var lastClickPosition by remember { mutableStateOf(Offset.Zero) }
    val clickThresholdMs = 200L
    val doubleClickThresholdMs = 300L
    
    Box(
        modifier = Modifier
            .size(imageSize)
            .then(
                if (isDragging || localIsDragging) {
                    Modifier.border(2.dp, Color(0xFF2196F3), RoundedCornerShape(imageCornerRadius))
                } else if (isSelected) {
                    Modifier.border(3.dp, Color(0xFF2196F3), RoundedCornerShape(imageCornerRadius))
                } else Modifier
            )
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInWindow()
                itemCenterGlobal = Offset(
                    bounds.left + bounds.width / 2,
                    bounds.top + bounds.height / 2
                )
            }
            .pointerInput(Unit) {
                val dragThreshold = 40f
                val longPressThresholdMs = 300L // 缩短为300ms
                // 长按移动阈值：图片大小
                val longPressMoveThresholdPx = with(density) { imageSize.toPx() }
                
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown()
                        val startPosition = down.position
                        val downTime = System.currentTimeMillis()
                        var hasDragged = false
                        var hasLongPressed = false
                        var maxMoveDistance = 0f
                        
                        down.consume()
                        
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            
                            if (change.changedToUpIgnoreConsumed()) {
                                val upTime = System.currentTimeMillis()
                                
                                if (localIsDragging) {
                                    // 拖动结束，计算放置目标
                                    val finalCenter = itemCenterGlobal + dragOffset
                                    var foundTier: String? = null
                                    for ((tierLabel, rect) in currentTierRowPositions) {
                                        if (finalCenter.x >= rect.left && finalCenter.x <= rect.right &&
                                            finalCenter.y >= rect.top && finalCenter.y <= rect.bottom) {
                                            foundTier = tierLabel
                                            break
                                        }
                                    }
                                    onDragEnd?.invoke(foundTier)
                                    localIsDragging = false
                                    dragOffset = Offset.Zero
                                } else if (!hasDragged && !hasLongPressed) {
                                    val timeSinceLastClick = upTime - lastClickTime
                                    val distanceFromLastClick = hypot(startPosition.x - lastClickPosition.x, startPosition.y - lastClickPosition.y)
                                    
                                    if (timeSinceLastClick < doubleClickThresholdMs && distanceFromLastClick < 50f) {
                                        onDoubleClick()
                                        lastClickTime = 0L
                                    } else {
                                        lastClickTime = upTime
                                        lastClickPosition = startPosition
                                        
                                        imageScope.launch {
                                            delay(doubleClickThresholdMs)
                                            if (lastClickTime == upTime) {
                                                onClick()
                                            }
                                        }
                                    }
                                }
                                break
                            }
                            
                            val currentPosition = change.position
                            val deltaY = currentPosition.y - startPosition.y
                            val deltaX = currentPosition.x - startPosition.x
                            val pressDuration = System.currentTimeMillis() - downTime
                            val moveDistance = hypot(deltaX, deltaY)
                            maxMoveDistance = maxMoveDistance.coerceAtLeast(moveDistance)
                            
                            if (!hasDragged && !hasLongPressed) {
                                // 优先检测垂直拖动（直接上下拖拽即可触发）
                                if (kotlin.math.abs(deltaY) > dragThreshold && kotlin.math.abs(deltaY) > kotlin.math.abs(deltaX)) {
                                    hasDragged = true
                                    dragOffset = Offset.Zero
                                    localIsDragging = true
                                    onDragStart?.invoke(itemCenterGlobal)
                                    change.consume()
                                    continue
                                }
                                
                                // 水平滑动不处理，让父组件处理
                                if (kotlin.math.abs(deltaX) > dragThreshold) {
                                    // 如果已经开始拖动，需要取消拖动状态
                                    if (localIsDragging) {
                                        localIsDragging = false
                                        dragOffset = Offset.Zero
                                        onDragEnd?.invoke(null)
                                    }
                                    break
                                }
                                
                                // 长按功能未实现，继续等待其他手势（单击或双击）
                                continue
                            }
                            
                            if (localIsDragging) {
                                val currentOffset = currentPosition - startPosition
                                dragOffset = currentOffset
                                change.consume()
                                
                                // 计算当前中心点和放置目标
                                val currentCenter = itemCenterGlobal + dragOffset
                                var foundTier: String? = null
                                for ((tierLabel, rect) in currentTierRowPositions) {
                                    if (currentCenter.x >= rect.left && currentCenter.x <= rect.right &&
                                        currentCenter.y >= rect.top && currentCenter.y <= rect.bottom) {
                                        foundTier = tierLabel
                                        break
                                    }
                                }
                                onDrag?.invoke(currentCenter, foundTier)
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(image.uri)
                .crossfade(true)
                .diskCachePolicy(CachePolicy.DISABLED)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(imageCornerRadius)),
            contentScale = ContentScale.Crop
        )

        // 显示小图标（编辑页面始终内置在右上角）
        if (image.badgeUri != null || image.badgeUri2 != null || image.badgeUri3 != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(2.dp)
            ) {
                // 小图标1
                if (image.badgeUri != null) {
                    Box(
                        modifier = Modifier.size(badgeSize)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(image.badgeUri)
                                .crossfade(true)
                                .diskCachePolicy(CachePolicy.DISABLED)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                // 小图标2
                if (image.badgeUri2 != null) {
                    Box(
                        modifier = Modifier.size(badgeSize)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(image.badgeUri2)
                                .crossfade(true)
                                .diskCachePolicy(CachePolicy.DISABLED)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                // 小图标3
                if (image.badgeUri3 != null) {
                    Box(
                        modifier = Modifier.size(badgeSize)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(image.badgeUri3)
                                .crossfade(true)
                                .diskCachePolicy(CachePolicy.DISABLED)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
        
        // 显示图片命名（如果有）
        if (image.name.isNotBlank()) {
            val nameFontSize = (9f * (imageSize.value / 70f)).coerceAtLeast(7f).sp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 2.dp, vertical = 1.dp)
            ) {
                Text(
                    text = TextUtils.truncateImageNameForEdit(image.name),
                    color = Color.White,
                    fontSize = nameFontSize,
                    maxLines = 1,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
