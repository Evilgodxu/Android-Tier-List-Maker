package com.tdds.jh.screens.tierlist.components

import android.graphics.Rect
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tdds.jh.R
import com.tdds.jh.model.tierlist.TierItem
import com.tdds.jh.ui.theme.LocalExtendedColors

/**
 * 待分级图片区域组件
 * 显示待添加到层级的图片，支持左右滑动选择，向下拖动放置到层级
 *
 * @param images 待分级图片 URI 列表
 * @param tiers 层级列表（用于拖拽时检测目标层级）
 * @param tierRowPositions 层级位置信息（用于拖拽目标检测）
 * @param onClear 清空按钮点击回调
 * @param onAdd 添加按钮点击回调
 * @param onDragStart 拖拽开始回调
 * @param onDragEnd 拖拽结束回调
 * @param onDropOnTier 放置到层级回调
 * @param onDeleteImage 删除图片回调
 * @param floatOffsetX 浮动图片水平偏移量
 * @param floatOffsetY 浮动图片垂直偏移量
 * @param onPositionUpdate 位置更新回调
 */
@Composable
fun PendingImagesSection(
    images: List<Uri>,
    tiers: List<TierItem>,
    tierRowPositions: Map<String, Rect>,
    onClear: () -> Unit,
    onAdd: () -> Unit = {},
    onDragStart: (Uri) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDropOnTier: (Uri, String) -> Unit,
    onDeleteImage: (Uri) -> Unit = {},
    floatOffsetX: Float = 125f,
    floatOffsetY: Float = 85f,
    onPositionUpdate: ((Rect) -> Unit)? = null,
    isExpanded: Boolean = false
) {
    // 固定尺寸配置
    val imageSize = 85.dp
    val imageCornerRadius = 4.dp
    // 计算待放置区域的高度（基于图片尺寸）
    val pendingSectionHeight = (imageSize.value * 0.8f + 8f).dp
    // 拖动状态
    var isDragging by remember { mutableStateOf(false) }
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var draggedUri by remember { mutableStateOf<Uri?>(null) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }
    var currentDropTarget by remember { mutableStateOf<String?>(null) }

    // 使用 Box 包裹内容
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        val extendedColors = LocalExtendedColors.current
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .padding(top = 4.dp, bottom = 4.dp)
                .onGloballyPositioned { coordinates ->
                    onPositionUpdate?.let { update ->
                        val bounds = coordinates.boundsInWindow()
                        val rect = Rect(
                            bounds.left.toInt(),
                            bounds.top.toInt(),
                            bounds.right.toInt(),
                            bounds.bottom.toInt()
                        )
                        update(rect)
                    }
                },
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = extendedColors.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = if (isExpanded) 4.dp else 12.dp)
                    .padding(top = 4.dp, bottom = 4.dp)
            ) {
                // 待分级图片标题、添加按钮和清空按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.character_pool, images.size),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onAdd,
                            modifier = Modifier.padding(0.dp)
                        ) {
                            Text(
                                stringResource(R.string.add),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        TextButton(
                            onClick = onClear,
                            modifier = Modifier.padding(0.dp)
                        ) {
                            Text(
                                stringResource(R.string.clear),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // 图片待选区 - 待放置图片列表
                if (isExpanded) {
                    // 双列布局：两行水平滚动，同步滚动位置
                    val mid = (images.size + 1) / 2
                    val row1Images = images.take(mid)
                    val row2Images = images.drop(mid)
                    val row1State = rememberLazyListState()
                    val row2State = rememberLazyListState()

                    // 同步两行滚动位置
                    LaunchedEffect(row1State.firstVisibleItemIndex, row1State.firstVisibleItemScrollOffset) {
                        row2State.scrollToItem(row1State.firstVisibleItemIndex, row1State.firstVisibleItemScrollOffset)
                    }

                    Column {
                        LazyRow(
                            state = row1State,
                            modifier = Modifier.height(pendingSectionHeight)
                        ) {
                            itemsIndexed(row1Images, key = { index, uri -> "${index}_${uri}_${uri.hashCode()}" }) { index, uri ->
                                val globalIndex = index
                                DraggablePendingImageItem(
                                    uri = uri,
                                    isDragging = isDragging && draggedUri == uri,
                                    tiers = tiers,
                                    tierRowPositions = tierRowPositions,
                                    onDragStart = { uriItem, initialCenter ->
                                        isDragging = true
                                        draggedIndex = globalIndex
                                        draggedUri = uriItem
                                        dragPosition = initialCenter
                                        onDragStart(uriItem)
                                    },
                                    onDrag = { currentCenter, dropTarget ->
                                        dragPosition = currentCenter
                                        currentDropTarget = dropTarget
                                    },
                                    onDragEnd = { finalDropTarget ->
                                        finalDropTarget?.let { tierLabel ->
                                            draggedUri?.let { u ->
                                                onDropOnTier(u, tierLabel)
                                            }
                                        }
                                        isDragging = false
                                        draggedIndex = null
                                        draggedUri = null
                                        dragPosition = Offset.Zero
                                        currentDropTarget = null
                                        onDragEnd()
                                    }
                                )
                            }
                        }
                        LazyRow(
                            state = row2State,
                            modifier = Modifier.height(pendingSectionHeight)
                        ) {
                            itemsIndexed(row2Images, key = { index, uri -> "${index + mid}_${uri}_${uri.hashCode()}" }) { index, uri ->
                                val globalIndex = index + mid
                                DraggablePendingImageItem(
                                    uri = uri,
                                    isDragging = isDragging && draggedUri == uri,
                                    tiers = tiers,
                                    tierRowPositions = tierRowPositions,
                                    onDragStart = { uriItem, initialCenter ->
                                        isDragging = true
                                        draggedIndex = globalIndex
                                        draggedUri = uriItem
                                        dragPosition = initialCenter
                                        onDragStart(uriItem)
                                    },
                                    onDrag = { currentCenter, dropTarget ->
                                        dragPosition = currentCenter
                                        currentDropTarget = dropTarget
                                    },
                                    onDragEnd = { finalDropTarget ->
                                        finalDropTarget?.let { tierLabel ->
                                            draggedUri?.let { u ->
                                                onDropOnTier(u, tierLabel)
                                            }
                                        }
                                        isDragging = false
                                        draggedIndex = null
                                        draggedUri = null
                                        dragPosition = Offset.Zero
                                        currentDropTarget = null
                                        onDragEnd()
                                    }
                                )
                            }
                        }
                    }
                } else {
                    LazyRow(
                        modifier = Modifier.height(pendingSectionHeight)
                    ) {
                        itemsIndexed(images, key = { index, uri -> "${index}_${uri}_${uri.hashCode()}" }) { index, uri ->
                            DraggablePendingImageItem(
                                uri = uri,
                                isDragging = isDragging && draggedIndex == index,
                                tiers = tiers,
                                tierRowPositions = tierRowPositions,
                                onDragStart = { uriItem, initialCenter ->
                                    isDragging = true
                                    draggedIndex = index
                                    draggedUri = uriItem
                                    dragPosition = initialCenter
                                    onDragStart(uriItem)
                                },
                                onDrag = { currentCenter, dropTarget ->
                                    dragPosition = currentCenter
                                    currentDropTarget = dropTarget
                                },
                                onDragEnd = { finalDropTarget ->
                                    finalDropTarget?.let { tierLabel ->
                                        draggedUri?.let { uri ->
                                            onDropOnTier(uri, tierLabel)
                                        }
                                    }
                                    isDragging = false
                                    draggedIndex = null
                                    draggedUri = null
                                    dragPosition = Offset.Zero
                                    currentDropTarget = null
                                    onDragEnd()
                                }
                            )
                        }
                    }
                }
            }
        }

        // 浮动显示的被拖动图片（全屏层）
        if (isDragging && draggedUri != null) {
            FloatingDragImage(
                uri = draggedUri!!,
                position = dragPosition,
                dropTarget = currentDropTarget,
                floatOffsetX = floatOffsetX,
                floatOffsetY = floatOffsetY
            )
        }
    }
}
