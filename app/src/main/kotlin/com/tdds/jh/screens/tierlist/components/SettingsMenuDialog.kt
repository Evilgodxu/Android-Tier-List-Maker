package com.tdds.jh.screens.tierlist.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.tdds.jh.R
import com.tdds.jh.ui.theme.LocalExtendedColors

/**
 * 设置项数据类
 *
 * @param title 菜单项标题
 * @param onClick 点击回调
 */
private data class SettingsItem(
    val title: String,
    val onClick: () -> Unit
)

/**
 * 设置菜单对话框（功能菜单）
 * 入口级别的对话框，提供应用主要功能的导航入口
 *
 * @param onDismiss 关闭对话框回调
 * @param onShowInstructions 显示使用说明回调
 * @param onShowFeedback 显示关于/反馈回调
 * @param onImagePackage 图包管理回调
 * @param onShowProgramSettings 程序设置回调
 * @param onManagePresets 预设管理回调
 */
@Composable
fun SettingsMenuDialog(
    onDismiss: () -> Unit,
    onShowInstructions: () -> Unit,
    onShowFeedback: () -> Unit,
    onImagePackage: () -> Unit,
    onShowProgramSettings: () -> Unit,
    onManagePresets: () -> Unit,
    onShowOpenSourceLicenses: () -> Unit
) {
    val extendedColors = LocalExtendedColors.current
    val settingsItems = listOf(
        SettingsItem(
            title = stringResource(R.string.image_package),
            onClick = onImagePackage
        ),
        SettingsItem(
            title = stringResource(R.string.manage_presets),
            onClick = onManagePresets
        ),
        SettingsItem(
            title = stringResource(R.string.program_settings),
            onClick = onShowProgramSettings
        ),
        SettingsItem(
            title = stringResource(R.string.instructions),
            onClick = onShowInstructions
        ),
        SettingsItem(
            title = stringResource(R.string.donate),
            onClick = onShowFeedback
        ),
        SettingsItem(
            title = stringResource(R.string.open_source_licenses),
            onClick = onShowOpenSourceLicenses
        )
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = extendedColors.cardBackground
            )
        ) {
            Column(
                modifier = Modifier.padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 标题
                Text(
                    text = stringResource(R.string.settings),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 菜单项
                settingsItems.forEach { item ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { item.onClick() }
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.title,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            softWrap = true,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
