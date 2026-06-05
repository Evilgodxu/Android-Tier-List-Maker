package com.tdds.jh.data.tierlist

import android.content.Context
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.tdds.jh.screens.tierlist.logic.utils.WebPConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * 图包类型
 */
sealed class PackageItem {
    abstract val name: String

    data class Imported(
        override val name: String,
        val file: File
    ) : PackageItem()
}

/**
 * 导入目标类型
 */
enum class ImportTarget {
    PENDING,    // 待分级区域
    BADGES      // 小图标区域
}

/**
 * 图包管理器
 * 负责管理图包的导入、提取、计数等操作
 * 直接解压到目标工作目录，使用哈希查重避免重复文件
 */
object PackageManager {

    /**
     * 生成唯一文件名，避免文件名冲突
     */
    private fun generateUniqueFileName(originalName: String, targetDir: File): String {
        val extension = originalName.substringAfterLast(".", "jpg")
        val baseName = originalName.substringBeforeLast(".")
        var candidateName = originalName
        var counter = 1
        
        while (File(targetDir, candidateName).exists()) {
            candidateName = "${baseName}_${counter}.$extension"
            counter++
        }
        
        return candidateName
    }

    /**
     * 从ZIP文件导入图片到目标目录
     * @param context 上下文
     * @param zipUri ZIP文件URI
     * @param targetDir 目标目录（工作目录）
     * @param password 密码（可选）
     * @return 导入的图片URI列表
     */
    suspend fun importImagesFromZip(
        context: Context,
        zipUri: Uri,
        targetDir: File,
        password: String?
    ): List<Uri> = withContext(Dispatchers.IO) {
        targetDir.mkdirs()
        val imageUris = mutableListOf<Uri>()
        val tempDir = context.cacheDir.resolve("zip_temp_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        // 构建目标目录中现有文件的哈希映射表，用于查重
        val existingFilesHashMap = mutableMapOf<String, File>()
        targetDir.listFiles()?.forEach { file ->
            if (file.isFile) {
                try {
                    val hash = ImageResourceManager.calculateQuickHash(file)
                    existingFilesHashMap[hash] = file
                } catch (e: Exception) {
                    // 忽略计算失败的文件
                }
            }
        }

        val tempZipFile = File(tempDir, "temp.zip")
        try {
            context.contentResolver.openInputStream(zipUri)?.use { input ->
                tempZipFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            val zipFile = ZipFile(tempZipFile)
            val isEncrypted = zipFile.isEncrypted

            if (isEncrypted && password == null) {
                throw ZipPasswordRequiredException("该ZIP文件已加密，请输入密码")
            }

            if (isEncrypted && password != null) {
                zipFile.setPassword(password.toCharArray())
            }

            val fileHeaders = zipFile.fileHeaders
            var reusedCount = 0
            var newCount = 0

            fileHeaders.forEach { fileHeader ->
                if (!fileHeader.isDirectory) {
                    val fileName = fileHeader.fileName.lowercase()
                    if (fileName.endsWith(".jpg") ||
                        fileName.endsWith(".jpeg") ||
                        fileName.endsWith(".png") ||
                        fileName.endsWith(".webp") ||
                        fileName.endsWith(".gif")) {

                        val tempFile = File(tempDir, fileHeader.fileName.substringAfterLast("/"))
                        zipFile.getInputStream(fileHeader).use { input ->
                            tempFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }

                        // 计算文件哈希，检查目标目录是否已存在
                        val fileHash = ImageResourceManager.calculateQuickHash(tempFile)
                        val existingFile = existingFilesHashMap[fileHash]

                        val destFile = if (existingFile != null) {
                            // 找到相同内容的文件，复用
                            reusedCount++
                            tempFile.delete()
                            existingFile
                        } else {
                            // 没有相同内容的文件，转换为WebP后保存
                            newCount++
                            val originalName = fileHeader.fileName.substringAfterLast("/")
                            val baseName = originalName.substringBeforeLast(".")
                            val webpName = "${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}_${baseName}.webp"
                            val destFile = File(targetDir, webpName)

                            // 转换为WebP格式
                            val conversionResult = WebPConverter.convertToWebP(tempFile, destFile)
                            if (!conversionResult.success) {
                                // WebP转换失败，回退到原格式
                                val fallbackName = generateUniqueFileName(originalName, targetDir)
                                val fallbackFile = File(targetDir, fallbackName)
                                tempFile.copyTo(fallbackFile, overwrite = false)
                                tempFile.delete()
                                existingFilesHashMap[fileHash] = fallbackFile
                                fallbackFile
                            } else {
                                tempFile.delete()
                                // 添加到哈希映射表，避免本次导入中重复
                                existingFilesHashMap[fileHash] = destFile
                                destFile
                            }
                        }

                        val imageUri = Uri.fromFile(destFile)
                        imageUris.add(imageUri)
                    }
                }
            }
        } catch (e: Exception) {
            throw e
        } finally {
            tempDir.deleteRecursively()
        }

        imageUris
    }

    /**
     * 从已导入的图包文件中提取图片到目标目录
     * @param context 上下文
     * @param packageFile 图包文件
     * @param targetDir 目标目录（工作目录）
     * @return 提取的图片URI列表
     */
    suspend fun extractImportedPackage(
        context: Context,
        packageFile: File,
        targetDir: File
    ): List<Uri> = withContext(Dispatchers.IO) {
        targetDir.mkdirs()
        val imageUris = mutableListOf<Uri>()
        val tempDir = context.cacheDir.resolve("imported_temp_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        // 构建目标目录中现有文件的哈希映射表，用于查重
        val existingFilesHashMap = mutableMapOf<String, File>()
        targetDir.listFiles()?.forEach { file ->
            if (file.isFile) {
                try {
                    val hash = ImageResourceManager.calculateQuickHash(file)
                    existingFilesHashMap[hash] = file
                } catch (e: Exception) {
                    // 忽略计算失败的文件
                }
            }
        }

        var reusedCount = 0
        var newCount = 0

        try {
            packageFile.inputStream().use { inputStream ->
                ZipArchiveInputStream(inputStream).use { zipInput ->
                    var entry = zipInput.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            val fileName = entry.name.lowercase()
                            if (fileName.endsWith(".jpg") ||
                                fileName.endsWith(".jpeg") ||
                                fileName.endsWith(".png") ||
                                fileName.endsWith(".webp") ||
                                fileName.endsWith(".gif")) {

                                val tempFile = File(tempDir, entry.name.substringAfterLast("/"))
                                tempFile.outputStream().use { output ->
                                    zipInput.copyTo(output)
                                }

                                // 计算文件哈希，检查目标目录是否已存在
                                val fileHash = ImageResourceManager.calculateQuickHash(tempFile)
                                val existingFile = existingFilesHashMap[fileHash]

                                val destFile = if (existingFile != null) {
                                    // 找到相同内容的文件，复用
                                    reusedCount++
                                    tempFile.delete()
                                    existingFile
                                } else {
                                    // 没有相同内容的文件，转换为WebP后保存
                                    newCount++
                                    val originalName = entry.name.substringAfterLast("/")
                                    val baseName = originalName.substringBeforeLast(".")
                                    val webpName = "${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}_${baseName}.webp"
                                    val destFile = File(targetDir, webpName)

                                    // 转换为WebP格式
                                    val conversionResult = WebPConverter.convertToWebP(tempFile, destFile)
                                    if (!conversionResult.success) {
                                        // WebP转换失败，回退到原格式
                                        val fallbackName = generateUniqueFileName(originalName, targetDir)
                                        val fallbackFile = File(targetDir, fallbackName)
                                        tempFile.copyTo(fallbackFile, overwrite = false)
                                        tempFile.delete()
                                        existingFilesHashMap[fileHash] = fallbackFile
                                        fallbackFile
                                    } else {
                                        tempFile.delete()
                                        // 添加到哈希映射表，避免本次导入中重复
                                        existingFilesHashMap[fileHash] = destFile
                                        destFile
                                    }
                                }

                                val imageUri = Uri.fromFile(destFile)
                                imageUris.add(imageUri)
                            }
                        }
                        entry = zipInput.nextEntry
                    }
                }
            }
        } catch (e: Exception) {
            throw e
        } finally {
            tempDir.deleteRecursively()
        }

        imageUris
    }

    /**
     * 计算已导入图包中的图片数量
     * @param packageFile 图包文件
     * @return 图片数量
     */
    suspend fun countImagesInImportedPackage(
        packageFile: File
    ): Int = withContext(Dispatchers.IO) {
        var count = 0
        try {
            packageFile.inputStream().use { inputStream ->
                ZipArchiveInputStream(inputStream).use { zipInput ->
                    var entry = zipInput.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            val fileName = entry.name.lowercase()
                            if (fileName.endsWith(".jpg") ||
                                fileName.endsWith(".jpeg") ||
                                fileName.endsWith(".png") ||
                                fileName.endsWith(".webp") ||
                                fileName.endsWith(".gif")) {
                                count++
                            }
                        }
                        entry = zipInput.nextEntry
                    }
                }
            }
        } catch (_: Exception) {
        }
        count
    }

    /**
     * 导出图包为WebP格式
     * 将图包中的所有图片转换为WebP格式并打包成ZIP
     * @param context 上下文
     * @param packageFile 源图包文件
     * @param outputUri 输出URI
     * @return 导出是否成功
     */
    suspend fun exportPackageAsWebP(
        context: Context,
        packageFile: File,
        outputUri: Uri
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val tempDir = context.cacheDir.resolve("export_webp_${System.currentTimeMillis()}")
            tempDir.mkdirs()

            var convertedCount = 0
            var failedCount = 0

            // 创建临时ZIP文件
            val tempZipFile = File(tempDir, "temp_export.zip")

            java.util.zip.ZipOutputStream(tempZipFile.outputStream()).use { zipOut ->
                packageFile.inputStream().use { inputStream ->
                    ZipArchiveInputStream(inputStream).use { zipInput ->
                        var entry = zipInput.nextEntry
                        while (entry != null) {
                            val entryName = entry.name

                            if (entry.isDirectory) {
                                // 保留目录结构
                                zipOut.putNextEntry(java.util.zip.ZipEntry(entryName))
                                zipOut.closeEntry()
                            } else {
                                val lowerName = entryName.lowercase()
                                val isImage = lowerName.endsWith(".jpg") ||
                                        lowerName.endsWith(".jpeg") ||
                                        lowerName.endsWith(".png") ||
                                        lowerName.endsWith(".webp") ||
                                        lowerName.endsWith(".gif")

                                if (isImage) {
                                    // 获取文件名（不含路径）
                                    val fileName = entryName.substringAfterLast("/")
                                    // 创建临时文件（放在 tempDir 根目录，不包含子目录）
                                    val tempImageFile = File(tempDir, "temp_${System.currentTimeMillis()}_$fileName")
                                    tempImageFile.outputStream().use { output ->
                                        zipInput.copyTo(output)
                                    }

                                    // 转换为WebP
                                    val baseName = fileName.substringBeforeLast(".")
                                    val webpName = "$baseName.webp"
                                    val webpFile = File(tempDir, webpName)

                                    val conversionResult = WebPConverter.convertToWebP(tempImageFile, webpFile)

                                    // 构建输出路径（保留原始目录结构）
                                    val outputEntryName = if (entryName.contains("/")) {
                                        val dirPath = entryName.substringBeforeLast("/")
                                        "$dirPath/$webpName"
                                    } else {
                                        webpName
                                    }

                                    if (conversionResult.success) {
                                        // 将WebP文件添加到ZIP（保留目录结构）
                                        zipOut.putNextEntry(java.util.zip.ZipEntry(outputEntryName))
                                        webpFile.inputStream().use { input ->
                                            input.copyTo(zipOut)
                                        }
                                        zipOut.closeEntry()
                                        // 只有真正进行了格式转换才计数，已经是WebP的不计入
                                        if (!conversionResult.isAlreadyWebP) {
                                            convertedCount++
                                        }
                                    } else {
                                        // 转换失败，使用原文件（保留目录结构）
                                        zipOut.putNextEntry(java.util.zip.ZipEntry(entryName))
                                        tempImageFile.inputStream().use { input ->
                                            input.copyTo(zipOut)
                                        }
                                        zipOut.closeEntry()
                                        failedCount++
                                    }

                                    // 清理临时文件
                                    tempImageFile.delete()
                                    webpFile.delete()
                                } else {
                                    // 非图片文件，直接复制
                                    zipOut.putNextEntry(java.util.zip.ZipEntry(entryName))
                                    zipInput.copyTo(zipOut)
                                    zipOut.closeEntry()
                                }
                            }
                            entry = zipInput.nextEntry
                        }
                    }
                }
            }

            // 将临时ZIP文件复制到输出位置
            // 使用 "rwt" 模式确保可以覆盖已存在的文件
            context.contentResolver.openOutputStream(outputUri, "rwt")?.use { output ->
                tempZipFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            } ?: throw IllegalStateException("无法打开输出流")

            // 清理临时目录
            tempDir.deleteRecursively()

            true
        } catch (e: Exception) {
            false
        }
    }
}
