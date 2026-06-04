package com.tdds.jh.screens.tierlist.handler

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.tdds.jh.data.manager.PresetManager
import com.tdds.jh.data.resource.PackageItem
import com.tdds.jh.data.resource.PackageManager as ResourcePackageManager
import com.tdds.jh.data.resource.ZipPasswordRequiredException
import com.tdds.jh.screens.tierlist.state.DialogState
import com.tdds.jh.ui.toast.showToastWithoutIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 图包操作处理类
 * 封装图包导入、导出的业务逻辑
 */
class PackageOperationHandler(
    private val context: Context,
    private val scope: CoroutineScope,
    private val dialogState: DialogState,
    private val presetManager: PresetManager,
    private val onPendingImagesChange: (List<Uri>) -> Unit,
    private val onSkipDraftSave: (() -> Unit)?,
    private val onResumeDraftSave: (() -> Unit)?,
    private val showToast: (String, Int) -> Unit
) {

    // ==================== 图包导出 ====================

    /**
     * 处理图包导出
     *
     * @param uri 导出目标URI
     * @param packageItem 要导出的图包项
     */
    fun handleExportPackage(
        uri: Uri?,
        packageItem: PackageItem.Imported?
    ) {
        if (uri == null || packageItem == null) {
            dialogState.packageToExport = null
            onResumeDraftSave?.invoke()
            return
        }

        dialogState.isExportingPackage = true
        onSkipDraftSave?.invoke()

        scope.launch {
            try {
                val success = withContext(Dispatchers.IO) {
                    ResourcePackageManager.exportPackageAsWebP(
                        context,
                        packageItem.file,
                        uri
                    )
                }

                if (success) {
                    showToast("导出图包成功", android.widget.Toast.LENGTH_SHORT)
                } else {
                    showToast("导出图包失败", android.widget.Toast.LENGTH_LONG)
                }

            } catch (e: Exception) {
                showToast("导出图包失败: ${e.message}", android.widget.Toast.LENGTH_LONG)
            } finally {
                dialogState.isExportingPackage = false
                dialogState.packageToExport = null
                onResumeDraftSave?.invoke()
            }
        }
    }

    /**
     * 启动图包导出流程
     *
     * @param packageItem 要导出的图包项
     * @param onLaunchExport 启动导出回调
     */
    fun startExportPackage(
        packageItem: PackageItem.Imported,
        onLaunchExport: () -> Unit
    ) {
        dialogState.packageToExport = packageItem
        onSkipDraftSave?.invoke()
        onLaunchExport()
    }

    // ==================== 图包导入 ====================

    /**
     * 处理从ZIP导入图片到待分级区域
     *
     * @param zipUri ZIP文件URI
     * @param password ZIP密码（可选）
     * @param currentPendingImages 当前待分级图片列表
     */
    fun handleImportZipToPending(
        zipUri: Uri?,
        password: String?,
        currentPendingImages: List<Uri>
    ) {
        if (zipUri == null) {
            dialogState.isImportingPackage = false
            onResumeDraftSave?.invoke()
            return
        }

        dialogState.isImportingPackage = true
        onSkipDraftSave?.invoke()

        scope.launch {
            try {
                val workImagesDir = presetManager.getWorkImagesDirectory()
                val imagesDir = java.io.File(workImagesDir, "images")

                val importedUris = withContext(Dispatchers.IO) {
                    ResourcePackageManager.importImagesFromZip(
                        context,
                        zipUri,
                        imagesDir,
                        password
                    )
                }

                // 追加到现有待分级图片
                onPendingImagesChange(currentPendingImages + importedUris)

                showToast("已导入 ${importedUris.size} 张图片", android.widget.Toast.LENGTH_SHORT)

            } catch (e: ZipPasswordRequiredException) {
                showToast("ZIP文件需要密码", android.widget.Toast.LENGTH_LONG)
                // 保持导入状态，等待用户输入密码
                return@launch
            } catch (e: Exception) {
                showToast("导入失败: ${e.message}", android.widget.Toast.LENGTH_LONG)
            } finally {
                dialogState.isImportingPackage = false
                onResumeDraftSave?.invoke()
            }
        }
    }

    /**
     * 处理从ZIP导入图片到小图标区域
     */
    fun handleImportZipToBadges(
        zipUri: Uri?,
        password: String?,
        onBadgeDialogRefresh: () -> Unit
    ) {
        if (zipUri == null) {
            dialogState.isImportingPackage = false
            onResumeDraftSave?.invoke()
            return
        }

        dialogState.isImportingPackage = true
        onSkipDraftSave?.invoke()

        scope.launch {
            try {
                val workBadgesDir = java.io.File(
                    presetManager.getWorkImagesDirectory(),
                    PresetManager.BADGES_FOLDER_NAME
                )

                val importedUris = withContext(Dispatchers.IO) {
                    ResourcePackageManager.importImagesFromZip(
                        context,
                        zipUri,
                        workBadgesDir,
                        password
                    )
                }

                onBadgeDialogRefresh()

                showToast("已导入 ${importedUris.size} 张小图标", android.widget.Toast.LENGTH_SHORT)

            } catch (e: ZipPasswordRequiredException) {
                showToast("ZIP文件需要密码", android.widget.Toast.LENGTH_LONG)
                return@launch
            } catch (e: Exception) {
                showToast("导入失败: ${e.message}", android.widget.Toast.LENGTH_LONG)
            } finally {
                dialogState.isImportingPackage = false
                onResumeDraftSave?.invoke()
            }
        }
    }

    /**
     * 获取图包中的图片数量
     */
    fun getPackageImageCount(
        packageItem: PackageItem.Imported,
        onCountCalculated: (Int) -> Unit
    ) {
        scope.launch {
            val count = withContext(Dispatchers.IO) {
                ResourcePackageManager.countImagesInImportedPackage(packageItem.file)
            }
            onCountCalculated(count)
        }
    }

    /**
     * 处理外部图包导入
     * 先检测ZIP是否加密，如需要密码则显示密码输入对话框
     */
    fun handleExternalPackageImport(
        uri: Uri,
        fileName: String?,
        onLoadingStateChange: (Boolean) -> Unit
    ) {
        scope.launch {
            try {
                // 先检测ZIP是否加密
                val tempDir = context.cacheDir.resolve("zip_check_${System.currentTimeMillis()}")
                tempDir.mkdirs()
                val tempZipFile = java.io.File(tempDir, "temp.zip")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempZipFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                val zipFile = net.lingala.zip4j.ZipFile(tempZipFile)
                val isEncrypted = zipFile.isEncrypted
                tempDir.deleteRecursively()

                if (isEncrypted) {
                    // ZIP已加密，需要密码，显示密码对话框
                    dialogState.externalPackageUri = uri
                    dialogState.externalPackageFileName = fileName ?: "imported_${System.currentTimeMillis()}.zip"
                    dialogState.externalPackagePassword = null
                    dialogState.externalPackagePasswordError = false
                    dialogState.showExternalPackagePasswordDialog = true
                } else {
                    // 未加密，直接导入
                    importExternalPackageInternal(uri, fileName, onLoadingStateChange)
                }
            } catch (e: Exception) {
                showToast("图包导入失败: ${e.message}", android.widget.Toast.LENGTH_SHORT)
                onLoadingStateChange(false)
            }
        }
    }

    /**
     * 使用密码继续外部图包导入
     */
    fun continueExternalPackageImportWithPassword(
        password: String,
        onLoadingStateChange: (Boolean) -> Unit
    ) {
        val uri = dialogState.externalPackageUri
        val fileName = dialogState.externalPackageFileName
        if (uri == null) {
            dialogState.showExternalPackagePasswordDialog = false
            onLoadingStateChange(false)
            return
        }

        dialogState.externalPackagePassword = password
        dialogState.showExternalPackagePasswordDialog = false
        importExternalPackageInternal(uri, fileName, onLoadingStateChange, password)
    }

    /**
     * 内部方法：执行外部图包导入
     */
    private fun importExternalPackageInternal(
        uri: Uri,
        fileName: String?,
        onLoadingStateChange: (Boolean) -> Unit,
        password: String? = null
    ) {
        scope.launch {
            try {
                val actualFileName = fileName ?: "imported_${System.currentTimeMillis()}.zip"
                val savedPackageFile = withContext(Dispatchers.IO) {
                    presetManager.saveImportedPackage(uri, actualFileName, password)
                }
                if (savedPackageFile != null) {
                    showToast("图包导入成功: ${savedPackageFile.nameWithoutExtension}", android.widget.Toast.LENGTH_SHORT)
                } else {
                    showToast("图包导入失败", android.widget.Toast.LENGTH_SHORT)
                }
            } catch (e: ZipPasswordRequiredException) {
                dialogState.externalPackagePasswordError = true
                dialogState.showExternalPackagePasswordDialog = true
            } catch (e: Exception) {
                showToast("图包导入失败: ${e.message}", android.widget.Toast.LENGTH_SHORT)
            } finally {
                // 清理状态
                dialogState.externalPackageUri = null
                dialogState.externalPackageFileName = ""
                dialogState.externalPackagePassword = null
                onLoadingStateChange(false)
            }
        }
    }
}

/**
 * 创建并记住 PackageOperationHandler 实例
 */
@Composable
fun rememberPackageOperationHandler(
    scope: CoroutineScope,
    dialogState: DialogState,
    presetManager: PresetManager,
    onPendingImagesChange: (List<Uri>) -> Unit,
    onSkipDraftSave: (() -> Unit)?,
    onResumeDraftSave: (() -> Unit)?,
    showToast: (String, Int) -> Unit
): PackageOperationHandler {
    val context = LocalContext.current
    return remember(scope, dialogState, presetManager) {
        PackageOperationHandler(
            context = context,
            scope = scope,
            dialogState = dialogState,
            presetManager = presetManager,
            onPendingImagesChange = onPendingImagesChange,
            onSkipDraftSave = onSkipDraftSave,
            onResumeDraftSave = onResumeDraftSave,
            showToast = showToast
        )
    }
}
