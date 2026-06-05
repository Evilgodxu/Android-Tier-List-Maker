package com.tdds.jh.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * 自定义垂直滚动条，支持触摸拖动交互
 *
 * @param scrollState 滚动状态
 * @param modifier 修饰符
 * @param thumbColor 滑块颜色
 * @param trackColor 轨道颜色
 * @param width 滚动条宽度
 * @param minThumbHeight 滑块最小高度
 */
@Composable
fun CustomVerticalScrollbar(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    thumbColor: Color = Color.Gray.copy(alpha = 0.5f),
    trackColor: Color = Color.Gray.copy(alpha = 0.12f),
    width: Dp = 14.dp,
    minThumbHeight: Dp = 28.dp
) {
    if (scrollState.maxValue <= 0) return

    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var dragStartRatio by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(width)
            .pointerInput(scrollState.maxValue) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        dragStartRatio = (offset.y / size.height).coerceIn(0f, 1f)
                        scope.launch {
                            scrollState.scrollTo(
                                (dragStartRatio * scrollState.maxValue).toInt()
                            )
                        }
                    },
                    onVerticalDrag = { _, dragAmount ->
                        val deltaRatio = dragAmount / size.height
                        dragStartRatio = (dragStartRatio + deltaRatio).coerceIn(0f, 1f)
                        scope.launch {
                            scrollState.scrollTo(
                                (dragStartRatio * scrollState.maxValue).toInt()
                            )
                        }
                    }
                )
            }
    ) {
        val trackHeightPx = with(density) { maxHeight.toPx() }
        val totalContentHeight = (scrollState.maxValue + scrollState.viewportSize).toFloat()
        if (totalContentHeight <= 0f) return@BoxWithConstraints

        val thumbFraction = trackHeightPx / totalContentHeight
        val thumbHeightPx = (thumbFraction * trackHeightPx)
            .coerceAtLeast(with(density) { minThumbHeight.toPx() })
        val maxThumbOffset = trackHeightPx - thumbHeightPx
        val thumbOffsetPx = if (scrollState.maxValue > 0) {
            (scrollState.value.toFloat() / scrollState.maxValue) * maxThumbOffset
        } else 0f

        // 轨道
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(width / 2))
                .background(trackColor)
        )

        // 滑块
        Box(
            modifier = Modifier
                .offset(y = with(density) { thumbOffsetPx.toDp() })
                .width(width)
                .height(with(density) { thumbHeightPx.toDp() })
                .clip(RoundedCornerShape(width / 2))
                .background(thumbColor)
        )
    }
}
