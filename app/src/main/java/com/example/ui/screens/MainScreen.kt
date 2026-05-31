package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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

val VazirmatnFontFamily = FontFamily(
    Font(com.example.R.font.vazirmatn, FontWeight.Normal),
    Font(com.example.R.font.vazirmatn_bold, FontWeight.Bold)
)

sealed class AppScreen {
    object Dashboard : AppScreen()
    object Schedule : AppScreen()
    object Chat : AppScreen()
    object Settings : AppScreen()
    data class AddEdit(val task: Task? = null) : AppScreen()
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
    Color(0xFF8E8E93), // 0: Grey
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
    Color.White, Color.White, Color.White, Color.Black, Color.Black, 
    Color.Black, Color.White, Color.White, Color.Black, Color.White
)

val DarkTaskColors = listOf(
    Color(0xFF8E8E93), // 0: Grey
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
    Color.White, Color.White, Color.White, Color.Black, Color.Black, 
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

    BackHandler(enabled = currentScreen != AppScreen.Dashboard) {
        currentScreen = AppScreen.Dashboard
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl
    ) {
        ProvideTextStyle(value = androidx.compose.ui.text.TextStyle(fontFamily = VazirmatnFontFamily)) {
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
                }
            }

            // FLOATING DOCK (Navigation Bar)
            if (currentScreen !is AppScreen.AddEdit) {
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
@Composable
fun DashboardScreen(viewModel: TaskViewModel, colors: ThemeColors, onNavigate: (AppScreen) -> Unit) {
    val tasks by viewModel.filteredAndSortedTasks.collectAsStateWithLifecycle()
    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val statusFilter by viewModel.statusFilter.collectAsStateWithLifecycle()
    
    var searchQuery by remember { mutableStateOf("") }
    var expandNotifications by remember { mutableStateOf(false) }
    
    val filteredBySearch = if (searchQuery.isBlank()) tasks else tasks.filter {
        it.title.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true)
    }

    val pendingCount = allTasks.count { it.status == "Pending" }
    
    // Group tasks by folder (null/empty becomes "بدون فولدر")
    val groupedTasks = filteredBySearch.groupBy { 
        if (it.folderName.isNullOrBlank()) "بدون فولدر" else it.folderName
    }
    
    // Notifications logic (Mocking urgent/overdue)
    val notifications = allTasks.filter {
        it.status == "Pending" && it.deadline > 0 && (it.deadline - System.currentTimeMillis() < 86400000L * 2) // within 2 days
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
                        
                        // Bell with Badge
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
                            if (notifications.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = (-4).dp, y = 4.dp)
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color.Red)
                                )
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
                            AnimatedTaskCard(task, cardColor, onCardColor, colors, onNavigate = { onNavigate(AppScreen.AddEdit(task)) }, onToggle = { viewModel.toggleTaskComplete(task) })
                        }
                    }
                    
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
        
        // Notifications Dropdown Mock (Overlay)
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
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("یادآورها", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textSecondary, modifier = Modifier.clickable { expandNotifications = false })
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (notifications.isEmpty()) {
                        Text("شما پیامی ندارید.", color = colors.textSecondary, fontSize = 14.sp)
                    } else {
                        notifications.take(3).forEach { notifTask ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.Red))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("تسکی نیاز به توجه شما دارد!", color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Text(notifTask.title, color = colors.textSecondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            HorizontalDivider(color = colors.surfaceVariant, thickness = 1.dp)
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
                        val prioText = if(task.priorityScore > 75) "فوری" else if(task.priorityScore > 40) "عادی" else "پایین"
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
            Text("رفیق هوشمندت", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
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
@Composable
fun AddEditScreen(taskToEdit: Task?, viewModel: TaskViewModel, colors: ThemeColors, onBack: () -> Unit) {
    var title by remember { mutableStateOf(taskToEdit?.title ?: "") }
    var description by remember { mutableStateOf(taskToEdit?.description ?: "") }
    var durationText by remember { mutableStateOf(taskToEdit?.timeEstimateText ?: "") }
    var emoji by remember { mutableStateOf(taskToEdit?.emoji ?: "📝") }
    var folderName by remember { mutableStateOf(taskToEdit?.folderName ?: "") }
    var colorIndex by remember { mutableStateOf(taskToEdit?.colorIndex ?: 0) }
    var deadlineMode by remember { mutableStateOf(false) }

    val initialSubtasks = description.split("\n").filter { it.isNotBlank() }
    val subtasks = remember { androidx.compose.runtime.mutableStateListOf<String>().apply { addAll(initialSubtasks) } }
    var newSubtask by remember { mutableStateOf("") }

    var showTimeSheet by remember { mutableStateOf(false) }
    var showFolderSheet by remember { mutableStateOf(false) }

    @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
    if (showTimeSheet) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showTimeSheet = false },
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
                            .clickable { timeTab = 0; deadlineMode = false }
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
                            .clickable { timeTab = 1; deadlineMode = true }
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
                
                Spacer(modifier = Modifier.height(24.dp))
                
                if (timeTab == 0) {
                    // DURATION PRESETS & INCREMENTERS
                    var dDays by remember { mutableStateOf(0) }
                    var dHours by remember { mutableStateOf(2) }
                    
                    Text("مدت زمان پیشنهادی یا دلخواه خود را تعیین کنید:", color = colors.textSecondary, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Prest Cards
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("۳۰ د", "۱ س", "۲ س", "۴ س", "۱ روز", "۳ روز").forEach { label ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.surface)
                                    .clickable {
                                        when (label) {
                                            "۳۰ د" -> { dDays = 0; dHours = 0; durationText = "۳۰ دقیقه"; showTimeSheet = false }
                                            "۱ س" -> { dDays = 0; dHours = 1 }
                                            "۲ س" -> { dDays = 0; dHours = 2 }
                                            "۴ س" -> { dDays = 0; dHours = 4 }
                                            "۱ روز" -> { dDays = 1; dHours = 0 }
                                            "۳ روز" -> { dDays = 3; dHours = 0 }
                                        }
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                    
                    // Incrementers Layout
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Days selector
                        Column(modifier = Modifier.weight(1f)) {
                            Text("روزها", color = colors.textPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.surface).padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(36.dp).clip(CircleShape).background(colors.bg).clickable { if (dDays > 0) dDays-- },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("-", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                }
                                Text("$dDays", color = colors.textPrimary, fontWeight = FontWeight.Black, fontSize = 18.sp)
                                Box(
                                    modifier = Modifier.size(36.dp).clip(CircleShape).background(colors.bg).clickable { dDays++ },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("+", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                }
                            }
                        }
                        
                        // Hours selector
                        Column(modifier = Modifier.weight(1f)) {
                            Text("ساعت‌ها", color = colors.textPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.surface).padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(36.dp).clip(CircleShape).background(colors.bg).clickable { if (dHours > 0) dHours-- },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("-", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                }
                                Text("$dHours", color = colors.textPrimary, fontWeight = FontWeight.Black, fontSize = 18.sp)
                                Box(
                                    modifier = Modifier.size(36.dp).clip(CircleShape).background(colors.bg).clickable { if (dHours < 23) dHours++ },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("+", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    androidx.compose.material3.Button(
                        onClick = {
                            val txt = buildString {
                                if (dDays > 0) append("$dDays روز")
                                if (dHours > 0) {
                                    if (isNotEmpty()) append(" و ")
                                    append("$dHours ساعت")
                                }
                                if (isEmpty()) append("۳۰ دقیقه")
                            }
                            durationText = txt
                            showTimeSheet = false
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = colors.taskColors[4]),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("تنظیم مدت انجام", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                } else {
                    // IRANIAN (SHAMSI) CALENDAR WIZARD
                    var dateStep by remember { mutableStateOf(0) } // 0 = Date, 1 = Time
                    var selectedYear by remember { mutableStateOf(1405) }
                    var selectedMonth by remember { mutableStateOf("خرداد") }
                    var selectedDay by remember { mutableStateOf(10) }
                    var selectedHour by remember { mutableStateOf(12) }
                    var selectedMinute by remember { mutableStateOf(0) }
                    
                    val shamsiMonths = remember {
                        listOf("فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور", "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند")
                    }
                    
                    if (dateStep == 0) {
                        // DATE SELECTOR STEP
                        Text("مرحله ۱: انتخاب روز و ماه شمسی", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Year Toggle
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1405, 1406, 1407).forEach { year ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (selectedYear == year) colors.taskColors[4] else colors.surface)
                                        .clickable { selectedYear = year }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("$year", color = if (selectedYear == year) Color.Black else colors.textPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("ماه", color = colors.textSecondary, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Months Grid (4 columns, 3 rows)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (row in 0 until 3) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    for (col in 0 until 4) {
                                        val idx = row * 4 + col
                                        val mName = shamsiMonths[idx]
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (selectedMonth == mName) colors.taskColors[4].copy(alpha = 0.5f) else colors.surface)
                                                .border(2.dp, if (selectedMonth == mName) colors.taskColors[4] else Color.Transparent, RoundedCornerShape(10.dp))
                                                .clickable { selectedMonth = mName }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(mName, color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("روز", color = colors.textSecondary, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Days Row
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(31) { index ->
                                val dayNum = index + 1
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (selectedDay == dayNum) colors.taskColors[4] else colors.surface)
                                        .clickable { selectedDay = dayNum },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("$dayNum", color = if (selectedDay == dayNum) Color.Black else colors.textPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        androidx.compose.material3.Button(
                            onClick = { dateStep = 1 },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = colors.taskColors[4]),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("بعدی: تنظیم ساعت ➔", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // TIME SELECTOR STEP
                        Text("مرحله ۲: تنظیم ساعت و دقیقه", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            // Hour Picker
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("ساعت", color = colors.textSecondary, modifier = Modifier.padding(bottom = 8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.surface).padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(36.dp).clip(CircleShape).background(colors.bg).clickable { if (selectedHour > 0) selectedHour-- },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("-", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                    }
                                    Text(String.format("%02d", selectedHour), color = colors.textPrimary, fontWeight = FontWeight.Black, fontSize = 22.sp)
                                    Box(
                                        modifier = Modifier.size(36.dp).clip(CircleShape).background(colors.bg).clickable { if (selectedHour < 23) selectedHour++ },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("+", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    }
                                }
                            }
                            
                            // Minute Picker
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("دقیقه", color = colors.textSecondary, modifier = Modifier.padding(bottom = 8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.surface).padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(36.dp).clip(CircleShape).background(colors.bg).clickable { if (selectedMinute >= 5) selectedMinute -= 5 },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("-", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                    }
                                    Text(String.format("%02d", selectedMinute), color = colors.textPrimary, fontWeight = FontWeight.Black, fontSize = 22.sp)
                                    Box(
                                        modifier = Modifier.size(36.dp).clip(CircleShape).background(colors.bg).clickable { if (selectedMinute <= 50) selectedMinute += 5 },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("+", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            androidx.compose.material3.Button(
                                onClick = { dateStep = 0 },
                                modifier = Modifier.weight(1f).height(52.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = colors.surface),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("❮ برگشت", color = colors.textPrimary, fontWeight = FontWeight.Bold)
                            }
                            
                            androidx.compose.material3.Button(
                                onClick = {
                                    durationText = "$selectedDay $selectedMonth $selectedYear ساعت $selectedHour:${String.format("%02d", selectedMinute)}"
                                    deadlineMode = true
                                    showTimeSheet = false
                                },
                                modifier = Modifier.weight(1.5f).height(52.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = colors.taskColors[4]),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("تایید نهایی ددلاین", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
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
                                deadline = 0L, 
                                priority = 50, 
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
                                    folderName = folderName.takeIf { it.isNotBlank() }
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
