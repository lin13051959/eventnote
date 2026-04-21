package com.example.eventnote.data

import android.content.Context
import android.net.Uri
import com.example.eventnote.EventNoteRepository
import com.example.eventnote.data.local.entity.Event
import com.example.eventnote.data.local.entity.EventStatus
import com.example.eventnote.data.local.entity.MediaItem
import com.example.eventnote.data.local.entity.MediaType
import com.example.eventnote.data.local.entity.Priority
import com.example.eventnote.data.local.entity.ReminderType
import com.example.eventnote.util.MediaManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 数据导出/导入管理器
 * 格式: TXT 文件，每行一个事件，字段用分隔符 | 分隔
 */
@Singleton
class DataManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: EventNoteRepository,
    private val mediaManager: MediaManager
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    companion object {
        // TXT 格式版本 (2.0 支持多媒体)
        const val FORMAT_VERSION = "2.0"
        // 分隔符
        const val DELIMITER = "|"
        // 多媒体分隔符
        const val MEDIA_DELIMITER = ";;"
        // 文件扩展名
        const val FILE_EXTENSION = ".txt"
        // 文件名前缀
        const val FILE_PREFIX = "eventnote_backup_"
        // 媒体文件目录名
        const val MEDIA_EXPORT_DIR = "media"
    }
    
    /**
     * 导出所有事件到 TXT 文件
     * 格式: 版本|ID|标题|内容|标签|媒体|优先级|状态|提醒时间|提醒类型|铃声|震动|创建时间|更新时间
     * 媒体格式: id|uri|type|name|mimeType|size|duration|createdAt;;id|uri|type|name|...
     */
    suspend fun exportToTxt(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val events = repository.getAllEvents().first()
            
            if (events.isEmpty()) {
                return@withContext Result.success(0)
            }
            
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    // 写入文件头
                    writer.write("# EventNote Backup Format v$FORMAT_VERSION\n")
                    writer.write("# Exported: ${dateFormat.format(Date())}\n")
                    writer.write("# Count: ${events.size}\n")
                    writer.write("# ========================================\n")
                    
                    // 写入每个事件
                    events.forEach { event ->
                        val mediaStr = serializeMediaItems(event.mediaItems)
                        
                        val line = buildString {
                            append(FORMAT_VERSION).append(DELIMITER)
                            append(event.id).append(DELIMITER)
                            append(escape(event.title)).append(DELIMITER)
                            append(escape(event.content)).append(DELIMITER)
                            append(escape(event.tags.joinToString(","))).append(DELIMITER)
                            append(escape(mediaStr)).append(DELIMITER)
                            append(event.priority.name).append(DELIMITER)
                            append(event.status.name).append(DELIMITER)
                            append(event.reminderTime ?: "").append(DELIMITER)
                            append(event.reminderType.name).append(DELIMITER)
                            append(if (event.reminderSound) "1" else "0").append(DELIMITER)
                            append(if (event.reminderVibration) "1" else "0").append(DELIMITER)
                            append(event.createdAt).append(DELIMITER)
                            append(event.updatedAt)
                        }
                        writer.write(line)
                        writer.write("\n")
                    }
                }
            }
            
            Result.success(events.size)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     * 从 TXT 文件导入事件
     */
    suspend fun importFromTxt(uri: Uri): Result<ImportResult> = withContext(Dispatchers.IO) {
        try {
            var successCount = 0
            var failCount = 0
            val errors = mutableListOf<String>()
            
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.lineSequence().forEachIndexed { index, line ->
                        // 跳过注释行和空行
                        if (line.startsWith("#") || line.isBlank()) {
                            return@forEachIndexed
                        }
                        
                        try {
                            val parts = line.split(DELIMITER)
                            if (parts.size < 13) {
                                failCount++
                                errors.add("行 ${index + 1}: 字段数量不足")
                                return@forEachIndexed
                            }
                            
                            // 解析媒体数据
                            val mediaItems = if (parts[5].isNotEmpty()) {
                                parseMediaItems(parts[5])
                            } else {
                                emptyList()
                            }
                            
                            val event = Event(
                                id = 0, // 重新生成 ID
                                title = unescape(parts[2]),
                                content = unescape(parts[3]),
                                tags = if (parts[4].isNotEmpty()) parts[4].split(",") else emptyList(),
                                mediaItems = mediaItems,
                                priority = try { Priority.valueOf(parts[6]) } catch (e: Exception) { Priority.MEDIUM },
                                status = try { EventStatus.valueOf(parts[7]) } catch (e: Exception) { EventStatus.IN_PROGRESS },
                                reminderTime = parts[8].toLongOrNull(),
                                reminderType = try { ReminderType.valueOf(parts[9]) } catch (e: Exception) { ReminderType.NOTIFICATION },
                                reminderSound = parts[10] == "1",
                                reminderVibration = parts[11] == "1",
                                createdAt = parts[12].toLongOrNull() ?: System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
                            
                            repository.insertEvent(event)
                            successCount++
                        } catch (e: Exception) {
                            failCount++
                            errors.add("行 ${index + 1}: ${e.message}")
                        }
                    }
                }
            }
            
            Result.success(ImportResult(successCount, failCount, errors))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     * 序列化媒体列表
     */
    private fun serializeMediaItems(mediaItems: List<MediaItem>): String {
        if (mediaItems.isEmpty()) return ""
        
        return mediaItems.joinToString(MEDIA_DELIMITER) { item ->
            listOf(
                item.id,
                item.uri,
                item.type.name,
                item.name,
                item.mimeType,
                item.size.toString(),
                item.duration?.toString() ?: "",
                item.createdAt.toString()
            ).joinToString("|")
        }
    }
    
    /**
     * 解析媒体列表
     */
    private fun parseMediaItems(data: String): List<MediaItem> {
        if (data.isBlank()) return emptyList()
        
        return try {
            data.split(MEDIA_DELIMITER).mapNotNull { itemStr ->
                val parts = itemStr.split("|")
                if (parts.size < 5) return@mapNotNull null
                
                MediaItem(
                    id = parts.getOrElse(0) { "" },
                    uri = parts.getOrElse(1) { "" },
                    type = try { MediaType.valueOf(parts.getOrElse(2) { "IMAGE" }) } catch (e: Exception) { MediaType.IMAGE },
                    name = parts.getOrElse(3) { "" },
                    mimeType = parts.getOrElse(4) { "" },
                    size = parts.getOrElse(5) { "0" }.toLongOrNull() ?: 0L,
                    duration = parts.getOrElse(6) { "" }.toLongOrNull(),
                    createdAt = parts.getOrElse(7) { "" }.toLongOrNull() ?: System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 生成导出文件名
     */
    fun generateExportFileName(): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return "$FILE_PREFIX${dateFormat.format(Date())}$FILE_EXTENSION"
    }
    
    /**
     * 生成ZIP导出文件名
     */
    fun generateZipExportFileName(): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return "$FILE_PREFIX${dateFormat.format(Date())}.zip"
    }
    
    /**
     * 清除所有数据
     */
    suspend fun clearAllData() = repository.deleteAllEvents()
    
    /**
     * 获取事件数量
     */
    suspend fun getEventCount(): Int = repository.getAllEvents().first().size
    
    /**
     * 导出所有事件到 ZIP 文件
     * 结构: data/events.txt + media/文件夹
     */
    suspend fun exportToZip(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val events = repository.getAllEvents().first()
            
            if (events.isEmpty()) {
                return@withContext Result.success(0)
            }
            
            // 创建临时 ZIP 文件
            val tempZipFile = File(context.cacheDir, "export_${System.currentTimeMillis()}.zip")
            
            // 收集所有媒体文件
            val allMediaFiles = mutableListOf<File>()
            
            ZipOutputStream(tempZipFile.outputStream()).use { zipOut ->
                // 构建事件数据，同时收集媒体文件
                val eventsData = buildString {
                    // 写入文件头
                    append("# EventNote Backup Format v$FORMAT_VERSION\n")
                    append("# Exported: ${dateFormat.format(Date())}\n")
                    append("# Count: ${events.size}\n")
                    append("# ========================================\n")
                    
                    // 写入每个事件
                    events.forEach { event ->
                        // 处理媒体的索引列表
                        val mediaIndices = mutableListOf<Int>()
                        
                        // 尝试保存每个媒体文件
                        event.mediaItems.forEachIndexed { index, mediaItem ->
                            val file = findMediaFile(mediaItem)
                            if (file != null && file.exists()) {
                                val mediaIndex = allMediaFiles.size
                                allMediaFiles.add(file)
                                mediaIndices.add(mediaIndex)
                            }
                        }
                        
                        // 序列化媒体索引列表
                        val mediaIndicesStr = mediaIndices.joinToString(",")
                        
                        val line = buildString {
                            append(FORMAT_VERSION).append(DELIMITER)
                            append(event.id).append(DELIMITER)
                            append(escape(event.title)).append(DELIMITER)
                            append(escape(event.content)).append(DELIMITER)
                            append(escape(event.tags.joinToString(","))).append(DELIMITER)
                            append(escape(mediaIndicesStr)).append(DELIMITER)
                            append(event.priority.name).append(DELIMITER)
                            append(event.status.name).append(DELIMITER)
                            append(event.reminderTime ?: "").append(DELIMITER)
                            append(event.reminderType.name).append(DELIMITER)
                            append(if (event.reminderSound) "1" else "0").append(DELIMITER)
                            append(if (event.reminderVibration) "1" else "0").append(DELIMITER)
                            append(event.createdAt).append(DELIMITER)
                            append(event.updatedAt)
                        }
                        append(line)
                        append("\n")
                    }
                }
                
                // 添加 events.txt 到 ZIP
                zipOut.putNextEntry(ZipEntry("data/events.txt"))
                zipOut.write(eventsData.toByteArray())
                zipOut.closeEntry()
                
                // 添加媒体文件到 ZIP - 使用简单序号命名
                allMediaFiles.forEachIndexed { index, file ->
                    if (file.exists()) {
                        val extension = file.extension.ifEmpty { "bin" }
                        val entryName = "media/$index.$extension"
                        zipOut.putNextEntry(ZipEntry(entryName))
                        file.inputStream().use { input ->
                            input.copyTo(zipOut)
                        }
                        zipOut.closeEntry()
                    }
                }
            }
            
            // 将临时 ZIP 文件内容写入目标 URI
            context.contentResolver.openOutputStream(uri)?.use { output ->
                tempZipFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            }
            
            // 清理临时文件
            tempZipFile.delete()
            
            Result.success(events.size)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     * 查找媒体文件
     */
    private fun findMediaFile(mediaItem: MediaItem): File? {
        val mediaUri = Uri.parse(mediaItem.uri)
        val mediaDir = File(context.filesDir, "event_media")
        
        // 方式1: 从 file:// URI
        if (mediaUri.scheme == "file") {
            val path = mediaUri.path
            if (path != null) {
                val file = File(path)
                if (file.exists()) return file
            }
        }
        
        // 方式2: 从私有目录
        if (mediaDir.exists()) {
            // 尝试 ID 开头匹配
            val file = mediaDir.listFiles()?.find { it.name.startsWith(mediaItem.id) }
            if (file != null) return file
        }
        
        return null
    }
    
    /**
     * 从 ZIP 文件导入事件
     */
    suspend fun importFromZip(uri: Uri): Result<ImportResult> = withContext(Dispatchers.IO) {
        try {
            var successCount = 0
            var failCount = 0
            val errors = mutableListOf<String>()
            
            // 创建临时目录用于解压
            val tempDir = File(context.cacheDir, "import_${System.currentTimeMillis()}")
            tempDir.mkdirs()
            
            try {
                // 解压 ZIP 文件
                context.contentResolver.openInputStream(uri)?.use { input ->
                    ZipInputStream(input).use { zipIn ->
                        var entry = zipIn.nextEntry
                        while (entry != null) {
                            val file = File(tempDir, entry.name)
                            if (entry.isDirectory) {
                                file.mkdirs()
                            } else {
                                file.parentFile?.mkdirs()
                                file.outputStream().use { output ->
                                    zipIn.copyTo(output)
                                }
                            }
                            entry = zipIn.nextEntry
                        }
                    }
                }
                
                // 读取 events.txt
                val eventsFile = File(tempDir, "data/events.txt")
                if (!eventsFile.exists()) {
                    return@withContext Result.failure(Exception("ZIP文件中没有events.txt"))
                }
                
                // 获取所有媒体文件
                val mediaDir = File(tempDir, "media")
                val mediaFiles = if (mediaDir.exists()) {
                    mediaDir.listFiles()?.sortedBy { it.name }?.toList() ?: emptyList()
                } else {
                    emptyList()
                }
                
                eventsFile.readLines().forEachIndexed { index, line ->
                    // 跳过注释行和空行
                    if (line.startsWith("#") || line.isBlank()) {
                        return@forEachIndexed
                    }
                    
                    try {
                        val parts = line.split(DELIMITER)
                        if (parts.size < 13) {
                            failCount++
                            errors.add("行 ${index + 1}: 字段数量不足")
                            return@forEachIndexed
                        }
                        
                        // 解析媒体索引列表 (新格式: "0,1,2")
                        val mediaIndicesStr = parts[5]
                        val mediaItems = if (mediaIndicesStr.isNotEmpty()) {
                            val indices = mediaIndicesStr.split(",").mapNotNull { it.trim().toIntOrNull() }
                            indices.mapNotNull { mediaIndex ->
                                if (mediaIndex < mediaFiles.size) {
                                    val file = mediaFiles[mediaIndex]
                                    if (file.exists()) {
                                        // 复制到应用私有存储
                                        val newUri = mediaManager.copyToPrivateStorage(file)
                                        MediaItem(
                                            id = UUID.randomUUID().toString(),
                                            uri = newUri.toString(),
                                            type = getMediaTypeFromExtension(file.extension),
                                            name = file.name,
                                            mimeType = getMimeTypeFromFileName(file.name),
                                            size = file.length(),
                                            duration = null,
                                            createdAt = System.currentTimeMillis()
                                        )
                                    } else null
                                } else null
                            }
                        } else {
                            emptyList()
                        }
                        
                        val event = Event(
                            id = 0,
                            title = unescape(parts[2]),
                            content = unescape(parts[3]),
                            tags = if (parts[4].isNotEmpty()) parts[4].split(",") else emptyList(),
                            mediaItems = mediaItems,
                            priority = try { Priority.valueOf(parts[6]) } catch (e: Exception) { Priority.MEDIUM },
                            status = try { EventStatus.valueOf(parts[7]) } catch (e: Exception) { EventStatus.IN_PROGRESS },
                            reminderTime = parts[8].toLongOrNull(),
                            reminderType = try { ReminderType.valueOf(parts[9]) } catch (e: Exception) { ReminderType.NOTIFICATION },
                            reminderSound = parts[10] == "1",
                            reminderVibration = parts[11] == "1",
                            createdAt = parts[12].toLongOrNull() ?: System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        
                        repository.insertEvent(event)
                        successCount++
                    } catch (e: Exception) {
                        failCount++
                        errors.add("行 ${index + 1}: ${e.message}")
                    }
                }
            } finally {
                // 清理临时目录
                tempDir.deleteRecursively()
            }
            
            Result.success(ImportResult(successCount, failCount, errors))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     * 从文件扩展名获取媒体类型
     */
    private fun getMediaTypeFromExtension(extension: String): MediaType {
        return when (extension.lowercase()) {
            "jpg", "jpeg", "png", "gif", "webp", "bmp" -> MediaType.IMAGE
            "mp4", "avi", "mkv", "mov", "3gp" -> MediaType.VIDEO
            "mp3", "wav", "ogg", "m4a", "aac" -> MediaType.AUDIO
            else -> MediaType.IMAGE
        }
    }
    
    /**
     * 序列化媒体列表（导出用）- 同时收集文件
     */
    private fun serializeMediaItemsForExport(
        mediaItems: List<MediaItem>,
        fileList: MutableList<Pair<MediaItem, File>>
    ): String {
        android.util.Log.d("DataManager", "导出: 开始序列化 ${mediaItems.size} 个媒体项")
        if (mediaItems.isEmpty()) return ""
        
        return mediaItems.joinToString(MEDIA_DELIMITER) { item ->
            android.util.Log.d("DataManager", "导出: 处理媒体项 ${item.id}, 类型=${item.type}, URI=${item.uri}")
            // 尝试找到对应的文件
            var sourceFile: File? = null
            val mediaUri = Uri.parse(item.uri)
            
            // 首先尝试从私有目录获取（MediaManager 保存的文件格式是 {id}.{extension}）
            val mediaDir = File(context.filesDir, "event_media")
            if (mediaDir.exists()) {
                // 尝试多种方式找到文件
                val extension = getExtensionFromMimeType(item.mimeType)
                val extensionWithDot = if (extension.isNotEmpty()) ".$extension" else ""
                
                // 方式1: {id}.{extension}
                val possibleFile = File(mediaDir, "${item.id}$extensionWithDot")
                if (possibleFile.exists()) {
                    sourceFile = possibleFile
                    android.util.Log.d("DataManager", "导出: 找到文件 ${possibleFile.absolutePath}")
                } else {
                    // 方式2: 直接用 ID 开头匹配
                    sourceFile = mediaDir.listFiles()?.find { it.name.startsWith(item.id) }
                    if (sourceFile != null) {
                        android.util.Log.d("DataManager", "导出: 通过ID匹配找到文件 ${sourceFile.absolutePath}")
                    } else {
                        android.util.Log.d("DataManager", "导出: 未找到文件，ID=${item.id}, 扩展名=$extensionWithDot")
                        android.util.Log.d("DataManager", "导出: 媒体目录内容: ${mediaDir.listFiles()?.joinToString(", ") { it.name }}")
                    }
                }
            } else {
                android.util.Log.d("DataManager", "导出: 媒体目录不存在: ${mediaDir.absolutePath}")
            }
            
            // 如果从私有目录找不到，尝试从 URI 获取
            if (sourceFile == null) {
                android.util.Log.d("DataManager", "导出: 尝试从 URI 获取，scheme=${mediaUri.scheme}")
                if (mediaUri.scheme == "file") {
                    val path = mediaUri.path
                    if (path != null) {
                        val file = File(path)
                        if (file.exists()) {
                            sourceFile = file
                            android.util.Log.d("DataManager", "导出: 从 file URI 找到文件 ${file.absolutePath}")
                        }
                    }
                } else if (mediaUri.scheme == "content") {
                    val mimeType = item.mimeType
                    val ext = getExtensionFromMimeType(mimeType)
                    val tempFile = File(context.cacheDir, "temp_export_${item.id}.$ext")
                    try {
                        context.contentResolver.openInputStream(mediaUri)?.use { input ->
                            tempFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        if (tempFile.exists()) {
                            sourceFile = tempFile
                            android.util.Log.d("DataManager", "导出: 从 content URI 复制到临时文件 ${tempFile.absolutePath}")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("DataManager", "导出: 从 content URI 失败", e)
                    }
                }
            }
            
            // 添加到文件列表
            if (sourceFile != null && sourceFile.exists()) {
                fileList.add(Pair(item, sourceFile))
                android.util.Log.d("DataManager", "导出: 添加到文件列表 ${sourceFile.absolutePath}")
            } else {
                android.util.Log.d("DataManager", "导出: 未找到文件，跳过媒体项 ${item.id}")
            }
            
            // 序列化时也使用 {id}.{extension} 格式
            listOf(
                item.id,
                item.uri,
                item.type.name,
                item.name,  // 原始文件名
                item.mimeType,
                item.size.toString(),
                item.duration?.toString() ?: "",
                item.createdAt.toString()
            ).joinToString("|")
        }
    }
    
    /**
     * 解析媒体数据并复制到应用私有存储（精确匹配）
     */
    private fun parseMediaItemsAndCopy(data: String, tempDir: File): List<MediaItem> {
        if (data.isBlank()) return emptyList()
        
        val mediaDir = File(tempDir, "media")
        if (!mediaDir.exists()) {
            android.util.Log.d("DataManager", "导入: 媒体目录不存在: ${mediaDir.absolutePath}")
            return emptyList()
        }
        
        // 获取所有媒体文件
        val mediaFiles = mediaDir.listFiles()?.toList() ?: return emptyList()
        android.util.Log.d("DataManager", "导入: 找到 ${mediaFiles.size} 个媒体文件: ${mediaFiles.joinToString(", ") { it.name }}")
        
        return try {
            data.split(MEDIA_DELIMITER).mapNotNull { itemStr ->
                val parts = itemStr.split("|")
                if (parts.size < 5) return@mapNotNull null
                
                val mediaId = parts.getOrElse(0) { "" }
                val mediaName = parts.getOrElse(3) { "" }
                val mimeType = parts.getOrElse(4) { "" }
                
                // 使用与 MediaManager 相同的文件名格式：{id}.{extension}
                val extension = getExtensionFromMimeType(mimeType)
                val extensionWithDot = if (extension.isNotEmpty()) ".$extension" else ""
                val exactFileName = "$mediaId$extensionWithDot"
                
                // 尝试多种方式查找文件
                var tempMediaFile: File? = null
                
                // 方式1: 精确匹配 {id}.{extension}
                tempMediaFile = mediaFiles.find { it.name == exactFileName }
                if (tempMediaFile != null) {
                    android.util.Log.d("DataManager", "导入: 精确匹配找到文件 ${tempMediaFile.absolutePath}")
                }
                
                // 方式2: 只用 ID 开头匹配
                if (tempMediaFile == null) {
                    tempMediaFile = mediaFiles.find { it.name.startsWith(mediaId) }
                    if (tempMediaFile != null) {
                        android.util.Log.d("DataManager", "导入: ID开头匹配找到文件 ${tempMediaFile.absolutePath}")
                    }
                }
                
                // 方式3: 模糊匹配 ID
                if (tempMediaFile == null) {
                    tempMediaFile = mediaFiles.find { file ->
                        file.name.contains(mediaId)
                    }
                    if (tempMediaFile != null) {
                        android.util.Log.d("DataManager", "导入: 模糊匹配找到文件 ${tempMediaFile.absolutePath}")
                    }
                }
                
                // 方式4: 如果还是找不到，尝试按顺序取第一个文件
                if (tempMediaFile == null && mediaFiles.isNotEmpty()) {
                    tempMediaFile = mediaFiles.firstOrNull()
                    if (tempMediaFile != null) {
                        android.util.Log.d("DataManager", "导入: 使用第一个文件 ${tempMediaFile.absolutePath}")
                    }
                }
                
                if (tempMediaFile != null && tempMediaFile.exists()) {
                    // 复制到应用私有存储
                    val newUri = mediaManager.copyToPrivateStorage(tempMediaFile)
                    android.util.Log.d("DataManager", "导入: 复制文件到 ${newUri}")
                    MediaItem(
                        id = mediaId.ifEmpty { UUID.randomUUID().toString() },
                        uri = newUri.toString(),
                        type = try { MediaType.valueOf(parts.getOrElse(2) { "IMAGE" }) } catch (e: Exception) { MediaType.IMAGE },
                        name = mediaName.ifEmpty { tempMediaFile.name },
                        mimeType = mimeType.ifEmpty { getMimeTypeFromFileName(tempMediaFile.name) },
                        size = tempMediaFile.length(),
                        duration = parts.getOrElse(6) { "" }.toLongOrNull(),
                        createdAt = parts.getOrElse(7) { "" }.toLongOrNull() ?: System.currentTimeMillis()
                    )
                } else {
                    android.util.Log.d("DataManager", "导入: 未找到文件，mediaId=$mediaId, exactFileName=$exactFileName")
                    null
                }
            }.filterNotNull()
        } catch (e: Exception) {
            android.util.Log.e("DataManager", "导入: 解析媒体失败", e)
            emptyList()
        }
    }
    
    /**
     * 从文件名获取 MIME 类型
     */
    private fun getMimeTypeFromFileName(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "bmp" -> "image/bmp"
            "mp4" -> "video/mp4"
            "avi" -> "video/avi"
            "mkv" -> "video/x-matroska"
            "3gp" -> "video/3gpp"
            "mov" -> "video/quicktime"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "ogg" -> "audio/ogg"
            "m4a" -> "audio/mp4"
            "aac" -> "audio/aac"
            else -> "application/octet-stream"
        }
    }
    
    /**
     * 从 MIME 类型获取文件扩展名
     */
    private fun getExtensionFromMimeType(mimeType: String): String {
        return when (mimeType) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/gif" -> "gif"
            "image/webp" -> "webp"
            "image/bmp" -> "bmp"
            "video/mp4" -> "mp4"
            "video/avi" -> "avi"
            "video/x-matroska" -> "mkv"
            "video/3gpp" -> "3gp"
            "video/quicktime" -> "mov"
            "audio/mpeg" -> "mp3"
            "audio/wav" -> "wav"
            "audio/ogg" -> "ogg"
            "audio/mp4" -> "m4a"
            "audio/aac" -> "aac"
            else -> ""
        }
    }
    
    // 转义特殊字符
    private fun escape(text: String): String {
        return text
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace(DELIMITER, "\\|")
            .replace(MEDIA_DELIMITER, "\\;\\;")
    }
    
    // 反转义
    private fun unescape(text: String): String {
        return text
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\|", DELIMITER)
            .replace("\\;\\;", MEDIA_DELIMITER)
    }
}

/**
 * 导入结果
 */
data class ImportResult(
    val successCount: Int,
    val failCount: Int,
    val errors: List<String>
)
