package com.example.data

import android.util.Log
import com.example.network.GeminiClient
import com.example.network.TaskCommand
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class TaskRepository(
    private val taskDao: TaskDao,
    private val chatMessageDao: ChatMessageDao
) {
    private val TAG = "TaskRepository"

    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()
    val allMessages: Flow<List<ChatMessage>> = chatMessageDao.getAllMessages()

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
    suspend fun sendChatMessage(userText: String, isChatMode: Boolean = false): String {
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

        // 5. Execute commands parsed from JSON
        executeCommands(response.commands, currentTasks)

        return response.reply
    }

    private suspend fun executeCommands(commands: List<TaskCommand>, currentTasks: List<Task>) {
        for (cmd in commands) {
            try {
                when (cmd.command) {
                    "ADD_TASK" -> {
                        val task = Task(
                            title = cmd.title.ifEmpty { "New Task" },
                            description = cmd.description,
                            deadline = cmd.deadline,
                            priorityScore = cmd.priorityScore.coerceIn(1, 100),
                            estimatedMinutes = cmd.estimatedMinutes,
                            timeEstimateText = cmd.timeEstimateText,
                            reasoning = cmd.reasoning,
                            emoji = cmd.emoji,
                            colorIndex = cmd.colorIndex.coerceIn(0, 9),
                            folderName = cmd.folderName,
                            status = "Pending"
                        )
                        insertTask(task)
                        Log.d(TAG, "Executed: ADD_TASK ${task.title}")
                    }
                    "UPDATE_TASK" -> {
                        // Locate existing task by ID or Title
                        val existingTask = findTask(cmd, currentTasks)
                        if (existingTask != null) {
                            val updated = existingTask.copy(
                                title = cmd.title.ifEmpty { existingTask.title },
                                description = cmd.description.ifEmpty { existingTask.description },
                                deadline = if (cmd.deadline != 0L) cmd.deadline else existingTask.deadline,
                                priorityScore = if (cmd.priorityScore != 50 || existingTask.priorityScore == 50) cmd.priorityScore else existingTask.priorityScore,
                                estimatedMinutes = if (cmd.estimatedMinutes != 0) cmd.estimatedMinutes else existingTask.estimatedMinutes,
                                timeEstimateText = cmd.timeEstimateText.ifEmpty { existingTask.timeEstimateText },
                                reasoning = cmd.reasoning.ifEmpty { existingTask.reasoning },
                                emoji = cmd.emoji.takeIf { it != "📝" } ?: existingTask.emoji,
                                colorIndex = if (cmd.colorIndex != 0) cmd.colorIndex else existingTask.colorIndex,
                                folderName = cmd.folderName ?: existingTask.folderName
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
