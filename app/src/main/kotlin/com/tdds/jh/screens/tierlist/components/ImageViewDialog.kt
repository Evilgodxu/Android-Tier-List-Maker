package com.tdds.jh.screens.tierlist.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tdds.jh.R
import com.tdds.jh.ui.theme.LocalExtendedColors

/**
 * 查看图片对话框
 * 显示图片的完整视图，包含图片尺寸信息
 *
 * @param imageUri 图片 URI
 * @param onDismiss 关闭对话框回调
 */
@Composable
fun ImageViewDialog(
    imageUri: Uri,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val extendedColors = LocalExtendedColors.current

    // 使用 remember(imageUri) 确保当 URI 变化时重置 bitmap 状态
    var bitmap by remember(imageUri) { mutableStateOf<Bitmap?>(null) }

    // 加载图片获取尺寸
    LaunchedEffect(imageUri) {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            inputStream?.use { stream ->
                bitmap = BitmapFactory.decodeStream(stream)
            }
        } catch (_: Exception) {
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = extendedColors.cardBackground,
        title = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.view))
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(400.dp),
                    contentAlignment = Alignment.Center
                ) {
                    bitmap?.let { bmp ->
                        // 自适应缩放到 400x400 区域内，保持宽高比
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } ?: run {
                        // 加载中显示占位
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                // 显示图片分辨率
                bitmap?.let { bmp ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${bmp.width} x ${bmp.height}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}
