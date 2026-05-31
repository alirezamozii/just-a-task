package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val deadline: Long = 0L, // 0 means no deadline
    val priorityScore: Int = 50, // 1-100
    val estimatedMinutes: Int = 0, // replaces effort
    val status: String = "Pending", // "Pending", "Completed"
    val createdAt: Long = System.currentTimeMillis(),
    val timeEstimateText: String = "",
    val reasoning: String = "",
    val emoji: String = "📝",
    val colorIndex: Int = 0,
    val folderName: String? = null,
    val subtasksJson: String = "[]" // JSON string representing List<Subtask>
)
