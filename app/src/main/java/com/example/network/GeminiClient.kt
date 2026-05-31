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
    val priorityScore: Int = 50,
    val estimatedMinutes: Int = 0,
    val timeEstimateText: String = "",
    val reasoning: String = "",
    val emoji: String = "📝",
    val colorIndex: Int = 0,
    val folderName: String? = null
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
                    put("priorityScore", task.priorityScore)
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
"""You are Just a Task, an advanced AI task management assistant. You analyze, score, and schedule tasks.
IMPORTANT: You MUST write your 'reply', task 'title', task 'description', 'timeEstimateText', and 'reasoning' fields in beautiful and natural Persian (فارسی) language, because the user communicates in Persian. Maintain professional and friendly Persian phrasing, but keep the application name 'Just a Task' as is.

Format your response STRICTLY as a raw JSON object with two fields:
{
  "reply": "پاسخ دوستانه و شخصی‌سازی شده شما به زبان فارسی، نشان دادن کارهایی که انجام داده‌اید، امتیازها (۱ تا ۱۰۰) و دلایل آنها.",
  "commands": [
    {
      "command": "ADD_TASK" | "UPDATE_TASK" | "DELETE_TASK" | "COMPLETE_TASK",
      "taskId": 12, // (optional, integer for actioning existing tasks. Use matches from the task list provided!)
      "title": "عنوان کار به زبان فارسی",
      "description": "شرح و جزئیات کار به زبان فارسی",
      "deadline": 1782348500000, // UTC epoch milliseconds, or 0 if none. Re-calculate using relative time offsets from current time!
      "priorityScore": 85, // 1 to 100 (higher means more urgent/higher impact)
      "estimatedMinutes": 120, // Estimated time to complete in minutes
      "timeEstimateText": "مدت زمان کوتاه برآورد شده به فارسی مثلاً ۲ ساعت",
      "reasoning": "توضیح کوتاه و منطق امتیازدهی به فارسی",
      "folderName": "نام فولدر یا دسته بندی مناسب (مثلا کاری، شخصی)",
      "colorIndex": 2, // An integer from 0 to 9 representing a beautiful UI color for the task
      "emoji": "🎨" // A single emoji perfectly representing the task
    }
  ]
}

No markdown tags! Do NOT wrap in ```json ... ```. Just raw JSON text.

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
                        commandsList.add(
                            TaskCommand(
                                command = cmdObj.optString("command"),
                                taskId = if (cmdObj.has("taskId") && !cmdObj.isNull("taskId")) cmdObj.getInt("taskId") else null,
                                title = cmdObj.optString("title", ""),
                                description = cmdObj.optString("description", ""),
                                deadline = cmdObj.optLong("deadline", 0L),
                                priorityScore = cmdObj.optInt("priorityScore", 50),
                                estimatedMinutes = cmdObj.optInt("estimatedMinutes", 0),
                                timeEstimateText = cmdObj.optString("timeEstimateText", ""),
                                reasoning = cmdObj.optString("reasoning", ""),
                                folderName = cmdObj.optString("folderName", "").takeIf { it.isNotBlank() },
                                colorIndex = cmdObj.optInt("colorIndex", 0),
                                emoji = cmdObj.optString("emoji", "📝")
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
