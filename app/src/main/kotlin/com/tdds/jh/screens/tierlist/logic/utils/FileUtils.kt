package com.tdds.jh.screens.tierlist.logic.utils

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns

/**
 * 文件工具类
 * 提供文件操作、URI处理等工具函数
 */
object FileUtils {

    /**
     * 获取读取存储权限的字符串
     * 根据Android版本返回不同的权限常量
     * @return 权限字符串
     */
    fun getReadStoragePermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    /**
     * 从URI获取文件名
     * @param context 上下文
     * @param uri 文件URI
     * @return 文件名,如果获取失败返回null
     */
    fun getFileNameFromUri(context: Context, uri: Uri): String? {
        var fileName: String? = null
        // 尝试从ContentResolver查询文件名
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    fileName = cursor.getString(nameIndex)
                }
            }
        }
        // 如果查询失败,尝试从URI路径解析
        // 移除可能存在的primary:等存储前缀
        if (fileName == null) {
            fileName = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':')
        }
        return fileName
    }
}
