package com.tdds.jh.screens.tierlist.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.tdds.jh.data.tierlist.PresetManager
import com.tdds.jh.R
import com.tdds.jh.data.tierlist.ResourceManager
import com.tdds.jh.data.tierlist.ResourceUsageDetail
import com.tdds.jh.ui.theme.LocalExtendedColors
import com.tdds.jh.ui.toast.showToastWithoutIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 创建资源管理状态（从 ResourceManager 提取到 UI 层）
 */
@Composable
fun rememberResourceState(
    presetManager: PresetManager
): Pair<ResourceManager.ResourceState, ResourceManager.ResourceActions> {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var details by remember { mutableStateOf(ResourceUsageDetail()) }
    var isCleaning by remember { mutableStateOf(false) }
    var isCalculating by remember { mutableStateOf(true) }

    fun calculateSize() {
        scope.launch {
            isCalculating = true
            withContext(Dispatchers.IO) {
                details = ResourceManager.calculateResourceDetails(context, presetManager)
            }
            isCalculating = false
        }
    }

    LaunchedEffect(Unit) { calculateSize() }

    val state = ResourceManager.ResourceState(
        details = details,
        isCalculating = isCalculating,
        isCleaning = isCleaning
    )

    val actions = ResourceManager.ResourceActions(
        refreshSize = { calculateSize() },
        cleanup = { onResettiermaster: () -> Unit, onComplete: () -> Unit ->
            scope.launch {
                isCleaning = true
                try {
                    ResourceManager.cleanupResources(context, presetManager, onResettiermaster, onComplete)
                } catch (_: Exception) {}
                withContext(Dispatchers.IO) {
                    details = ResourceManager.calculateResourceDetails(context, presetManager)
                }
                isCleaning = false
            }
        }
    )

    return state to actions
}

/**
 * 资源管理对话框
 *
 * @param onDismiss 关闭对话框回调
 * @param presetManager 预设管理器实例
 * @param onResettiermaster 重置 TierMaster 回调
 */
@Composable
fun ResourceManageDialog(
    onDismiss: () -> Unit,
    presetManager: PresetManager,
    onResettiermaster: () -> Unit
) {
    val context = LocalContext.current
    val extendedColors = LocalExtendedColors.current

    // 使用资源管理器的状态
    val (state, actions) = rememberResourceState(presetManager)

    // 显示清理确认对话框
    var showCleanupConfirm by remember { mutableStateOf(false) }

    // 清理确认对话框
    if (showCleanupConfirm) {
        AlertDialog(
            onDismissRequest = { showCleanupConfirm = false },
            title = {
                Text(
                    text = stringResource(R.string.confirm_cleanup),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.cleanup_data_title),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "• ${stringResource(R.string.cache_files_label)}：${ResourceManager.formatFileSize(state.details.cacheSize)}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• ${stringResource(R.string.work_images_label)}：${ResourceManager.formatFileSize(state.details.workImagesSize)}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• ${stringResource(R.string.drafts_label)}：${ResourceManager.formatFileSize(state.details.draftsSize)}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.packages_and_presets_kept),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.cleanup_warning),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                val cleanedMessage = stringResource(R.string.all_data_cleaned)
                TextButton(
                    onClick = {
                        showCleanupConfirm = false
                        actions.cleanup(
                            onResettiermaster,
                            {
                                showToastWithoutIcon(context, cleanedMessage)
                            }
                        )
                    }
                ) {
                    Text(
                        text = stringResource(R.string.confirm_cleanup),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showCleanupConfirm = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
            containerColor = extendedColors.cardBackground
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = extendedColors.cardBackground
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 标题
                Text(
                    text = stringResource(R.string.resource_management),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                if (state.isCalculating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.calculating),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    // 缓存（包含缓存目录、日志文件、临时ZIP目录和孤立文件）
                    ResourceSizeItem(
                        label = stringResource(R.string.cache_files_label),
                        size = state.details.cacheSize
                    )

                    // 工作图片
                    ResourceSizeItem(
                        label = stringResource(R.string.work_images_label),
                        size = state.details.workImagesSize
                    )

                    // 草稿文件
                    ResourceSizeItem(
                        label = stringResource(R.string.drafts_label),
                        size = state.details.draftsSize
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )

                    // 导入的图包（仅显示，不清理）
                    ResourceSizeItem(
                        label = stringResource(R.string.imported_packages),
                        size = state.details.importedPackagesSize,
                        note = stringResource(R.string.kept_note)
                    )

                    // 预设文件（仅显示，不清理）
                    ResourceSizeItem(
                        label = stringResource(R.string.preset_files),
                        size = state.details.presetsSize,
                        note = stringResource(R.string.kept_note)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )

                    // 可清理总计（不含导入的图包和预设文件）
                    val cleanableSize = state.details.cacheSize +
                            state.details.workImagesSize +
                            state.details.draftsSize
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.cleanable_total),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = ResourceManager.formatFileSize(cleanableSize),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 清理按钮
                val cleanableSize = state.details.cacheSize +
                        state.details.workImagesSize +
                        state.details.draftsSize
                Button(
                    onClick = { showCleanupConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isCleaning && !state.isCalculating && cleanableSize > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White
                    )
                ) {
                    if (state.isCleaning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.cleanup_button),
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 关闭按钮
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

/**
 * 资源大小项
 *
 * @param label 标签文本
 * @param size 文件大小（字节）
 * @param note 备注文本（可选）
 */
@Composable
private fun ResourceSizeItem(
    label: String,
    size: Long,
    note: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (note != null) {
                Text(
                    text = note,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
        Text(
            text = ResourceManager.formatFileSize(size),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (size > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
