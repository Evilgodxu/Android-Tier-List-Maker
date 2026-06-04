package com.tdds.jh.data.resource

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.tdds.jh.data.manager.PresetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 资源使用详情
 */
data class ResourceUsageDetail(
    val cacheSize: Long = 0L,
    val workImagesSize: Long = 0L,
    val importedPackagesSize: Long = 0L,
    val draftsSize: Long = 0L,
    val presetsSize: Long = 0L,
    val totalSize: Long = 0L
)

/**
 * 资源管理器
 * 负责管理应用资源相关的操作，包括存储计算、缓存清理、图包管理等
 */
object ResourceManager {

    /**
     * 计算缓存大小（包含缓存目录、日志文件、临时ZIP目录和孤立文件）
     */
    fun calculateCacheSize(context: Context): Long {
        var size = 0L

        // 1. 缓存目录
        context.cacheDir.listFiles()?.forEach { file ->
            size += if (file.isFile) file.length() else getFolderSize(file)
        }

        // 2. 日志文件目录
        val logsDir = File(context.getExternalFilesDir(null)?.parentFile, "log")
        if (logsDir.exists()) {
            size += getFolderSize(logsDir)
        }

        // 3. 临时ZIP解压目录（zip_temp_*, imported_temp_*, zip_check_*）
        context.cacheDir.listFiles()
            ?.filter { file ->
                file.isDirectory && (
                    file.name.startsWith("zip_temp_") ||
                    file.name.startsWith("imported_temp_") ||
                    file.name.startsWith("zip_check_")
                )
            }
            ?.forEach { size += getFolderSize(it) }

        // 4. filesDir 根目录下的孤立文件（非预设文件）
        context.filesDir.listFiles()
            ?.filter { file ->
                file.isFile && !file.name.endsWith(".tdds")
            }
            ?.forEach { size += it.length() }

        return size
    }

    /**
     * 计算所有临时文件大小（包括缓存目录和files目录中的非持久化数据）
     */
    fun calculateTotalTempSize(context: Context, presetManager: PresetManager): Long {
        val details = calculateResourceDetails(context, presetManager)
        return details.totalSize
    }

    /**
     * 计算资源使用详情
     */
    fun calculateResourceDetails(context: Context, presetManager: PresetManager): ResourceUsageDetail {
        // 1. 缓存文件（包含缓存目录、日志文件、临时ZIP目录和孤立文件）
        val cacheSize = calculateCacheSize(context)

        // 2. 工作目录
        val workDir = presetManager.getWorkImagesDirectory()
        val workImagesSize = if (workDir.exists()) getFolderSize(workDir) else 0L

        // 3. 导入的图包
        val packagesDir = presetManager.getPackagesDirectory()
        val importedPackagesSize = if (packagesDir.exists()) getFolderSize(packagesDir) else 0L

        // 4. 草稿文件（使用 Draft 文件夹名，与 PresetManager 保持一致）
        val draftsDir = File(context.filesDir, "Draft")
        val draftsSize = if (draftsDir.exists()) getFolderSize(draftsDir) else 0L

        // 5. 预设文件（使用 Presets 文件夹名，与 PresetManager 保持一致）
        val presetsDir = File(context.filesDir, "Presets")
        val presetsSize = if (presetsDir.exists()) getFolderSize(presetsDir) else 0L

        return ResourceUsageDetail(
            cacheSize = cacheSize,
            workImagesSize = workImagesSize,
            importedPackagesSize = importedPackagesSize,
            draftsSize = draftsSize,
            presetsSize = presetsSize,
            totalSize = cacheSize + workImagesSize + importedPackagesSize + draftsSize + presetsSize
        )
    }

    /**
     * 获取文件夹大小
     */
    fun getFolderSize(folder: File): Long {
        var size = 0L
        folder.listFiles()?.forEach { file ->
            size += if (file.isFile) file.length() else getFolderSize(file)
        }
        return size
    }

