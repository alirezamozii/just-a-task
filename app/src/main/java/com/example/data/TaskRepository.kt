package com.example.data

import android.util.Log
import com.example.network.GeminiClient
import com.example.network.TaskCommand
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class TaskRepository(
    private val taskDao: TaskDao,
    private val chatMessageDao: ChatMessageDao,
    private val notificationDao: NotificationDao
) {
    private val TAG = "TaskRepository"

    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()
    val allMessages: Flow<List<ChatMessage>> = chatMessageDao.getAllMessages()
    val allNotifications: Flow<List<NotificationEntity>> = notificationDao.getAllNotifications()
    val unreadNotificationsCount: Flow<Int> = notificationDao.getUnreadCountFlow()

    suspend fun insertNotification(notification: NotificationEntity): Long {
        return notificationDao.insertNotification(notification)
    }

    suspend fun markNotificationAsRead(id: Int) {
        notificationDao.markAsRead(id)
    }

    suspend fun markAllNotificationsAsRead() {
        notificationDao.markAllAsRead()
    }

    suspend fun deleteNotificationById(id: Int) {
        notificationDao.deleteNotificationById(id)
    }

    suspend fun clearAllNotifications() {
        notificationDao.clearAllNotifications()
    }

    suspend fun getTaskById(id: Int): Task? {
        return taskDao.getTaskById(id)
    }

    suspend fun insertTask(task: Task): Long {
        return taskDao.insertTask(task)
    }

    suspend fun updateTask(task: Task) {
        taskDao.updateTask(task)
    }

    suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)
    }

    suspend fun deleteTaskById(id: Int) {
        taskDao.deleteTaskById(id)
    }

    suspend fun clearAllTasks() {
        taskDao.clearAllTasks()
    }

    suspend fun insertMessage(message: ChatMessage): Long {
        return chatMessageDao.insertMessage(message)
    }

    suspend fun clearHistory() {
        chatMessageDao.clearAllMessages()
    }

    /**
     * Sends a chat message to Gemini, saves both the user's and the AI's responses,
     * and executes the resulting task modifications locally.
     */
    suspend fun sendChatMessage(userText: String, isChatMode: Boolean = false): Pair<String, List<TaskCommand>> {
        // 1. Save user's message
        val userMsg = ChatMessage(sender = "user", text = userText)
        insertMessage(userMsg)

        // 2. Fetch full lists for context
        val currentHistory = chatMessageDao.getAllMessagesList()
        val currentTasks = taskDao.getAllTasksList()

        // 3. Request AI evaluation
        val response = GeminiClient.sendMessage(currentHistory, currentTasks, isChatMode)

        // 4. Save AI reply in message history
        val aiMsg = ChatMessage(sender = "ai", text = response.reply)
        insertMessage(aiMsg)

        // Return to VM to decide whether to execute or wait for validation
        return Pair(response.reply, response.commands)
    }

    suspend fun executeCommands(commands: List<TaskCommand>) {
        val currentTasks = taskDao.getAllTasksList()
        for (cmd in commands) {
            try {
                when (cmd.command) {
                    "ADD_TASK" -> {
                            val subtasksStr = if (cmd.subtasks.isNotEmpty()) org.json.JSONArray(cmd.subtasks).toString() else "[]"
                            val task = Task(
                                title = cmd.title.ifEmpty { "New Task" },
                                description = cmd.description,
                                deadline = cmd.deadline,
                                importanceScore = cmd.importanceScore.coerceIn(1, 100),
                                urgencyScore = cmd.urgencyScore.coerceIn(1, 100),
                                estimatedMinutes = cmd.estimatedMinutes,
                                timeEstimateText = cmd.timeEstimateText,
                                reasoning = cmd.reasoning,
                                emoji = cmd.emoji,
                                colorIndex = cmd.colorIndex.coerceIn(0, 9),
                                folderName = cmd.folderName,
                                status = "Pending",
                                subtasksJson = subtasksStr
                            )
                        insertTask(task)
                        Log.d(TAG, "Executed: ADD_TASK ${task.title}")
                    }
                    "UPDATE_TASK" -> {
                        // Locate existing task by ID or Title
                        val existingTask = findTask(cmd, currentTasks)
                        if (existingTask != null) {
                            val subtasksStr = if (cmd.subtasks.isNotEmpty()) org.json.JSONArray(cmd.subtasks).toString() else existingTask.subtasksJson
                            val updated = existingTask.copy(
                                title = cmd.title.ifEmpty { existingTask.title },
                                description = cmd.description.ifEmpty { existingTask.description },
                                deadline = if (cmd.deadline != 0L) cmd.deadline else existingTask.deadline,
                                importanceScore = if (cmd.importanceScore != 50 || existingTask.importanceScore == 50) cmd.importanceScore else existingTask.importanceScore,
                                urgencyScore = if (cmd.urgencyScore != 50 || existingTask.urgencyScore == 50) cmd.urgencyScore else existingTask.urgencyScore,
                                estimatedMinutes = if (cmd.estimatedMinutes != 0) cmd.estimatedMinutes else existingTask.estimatedMinutes,
                                timeEstimateText = cmd.timeEstimateText.ifEmpty { existingTask.timeEstimateText },
                                reasoning = cmd.reasoning.ifEmpty { existingTask.reasoning },
                                emoji = cmd.emoji.takeIf { it != "📝" } ?: existingTask.emoji,
                                colorIndex = if (cmd.colorIndex != 0) cmd.colorIndex else existingTask.colorIndex,
                                folderName = cmd.folderName ?: existingTask.folderName,
                                subtasksJson = subtasksStr
                            )
                            updateTask(updated)
                            Log.d(TAG, "Executed: UPDATE_TASK ID=${updated.id} ${updated.title}")
                        }
                    }
                    "COMPLETE_TASK" -> {
                        val existingTask = findTask(cmd, currentTasks)
                        if (existingTask != null) {
                            updateTask(existingTask.copy(status = "Completed"))
                            Log.d(TAG, "Executed: COMPLETE_TASK ID=${existingTask.id} ${existingTask.title}")
                        }
                    }
                    "DELETE_TASK" -> {
                        val existingTask = findTask(cmd, currentTasks)
                        if (existingTask != null) {
                            deleteTask(existingTask)
                            Log.d(TAG, "Executed: DELETE_TASK ID=${existingTask.id} ${existingTask.title}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to execute command: $cmd", e)
            }
        }
    }

    // Helper to match a command target to actual database items
    private fun findTask(cmd: TaskCommand, tasks: List<Task>): Task? {
        if (cmd.taskId != null) {
            val found = tasks.find { it.id == cmd.taskId }
            if (found != null) return found
        }
        if (cmd.title.isNotEmpty()) {
            val found = tasks.find { it.title.equals(cmd.title, ignoreCase = true) }
            if (found != null) return found
        }
        return null
    }
}
