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

    // System Notification Dispatch Callback
    var systemNotificationTrigger: ((title: String, body: String) -> Unit)? = null

    val allNotifications: StateFlow<List<com.example.data.NotificationEntity>> = repository.allNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotificationsCount: StateFlow<Int> = repository.unreadNotificationsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

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

    private val _pendingCommands = MutableStateFlow<List<com.example.network.TaskCommand>?>(null)
    val pendingCommands: StateFlow<List<com.example.network.TaskCommand>?> = _pendingCommands.asStateFlow()

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
                list.sortedByDescending { it.importanceScore + it.urgencyScore }
            }
            SortMode.BALANCE -> {
                // Balance is priority versus estimated minutes
                list.sortedByDescending { (it.importanceScore + it.urgencyScore).toFloat() / it.estimatedMinutes.coerceAtLeast(1).toFloat() }
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
                val (_, commands) = repository.sendChatMessage(text, isChatMode)
                if (commands.isNotEmpty()) {
                    _pendingCommands.value = commands
                }
            } catch (e: Exception) {
                _uiError.value = e.localizedMessage ?: "Failed to contact Gemini"
                // Save a temporary failure in user's chat listing for visual cues
                repository.insertMessage(ChatMessage(sender = "ai", text = "Sorry, I had trouble evaluating that. Please check your connectivity or API Key settings."))
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun acceptPendingCommands() {
        val cmds = _pendingCommands.value ?: return
        _pendingCommands.value = null
        viewModelScope.launch {
            repository.executeCommands(cmds)
        }
    }

    fun rejectPendingCommands(feedback: String? = null) {
        _pendingCommands.value = null
        if (!feedback.isNullOrBlank()) {
            sendChatMessage("این پیشنهادها رو رد کردم چون: $feedback", true)
        }
    }

    // --- NOTIFICATION ACTIONS ---

    fun markNotificationAsRead(id: Int) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            repository.clearAllNotifications()
        }
    }

    fun addNotificationToHistoryAndTriggerSystem(title: String, body: String, type: String = "info") {
        viewModelScope.launch {
            val entity = com.example.data.NotificationEntity(
                title = title,
                body = body,
                type = type
            )
            repository.insertNotification(entity)
            systemNotificationTrigger?.invoke(title, body)
        }
    }

    fun checkUpcomingDeadlines() {
        viewModelScope.launch {
            val tasks = allTasks.value
            val now = System.currentTimeMillis()
            for (task in tasks) {
                if (task.status == "Pending" && task.deadline > now && (task.deadline - now) <= 24 * 60 * 60 * 1000) {
                    addNotificationToHistoryAndTriggerSystem(
                        title = "⚠️ ددلاین نزدیک است، داشیییی!",
                        body = "کمتر از ۲۴ ساعت به ددلاین تسک «${task.title}» باقی مانده است. بجنب!",
                        type = "deadline_near"
                    )
                }
            }
        }
    }

    fun addTaskManually(
        title: String,
        description: String,
        deadline: Long,
        priority: Int,
        urgency: Int,
        estimatedMinutes: Int,
        timeEstimateText: String = "",
        reasoning: String = "Created manually by user",
        emoji: String = "📝",
        colorIndex: Int = 0,
        folderName: String? = null
    ) {
        viewModelScope.launch {
            val task = Task(
                title = title.ifEmpty { "تسک دستی" },
                description = description,
                deadline = deadline,
                importanceScore = priority.coerceIn(1, 100),
                urgencyScore = urgency.coerceIn(1, 100),
                estimatedMinutes = estimatedMinutes,
                timeEstimateText = timeEstimateText,
                reasoning = reasoning,
                emoji = emoji,
                colorIndex = colorIndex,
                folderName = folderName
            )
            repository.insertTask(task)
            addNotificationToHistoryAndTriggerSystem(
                title = "➕ تسک جدید ثبت شد",
                body = "تسک «${task.title}» با موفقیت به برنامه شما اضافه شد، داشیییی!",
                type = "info"
            )
        }
    }

    fun toggleTaskComplete(task: Task) {
        viewModelScope.launch {
            val nextStatus = if (task.status == "Completed") "Pending" else "Completed"
            repository.updateTask(task.copy(status = nextStatus))
            if (nextStatus == "Completed") {
                addNotificationToHistoryAndTriggerSystem(
                    title = "🎖️ کارت عالی بود داشیییی!",
                    body = "تسک «${task.title}» با موفقیت تکمیل شد! همین‌جوری ادامه بده ⚡",
                    type = "high_priority"
                )
            } else {
                addNotificationToHistoryAndTriggerSystem(
                    title = "↩️ بازگردانی تسک انجام شده",
                    body = "تسک «${task.title}» دوباره به حالت در جریان قرار گرفت.",
                    type = "info"
                )
            }
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
            obj.put("importanceScore", t.importanceScore)
            obj.put("urgencyScore", t.urgencyScore)
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
                        title = obj.optString("title", "تسک بک‌آپ"),
                        description = obj.optString("description", ""),
                        deadline = obj.optLong("deadline", 0L),
                        importanceScore = obj.optInt("importanceScore", 50),
                        urgencyScore = obj.optInt("urgencyScore", 50),
                        estimatedMinutes = obj.optInt("estimatedMinutes", 0),
                        status = obj.optString("status", "Pending"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        timeEstimateText = obj.optString("timeEstimateText", ""),
                        reasoning = obj.optString("reasoning", "بازگردانی شده"),
                        emoji = obj.optString("emoji", "📝"),
                        colorIndex = obj.optInt("colorIndex", 0),
                        folderName = obj.optString("folderName", "").takeIf { it.isNotBlank() },
                        subtasksJson = obj.optString("subtasksJson", "[]")
                    )
                    repository.insertTask(task)
                }
                
                addNotificationToHistoryAndTriggerSystem(
                    title = "📥 بازیابی موفق بک‌آپ",
                    body = "تعداد ${arr.length()} تسک با موفقیت از بک‌آپ بازگردانی شد، داشیییی! ⚡",
                    type = "info"
                )
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
        
        // Dynamic automatic check for upcoming deadlines upon launch
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000) // Let database load
            checkUpcomingDeadlines()
        }
    }
}
