package com.tdds.jh.screens.tierlist.components

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.tdds.jh.data.tierlist.PresetManager
import com.tdds.jh.R
import com.tdds.jh.ui.theme.LocalExtendedColors
import kotlin.math.roundToInt

/**
 * 设置小图标对话框
 * 用于为图片设置最多3个小图标，支持从预览区域选择或添加新图标
 *
 * @param badgeUri1 小图标1的URI
 * @param badgeUri2 小图标2的URI
 * @param badgeUri3 小图标3的URI
 * @param presetManager 预设管理器
 * @param onDismiss 关闭对话框回调
 * @param onSelectBadge1 选择小图标1回调
 * @param onSelectBadge2 选择小图标2回调
 * @param onSelectBadge3 选择小图标3回调
 * @param onDeleteBadge1 删除小图标1回调
 * @param onDeleteBadge2 删除小图标2回调
 * @param onDeleteBadge3 删除小图标3回调
 * @param onAddBadge 添加小图标回调
 * @param onSelectBadgeFromPreview 从预览区域选择小图标回调
 * @param onDeleteBadgeFromPreview 从预览区域删除小图标回调
 * @param externalRefreshKey 外部传入的刷新键，用于触发重新加载小图标列表
 */
@Composable
fun SetBadgeDialog(
    badgeUri1: Uri?,
    badgeUri2: Uri?,
    badgeUri3: Uri?,
    presetManager: PresetManager,
    onDismiss: () -> Unit,
    onSelectBadge1: () -> Unit,
    onSelectBadge2: () -> Unit,
    onSelectBadge3: () -> Unit,
    onDeleteBadge1: () -> Unit,
    onDeleteBadge2: () -> Unit,
    onDeleteBadge3: () -> Unit,
    onAddBadge: () -> Unit,
    onSelectBadgeFromPreview: (Uri, Int) -> Unit,
    onDeleteBadgeFromPreview: (Uri) -> Boolean,
    externalRefreshKey: Int = 0
) {
    val extendedColors = LocalExtendedColors.current
    var selectedBadgeSlot by remember { mutableStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = extendedColors.cardBackground,
        title = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.set_badges_title))
            }
        },
        text = {
            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isLandscape) {
                            Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())
                        } else {
                            Modifier
                        }
                    ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 小图标1选项
                BadgeSlotRow(
                    slotNumber = 1,
                    badgeUri = badgeUri1,
                    onSelect = onSelectBadge1,
                    onDelete = onDeleteBadge1
                )

                // 小图标2选项
                BadgeSlotRow(
                    slotNumber = 2,
                    badgeUri = badgeUri2,
                    onSelect = onSelectBadge2,
                    onDelete = onDeleteBadge2
                )

                // 小图标3选项
                BadgeSlotRow(
                    slotNumber = 3,
                    badgeUri = badgeUri3,
                    onSelect = onSelectBadge3,
                    onDelete = onDeleteBadge3
                )

                // 分隔线
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // 小图标槽位选择器
                Text(
                    text = stringResource(R.string.select_badge_slot),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (1..3).forEach { slot ->
                        val isSelected = selectedBadgeSlot == slot
                        val hasBadge = when (slot) {
                            1 -> badgeUri1 != null
                            2 -> badgeUri2 != null
                            else -> badgeUri3 != null
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedBadgeSlot = slot },
                            label = {
                                Text(
                                    stringResource(R.string.badge_slot_label, slot, if (hasBadge) "✓" else ""),
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            },
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .weight(1f)
                        )
                    }
                }

                // 添加小图标按钮
                Button(
                    onClick = onAddBadge,
                    modifier = Modifier.padding(vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.add_badge), fontSize = 14.sp)
                }

                // 小图标预览区域
                BadgePreviewArea(
                    presetManager = presetManager,
                    selectedBadgeSlot = selectedBadgeSlot,
                    badgeUri1 = badgeUri1,
                    badgeUri2 = badgeUri2,
                    badgeUri3 = badgeUri3,
                    externalRefreshKey = externalRefreshKey,
                    onSelectBadgeFromPreview = onSelectBadgeFromPreview,
                    onDeleteBadgeFromPreview = onDeleteBadgeFromPreview
                )
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

