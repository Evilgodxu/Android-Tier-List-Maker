package com.tdds.jh.screens.tierlist.components

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.tdds.jh.data.tierlist.PresetManager
import com.tdds.jh.R
import com.tdds.jh.data.tierlist.ImageResourceManager
import com.tdds.jh.data.tierlist.ImportTarget
import com.tdds.jh.data.tierlist.PackageItem
import com.tdds.jh.data.tierlist.PackageManager as ResourcePackageManager
import com.tdds.jh.ui.theme.LocalExtendedColors
import com.tdds.jh.ui.toast.showToastWithoutIcon
import kotlinx.coroutines.launch
import java.io.File

/**
 * 管理图包对话框
 * 提供已导入图包的查看、导入、导出和删除功能
 *
 * @param context Android 上下文
 * @param presetManager 预设管理器实例
 * @param onDismiss 关闭对话框回调
 * @param onImportPackage 导入图包回调
 * @param onPackageSelected 选择图包回调（用于导入到工作区）
 * @param onExportPackage 导出图包回调
 */
@Composable
fun ManagePackagesDialog(
    context: Context,
    presetManager: PresetManager,
    onDismiss: () -> Unit,
    onImportPackage: () -> Unit,
    onPackageSelected: (PackageItem.Imported) -> Unit,
    onExportPackage: (PackageItem.Imported) -> Unit
) {
    val extendedColors = LocalExtendedColors.current
    var importedPackages by remember { mutableStateOf(presetManager.getImportedPackages()) }
    var showDeleteConfirm by remember { mutableStateOf<PackageItem.Imported?>(null) }
    val scope = rememberCoroutineScope()

    // 刷新导入的图包列表
    fun refreshImportedPackages() {
        importedPackages = presetManager.getImportedPackages()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
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
                    text = stringResource(R.string.manage_packages_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 导入图包按钮
                Button(
                    onClick = {
                        onImportPackage()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = extendedColors.buttonContainer,
                        contentColor = extendedColors.buttonContent
                    )
                ) {
                    Text(stringResource(R.string.import_package_short))
                }

                // 已导入图包区域
                if (importedPackages.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.imported_packages),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 150.dp)
                            .padding(horizontal = 16.dp)
                    ) {
                        items(importedPackages) { packageFile ->
                            val packageItem = PackageItem.Imported(
                                name = packageFile.nameWithoutExtension,
                                file = packageFile
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = packageItem.name,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // 导出按钮
                                    TextButton(
                                        onClick = { onExportPackage(packageItem) }
                                    ) {
                                        Text(stringResource(R.string.export_package))
                                    }
                                    TextButton(
                                        onClick = { onPackageSelected(packageItem) }
                                    ) {
                                        Text(stringResource(R.string.import_package_short))
                                    }
                                    TextButton(
                                        onClick = { showDeleteConfirm = packageItem }
                                    ) {
                                        Text(
                                            stringResource(R.string.delete),
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 删除确认对话框
    showDeleteConfirm?.let { packageToDelete ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            containerColor = extendedColors.cardBackground,
            title = { Text(stringResource(R.string.confirm_delete)) },
            text = { Text(stringResource(R.string.confirm_delete_message, packageToDelete.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            presetManager.deleteImportedPackage(packageToDelete.file)
                            refreshImportedPackages()
                            showDeleteConfirm = null
                        }
                    }
                ) {
                    Text(
                        stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

/**
 * 图包确认对话框（选择导入位置）
 * 用于选择图包图片的导入目标区域
 *
 * @param packageName 图包名称
 * @param imageCount 图片数量
 * @param isImporting 是否正在导入中
 * @param onDismiss 关闭对话框回调
 * @param onConfirm 确认导入回调，返回导入目标位置
 */
@Composable
fun PackageConfirmDialog(
    packageName: String,
    imageCount: Int,
    isImporting: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (ImportTarget) -> Unit
) {
    val extendedColors = LocalExtendedColors.current

    if (isImporting) {
        // 导入中状态显示
        AlertDialog(
            onDismissRequest = { },
            containerColor = extendedColors.cardBackground,
            title = { Text(stringResource(R.string.importing)) },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(R.string.importing), fontSize = 14.sp)
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    } else {
        // 显示导入目标选择对话框
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = extendedColors.cardBackground,
            title = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.select_import_target))
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.import_target_description),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { onConfirm(ImportTarget.PENDING) },
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = extendedColors.buttonContainer,
                                contentColor = extendedColors.buttonContent
                            )
                        ) {
                            Text(stringResource(R.string.import_to_pending))
                        }
                        Button(
                            onClick = { onConfirm(ImportTarget.BADGES) },
                            modifier = Modifier.weight(1f).padding(start = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = extendedColors.buttonContainer,
                                contentColor = extendedColors.buttonContent
                            )
                        ) {
                            Text(stringResource(R.string.import_to_badges))
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

/**
 * 导入图包对话框
 * 提供选择 ZIP 文件、输入密码、选择导入位置等功能
 *
 * @param context Android 上下文
 * @param presetManager 预设管理器实例
 * @param pendingImages 当前待分级图片列表
 * @param onPendingImagesChanged 待分级图片变化回调
 * @param onDismiss 关闭对话框回调
 * @param onSkipDraftSave 跳过草稿保存回调（用于导入前）
 * @param onResumeDraftSave 恢复草稿保存回调（用于导入后）
 */
@Composable
fun ImportPackageDialog(
    context: Context,
    presetManager: PresetManager,
    pendingImages: List<Uri>,
    onPendingImagesChanged: (List<Uri>) -> Unit,
    onDismiss: () -> Unit,
    onSkipDraftSave: (() -> Unit)? = null,
    onResumeDraftSave: (() -> Unit)? = null
) {
    val extendedColors = LocalExtendedColors.current
    var isImporting by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showTargetDialog by remember { mutableStateOf(false) }
    var pendingZipUri by remember { mutableStateOf<Uri?>(null) }
    var pendingZipFileName by remember { mutableStateOf<String>("") }
    var pendingPassword by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // ZIP文件选择器
    val zipPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { zipUri ->
            pendingZipUri = zipUri
            pendingPassword = null
            // 获取原始文件名
            pendingZipFileName = ImageResourceManager.getFileNameFromUri(zipUri) ?: "imported_${System.currentTimeMillis()}.zip"
            // 先检测ZIP是否加密，然后显示导入目标选择对话框
            scope.launch {
                isImporting = true
                try {
                    // 检测ZIP是否加密
                    val tempDir = context.cacheDir.resolve("zip_check_${System.currentTimeMillis()}")
                    tempDir.mkdirs()
                    val tempZipFile = File(tempDir, "temp.zip")
                    context.contentResolver.openInputStream(zipUri)?.use { input ->
                        tempZipFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    val zipFile = net.lingala.zip4j.ZipFile(tempZipFile)
                    val isEncrypted = zipFile.isEncrypted
                    tempDir.deleteRecursively()

                    isImporting = false
                    if (isEncrypted) {
                        // ZIP已加密，需要密码，显示密码对话框
                        showPasswordDialog = true
                    } else {
                        // 未加密，直接显示导入目标选择对话框
                        showTargetDialog = true
                    }
                } catch (e: Exception) {
                    isImporting = false
                    showToastWithoutIcon(
                        context,
                        context.getString(R.string.import_failed, e.message),
                        android.widget.Toast.LENGTH_LONG
                    )
                    onDismiss()
                }
            }
        } ?: run {
            // 用户取消了选择，恢复草稿保存
            onResumeDraftSave?.invoke()
        }
    }

    // 处理带密码的ZIP导入确认
    fun processZipWithPassword(password: String) {
        pendingPassword = password
        showPasswordDialog = false
        showTargetDialog = true
    }

    // 处理导入目标选择
    fun handleImportTargetSelected(target: ImportTarget) {
        // 先显示导入中，然后解压并保存
        scope.launch {
            isImporting = true
            showTargetDialog = false

            try {
                pendingZipUri?.let { uri ->
                    // 确定目标目录
                    val targetDir = when (target) {
                        ImportTarget.PENDING -> File(presetManager.getWorkImagesDirectory(), PresetManager.IMAGES_FOLDER_NAME)
                        ImportTarget.BADGES -> File(presetManager.getWorkImagesDirectory(), PresetManager.BADGES_FOLDER_NAME)
                    }

                    // 解压ZIP文件到目标目录
                    val imageUris = ResourcePackageManager.importImagesFromZip(context, uri, targetDir, pendingPassword)
                    if (imageUris.isNotEmpty()) {
                        // 保存图包到图包目录，使用原始文件名
                        presetManager.saveImportedPackage(uri, pendingZipFileName)

                        isImporting = false
                        // 直接更新UI，不需要再复制
                        when (target) {
                            ImportTarget.PENDING -> {
                                onPendingImagesChanged(pendingImages + imageUris)
                                showToastWithoutIcon(
                                    context,
                                    context.getString(R.string.import_success, imageUris.size)
                                )
                            }
                            ImportTarget.BADGES -> {
                                showToastWithoutIcon(
                                    context,
                                    context.getString(R.string.badge_added)
                                )
                            }
                        }
                        onDismiss()
                    } else {
                        isImporting = false
                        showToastWithoutIcon(
                            context,
                            context.getString(R.string.no_images_in_zip)
                        )
                        onDismiss()
                    }
                }
            } catch (e: Exception) {
                isImporting = false
                showToastWithoutIcon(
                    context,
                    context.getString(R.string.import_failed, e.message),
                    android.widget.Toast.LENGTH_LONG
                )
                onDismiss()
            } finally {
                // 恢复草稿保存
                onResumeDraftSave?.invoke()
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isImporting) onDismiss() },
        containerColor = extendedColors.cardBackground,
        title = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.import_image_package))
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isImporting) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.importing))
                } else {
                    Text(
                        text = stringResource(R.string.import_package_description),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            onSkipDraftSave?.invoke()
                            zipPicker.launch("application/zip")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = extendedColors.buttonContainer,
                            contentColor = extendedColors.buttonContent
                        )
                    ) {
                        Text(stringResource(R.string.select_zip_file))
                    }
                }
            }
        },
        confirmButton = {}
    )

    // 密码输入对话框（仅在有密码保护时显示）
    if (showPasswordDialog) {
        ZipPasswordDialog(
            showError = passwordError,
            onDismiss = {
                showPasswordDialog = false
                pendingZipUri = null
                onDismiss()
            },
            onConfirm = { password ->
                if (password.isNotBlank()) {
                    processZipWithPassword(password)
                }
            }
        )
    }

    // 导入位置选择对话框
    if (showTargetDialog) {
        ImportTargetDialog(
            onDismiss = {
                showTargetDialog = false
                pendingZipUri = null
                pendingPassword = null
                onDismiss()
            },
            onTargetSelected = { target ->
                handleImportTargetSelected(target)
            }
        )
    }
}

