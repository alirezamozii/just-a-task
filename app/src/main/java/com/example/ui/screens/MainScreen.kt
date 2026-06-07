package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import com.example.data.Task
import com.example.ui.StatusFilter
import com.example.ui.TaskViewModel
import java.text.SimpleDateFormat
import java.util.*

val VazirmatnFontFamily = FontFamily.Default

sealed class AppScreen {
    object Dashboard : AppScreen()
    object Schedule : AppScreen()
    object Chat : AppScreen()
    object Settings : AppScreen()
    data class AddEdit(val task: Task? = null) : AppScreen()
    data class TaskDetail(val task: Task) : AppScreen()
}

// --- COLOR SYSTEM ---
data class ThemeColors(
    val bg: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val dockBg: Color,
    val dockIcon: Color,
    val dockActiveBg: Color,
    val dockActiveIcon: Color,
    val accent: Color,
    val taskColors: List<Color>,
    val onTaskColors: List<Color>
)

val LightTaskColors = listOf(
    Color(0xFFFFFFFF), // 0: White
    Color(0xFFFF3B30), // 1: Red
    Color(0xFFFF9500), // 2: Orange
    Color(0xFFFFCC00), // 3: Yellow
    Color(0xFF97F427), // 4: Lime Green
    Color(0xFF5AC8FA), // 5: Light Blue / Teal
    Color(0xFF6500FF), // 6: Purple
    Color(0xFFFF2D55), // 7: Pink
    Color(0xFF00C7BE), // 8: Mint
    Color(0xFF007AFF)  // 9: Blue
)

val LightOnTaskColors = listOf(
    Color.Black, Color.White, Color.White, Color.Black, Color.Black, 
    Color.Black, Color.White, Color.White, Color.Black, Color.White
)

val DarkTaskColors = listOf(
    Color(0xFFFFFFFF), // 0: White
    Color(0xFFFF3B30), // 1: Red
    Color(0xFFFF9500), // 2: Orange
    Color(0xFFFFCC00), // 3: Yellow
    Color(0xFF97F427), // 4: Lime Green
    Color(0xFF5AC8FA), // 5: Light Blue / Teal
    Color(0xFF6500FF), // 6: Purple
    Color(0xFFFF2D55), // 7: Pink
    Color(0xFF00C7BE), // 8: Mint
    Color(0xFF007AFF)  // 9: Blue
)

val DarkOnTaskColors = listOf(
    Color.Black, Color.White, Color.White, Color.Black, Color.Black, 
    Color.Black, Color.White, Color.White, Color.Black, Color.White
)

val LightTheme = ThemeColors(
    bg = Color(0xFFF5F5F7), // Light clean grey background
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEAEAEE),
    textPrimary = Color(0xFF1C1C1E),
    textSecondary = Color(0xFF8E8E93),
    dockBg = Color(0xFF222222), // Dark dock
    dockIcon = Color(0xFF8E8E93), // In-active dark dock icon
    dockActiveBg = Color(0xFFFFFFFF), // Active dock item bg
    dockActiveIcon = Color(0xFF1C1C1E), // Active dock item icon
    accent = Color(0xFF1C1C1E),
    taskColors = LightTaskColors,
    onTaskColors = LightOnTaskColors
)

val DarkTheme = ThemeColors(
    bg = Color(0xFF151517), // Deep dark bg
    surface = Color(0xFF242426),
    surfaceVariant = Color(0xFF323236),
    textPrimary = Color(0xFFF2F2F7),
    textSecondary = Color(0xFFAEAEB2),
    dockBg = Color(0xFF222222), // Consistent dock
    dockIcon = Color(0xFF8E8E93),
    dockActiveBg = Color(0xFFFFFFFF),
    dockActiveIcon = Color(0xFF1C1C1E),
    accent = Color(0xFFFFFFFF),
    taskColors = DarkTaskColors,
    onTaskColors = DarkOnTaskColors
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainScreen(viewModel: TaskViewModel, modifier: Modifier = Modifier) {
    val systemDark = isSystemInDarkTheme()
    var isDarkMode by remember { mutableStateOf(systemDark) }
    val colors = if (isDarkMode) DarkTheme else LightTheme

    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Dashboard) }

    val context = LocalContext.current
    val vazirmatnFontFamily = remember(context) {
        try {
            val regular = androidx.core.content.res.ResourcesCompat.getFont(context, com.example.R.font.vazirmatn)
            val bold = androidx.core.content.res.ResourcesCompat.getFont(context, com.example.R.font.vazirmatn_bold)
            if (regular != null && bold != null) {
                FontFamily(
                    Font(com.example.R.font.vazirmatn, FontWeight.Normal),
                    Font(com.example.R.font.vazirmatn_bold, FontWeight.Bold)
                )
            } else {
                FontFamily.Default
            }
        } catch (e: Throwable) {
            FontFamily.Default
        }
    }

    BackHandler(enabled = currentScreen != AppScreen.Dashboard) {
        currentScreen = AppScreen.Dashboard
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl
    ) {
        ProvideTextStyle(value = androidx.compose.ui.text.TextStyle(fontFamily = vazirmatnFontFamily)) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(colors.bg)
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 0.95f, animationSpec = tween(400))) togetherWith
                            (fadeOut(animationSpec = tween(400)) + scaleOut(targetScale = 1.05f, animationSpec = tween(400)))
                },
                label = "ScreenSwitch"
            ) { screen ->
                when (screen) {
                    is AppScreen.Dashboard -> DashboardScreen(viewModel, colors, onNavigate = { currentScreen = it })
                    is AppScreen.Schedule -> ScheduleScreen(viewModel, colors, onNavigate = { currentScreen = it }, onBack = { currentScreen = AppScreen.Dashboard })
                    is AppScreen.Chat -> ChatScreen(viewModel, colors, onBack = { currentScreen = AppScreen.Dashboard })
                    is AppScreen.Settings -> SettingsScreen(viewModel, colors, isDarkMode, { isDarkMode = it }, { currentScreen = AppScreen.Dashboard })
                    is AppScreen.AddEdit -> AddEditScreen(screen.task, viewModel, colors) { currentScreen = AppScreen.Dashboard }
                    is AppScreen.TaskDetail -> TaskDetailScreen(screen.task, viewModel, colors, { currentScreen = AppScreen.Dashboard }, { currentScreen = AppScreen.AddEdit(screen.task) })
                }
            }

            // FLOATING DOCK (Navigation Bar)
            if (currentScreen !is AppScreen.AddEdit && currentScreen !is AppScreen.TaskDetail) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    DockNavigation(
                        currentScreen = currentScreen,
                        colors = colors,
                        onNavigate = { currentScreen = it }
                    )
                }
            }
            
            // PENDING AI ACTIONS CONFIRMATION
            var isRejecting by remember { mutableStateOf(false) }
            var rejectFeedback by remember { mutableStateOf("") }
            val pendingCommands by viewModel.pendingCommands.collectAsStateWithLifecycle()
            if (pendingCommands != null && pendingCommands!!.isNotEmpty()) {
                if (isRejecting) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { viewModel.rejectPendingCommands(); isRejecting = false },
                        containerColor = colors.surface,
                        titleContentColor = colors.textPrimary,
                        textContentColor = colors.textSecondary,
                        title = { Text("چرا رد شدن؟ (اختیاری)", fontWeight = FontWeight.Bold) },
                        text = {
                            androidx.compose.material3.OutlinedTextField(
                                value = rejectFeedback,
                                onValueChange = { rejectFeedback = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("مثلا: دو تا تسک اول اضافه بودن...") },
                                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colors.accent,
                                    unfocusedBorderColor = colors.textSecondary.copy(alpha=0.5f),
                                    focusedTextColor = colors.textPrimary,
                                    unfocusedTextColor = colors.textPrimary
                                )
                            )
                        },
                        confirmButton = {
                            androidx.compose.material3.Button(onClick = { 
                                viewModel.rejectPendingCommands(rejectFeedback)
                                isRejecting = false
                                rejectFeedback = ""
                            }) {
                                Text("ثبت و رد")
                            }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(onClick = { 
                                viewModel.rejectPendingCommands()
                                isRejecting = false
                                rejectFeedback = ""
                            }) {
                                Text("رد بدون دلیل")
                            }
                        }
                    )
                } else {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { isRejecting = true },
                        containerColor = colors.surface,
                        titleContentColor = colors.textPrimary,
                        textContentColor = colors.textSecondary,
                        title = { Text("تأیید کارهای جدید", fontWeight = FontWeight.Bold) },
                        text = {
                            LazyColumn {
                                items(pendingCommands!!.size) { i ->
                                    val cmd = pendingCommands!![i]
                                    val actionName = when (cmd.command) {
                                        "ADD_TASK" -> "افزودن تسک: "
                                        "UPDATE_TASK" -> "بروزرسانی تسک: "
                                        "COMPLETE_TASK" -> "تکمیل تسک: "
                                        "DELETE_TASK" -> "حذف تسک: "
                                        else -> "اکشن: "
                                    }
                                    Text("${i + 1}. $actionName${cmd.title.ifEmpty { cmd.command }}", modifier = Modifier.padding(bottom = 8.dp))
                                }
                            }
                        },
                        confirmButton = {
                            androidx.compose.material3.Button(onClick = { viewModel.acceptPendingCommands() }) {
                                Text("آره، انجامش بده")
                            }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(onClick = { isRejecting = true }) {
                                Text("نه، رد کن")
                            }
                        }
                    )
                }
            } else {
                if (isRejecting) isRejecting = false
            }
        }
        }
    }
}

