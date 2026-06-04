package com.tdds.jh.screens.tierlist.dialog

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tdds.jh.R
import com.tdds.jh.ui.theme.LocalExtendedColors

/**
 * 关于程序对话框
 * 显示应用信息、作者信息和开源许可
 *
 * @param onDismiss 关闭对话框回调
 * @param context 上下文，用于打开外部链接
 */
@Composable
fun AboutDialog(
    onDismiss: () -> Unit,
    context: Context
) {
    val extendedColors = LocalExtendedColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = extendedColors.cardBackground,
        title = { Text(stringResource(R.string.about)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.about_description),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "• ${stringResource(R.string.author_jianghan)}${stringResource(R.string.author_role_separator, stringResource(R.string.author_jianghan_role))}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "• ${stringResource(R.string.author_bug_reporter)}${stringResource(R.string.author_role_separator, stringResource(R.string.author_bug_reporter_role))}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "• ${stringResource(R.string.author_kimi)}${stringResource(R.string.author_role_separator, stringResource(R.string.author_kimi_role))}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "• ${stringResource(R.string.author_glm)}${stringResource(R.string.author_role_separator, stringResource(R.string.author_glm_role))}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = stringResource(R.string.open_source_license), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.about_contact_hint),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Evilgodxu/Tier-Master"))
                    context.startActivity(intent)
                }
            ) {
                Text(stringResource(R.string.open_source_repository))
            }
        },
        dismissButton = null
    )
}
