package com.tdds.jh.screens.tierlist.dialog

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tdds.jh.R
import com.tdds.jh.ui.theme.LocalExtendedColors

/**
 * 预览对话框
 * 显示生成的 tier list 图片预览,支持主题切换、保存和分享
 */
@Composable
fun PreviewDialog(
    bitmap: Bitmap,
    isSaving: Boolean = false,
    isSharing: Boolean = false,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onShare: (() -> Unit)? = null,
    onThemeToggle: (() -> Unit)? = null,
    isDarkTheme: Boolean = false,
    appDarkTheme: Boolean = false
) {
    val extendedColors = LocalExtendedColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = extendedColors.cardBackground,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.preview_title))
                // 主题切换按钮 - 图标颜色跟随界面主题
                if (onThemeToggle != null && !isSaving && !isSharing) {
                    IconButton(onClick = onThemeToggle) {
                        // 图标形状根据预览主题决定:预览深色显示太阳,预览浅色显示月亮
                        val iconRes = if (appDarkTheme) R.drawable.ic_sun_light else R.drawable.ic_moon_light
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = if (isDarkTheme) stringResource(R.string.switch_to_light_theme) else stringResource(R.string.switch_to_dark_theme),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = stringResource(R.string.preview_image_desc),
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.Fit
                        )

                        // 加载对话框覆盖层 - 只覆盖图片区域
                        if (isSaving || isSharing) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(Color.Black.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (isSaving) stringResource(R.string.saving_chart) else stringResource(R.string.sharing_chart),
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row {
                // 分享按钮
                if (onShare != null) {
                    TextButton(
                        onClick = onShare,
                        enabled = !isSaving && !isSharing
                    ) {
                        if (isSharing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(stringResource(R.string.share))
                        }
                    }
                }
                // 保存按钮
                TextButton(
                    onClick = onSave,
                    enabled = !isSaving && !isSharing
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(stringResource(R.string.confirm_save))
                    }
                }
            }
        }
    )
}