// ===================================
// DOCK NAVIGATION
// ===================================
@Composable
fun DockNavigation(currentScreen: AppScreen, colors: ThemeColors, onNavigate: (AppScreen) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 16.dp, start = 32.dp, end = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .shadow(16.dp, CircleShape, spotColor = Color.Black.copy(alpha=0.3f))
                .clip(CircleShape)
                .background(colors.dockBg)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                DockItem(
                    icon = Icons.Default.DateRange,
                    isActive = currentScreen is AppScreen.Schedule,
                    colors = colors,
                    onClick = { onNavigate(AppScreen.Schedule) }
                )
                DockItem(
                    icon = Icons.Default.List,
                    isActive = currentScreen is AppScreen.Dashboard,
                    colors = colors,
                    onClick = { onNavigate(AppScreen.Dashboard) }
                )
                DockItem(
                    icon = Icons.Default.Face, // Chat/AI
                    isActive = currentScreen is AppScreen.Chat,
                    colors = colors,
                    onClick = { onNavigate(AppScreen.Chat) }
                )
            }
        }
    }
}

@Composable
fun DockItem(icon: ImageVector, isActive: Boolean, colors: ThemeColors, onClick: () -> Unit) {
    val scale by animateFloatAsState(targetValue = if (isActive) 1.2f else 1f, label = "", animationSpec = tween(300))
    
    Box(
        modifier = Modifier
            .size(44.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(if (isActive) colors.dockActiveBg else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) colors.dockActiveIcon else colors.dockIcon,
            modifier = Modifier.size(28.dp)
        )
    }
}


