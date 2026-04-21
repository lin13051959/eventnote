package com.example.eventnote.util

import android.content.ContentResolver
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.example.eventnote.data.local.entity.MediaItem
import com.example.eventnote.data.local.entity.MediaType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 媒体文件管理器
 * 负责复制、删除、获取媒体文件信息
 */
@Singleton
class MediaManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val MEDIA_DIR = "event_media"
    }
    
    /**
     * 获取应用私有媒体目录
     */
    private fun getMediaDirectory(): File {
        val mediaDir = File(context.filesDir, MEDIA_DIR)
        if (!mediaDir.exists()) {
            mediaDir.mkdirs()
        }
        return mediaDir
    }
    
    /**
     * 从文件创建 MediaItem
     */
    fun createMediaItemFromFile(file: File): MediaItem? {
        return try {
            if (!file.exists()) return null
            
            val mimeType = getMimeTypeFromFile(file) ?: "image/jpeg"
            
            val mediaType = when {
                mimeType.startsWith("image/") -> MediaType.IMAGE
                mimeType.startsWith("video/") -> MediaType.VIDEO
                mimeType.startsWith("audio/") -> MediaType.AUDIO
                else -> MediaType.IMAGE
            }
            
            val id = UUID.randomUUID().toString()
            val extension = getExtension(mimeType)
            
            // 复制到私有存储
            val savedUri = copyToPrivateStorage(file)
            
            MediaItem(
                id = id,
                uri = savedUri.toString(),
                type = mediaType,
                name = file.name,
                mimeType = mimeType,
                size = file.length(),
                duration = null,
                createdAt = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 从文件获取 MIME 类型
     */
    private fun getMimeTypeFromFile(file: File): String? {
        val extension = file.extension.lowercase()
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
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "ogg" -> "audio/ogg"
            "m4a" -> "audio/mp4"
            else -> null
        }
    }
    
    /**
     * 从 URI 创建 MediaItem 并复制到应用私有存储
     */
    fun createMediaItem(uri: Uri): MediaItem? {
        return try {
            val contentResolver = context.contentResolver
            
            // 获取文件名
            val fileName = getFileName(uri) ?: "media_${System.currentTimeMillis()}"
            
            // 获取 MIME 类型
            val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
            
            // 确定媒体类型
            val mediaType = when {
                mimeType.startsWith("image/") -> MediaType.IMAGE
                mimeType.startsWith("video/") -> MediaType.VIDEO
                mimeType.startsWith("audio/") -> MediaType.AUDIO
                else -> MediaType.IMAGE
            }
            
            // 生成唯一 ID
            val id = UUID.randomUUID().toString()
            
            // 获取文件大小
            val size = getFileSize(uri)
            
            // 获取时长 (视频/音频)
            val duration = getMediaDuration(uri, mediaType)
            
            // 复制文件到私有存储
            val savedUri = copyToPrivateStorage(uri, id, getExtension(mimeType))
            
            MediaItem(
                id = id,
                uri = savedUri.toString(),
                type = mediaType,
                name = fileName,
                mimeType = mimeType,
                size = size,
                duration = duration,
                createdAt = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 从 URI 获取文件名
     */
    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        result = cursor.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path?.let { path ->
                val cut = path.lastIndexOf('/')
                if (cut != -1) path.substring(cut + 1) else path
            }
        }
        return result
    }
    
    /**
     * 获取文件大小
     */
    private fun getFileSize(uri: Uri): Long {
        var size = 0L
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            size = pfd.statSize
        }
        if (size == 0L) {
            // 尝试通过查询获取大小
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex >= 0) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }
        }
        return size
    }
    
    /**
     * 获取媒体时长
     */
    private fun getMediaDuration(uri: Uri, mediaType: MediaType): Long? {
        if (mediaType != MediaType.VIDEO && mediaType != MediaType.AUDIO) {
            return null
        }
        
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            duration?.toLongOrNull()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 复制文件到应用私有存储
     */
    private fun copyToPrivateStorage(uri: Uri, id: String, extension: String): Uri {
        val mediaDir = getMediaDirectory()
        val extensionWithDot = if (extension.isNotEmpty()) ".$extension" else ""
        val destFile = File(mediaDir, "$id$extensionWithDot")
        
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        
        return Uri.fromFile(destFile)
    }
    
    /**
     * 复制文件到应用私有存储（从File对象）
     */
    fun copyToPrivateStorage(sourceFile: File): Uri {
        val mediaDir = getMediaDirectory()
        val fileName = sourceFile.name
        val destFile = File(mediaDir, fileName)
        
        sourceFile.inputStream().use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        
        return Uri.fromFile(destFile)
    }
    
    /**
     * 从 MIME 类型获取文件扩展名
     */
    private fun getExtension(mimeType: String): String {
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: ""
    }
    
    /**
     * 删除媒体文件
     */
    fun deleteMediaFile(uriString: String): Boolean {
        return try {
            val uri = Uri.parse(uriString)
            if (uri.scheme == ContentResolver.SCHEME_FILE) {
                val file = File(uri.path ?: return false)
                file.delete()
            } else {
                // 对于 content URI，尝试删除私有存储中的对应文件
                val path = uriString.substringAfterLast("/")
                val file = File(getMediaDirectory(), path)
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 删除多个媒体文件
     */
    fun deleteMediaFiles(mediaItems: List<MediaItem>) {
        mediaItems.forEach { deleteMediaFile(it.uri) }
    }
    
    /**
     * 获取所有已存储的媒体文件
     */
    fun getAllStoredMediaFiles(): List<File> {
        val mediaDir = getMediaDirectory()
        return mediaDir.listFiles()?.toList() ?: emptyList()
    }
    
    /**
     * 清理孤立媒体文件（事件已删除但文件还在）
     */
    fun cleanupOrphanedFiles(eventMediaUris: Set<String>) {
        val allFiles = getAllStoredMediaFiles()
        val eventPaths = eventMediaUris.map { Uri.parse(it).lastPathSegment }.toSet()
        
        allFiles.forEach { file ->
            if (file.name !in eventPaths) {
                file.delete()
            }
        }
    }
    
    /**
     * 格式化文件大小
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }
    
    /**
     * 格式化时长
     */
    fun formatDuration(millis: Long?): String {
        if (millis == null) return ""
        
        val seconds = millis / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        
        return when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes % 60, seconds % 60)
            else -> String.format("%d:%02d", minutes, seconds % 60)
        }
    }
}
