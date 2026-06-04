package com.tdds.jh.screens.tierlist.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.tdds.jh.R
import com.tdds.jh.screens.tierlist.logic.utils.TextUtils
import com.tdds.jh.ui.theme.LocalExtendedColors

/**
 * 编辑图片名称对话框
 * 用于为图片设置或修改名称，支持中英文命名规则
 *
 * @param currentName 当前图片名称
 * @param onDismiss 关闭对话框回调
 * @param onConfirm 确认命名回调，返回输入的名称（空字符串表示不命名）
 */
@Composable
fun EditImageNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val extendedColors = LocalExtendedColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = extendedColors.cardBackground,
        title = { Text(stringResource(R.string.edit_image_name)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { newValue ->
                    // 根据命名规则限制输入长度
                    name = if (TextUtils.containsChinese(newValue)) {
                        // 中文名称：不超过10个字
                        newValue.take(10)
                    } else {
                        // 非中文名称：不超过18个字符
                        newValue.take(18)
                    }
                },
                label = { Text(stringResource(R.string.image_name)) },
                placeholder = { Text(currentName.ifBlank { stringResource(R.string.image_name_hint) }) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        // 为空则不添加命名(返回空字符串)
                        onConfirm(name.trim())
                    }
                )
            )
        },
        confirmButton = {},
        dismissButton = {}
    )
}
