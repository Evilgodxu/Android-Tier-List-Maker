package com.tdds.jh.screens.tierlist.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.tdds.jh.R
import com.tdds.jh.data.bitmap.TierImage
import com.tdds.jh.data.bitmap.TierItem
import com.tdds.jh.data.bitmap.generateTierListBitmap
import com.tdds.jh.ui.toast.showToastWithoutIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * 保存Bitmap到相册
 *
 * @param context 上下文
 * @param bitmap 要保存的位图
 * @param title 图片标题（用于生成文件名）
 */
suspend fun saveBitmapToGallery(
    context: Context,
    bitmap: Bitmap,
    title: String = ""
) {
    try {
        withContext(Dispatchers.IO) {
            // 生成文件名：标题_梯度表_时间戳.webp（确保唯一性）
            val tierListSuffix = context.getString(R.string.tier_list_suffix)
            val sanitizedTitle = if (title.isNotBlank()) {
                title.replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5_-]"), "_").take(50)
            } else {
                "tier_list"
            }
            val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
            val fileName = "${sanitizedTitle}_${tierListSuffix}_${timeStamp}.webp"

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/webp")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }

            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                val outputStream: OutputStream? = context.contentResolver.openOutputStream(it)
                outputStream?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 100, stream)
                }
                withContext(Dispatchers.Main) {
                    showToastWithoutIcon(context, context.getString(R.string.saved_to_gallery))
                }
            }
        }
    } catch (e: Exception) {
        showToastWithoutIcon(context, context.getString(R.string.save_failed, e.message))
    }
}


/**
 * 分享Bitmap图片
 *
 * @param context 上下文
 * @param bitmap 要分享的位图
 * @param title 图片标题（用于生成文件名）
 */
suspend fun shareBitmap(
    context: Context,
    bitmap: Bitmap,
    title: String = ""
) {
    try {
        withContext(Dispatchers.IO) {
            val cacheDir = context.cacheDir

            // 清理之前分享生成的所有图片
            cacheDir.listFiles { file ->
                file.name.endsWith("_${context.getString(R.string.tier_list_suffix)}.webp")
            }?.forEach { file ->
                file.delete()
            }

            // 生成文件名：标题_梯度表.webp
            val tierListSuffix = context.getString(R.string.tier_list_suffix)
            val sanitizedTitle = if (title.isNotBlank()) {
                title.replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5_-]"), "_").take(50)
            } else {
                "tier_list"
            }
            val fileName = "${sanitizedTitle}_${tierListSuffix}.webp"

            // 创建临时文件
            val file = File(cacheDir, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 100, out)
            }

            // 获取FileProvider Uri
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            // 创建分享Intent
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "image/webp"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_image))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // 启动分享对话框
            val chooser = Intent.createChooser(shareIntent, context.getString(R.string.share_image))
            withContext(Dispatchers.Main) {
                context.startActivity(chooser)
            }
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            showToastWithoutIcon(context, context.getString(R.string.share_failed, e.message))
        }
    }
}

/**
 * 保存梯度表图片
 *
 * @param context 上下文
 * @param tiers 层级列表
 * @param tierImages 层级图片列表
 * @param title 标题
 * @param authorName 作者名称
 * @param externalBadgeEnabled 是否启用外部小图标
 * @param disableCustomFont 是否禁用自定义字体
 * @param nameBelowImage 名称是否显示在图片下方
 */
suspend fun saveTierListImage(
    context: Context,
    tiers: List<TierItem>,
    tierImages: List<TierImage>,
    title: String = context.getString(R.string.tier_list_default_title),
    authorName: String = "",
    externalBadgeEnabled: Boolean = false,
    disableCustomFont: Boolean = false,
    nameBelowImage: Boolean = false
) {
    try {
        // 使用 generateTierListBitmap 生成图片（浅色主题）
        val bitmap = generateTierListBitmap(
            context = context,
            tiers = tiers,
            tierImages = tierImages,
            title = title,
            authorName = authorName,
            isDarkTheme = false,
            externalBadgeEnabled = externalBadgeEnabled,
            disableCustomFont = disableCustomFont,
            nameBelowImage = nameBelowImage
        )

        withContext(Dispatchers.IO) {
            // 保存到相册
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "梯度表_${System.currentTimeMillis()}.webp")
                put(MediaStore.Images.Media.MIME_TYPE, "image/webp")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }

            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                val outputStream: OutputStream? = context.contentResolver.openOutputStream(it)
                outputStream?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 85, stream)
                }
                withContext(Dispatchers.Main) {
                    showToastWithoutIcon(context, context.getString(R.string.saved_to_gallery))
                }
            }
        }
    } catch (e: Exception) {
        showToastWithoutIcon(context, context.getString(R.string.save_failed, e.message))
    }
}