/**
 * 小图标槽位行组件
 */
@Composable
private fun BadgeSlotRow(
    slotNumber: Int,
    badgeUri: Uri?,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .wrapContentWidth()
            .clickable { onSelect() }
            .padding(vertical = 6.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Color.LightGray, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (badgeUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(badgeUri)
                        .crossfade(true)
                        .diskCachePolicy(CachePolicy.DISABLED)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Text(
                    text = stringResource(R.string.none),
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = when (slotNumber) {
                1 -> stringResource(R.string.badge_1)
                2 -> stringResource(R.string.badge_2)
                else -> stringResource(R.string.badge_3)
            },
            fontSize = 15.sp
        )
        Spacer(modifier = Modifier.width(12.dp))
        if (badgeUri != null) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = when (slotNumber) {
                        1 -> stringResource(R.string.delete_badge_1)
                        2 -> stringResource(R.string.delete_badge_2)
                        else -> stringResource(R.string.delete_badge_3)
                    },
                    tint = Color.Red,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.size(28.dp))
        }
    }
}

/**
 * 小图标预览区域 - 独立的Composable以实现局部刷新
 * 当小图标列表变化时,只有这个区域会重新加载,而不会导致整个对话框闪烁
 */
@Composable
fun BadgePreviewArea(
    presetManager: PresetManager,
    selectedBadgeSlot: Int,
    badgeUri1: Uri?,
    badgeUri2: Uri?,
    badgeUri3: Uri?,
    externalRefreshKey: Int,
    onSelectBadgeFromPreview: (Uri, Int) -> Unit,
    onDeleteBadgeFromPreview: (Uri) -> Boolean
) {
    var availableBadges by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var internalRefreshKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(externalRefreshKey, internalRefreshKey) {
        availableBadges = presetManager.getAvailableBadges()
    }

    if (availableBadges.isNotEmpty()) {
        Text(
            text = stringResource(R.string.click_badge_to_set),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(8.dp)
                )
                .padding(8.dp)
        ) {
            LazyRow(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                itemsIndexed(
                    items = availableBadges,
                    key = { index, badgeUri -> "${index}_${badgeUri}" }
                ) { index, badgeUri ->
                    var offsetY by remember(badgeUri) { mutableFloatStateOf(0f) }
                    val density = LocalDensity.current
                    val itemHeightPx = with(density) { 64.dp.toPx() }
                    val thresholdPx = itemHeightPx * 0.5f
                    val isDeleting = offsetY > thresholdPx

                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .offset { IntOffset(0, offsetY.roundToInt()) }
                            .pointerInput(badgeUri) {
                                detectVerticalDragGestures(
                                    onDragEnd = {
                                        if (offsetY > thresholdPx) {
                                            val success = onDeleteBadgeFromPreview(badgeUri)
                                            if (success) {
                                                internalRefreshKey++
                                            }
                                        }
                                        offsetY = 0f
                                    },
                                    onVerticalDrag = { change, dragAmount ->
                                        change.consume()
                                        offsetY += dragAmount
                                        if (offsetY < 0f) {
                                            offsetY = 0f
                                        }
                                        if (offsetY > itemHeightPx * 1.5f) {
                                            offsetY = itemHeightPx * 1.5f
                                        }
                                    }
                                )
                            }
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isDeleting) MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                                else Color.LightGray
                            )
                            .clickable {
                                onSelectBadgeFromPreview(badgeUri, selectedBadgeSlot)
                            }
                            .border(
                                width = 2.dp,
                                color = if (
                                    (selectedBadgeSlot == 1 && badgeUri1 == badgeUri) ||
                                    (selectedBadgeSlot == 2 && badgeUri2 == badgeUri) ||
                                    (selectedBadgeSlot == 3 && badgeUri3 == badgeUri)
                                ) Color(0xFF2196F3) else Color.Transparent,
                                shape = RoundedCornerShape(6.dp)
                            )
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(badgeUri)
                                .crossfade(true)
                                .diskCachePolicy(CachePolicy.DISABLED)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.no_badges_available),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
