package com.example.eventnote.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.eventnote.data.local.entity.Event
import com.example.eventnote.data.local.entity.EventStatus
import com.example.eventnote.data.local.entity.MediaItem
import com.example.eventnote.data.local.entity.MediaType
import com.example.eventnote.data.local.entity.Priority
import com.example.eventnote.ui.viewmodel.EventNoteViewModel
import kotlinx.coroutines.launch
import java.io.File

private val AppleBlue = Color(0xFF007AFF)
private val AppleGreen = Color(0xFF34C759)
private val AppleRed = Color(0xFFFF3B30)
private val AppleOrange = Color(0xFFFF9500)
private val ApplePurple = Color(0xFF5856D6)
private val AppleGray = Color(0xFF8E8E93)
private val AppleLightGray = Color(0xFFF2F2F7)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EventDetailScreen(
    eventId: Long?,
    viewModel: EventNoteViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onEdit: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }

    val event = remember(eventId, uiState.events) {
        uiState.events.find { it.id == eventId }
    }

    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = AppleRed) },
            title = { Text("确认删除事件？") },
            text = { Text("此操作将删除该事件，且无法恢复！") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false // 先关闭对话框
                        eventId?.let {
                            scope.launch {
                                viewModel.deleteEvent(it)
                            }
                        }
                        onBack() // 立即返回
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = AppleRed)
                ) { Text("确认删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (event == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("事件不存在或已被删除", color = AppleRed)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // ==================== Apple 风格顶部导航栏 ====================
                // 改进顶部导航栏动画效果
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ChevronLeft,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(28.dp)
                                .graphicsLayer(
                                    scaleX = 1.1f,
                                    scaleY = 1.1f
                                )
                        )
                    }
                    Text(
                        text = "事件详情",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row {
                        IconButton(onClick = onEdit) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "编辑",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "删除",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                HorizontalDivider(color = AppleGray.copy(alpha = 0.15f))

                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ==================== 标题卡片 ====================
                    AppleDetailCard {
                        Column(modifier = Modifier.padding(20.dp)) {
                            // 标题
                            Text(
                                text = event.title,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // 优先级 + 状态 标签行
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppleDetailPriorityChip(priority = event.priority)
                                AppleDetailStatusChip(status = event.status)
                            }

                            // 分类
                            val category = event.categoryId?.let { id -> uiState.categories.find { it.id == id } }
                            if (category != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                AppleCategoryTag(label = category.name, color = getCategoryColor(category.name))
                            }

                            // 提醒时间
                            if (event.reminderTime != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = AppleOrange,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "提醒: ${formatDateTime(event.reminderTime)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = AppleOrange,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // 标签
                            if (event.tags.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    event.tags.forEach { tag ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = AppleLightGray,
                                            contentColor = AppleBlue
                                        ) {
                                            Text(
                                                text = "#$tag",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // 时间信息
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "创建时间",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AppleGray
                                    )
                                    Text(
                                        text = formatDateTime(event.createdAt),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "更新时间",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AppleGray
                                    )
                                    Text(
                                        text = formatDateTime(event.updatedAt),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // ==================== 内容卡片 ====================
                    AppleDetailCard {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.Article,
                                    contentDescription = null,
                                    tint = AppleBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "内容",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppleBlue
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = event.content,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.3
                            )
                        }
                    }

                    // ==================== 多媒体附件 ====================
                    if (event.mediaItems.isNotEmpty()) {
                        AppleDetailCard {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Default.AttachFile,
                                        contentDescription = null,
                                        tint = AppleBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "附件 (${event.mediaItems.size})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AppleBlue
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(event.mediaItems) { mediaItem ->
                                        MediaDetailItem(mediaItem = mediaItem)
                                    }
                                }
                            }
                        }
                    }

                    // ==================== 操作按钮 ====================
                    if (event.status != EventStatus.COMPLETED) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AppleActionButton(
                                text = "标记完成",
                                icon = Icons.Default.CheckCircle,
                                color = AppleGreen,
                                onClick = {
                                    scope.launch {
                                        viewModel.updateEvent(event.copy(status = EventStatus.COMPLETED))
                                        snackbarHostState.showSnackbar("事件标记为已完成")
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                filled = true
                            )
                            AppleActionButton(
                                text = "归档",
                                icon = Icons.Default.Archive,
                                color = AppleGray,
                                onClick = {
                                    scope.launch {
                                        viewModel.updateEvent(event.copy(status = EventStatus.ARCHIVED))
                                        snackbarHostState.showSnackbar("事件已归档")
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                filled = false
                            )
                        }
                    }

                    // 底部留白
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

// ==================== Apple 风格详情页组件 ====================

@Composable
fun AppleDetailCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppleGray.copy(alpha = 0.1f))
    ) {
        content()
    }
}

@Composable
fun AppleDetailPriorityChip(priority: Priority) {
    val (color, label) = when (priority) {
        Priority.HIGH -> AppleRed to "高优先级"
        Priority.MEDIUM -> AppleOrange to "中优先级"
        Priority.LOW -> AppleGreen to "低优先级"
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f),
        contentColor = color
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun AppleDetailStatusChip(status: EventStatus) {
    val (color, label) = when (status) {
        EventStatus.IN_PROGRESS -> AppleBlue to "进行中"
        EventStatus.COMPLETED -> AppleGreen to "已完成"
        EventStatus.ARCHIVED -> AppleGray to "已归档"
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f),
        contentColor = color
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun AppleCategoryTag(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f),
        contentColor = color
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun AppleActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = true
) {
    if (filled) {
        Button(
            onClick = onClick,
            modifier = modifier.height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, color)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ==================== 媒体查看组件 ====================

@Composable
fun MediaDetailItem(mediaItem: MediaItem) {
    val context = LocalContext.current
    var showFullScreenDialog by remember { mutableStateOf(false) }

    if (showFullScreenDialog && mediaItem.type == MediaType.IMAGE) {
        FullScreenImageDialog(
            imageUri = mediaItem.uri,
            onDismiss = { showFullScreenDialog = false }
        )
    }

    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                when (mediaItem.type) {
                    MediaType.IMAGE -> { showFullScreenDialog = true }
                    MediaType.VIDEO, MediaType.AUDIO -> { openMedia(context, mediaItem) }
                }
            }
    ) {
        when (mediaItem.type) {
            MediaType.IMAGE -> {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(mediaItem.uri).crossfade(true).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            MediaType.VIDEO -> {
                Box(Modifier.fillMaxSize().background(AppleLightGray), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Videocam, contentDescription = null, tint = AppleBlue, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = mediaItem.name.take(10) + if (mediaItem.name.length > 10) "..." else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppleGray
                        )
                    }
                    Box(
                        modifier = Modifier.align(Alignment.Center)
                            .size(40.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
            }
            MediaType.AUDIO -> {
                Box(Modifier.fillMaxSize().background(AppleLightGray), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = AppleBlue, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = mediaItem.name.take(10) + if (mediaItem.name.length > 10) "..." else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppleGray
                        )
                    }
                    Box(
                        modifier = Modifier.align(Alignment.Center)
                            .size(40.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}

private fun openMedia(context: android.content.Context, mediaItem: MediaItem) {
    try {
        var uri = Uri.parse(mediaItem.uri)
        if (uri.scheme == "file") {
            val file = File(uri.path ?: return)
            if (file.exists()) {
                try {
                    uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mediaItem.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
            } catch (e2: Exception) {
                android.widget.Toast.makeText(context, "无法播放：${mediaItem.name}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        android.widget.Toast.makeText(context, "打开失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun FullScreenImageDialog(imageUri: String, onDismiss: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 4f)
                        if (scale > 1f) {
                            offsetX += pan.x
                            offsetY += pan.y
                        } else {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                }
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "关闭", tint = Color.White, modifier = Modifier.size(28.dp))
            }
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(imageUri).crossfade(true).build(),
                contentDescription = "全屏图片",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offsetX, translationY = offsetY)
            )
        }
    }
}

// ==================== 工具函数 ====================

private fun formatDateTime(timestamp: Long): String {
    return try {
        val date = java.util.Date(timestamp)
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        formatter.format(date)
    } catch (e: Exception) {
        "时间错误"
    }
}
