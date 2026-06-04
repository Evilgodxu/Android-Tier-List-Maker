package com.tdds.jh.screens.tierlist.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.tdds.jh.R
import com.tdds.jh.ui.theme.LocalExtendedColors

/**
 * 使用说明对话框
 * 显示应用的使用说明和帮助信息
 *
 * @param onDismiss 关闭对话框回调
 */
@Composable
fun InstructionsDialog(
    onDismiss: () -> Unit
) {
    val extendedColors = LocalExtendedColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = extendedColors.cardBackground,
        title = { Text(stringResource(R.string.instructions)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.instructions_content),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}
