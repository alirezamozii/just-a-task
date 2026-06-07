package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.data.AppDatabase
import com.example.data.TaskRepository
import com.example.ui.TaskViewModel
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Initialize local database persistence
    val database = AppDatabase.getDatabase(applicationContext)
    val repository = TaskRepository(database.taskDao(), database.chatMessageDao(), database.notificationDao())
    val viewModel = TaskViewModel(repository)
    
    // Wire up system notification dispatcher
    val notificationHelper = com.example.ui.utils.NotificationHelper(applicationContext)
    viewModel.systemNotificationTrigger = { title, body ->
        notificationHelper.showSystemNotification(
            (System.currentTimeMillis() % 100000).toInt(),
            title,
            body
        )
    }

    setContent {
      MyApplicationTheme {
        MainScreen(viewModel = viewModel)
      }
    }
  }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  MyApplicationTheme { Greeting("Android") }
}
