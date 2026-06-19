package com.tdds.jh.data.tierlist

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.tdds.jh.model.tierlist.TierImage
import com.tdds.jh.model.tierlist.TierItem
import com.tdds.jh.model.tierlist.video.ArrangementGranularity
import com.tdds.jh.model.tierlist.video.AudioIntervalSource
import com.tdds.jh.model.tierlist.video.AudioOverlayMode
import com.tdds.jh.model.tierlist.video.NameDisplayMode
import com.tdds.jh.model.tierlist.video.VideoActionType
import com.tdds.jh.model.tierlist.video.VideoGenerationConfig
import com.tdds.jh.screens.tierlist.logic.utils.WebPConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.concurrent.Volatile
import kotlin.math.min

/**
 * 预设数据类
 */
data class PresetData(
    val title: String,
    val author: String,
    val tiers: List<TierItemData>,
    val tierImages: List<TierImageData>,
    val pendingImages: List<String>, // 图片文件名列表
    val cropPositionX: Float = 0.5f,
    val cropPositionY: Float = 0.5f,
    val customCropWidth: Int = 0,
    val customCropHeight: Int = 0,
    val useCustomCropSize: Boolean = false,
    val cropRatio: Float = 1f, // 裁剪比例: 1f = 1:1, 0.75f = 3:4, 1.33f = 4:3
    val videoConfig: VideoGenerationConfig = VideoGenerationConfig()
)

/**
 * 层级数据类（用于序列化）
 */
data class TierItemData(
    val label: String,
    val color: String // 十六进制颜色字符串
)

/**
 * 层级图片数据类（用于序列化）
 */
data class TierImageData(
    val id: String,
    val tierLabel: String,
    val imageFileName: String,
    val name: String = "",
    val badgeFileName1: String? = null,
    val badgeFileName2: String? = null,
    val badgeFileName3: String? = null,
    val audioFileName: String? = null,
    // 裁剪状态字段
    val cropPositionX: Float = 0.5f,
    val cropPositionY: Float = 0.5f,
    val cropScale: Float = 1.0f,
    val isCropped: Boolean = false,
    val cropRatio: Float = 0f, // 裁剪比例: 0f = 无裁剪, 1f = 1:1, 0.75f = 3:4, 1.33f = 4:3
    val useCustomCrop: Boolean = false, // 是否使用自定义裁剪
    val customCropWidth: Int = 0,
    val customCropHeight: Int = 0
)

/**
 * 预设信息类
 */
data class PresetInfo(
    val name: String,
    val fileName: String,
    val file: File,
    val createTime: Long
)

/**
 * 预设管理器
 * 负责预设的存储、导入、导出和管理
 */
class PresetManager(private val context: Context) {

    companion object {
        const val PRESET_EXTENSION = ".tdds"
        const val PRESETS_FOLDER_NAME = "Presets"
        const val WORK_FOLDER_NAME = "WorkImages"
        const val TEMP_FOLDER_PREFIX = "temp_preset_"
        const val CONFIG_FILE_NAME = "preset_config.json"
        const val IMAGES_FOLDER_NAME = "images"
        const val BADGES_FOLDER_NAME = "badges"
        const val AUDIO_FOLDER_NAME = "audio"
        const val DRAFT_PRESET_NAME = "__draft__"
        const val DRAFT_FOLDER_NAME = "Draft"
        const val PACKAGES_FOLDER_NAME = "Packages"
    }

