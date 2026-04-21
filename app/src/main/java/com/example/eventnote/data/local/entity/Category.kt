package com.example.eventnote.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 分类实体
 */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val color: Int = 0xFF6200EE.toInt(), // 默认颜色
    val createdAt: Long = System.currentTimeMillis()
)