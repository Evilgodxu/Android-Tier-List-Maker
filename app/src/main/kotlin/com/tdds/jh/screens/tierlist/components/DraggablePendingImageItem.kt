package com.tdds.jh.screens.tierlist.components

import android.graphics.Rect
import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.tdds.jh.model.tierlist.TierItem
import kotlin.math.abs

/**
 * 待添加区的图片项（拖动时原位置保持不动）
 * 支持垂直拖动到层级区域
 *
 * @param uri 图片 URI
 * @param isDragging 是否正在拖动
 * @param tiers 层级列表
 * @param tierRowPositions 层级行位置映射，用于计算放置目标
 * @param onDragStart 拖动开始回调，返回图片URI和中心偏移量
 * @param onDrag 拖动中回调，返回当前中心点和目标层级
 * @param onDragEnd 拖动结束回调，返回放置目标层级标签
 */
@Composable
fun DraggablePendingImageItem(
    uri: Uri,
    isDragging: Boolean,
    tiers: List<TierItem>,
    tierRowPositions: Map<String, Rect>,
    onDragStart: (Uri, Offset) -> Unit,
    onDrag: (Offset, String?) -> Unit,
    onDragEnd: (String?) -> Unit
) {
    // 固定尺寸配置
    val imageSize = 85.dp
    val imageCornerRadius = 4.dp
    val density = LocalDensity.current
    // 记录图片中心点的全局位置
    var itemCenterGlobal by remember { mutableStateOf(Offset.Zero) }
    // 记录拖动偏移量
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    // 本地拖动状态（用于长按后启动拖动）
    var localIsDragging by remember { mutableStateOf(false) }
    // 使用 rememberUpdatedState 确保总是使用最新的 tierRowPositions
    val currentTierRowPositions by rememberUpdatedState(tierRowPositions)
    // 计算待放置区图片尺寸（比层级中的图片稍小）
    val pendingImageSize = (imageSize.value * 0.8f).dp

    Box(
        modifier = Modifier
            .size(pendingImageSize)
            .padding(2.dp)
            .then(
                if (isDragging || localIsDragging) {
                    Modifier.border(2.dp, Color(0xFF2196F3), RoundedCornerShape(imageCornerRadius))
                } else Modifier
            )
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInWindow()
                // 计算中心点位置（在窗口坐标系中）
                itemCenterGlobal = Offset(
                    bounds.left + bounds.width / 2,
                    bounds.top + bounds.height / 2
                )
            }
            .pointerInput(Unit) {
                // 使用底层 API 处理手势
                // 往下拖动超过此阈值才触发拖放
                val verticalDragThreshold = 20f
                val horizontalDragThreshold = 30f // 水平滑动阈值，超过此值才认为是水平滑动
                
                awaitPointerEventScope {
                    while (true) {
                        // 等待手指按下
                        val down = awaitFirstDown()
                        val startPosition = down.position
                        var dragTriggered = false
                        var isHorizontalScroll = false
                        
                        down.consume()
                        
                        // 持续处理后续事件
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            
                            // 手指抬起
                            if (change.changedToUpIgnoreConsumed()) {
                                if (localIsDragging) {
                                    // 结束拖动，计算放置目标
                                    val finalCenter = itemCenterGlobal + dragOffset
                                    var foundTier: String? = null
                                    for ((tierLabel, rect) in currentTierRowPositions) {
                                        if (finalCenter.x >= rect.left && finalCenter.x <= rect.right &&
                                            finalCenter.y >= rect.top && finalCenter.y <= rect.bottom) {
                                            foundTier = tierLabel
                                            break
                                        }
                                    }
                                    onDragEnd(foundTier)
                                    localIsDragging = false
                                    dragOffset = Offset.Zero
                                }
                                break
                            }

                            // 手指移动
                            val currentPosition = change.position
                            val deltaY = currentPosition.y - startPosition.y
                            val deltaX = currentPosition.x - startPosition.x

                            // 如果还没触发拖动，检测是否往下或往上拖动超过阈值
                            if (!dragTriggered) {
                                // 检测是否为水平滑动（水平移动更多且超过阈值）
                                if (kotlin.math.abs(deltaX) > kotlin.math.abs(deltaY) && kotlin.math.abs(deltaX) > horizontalDragThreshold) {
                                    // 水平滑动，不消费事件，让父组件(LazyRow)处理
                                    isHorizontalScroll = true
                                    break
                                }
                                // 如果往下或往上拖动超过阈值，触发拖放
                                if (deltaY > verticalDragThreshold || deltaY < -verticalDragThreshold) {
                                    dragTriggered = true
                                    dragOffset = Offset.Zero
                                    localIsDragging = true
                                    onDragStart(uri, itemCenterGlobal)
                                    change.consume()
                                }
                                // 移动距离还不够，继续等待
                                continue
                            }

                            // 拖动已触发，处理拖动
                            // 计算相对于起始位置的偏移
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
                            onDrag(currentCenter, foundTier)
                        }
                    }
                }
            }
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
                .clip(RoundedCornerShape(imageCornerRadius))
                .then(if (isDragging) Modifier.alpha(0.5f) else Modifier),
            contentScale = ContentScale.Crop
        )
    }
}
