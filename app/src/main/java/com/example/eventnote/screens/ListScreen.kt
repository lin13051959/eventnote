package com.example.eventnote.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.eventnote.data.local.entity.Category
import com.example.eventnote.data.local.entity.Event
import com.example.eventnote.data.local.entity.EventStatus
import com.example.eventnote.data.local.entity.Priority
import com.example.eventnote.ui.viewmodel.EventNoteViewModel
import com.example.eventnote.BuildConfig

// ==================== Apple 风格颜色 ====================
private val AppleBlue = Color(0xFF007AFF)
private val AppleGreen = Color(0xFF34C759)
private val AppleRed = Color(0xFFFF3B30)
private val AppleOrange = Color(0xFFFF9500)
private val ApplePurple = Color(0xFF5856D6)
private val AppleGray = Color(0xFF8E8E93)
private val AppleLightGray = Color(0xFFF2F2F7)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    viewModel: EventNoteViewModel = hiltViewModel(),
    onEventClick: (Long) -> Unit,
    onCreateEvent: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val events = uiState.events

    // 搜索状态
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    // 分类筛选
    var showCategoryFilter by remember { mutableStateOf(false) }

    // 状态筛选
    var selectedStatusFilter by remember { mutableStateOf<EventStatus?>(null) }

    Scaffold(
        // ==================== 顶部大标题导航栏 ====================
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // 第一行：标题 + 设置按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isSearchActive) {
                        Text(
                            text = "事件笔记",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 搜索图标按钮
                        IconButton(
                            onClick = { isSearchActive = !isSearchActive },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSearchActive) AppleBlue.copy(alpha = 0.12f)
                                    else Color.Transparent
                                )
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "搜索",
                                tint = if (isSearchActive) AppleBlue else MaterialTheme.colorScheme.onBackground
                            )
                        }
                        // 设置按钮
                        IconButton(
                            onClick = onSettingsClick,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "设置",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }

                // 免费版限额条
                if (BuildConfig.FREE_MAX_EVENTS != Int.MAX_VALUE) {
                    Spacer(modifier = Modifier.height(8.dp))
                    AppleLimitBar(
                        used = events.size,
                        max = BuildConfig.FREE_MAX_EVENTS
                    )
                }

                // 搜索框（展开时显示）
                AnimatedVisibility(
                    visible = isSearchActive,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    AppleSearchBar(
                        query = searchQuery,
                        onQueryChange = {
                            searchQuery = it
                            if (it.isNotEmpty()) {
                                viewModel.searchEvents(it)
                            } else {
                                viewModel.loadEvents()
                            }
                        },
                        onClose = {
                            isSearchActive = false
                            searchQuery = ""
                            viewModel.loadEvents()
                        }
                    )
                }
            }
        },

        // ==================== 底部悬浮按钮 ====================
        floatingActionButton = {
            AppleFloatingButton(onClick = onCreateEvent)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ==================== 分类横向滚动条 ====================
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 全部
                item {
                    AppleSegmentChip(
                        label = "全部",
                        selected = uiState.selectedCategoryId == null && selectedStatusFilter == null,
                        onClick = {
                            selectedStatusFilter = null
                            viewModel.filterByCategory(null)
                            viewModel.loadEvents()
                        }
                    )
                }
                // 现有分类
                items(uiState.categories) { category ->
                    AppleSegmentChip(
                        label = category.name,
                        selected = uiState.selectedCategoryId == category.id,
                        onClick = {
                            selectedStatusFilter = null
                            viewModel.filterByCategory(category.id)
                        },
                        color = getCategoryColor(category.name)
                    )
                }
            }

            // ==================== 状态筛选标签行 ====================
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    AppleStatusChip(
                        label = "全部",
                        selected = selectedStatusFilter == null,
                        onClick = {
                            selectedStatusFilter = null
                            viewModel.loadEvents()
                        },
                        color = AppleGray
                    )
                }
                item {
                    AppleStatusChip(
                        label = "进行中",
                        selected = selectedStatusFilter == EventStatus.IN_PROGRESS,
                        onClick = {
                            selectedStatusFilter = EventStatus.IN_PROGRESS
                            viewModel.getEventsByStatus(EventStatus.IN_PROGRESS)
                        },
                        color = AppleBlue
                    )
                }
                item {
                    AppleStatusChip(
                        label = "已完成",
                        selected = selectedStatusFilter == EventStatus.COMPLETED,
                        onClick = {
                            selectedStatusFilter = EventStatus.COMPLETED
                            viewModel.getEventsByStatus(EventStatus.COMPLETED)
                        },
                        color = AppleGreen
                    )
                }
                item {
                    AppleStatusChip(
                        label = "已归档",
                        selected = selectedStatusFilter == EventStatus.ARCHIVED,
                        onClick = {
                            selectedStatusFilter = EventStatus.ARCHIVED
                        },
                        color = AppleGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ==================== 事件列表 ====================
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    AppleLoadingIndicator()
                }
            } else if (events.isEmpty()) {
                AppleEmptyState(
                    icon = Icons.Default.NoteAlt,
                    title = "暂无事件",
                    subtitle = "点击右下角 + 创建你的第一个事件"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = events,
                        key = { it.id }
                    ) { event ->
                        AppleEventCard(
                            event = event,
                            category = event.categoryId?.let { id ->
                                uiState.categories.find { it.id == id }
                            },
                            onClick = { onEventClick(event.id) }
                        )
                    }
                    // 底部留白（给FAB留空间）
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

// ==================== Apple 风格组件 ====================

@Composable
fun AppleLimitBar(used: Int, max: Int) {
    val progress = (used.toFloat() / max).coerceIn(0f, 1f)
    val barColor = when {
        progress >= 0.9f -> AppleRed
        progress >= 0.7f -> AppleOrange
        else -> AppleBlue
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppleLightGray)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (used >= max) Icons.Default.Warning else Icons.Default.BarChart,
                    contentDescription = null,
                    tint = barColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (used >= max) "免费额度已用尽" else "免费额度",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "$used / $max 条",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = barColor
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = barColor,
            trackColor = AppleGray.copy(alpha = 0.2f),
        )
    }
}

