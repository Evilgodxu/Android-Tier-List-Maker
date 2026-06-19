package com.tdds.jh.screens.tierlist.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tdds.jh.R
import com.tdds.jh.ui.theme.LocalExtendedColors

/**
 * 图片操作对话框
 * 入口级对话框，提供图片的查看、裁剪、移动、替换、重命名、设置小图标等操作选项
 *
 * @param onDismiss 关闭对话框回调
 * @param onSetBadge 设置小图标回调
 * @param onReplace 替换图片回调
 * @param onMove 移动图片回调
 * @param onRename 重命名回调
 * @param onView 查看图片回调
 * @param onCrop 裁剪图片回调
 * @param onAudio 设置解说音频回调
 */
@Composable
fun ImageActionDialog(
    onDismiss: () -> Unit,
    onSetBadge: () -> Unit,
    onReplace: () -> Unit,
    onMove: () -> Unit,
    onRename: () -> Unit,
    onView: () -> Unit,
    onCrop: () -> Unit,
    onAudio: () -> Unit
) {
    val extendedColors = LocalExtendedColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = extendedColors.cardBackground,
        title = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.image_action))
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.select_operation))
                Spacer(modifier = Modifier.height(16.dp))
                // 第一行：查看、裁剪、移动
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = onView) {
                        Text(stringResource(R.string.view))
                    }
                    TextButton(onClick = onCrop) {
                        Text(stringResource(R.string.crop))
                    }
                    TextButton(onClick = onMove) {
                        Text(stringResource(R.string.move))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                // 第二行：替换、命名、设置小图标
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = onReplace) {
                        Text(stringResource(R.string.replace))
                    }
                    TextButton(onClick = onRename) {
                        Text(stringResource(R.string.rename))
                    }
                    TextButton(onClick = onSetBadge) {
                        Text(stringResource(R.string.set_badges))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                // 第三行：解说音频
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(onClick = onAudio) {
                        Text(stringResource(R.string.image_audio))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}
