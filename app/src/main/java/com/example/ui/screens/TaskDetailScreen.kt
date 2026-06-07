package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Task
import com.example.ui.TaskViewModel

@Composable
fun TaskDetailScreen(task: Task, viewModel: TaskViewModel, colors: ThemeColors, onBack: () -> Unit, onEdit: () -> Unit) {
    val cardColor = colors.taskColors.getOrNull(task.colorIndex) ?: colors.taskColors[0]
    val onCardColor = colors.onTaskColors.getOrNull(task.colorIndex) ?: colors.onTaskColors[0]
    
    val initialSubtasks = remember(task.subtasksJson) {
        try {
            val arr = org.json.JSONArray(task.subtasksJson)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) { list.add(arr.optString(i)) }
            if (list.isEmpty()) {
                task.description.split("\n").filter { it.isNotBlank() }
            } else list
        } catch (e: Exception) {
            task.description.split("\n").filter { it.isNotBlank() }
        }
    }
    
    val checkedStates = remember { androidx.compose.runtime.mutableStateListOf<Boolean>().apply { 
        repeat(initialSubtasks.size) { add(false) } 
    } }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        // App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.IconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp).clip(CircleShape).background(colors.surface)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
            }
            Text(
                text = "جزئیات تسک", 
                fontSize = 18.sp, 
                fontWeight = FontWeight.Bold, 
                color = colors.textPrimary
            )
            androidx.compose.material3.IconButton(
                onClick = onEdit,
                modifier = Modifier.size(48.dp).clip(CircleShape).background(colors.surface)
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = colors.textPrimary)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 24.dp, top = 0.dp, end = 24.dp, bottom = 100.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(24.dp, RoundedCornerShape(24.dp), spotColor = cardColor.copy(alpha = 0.5f))
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(cardColor, cardColor.copy(alpha = 0.8f))
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(task.emoji, fontSize = 32.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = task.title,
                                color = onCardColor,
                                fontWeight = FontWeight.Black,
                                fontSize = 26.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            if (task.timeEstimateText.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Schedule, tint = onCardColor.copy(alpha=0.7f), contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(task.timeEstimateText, color = onCardColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            if (task.folderName != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Folder, tint = onCardColor.copy(alpha=0.7f), contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(task.folderName, color = onCardColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, tint = onCardColor.copy(alpha=0.7f), contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("اهمیت: ${task.importanceScore}/100 - سرعت: ${task.urgencyScore}/100 - اولویت متوسط: ${(task.importanceScore+task.urgencyScore)/2}/100", color = onCardColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
            
            if (initialSubtasks.isNotEmpty()) {
                item {
                    Text("ساب‌تسک‌ها (چک لیست):", color = colors.textSecondary, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 12.dp))
                }
                itemsIndexed(initialSubtasks) { index, sub ->
                    val isChecked = checkedStates[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.surface)
                            .clickable { checkedStates[index] = !checkedStates[index] }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (isChecked) cardColor else Color.Transparent)
                                .border(2.dp, if (isChecked) cardColor else colors.textSecondary.copy(alpha=0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isChecked) {
                                Icon(Icons.Default.Check, tint = onCardColor, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = sub,
                            color = if (isChecked) colors.textSecondary.copy(alpha = 0.5f) else colors.textPrimary,
                            textDecoration = if (isChecked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else if (task.description.isNotBlank()) {
                item {
                    Text("توضیحات:", color = colors.textSecondary, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 12.dp))
                    Text(
                        text = task.description,
                        color = colors.textPrimary,
                        fontSize = 15.sp,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    )
                }
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
            
            item {
                androidx.compose.material3.Button(
                    onClick = { 
                        viewModel.toggleTaskComplete(task)
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = if (task.status == "Completed") colors.surface else cardColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (task.status == "Completed") "برگرداندن به حالت در حال انجام" else "تکمیل تسک!", 
                        color = if (task.status == "Completed") colors.textPrimary else onCardColor, 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
