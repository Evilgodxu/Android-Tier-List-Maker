package com.tdds.jh.screens.tierlist.logic.usecases

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.tdds.jh.data.tierlist.PresetManager
import com.tdds.jh.data.tierlist.PackageItem
import com.tdds.jh.data.tierlist.PackageManager as ResourcePackageManager
import com.tdds.jh.data.tierlist.ZipPasswordRequiredException
import com.tdds.jh.screens.tierlist.DialogState
import com.tdds.jh.ui.toast.showToastWithoutIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    fun handleExportPackage(uri: Uri?, packageItem: PackageItem.Imported?) {
        if (uri == null || packageItem == null) { dialogState.packageToExport = null; onResumeDraftSave?.invoke(); return }
        dialogState.isExportingPackage = true; onSkipDraftSave?.invoke()
        scope.launch {
            try {
                val success = withContext(Dispatchers.IO) { ResourcePackageManager.exportPackageAsWebP(context, packageItem.file, uri) }
                if (success) showToast("导出图包成功", android.widget.Toast.LENGTH_SHORT) else showToast("导出图包失败", android.widget.Toast.LENGTH_LONG)
            } catch (e: Exception) { showToast("导出图包失败: ${e.message}", android.widget.Toast.LENGTH_LONG) }
            finally { dialogState.isExportingPackage = false; dialogState.packageToExport = null; onResumeDraftSave?.invoke() }
        }
    }

    fun startExportPackage(packageItem: PackageItem.Imported, onLaunchExport: () -> Unit) { dialogState.packageToExport = packageItem; onSkipDraftSave?.invoke(); onLaunchExport() }

    fun handleImportZipToPending(zipUri: Uri?, password: String?, currentPendingImages: List<Uri>) {
        if (zipUri == null) { dialogState.isImportingPackage = false; onResumeDraftSave?.invoke(); return }
        dialogState.isImportingPackage = true; onSkipDraftSave?.invoke()
        scope.launch {
            try {
                val imagesDir = java.io.File(presetManager.getWorkImagesDirectory(), "images")
                val importedUris = withContext(Dispatchers.IO) { ResourcePackageManager.importImagesFromZip(context, zipUri, imagesDir, password) }
                onPendingImagesChange(currentPendingImages + importedUris)
                showToast("已导入 ${importedUris.size} 张图片", android.widget.Toast.LENGTH_SHORT)
            } catch (e: ZipPasswordRequiredException) { showToast("ZIP文件需要密码", android.widget.Toast.LENGTH_LONG); return@launch }
            catch (e: Exception) { showToast("导入失败: ${e.message}", android.widget.Toast.LENGTH_LONG) }
            finally { dialogState.isImportingPackage = false; onResumeDraftSave?.invoke() }
        }
    }

    fun handleImportZipToBadges(zipUri: Uri?, password: String?, onBadgeDialogRefresh: () -> Unit) {
        if (zipUri == null) { dialogState.isImportingPackage = false; onResumeDraftSave?.invoke(); return }
        dialogState.isImportingPackage = true; onSkipDraftSave?.invoke()
        scope.launch {
            try {
                val workBadgesDir = java.io.File(presetManager.getWorkImagesDirectory(), PresetManager.BADGES_FOLDER_NAME)
                val importedUris = withContext(Dispatchers.IO) { ResourcePackageManager.importImagesFromZip(context, zipUri, workBadgesDir, password) }
                onBadgeDialogRefresh(); showToast("已导入 ${importedUris.size} 张小图标", android.widget.Toast.LENGTH_SHORT)
            } catch (e: ZipPasswordRequiredException) { showToast("ZIP文件需要密码", android.widget.Toast.LENGTH_LONG); return@launch }
            catch (e: Exception) { showToast("导入失败: ${e.message}", android.widget.Toast.LENGTH_LONG) }
            finally { dialogState.isImportingPackage = false; onResumeDraftSave?.invoke() }
        }
    }

    fun getPackageImageCount(packageItem: PackageItem.Imported, onCountCalculated: (Int) -> Unit) { scope.launch { val count = withContext(Dispatchers.IO) { ResourcePackageManager.countImagesInImportedPackage(packageItem.file) }; onCountCalculated(count) } }

    fun handleExternalPackageImport(uri: Uri, fileName: String?, onLoadingStateChange: (Boolean) -> Unit) {
        dialogState.isImportingPackage = true; onSkipDraftSave?.invoke()
        scope.launch {
            try {
                val tempDir = context.cacheDir.resolve("zip_check_${System.currentTimeMillis()}"); tempDir.mkdirs()
                val tempZipFile = java.io.File(tempDir, "temp.zip")
                context.contentResolver.openInputStream(uri)?.use { input -> tempZipFile.outputStream().use { output -> input.copyTo(output) } }
                val zipFile = net.lingala.zip4j.ZipFile(tempZipFile); val isEncrypted = zipFile.isEncrypted; tempDir.deleteRecursively()
                if (isEncrypted) { dialogState.externalPackageUri = uri; dialogState.externalPackageFileName = fileName ?: "imported_${System.currentTimeMillis()}.zip"; dialogState.externalPackagePassword = null; dialogState.externalPackagePasswordError = false; dialogState.showExternalPackagePasswordDialog = true; dialogState.isImportingPackage = false; onResumeDraftSave?.invoke() }
                else { importExternalPackageInternal(uri, fileName, onLoadingStateChange) }
            } catch (e: Exception) { dialogState.isImportingPackage = false; onResumeDraftSave?.invoke(); showToast("图包导入失败: ${e.message}", android.widget.Toast.LENGTH_SHORT); onLoadingStateChange(false) }
        }
    }

    fun continueExternalPackageImportWithPassword(password: String, onLoadingStateChange: (Boolean) -> Unit) {
        val uri = dialogState.externalPackageUri; val fileName = dialogState.externalPackageFileName
        if (uri == null) { dialogState.showExternalPackagePasswordDialog = false; dialogState.isImportingPackage = false; onResumeDraftSave?.invoke(); onLoadingStateChange(false); return }
        dialogState.externalPackagePassword = password; dialogState.showExternalPackagePasswordDialog = false
        importExternalPackageInternal(uri, fileName, onLoadingStateChange, password)
    }

    private fun importExternalPackageInternal(uri: Uri, fileName: String?, onLoadingStateChange: (Boolean) -> Unit, password: String? = null) {
        scope.launch {
            try {
                val actualFileName = fileName ?: "imported_${System.currentTimeMillis()}.zip"
                val savedPackageFile = withContext(Dispatchers.IO) { presetManager.saveImportedPackage(uri, actualFileName, password) }
                if (savedPackageFile != null) showToast("图包导入成功: ${savedPackageFile.nameWithoutExtension}", android.widget.Toast.LENGTH_SHORT) else showToast("图包导入失败", android.widget.Toast.LENGTH_SHORT)
            } catch (e: ZipPasswordRequiredException) { dialogState.externalPackagePasswordError = true; dialogState.showExternalPackagePasswordDialog = true; return@launch }
            catch (e: Exception) { showToast("图包导入失败: ${e.message}", android.widget.Toast.LENGTH_SHORT) }
            finally { dialogState.externalPackageUri = null; dialogState.externalPackageFileName = ""; dialogState.externalPackagePassword = null; dialogState.isImportingPackage = false; onResumeDraftSave?.invoke(); onLoadingStateChange(false) }
        }
    }
}

@Composable
fun rememberPackageOperationHandler(scope: CoroutineScope, dialogState: DialogState, presetManager: PresetManager, onPendingImagesChange: (List<Uri>) -> Unit, onSkipDraftSave: (() -> Unit)?, onResumeDraftSave: (() -> Unit)?, showToast: (String, Int) -> Unit): PackageOperationHandler {
    val context = LocalContext.current
    return remember(scope, dialogState, presetManager) { PackageOperationHandler(context = context, scope = scope, dialogState = dialogState, presetManager = presetManager, onPendingImagesChange = onPendingImagesChange, onSkipDraftSave = onSkipDraftSave, onResumeDraftSave = onResumeDraftSave, showToast = showToast) }
}