@Composable
fun AppleSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text("搜索事件...", color = AppleGray)
        },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null, tint = AppleGray)
        },
        trailingIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "清除", tint = AppleGray)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppleBlue,
            unfocusedBorderColor = AppleGray.copy(alpha = 0.3f),
            focusedContainerColor = AppleBlue.copy(alpha = 0.05f),
            unfocusedContainerColor = AppleLightGray
        )
    )
}

@Composable
fun AppleSegmentChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    color: Color = AppleBlue
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) color else AppleLightGray,
        contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun AppleStatusChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    color: Color
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) color.copy(alpha = 0.15f) else Color.Transparent,
        contentColor = if (selected) color else AppleGray,
        border = if (!selected) androidx.compose.foundation.BorderStroke(
            1.dp,
            AppleGray.copy(alpha = 0.3f)
        ) else null
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun AppleFloatingButton(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = AppleBlue,
        contentColor = Color.White,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.size(56.dp)
    ) {
        Icon(Icons.Default.Add, contentDescription = "添加事件", modifier = Modifier.size(28.dp))
    }
}

@Composable
fun AppleLoadingIndicator() {
    CircularProgressIndicator(
        color = AppleBlue,
        modifier = Modifier.size(36.dp),
        strokeWidth = 3.dp
    )
}

@Composable
fun AppleEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(AppleLightGray),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = AppleGray,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = AppleGray,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

// ==================== Apple 风格事件卡片 ====================
// 改进事件卡片样式
@Composable
fun AppleEventCard(
    event: Event,
    category: Category?,
    onClick: () -> Unit
) {
    // Get category color for the card
    val categoryColor = category?.let { getCategoryColor(it.name) } ?: AppleGray
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        // Left color border
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(4.dp)
                .background(
                    color = categoryColor,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        bottomStart = 16.dp
                    )
                )
        )
        
        // Main card content
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = categoryColor.copy(alpha = 0.12f),
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            ),
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 16.dp,
                bottomStart = 12.dp,
                bottomEnd = 16.dp
            )
        ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false)
                )
                category?.let {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = it.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = event.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(10.dp))
            // 事件日期 + 相对天数
            val currentTime = System.currentTimeMillis()
            val daysDiff = ((currentTime - event.eventDate) / (1000 * 60 * 60 * 24)).toInt()
            val daysLabel = when {
                daysDiff == 0 -> "今天"
                daysDiff == 1 -> "昨天"
                daysDiff > 1 -> "${daysDiff}天前"
                daysDiff == -1 -> "明天"
                daysDiff < -1 -> "${-daysDiff}天后"
                else -> ""
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.Event,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = AppleBlue
                )
                Text(
                    text = formatDate(event.eventDate),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppleBlue
                )
                Text(
                    text = "·",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppleGray.copy(alpha = 0.5f)
                )
                Text(
                    text = daysLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = AppleGray
                )
            }
            // 创建时间（次要信息）
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = AppleGray.copy(alpha = 0.7f)
                )
                Text(
                    text = "Created ${formatDate(event.createdAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppleGray.copy(alpha = 0.7f)
                )
            }
        }
        } // Card closing bracket
    }
}



@Composable
fun AppleMiniTag(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.12f),
        contentColor = color
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

// ==================== 工具函数 ====================

fun getCategoryColor(name: String): Color {
    return when (name) {
        "工作" -> Color(0xFF007AFF)
        "学习" -> Color(0xFF5856D6)
        "生活" -> Color(0xFF34C759)
        "娱乐" -> Color(0xFFFF9500)
        "健康" -> Color(0xFFFF2D55)
        "财务" -> Color(0xFFAF52DE)
        else -> Color(0xFF8E8E93)
    }
}

private fun formatDate(timestamp: Long): String {
    return try {
        val date = java.util.Date(timestamp)
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        formatter.format(date)
    } catch (e: Exception) {
        ""
    }
}
