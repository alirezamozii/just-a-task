package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ChatMessage
import com.example.data.Task
import com.example.data.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class SortMode {
    DEADLINE_SOONEST,
    ESTIMATED_FASTEST,
    PRIORITY_HIGHEST,
    BALANCE
}

enum class StatusFilter {
    ALL,
    PENDING,
    COMPLETED
}

class TaskViewModel(private val repository: TaskRepository) : ViewModel() {

    // RAW tasks and chat lists directly from DB flows
    val allTasks: StateFlow<List<Task>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatMessages: StateFlow<List<ChatMessage>> = repository.allMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI-side filters and sorting options states
    private val _sortMode = MutableStateFlow(SortMode.DEADLINE_SOONEST)
    val sortMode: StateFlow<SortMode> = _sortMode.asStateFlow()

    private val _statusFilter = MutableStateFlow(StatusFilter.PENDING)
    val statusFilter: StateFlow<StatusFilter> = _statusFilter.asStateFlow()

    // Loading & chat states
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _uiError = MutableStateFlow<String?>(null)
    val uiError: StateFlow<String?> = _uiError.asStateFlow()

    // COMBINED & processed tasks flow for the Task List UI
    val filteredAndSortedTasks: StateFlow<List<Task>> = combine(
        allTasks,
        _sortMode,
        _statusFilter
    ) { tasks, sortMode, statusFilter ->
        var list = when (statusFilter) {
            StatusFilter.ALL -> tasks
            StatusFilter.PENDING -> tasks.filter { it.status == "Pending" }
            StatusFilter.COMPLETED -> tasks.filter { it.status == "Completed" }
        }

        list = when (sortMode) {
            SortMode.DEADLINE_SOONEST -> {
                // Keep non-zero deadlines first, then infinite ones (0L)
                list.sortedWith(compareBy<Task> { it.deadline == 0L }.thenBy { it.deadline })
            }
            SortMode.ESTIMATED_FASTEST -> {
                list.sortedWith(compareBy<Task> { it.estimatedMinutes == 0 }.thenBy { it.estimatedMinutes })
            }
            SortMode.PRIORITY_HIGHEST -> {
                // High priority score is highest priority
                list.sortedByDescending { it.priorityScore }
            }
            SortMode.BALANCE -> {
                // Balance is priority versus estimated minutes
                list.sortedByDescending { it.priorityScore.toFloat() / it.estimatedMinutes.coerceAtLeast(1).toFloat() }
            }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSortMode(mode: SortMode) {
        _sortMode.value = mode
    }

    fun setStatusFilter(filter: StatusFilter) {
        _statusFilter.value = filter
    }

    fun clearError() {
        _uiError.value = null
    }

    // --- ACTIONS ---

    fun sendChatMessage(text: String, isChatMode: Boolean = false) {
        if (text.trim().isEmpty()) return
        viewModelScope.launch {
            _isGenerating.value = true
            _uiError.value = null
            try {
                repository.sendChatMessage(text, isChatMode)
            } catch (e: Exception) {
                _uiError.value = e.localizedMessage ?: "Failed to contact Gemini"
                // Save a temporary failure in user's chat listing for visual cues
                repository.insertMessage(ChatMessage(sender = "ai", text = "Sorry, I had trouble evaluating that. Please check your connectivity or API Key settings."))
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun addTaskManually(
        title: String,
        description: String,
        deadline: Long,
        priority: Int,
        estimatedMinutes: Int,
        timeEstimateText: String = "",
        reasoning: String = "Created manually by user",
        emoji: String = "📝",
        colorIndex: Int = 0,
        folderName: String? = null
    ) {
        viewModelScope.launch {
            val task = Task(
                title = title.ifEmpty { "Manual Task" },
                description = description,
                deadline = deadline,
                priorityScore = priority.coerceIn(1, 100),
                estimatedMinutes = estimatedMinutes,
                timeEstimateText = timeEstimateText,
                reasoning = reasoning,
                emoji = emoji,
                colorIndex = colorIndex,
                folderName = folderName
            )
            repository.insertTask(task)
        }
    }

    fun toggleTaskComplete(task: Task) {
        viewModelScope.launch {
            val nextStatus = if (task.status == "Completed") "Pending" else "Completed"
            repository.updateTask(task.copy(status = nextStatus))
        }
    }

    fun updateTaskManually(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task)
        }
    }

    fun exportTasksToJson(): String {
        val tasks = allTasks.value
        val arr = org.json.JSONArray()
        for (t in tasks) {
            val obj = org.json.JSONObject()
            obj.put("title", t.title)
            obj.put("description", t.description)
            obj.put("deadline", t.deadline)
            obj.put("priorityScore", t.priorityScore)
            obj.put("estimatedMinutes", t.estimatedMinutes)
            obj.put("status", t.status)
            obj.put("createdAt", t.createdAt)
            obj.put("timeEstimateText", t.timeEstimateText)
            obj.put("reasoning", t.reasoning)
            obj.put("emoji", t.emoji)
            obj.put("colorIndex", t.colorIndex)
            obj.put("folderName", t.folderName ?: "")
            obj.put("subtasksJson", t.subtasksJson)
            arr.put(obj)
        }
        return arr.toString(2)
    }

    fun importTasksFromJson(jsonStr: String): Boolean {
        return try {
            val arr = org.json.JSONArray(jsonStr)
            viewModelScope.launch {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val task = Task(
                        title = obj.optString("title", "Imported Task"),
                        description = obj.optString("description", ""),
                        deadline = obj.optLong("deadline", 0L),
                        priorityScore = obj.optInt("priorityScore", 50),
                        estimatedMinutes = obj.optInt("estimatedMinutes", 0),
                        status = obj.optString("status", "Pending"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        timeEstimateText = obj.optString("timeEstimateText", ""),
                        reasoning = obj.optString("reasoning", "Imported"),
                        emoji = obj.optString("emoji", "📝"),
                        colorIndex = obj.optInt("colorIndex", 0),
                        folderName = obj.optString("folderName", "").takeIf { it.isNotBlank() },
                        subtasksJson = obj.optString("subtasksJson", "[]")
                    )
                    repository.insertTask(task)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            // Welcome message back
            repository.insertMessage(
                ChatMessage(
                    sender = "ai",
                    text = "داستان بفرست داستان کنم برات دا"
                )
            )
        }
    }

    fun clearAllTasks() {
        viewModelScope.launch {
            repository.clearAllTasks()
        }
    }

    init {
        // Initialize an empty history with a welcome message if the database is fresh
        viewModelScope.launch {
            val existing = repository.allMessages.first()
            if (existing.isEmpty()) {
                repository.insertMessage(
                    ChatMessage(
                        sender = "ai",
                        text = "داستان بفرست داستان کنم برات دا"
                    )
                )
            }
        }
    }
}
