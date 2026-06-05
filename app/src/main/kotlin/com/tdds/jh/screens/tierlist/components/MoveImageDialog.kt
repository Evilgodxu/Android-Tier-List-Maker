package com.tdds.jh.screens.tierlist.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tdds.jh.R
import com.tdds.jh.model.tierlist.TierItem
import com.tdds.jh.screens.tierlist.logic.utils.ColorUtils
import com.tdds.jh.ui.theme.LocalExtendedColors

/**
 * 移动图片对话框
 * 用于将图片移动到其他层级或调整位置
 *
 * @param tiers 所有层级列表
 * @param currentTierLabel 当前层级标签
 * @param onDismiss 关闭对话框回调
 * @param onMoveToTier 移动到指定层级回调
 * @param onMoveToFirst 移动到首位回调
 * @param onMoveToLast 移动到末位回调
 * @param onMoveLeft 左移回调
 * @param onMoveRight 右移回调
 */
@Composable
fun MoveImageDialog(
    tiers: List<TierItem>,
    currentTierLabel: String,
    onDismiss: () -> Unit,
    onMoveToTier: (String) -> Unit,
    onMoveToFirst: () -> Unit,
    onMoveToLast: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit
) {
    val extendedColors = LocalExtendedColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = extendedColors.cardBackground,
        title = { Text(stringResource(R.string.move_to_tier)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(stringResource(R.string.select_target_tier))
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    items(tiers.filter { it.label != currentTierLabel }) { tier ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onMoveToTier(tier.label) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        tier.color,
                                        RoundedCornerShape(4.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tier.label,
                                    fontWeight = FontWeight.Bold,
                                    color = if (ColorUtils.isDarkColor(tier.color)) Color.White else Color.Black,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    fontSize = 12.sp,
                                    lineHeight = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = stringResource(R.string.tier_label, tier.label))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(R.string.move_position))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(
                        onClick = onMoveToFirst
                    ) {
                        Text(stringResource(R.string.move_to_first))
                    }
                    TextButton(
                        onClick = onMoveToLast
                    ) {
                        Text(stringResource(R.string.move_to_last))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(
                        onClick = onMoveLeft
                    ) {
                        Text(stringResource(R.string.move_left))
                    }
                    TextButton(
                        onClick = onMoveRight
                    ) {
                        Text(stringResource(R.string.move_right))
                    }
                }
            }
        },
        confirmButton = {}
    )
}
