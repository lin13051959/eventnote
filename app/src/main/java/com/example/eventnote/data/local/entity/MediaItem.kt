package com.example.eventnote.data.local.entity

/**
 * 多媒体项目数据类
 */
data class MediaItem(
    val id: String,           // 唯一标识符
    val uri: String,          // 文件 URI
    val type: MediaType,     // 媒体类型
    val name: String,         // 文件名
    val mimeType: String,    // MIME 类型
    val size: Long,          // 文件大小（字节）
    val duration: Long? = null,  // 音视频时长（毫秒）
    val createdAt: Long = System.currentTimeMillis()
)

enum class MediaType {
    IMAGE,      // 图片
    VIDEO,      // 视频
    AUDIO       // 音频
}