// ===================================
// DASHBOARD SCREEN
// ===================================
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: TaskViewModel, colors: ThemeColors, onNavigate: (AppScreen) -> Unit) {
    val tasks by viewModel.filteredAndSortedTasks.collectAsStateWithLifecycle()
    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val statusFilter by viewModel.statusFilter.collectAsStateWithLifecycle()
    
    val unreadNotificationsCount by viewModel.unreadNotificationsCount.collectAsStateWithLifecycle()
    val allNotifications by viewModel.allNotifications.collectAsStateWithLifecycle()
    
    var searchQuery by remember { mutableStateOf("") }
    var expandNotifications by remember { mutableStateOf(false) }
    
    val filteredBySearch = if (searchQuery.isBlank()) tasks else tasks.filter {
        it.title.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true)
    }

    val pendingCount = allTasks.count { it.status == "Pending" }
    
    val allAvailableFolders = remember(tasks) {
        tasks.mapNotNull { it.folderName.takeIf { f -> !f.isNullOrBlank() } }.distinct()
    }
    var selectedFolders by remember { mutableStateOf(emptySet<String>()) }
    
    val filteredBySearchAndFolder = filteredBySearch.filter {
        if (selectedFolders.isEmpty()) true
        else {
            val fName = it.folderName
            if (fName.isNullOrBlank()) selectedFolders.contains("بدون فولدر") else selectedFolders.contains(fName)
        }
    }
    
    // Group tasks by folder (null/empty becomes "بدون فولدر")
    val groupedTasks = filteredBySearchAndFolder.groupBy { 
        if (it.folderName.isNullOrBlank()) "بدون فولدر" else it.folderName!!
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 24.dp, bottom = 140.dp)
        ) {
            item {
                // Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile (Clicks to settings)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .shadow(8.dp, CircleShape, spotColor = Color.Black.copy(alpha=0.15f))
                            .clip(CircleShape)
                            .background(colors.surface)
                            .clickable { onNavigate(AppScreen.Settings) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = "Profile", tint = colors.textPrimary)
                    }
                    
                    // End Actions
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .shadow(8.dp, CircleShape, spotColor = Color.Black.copy(alpha=0.15f))
                                .clip(CircleShape)
                                .background(colors.surface)
                                .clickable { onNavigate(AppScreen.AddEdit()) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Task", tint = colors.textPrimary)
                        }
                        
                        // Bell with Badge with dynamic real SQLite unread count
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .shadow(8.dp, CircleShape, spotColor = Color.Black.copy(alpha=0.15f))
                                .clip(CircleShape)
                                .background(colors.surface)
                                .clickable { expandNotifications = !expandNotifications },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = colors.textPrimary)
                            if (unreadNotificationsCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = (-2).dp, y = 2.dp)
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(Color.Red),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$unreadNotificationsCount",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(24.dp)) }

            item {
                // Search Bar
                Box(modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth().shadow(8.dp, RoundedCornerShape(26.dp), spotColor = Color.Black.copy(alpha=0.1f)).clip(RoundedCornerShape(26.dp))) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("جستجو...", color = colors.textSecondary) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = colors.textSecondary) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = colors.surface,
                            unfocusedContainerColor = colors.surface,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        ),
                        singleLine = true
                    )
                }
            }
            
            item { Spacer(modifier = Modifier.height(24.dp)) }

            // FOLDER FILTERS
            if (allAvailableFolders.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val allFoldersWithNone = listOf("بدون فولدر") + allAvailableFolders
                        for (folder in allFoldersWithNone) {
                            val isSelected = selectedFolders.contains(folder)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedFolders = if (isSelected) {
                                        selectedFolders - folder
                                    } else {
                                        selectedFolders + folder
                                    }
                                },
                                label = { Text(folder, fontWeight = FontWeight.Medium) },
                                colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.taskColors[4].copy(alpha = 0.2f),
                                    selectedLabelColor = colors.textPrimary,
                                    labelColor = colors.textSecondary
                                ),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.material3.FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) colors.taskColors[4] else colors.textSecondary.copy(alpha=0.3f)
                                )
                            )
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }

            // SORT MODES
            item {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val currentSort by viewModel.sortMode.collectAsStateWithLifecycle()
                    
                    FilterPill("بالانس", 0, currentSort == com.example.ui.SortMode.BALANCE, colors) { viewModel.setSortMode(com.example.ui.SortMode.BALANCE) }
                    FilterPill("مهم‌ترین", 0, currentSort == com.example.ui.SortMode.PRIORITY_HIGHEST, colors) { viewModel.setSortMode(com.example.ui.SortMode.PRIORITY_HIGHEST) }
                    FilterPill("سریع‌ترین", 0, currentSort == com.example.ui.SortMode.ESTIMATED_FASTEST, colors) { viewModel.setSortMode(com.example.ui.SortMode.ESTIMATED_FASTEST) }
                    FilterPill("نزدیکترین", 0, currentSort == com.example.ui.SortMode.DEADLINE_SOONEST, colors) { viewModel.setSortMode(com.example.ui.SortMode.DEADLINE_SOONEST) }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            if (filteredBySearch.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 80.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .shadow(32.dp, CircleShape, spotColor = colors.taskColors[4])
                                .clip(CircleShape)
                                .background(colors.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription=null, tint=colors.taskColors[4], modifier=Modifier.size(48.dp))
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("تسکی نیست", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Text("برو خوپیش 😴", color = colors.textSecondary, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            } else {
                groupedTasks.forEach { (folderName, tasksInFolder) ->
                    item {
                        Text(
                            text = folderName,
                            color = colors.textPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                    }
                    
                    items(tasksInFolder, key = { it.id }) { task ->
                        val cardColor = colors.taskColors.getOrNull(task.colorIndex) ?: colors.taskColors[0]
                        val onCardColor = colors.onTaskColors.getOrNull(task.colorIndex) ?: colors.onTaskColors[0]
                        
                        Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                            AnimatedTaskCard(task, cardColor, onCardColor, colors, onNavigate = { onNavigate(AppScreen.TaskDetail(task)) }, onToggle = { viewModel.toggleTaskComplete(task) })
                        }
                    }
                    
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
        
        // Notifications Dropdown SQLite-backed container (Real list)
        AnimatedVisibility(
            visible = expandNotifications,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp).padding(horizontal = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(24.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha=0.3f))
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.surface)
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(), 
                        horizontalArrangement = Arrangement.SpaceBetween, 
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("اعلان‌ها و یادآوری‌ها", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (allNotifications.any { !it.isRead }) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Mark all read",
                                    tint = colors.taskColors[4],
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable { viewModel.markAllNotificationsAsRead() }
                                )
                            }
                            if (allNotifications.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Clear all",
                                    tint = colors.textSecondary,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable { viewModel.clearAllNotifications() }
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = colors.textSecondary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { expandNotifications = false }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (allNotifications.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Notifications, contentDescription = null, tint = colors.textSecondary.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("هیچ اعلانی یافت نشد. همه کارات رو رواله! ⚡", color = colors.textSecondary, fontSize = 14.sp)
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                        ) {
                            allNotifications.forEach { notif ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.markNotificationAsRead(notif.id) }
                                        .padding(vertical = 12.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (!notif.isRead) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Color.Red)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(notif.title, color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(notif.body, color = colors.textSecondary, fontSize = 12.sp)
                                    }
                                    
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Mark read",
                                        tint = if (notif.isRead) colors.textSecondary.copy(alpha=0.3f) else colors.taskColors[4],
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                HorizontalDivider(color = colors.surfaceVariant.copy(alpha=0.5f), thickness = 1.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterPill(label: String, badgeCount: Int, isActive: Boolean, colors: ThemeColors, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(40.dp)
            .shadow(if(isActive) 8.dp else 2.dp, RoundedCornerShape(20.dp), spotColor = if(isActive) colors.taskColors[4] else Color.Black)
            .clip(RoundedCornerShape(20.dp))
            .background(if (isActive) colors.taskColors[4] else colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = if(isActive) Color.Black else colors.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        if (badgeCount > 0) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier.size(24.dp).clip(CircleShape).background(if(isActive) Color.White.copy(alpha=0.5f) else colors.bg),
                contentAlignment = Alignment.Center
            ) {
                Text(badgeCount.toString(), color = if(isActive) Color.Black else colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

// A beautiful overlapping card mimicking the reference image
@Composable
fun AnimatedTaskCard(task: Task, blockColor: Color, onBlockColor: Color, colors: ThemeColors, onNavigate: () -> Unit, onToggle: () -> Unit) {
    val textColor = onBlockColor
    val mutedTextColor = onBlockColor.copy(alpha = 0.7f)
    val pillBg = onBlockColor.copy(alpha = 0.15f)
    
    val isDone = task.status == "Completed"
    
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.6f, stiffness = 400f)
    )

    Column(modifier = Modifier.fillMaxWidth().scale(scale)) {
        // Main Upper Block
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(if (isPressed) 4.dp else 16.dp, RoundedCornerShape(28.dp), spotColor = if(isDone) colors.textSecondary else blockColor)
                .clip(RoundedCornerShape(28.dp))
                .background(if(isDone) colors.surfaceVariant else blockColor)
                .clickable(interactionSource = interactionSource, indication = androidx.compose.foundation.LocalIndication.current) { onNavigate() }
                .padding(24.dp)
        ) {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Text(
                        "${task.emoji} ${task.title}",
                        color = if(isDone) colors.textPrimary else textColor,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 28.sp,
                        modifier = Modifier.weight(1f).padding(end = 16.dp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // Arrow button
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White).clickable { onToggle() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDone) {
                            Icon(Icons.Default.Check, contentDescription = "Done", tint = Color.Black)
                        } else {
                            Icon(Icons.Default.ArrowForward, contentDescription = "View", tint = Color.Black, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = if(isDone) colors.textSecondary else mutedTextColor, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (task.timeEstimateText.isNotBlank()) task.timeEstimateText else com.example.ui.utils.JalaliDateConverter.formatJalali(task.deadline),
                            color = if(isDone) colors.textSecondary else mutedTextColor, 
                            fontSize = 13.sp, 
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    // Priority Pill
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(if(isDone) colors.surface else pillBg).padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        val prioText = if((task.importanceScore + task.urgencyScore) / 2 > 75) "فوری" else if((task.importanceScore + task.urgencyScore) / 2 > 40) "عادی" else "پایین"
                        Text(prioText, color = if(isDone) colors.textPrimary else textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ===================================
// SCHEDULE SCREEN (Timeline View)
// ===================================
@Composable
fun ScheduleScreen(viewModel: TaskViewModel, colors: ThemeColors, onNavigate: (AppScreen) -> Unit, onBack: () -> Unit) {
    val tasks by viewModel.allTasks.collectAsStateWithLifecycle()
    
    // Sort tasks by closest deadline
    val sortedTasks = tasks.sortedBy { it.deadline }

    Column(modifier = Modifier.fillMaxSize()) {
        // App Bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(colors.surface).clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
            }

            Text("تسک‌های امروز", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)

            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(colors.taskColors[0]).clickable { onNavigate(AppScreen.AddEdit()) },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = colors.onTaskColors[0])
            }
        }

        // Calendar Week Strip
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val days = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
            val dates = listOf("1", "2", "3", "4", "5", "6", "7")
            
            for (i in 0..6) {
                val isActive = i == 3 // Mock active day
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(days[i], color = colors.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isActive) colors.taskColors[4] else Color.Transparent), // soft green mock
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            dates[i], 
                            color = if(isActive) colors.onTaskColors[4] else colors.textPrimary, 
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = colors.surfaceVariant, thickness = 1.dp)

        // Timeline Area
        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 24.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 140.dp)
        ) {
            val times = listOf("۰۸ ق.ظ", "۰۹ ق.ظ", "۱۰ ق.ظ", "۱۱ ق.ظ", "۱۲ ب.ظ", "۰۱ ب.ظ", "۰۲ ب.ظ", "۰۳ ب.ظ")
            
            itemsIndexed(times) { index, time ->
                Row(
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        time, 
                        color = colors.textSecondary, 
                        fontSize = 12.sp, 
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(50.dp).padding(top = 8.dp)
                    )
                    
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        // Background line
                        HorizontalDivider(
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = 15.dp),
                            color = colors.surfaceVariant, 
                            thickness = 1.dp
                        )
                        
                        // Mapping dynamically based on sorted Tasks mock
                        val eventTask = sortedTasks.getOrNull(index)
                        if (eventTask != null && index % 2 == 0) { // arbitrary spacing
                            val bgColor = colors.taskColors.getOrNull(eventTask.colorIndex) ?: colors.taskColors[0]
                            val contentColor = colors.onTaskColors.getOrNull(eventTask.colorIndex) ?: colors.onTaskColors[0]
                            TimelineEventCard(eventTask, bgColor, contentColor, time, Modifier.offset(y = 15.dp).height(50.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineEventCard(task: Task, bgColor: Color, contentColor: Color, timeRange: String, modifier: Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(percent = 50))
            .background(bgColor)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${task.emoji} ${task.title}",
                color = contentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            )
            
            Box(
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).border(1.dp, contentColor.copy(alpha=0.3f), RoundedCornerShape(12.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(timeRange, color = contentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}


// ===================================
// CHAT SCREEN (AI Assistant)
// ===================================
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(viewModel: TaskViewModel, colors: ThemeColors, onBack: () -> Unit) {
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    var input by remember { mutableStateOf("") }
    val scrollState = rememberLazyListState()
    var isChatMode by remember { mutableStateOf(false) }

    LaunchedEffect(messages.size, isGenerating) {
        if (messages.isNotEmpty()) {
            scrollState.animateScrollToItem(messages.size)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(colors.surface).clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text("برده", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        }

        LazyColumn(
            state = scrollState,
            modifier = Modifier.weight(1f).padding(horizontal = 24.dp),
            contentPadding = PaddingValues(bottom = 160.dp, top = 8.dp)
        ) {
            itemsIndexed(messages) { _, msg ->
                val isUser = msg.sender == "user"
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = if (isUser) Arrangement.Start else Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .clip(RoundedCornerShape(
                                topStart = 24.dp, topEnd = 24.dp,
                                bottomEnd = if (isUser) 24.dp else 4.dp,
                                bottomStart = if (isUser) 4.dp else 24.dp
                            ))
                            .background(if (isUser) colors.taskColors[0] else colors.surface)
                            .padding(20.dp)
                    ) {
                        Text(
                            msg.text,
                            color = if (isUser) Color.White else colors.textPrimary,
                            lineHeight = 22.sp,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Justify
                        )
                    }
                }
            }
            if (isGenerating) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(24.dp)).background(colors.surface).padding(20.dp)) {
                            Text("در حال نوشتن...", color = colors.textSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().navigationBarsPadding(), contentAlignment = Alignment.BottomCenter) {
        Row(
            modifier = Modifier
                .padding(bottom = 80.dp, start = 24.dp, end = 24.dp)
                .fillMaxWidth()
                .height(64.dp)
                .shadow(16.dp, CircleShape, spotColor = Color.Black.copy(alpha=0.3f))
                .clip(CircleShape)
                .background(colors.surface)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f).padding(start = 12.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = colors.textPrimary,
                    cursorColor = colors.accent
                ),
                placeholder = { Text(if (isChatMode) "چت کن..." else "چه تسکی باید انجام شه؟", color = colors.textSecondary, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                maxLines = 1
            )
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(if (isChatMode) colors.taskColors[6] else colors.taskColors[4])
                    .combinedClickable(
                        onClick = {
                            if (input.isNotBlank() && !isGenerating) {
                                viewModel.sendChatMessage(input, isChatMode)
                                input = ""
                            }
                        },
                        onLongClick = {
                            isChatMode = !isChatMode
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.animation.Crossfade(targetState = isChatMode, label = "ModeIcon") { mode ->
                    if (mode) {
                        Icon(Icons.Default.Face, contentDescription = "Chat Mode", tint = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Task Mode", tint = Color.Black, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

// ===================================
// SETTINGS SCREEN
// ===================================
@Composable
fun SettingsScreen(
    viewModel: TaskViewModel,
    colors: ThemeColors,
    isDarkMode: Boolean,
    onDarkModeToggle: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var backupJson by remember { mutableStateOf("") }
    
    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                val json = viewModel.exportTasksToJson()
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(json) }
                Toast.makeText(context, "فایل با موفقیت ذخیره شد!", Toast.LENGTH_SHORT).show()
                viewModel.addNotificationToHistoryAndTriggerSystem(
                    title = "📤 پشتیبان‌گیری موفق فایل",
                    body = "یک نسخه پشتیبان از کارهای شما به صورت فایل با موفقیت ذخیره شد، داشیییی! ⚡",
                    type = "success"
                )
            } catch (e: Exception) {
                Toast.makeText(context, "خطا در ذخیره فایل", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                backupJson = json
                if (json.isNotBlank()) {
                    val success = viewModel.importTasksFromJson(json)
                    if (success) Toast.makeText(context, "با موفقیت وارد شد!", Toast.LENGTH_SHORT).show()
                    else Toast.makeText(context, "فرمت نامعتبر!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "خطا در خواندن فایل!", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(colors.surface).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text("تنظیمات", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        }

        Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
            
            // Dark Mode
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(colors.surface).padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("حالت تاریک", color = colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Switch(checked = isDarkMode, onCheckedChange = onDarkModeToggle, colors = SwitchDefaults.colors(checkedThumbColor = colors.taskColors[4], checkedTrackColor = colors.taskColors[0]))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Export
            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(colors.taskColors[6]).padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("خروجی اطلاعات", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("ساخت فایل JSON یا کپی تسک‌ها", color = Color.White.copy(alpha=0.8f), fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm", java.util.Locale.ENGLISH)
                            val dateStr = sdf.format(java.util.Date())
                            exportLauncher.launch("my-task-$dateStr.json")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("ذخیره فایل", color = Color.White)
                    }
                    Button(
                        onClick = {
                            val json = viewModel.exportTasksToJson()
                            clipboardManager.setText(AnnotatedString(json))
                            Toast.makeText(context, "اطلاعات کپی شد!", Toast.LENGTH_SHORT).show()
                            viewModel.addNotificationToHistoryAndTriggerSystem(
                                title = "📋 کپی کدهای پشتیبان",
                                body = "کدهای پشتیبان تفصیلی تسک‌هایت در حافظه موقت کپی شد، داشیییی! ⚡",
                                type = "success"
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("کپی متن", color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Import
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(colors.surface).padding(24.dp)) {
                Column {
                    Text("ورود اطلاعات", color = colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.taskColors[4]),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("انتخاب فایل JSON", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("یا متن JSON را پیست کنید:", color = colors.textSecondary, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = backupJson,
                        onValueChange = { backupJson = it },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        placeholder = { Text("کد JSON خود را اینجا پیست کنید...", color = colors.textSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.surfaceVariant,
                            focusedTextColor = colors.textPrimary
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (backupJson.isNotBlank()) {
                                val success = viewModel.importTasksFromJson(backupJson)
                                if (success) Toast.makeText(context, "با موفقیت وارد شد!", Toast.LENGTH_SHORT).show()
                                else Toast.makeText(context, "فرمت نامعتبر!", Toast.LENGTH_SHORT).show()
                                backupJson = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.taskColors[0]),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("بازگردانی", color = Color.White)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(140.dp))
        }
    }
}

// ===================================
// ADD / EDIT SCREEN
// ===================================
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AddEditScreen(taskToEdit: Task?, viewModel: TaskViewModel, colors: ThemeColors, onBack: () -> Unit) {
    var title by remember { mutableStateOf(taskToEdit?.title ?: "") }
    var description by remember { mutableStateOf(taskToEdit?.description ?: "") }
    var durationText by remember { mutableStateOf(taskToEdit?.timeEstimateText ?: "") }
    var emoji by remember { mutableStateOf(taskToEdit?.emoji ?: "📝") }
    var folderName by remember { mutableStateOf(taskToEdit?.folderName ?: "") }
    var colorIndex by remember { mutableStateOf(taskToEdit?.colorIndex ?: 0) }
    var deadlineMode by remember { mutableStateOf(if (taskToEdit != null && taskToEdit.deadline > 0L) true else false) }
    var deadlineTimestamp by remember { mutableStateOf(taskToEdit?.deadline ?: 0L) }
    var importanceScore by remember { mutableStateOf(taskToEdit?.importanceScore?.toFloat() ?: 50f) }
    var urgencyScore by remember { mutableStateOf(taskToEdit?.urgencyScore?.toFloat() ?: 50f) }

    val initialSubtasks = description.split("\n").filter { it.isNotBlank() }
    val subtasks = remember { androidx.compose.runtime.mutableStateListOf<String>().apply { addAll(initialSubtasks) } }
    var newSubtask by remember { mutableStateOf("") }

    var showTimeSheet by remember { mutableStateOf(false) }
    var showFolderSheet by remember { mutableStateOf(false) }

    val timeSheetState = androidx.compose.material3.rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val folderSheetState = androidx.compose.material3.rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
    if (showTimeSheet) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showTimeSheet = false },
            sheetState = timeSheetState,
            containerColor = colors.bg
        ) {
            var timeTab by remember { mutableStateOf(if (deadlineMode) 1 else 0) }
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
                // Tab Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surface)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (timeTab == 0) colors.taskColors[4] else Color.Transparent)
                            .clickable { timeTab = 0 }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "مدت زمان لازم",
                            color = if (timeTab == 0) Color.Black else colors.textSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (timeTab == 1) colors.taskColors[4] else Color.Transparent)
                            .clickable { timeTab = 1 }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ددلاین مشخص (تقویم)",
                            color = if (timeTab == 1) Color.Black else colors.textSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                // Apple/Google Slate No-Time-Limit alternative button for unconstrained actions
                androidx.compose.material3.Button(
                    onClick = {
                        durationText = "بدون زمان مشخص"
                        deadlineMode = false
                        deadlineTimestamp = 0L
                        showTimeSheet = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = colors.surface.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = colors.textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "بدون زمان مشخص (تسک بدون ددلاین)",
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                if (timeTab == 0) {
                    // DURATION - APPLE-STYLE SCROLL WHEEL PICKERS
                    var dDays by remember { mutableStateOf(0) }
                    var dHours by remember { mutableStateOf(1) }
                    var dMinutes by remember { mutableStateOf(30) }

                    Text(
                        text = "مدت زمان مورد نیاز برای انجام تسک را مشخص کنید:",
                        color = colors.textSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Display columns side by side
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(colors.surface)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Days picker
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "روز",
                                color = colors.textSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            val daysList = (0..30).map { "$it" }
                            WheelPicker(
                                items = daysList,
                                initialIndex = dDays,
                                onIndexSelected = { dDays = it },
                                textColor = colors.textPrimary,
                                selectedColor = colors.taskColors[4],
                                isLoop = false
                            )
                        }

                        // Hours picker
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "ساعت",
                                color = colors.textSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            val hoursList = (0..23).map { "$it" }
                            WheelPicker(
                                items = hoursList,
                                initialIndex = dHours,
                                onIndexSelected = { dHours = it },
                                textColor = colors.textPrimary,
                                selectedColor = colors.taskColors[4],
                                isLoop = true
                            )
                        }

                        // Minutes picker
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "دقیقه",
                                color = colors.textSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            val minutesList = (0..59).map { "$it" }
                            WheelPicker(
                                items = minutesList,
                                initialIndex = dMinutes,
                                onIndexSelected = { dMinutes = it },
                                textColor = colors.textPrimary,
                                selectedColor = colors.taskColors[4],
                                isLoop = true
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Live Preview description of the selected duration
                    val durationPreview = remember(dDays, dHours, dMinutes) {
                        buildString {
                            if (dDays > 0) append("$dDays روز")
                            if (dHours > 0) {
                                if (isNotEmpty()) append(" و ")
                                append("$dHours ساعت")
                            }
                            if (dMinutes > 0) {
                                if (isNotEmpty()) append(" و ")
                                append("$dMinutes دقیقه")
                            }
                            if (isEmpty()) append("۳۰ دقیقه")
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.taskColors[4].copy(alpha = 0.1f))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "مدت برگزیده: $durationPreview",
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    androidx.compose.material3.Button(
                        onClick = {
                            durationText = durationPreview
                            deadlineMode = false
                            showTimeSheet = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = colors.taskColors[4]
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            "تنظیم مدت انجام",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                } else {
                    // IRANIAN (SHAMSI) CALENDAR GRID VIEW - BEAUTIFUL, MODERN, UNIFIED
                    val todayShamsi = remember { getCurrentShamsiDate() }
                    val currentShamsiYear = todayShamsi.first
                    val currentShamsiMonthIdx = (todayShamsi.second - 1).coerceIn(0, 11)
                    val currentShamsiDay = todayShamsi.third

                    var selectedYear by remember { mutableStateOf(currentShamsiYear) }
                    var selectedMonthIndex by remember { mutableStateOf(currentShamsiMonthIdx) }
                    var selectedDay by remember { mutableStateOf(currentShamsiDay) }
                    var selectedHour by remember { mutableStateOf(12) }
                    var selectedMinute by remember { mutableStateOf(0) }

                    val shamsiMonths = remember {
                        listOf("فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور", "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند")
                    }
                    val selectedMonthName = shamsiMonths[selectedMonthIndex]

                    // Calculate shamsi leap year
                    fun isShamsiLeapYear(y: Int): Boolean {
                        val r = (y - 474) % 2820
                        val l = (r + 38) * 31 % 128
                        return l < 31
                    }

                    // Get total days in month
                    val daysInSelectedMonth = when {
                        selectedMonthIndex < 6 -> 31
                        selectedMonthIndex < 11 -> 30
                        else -> if (isShamsiLeapYear(selectedYear)) 30 else 29
                    }

                    // Get first day weekday offset (0 = Sat, ..., 6 = Fri)
                    fun getFirstDayWeekday(y: Int, m: Int): Int {
                        val converter = com.example.ui.utils.JalaliDateConverter()
                        val gregorianTime = converter.jalaliToGregorian(y, m, 1)
                        val c = java.util.Calendar.getInstance().apply {
                            timeInMillis = gregorianTime
                        }
                        return when (c.get(java.util.Calendar.DAY_OF_WEEK)) {
                            java.util.Calendar.SATURDAY -> 0
                            java.util.Calendar.SUNDAY -> 1
                            java.util.Calendar.MONDAY -> 2
                            java.util.Calendar.TUESDAY -> 3
                            java.util.Calendar.WEDNESDAY -> 4
                            java.util.Calendar.THURSDAY -> 5
                            java.util.Calendar.FRIDAY -> 6
                            else -> 0
                        }
                    }

                    val firstDayOffset = remember(selectedYear, selectedMonthIndex) {
                        getFirstDayWeekday(selectedYear, selectedMonthIndex + 1)
                    }

                    // Reset selectedDay if it's out of range for the new month
                    LaunchedEffect(daysInSelectedMonth) {
                        if (selectedDay > daysInSelectedMonth) {
                            selectedDay = daysInSelectedMonth
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        // Month & Year Selector Banner
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.IconButton(
                                onClick = {
                                    if (selectedMonthIndex > 0) {
                                        selectedMonthIndex--
                                    } else {
                                        selectedMonthIndex = 11
                                        selectedYear--
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Previous Month",
                                    tint = colors.textPrimary
                                )
                            }

                            Text(
                                text = "$selectedMonthName $selectedYear",
                                color = colors.textPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )

                            androidx.compose.material3.IconButton(
                                onClick = {
                                    if (selectedMonthIndex < 11) {
                                        selectedMonthIndex++
                                    } else {
                                        selectedMonthIndex = 0
                                        selectedYear++
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Next Month",
                                    tint = colors.textPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Weekdays Grid Header
                        val weekdays = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            weekdays.forEach { dayName ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayName,
                                        color = if (dayName == "ج") colors.taskColors[1] else colors.textSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Calendar Grid Days Builder (Max 6 rows of 7 days)
                        val totalCells = firstDayOffset + daysInSelectedMonth
                        val totalRows = (totalCells + 6) / 7
                        Column(modifier = Modifier.fillMaxWidth()) {
                            for (rowIndex in 0 until totalRows) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    for (colIndex in 0 until 7) {
                                        val cellIndex = rowIndex * 7 + colIndex
                                        if (cellIndex < firstDayOffset || cellIndex >= totalCells) {
                                            // Empty cell
                                            Box(modifier = Modifier.weight(1f).aspectRatio(1.2f))
                                        } else {
                                            val dayNumber = cellIndex - firstDayOffset + 1
                                            val isSelected = selectedDay == dayNumber
                                            val isToday = currentShamsiYear == selectedYear &&
                                                    currentShamsiMonthIdx == selectedMonthIndex &&
                                                    currentShamsiDay == dayNumber

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .aspectRatio(1.2f)
                                                    .padding(2.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(
                                                        if (isSelected) colors.taskColors[4] else Color.Transparent
                                                    )
                                                    .clickable {
                                                        selectedDay = dayNumber
                                                    }
                                                    .border(
                                                        width = if (isToday && !isSelected) 1.5.dp else 0.dp,
                                                        color = if (isToday) colors.taskColors[4].copy(alpha=0.6f) else Color.Transparent,
                                                        shape = RoundedCornerShape(12.dp)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "$dayNumber",
                                                    color = if (isSelected) Color.Black else colors.textPrimary,
                                                    fontSize = 14.sp,
                                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Unified Clock Selector Under the Calendar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.surface)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = colors.taskColors[4],
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تنظیم ساعت مهلت:",
                                    color = colors.textSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Hour Wheel Picker Box
                                Box(
                                    modifier = Modifier
                                        .width(70.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.bg.copy(alpha=0.6f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val hoursList = (0..23).map { String.format("%02d", it) }
                                    WheelPicker(
                                        items = hoursList,
                                        initialIndex = selectedHour,
                                        onIndexSelected = { selectedHour = hoursList[it].toInt() },
                                        itemHeight = 32.dp,
                                        visibleItemsCount = 1,
                                        textColor = colors.textPrimary,
                                        selectedColor = colors.taskColors[4],
                                        isLoop = true
                                    )
                                }

                                Text(":", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)

                                // Minute Wheel Picker Box
                                Box(
                                    modifier = Modifier
                                        .width(70.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.bg.copy(alpha=0.6f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val minutesList = (0..59).map { String.format("%02d", it) }
                                    WheelPicker(
                                        items = minutesList,
                                        initialIndex = selectedMinute,
                                        onIndexSelected = { selectedMinute = minutesList[it].toInt() },
                                        itemHeight = 32.dp,
                                        visibleItemsCount = 1,
                                        textColor = colors.textPrimary,
                                        selectedColor = colors.taskColors[4],
                                        isLoop = true
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Submit Button
                        androidx.compose.material3.Button(
                            onClick = {
                                durationText = "$selectedDay $selectedMonthName $selectedYear ساعت $selectedHour:${String.format("%02d", selectedMinute)}"
                                deadlineMode = true
                                val finalTs = com.example.ui.utils.JalaliDateConverter().jalaliToGregorian(
                                    selectedYear,
                                    selectedMonthIndex + 1,
                                    selectedDay
                                )
                                val cal = java.util.Calendar.getInstance().apply {
                                    timeInMillis = finalTs
                                    set(java.util.Calendar.HOUR_OF_DAY, selectedHour)
                                    set(java.util.Calendar.MINUTE, selectedMinute)
                                    set(java.util.Calendar.SECOND, 0)
                                }
                                deadlineTimestamp = cal.timeInMillis
                                showTimeSheet = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = colors.taskColors[4]
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("تایید نهایی ددلاین", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
    if (showFolderSheet) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showFolderSheet = false },
            sheetState = folderSheetState,
            containerColor = colors.bg
        ) {
            val allFolders by viewModel.allTasks.collectAsStateWithLifecycle()
            val uniqueFolders = allFolders.mapNotNull { it.folderName }.distinct()
            var newFolderInput by remember { mutableStateOf("") }
            
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
                Text("انتخاب پوشه تسک", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    if (uniqueFolders.isEmpty()) {
                        item {
                            Text("هنوز پوشه‌ای ساخته نشده است.", color = colors.textSecondary, fontSize = 14.sp, modifier = Modifier.padding(vertical = 12.dp))
                        }
                    } else {
                        items(uniqueFolders) { folder ->
                            val isSelected = folder == folderName
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) colors.taskColors[4].copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable { folderName = folder; showFolderSheet = false }
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder, 
                                    contentDescription = null, 
                                    tint = if (isSelected) colors.taskColors[4] else colors.textSecondary
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = folder, 
                                    color = colors.textPrimary, 
                                    fontSize = 16.sp, 
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = "Active", tint = colors.taskColors[4])
                                }
                            }
                        }
                    }
                }
                
                HorizontalDivider(color = colors.surfaceVariant, modifier = Modifier.padding(vertical = 16.dp))
                Text("افزودن پوشه جدید", color = colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = newFolderInput, 
                    onValueChange = { newFolderInput = it },
                    placeholder = { Text("نام پوشه با ایموجی (مثلاً 💼 کارها)", color = colors.textSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        focusedBorderColor = colors.taskColors[4],
                        unfocusedBorderColor = colors.surfaceVariant
                    )
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                androidx.compose.material3.Button(
                    onClick = { folderName = newFolderInput.trim(); showFolderSheet = false },
                    modifier = Modifier.fillMaxWidth().height(48.dp), 
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = colors.taskColors[4]),
                    enabled = newFolderInput.isNotBlank(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("ساختن پوشه جدید", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(colors.surface).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(if (taskToEdit == null) "تسک جدید" else "ویرایش تسک", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            
            Spacer(modifier = Modifier.weight(1f))
            if (taskToEdit != null) {
                Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFFF3B30).copy(alpha=0.1f)).clickable {
                    viewModel.deleteTask(taskToEdit)
                    onBack()
                }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF3B30))
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            // Core
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(colors.surface).padding(24.dp)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = emoji,
                            onValueChange = { if(it.length <= 2) emoji = it },
                            modifier = Modifier.width(64.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                            ),
                            textStyle = LocalTextStyle.current.copy(fontSize = 24.sp, textAlign = TextAlign.Center),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = { Text("عنوان تسک...", color = colors.textSecondary, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = colors.textPrimary,
                            ),
                            textStyle = LocalTextStyle.current.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                    
                    if (subtasks.isNotEmpty()) {
                        HorizontalDivider(color = colors.bg, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))
                        Text("ساب‌تسک‌ها", color = colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                        subtasks.forEachIndexed { index, sub ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Icon(Icons.Default.CheckCircle, contentDescription=null, tint=colors.textSecondary, modifier=Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(sub, color = colors.textPrimary, fontSize = 16.sp, modifier = Modifier.weight(1f))
                                IconButton(onClick = { subtasks.removeAt(index) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Clear, contentDescription="Remove", tint=Color(0xFFFF3B30))
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = colors.bg, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newSubtask,
                            onValueChange = { newSubtask = it },
                            placeholder = { Text("اضافه کردن ساب‌تسک...", color = colors.textSecondary, fontSize = 16.sp) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = colors.textPrimary,
                            )
                        )
                        if (newSubtask.isNotBlank()) {
                            IconButton(onClick = { subtasks.add(newSubtask); newSubtask = "" }) {
                                Icon(Icons.Default.Add, contentDescription="Add", tint=colors.textPrimary)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Sticky Bottom Panel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.bg)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Meta Actions (Calendar and Folder side by side)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(colors.taskColors[colorIndex])
                        .clickable { showTimeSheet = true }
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.DateRange, tint = colors.onTaskColors[colorIndex], contentDescription = "Time")
                        if (durationText.isNotBlank()) {
                            Text(durationText, color = colors.onTaskColors[colorIndex], fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp), maxLines = 1)
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(colors.taskColors[colorIndex])
                        .clickable { showFolderSheet = true }
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Folder, tint = colors.onTaskColors[colorIndex], contentDescription = "Folder")
                        if (folderName.isNotBlank()) {
                            Text(folderName, color = colors.onTaskColors[colorIndex], fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp), maxLines = 1)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Color Selector
            Text("انتخاب رنگ تسک", color = colors.textPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    colors.taskColors.take(5).forEachIndexed { index, color ->
                        ColorDot(index = index, selectedColorIndex = colorIndex, color = color, onSelect = { colorIndex = index })
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    colors.taskColors.drop(5).forEachIndexed { i, color ->
                        val index = i + 5
                        ColorDot(index = index, selectedColorIndex = colorIndex, color = color, onSelect = { colorIndex = index })
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // Priority Slider
            Text("امتیاز اهمیت (۰ تا ۱۰۰): ${importanceScore.toInt()}", color = colors.textPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            androidx.compose.material3.Slider(
                value = importanceScore,
                onValueChange = { importanceScore = it },
                valueRange = 0f..100f,
                colors = androidx.compose.material3.SliderDefaults.colors(
                    thumbColor = colors.taskColors[colorIndex],
                    activeTrackColor = colors.taskColors[colorIndex],
                    inactiveTrackColor = colors.surface
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Urgency Slider
            Text("امتیاز فوریت (۰ تا ۱۰۰): ${urgencyScore.toInt()}", color = colors.textPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            androidx.compose.material3.Slider(
                value = urgencyScore,
                onValueChange = { urgencyScore = it },
                valueRange = 0f..100f,
                colors = androidx.compose.material3.SliderDefaults.colors(
                    thumbColor = colors.taskColors[colorIndex],
                    activeTrackColor = colors.taskColors[colorIndex],
                    inactiveTrackColor = colors.surface
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))
            
            // Save Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(24.dp), spotColor = colors.taskColors[colorIndex])
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.taskColors[colorIndex])
                    .clickable(enabled = title.isNotBlank()) {
                        val finalDescription = subtasks.joinToString("\n")
                        if (taskToEdit == null) {
                            viewModel.addTaskManually(
                                title = title, 
                                description = finalDescription, 
                                deadline = deadlineTimestamp, 
                                priority = importanceScore.toInt(), 
                                urgency = urgencyScore.toInt(),
                                estimatedMinutes = 30, // Default estimate
                                timeEstimateText = durationText,
                                emoji = emoji.ifBlank { "📝" },
                                colorIndex = colorIndex,
                                folderName = folderName.takeIf { it.isNotBlank() }
                            )
                        } else {
                            viewModel.updateTaskManually(
                                taskToEdit.copy(
                                    title = title,
                                    description = finalDescription,
                                    timeEstimateText = durationText,
                                    emoji = emoji.ifBlank { "📝" },
                                    colorIndex = colorIndex,
                                    folderName = folderName.takeIf { it.isNotBlank() },
                                    deadline = deadlineTimestamp,
                                    importanceScore = importanceScore.toInt(),
                                    urgencyScore = urgencyScore.toInt()
                                )
                            )
                        }
                        onBack()
                    }
                    .padding(18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("ذخیره تسک", color = colors.onTaskColors[colorIndex], fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ColorDot(index: Int, selectedColorIndex: Int, color: Color, onSelect: () -> Unit) {
    val isSelected = index == selectedColorIndex
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(color)
            .border(if(isSelected) 3.dp else 0.dp, if(isSelected) Color.White else Color.Transparent, CircleShape)
            .clickable { onSelect() },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black)
        }
    }
}

fun formatShortDate(timestamp: Long): String {
    if (timestamp <= 0L) return "آزاد"
    val sdf = SimpleDateFormat("dd MMM", Locale.ENGLISH)
    return sdf.format(Date(timestamp))
}

// ===================================
// CUSTOM DYNAMIC JALALI DATE CALCULATOR
// ===================================
fun getCurrentShamsiDate(): Triple<Int, Int, Int> {
    val calendar = Calendar.getInstance()
    val gYear = calendar.get(Calendar.YEAR)
    val gMonth = calendar.get(Calendar.MONTH) + 1 // 1-12
    val gDay = calendar.get(Calendar.DAY_OF_MONTH)

    val gLeap = if ((gYear % 4 == 0 && gYear % 100 != 0) || (gYear % 400 == 0)) 1 else 0
    val gDays = intArrayOf(0, 31, 28 + gLeap, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    
    var daysSinceJan1 = 0
    for (i in 1 until gMonth) {
         daysSinceJan1 += gDays[i]
    }
    daysSinceJan1 += gDay
    
    var sYear = gYear - 621
    var sMonth = 1
    var sDay = 1
    
    val farvardin1DayOfYear = if (gLeap == 1) { 81 } else { 80 }
    
    if (daysSinceJan1 >= farvardin1DayOfYear) {
        val daysInShamsi = daysSinceJan1 - farvardin1DayOfYear + 1
        if (daysInShamsi <= 186) {
            sMonth = (daysInShamsi - 1) / 31 + 1
            sDay = (daysInShamsi - 1) % 31 + 1
        } else {
            val remainingDays = daysInShamsi - 186
            sMonth = (remainingDays - 1) / 30 + 7
            sDay = (remainingDays - 1) % 30 + 1
        }
    } else {
        sYear -= 1
        val prevGLeap = if (((gYear - 1) % 4 == 0 && (gYear - 1) % 100 != 0) || ((gYear - 1) % 400 == 0)) 1 else 0
        val totalDaysInPrevG = 365 + prevGLeap
        val prevFarvardin1Day = if (prevGLeap == 1) { 81 } else { 80 }
        val daysInShamsi = (totalDaysInPrevG - prevFarvardin1Day + 1) + daysSinceJan1
        
        if (daysInShamsi <= 186) {
            sMonth = (daysInShamsi - 1) / 31 + 1
            sDay = (daysInShamsi - 1) % 31 + 1
        } else {
            val remainingDays = daysInShamsi - 186
            sMonth = (remainingDays - 1) / 30 + 7
            sDay = (remainingDays - 1) % 30 + 1
        }
    }
    return Triple(sYear, sMonth, sDay)
}

// ===================================
// APPLE-STYLE CYLINDRICAL WHEEL PICKER
// ===================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(
    items: List<String>,
    initialIndex: Int,
    onIndexSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    visibleItemsCount: Int = 3,
    itemHeight: androidx.compose.ui.unit.Dp = 45.dp,
    label: String = "",
    textColor: Color = Color.White,
    selectedColor: Color = Color.Yellow,
    isLoop: Boolean = true
) {
    if (items.isEmpty()) return

    val itemsCount = if (isLoop) 10000 else items.size
    val startIndex = if (isLoop) {
        val middleOffset = itemsCount / 2
        middleOffset - (middleOffset % items.size) + (initialIndex % items.size)
    } else {
        initialIndex.coerceIn(0, items.size - 1)
    }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    
    val centerIndex by remember {
        derivedStateOf {
            val rawIdx = listState.firstVisibleItemIndex
            if (isLoop) {
                if (items.isNotEmpty()) rawIdx % items.size else 0
            } else {
                rawIdx.coerceIn(0, items.size - 1)
            }
        }
    }

    LaunchedEffect(centerIndex) {
        onIndexSelected(centerIndex)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(itemHeight * visibleItemsCount),
        contentAlignment = Alignment.Center
    ) {
        // Selection highlight bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .background(selectedColor.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                .border(1.dp, selectedColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
        )

        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = itemHeight * (visibleItemsCount / 2))
        ) {
            items(itemsCount) { index ->
                val realIndex = if (isLoop) index % items.size else index
                val isSelected = realIndex == centerIndex
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = items[realIndex] + if (label.isNotBlank() && isSelected) " $label" else "",
                        fontSize = if (isSelected) 19.sp else 15.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                        color = if (isSelected) selectedColor else textColor.copy(alpha = 0.35f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

