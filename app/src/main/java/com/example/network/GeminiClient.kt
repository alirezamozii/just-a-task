package com.example.network

import android.util.Log
import com.example.BuildConfig
import com.example.data.ChatMessage
import com.example.data.Task
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class GeminiResponse(
    val reply: String,
    val commands: List<TaskCommand> = emptyList()
)

data class TaskCommand(
    val command: String, // "ADD_TASK", "UPDATE_TASK", "DELETE_TASK", "COMPLETE_TASK"
    val taskId: Int? = null,
    val title: String = "",
    val description: String = "",
    val deadline: Long = 0L,
    val importanceScore: Int = 50,
    val urgencyScore: Int = 50,
    val estimatedMinutes: Int = 0,
    val timeEstimateText: String = "",
    val reasoning: String = "",
    val emoji: String = "📝",
    val colorIndex: Int = 0,
    val folderName: String? = null,
    val subtasks: List<String> = emptyList()
)

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun sendMessage(
        history: List<ChatMessage>,
        currentTasks: List<Task>,
        isChatMode: Boolean = false
    ): GeminiResponse = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext GeminiResponse(
                reply = "⚠️ Gemini API key is not configured. Please add your API key securely to the Secrets panel in AI Studio.",
                commands = emptyList()
            )
        }

        val currentTimeString = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        
        // Format current tasks for AI context
        val tasksContext = JSONArray().apply {
            for (task in currentTasks) {
                put(JSONObject().apply {
                    put("id", task.id)
                    put("title", task.title)
                    put("description", task.description)
                    put("deadline", task.deadline)
                    put("importanceScore", task.importanceScore)
                    put("urgencyScore", task.urgencyScore)
                    put("estimatedMinutes", task.estimatedMinutes)
                    put("status", task.status)
                    put("timeEstimateText", task.timeEstimateText)
                    put("emoji", task.emoji)
                    put("colorIndex", task.colorIndex)
                    put("folderName", task.folderName ?: "")
                })
            }
        }.toString()

        val systemInstruction = if (isChatMode) {
"""You are a helpful AI counselor and an organizer.
The user wants to chat naturally, get motivation, and talk about their tasks. 
DO NOT manage tasks (no adding, deleting, or updating), just respond conversationally in Persian (فارسی).

Format your response STRICTLY as a raw JSON object with two fields (commands should be empty):
{
  "reply": "پاسخ دوستانه و مشاوره‌ای شما در مورد کلماتی که کاربر گفته است.",
  "commands": []
}

No markdown tags! Do NOT wrap in ```json ... ```. Just raw JSON text.

Current Time: $currentTimeString 
Current Tasks Info as Context (Do NOT repeat the list, just use it to understand what the user has to do if they ask):
$tasksContext
"""
        } else {
"""You are an advanced AI Task Management Agent (like natigtavity). You must understand complex user requests, break them down into multi-action plans, and propose them logicially.
IMPORTANT: You MUST write your 'reply', task 'title', 'subtasks', 'timeEstimateText', and 'reasoning' fields in natural and conversational Persian (فارسی عامیانه). 

Rules:
1. Break down big or complex user requests into multiple small, manageable `ADD_TASK` commands.
2. For complex tasks (e.g. "Create a fullstack app"), create as many `subtasks` as needed (e.g. 10 subtasks). For simple tasks ("drink water"), use 0 subtasks.
3. Automatically group related tasks by assigning them a logical `folderName` (e.g. "آموزش", "برنامه‌نویسی"). Create multiple folders if the user assigns multiple unrelated things at once.
4. If the user asks to edit some tasks, figure out which ones they mean and issue multiple `UPDATE_TASK` or `DELETE_TASK` commands.

Format your response STRICTLY as a raw JSON object with two fields (No Markdown):
{
  "reply": "پاسخ عامیانه شما: من اینطوری متوجه شدم که نیاز به ۲ فولدر جدید و ۶ تسک داریم. آیا موافقی اینها رو اضافه کنم؟",
  "commands": [
    {
      "command": "ADD_TASK" | "UPDATE_TASK" | "DELETE_TASK" | "COMPLETE_TASK",
      "taskId": 12, // (optional, integer for actioning existing tasks)
      "title": "عنوان کار (عامیانه)",
      "description": "شرح (میتونه خالی باشه)",
      "deadline": 1782348500000, // UTC epoch milliseconds, or 0 if none
      "importanceScore": 85, // 1 to 100
      "urgencyScore": 90, // 1 to 100
      "timeEstimateText": "مثلاً ۲ ساعت",
      "reasoning": "دلیل این تسک",
      "folderName": "نام فولدر (مثل باشگاه)",
      "colorIndex": 2, // 0 to 9 index for color
      "emoji": "🎨",
      "subtasks": ["قدم اول", "قدم دوم"] // List of strings for subtasks checklists
    }
  ]
}

Current Time: $currentTimeString (Re-calculate deadlines relative to this epoch millis: ${System.currentTimeMillis()})
Current Active & Completed Tasks List:
$tasksContext
"""
        }

        try {
            // Build content JSON
            val requestBodyJson = JSONObject().apply {
                // System Instruction
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", systemInstruction) })
                    })
                })

                // Contents (multi-turn history)
                put("contents", JSONArray().apply {
                    // Chat history
                    for (msg in history.takeLast(15)) { // Limit context turns
                        val role = if (msg.sender == "user") "user" else "model"
                        put(JSONObject().apply {
                            put("role", role)
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply { put("text", msg.text) })
                            })
                        })
                    }
                })

                // Generation Config for JSON
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                })
            }

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "No content"
                    Log.e(TAG, "Request failed: ${response.code} $errorBody")
                    return@withContext GeminiResponse(
                        reply = "Error: Gemini API request failed with status code ${response.code}. Please ensure your API key is valid.",
                        commands = emptyList()
                    )
                }

                val responseBodyStr = response.body?.string() ?: throw Exception("Empty response body")
                Log.d(TAG, "Raw response: $responseBodyStr")

                val rootJson = JSONObject(responseBodyStr)
                val textResponse = rootJson.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                // Clean the text in case Gemini wraps it in code blocks despite instructions
                val cleanedText = textResponse.trim().trim('`').removePrefix("json").trim()
                Log.d(TAG, "Cleaned AI output: $cleanedText")

                val aiResult = JSONObject(cleanedText)
                val reply = aiResult.optString("reply", "I've handled that.")
                
                val commandsList = mutableListOf<TaskCommand>()
                val commandsJson = aiResult.optJSONArray("commands")
                if (commandsJson != null) {
                    for (i in 0 until commandsJson.length()) {
                        val cmdObj = commandsJson.getJSONObject(i)
                        val subtasksArr = cmdObj.optJSONArray("subtasks")
                        val parsedSubtasks = mutableListOf<String>()
                        if (subtasksArr != null) {
                            for (j in 0 until subtasksArr.length()) {
                                parsedSubtasks.add(subtasksArr.optString(j))
                            }
                        }
                        commandsList.add(
                            TaskCommand(
                                command = cmdObj.optString("command"),
                                taskId = if (cmdObj.has("taskId") && !cmdObj.isNull("taskId")) cmdObj.getInt("taskId") else null,
                                title = cmdObj.optString("title", ""),
                                description = cmdObj.optString("description", ""),
                                deadline = cmdObj.optLong("deadline", 0L),
                                importanceScore = cmdObj.optInt("importanceScore", 50),
                                urgencyScore = cmdObj.optInt("urgencyScore", 50),
                                estimatedMinutes = cmdObj.optInt("estimatedMinutes", 0),
                                timeEstimateText = cmdObj.optString("timeEstimateText", ""),
                                reasoning = cmdObj.optString("reasoning", ""),
                                folderName = cmdObj.optString("folderName", "").takeIf { it.isNotBlank() },
                                colorIndex = cmdObj.optInt("colorIndex", 0),
                                emoji = cmdObj.optString("emoji", "📝"),
                                subtasks = parsedSubtasks
                            )
                        )
                    }
                }

                return@withContext GeminiResponse(reply, commandsList)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini call exception", e)
            return@withContext GeminiResponse(
                reply = "Unable to process that request: ${e.localizedMessage ?: "Network error"}",
                commands = emptyList()
            )
        }
    }
}
