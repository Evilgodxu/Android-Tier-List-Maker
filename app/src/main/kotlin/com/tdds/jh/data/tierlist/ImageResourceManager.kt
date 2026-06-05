package com.tdds.jh.data.tierlist

import android.content.Context
import android.net.Uri
import com.tdds.jh.screens.tierlist.logic.utils.WebPConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.UUID

/**
 * 图片资源管理器
 * 负责图片的URI映射复用和快速哈希去重
 * 将两种不同场景的逻辑解耦，避免维护错误
 */
object ImageResourceManager {

    // ==================== URI映射复用（用于导入图片到编辑页面） ====================

    private const val URI_MAPPING_PREFS = "uri_file_mapping"
    private const val MAX_CACHE_ENTRIES = 1500

    /**
     * 将URI指向的文件复制到工作目录，支持URI映射复用
     * 用于：导入图片到编辑页面时，避免重复复制相同文件
     *
     * @param context 上下文
     * @param uri 文件URI
     * @param targetDir 目标目录
     * @return Pair<文件名, 是否复用>, 如果失败则返回 Pair(null, false)
     */
    fun copyUriToWorkDirWithStatus(
        context: Context,
        uri: Uri,
        targetDir: File
    ): Pair<String?, Boolean> {
        return try {
            val originalFileName = extractFileNameFromUriInternal(uri)
            if (originalFileName == null) {
                return Pair(null, false)
            }

            // 检查工作目录中是否已存在从该URI复制的文件
            val existingFile = findExistingFileForUri(context, uri, targetDir)
            if (existingFile != null) {
                return Pair(existingFile.name, true)
            }

            // 生成唯一文件名避免冲突
            val uniqueFileName = "${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}_$originalFileName"
            val targetFile = File(targetDir, uniqueFileName)

            // 复制文件
            val (copiedFileName, isReused) = when (uri.scheme) {
                "file" -> {
                    val sourceFile = File(uri.path!!)
                    if (!sourceFile.exists()) {
                        Pair(null, false)
                    } else if (sourceFile.parentFile?.absolutePath == targetDir.absolutePath) {
                        // 如果源文件已经在目标目录中，直接返回原文件名（复用）
                        Pair(sourceFile.name, true)
                    } else {
                        // 从其他位置复制到工作目录
                        sourceFile.copyTo(targetFile)
                        saveUriToFileNameMapping(context, uri, targetDir.name, uniqueFileName)
                        Pair(uniqueFileName, false)
                    }
                }
                else -> {
                    // 对于content://等URI，使用ContentResolver复制
                    try {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            java.io.FileOutputStream(targetFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        saveUriToFileNameMapping(context, uri, targetDir.name, uniqueFileName)
                        Pair(uniqueFileName, false)
                    } catch (_: Exception) {
                        Pair(null, false)
                    }
                }
            }

            Pair(copiedFileName, isReused)
        } catch (_: Exception) {
            Pair(null, false)
        }
    }

    /**
     * 将URI指向的文件复制到工作目录（简化版）
     * @return 复制后的文件名，如果失败则返回null
     */
    fun copyUriToWorkDir(context: Context, uri: Uri, targetDir: File): String? {
        return copyUriToWorkDirWithStatus(context, uri, targetDir).first
    }

    // ==================== 带WebP转换和哈希查重的图片导入 ====================

    /**
     * 图片导入结果
     */
    data class ImageImportResult(
        val fileName: String?,           // 导入后的文件名
        val isReused: Boolean,           // 是否复用已有文件
        val isConvertedToWebP: Boolean   // 是否转换为WebP格式
    )

    /**
     * 将URI图片导入到工作目录，支持WebP转换和哈希查重
     * 用于：从图片选择器导入图片时，自动转换为WebP并进行内容查重
     *
     * @param context 上下文
     * @param uri 图片URI
     * @param targetDir 目标目录
     * @param existingHashes 已有文件的哈希映射表（用于查重）
     * @param convertToWebP 是否转换为WebP格式
     * @return 导入结果
     */
    suspend fun importImageWithWebPAndHash(
        context: Context,
        uri: Uri,
        targetDir: File,
        existingHashes: MutableMap<String, File> = mutableMapOf(),
        convertToWebP: Boolean = true
    ): ImageImportResult = withContext(Dispatchers.IO) {
        try {
            // 1. 首先检查URI映射（避免重复导入同一URI）
            val existingFile = findExistingFileForUri(context, uri, targetDir)
            if (existingFile != null) {
                return@withContext ImageImportResult(existingFile.name, true, false)
            }

            // 2. 创建临时文件保存原始图片
            val tempFile = File(context.cacheDir, "import_temp_${System.currentTimeMillis()}.tmp")

            // 复制URI内容到临时文件
            when (uri.scheme) {
                "file" -> {
                    val sourceFile = File(uri.path!!)
                    if (!sourceFile.exists()) {
                        return@withContext ImageImportResult(null, false, false)
                    }
                    sourceFile.copyTo(tempFile)
                }
                else -> {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        java.io.FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    } ?: run {
                        return@withContext ImageImportResult(null, false, false)
                    }
                }
            }

            // 3. 计算文件哈希进行内容查重
            val fileHash = calculateQuickHash(tempFile)
            val duplicateFile = existingHashes[fileHash]
            if (duplicateFile != null) {
                // 找到重复文件，复用
                tempFile.delete()
                return@withContext ImageImportResult(duplicateFile.name, true, false)
            }

            // 4. 生成唯一文件名
            val originalName = extractFileNameFromUriInternal(uri) ?: "image.webp"
            val baseName = originalName.substringBeforeLast(".")
            val extension = if (convertToWebP) "webp" else originalName.substringAfterLast(".", "jpg")
            var uniqueName = "${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}_${baseName}.${extension}"
            var counter = 1
            while (File(targetDir, uniqueName).exists()) {
                uniqueName = "${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}_${baseName}_${counter}.${extension}"
                counter++
            }

            val targetFile = File(targetDir, uniqueName)

            // 5. 转换为WebP（如果需要）或直接复制
            val conversionResult = if (convertToWebP) {
                WebPConverter.convertToWebP(tempFile, targetFile)
            } else {
                tempFile.copyTo(targetFile)
                WebPConverter.ConversionResult(success = true, isAlreadyWebP = false)
            }

            if (!conversionResult.success) {
                // WebP转换失败，回退到原格式
                tempFile.copyTo(targetFile)
            }

            // 删除临时文件
            tempFile.delete()

            // 6. 保存URI映射和哈希
            saveUriToFileNameMapping(context, uri, targetDir.name, uniqueName)
            existingHashes[fileHash] = targetFile

            val isConverted = convertToWebP && conversionResult.success && !conversionResult.isAlreadyWebP
            ImageImportResult(uniqueName, false, isConverted)

        } catch (_: Exception) {
            ImageImportResult(null, false, false)
        }
    }

