package com.tdds.jh.screens.tierlist.utils

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import com.tdds.jh.screens.tierlist.logic.utils.FileUtils

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
     * @param context 上下文
     * @return 权限状态
     */
    fun checkStoragePermission(context: Context): PermissionState {
        val permission = FileUtils.getReadStoragePermission()
        return if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            PermissionState.Granted
        } else {
            PermissionState.Denied
        }
    }

    /**
     * 检查存储权限是否已授予（简化版）
     * @param context 上下文
     * @return true 表示已授予
     */
    fun hasStoragePermission(context: Context): Boolean {
        return checkStoragePermission(context) is PermissionState.Granted
    }

    /**
     * 请求存储权限
     * @param launcher 权限请求启动器
     */
    fun requestStoragePermission(launcher: ManagedActivityResultLauncher<String, Boolean>) {
        val permission = FileUtils.getReadStoragePermission()
        launcher.launch(permission)
    }
}

/**
 * 根据权限状态执行相应操作
 * @param context 上下文
 * @param permissionLauncher 权限请求启动器
 * @param onGranted 权限已授予时执行的操作
 * @param onSkipDraftSave 临时禁用草稿保存的回调（可选）
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
