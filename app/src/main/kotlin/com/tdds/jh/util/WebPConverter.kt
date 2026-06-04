package com.tdds.jh.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * WebP 图片转换工具
 * 将各种格式的图片转换为 WebP 格式以节省存储空间
 */
object WebPConverter {

    // WebP 转换质量 (0-100)，建议 80-90 平衡质量和体积
    private const val WEBP_QUALITY = 85

    /**
     * 转换结果数据类
     */
    data class ConversionResult(
        val success: Boolean,
        val isAlreadyWebP: Boolean = false,
        val originalSize: Long = 0,
        val outputSize: Long = 0,
        val error: Exception? = null
    )

    /**
     * 将图片文件转换为 WebP 格式
     * @param sourceFile 源图片文件
     * @param outputFile 输出 WebP 文件
     * @param quality 压缩质量 (0-100)
     * @return 转换结果
     */
    fun convertToWebP(
        sourceFile: File,
        outputFile: File,
        quality: Int = WEBP_QUALITY
    ): ConversionResult {
        return try {
            // 如果源文件已经是 WebP 格式，直接复制
            if (sourceFile.extension.lowercase() == "webp") {
                sourceFile.copyTo(outputFile, overwrite = true)
                return ConversionResult(
                    success = true,
                    isAlreadyWebP = true,
                    originalSize = sourceFile.length(),
                    outputSize = outputFile.length()
                )
            }

            val bitmap = BitmapFactory.decodeFile(sourceFile.absolutePath)
            if (bitmap == null) {
                return ConversionResult(success = false)
            }

            FileOutputStream(outputFile).use { output ->
                bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, quality, output)
            }

            // 回收 Bitmap
            bitmap.recycle()

            ConversionResult(
                success = true,
                isAlreadyWebP = false,
                originalSize = sourceFile.length(),
                outputSize = outputFile.length()
            )
        } catch (e: Exception) {
            ConversionResult(success = false, error = e)
        }
    }

    /**
     * 从 URI 转换图片为 WebP 并保存
     * @param context 上下文
     * @param uri 图片 URI
     * @param outputFile 输出 WebP 文件
     * @param quality 压缩质量 (0-100)
     * @return 转换结果
     */
    suspend fun convertUriToWebP(
        context: Context,
        uri: Uri,
        outputFile: File,
        quality: Int = WEBP_QUALITY
    ): ConversionResult = withContext(Dispatchers.IO) {
        try {
            // 如果是 WebP URI，直接复制
            val mimeType = context.contentResolver.getType(uri)
            if (mimeType == "image/webp") {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(outputFile).use { output ->
                        input.copyTo(output)
                    }
                }
                return@withContext ConversionResult(
                    success = true,
                    isAlreadyWebP = true,
                    outputSize = outputFile.length()
                )
            }

            val bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            }

            if (bitmap == null) {
                return@withContext ConversionResult(success = false)
            }

            FileOutputStream(outputFile).use { output ->
                bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, quality, output)
            }

            bitmap.recycle()

            ConversionResult(
                success = true,
                isAlreadyWebP = false,
                outputSize = outputFile.length()
            )
        } catch (e: Exception) {
            ConversionResult(success = false, error = e)
        }
    }

    /**
     * 获取 WebP 格式的文件名
     * @param originalName 原文件名
     * @return WebP 文件名
     */
    fun getWebPFileName(originalName: String): String {
        val baseName = originalName.substringBeforeLast(".")
        return "${baseName}.webp"
    }
}
