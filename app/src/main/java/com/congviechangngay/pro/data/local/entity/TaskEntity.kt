package com.congviechangngay.pro.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,            // Tên công việc
    val description: String = "", // Mô tả
    val category: String = "",    // Phân loại (3S/3D, OT/AMH, Hàng hóa...)
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