    /**
     * 查找工作目录中是否已存在从指定URI复制的文件
     * 通过URI映射来查找
     */
    fun findExistingFileForUri(context: Context, uri: Uri, targetDir: File): File? {
        val prefs = getUriMappingPrefs(context)
        val uriKey = generateUriKey(uri)
        val mappedFileName = prefs.getString("${targetDir.name}_${uriKey}", null)

        if (mappedFileName != null) {
            val existingFile = File(targetDir, mappedFileName)
            if (existingFile.exists()) {
                return existingFile
            }
        }

        return null
    }

    /**
     * 保存URI到文件名的映射
     * 使用LRU策略：当缓存超过限制时，删除最旧的一半条目
     */
    private fun saveUriToFileNameMapping(context: Context, uri: Uri, folderName: String, fileName: String) {
        val prefs = getUriMappingPrefs(context)
        val uriKey = generateUriKey(uri)
        val editor = prefs.edit().putString("${folderName}_${uriKey}", fileName)

        // 限制缓存条目数量，防止无限增长
        val allEntries = prefs.all
        if (allEntries.size > MAX_CACHE_ENTRIES) {
            val sortedEntries = allEntries.entries.sortedBy { it.value.hashCode() }
            val keysToRemove = sortedEntries.take(sortedEntries.size / 2).map { it.key }
            keysToRemove.forEach { editor.remove(it) }
        }

        editor.apply()
    }

    /**
     * 生成URI的唯一标识键
     */
    private fun generateUriKey(uri: Uri): String {
        val uriString = uri.toString()
        return "${uriString.hashCode()}_${uriString.length}"
    }

    /**
     * 获取URI映射的SharedPreferences
     */
    private fun getUriMappingPrefs(context: Context): android.content.SharedPreferences {
        return context.getSharedPreferences(URI_MAPPING_PREFS, Context.MODE_PRIVATE)
    }

