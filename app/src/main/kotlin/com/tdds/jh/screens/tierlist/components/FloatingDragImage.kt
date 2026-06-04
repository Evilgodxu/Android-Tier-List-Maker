package com.tdds.jh.screens.tierlist.components

import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlin.math.roundToInt

/**
 * 浮动显示的被拖动图片
 * 在图片拖拽过程中跟随手指显示图片预览
 *
 * @param uri 图片 URI
 * @param position 当前手指位置
 * @param dropTarget 当前悬停的放置目标（层级标签），null 表示无目标
 * @param floatOffsetX 图片相对于手指的水平偏移量
 * @param floatOffsetY 图片相对于手指的垂直偏移量
 */
@Composable
fun FloatingDragImage(
    uri: Uri,
    position: Offset,
    dropTarget: String?,
    floatOffsetX: Float = 125f,
    floatOffsetY: Float = 85f
) {
    val scale by animateFloatAsState(
        targetValue = if (dropTarget != null) 1.15f else 1.1f,
        label = ""
    )

    // 使用 Popup 在全屏窗口中显示拖动图片，这样不会遮挡层级
    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(0, 0),
        properties = PopupProperties(
            focusable = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // 使用调节浮显设置的偏移量
            val offsetX = floatOffsetX.dp
            val offsetY = floatOffsetY.dp

            // 图片显示位置：根据滑动条设置的偏移量
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .offset {
                        IntOffset(
                            (position.x - offsetX.toPx()).roundToInt(),
                            (position.y - offsetY.toPx()).roundToInt()
                        )
                    }
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        alpha = 0.95f
                    )
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
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                            width = if (dropTarget != null) 4.dp else 3.dp,
                            color = if (dropTarget != null) Color(0xFF4CAF50) else Color(0xFF2196F3),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .shadow(
                            elevation = if (dropTarget != null) 12.dp else 8.dp,
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentScale = ContentScale.Crop
                )

                // 显示放置提示
                if (dropTarget != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF4CAF50).copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        // 限制层级名称显示：中文2个字，非中文3个字符
                        val displayText = if (dropTarget.any { it.code in 0x4E00..0x9FFF }) {
                            // 包含中文字符，限制为2个字
                            dropTarget.take(2)
                        } else {
                            // 非中文，限制为3个字符
                            dropTarget.take(3)
                        }
                        Text(
                            text = displayText,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier
                                .background(Color(0xFF4CAF50), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