    // 预设文件夹
    private val presetsDir: File by lazy {
        File(context.filesDir, PRESETS_FOLDER_NAME).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    // 工作图片文件夹（持久化存储当前编辑的图片）
    private val workImagesDir: File by lazy {
        File(context.filesDir, WORK_FOLDER_NAME).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    /**
     * 当前临时文件夹
     * 使用@Volatile确保线程可见性
     */
    @Volatile
    private var currentTempDir: File? = null

    /**
     * 获取当前临时文件夹
     * 如果不存在则创建新的临时文件夹
     * @return 当前临时文件夹File对象
     */
    fun getCurrentTempDir(): File {
        return currentTempDir ?: createTempDir().also {
            currentTempDir = it
        }
    }

    /**
     * 创建新的临时文件夹
     */
    private fun createTempDir(): File {
        // 先清理所有已有临时文件夹，确保只有一个临时文件夹存在
        cleanupTempDirs()

        val tempDir = File(context.cacheDir, "${TEMP_FOLDER_PREFIX}${System.currentTimeMillis()}")
        tempDir.mkdirs()

        // 创建子文件夹
        File(tempDir, IMAGES_FOLDER_NAME).mkdirs()
        File(tempDir, BADGES_FOLDER_NAME).mkdirs()

        return tempDir
    }

    /**
     * 清理所有临时文件夹
     */
    fun cleanupTempDirs() {
        var deletedCount = 0
        context.cacheDir.listFiles()?.forEach { file ->
            if (file.isDirectory && file.name.startsWith(TEMP_FOLDER_PREFIX)) {
                file.deleteRecursively()
                deletedCount++
            }
        }
        currentTempDir = null
    }

    /**
     * 清理工作图片文件夹
     * 包括images、badges、audio子文件夹中的所有文件
     */
    fun cleanupWorkImages() {
        var deletedFiles = 0
        var deletedFolders = 0

        // 清理images子文件夹
        val imagesDir = File(workImagesDir, IMAGES_FOLDER_NAME)
        imagesDir.listFiles()?.forEach { file ->
            file.deleteRecursively()
            deletedFiles++
        }

        // 清理badges子文件夹
        val badgesDir = File(workImagesDir, BADGES_FOLDER_NAME)
        badgesDir.listFiles()?.forEach { file ->
            file.deleteRecursively()
            deletedFiles++
        }

        // 清理audio子文件夹
        val audioDir = File(workImagesDir, AUDIO_FOLDER_NAME)
        audioDir.listFiles()?.forEach { file ->
            file.deleteRecursively()
            deletedFiles++
        }

        // 清理工作目录下的其他直接文件（兼容旧数据）
        workImagesDir.listFiles()?.forEach { file ->
            if (file.isFile) {
                file.delete()
                deletedFiles++
            } else if (file.name != IMAGES_FOLDER_NAME && file.name != BADGES_FOLDER_NAME && file.name != AUDIO_FOLDER_NAME) {
                file.deleteRecursively()
                deletedFolders++
            }
        }
    }

    /**
     * 清理所有缓存（临时文件夹 + 工作目录 + URI映射缓存）
     * 应用启动和退出时调用，保留预设、草稿、图包
     * 注意：URI映射缓存会被完全清空，确保启动时环境干净
     */
    fun cleanupAllCache() {
        cleanupTempDirs()
        cleanupWorkImages()
        // 完全清空URI映射缓存（与resetEditEnvironment保持一致）
        try {
            val prefs = context.getSharedPreferences("uri_file_mapping", Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
        } catch (e: Exception) {
        }
    }

    /**
     * 获取工作图片文件夹路径
     */
    fun getWorkImagesDirectory(): File {
        return workImagesDir
    }

    // 图包文件夹
    private val packagesDir: File by lazy {
        File(context.filesDir, PACKAGES_FOLDER_NAME).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    /**
     * 获取图包文件夹路径
     */
    fun getPackagesDirectory(): File {
        return packagesDir
    }

    /**
     * 获取所有可用的小图标，使用xxHash64快速哈希去重，按文件名自然排序
     * 自然排序会将数字按数值比较，例如: b_img1, b_img2, b_img10 而不是 b_img1, b_img10, b_img2
     */
    fun getAvailableBadges(): List<Uri> {
        data class BadgeInfo(val uri: Uri, val fileName: String, val sortKey: String)
        val badges = mutableListOf<BadgeInfo>()
        val seenHashes = mutableSetOf<String>() // 用于去重的哈希集合
        try {
            val workBadgesDir = File(workImagesDir, BADGES_FOLDER_NAME)
            if (workBadgesDir.exists() && workBadgesDir.isDirectory) {
                workBadgesDir.listFiles()?.forEach { file ->
                    if (file.isFile && (file.extension.lowercase() in listOf("jpg", "jpeg", "png", "webp", "gif"))) {
                        // 使用ImageResourceManager的快速哈希算法
                        val quickHash = ImageResourceManager.calculateQuickHash(file)
                        if (!seenHashes.contains(quickHash)) {
                            seenHashes.add(quickHash)
                            // 提取排序键：去掉时间戳前缀（格式: timestamp_uuid_originalName）
                            val sortKey = extractSortKeyFromFileName(file.name)
                            badges.add(BadgeInfo(Uri.fromFile(file), file.name, sortKey))
                        }
                    }
                }
            }
            // 按提取的排序键进行自然排序
            val sortedBadges = badges.sortedWith { a, b ->
                naturalOrderCompare(a.sortKey, b.sortKey)
            }.map { it.uri }
            return sortedBadges
        } catch (e: Exception) {
        }
        return emptyList()
    }
    
    /**
     * 从文件名中提取排序键
     * 去掉时间戳前缀（格式: timestamp_uuid_originalName）
     */
    private fun extractSortKeyFromFileName(fileName: String): String {
        // 匹配格式: 数字_8位UUID_原始文件名
        val regex = Regex("^\\d+_[a-f0-9]{8}_(.+)$")
        val match = regex.find(fileName)
        return if (match != null) {
            // 返回原始文件名部分
            match.groupValues[1]
        } else {
            // 如果不匹配，返回原文件名
            fileName
        }
    }
    
    /**
     * 自然排序比较两个字符串
     */
    private fun naturalOrderCompare(s1: String, s2: String): Int {
        val regex = Regex("([0-9]+)|([^0-9]+)")
        
        val parts1 = regex.findAll(s1).map { match ->
            val numberPart = match.groupValues[1]
            if (numberPart.isNotEmpty()) {
                SortPart.Number(numberPart.toLong())
            } else {
                SortPart.Text(match.groupValues[2].lowercase())
            }
        }.toList()
        
        val parts2 = regex.findAll(s2).map { match ->
            val numberPart = match.groupValues[1]
            if (numberPart.isNotEmpty()) {
                SortPart.Number(numberPart.toLong())
            } else {
                SortPart.Text(match.groupValues[2].lowercase())
            }
        }.toList()

        val minLength = minOf(parts1.size, parts2.size)
        for (i in 0 until minLength) {
            val cmp = compareParts(parts1[i], parts2[i])
            if (cmp != 0) return cmp
        }
        return parts1.size - parts2.size
    }

    /**
     * 排序部分密封类
     */
    private sealed class SortPart {
        data class Text(val value: String) : SortPart()
        data class Number(val value: Long) : SortPart()
    }
    
    /**
     * 比较两个排序部分
     */
    private fun compareParts(p1: SortPart, p2: SortPart): Int {
        return when {
            p1 is SortPart.Text && p2 is SortPart.Text -> p1.value.compareTo(p2.value)
            p1 is SortPart.Number && p2 is SortPart.Number -> p1.value.compareTo(p2.value)
            // 当类型不同时，数字总是排在文本前面（这是自然排序的惯例）
            p1 is SortPart.Number && p2 is SortPart.Text -> -1
            p1 is SortPart.Text && p2 is SortPart.Number -> 1
            else -> 0
        }
    }

    /**
     * 获取所有已导入的图包
     */
    fun getImportedPackages(): List<File> {
        return packagesDir.listFiles()?.filter { it.isFile && it.extension.lowercase() == "zip" }
            ?.sortedWith(compareBy { naturalOrderCompare(it.name.lowercase(), "") })
            ?: emptyList()
    }

    /**
     * 删除已导入的图包
     */
    fun deleteImportedPackage(packageFile: File): Boolean {
        return try {
            if (packageFile.exists() && packageFile.parentFile?.absolutePath == packagesDir.absolutePath) {
                packageFile.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 保存导入的图包到图包目录
     * 自动检查并转换为WebP格式，支持密码解压
     *
     * @param sourceUri 源文件URI
     * @param fileName 文件名
     * @param password ZIP密码（可选）
     * @return 保存的文件，失败返回null
     */
    suspend fun saveImportedPackage(sourceUri: Uri, fileName: String, password: String? = null): File? = withContext(Dispatchers.IO) {
        try {
            val destFile = File(packagesDir, fileName)

            // 先保存到临时文件
            val tempFile = File(context.cacheDir, "temp_package_$fileName")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            // 检查ZIP是否加密且需要密码
            val zipFile = net.lingala.zip4j.ZipFile(tempFile)
            if (zipFile.isEncrypted && password == null) {
                tempFile.delete()
                throw ZipPasswordRequiredException("该ZIP文件已加密，请输入密码")
            }

            // 检查是否需要转换格式
            val needsConversion = checkPackageNeedsConversion(tempFile, if (zipFile.isEncrypted) password else null)

            if (needsConversion) {
                // 转换为WebP格式并覆盖
                val convertedFile = convertPackageToWebP(tempFile, password)
                if (convertedFile != null) {
                    convertedFile.copyTo(destFile, overwrite = true)
                    convertedFile.delete()
                } else {
                    // 转换失败，使用原文件
                    tempFile.copyTo(destFile, overwrite = true)
                }
                tempFile.delete()
            } else {
                // 不需要转换，直接保存
                tempFile.copyTo(destFile, overwrite = true)
                tempFile.delete()
            }

            destFile
        } catch (e: net.lingala.zip4j.exception.ZipException) {
            // 密码错误或其他ZIP异常
            if (e.message?.contains("password", ignoreCase = true) == true ||
                e.message?.contains("Wrong Password", ignoreCase = true) == true) {
                throw ZipPasswordRequiredException("密码错误，请重新输入")
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 检查图包是否需要转换格式
     * @param packageFile 图包文件
     * @param password 密码（加密ZIP需要）
     */
    private fun checkPackageNeedsConversion(packageFile: File, password: String? = null): Boolean {
        return try {
            val zipFile = net.lingala.zip4j.ZipFile(packageFile)
            if (password != null) zipFile.setPassword(password.toCharArray())
            zipFile.fileHeaders.any { fileHeader ->
                if (fileHeader.isDirectory) return@any false
                val name = fileHeader.fileName.lowercase()
                (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".gif")) && !name.endsWith(".webp")
            }
        } catch (e: Exception) { false }
    }

    /**
     * 将图包转换为WebP格式
     * @param sourceFile 源图包文件
     * @param password 密码（加密ZIP需要）
     */
    private fun convertPackageToWebP(sourceFile: File, password: String? = null): File? {
        return try {
            val tempDir = context.cacheDir.resolve("convert_webp_${System.currentTimeMillis()}")
            tempDir.mkdirs()

            val outputFile = File(tempDir, "converted.zip")
            val sourceZipFile = net.lingala.zip4j.ZipFile(sourceFile)
            if (password != null) sourceZipFile.setPassword(password.toCharArray())

            java.util.zip.ZipOutputStream(FileOutputStream(outputFile)).use { zipOut ->
                val fileHeaders = sourceZipFile.fileHeaders
                for (fileHeader in fileHeaders) {
                    val entryName = fileHeader.fileName

                    if (fileHeader.isDirectory) {
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
                            val fileName = entryName.substringAfterLast("/")
                            val tempImageFile = File(tempDir, "temp_$fileName")
                            sourceZipFile.getInputStream(fileHeader).use { input ->
                                tempImageFile.outputStream().use { output -> input.copyTo(output) }
                            }

                            val baseName = fileName.substringBeforeLast(".")
                            val webpName = "$baseName.webp"
                            val webpFile = File(tempDir, webpName)
                            val conversionResult = WebPConverter.convertToWebP(tempImageFile, webpFile)

                            val outputEntryName = if (entryName.contains("/")) {
                                "${entryName.substringBeforeLast("/")}/$webpName"
                            } else webpName

                            if (conversionResult.success) {
                                zipOut.putNextEntry(java.util.zip.ZipEntry(outputEntryName))
                                webpFile.inputStream().use { input -> input.copyTo(zipOut) }
                                zipOut.closeEntry()
                            } else {
                                zipOut.putNextEntry(java.util.zip.ZipEntry(entryName))
                                tempImageFile.inputStream().use { input -> input.copyTo(zipOut) }
                                zipOut.closeEntry()
                            }

                            tempImageFile.delete()
                            webpFile.delete()
                        } else {
                            zipOut.putNextEntry(java.util.zip.ZipEntry(entryName))
                            sourceZipFile.getInputStream(fileHeader).use { input -> input.copyTo(zipOut) }
                            zipOut.closeEntry()
                        }
                    }
                }
            }

            val result = outputFile.copyTo(File(context.cacheDir, "final_converted.zip"), overwrite = true)
            tempDir.deleteRecursively()
            result
        } catch (e: Exception) { null }
    }

    /**
     * 获取所有已保存的预设列表
     */
    fun getAllPresets(): List<PresetInfo> {
        return presetsDir.listFiles { file ->
            file.isFile && file.name.endsWith(PRESET_EXTENSION)
        }?.map { file ->
            PresetInfo(
                name = file.nameWithoutExtension,
                fileName = file.name,
                file = file,
                createTime = file.lastModified()
            )
        }?.sortedWith(compareBy { naturalOrderCompare(it.name.lowercase(), "") }) ?: emptyList()
    }

    /**
     * 检查预设名称是否已存在
     */
    fun isPresetNameExists(name: String): Boolean {
        val fileName = sanitizeFileName(name) + PRESET_EXTENSION
        return File(presetsDir, fileName).exists()
    }

    /**
     * 保存预设到预设文件夹（从工作目录读取图片）
     * @param presetName 预设名称
     * @param presetData 预设数据
     * @return 保存的文件
     */
    suspend fun savePreset(
        presetName: String,
        presetData: PresetData
    ): File = withContext(Dispatchers.IO) {
        val sanitizedName = sanitizeFileName(presetName)
        val presetFile = File(presetsDir, "$sanitizedName$PRESET_EXTENSION")

        // 如果文件已存在，先删除
        if (presetFile.exists()) {
            presetFile.delete()
        }

        // 从工作目录创建ZIP文件（直接保存，不做规范化）
        savePresetFromWorkDir(presetData, presetFile)

        presetFile
    }

    /**
     * 导出预设到指定路径（从工作目录读取图片，进行规范化命名）
     * @param presetData 预设数据
     * @param outputFile 输出文件
     * @return 导出的文件
     */
    suspend fun exportPreset(
        presetData: PresetData,
        outputFile: File
    ): File = withContext(Dispatchers.IO) {
        // 从工作目录创建ZIP文件（规范化命名）
        exportPresetFromWorkDir(presetData, outputFile)

        outputFile
    }

    // 保留旧方法用于兼容性（从临时文件夹导出）
    suspend fun exportPreset(
        presetName: String,
        presetData: PresetData,
        tempDir: File,
        outputFile: File
    ): File = withContext(Dispatchers.IO) {
        // 创建ZIP文件
        ZipOutputStream(FileOutputStream(outputFile)).use { zipOut ->
            // 写入配置文件
            val configJson = presetDataToJson(presetData)
            zipOut.putNextEntry(ZipEntry(CONFIG_FILE_NAME))
            zipOut.write(configJson.toString().toByteArray())
            zipOut.closeEntry()
            
            // 写入图片文件
            val imagesDir = File(tempDir, IMAGES_FOLDER_NAME)
            imagesDir.listFiles()?.forEach { file ->
                zipOut.putNextEntry(ZipEntry("$IMAGES_FOLDER_NAME/${file.name}"))
                FileInputStream(file).use { input ->
                    input.copyTo(zipOut)
                }
                zipOut.closeEntry()
            }
            
            // 写入小图标文件
            val badgesDir = File(tempDir, BADGES_FOLDER_NAME)
            badgesDir.listFiles()?.forEach { file ->
                zipOut.putNextEntry(ZipEntry("$BADGES_FOLDER_NAME/${file.name}"))
                FileInputStream(file).use { input ->
                    input.copyTo(zipOut)
                }
                zipOut.closeEntry()
            }
        }
        
        outputFile
    }

    /**
     * 导入结果数据类
     */
    data class ImportResult(
        val presetFile: File,
        val presetData: PresetData,
        val status: ImportStatus,
        val existingPresetFile: File? = null
    )
    
    /**
     * 导入状态枚举
     */
    enum class ImportStatus {
        SUCCESS,           // 导入成功
        ALREADY_EXISTS,    // 完全相同的预设已存在
        NEEDS_OVERWRITE    // 需要询问是否覆盖
    }
    
    /**
     * 从预设文件只读取配置（不解压图片）
     * @param presetFile 预设文件
     * @return 预设数据
     */
    private fun readPresetConfig(presetFile: File): PresetData? {
        ZipInputStream(FileInputStream(presetFile)).use { zipIn ->
            var entry: ZipEntry?
            while (zipIn.nextEntry.also { entry = it } != null) {
                if (entry!!.name == CONFIG_FILE_NAME) {
                    val jsonString = zipIn.bufferedReader().readText()
                    zipIn.closeEntry()
                    return jsonToPresetData(JSONObject(jsonString))
                }
                zipIn.closeEntry()
            }
        }
        return null
    }

    /**
     * 从URI导入预设
     * @param uri 预设文件URI
     * @return 导入结果，包含预设数据、临时文件夹和状态
     */
    suspend fun importPreset(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, "import_temp_${System.currentTimeMillis()}$PRESET_EXTENSION")

        try {
            // 复制到临时文件
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalStateException("无法打开文件")

            // 先只读取配置获取标题（不解压图片，提升性能）
            val presetData = readPresetConfig(tempFile)
                ?: throw IllegalStateException("预设文件损坏: 无法读取配置")

            // 检查是否已存在相同标题的预设
            val existingPresetByName = findPresetByName(presetData.title)
            if (existingPresetByName != null) {
                // 标题相同，返回需要覆盖的状态（不删除临时文件，后续覆盖时需要使用）
                return@withContext ImportResult(tempFile, presetData, ImportStatus.NEEDS_OVERWRITE, existingPresetByName.file)
            }

            // 将预设复制到预设文件夹
            val presetName = presetData.title
            val sanitizedName = sanitizeFileName(presetName)
            val presetFile = File(presetsDir, "$sanitizedName$PRESET_EXTENSION")

            tempFile.copyTo(presetFile, overwrite = true)

            // 成功导入后删除临时文件
            tempFile.delete()

            ImportResult(presetFile, presetData, ImportStatus.SUCCESS)
        } catch (e: Exception) {
            // 发生异常时删除临时文件
            tempFile.delete()
            throw e
        }
    }
    
    /**
     * 覆盖已存在的预设
     * @param sourceFile 源预设文件（新导入的文件）
     * @param existingFile 已存在的预设文件
     * @return 保存的文件
     */
    suspend fun overwritePreset(
        sourceFile: File,
        existingFile: File
    ): File = withContext(Dispatchers.IO) {
        // 删除旧文件
        if (existingFile.exists()) {
            existingFile.delete()
        }

        // 读取源文件配置获取标题
        val presetData = readPresetConfig(sourceFile)
            ?: throw IllegalStateException("源预设文件损坏")

        // 保存新预设
        val presetName = presetData.title
        val sanitizedName = sanitizeFileName(presetName)
        val presetFile = File(presetsDir, "$sanitizedName$PRESET_EXTENSION")

        // 直接复制源文件
        sourceFile.copyTo(presetFile, overwrite = true)

        // 注意: 不删除源临时文件，由调用方决定何时删除
        // 因为调用方可能还需要使用源文件（如应用预设）

        presetFile
    }

    /**
     * 保存导入的预设为新名称
     * 复制源文件内容，但修改配置文件中的标题
     * @param sourceFile 源预设文件
     * @param newName 新预设名称
     * @return 保存的文件
     */
    suspend fun saveImportedPresetAsNew(
        sourceFile: File,
        newName: String
    ): File = withContext(Dispatchers.IO) {
        // 读取源文件配置
        val presetData = readPresetConfig(sourceFile)
            ?: throw IllegalStateException("源预设文件损坏")

        // 创建新的预设数据，使用新名称
        val newPresetData = presetData.copy(title = newName)
        val sanitizedName = sanitizeFileName(newName)
        val presetFile = File(presetsDir, "$sanitizedName$PRESET_EXTENSION")

        // 创建新的ZIP文件，复制源文件内容但更新配置
        ZipOutputStream(FileOutputStream(presetFile)).use { zipOut ->
            // 写入更新后的配置文件
            val configJson = presetDataToJson(newPresetData)
            zipOut.putNextEntry(ZipEntry(CONFIG_FILE_NAME))
            zipOut.write(configJson.toString().toByteArray())
            zipOut.closeEntry()

            // 复制源文件中的其他条目（图片等）
            ZipInputStream(FileInputStream(sourceFile)).use { zipIn ->
                var entry: ZipEntry?
                while (zipIn.nextEntry.also { entry = it } != null) {
                    entry?.let {
                        if (it.name != CONFIG_FILE_NAME) {
                            zipOut.putNextEntry(ZipEntry(it.name))
                            zipIn.copyTo(zipOut)
                            zipOut.closeEntry()
                        }
                    }
                    zipIn.closeEntry()
                }
            }
        }

        presetFile
    }
    
    /**
     * 根据标题查找预设（快速检查）
     */
    private fun findPresetByName(title: String): PresetInfo? {
        return getAllPresets().find { it.name == title }
    }

    /**
     * 删除预设
     * @param presetInfo 预设信息
     * @return 是否删除成功
     */
    fun deletePreset(presetInfo: PresetInfo): Boolean {
        val result = presetInfo.file.delete()
        if (result) {
            // 删除预设后立即清理未被引用的工作目录图片
            cleanupUnusedWorkImages()
        }
        return result
    }

    /**
     * 清理工作目录中未被任何预设引用的图片
     */
    private fun cleanupUnusedWorkImages() {
        try {
            val workImagesDir = File(context.filesDir, WORK_FOLDER_NAME)
            if (!workImagesDir.exists()) return

            // 获取所有预设中引用的图片文件名
            val referencedImages = mutableSetOf<String>()
            val allPresets = getAllPresets()
            
            for (preset in allPresets) {
                try {
                    ZipInputStream(FileInputStream(preset.file)).use { zipIn ->
                        var entry: ZipEntry?
                        while (zipIn.nextEntry.also { entry = it } != null) {
                            entry?.let {
                                if (it.name.startsWith("$IMAGES_FOLDER_NAME/") || 
                                    it.name.startsWith("$BADGES_FOLDER_NAME/")) {
                                    referencedImages.add(it.name.substringAfterLast('/'))
                                }
                            }
                            zipIn.closeEntry()
                        }
                    }
                } catch (e: Exception) {
                }
            }

            // 清理工作目录中未被引用的图片
            var deletedCount = 0
            var totalToDelete = 0
            
            // 先统计需要删除的文件数量
            workImagesDir.listFiles()?.forEach { folder ->
                if (folder.isDirectory) {
                    folder.listFiles()?.forEach { file ->
                        if (file.name !in referencedImages) {
                            totalToDelete++
                        }
                    }
                }
            }
            
            // 执行删除
            workImagesDir.listFiles()?.forEach { folder ->
                if (folder.isDirectory) {
                    folder.listFiles()?.forEach { file ->
                        if (file.name !in referencedImages) {
                            file.delete()
                            deletedCount++
                        }
                    }
                    // 如果文件夹为空，删除文件夹
                    if (folder.listFiles()?.isEmpty() == true) {
                        folder.delete()
                    }
                }
            }
        } catch (e: Exception) {
        }
    }

    /**
     * 将音频URI复制到工作目录的 audio 文件夹
     * @param uri 音频文件 URI
     * @return 复制后的文件 URI，失败返回 null
     */
    fun copyAudioUriToWorkDir(uri: Uri): Uri? {
        val audioDir = File(workImagesDir, AUDIO_FOLDER_NAME)
        audioDir.mkdirs()

        val originalName = extractFileNameFromUri(uri) ?: "audio_${System.currentTimeMillis()}.m4a"
        val safeName = sanitizeFileName(originalName).let { if (it.isBlank()) "audio_${System.currentTimeMillis()}.m4a" else it }
        val extension = safeName.substringAfterLast(".", "m4a").lowercase().let {
            if (it in listOf("mp3", "wav", "m4a", "aac", "ogg", "flac")) it else "m4a"
        }
        val baseName = safeName.substringBeforeLast(".", safeName)
        val targetFile = File(audioDir, "${baseName}.${extension}")

        return try {
            when (uri.scheme) {
                "file" -> {
                    val sourceFile = File(uri.path!!)
                    if (sourceFile.exists()) sourceFile.copyTo(targetFile, overwrite = true)
                    else return null
                }
                else -> {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(targetFile).use { output -> input.copyTo(output) }
                    } ?: return null
                }
            }
            Uri.fromFile(targetFile)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 将URI复制到临时文件夹
     * @param uri 图片URI
     * @param folder 目标文件夹（IMAGES_FOLDER_NAME 或 BADGES_FOLDER_NAME）
     * @return 文件名，如果复制失败则返回null
     */
    fun copyUriToTemp(uri: Uri, folder: String = IMAGES_FOLDER_NAME): String? {
        val tempDir = getCurrentTempDir()
        val targetDir = File(tempDir, folder)
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }
        
        val fileName = "${UUID.randomUUID()}.webp"
        val targetFile = File(targetDir, fileName)
        
        return try {
            // 检查URI是否是文件URI
            if (uri.scheme == "file") {
                val sourceFile = File(uri.path!!)
                if (sourceFile.exists()) {
                    // 直接复制文件
                    sourceFile.copyTo(targetFile)
                } else {
                    // 文件不存在于URI路径，尝试从工作目录查找
                    val workDirFile = File(workImagesDir, "$folder/${sourceFile.name}")
                    if (workDirFile.exists()) {
                        workDirFile.copyTo(targetFile)
                    } else {
                        return null
                    }
                }
            } else {
                // 使用ContentResolver复制
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: run {
                    return null
                }
            }
            fileName
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 从临时文件夹获取图片URI
     * @param fileName 文件名
     * @param folder 文件夹名称
     * @return 图片URI
     */
    fun getImageUriFromTemp(fileName: String, folder: String = IMAGES_FOLDER_NAME): Uri {
        val tempDir = getCurrentTempDir()
        val file = File(tempDir, "$folder/$fileName")
        return Uri.fromFile(file)
    }

    /**
     * 从工作目录获取图片URI
     * @param fileName 文件名
     * @param folder 文件夹名称
     * @return 图片URI
     */
    fun getImageUriFromWorkDir(fileName: String, folder: String = IMAGES_FOLDER_NAME): Uri {
        val file = File(workImagesDir, "$folder/$fileName")
        return Uri.fromFile(file)
    }

    /**
     * 将预设数据转换为JSON
     */
    private fun presetDataToJson(presetData: PresetData): JSONObject {
        return JSONObject().apply {
            put("title", presetData.title)
            put("author", presetData.author)
            put("cropPositionX", presetData.cropPositionX)
            put("cropPositionY", presetData.cropPositionY)
            put("customCropWidth", presetData.customCropWidth)
            put("customCropHeight", presetData.customCropHeight)
            put("useCustomCropSize", presetData.useCustomCropSize)
            put("cropRatio", presetData.cropRatio)
            
            // 层级列表
            put("tiers", JSONArray().apply {
                presetData.tiers.forEach { tier ->
                    put(JSONObject().apply {
                        put("label", tier.label)
                        put("color", tier.color)
                    })
                }
            })
            
            // 层级图片列表
            put("tierImages", JSONArray().apply {
                presetData.tierImages.forEach { image ->
                    put(JSONObject().apply {
                        put("id", image.id)
                        put("tierLabel", image.tierLabel)
                        put("imageFileName", image.imageFileName)
                        put("name", image.name)
                        put("badgeFileName1", image.badgeFileName1)
                        put("badgeFileName2", image.badgeFileName2)
                        put("badgeFileName3", image.badgeFileName3)
                        put("audioFileName", image.audioFileName)
                        // 保存裁剪状态
                        put("cropPositionX", image.cropPositionX)
                        put("cropPositionY", image.cropPositionY)
                        put("cropScale", image.cropScale)
                        put("isCropped", image.isCropped)
                        // 保存裁剪比例
                        put("cropRatio", image.cropRatio)
                        put("useCustomCrop", image.useCustomCrop)
                        put("customCropWidth", image.customCropWidth)
                        put("customCropHeight", image.customCropHeight)
                    })
                }
            })
            
            // 待分级图片列表
            put("pendingImages", JSONArray().apply {
                presetData.pendingImages.forEach { put(it) }
            })

            // 视频生成配置
            put("videoConfig", videoConfigToJson(presetData.videoConfig))
        }
    }

    /**
     * 从JSON解析预设数据
     */
    private fun jsonToPresetData(json: JSONObject): PresetData {
        // 解析层级列表
        val tiers = mutableListOf<TierItemData>()
        val tiersArray = json.getJSONArray("tiers")
        for (i in 0 until tiersArray.length()) {
            val tierJson = tiersArray.getJSONObject(i)
            tiers.add(TierItemData(
                label = tierJson.getString("label"),
                color = tierJson.getString("color")
            ))
        }
        
        // 解析层级图片列表
        val tierImages = mutableListOf<TierImageData>()
        val imagesArray = json.getJSONArray("tierImages")
        for (i in 0 until imagesArray.length()) {
            val imageJson = imagesArray.getJSONObject(i)
            tierImages.add(TierImageData(
                id = imageJson.getString("id"),
                tierLabel = imageJson.getString("tierLabel"),
                imageFileName = imageJson.getString("imageFileName"),
                name = imageJson.optString("name", ""),
                badgeFileName1 = imageJson.optString("badgeFileName1").takeIf { it.isNotEmpty() },
                badgeFileName2 = imageJson.optString("badgeFileName2").takeIf { it.isNotEmpty() },
                badgeFileName3 = imageJson.optString("badgeFileName3").takeIf { it.isNotEmpty() },
                audioFileName = imageJson.optString("audioFileName").takeIf { it.isNotEmpty() },
                // 恢复裁剪状态
                cropPositionX = imageJson.optDouble("cropPositionX", 0.5).toFloat(),
                cropPositionY = imageJson.optDouble("cropPositionY", 0.5).toFloat(),
                cropScale = imageJson.optDouble("cropScale", 1.0).toFloat(),
                isCropped = imageJson.optBoolean("isCropped", false),
                // 恢复裁剪比例
                cropRatio = imageJson.optDouble("cropRatio", 1.0).toFloat(),
                useCustomCrop = imageJson.optBoolean("useCustomCrop", false),
                customCropWidth = imageJson.optInt("customCropWidth", 0),
                customCropHeight = imageJson.optInt("customCropHeight", 0)
            ))
        }
        
        // 解析待分级图片列表
        val pendingImages = mutableListOf<String>()
        val pendingArray = json.getJSONArray("pendingImages")
        for (i in 0 until pendingArray.length()) {
            pendingImages.add(pendingArray.getString(i))
        }
        
        return PresetData(
            title = json.getString("title"),
            author = json.getString("author"),
            tiers = tiers,
            tierImages = tierImages,
            pendingImages = pendingImages,
            cropPositionX = json.optDouble("cropPositionX", 0.5).toFloat(),
            cropPositionY = json.optDouble("cropPositionY", 0.5).toFloat(),
            customCropWidth = json.optInt("customCropWidth", 0),
            customCropHeight = json.optInt("customCropHeight", 0),
            useCustomCropSize = json.optBoolean("useCustomCropSize", false),
            cropRatio = json.optDouble("cropRatio", 1.0).toFloat(),
            videoConfig = json.optJSONObject("videoConfig")?.let { videoConfigFromJson(it) } ?: VideoGenerationConfig()
        )
    }

    /**
     * 将视频生成配置转换为JSON
     */
    private fun videoConfigToJson(config: VideoGenerationConfig): JSONObject {
        return JSONObject().apply {
            put("actionOrder", JSONArray(config.actionOrder.map { it.name }))
            put("granularity", config.granularity.name)
            put("imageIntervalSource", config.imageIntervalSource.name)
            put("fixedImageInterval", config.fixedImageInterval.toDouble())
            put("badgeInterval", config.badgeInterval.toDouble())
            put("nameDisplayMode", config.nameDisplayMode.name)
            put("nameCharInterval", config.nameCharInterval.toDouble())
            put("crossTypePause", config.crossTypePause.toDouble())
            put("crossImagePause", config.crossImagePause.toDouble())
            put("extraAudioOffset", config.extraAudioOffset.toDouble())
            put("audioOverlayMode", config.audioOverlayMode.name)
            put("backgroundMusicUri", config.backgroundMusicUri ?: JSONObject.NULL)
            put("backgroundMusicVolume", config.backgroundMusicVolume.toDouble())
            put("narrationVolume", config.narrationVolume.toDouble())
            put("sfxVolume", config.sfxVolume.toDouble())
            put("outputWidth", config.outputWidth)
            put("outputHeight", config.outputHeight)
        }
    }

    /**
     * 从JSON解析视频生成配置
     */
    private fun videoConfigFromJson(json: JSONObject): VideoGenerationConfig? {
        return try {
            VideoGenerationConfig(
                actionOrder = json.getJSONArray("actionOrder").let { array ->
                    List(array.length()) { index ->
                        VideoActionType.valueOf(array.getString(index))
                    }
                },
                granularity = try {
                    ArrangementGranularity.valueOf(json.getString("granularity"))
                } catch (_: Exception) {
                    ArrangementGranularity.PER_IMAGE
                },
                imageIntervalSource = AudioIntervalSource.valueOf(json.getString("imageIntervalSource")),
                fixedImageInterval = json.getDouble("fixedImageInterval").toFloat(),
                badgeInterval = json.getDouble("badgeInterval").toFloat(),
                nameDisplayMode = NameDisplayMode.valueOf(json.getString("nameDisplayMode")),
                nameCharInterval = json.getDouble("nameCharInterval").toFloat(),
                crossTypePause = json.getDouble("crossTypePause").toFloat(),
                crossImagePause = json.getDouble("crossImagePause").toFloat(),
                extraAudioOffset = json.getDouble("extraAudioOffset").toFloat(),
                audioOverlayMode = AudioOverlayMode.valueOf(json.getString("audioOverlayMode")),
                backgroundMusicUri = json.optString("backgroundMusicUri").takeIf { it != "null" },
                backgroundMusicVolume = json.getDouble("backgroundMusicVolume").toFloat(),
                narrationVolume = json.getDouble("narrationVolume").toFloat(),
                sfxVolume = json.getDouble("sfxVolume").toFloat(),
                outputWidth = json.getInt("outputWidth"),
                outputHeight = json.getInt("outputHeight")
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 清理文件名，移除不安全字符
     */
    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5_-]"), "_")
            .take(50) // 限制长度
    }

    // ==================== 草稿管理功能 ====================

    private val draftDir: File by lazy {
        File(context.filesDir, DRAFT_FOLDER_NAME).apply { mkdirs() }
    }

    // 草稿文件路径（单文件覆盖模式）
    private val draftFile: File by lazy {
        File(draftDir, "$DRAFT_PRESET_NAME$PRESET_EXTENSION")
    }

    /**
     * 检查是否存在草稿
     * 同时处理原子写入过程中断的情况：
     * - 如果存在 .tmp 文件但不存在 .tdpreset，尝试恢复 .tmp
     * - 如果存在 .bak 文件但不存在 .tdpreset，尝试恢复 .bak
     * @return 是否存在草稿
     */
    fun hasDraft(): Boolean {
        // 正常情况：正式文件存在
        if (draftFile.exists() && draftFile.length() > 0) {
            return true
        }
        
        // 异常情况1：保存过程中断，临时文件存在但正式文件不存在
        val tempFile = File(draftDir, "$DRAFT_PRESET_NAME.tmp")
        if (tempFile.exists() && tempFile.length() > 0) {
            if (tempFile.renameTo(draftFile)) {
                return true
            }
        }
        
        // 异常情况2：保存过程中断，备份文件存在但正式文件不存在
        val backupFile = File(draftDir, "$DRAFT_PRESET_NAME.bak")
        if (backupFile.exists() && backupFile.length() > 0) {
            if (backupFile.renameTo(draftFile)) {
                return true
            }
        }
        
        return false
    }

    /**
     * 保存草稿（覆盖模式，始终只有一个草稿文件）
     * 使用原子写入：先写入临时文件，完成后重命名，避免写入过程中断导致文件损坏
     * @param presetData 预设数据
     * @return 保存成功返回true，失败返回false
     */
    suspend fun saveDraft(presetData: PresetData): Boolean = withContext(Dispatchers.IO) {
        try {
            // 确保草稿目录存在
            if (!draftDir.exists()) {
                draftDir.mkdirs()
            }

            // 原子写入：先写入临时文件（直接保存，不做规范化）
            val tempFile = File(draftDir, "$DRAFT_PRESET_NAME.tmp")
            savePresetFromWorkDir(presetData, tempFile)

            // 原子替换：新文件 -> 备份文件 -> 删除旧文件 -> 重命名新文件
            val backupFile = File(draftDir, "$DRAFT_PRESET_NAME.bak")
            
            // 如果已存在旧文件，先重命名为备份
            if (draftFile.exists()) {
                draftFile.renameTo(backupFile)
            }
            
            // 将新文件重命名为正式文件
            val success = tempFile.renameTo(draftFile)
            
            // 成功后删除备份文件
            if (success && backupFile.exists()) {
                backupFile.delete()
            }

            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 检查文件名是否符合规范格式
     * 规范格式: img_{数字}.{扩展名} 或 badge_{数字}.{扩展名}
     */
    private fun isValidFileName(fileName: String, prefix: String): Boolean {
        val regex = Regex("^${prefix}_(\\d+)\\.([a-zA-Z0-9]+)$")
        return regex.matches(fileName)
    }

    /**
     * 从工作目录保存预设（不经过临时文件夹，不做规范化命名）
     * 直接打包，保持原有文件名
     * 自动将图包中的图片复制到工作目录
     * 保存所有可用的小图标（去重后的），而不仅仅是预设中引用的
     * @param presetData 预设数据
     * @param outputFile 输出文件
     * @return 更新后的预设数据（图包图片已复制到工作目录并更新文件名）
     */
    private fun savePresetFromWorkDir(presetData: PresetData, outputFile: File): PresetData {
        // 收集所有需要的图片文件名
        val neededImages = mutableSetOf<String>()
        val neededBadges = mutableSetOf<String>()
        val neededAudio = mutableSetOf<String>()

        presetData.tierImages.forEach { imageData ->
            neededImages.add(imageData.imageFileName)
            imageData.badgeFileName1?.let { neededBadges.add(it) }
            imageData.badgeFileName2?.let { neededBadges.add(it) }
            imageData.badgeFileName3?.let { neededBadges.add(it) }
            imageData.audioFileName?.let { neededAudio.add(it) }
        }
        presetData.pendingImages.forEach { neededImages.add(it) }

        val workImagesFolder = File(workImagesDir, IMAGES_FOLDER_NAME)
        val workBadgesFolder = File(workImagesDir, BADGES_FOLDER_NAME)
        val workAudioFolder = File(workImagesDir, AUDIO_FOLDER_NAME)
        workImagesFolder.mkdirs()
        workBadgesFolder.mkdirs()
        workAudioFolder.mkdirs()
        
        // 获取所有可用的小图标文件名（去重后的），并添加到neededBadges中
        // 这样保存预设时会包含所有小图标，而不仅仅是预设中引用的
        val allAvailableBadges = getAvailableBadges()
        allAvailableBadges.forEach { uri ->
            val fileName = uri.lastPathSegment?.substringAfterLast('/')
            if (fileName != null) {
                neededBadges.add(fileName)
            }
        }
        
        // 图包图片存储在 context.filesDir 根目录
        val packageImagesFolder = context.filesDir
        
        // 记录需要更新文件名的图片映射（旧文件名 -> 新文件名）
        val imageNameMapping = mutableMapOf<String, String>()
        var copiedImagesCount = 0

        // 首先检查并复制图包图片到工作目录
        neededImages.forEach { fileName ->
            val workFile = File(workImagesFolder, fileName)
            if (!workFile.exists()) {
                // 如果不在工作目录，在图包目录中查找
                val packageFile = File(packageImagesFolder, fileName)
                if (packageFile.exists()) {
                    // 复制到工作目录，使用新的文件名（时间戳+序号格式）
                    val newFileName = "${System.currentTimeMillis()}_${copiedImagesCount}.${fileName.substringAfterLast(".", "jpg")}"
                    val destFile = File(workImagesFolder, newFileName)
                    try {
                        packageFile.copyTo(destFile, overwrite = true)
                        imageNameMapping[fileName] = newFileName
                        copiedImagesCount++
                    } catch (e: Exception) {
                    }
                }
            }
        }
        
        // 如果有图片被复制，更新预设数据中的文件名
        val updatedPresetData = if (imageNameMapping.isNotEmpty()) {
            val updatedTierImages = presetData.tierImages.map { imageData ->
                val newFileName = imageNameMapping[imageData.imageFileName]
                if (newFileName != null) {
                    imageData.copy(imageFileName = newFileName)
                } else {
                    imageData
                }
            }
            val updatedPendingImages = presetData.pendingImages.map { fileName ->
                imageNameMapping[fileName] ?: fileName
            }
            presetData.copy(
                tierImages = updatedTierImages,
                pendingImages = updatedPendingImages
            )
        } else {
            presetData
        }
        
        // 重新收集更新后的图片文件名
        val finalNeededImages = mutableSetOf<String>()
        val finalNeededBadges = mutableSetOf<String>()
        val finalNeededAudio = mutableSetOf<String>()
        updatedPresetData.tierImages.forEach { imageData ->
            finalNeededImages.add(imageData.imageFileName)
            imageData.badgeFileName1?.let { finalNeededBadges.add(it) }
            imageData.badgeFileName2?.let { finalNeededBadges.add(it) }
            imageData.badgeFileName3?.let { finalNeededBadges.add(it) }
            imageData.audioFileName?.let { finalNeededAudio.add(it) }
        }
        updatedPresetData.pendingImages.forEach { finalNeededImages.add(it) }
        
        // 添加所有可用的小图标（去重后的），确保保存预设时包含所有小图标
        allAvailableBadges.forEach { uri ->
            val fileName = uri.lastPathSegment?.substringAfterLast('/')
            if (fileName != null) {
                finalNeededBadges.add(fileName)
            }
        }

        ZipOutputStream(FileOutputStream(outputFile)).use { zipOut ->
            // 写入配置文件（使用更新后的数据）
            val configJson = presetDataToJson(updatedPresetData)
            zipOut.putNextEntry(ZipEntry(CONFIG_FILE_NAME))
            zipOut.write(configJson.toString().toByteArray())
            zipOut.closeEntry()

            // 写入图片到ZIP
            var exportedImages = 0
            finalNeededImages.forEach { fileName ->
                val sourceFile = File(workImagesFolder, fileName)
                if (sourceFile.exists()) {
                    zipOut.putNextEntry(ZipEntry("$IMAGES_FOLDER_NAME/$fileName"))
                    FileInputStream(sourceFile).use { input ->
                        input.copyTo(zipOut)
                    }
                    zipOut.closeEntry()
                    exportedImages++
                }
            }

            // 写入小图标到ZIP
            var exportedBadges = 0
            finalNeededBadges.forEach { fileName ->
                val sourceFile = File(workBadgesFolder, fileName)
                if (sourceFile.exists()) {
                    zipOut.putNextEntry(ZipEntry("$BADGES_FOLDER_NAME/$fileName"))
                    FileInputStream(sourceFile).use { input ->
                        input.copyTo(zipOut)
                    }
                    zipOut.closeEntry()
                    exportedBadges++
                }
            }

            // 写入解说音频到ZIP
            var exportedAudio = 0
            finalNeededAudio.forEach { fileName ->
                val sourceFile = File(workAudioFolder, fileName)
                if (sourceFile.exists()) {
                    zipOut.putNextEntry(ZipEntry("$AUDIO_FOLDER_NAME/$fileName"))
                    FileInputStream(sourceFile).use { input ->
                        input.copyTo(zipOut)
                    }
                    zipOut.closeEntry()
                    exportedAudio++
                }
            }
        }
        
        // 返回更新后的预设数据（包含新的文件名）
        return updatedPresetData
    }

    /**
     * 从工作目录直接导出预设（不经过临时文件夹）
     * 自动重命名图片为规范格式，并去重相同内容的图片
     * 导出所有可用的小图标（去重后的），而不仅仅是预设中引用的
     * @param presetData 预设数据
     * @param outputFile 输出文件
     * @return 更新后的预设数据（包含规范化的文件名）
     */
    private fun exportPresetFromWorkDir(presetData: PresetData, outputFile: File): PresetData {
        // 收集所有需要的图片文件名
        val neededImages = mutableSetOf<String>()
        val neededBadges = mutableSetOf<String>()
        val neededAudio = mutableSetOf<String>()

        presetData.tierImages.forEach { imageData ->
            neededImages.add(imageData.imageFileName)
            imageData.badgeFileName1?.let { neededBadges.add(it) }
            imageData.badgeFileName2?.let { neededBadges.add(it) }
            imageData.badgeFileName3?.let { neededBadges.add(it) }
            imageData.audioFileName?.let { neededAudio.add(it) }
        }
        presetData.pendingImages.forEach { neededImages.add(it) }
        
        // 获取所有可用的小图标文件名（去重后的），并添加到neededBadges中
        // 这样导出预设时会包含所有小图标，而不仅仅是预设中引用的
        val allAvailableBadges = getAvailableBadges()
        allAvailableBadges.forEach { uri ->
            val fileName = uri.lastPathSegment?.substringAfterLast('/')
            if (fileName != null) {
                neededBadges.add(fileName)
            }
        }



        // 检测相同内容的图片，建立内容到文件名的映射
        val workImagesFolder = File(workImagesDir, IMAGES_FOLDER_NAME)
        val workBadgesFolder = File(workImagesDir, BADGES_FOLDER_NAME)
        val workAudioFolder = File(workImagesDir, AUDIO_FOLDER_NAME)
        workAudioFolder.mkdirs()
        
        // 图片内容去重: 内容标识 -> 规范文件名
        val imageContentMap = mutableMapOf<String, String>()
        // 原始文件名 -> 规范文件名
        val imageNameMap = mutableMapOf<String, String>()
        
        // 为图片生成规范文件名，同时去重相同内容
        var imageIndex = 1
        var duplicateImages = 0
        neededImages.forEach { originalName ->
            val sourceFile = File(workImagesFolder, originalName)
            if (!sourceFile.exists()) {
                return@forEach
            }
            
            // 使用ImageResourceManager的快速哈希作为内容标识
            val contentKey = ImageResourceManager.calculateQuickHash(sourceFile)
            
            val existingName = imageContentMap[contentKey]
            if (existingName != null) {
                // 相同内容的图片，复用已有的规范文件名
                imageNameMap[originalName] = existingName
                duplicateImages++
            } else {
                // 新内容，生成规范文件名
                val newName = if (isValidFileName(originalName, "img") && !imageContentMap.values.contains(originalName)) {
                    originalName
                } else {
                    val ext = originalName.substringAfterLast(".", "jpg")
                    var name = "img_${imageIndex}.${ext}"
                    while (imageContentMap.values.contains(name)) {
                        imageIndex++
                        name = "img_${imageIndex}.${ext}"
                    }
                    imageIndex++
                    name
                }
                imageContentMap[contentKey] = newName
                imageNameMap[originalName] = newName
            }
        }
        
        // 小图标不需要规范化命名，保持原文件名
        // 只需要去重相同内容的小图标（多个相同内容的小图标只保存一份）
        val badgeContentMap = mutableMapOf<String, String>() // contentKey -> originalName (第一个出现的文件名)
        val badgeNameMap = mutableMapOf<String, String>() // originalName -> originalName (映射到去重后的文件名)
        
        var duplicateBadges = 0
        neededBadges.forEach { originalName ->
            val sourceFile = File(workBadgesFolder, originalName)
            if (!sourceFile.exists()) {
                return@forEach
            }
            
            // 使用ImageResourceManager的快速哈希作为内容标识
            val contentKey = ImageResourceManager.calculateQuickHash(sourceFile)

            val existingName = badgeContentMap[contentKey]
            if (existingName != null) {
                // 相同内容的小图标，映射到已存在的文件名
                badgeNameMap[originalName] = existingName
                duplicateBadges++
            } else {
                // 新内容的小图标，保持原文件名
                badgeContentMap[contentKey] = originalName
                badgeNameMap[originalName] = originalName
            }
        }
        


        // 创建更新后的PresetData，图片使用新的文件名，小图标保持原文件名（只更新去重映射）
        val updatedTierImages = presetData.tierImages.map { imageData ->
            imageData.copy(
                imageFileName = imageNameMap[imageData.imageFileName] ?: imageData.imageFileName,
                badgeFileName1 = imageData.badgeFileName1?.let { badgeNameMap[it] ?: it },
                badgeFileName2 = imageData.badgeFileName2?.let { badgeNameMap[it] ?: it },
                badgeFileName3 = imageData.badgeFileName3?.let { badgeNameMap[it] ?: it }
            )
        }
        
        val updatedPendingImages = presetData.pendingImages.map { originalName ->
            imageNameMap[originalName] ?: originalName
        }
        
        val updatedPresetData = presetData.copy(
            tierImages = updatedTierImages,
            pendingImages = updatedPendingImages
        )

        ZipOutputStream(FileOutputStream(outputFile)).use { zipOut ->
            // 写入配置文件
            val configJson = presetDataToJson(updatedPresetData)
            zipOut.putNextEntry(ZipEntry(CONFIG_FILE_NAME))
            zipOut.write(configJson.toString().toByteArray())
            zipOut.closeEntry()

            // 写入图片到ZIP，按内容去重后的文件名
            var exportedImages = 0
            imageContentMap.forEach { (contentKey, newName) ->
                // 通过 imageNameMap 查找映射到该 newName 的原始文件
                // imageNameMap: 原始文件名 -> 新文件名
                val originalNames = imageNameMap.entries.filter { it.value == newName }.map { it.key }
                
                // 查找第一个存在的原始文件
                val sourceFile = originalNames.firstNotNullOfOrNull { 
                    File(workImagesFolder, it).takeIf { f -> f.exists() } 
                } ?: run {
                    // 如果通过映射找不到，尝试通过contentKey反向查找
                    neededImages.firstNotNullOfOrNull { origName ->
                        val f = File(workImagesFolder, origName)
                        if (f.exists() && ImageResourceManager.calculateQuickHash(f) == contentKey) f else null
                    }
                }
                
                if (sourceFile != null && sourceFile.exists()) {
                    zipOut.putNextEntry(ZipEntry("$IMAGES_FOLDER_NAME/$newName"))
                    FileInputStream(sourceFile).use { input ->
                        input.copyTo(zipOut)
                    }
                    zipOut.closeEntry()
                    exportedImages++
                }
            }

            // 写入小图标到ZIP，按内容去重
            var exportedBadges = 0
            badgeContentMap.forEach { (contentKey, newName) ->
                // 通过 badgeNameMap 查找映射到该 newName 的原始文件
                // badgeNameMap: 原始文件名 -> 去重后的文件名
                val originalNames = badgeNameMap.entries.filter { it.value == newName }.map { it.key }
                
                // 查找第一个存在的原始文件
                val sourceFile = originalNames.firstNotNullOfOrNull { 
                    File(workBadgesFolder, it).takeIf { f -> f.exists() } 
                } ?: run {
                    // 如果通过映射找不到，尝试通过contentKey反向查找
                    neededBadges.firstNotNullOfOrNull { origName ->
                        val f = File(workBadgesFolder, origName)
                        if (f.exists() && ImageResourceManager.calculateQuickHash(f) == contentKey) f else null
                    }
                }
                
                if (sourceFile != null && sourceFile.exists()) {
                    zipOut.putNextEntry(ZipEntry("$BADGES_FOLDER_NAME/$newName"))
                    FileInputStream(sourceFile).use { input ->
                        input.copyTo(zipOut)
                    }
                    zipOut.closeEntry()
                    exportedBadges++
                }
            }

            // 写入解说音频到ZIP（保持原文件名）
            var exportedAudio = 0
            neededAudio.forEach { fileName ->
                val sourceFile = File(workAudioFolder, fileName)
                if (sourceFile.exists()) {
                    zipOut.putNextEntry(ZipEntry("$AUDIO_FOLDER_NAME/$fileName"))
                    FileInputStream(sourceFile).use { input ->
                        input.copyTo(zipOut)
                    }
                    zipOut.closeEntry()
                    exportedAudio++
                }
            }
        }

        // 注意: 不要重命名工作目录中的文件
        // 因为 createPresetData 每次都会根据URI生成新的随机文件名
        // 重命名会导致后续保存时文件名不一致
        
        // 返回更新后的预设数据
        return updatedPresetData
    }

    /**
     * 读取草稿配置（不解压图片，用于显示恢复对话框）
     * @return 预设数据，如果草稿不存在则返回null
     */
    suspend fun readDraftConfig(): PresetData? = withContext(Dispatchers.IO) {
        if (!draftFile.exists()) {
            return@withContext null
        }

        try {
            // 只读取配置文件，不解压图片
            val presetData = readPresetConfig(draftFile)
            presetData
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 加载草稿文件
     * @return 草稿文件，如果草稿不存在则返回null
     */
    fun obtainDraftFile(): File? {
        return if (draftFile.exists()) draftFile else null
    }

    /**
     * 从预设文件加载到工作目录（不解压到临时文件夹）
     * @param presetFile 预设文件
     * @return 预设数据
     */
    private fun loadPresetToWorkDir(presetFile: File): PresetData {
        var presetData: PresetData? = null
        var imageCount = 0
        var badgeCount = 0

        // 文件名映射表：旧文件名 -> 新文件名（WebP格式）
        val fileNameMapping = mutableMapOf<String, String>()

        ZipInputStream(FileInputStream(presetFile)).use { zipIn ->
            var entry: ZipEntry?
            while (zipIn.nextEntry.also { entry = it } != null) {
                val entryName = entry!!.name

                when {
                    entryName == CONFIG_FILE_NAME -> {
                        // 读取配置文件
                        val jsonString = zipIn.bufferedReader().readText()
                        presetData = jsonToPresetData(JSONObject(jsonString))
                    }
                    entryName.startsWith("$IMAGES_FOLDER_NAME/") -> {
                        // 解压图片到临时文件，然后转换为WebP
                        val fileName = entryName.substringAfterLast("/")
                        val tempFile = File(context.cacheDir, "preset_temp_${System.currentTimeMillis()}_$fileName")
                        tempFile.outputStream().use { output ->
                            zipIn.copyTo(output)
                        }

                        // 转换为WebP格式保存
                        val baseName = fileName.substringBeforeLast(".")
                        val webpName = "${baseName}.webp"
                        val outputFile = File(workImagesDir, "$IMAGES_FOLDER_NAME/$webpName")
                        outputFile.parentFile?.mkdirs()

                        val conversionResult = WebPConverter.convertToWebP(tempFile, outputFile)
                        if (conversionResult.success) {
                            // 转换成功，记录文件名映射
                            fileNameMapping[fileName] = webpName
                        } else {
                            // WebP转换失败，回退到原格式
                            val fallbackFile = File(workImagesDir, "$IMAGES_FOLDER_NAME/$fileName")
                            tempFile.copyTo(fallbackFile, overwrite = true)
                            fileNameMapping[fileName] = fileName
                        }
                        tempFile.delete()
                        imageCount++
                    }
                    entryName.startsWith("$BADGES_FOLDER_NAME/") -> {
                        // 解压小图标到临时文件，然后转换为WebP
                        val fileName = entryName.substringAfterLast("/")
                        val tempFile = File(context.cacheDir, "preset_temp_${System.currentTimeMillis()}_$fileName")
                        tempFile.outputStream().use { output ->
                            zipIn.copyTo(output)
                        }

                        // 转换为WebP格式保存
                        val baseName = fileName.substringBeforeLast(".")
                        val webpName = "${baseName}.webp"
                        val outputFile = File(workImagesDir, "$BADGES_FOLDER_NAME/$webpName")
                        outputFile.parentFile?.mkdirs()

                        val conversionResult = WebPConverter.convertToWebP(tempFile, outputFile)
                        if (conversionResult.success) {
                            // 转换成功，记录文件名映射
                            fileNameMapping[fileName] = webpName
                        } else {
                            // WebP转换失败，回退到原格式
                            val fallbackFile = File(workImagesDir, "$BADGES_FOLDER_NAME/$fileName")
                            tempFile.copyTo(fallbackFile, overwrite = true)
                            fileNameMapping[fileName] = fileName
                        }
                        tempFile.delete()
                        badgeCount++
                    }
                    entryName.startsWith("$AUDIO_FOLDER_NAME/") -> {
                        // 解压音频文件到工作目录，保持原格式
                        val fileName = entryName.substringAfterLast("/")
                        val outputFile = File(workImagesDir, "$AUDIO_FOLDER_NAME/$fileName")
                        outputFile.parentFile?.mkdirs()
                        outputFile.outputStream().use { output ->
                            zipIn.copyTo(output)
                        }
                        fileNameMapping[fileName] = fileName
                    }
                }
                zipIn.closeEntry()
            }
        }

        val data = presetData
            ?: throw IllegalStateException("预设文件损坏: 无法读取配置")

        // 更新预设数据中的文件名引用
        val updatedTierImages = data.tierImages.map { imageData ->
            imageData.copy(
                imageFileName = fileNameMapping[imageData.imageFileName] ?: imageData.imageFileName,
                badgeFileName1 = imageData.badgeFileName1?.let { fileNameMapping[it] ?: it },
                badgeFileName2 = imageData.badgeFileName2?.let { fileNameMapping[it] ?: it },
                badgeFileName3 = imageData.badgeFileName3?.let { fileNameMapping[it] ?: it },
                audioFileName = imageData.audioFileName?.let { fileNameMapping[it] ?: it }
            )
        }

        val updatedPendingImages = data.pendingImages.map { fileName ->
            fileNameMapping[fileName] ?: fileName
        }

        // 创建更新后的预设数据
        val updatedPresetData = data.copy(
            tierImages = updatedTierImages,
            pendingImages = updatedPendingImages
        )

        // 统计配置文件中引用的图片数量
        val neededImages = mutableSetOf<String>()
        val neededBadges = mutableSetOf<String>()
        updatedPresetData.tierImages.forEach { imageData ->
            neededImages.add(imageData.imageFileName)
            imageData.badgeFileName1?.let { neededBadges.add(it) }
            imageData.badgeFileName2?.let { neededBadges.add(it) }
            imageData.badgeFileName3?.let { neededBadges.add(it) }
        }
        updatedPresetData.pendingImages.forEach { neededImages.add(it) }

        return updatedPresetData
    }

    /**
     * 清理草稿（单文件模式）
     * 仅删除草稿文件，不清理工作目录（工作目录存放当前会话的有效图片）
     */
    fun cleanupDraft() {
        if (draftFile.exists()) {
            draftFile.delete()
        }
    }

    /**
     * 仅清理草稿文件，不清理工作目录
     * 用于外部导入预设成功后，避免删除刚解压的图片
     */
    fun cleanupDraftOnly() {
        if (draftFile.exists()) {
            draftFile.delete()
        }
    }

    // ==================== 预设应用功能 ====================

    /**
     * 应用预设结果数据类
     */
    data class ApplyPresetResult(
        val title: String,
        val author: String,
        val tiers: List<TierItemData>,
        val tierImages: List<AppliedTierImage>,
        val pendingImages: List<Uri>,
        val cropPositionX: Float,
        val cropPositionY: Float,
        val customCropWidth: Int,
        val customCropHeight: Int,
        val useCustomCropSize: Boolean,
        val cropRatio: Float = 1f, // 裁剪比例
        val videoConfig: VideoGenerationConfig = VideoGenerationConfig(),
        val stats: ApplyStats
    )

    /**
     * 已应用的层级图片数据
     */
    data class AppliedTierImage(
        val id: String,
        val tierLabel: String,
        val uri: Uri,
        val name: String = "",
        val badgeUri: Uri? = null,
        val badgeUri2: Uri? = null,
        val badgeUri3: Uri? = null,
        val audioUri: Uri? = null,
        // 裁剪状态字段
        val cropPositionX: Float = 0.5f,
        val cropPositionY: Float = 0.5f,
        val cropScale: Float = 1.0f,
        val isCropped: Boolean = false,
        val originalUri: Uri? = null,  // 原图URI
        val cropRatio: Float = 1f,  // 裁剪比例
        val useCustomCrop: Boolean = false,
        val customCropWidth: Int = 0,
        val customCropHeight: Int = 0
    )

    /**
     * 应用统计信息
     */
    data class ApplyStats(
        val copiedTierImages: Int,
        val skippedTierImages: Int,
        val copiedBadges: Int,
        val skippedBadges: Int,
        val copiedPendingImages: Int,
        val skippedPendingImages: Int
    )

    /**
     * 应用预设数据
     * 简化逻辑：清理所有资源后直接解压预设到工作目录
     * @param presetFile 预设文件
     * @param isDraft 是否为草稿恢复（草稿不清理环境）
     * @return 应用结果，包含转换后的数据和统计信息
     */
    fun applyPreset(presetFile: File, isDraft: Boolean = false): ApplyPresetResult {

        // 1. 清理所有资源（草稿不需要清理，因为编辑页面为空）
        if (!isDraft) {
            cleanupAllCache()
        }

        // 2. 直接解压预设到工作目录
        val presetData = loadPresetToWorkDir(presetFile)

        // 3. 构建结果数据（图片已经在工作目录中）
        val appliedTierImages = mutableListOf<AppliedTierImage>()
        var validTierImages = 0
        var missingTierImages = 0
        var validBadges = 0
        var missingBadges = 0
        var croppedCount = 0  // 裁剪计数

        presetData.tierImages.forEach { imageData ->
            // 检查主图片是否存在
            val imageFile = File(workImagesDir, "$IMAGES_FOLDER_NAME/${imageData.imageFileName}")
            if (!imageFile.exists()) {
                missingTierImages++
                return@forEach
            }
            val originalUri = Uri.fromFile(imageFile)
            validTierImages++

            // 检查是否有裁剪数据: 有自定义尺寸,或有cropRatio(包括1:1),就执行裁剪
            val hasCustomSize = imageData.customCropWidth > 0 && imageData.customCropHeight > 0
            val hasCropRatio = imageData.cropRatio > 0
            val shouldCrop = hasCustomSize || hasCropRatio
            
            val displayUri = if (shouldCrop) {
                // 计算裁剪比例: 优先使用自定义尺寸,其次使用cropRatio
                val imageCropRatio = if (hasCustomSize) {
                    imageData.customCropWidth.toFloat() / imageData.customCropHeight.toFloat()
                } else {
                    imageData.cropRatio
                }
                // 执行裁剪
                val croppedFile = File(workImagesDir, "$IMAGES_FOLDER_NAME/cropped_${imageData.id}_${System.currentTimeMillis()}.webp")
                val success = cropImageWithState(
                    originalUri,
                    imageData.cropPositionX,
                    imageData.cropPositionY,
                    imageData.cropScale,
                    imageCropRatio,
                    imageData.customCropWidth,
                    imageData.customCropHeight,
                    croppedFile,
                    logOutput = false  // 批量裁剪时不输出单张日志
                )
                if (success) {
                    croppedCount++
                    Uri.fromFile(croppedFile)
                } else {
                    originalUri
                }
            } else {
                // 没有裁剪数据,使用原图
                originalUri
            }

            // 检查小图标是否存在
            val badgeUri1 = imageData.badgeFileName1?.let {
                val badgeFile = File(workImagesDir, "$BADGES_FOLDER_NAME/$it")
                if (badgeFile.exists()) {
                    validBadges++
                    Uri.fromFile(badgeFile)
                } else {
                    missingBadges++
                    null
                }
            }
            val badgeUri2 = imageData.badgeFileName2?.let {
                val badgeFile = File(workImagesDir, "$BADGES_FOLDER_NAME/$it")
                if (badgeFile.exists()) {
                    validBadges++
                    Uri.fromFile(badgeFile)
                } else {
                    missingBadges++
                    null
                }
            }
            val badgeUri3 = imageData.badgeFileName3?.let {
                val badgeFile = File(workImagesDir, "$BADGES_FOLDER_NAME/$it")
                if (badgeFile.exists()) {
                    validBadges++
                    Uri.fromFile(badgeFile)
                } else {
                    missingBadges++
                    null
                }
            }

            val audioUri = imageData.audioFileName?.let {
                val audioFile = File(workImagesDir, "$AUDIO_FOLDER_NAME/$it")
                if (audioFile.exists()) Uri.fromFile(audioFile) else null
            }

            appliedTierImages.add(AppliedTierImage(
                id = imageData.id,
                tierLabel = imageData.tierLabel,
                uri = displayUri,  // 显示裁剪后的图片
                name = imageData.name,
                badgeUri = badgeUri1,
                badgeUri2 = badgeUri2,
                badgeUri3 = badgeUri3,
                audioUri = audioUri,
                // 恢复裁剪状态
                cropPositionX = imageData.cropPositionX,
                cropPositionY = imageData.cropPositionY,
                cropScale = imageData.cropScale,
                isCropped = imageData.isCropped,
                originalUri = originalUri,  // 原图URI用于重新裁剪
                // 恢复裁剪比例
                cropRatio = imageData.cropRatio,
                useCustomCrop = imageData.useCustomCrop,
                customCropWidth = imageData.customCropWidth,
                customCropHeight = imageData.customCropHeight
            ))
        }

        // 处理待分级图片
        val pendingImageUris = mutableListOf<Uri>()
        var validPendingImages = 0
        var missingPendingImages = 0

        presetData.pendingImages.forEach { fileName ->
            val imageFile = File(workImagesDir, "$IMAGES_FOLDER_NAME/$fileName")
            if (imageFile.exists()) {
                pendingImageUris.add(Uri.fromFile(imageFile))
                validPendingImages++
            } else {
                missingPendingImages++
            }
        }

        return ApplyPresetResult(
            title = presetData.title,
            author = presetData.author,
            tiers = presetData.tiers,
            tierImages = appliedTierImages,
            pendingImages = pendingImageUris,
            cropPositionX = presetData.cropPositionX,
            cropPositionY = presetData.cropPositionY,
            customCropWidth = presetData.customCropWidth,
            customCropHeight = presetData.customCropHeight,
            useCustomCropSize = presetData.useCustomCropSize,
            cropRatio = presetData.cropRatio,
            videoConfig = presetData.videoConfig,
            stats = ApplyStats(
                copiedTierImages = validTierImages,
                skippedTierImages = missingTierImages,
                copiedBadges = validBadges,
                skippedBadges = missingBadges,
                copiedPendingImages = validPendingImages,
                skippedPendingImages = missingPendingImages
            )
        )
    }

    /**
     * 恢复草稿数据（使用applyPreset，isDraft=true）
     * @param presetFile 草稿文件
     * @return 应用结果
     */
    fun restoreDraft(presetFile: File): ApplyPresetResult {
        return applyPreset(presetFile, isDraft = true)
    }

    // ==================== 预设数据创建功能 ====================

    /**
     * 创建预设数据
     * 注意：此函数只收集文件名信息，不进行任何文件复制操作
     * 文件应该已经在工作目录中（应用预设时解压的或用户添加的）
     * @param title 标题
     * @param author 作者
     * @param tiers 层级列表
     * @param tierImages 层级图片列表
     * @param pendingImages 待分级图片URI列表
     * @param cropPositionX 裁剪位置X
     * @param cropPositionY 裁剪位置Y
     * @param customCropWidth 自定义裁剪宽度
     * @param customCropHeight 自定义裁剪高度
     * @param useCustomCropSize 是否使用自定义裁剪尺寸
     * @return 预设数据
     */
    fun createPresetData(
        title: String,
        author: String,
        tiers: List<TierItem>,
        tierImages: List<TierImage>,
        pendingImages: List<Uri>,
        cropPositionX: Float,
        cropPositionY: Float,
        customCropWidth: Int,
        customCropHeight: Int,
        useCustomCropSize: Boolean,
        cropRatio: Float = 1f,
        videoConfig: VideoGenerationConfig = VideoGenerationConfig()
    ): PresetData {
        val tierImagesData = mutableListOf<TierImageData>()
        val pendingImagesNames = mutableListOf<String>()

        // 处理层级图片 - 直接从URI提取文件名，不复制文件
        // 保存原图文件名和裁剪状态
        tierImages.forEach { tierImage ->
            // 优先使用原图URI（如果有），否则使用当前URI
            val uriToSave = tierImage.originalUri ?: tierImage.uri
            val imageFileName = extractFileNameFromUri(uriToSave)

            // 提取小图标文件名
            val badgeFileName1 = tierImage.badgeUri?.let { extractFileNameFromUri(it) }
            val badgeFileName2 = tierImage.badgeUri2?.let { extractFileNameFromUri(it) }
            val badgeFileName3 = tierImage.badgeUri3?.let { extractFileNameFromUri(it) }
            val audioFileName = tierImage.audioUri?.let { extractFileNameFromUri(it) }

            if (imageFileName != null) {
                tierImagesData.add(TierImageData(
                    id = tierImage.id,
                    tierLabel = tierImage.tierLabel,
                    imageFileName = imageFileName,
                    name = tierImage.name,
                    badgeFileName1 = badgeFileName1,
                    badgeFileName2 = badgeFileName2,
                    badgeFileName3 = badgeFileName3,
                    audioFileName = audioFileName,
                    // 保存裁剪状态
                    cropPositionX = tierImage.cropPositionX,
                    cropPositionY = tierImage.cropPositionY,
                    cropScale = tierImage.cropScale,
                    isCropped = tierImage.isCropped,
                    // 保存裁剪比例
                    cropRatio = tierImage.cropRatio,
                    useCustomCrop = tierImage.useCustomCrop,
                    customCropWidth = tierImage.customCropWidth,
                    customCropHeight = tierImage.customCropHeight
                ))
            }
        }

        // 处理待分级图片 - 直接提取文件名
        pendingImages.forEach { uri ->
            val fileName = extractFileNameFromUri(uri)
            if (fileName != null) {
                pendingImagesNames.add(fileName)
            }
        }

        return PresetData(
            title = title,
            author = author,
            tiers = tiers.map { TierItemData(it.label, String.format("%06X", it.color.toArgb() and 0xFFFFFF)) },
            tierImages = tierImagesData,
            pendingImages = pendingImagesNames,
            cropPositionX = cropPositionX,
            cropPositionY = cropPositionY,
            customCropWidth = customCropWidth,
            customCropHeight = customCropHeight,
            useCustomCropSize = useCustomCropSize,
            cropRatio = cropRatio,
            videoConfig = videoConfig
        )
    }

    /**
     * 根据裁剪状态裁剪图片
     * 与ImageCropDialog中的裁剪逻辑保持一致
     * @param sourceUri 原图URI
     * @param cropPositionX 裁剪框水平位置 (0.0f = 左, 1.0f = 右)
     * @param cropPositionY 裁剪框垂直位置 (0.0f = 上, 1.0f = 下)
     * @param cropScale 裁剪缩放比例 (未使用,保留用于兼容性)
     * @param aspectRatio 裁剪宽高比 (1f = 1:1, 0.75f = 3:4, 1.33f = 4:3)
     * @param outputFile 输出文件
     * @param logOutput 是否输出日志（批量裁剪时设为false以避免日志过多）
     * @return 是否成功
     */
    private fun cropImageWithState(
        sourceUri: Uri,
        cropPositionX: Float,
        cropPositionY: Float,
        cropScale: Float,
        aspectRatio: Float = 1f,
        customCropWidth: Int = 0,
        customCropHeight: Int = 0,
        outputFile: File,
        logOutput: Boolean = true
    ): Boolean {
        return try {
            // 加载原图
            val sourceBitmap = context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: return false

            val imageWidth = sourceBitmap.width
            val imageHeight = sourceBitmap.height

            // 计算裁切区域
            val cropWidth: Int
            val cropHeight: Int
            val hasCustomSize = customCropWidth > 0 && customCropHeight > 0
            
            if (hasCustomSize) {
                // 使用自定义裁切框大小
                cropWidth = customCropWidth.coerceIn(1, imageWidth)
                cropHeight = customCropHeight.coerceIn(1, imageHeight)
            } else {
                // 根据传入的宽高比计算
                if (imageWidth.toFloat() / imageHeight > aspectRatio) {
                    cropHeight = imageHeight
                    cropWidth = (imageHeight * aspectRatio).toInt()
                } else {
                    cropWidth = imageWidth
                    cropHeight = (imageWidth / aspectRatio).toInt()
                }
            }

            // 根据位置计算偏移
            val maxXOffset = imageWidth - cropWidth
            val maxYOffset = imageHeight - cropHeight
            val xOffset = (maxXOffset * cropPositionX).toInt()
            val yOffset = (maxYOffset * cropPositionY).toInt()

            // 执行裁切
            val croppedBitmap = Bitmap.createBitmap(
                sourceBitmap,
                xOffset,
                yOffset,
                cropWidth,
                cropHeight
            )

            // 保存裁剪后的图片
            FileOutputStream(outputFile).use { out ->
                croppedBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 100, out)
            }

            // 清理资源
            sourceBitmap.recycle()
            croppedBitmap.recycle()


            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 从URI中提取文件名
     * 支持file://和content://两种URI格式
     * @param uri 文件URI
     * @return 文件名，如果无法提取则返回null
     */
    private fun extractFileNameFromUri(uri: Uri): String? {
        return when (uri.scheme) {
            "file" -> {
                // file:// URI，直接提取路径中的文件名
                File(uri.path!!).name
            }
            "content" -> {
                // content:// URI，从lastPathSegment提取
                uri.lastPathSegment?.substringAfterLast('/')
            }
            else -> {
                // 其他情况，尝试使用lastPathSegment
                uri.lastPathSegment
            }
        }
    }
}

/**
 * ZIP需要密码的自定义异常
 */
class ZipPasswordRequiredException(message: String) : Exception(message)