    /**
     * 清空URI映射缓存
     * 在应用预设前调用，避免旧映射干扰
     */
    fun clearUriMappingCache(context: Context) {
        try {
            getUriMappingPrefs(context).edit().clear().apply()
        } catch (_: Exception) {
        }
    }

    /**
     * 从URI映射中获取文件名
     * 用于保存草稿时获取content:// URI对应的文件名
     * @param uri 文件URI
     * @return 文件名，如果没有映射则返回null
     */
    fun getFileNameFromUri(uri: Uri): String? {
        return when (uri.scheme) {
            "file" -> {
                // file:// URI，直接提取文件名
                File(uri.path!!).name
            }
            else -> {
                // content:// URI，从lastPathSegment提取
                // 移除可能存在的primary:等存储前缀
                uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':')
            }
        }
    }

    /**
     * 从URI中提取文件名（内部使用）
     */
    private fun extractFileNameFromUriInternal(uri: Uri): String? {
        return when {
            uri.scheme == "file" -> {
                File(uri.path!!).name
            }
            else -> {
                // 移除可能存在的primary:等存储前缀
                uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':')
            }
        }
    }

    // ==================== 快速哈希去重（用于保存/导出预设） ====================

    private const val HASH_SAMPLE_SIZE = 4096 // 4KB

    /**
     * 计算文件的快速哈希（xxHash64）
     * 用于：保存/导出预设时检测重复图片
     * 读取文件头部和尾部各4KB进行哈希，平衡性能和准确性
     *
     * @param file 文件
     * @return 哈希字符串
     */
    fun calculateQuickHash(file: File): String {
        return try {
            val fileSize = file.length()
            FileInputStream(file).use { input ->
                calculateQuickHashFromStream(input, fileSize)
            }
        } catch (_: Exception) {
            "${file.lastModified()}_${file.length()}"
        }
    }

    /**
     * 从输入流计算快速哈希（xxHash64）
     * 用于：导入图包时直接对流计算哈希，避免创建临时文件
     * 读取文件头部和尾部各4KB进行哈希，平衡性能和准确性
     *
     * @param input 输入流
     * @param fileSize 文件大小（如果已知）
     * @return 哈希字符串
     */
    fun calculateQuickHashFromStream(input: InputStream, fileSize: Long = -1): String {
        return try {
            // 如果文件大小未知，需要读取全部内容到内存
            if (fileSize < 0) {
                // 读取全部内容到字节数组
                val bytes = input.readBytes()
                val size = bytes.size.toLong()
                // 使用字节数组计算哈希
                val headBuffer = if (bytes.size >= HASH_SAMPLE_SIZE) {
                    bytes.copyOf(HASH_SAMPLE_SIZE)
                } else {
                    bytes
                }
                val headHash = xxHash64(headBuffer, headBuffer.size, 0)

                val finalHash = if (size > HASH_SAMPLE_SIZE * 2) {
                    val tailBuffer = bytes.copyOfRange(bytes.size - HASH_SAMPLE_SIZE, bytes.size)
                    xxHash64(tailBuffer, tailBuffer.size, headHash)
                } else {
                    headHash
                }
                "${finalHash}_${size}"
            } else {
                // 文件大小已知，可以直接读取头部和尾部
                // 读取前4KB
                val headBuffer = ByteArray(HASH_SAMPLE_SIZE)
                val headRead = input.read(headBuffer)
                val headHash = if (headRead > 0) {
                    xxHash64(headBuffer, headRead, 0)
                } else {
                    0L
                }

                // 如果文件大于8KB，跳过中间部分，读取后4KB
                val finalHash = if (fileSize > HASH_SAMPLE_SIZE * 2 && headRead > 0) {
                    val skipBytes = fileSize - HASH_SAMPLE_SIZE * 2
                    val actualSkip = input.skip(skipBytes)
                    if (actualSkip == skipBytes) {
                        val tailBuffer = ByteArray(HASH_SAMPLE_SIZE)
                        val tailRead = input.read(tailBuffer)
                        if (tailRead > 0) {
                            // 使用头部哈希作为种子计算尾部哈希
                            xxHash64(tailBuffer, tailRead, headHash)
                        } else {
                            headHash
                        }
                    } else {
                        headHash
                    }
                } else {
                    headHash
                }

                "${finalHash}_${fileSize}"
            }
        } catch (_: Exception) {
            "${System.currentTimeMillis()}_${fileSize}"
        }
    }

    /**
     * xxHash64实现
     */
    private fun xxHash64(data: ByteArray, length: Int, seed: Long): Long {
        val prime1 = -7046029288634856825L // 11400714785074694791
        val prime2 = -4417276706812531889L  // 14029467366897019327
        val prime3 = 1609587929392839161L
        val prime4 = -8796714834491720963L  // 9650029242287828579
        val prime5 = 2870177450012600261L

        var hash: Long
        var remaining = length
        var offset = 0

        if (length >= 32) {
            var v1 = seed + prime1 + prime2
            var v2 = seed + prime2
            var v3 = seed + 0
            var v4 = seed - prime1

            do {
                v1 += getLong(data, offset) * prime2
                v1 = rotateLeft(v1, 31)
                v1 *= prime1
                offset += 8

                v2 += getLong(data, offset) * prime2
                v2 = rotateLeft(v2, 31)
                v2 *= prime1
                offset += 8

                v3 += getLong(data, offset) * prime2
                v3 = rotateLeft(v3, 31)
                v3 *= prime1
                offset += 8

                v4 += getLong(data, offset) * prime2
                v4 = rotateLeft(v4, 31)
                v4 *= prime1
                offset += 8

                remaining -= 32
            } while (remaining >= 32)

            hash = rotateLeft(v1, 1) + rotateLeft(v2, 7) + rotateLeft(v3, 12) + rotateLeft(v4, 18)

            v1 *= prime2
            v1 = rotateLeft(v1, 31)
            v1 *= prime1
            hash = hash xor v1
            hash = hash * prime1 + prime4

            v2 *= prime2
            v2 = rotateLeft(v2, 31)
            v2 *= prime1
            hash = hash xor v2
            hash = hash * prime1 + prime4

            v3 *= prime2
            v3 = rotateLeft(v3, 31)
            v3 *= prime1
            hash = hash xor v3
            hash = hash * prime1 + prime4

            v4 *= prime2
            v4 = rotateLeft(v4, 31)
            v4 *= prime1
            hash = hash xor v4
            hash = hash * prime1 + prime4
        } else {
            hash = seed + prime5
        }

        hash += length.toLong()

        // 处理剩余字节（8字节为单位）
        while (remaining >= 8) {
            val k1 = getLong(data, offset)
            hash = hash xor (k1 * prime2)
            hash = rotateLeft(hash, 27)
            hash = hash * prime1 + prime4
            offset += 8
            remaining -= 8
        }

        // 处理剩余字节（4字节为单位）
        if (remaining >= 4) {
            val k1 = getInt(data, offset).toLong() and 0xFFFFFFFFL
            hash = hash xor (k1 * prime1)
            hash = rotateLeft(hash, 23)
            hash = hash * prime2 + prime3
            offset += 4
            remaining -= 4
        }

        // 处理剩余字节（1字节为单位）
        while (remaining > 0) {
            val k1 = data[offset].toLong() and 0xFFL
            hash = hash xor (k1 * prime5)
            hash = rotateLeft(hash, 11)
            hash *= prime1
            offset++
            remaining--
        }

        // 最终化
        hash = hash xor (hash ushr 33)
        hash *= prime2
        hash = hash xor (hash ushr 29)
        hash *= prime3
        hash = hash xor (hash ushr 32)

        return hash
    }

    private fun getLong(data: ByteArray, offset: Int): Long {
        return (data[offset].toLong() and 0xFFL) or
                ((data[offset + 1].toLong() and 0xFFL) shl 8) or
                ((data[offset + 2].toLong() and 0xFFL) shl 16) or
                ((data[offset + 3].toLong() and 0xFFL) shl 24) or
                ((data[offset + 4].toLong() and 0xFFL) shl 32) or
                ((data[offset + 5].toLong() and 0xFFL) shl 40) or
                ((data[offset + 6].toLong() and 0xFFL) shl 48) or
                ((data[offset + 7].toLong() and 0xFFL) shl 56)
    }

    private fun getInt(data: ByteArray, offset: Int): Int {
        return (data[offset].toInt() and 0xFF) or
                ((data[offset + 1].toInt() and 0xFF) shl 8) or
                ((data[offset + 2].toInt() and 0xFF) shl 16) or
                ((data[offset + 3].toInt() and 0xFF) shl 24)
    }

    private fun rotateLeft(value: Long, bits: Int): Long {
        return (value shl bits) or (value ushr (64 - bits))
    }
}