/**
 * ZIP密码输入对话框
 *
 * @param showError 是否显示密码错误提示
 * @param onDismiss 关闭对话框回调
 * @param onConfirm 确认回调，返回输入的密码
 */
@Composable
fun ZipPasswordDialog(
    showError: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    val extendedColors = LocalExtendedColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = extendedColors.cardBackground,
        title = { Text(stringResource(R.string.enter_zip_password)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.zip_password_hint),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.password)) },
                    singleLine = true,
                    isError = showError
                )
                if (showError) {
                    Text(
                        text = stringResource(R.string.wrong_password),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (password.isNotBlank()) onConfirm(password) }) {
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
 * 导入位置选择对话框
 *
 * @param onDismiss 关闭对话框回调
 * @param onTargetSelected 选择目标位置回调
 */
@Composable
fun ImportTargetDialog(
    onDismiss: () -> Unit,
    onTargetSelected: (ImportTarget) -> Unit
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
                Text(stringResource(R.string.select_import_target))
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.import_target_description),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { onTargetSelected(ImportTarget.PENDING) },
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = extendedColors.buttonContainer,
                            contentColor = extendedColors.buttonContent
                        )
                    ) {
                        Text(stringResource(R.string.import_to_pending))
                    }
                    Button(
                        onClick = { onTargetSelected(ImportTarget.BADGES) },
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = extendedColors.buttonContainer,
                            contentColor = extendedColors.buttonContent
                        )
                    ) {
                        Text(stringResource(R.string.import_to_badges))
                    }
                }
            }
        },
        confirmButton = {}
    )
}
