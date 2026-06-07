package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "in_app_notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val body: String,
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String = "info" // "info", "deadline_near", "high_priority", "gemini"
)