    /**
     * 格式化文件大小
     */
    fun formatFileSize(size: Long): String {
        return when {
            size >= 1024 * 1024 * 1024 -> String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0))
            size >= 1024 * 1024 -> String.format("%.2f MB", size / (1024.0 * 1024.0))
            size >= 1024 -> String.format("%.2f KB", size / 1024.0)
            else -> "$size B"
        }
    }

    /**
     * 清理资源（不包含导入的图包和预设文件）
     * @param context 上下文
     * @param presetManager 预设管理器
     * @param onResettiermaster 重置模板回调
     * @param onComplete 完成回调
     */
    suspend fun cleanupResources(
        context: Context,
        presetManager: PresetManager,
        onResettiermaster: () -> Unit,
        onComplete: () -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            // 1. 清理工作目录
            presetManager.cleanupWorkImages()
            // 2. 清理缓存目录
            context.cacheDir.deleteRecursively()
            // 3. 清理草稿文件（使用 Draft 文件夹名，与 PresetManager 保持一致）
            val draftsDir = File(context.filesDir, "Draft")
            if (draftsDir.exists()) {
                draftsDir.deleteRecursively()
            }
            // 4. 清理临时ZIP解压目录
            cleanupTempZipDirs(context)
            // 5. 清理孤立文件
            cleanupOrphanedFiles(context)
            // 6. 重置为默认模板
            onResettiermaster()
            onComplete()
        } catch (e: Exception) {
            throw e
        }
    }

    /**
     * 清理临时ZIP解压目录
     */
    private fun cleanupTempZipDirs(context: Context) {
        var deletedCount = 0
        context.cacheDir.listFiles()?.forEach { file ->
            if (file.isDirectory && (
                file.name.startsWith("zip_temp_") ||
                file.name.startsWith("imported_temp_") ||
                file.name.startsWith("zip_check_")
            )) {
                file.deleteRecursively()
                deletedCount++
            }
        }
    }

    /**
     * 清理 filesDir 根目录下的孤立文件
     */
    private fun cleanupOrphanedFiles(context: Context) {
        var deletedCount = 0
        context.filesDir.listFiles()?.forEach { file ->
            if (file.isFile && !file.name.endsWith(".tdds")) {
                file.delete()
                deletedCount++
            }
        }
    }

    /**
     * 清理日志文件（保留最近指定天数的日志）
     * @param context 上下文
     * @param keepDays 保留天数，默认7天
     */
    fun cleanupLogFiles(context: Context, keepDays: Int = 7) {
        val logsDir = File(context.getExternalFilesDir(null)?.parentFile, "log")
        if (!logsDir.exists()) return

        val cutoffTime = System.currentTimeMillis() - keepDays * 24 * 60 * 60 * 1000
        var deletedCount = 0

        logsDir.listFiles()?.forEach { file ->
            if (file.isFile && file.lastModified() < cutoffTime) {
                file.delete()
                deletedCount++
            }
        }

    }

    /**
     * 资源管理状态
     */
    data class ResourceState(
        val details: ResourceUsageDetail = ResourceUsageDetail(),
        val isCalculating: Boolean = false,
        val isCleaning: Boolean = false
    ) {
        val totalTempSize: Long get() = details.totalSize
    }

    /**
     * 创建资源管理状态
     */
    @Composable
    fun rememberResourceState(
        presetManager: PresetManager
    ): Pair<ResourceState, ResourceActions> {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        var details by remember { mutableStateOf(ResourceUsageDetail()) }
        var isCleaning by remember { mutableStateOf(false) }
        var isCalculating by remember { mutableStateOf(true) }

        // 计算存储使用情况
        fun calculateSize() {
            scope.launch {
                isCalculating = true
                withContext(Dispatchers.IO) {
                    details = calculateResourceDetails(context, presetManager)
                }
                isCalculating = false
            }
        }

        // 初始计算
        androidx.compose.runtime.LaunchedEffect(Unit) {
            calculateSize()
        }

        val state = ResourceState(
            details = details,
            isCalculating = isCalculating,
            isCleaning = isCleaning
        )

        val actions = ResourceActions(
            refreshSize = { calculateSize() },
            cleanup = { onResettiermaster: () -> Unit, onComplete: () -> Unit ->
                scope.launch {
                    isCleaning = true
                    try {
                        cleanupResources(
                            context = context,
                            presetManager = presetManager,
                            onResettiermaster = onResettiermaster,
                            onComplete = onComplete
                        )
                    } catch (_: Exception) {
                    }
                    // 重新计算大小
                    withContext(Dispatchers.IO) {
                        details = calculateResourceDetails(context, presetManager)
                    }
                    isCleaning = false
                }
            }
        )

        return state to actions
    }

    /**
     * 资源管理操作
     */
    data class ResourceActions(
        val refreshSize: () -> Unit,
        val cleanup: (
            onResettiermaster: () -> Unit,
            onComplete: () -> Unit
        ) -> Unit
    )
}
