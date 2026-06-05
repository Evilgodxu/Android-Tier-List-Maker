package com.tdds.jh.screens.tierlist.logic.utils

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat

/**
 * 权限状态
 */
sealed class PermissionState {
    data object Granted : PermissionState()
    data object Denied : PermissionState()
}

/**
 * 权限检查工具类
 * 统一处理存储权限的检查和请求
 */
object PermissionUtils {

    /**
     * 检查存储权限是否已授予
     */
    fun checkStoragePermission(context: Context): PermissionState {
        val permission = FileUtils.getReadStoragePermission()
        return if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            PermissionState.Granted
        } else {
            PermissionState.Denied
        }
    }

    fun hasStoragePermission(context: Context): Boolean {
        return checkStoragePermission(context) is PermissionState.Granted
    }

    fun requestStoragePermission(launcher: ManagedActivityResultLauncher<String, Boolean>) {
        val permission = FileUtils.getReadStoragePermission()
        launcher.launch(permission)
    }
}

/**
 * 根据权限状态执行相应操作
 */
inline fun withStoragePermission(
    context: Context,
    permissionLauncher: ManagedActivityResultLauncher<String, Boolean>,
    noinline onSkipDraftSave: (() -> Unit)? = null,
    crossinline onGranted: () -> Unit
) {
    when (PermissionUtils.checkStoragePermission(context)) {
        is PermissionState.Granted -> {
            onSkipDraftSave?.invoke()
            onGranted()
        }
        is PermissionState.Denied -> {
            PermissionUtils.requestStoragePermission(permissionLauncher)
        }
    }
}
