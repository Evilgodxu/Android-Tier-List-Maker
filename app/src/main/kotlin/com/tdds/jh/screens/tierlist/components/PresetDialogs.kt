package com.tdds.jh.screens.tierlist.components

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.tdds.jh.data.tierlist.PresetInfo
import com.tdds.jh.data.tierlist.PresetManager
import com.tdds.jh.R
import com.tdds.jh.ui.theme.LocalExtendedColors
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

/**
 * 管理预设对话框
 *
 * @param onDismiss 关闭对话框回调
 * @param onExportPreset 导出预设回调
 * @param onImportPreset 导入预设回调
 * @param onSavePreset 保存预设回调
 * @param onManagePresetList 管理预设列表回调
 */
@Composable
fun ManagePresetsDialog(
    onDismiss: () -> Unit,
    onExportPreset: () -> Unit,
    onImportPreset: () -> Unit,
    onSavePreset: () -> Unit,
    onManagePresetList: () -> Unit
) {
    val extendedColors = LocalExtendedColors.current

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
                    text = stringResource(R.string.preset_manager_title),
                    fontSize = 20.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 功能按钮列表
                val items = listOf(
                    stringResource(R.string.export_preset) to onExportPreset,
                    stringResource(R.string.import_preset) to onImportPreset,
                    stringResource(R.string.save_preset) to onSavePreset,
                    stringResource(R.string.preset_list) to onManagePresetList
                )

                items.forEach { (title, onClick) ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onClick() }
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
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

/**
 * 通用加载对话框
 * @param message 加载提示文字
 */
@Composable
fun LoadingDialog(message: String) {
    val extendedColors = LocalExtendedColors.current

    Dialog(onDismissRequest = { }) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = extendedColors.cardBackground
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    strokeWidth = 4.dp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * 预设名称输入对话框
 *
 * @param defaultName 默认预设名称
 * @param onDismiss 关闭对话框回调
 * @param onConfirm 确认回调，返回输入的预设名称
 */
@Composable
fun PresetNameDialog(
    defaultName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(defaultName) }
    val extendedColors = LocalExtendedColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = extendedColors.cardBackground,
        title = { Text(stringResource(R.string.enter_preset_name)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.preset_name_hint)) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * 草稿恢复对话框
 * @param title 草稿标题
 * @param author 草稿作者
 * @param onDismiss 取消恢复
 * @param onRestore 确认恢复
 */
@Composable
fun DraftRestoreDialog(
    title: String,
    author: String,
    onDismiss: () -> Unit,
    onRestore: () -> Unit
) {
    val extendedColors = LocalExtendedColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = extendedColors.cardBackground,
        title = { Text(stringResource(R.string.restore_draft)) },
        text = {
            Column {
                Text(stringResource(R.string.draft_found_message))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.draft_title_label, title),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = stringResource(R.string.draft_author_label, author),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onRestore) {
                Text(stringResource(R.string.restore))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * 预设列表管理对话框
 *
 * @param presetManager 预设管理器实例
 * @param onDismiss 关闭对话框回调
 * @param onApplyPreset 应用预设回调
 */
@Composable
fun PresetListDialog(
    presetManager: PresetManager,
    onDismiss: () -> Unit,
    onApplyPreset: suspend (PresetInfo) -> Unit
) {
    val extendedColors = LocalExtendedColors.current
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    var presets by remember { mutableStateOf(presetManager.getAllPresets()) }
    var showDeleteConfirmDialog by remember { mutableStateOf<PresetInfo?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var loadingPresetName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        containerColor = extendedColors.cardBackground,
        title = { Text(stringResource(R.string.preset_list)) },
        text = {
            Box {
                if (isLoading) {
                    // 加载中状态
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 4.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.preset_applying, loadingPresetName),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else if (presets.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_presets),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        items(presets) { preset ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = preset.name,
                                    fontSize = 16.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Row {
                                    // 应用按钮
                                    TextButton(
                                        onClick = {
                                            isLoading = true
                                            loadingPresetName = preset.name
                                            scope.launch {
                                                // 让出时间片，确保UI有时间显示加载对话框
                                                yield()
                                                try {
                                                    // 在后台线程执行耗时操作
                                                    withContext(Dispatchers.IO) {
                                                        onApplyPreset(preset)
                                                    }
                                                    // 应用成功后关闭对话框
                                                    onDismiss()
                                                } catch (e: Exception) {
                                                    // 在主线程显示错误提示
                                                    withContext(Dispatchers.Main) {
                                                        android.widget.Toast.makeText(
                                                            context,
                                                            "应用预设失败: ${e.message}",
                                                            android.widget.Toast.LENGTH_LONG
                                                        ).show()
                                                    }
                                                } finally {
                                                    isLoading = false
                                                    loadingPresetName = ""
                                                }
                                            }
                                        },
                                        enabled = !isLoading
                                    ) {
                                        Text(stringResource(R.string.preset_apply))
                                    }
                                    // 删除按钮
                                    TextButton(
                                        onClick = { showDeleteConfirmDialog = preset },
                                        enabled = !isLoading
                                    ) {
                                        Text(
                                            stringResource(R.string.delete),
                                            color = Color.Red
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )

    // 删除确认对话框
    showDeleteConfirmDialog?.let { preset ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            containerColor = extendedColors.cardBackground,
            title = { Text(stringResource(R.string.delete)) },
            text = { Text(stringResource(R.string.preset_delete_confirm, preset.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (presetManager.deletePreset(preset)) {
                            presets = presetManager.getAllPresets()
                        }
                        showDeleteConfirmDialog = null
                    }
                ) {
                    Text(stringResource(R.string.delete), color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

/**
 * 导入预设覆盖确认对话框
 * 复用保存预设的交互逻辑，提供"覆盖"和"新建预设"选项
 *
 * @param presetName 已存在的预设名称
 * @param onDismiss 关闭对话框回调
 * @param onOverwrite 覆盖现有预设回调
 * @param onCreateNew 创建新预设回调
 */
@Composable
fun ImportOverwriteDialog(
    presetName: String,
    onDismiss: () -> Unit,
    onOverwrite: () -> Unit,
    onCreateNew: () -> Unit
) {
    val extendedColors = LocalExtendedColors.current

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
                modifier = Modifier.padding(vertical = 20.dp, horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.preset_name_exists),
                    fontSize = 20.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.preset_name_exists_message, presetName),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onOverwrite,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = extendedColors.buttonContainer,
                            contentColor = extendedColors.buttonContent
                        )
                    ) {
                        Text(stringResource(R.string.overwrite))
                    }

                    OutlinedButton(
                        onClick = onCreateNew,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.create_new_preset))
                    }
                }
            }
        }
    }
}


