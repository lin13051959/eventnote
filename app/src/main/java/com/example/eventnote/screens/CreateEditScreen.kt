package com.example.eventnote.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.eventnote.data.local.entity.Event
import com.example.eventnote.data.local.entity.EventStatus
import com.example.eventnote.data.local.entity.MediaItem
import com.example.eventnote.data.local.entity.MediaType
import com.example.eventnote.data.local.entity.Priority
import com.example.eventnote.data.local.entity.ReminderType
import com.example.eventnote.ui.viewmodel.EventNoteViewModel
import com.example.eventnote.util.AudioRecorderManager
import com.example.eventnote.util.MediaManager
import com.example.eventnote.BuildConfig
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

private val AppleBlue = Color(0xFF007AFF)
private val AppleGreen = Color(0xFF34C759)
private val AppleRed = Color(0xFFFF3B30)
private val AppleOrange = Color(0xFFFF9500)
private val ApplePurple = Color(0xFF5856D6)
private val AppleGray = Color(0xFF8E8E93)
private val AppleLightGray = Color(0xFFF2F2F7)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditScreen(
    eventId: Long? = null,
    viewModel: EventNoteViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val mediaManager = remember { MediaManager(context) }
    // AudioRecorderManager 必须在 Activity Context 下创建
    val audioRecorderManager = remember { AudioRecorderManager(context) }

    val existingEvent = remember(eventId, uiState.events) {
        eventId?.let { id -> uiState.events.find { it.id == id } }
    }

    // 表单状态
    var title by remember { mutableStateOf(existingEvent?.title ?: "") }
    var content by remember { mutableStateOf(existingEvent?.content ?: "") }
    var selectedPriority by remember { mutableStateOf(existingEvent?.priority ?: Priority.MEDIUM) }
    var selectedStatus by remember { mutableStateOf(existingEvent?.status ?: EventStatus.IN_PROGRESS) }
    var tagsText by remember { mutableStateOf(existingEvent?.tags?.joinToString(", ") ?: "") }
    var selectedCategoryId by remember { mutableStateOf(existingEvent?.categoryId) }
    var categoryNameBuffer by remember { mutableStateOf("") }
    var mediaItems by remember { mutableStateOf(existingEvent?.mediaItems ?: emptyList()) }
    var eventDate by remember { mutableStateOf<Date>(existingEvent?.eventDate?.let { Date(it) } ?: Date()) }  // 事件日期（默认当天）
    var reminderDate by remember { mutableStateOf<Date?>(existingEvent?.reminderTime?.let { Date(it) }) }
    var reminderType by remember { mutableStateOf(existingEvent?.reminderType ?: ReminderType.NOTIFICATION) }
    var reminderSound by remember { mutableStateOf(existingEvent?.reminderSound ?: true) }
    var reminderVibration by remember { mutableStateOf(existingEvent?.reminderVibration ?: true) }

    // 对话框状态
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showLimitDialog by remember { mutableStateOf(false) }
    var showEventDatePicker by remember { mutableStateOf(false) }  // 事件日期选择器
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showReminderTypeMenu by remember { mutableStateOf(false) }
    var showCameraDialog by remember { mutableStateOf(false) }
    var showRecordingDialog by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) } // 防止重复保存

    val isFormValid = title.isNotBlank() && content.isNotBlank()

    // ========== 多媒体 Launchers ==========

    // 拍照
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var pendingPhotoFile by remember { mutableStateOf<File?>(null) }
    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && pendingPhotoFile != null) {
            mediaManager.createMediaItemFromFile(pendingPhotoFile!!)?.let { mediaItems += it }
        }
        pendingPhotoFile = null
    }

    // 录像
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var pendingVideoFile by remember { mutableStateOf<File?>(null) }
    val recordVideoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
        if (success && pendingVideoFile != null) {
            mediaManager.createMediaItemFromFile(pendingVideoFile!!)?.let { mediaItems += it }
        }
        pendingVideoFile = null
    }

    // 系统相册选择 - 使用 ACTION_GET_CONTENT 打开系统相册 APP
    val albumLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.clipData?.let { clipData ->
                for (i in 0 until clipData.itemCount) {
                    val uri = clipData.getItemAt(i).uri
                    mediaManager.createMediaItem(uri)?.let { mediaItems += it }
                }
            }
            result.data?.data?.let { uri ->
                mediaManager.createMediaItem(uri)?.let { mediaItems += it }
            }
        }
    }

    // 权限回调
    var pendingCameraAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) { pendingCameraAction?.invoke() }
        pendingCameraAction = null
    }

    // ========== 录音权限（由 CreateEditScreen 处理）==========
    // 存储录音权限回调，传递给 RecordingDialog
    var pendingRecordPermissionCallback by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }
    val recordPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        pendingRecordPermissionCallback?.invoke(isGranted)
        pendingRecordPermissionCallback = null
    }

    // 录音权限申请函数，传递给 RecordingDialog
    fun requestRecordPermission(onResult: (Boolean) -> Unit) {
        pendingRecordPermissionCallback = onResult
        // 录音功能只需要 RECORD_AUDIO 权限（Android 13+ 同样需要）
        val perm = Manifest.permission.RECORD_AUDIO
        recordPermissionLauncher.launch(perm)
    }

    // 拍照
    fun takePicture() {
        val photoFile = File(context.cacheDir, "camera_photo_${System.currentTimeMillis()}.jpg")
        pendingPhotoFile = photoFile
        photoUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
        pendingCameraAction = { takePictureLauncher.launch(photoUri!!) }
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // 录像
    fun recordVideo() {
        val videoFile = File(context.cacheDir, "camera_video_${System.currentTimeMillis()}.mp4")
        pendingVideoFile = videoFile
        videoUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", videoFile)
        pendingCameraAction = { recordVideoLauncher.launch(videoUri!!) }
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // 打开系统相册 APP（图片+视频）
    fun pickFromAlbum() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        albumLauncher.launch(intent)
    }

    // 事件日期选择器
    if (showEventDatePicker) {
        val eventDatePickerState = rememberDatePickerState(initialSelectedDateMillis = eventDate.time)
        DatePickerDialog(
            onDismissRequest = { showEventDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    eventDatePickerState.selectedDateMillis?.let { millis ->
                        eventDate = Date(millis)
                    }
                    showEventDatePicker = false
                }) { Text("确定", color = AppleBlue) }
            },
            dismissButton = {
                TextButton(onClick = { showEventDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = eventDatePickerState)
        }
    }

    // 日期选择器
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = reminderDate?.time ?: System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val cal = Calendar.getInstance()
                        if (reminderDate != null) cal.time = reminderDate!!
                        val selCal = Calendar.getInstance().apply { timeInMillis = millis }
                        cal.set(selCal.get(Calendar.YEAR), selCal.get(Calendar.MONTH), selCal.get(Calendar.DAY_OF_MONTH))
                        reminderDate = cal.time
                    }
                    showDatePicker = false
                }) { Text("确定", color = AppleBlue) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // 时间选择器
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = reminderDate?.let { Calendar.getInstance().apply { time = it }.get(Calendar.HOUR_OF_DAY) } ?: 9,
            initialMinute = reminderDate?.let { Calendar.getInstance().apply { time = it }.get(Calendar.MINUTE) } ?: 0
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("选择时间") },
            confirmButton = {
                TextButton(onClick = {
                    val cal = Calendar.getInstance()
                    if (reminderDate != null) cal.time = reminderDate!!
                    cal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    cal.set(Calendar.MINUTE, timePickerState.minute)
                    reminderDate = cal.time
                    showTimePicker = false
                }) { Text("确定", color = AppleBlue) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("取消") }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }

    // 添加分类对话框
    if (showAddCategoryDialog) {
        var newCategoryName by remember { mutableStateOf(categoryNameBuffer) }
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false; categoryNameBuffer = "" },
            icon = { Icon(Icons.Default.Add, contentDescription = null, tint = AppleBlue) },
            title = { Text("添加新分类") },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("分类名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppleBlue)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newCategoryName.isNotBlank()) {
                        scope.launch {
                            val category = com.example.eventnote.data.local.entity.Category(name = newCategoryName.trim())
                            val categoryId = viewModel.insertCategory(category)
                            selectedCategoryId = categoryId
                            categoryNameBuffer = ""
                        }
                    }
                    showAddCategoryDialog = false
                }) { Text("添加", color = AppleBlue) }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false; categoryNameBuffer = "" }) { Text("取消") }
            }
        )
    }

    // 限额提示对话框
    if (showLimitDialog) {
        AlertDialog(
            onDismissRequest = { showLimitDialog = false },
            icon = { Icon(Icons.Default.Info, contentDescription = null, tint = AppleOrange) },
            title = { Text("已达到免费版限制") },
            text = { Text("免费版最多记录 ${BuildConfig.FREE_MAX_EVENTS} 条事件，升级到专业版无限制使用") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { snackbarHostState.showSnackbar("请联系开发者获取专业版") }
                    showLimitDialog = false
                }) { Text("升级到专业版", color = AppleBlue) }
            },
            dismissButton = {
                TextButton(onClick = { showLimitDialog = false }) { Text("好的") }
            }
        )
    }

    // ========== 相机选择对话框（拍照 / 录像）==========
    if (showCameraDialog) {
        AlertDialog(
            onDismissRequest = { showCameraDialog = false },
            icon = { Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = AppleBlue) },
            title = { Text("选择拍摄方式") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppleDialogButton(
                        icon = Icons.Default.PhotoCamera,
                        label = "拍照",
                        color = AppleBlue,
                        onClick = {
                            showCameraDialog = false
                            takePicture()
                        }
                    )
                    AppleDialogButton(
                        icon = Icons.Default.Videocam,
                        label = "录像",
                        color = ApplePurple,
                        onClick = {
                            showCameraDialog = false
                            recordVideo()
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCameraDialog = false }) { Text("取消") }
            }
        )
    }

    // ========== 录音对话框 ==========
    if (showRecordingDialog) {
        RecordingDialog(
            audioRecorderManager = audioRecorderManager,
            onRequestRecordPermission = { callback -> requestRecordPermission(callback) },
            onDismiss = { showRecordingDialog = false },
            onRecordingComplete = { file ->
                mediaManager.createMediaItemFromFile(file)?.let { mediaItems += it }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // ==================== Apple 风格顶部导航栏 ====================
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
                    Icon(Icons.Default.ChevronLeft, contentDescription = "返回", tint = AppleBlue, modifier = Modifier.size(28.dp))
                }
                Text(
                    text = if (existingEvent == null) "新建事件" else "编辑事件",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(
                    onClick = {
                        if (isSaving) return@IconButton // 防抖
                        if (existingEvent == null && viewModel.isLimitReached()) {
                            showLimitDialog = true
                            return@IconButton
                        }
                        if (isFormValid) {
                            isSaving = true
                            scope.launch {
                                val tags = tagsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                                val event = Event(
                                    id = existingEvent?.id ?: 0,
                                    title = title,
                                    content = content,
                                    tags = tags,
                                    mediaItems = mediaItems,
                                    priority = selectedPriority,
                                    status = selectedStatus,
                                    eventDate = eventDate.time,
                                    reminderTime = reminderDate?.time,
                                    reminderType = if (reminderDate != null) reminderType else ReminderType.NOTIFICATION,
                                    reminderSound = reminderSound,
                                    reminderVibration = reminderVibration,
                                    categoryId = selectedCategoryId,
                                    createdAt = existingEvent?.createdAt ?: System.currentTimeMillis(),
                                    updatedAt = System.currentTimeMillis()
                                )
                                if (existingEvent == null) {
                                    viewModel.insertEvent(event)
                                } else {
                                    viewModel.updateEvent(event)
                                }
                                onSave()
                            }
                        } else {
                            scope.launch { snackbarHostState.showSnackbar("请填写标题和内容") }
                        }
                    },
                    enabled = isFormValid && !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = AppleBlue
                        )
                    } else {
                        Text(
                            text = "保存",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isFormValid) AppleBlue else AppleGray
                        )
                    }
                }
            }

            HorizontalDivider(color = AppleGray.copy(alpha = 0.15f))

            // ==================== 表单内容 ====================
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 标题输入
                AppleTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "标题",
                    placeholder = "输入事件标题",
                    isError = title.isBlank() && title.isNotEmpty().let { false }
                )

                // 内容输入
                AppleTextArea(
                    value = content,
                    onValueChange = { content = it },
                    label = "内容",
                    placeholder = "详细描述你的事件...",
                    minHeight = 120
                )

                // ==================== 优先级 ====================
                AppleSection(title = "优先级") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Priority.entries.forEach { priority ->
                            val (color, label) = when (priority) {
                                Priority.HIGH -> AppleRed to "高"
                                Priority.MEDIUM -> AppleOrange to "中"
                                Priority.LOW -> AppleGreen to "低"
                            }
                            AppleSelectableChip(
                                label = label,
                                selected = selectedPriority == priority,
                                color = color,
                                onClick = { selectedPriority = priority },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // ==================== 状态 ====================
                AppleSection(title = "状态") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EventStatus.entries.forEach { status ->
                            val (color, label) = when (status) {
                                EventStatus.IN_PROGRESS -> AppleBlue to "进行中"
                                EventStatus.COMPLETED -> AppleGreen to "已完成"
                                EventStatus.ARCHIVED -> AppleGray to "已归档"
                            }
                            AppleSelectableChip(
                                label = label,
                                selected = selectedStatus == status,
                                color = color,
                                onClick = { selectedStatus = status },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // ==================== 事件日期 ====================
                AppleSection(title = "事件日期") {
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showEventDatePicker = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = AppleBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = formatDate(eventDate),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = "选择日期",
                                tint = AppleGray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // ==================== 分类 ====================
                AppleSection(title = "分类") {
                    // 分类下拉
                    ExposedDropdownMenuBox(expanded = showCategoryMenu, onExpandedChange = { showCategoryMenu = it }) {
                        OutlinedTextField(
                            value = selectedCategoryId?.let { id -> uiState.categories.find { it.id == id }?.name } ?: "未分类",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryMenu) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AppleBlue,
                                unfocusedBorderColor = AppleGray.copy(alpha = 0.3f)
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = showCategoryMenu,
                            onDismissRequest = { showCategoryMenu = false }
                        ) {
                            listOf("工作", "学习", "生活", "娱乐", "健康", "财务").forEach { name ->
                                if (uiState.categories.none { it.name == name }) {
                                    DropdownMenuItem(
                                        text = { Text(name) },
                                        onClick = { categoryNameBuffer = name; showAddCategoryDialog = true; showCategoryMenu = false }
                                    )
                                }
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("未分类") },
                                onClick = { selectedCategoryId = null; showCategoryMenu = false }
                            )
                            uiState.categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.name) },
                                    onClick = { selectedCategoryId = category.id; showCategoryMenu = false }
                                )
                            }
                        }
                    }
                }

                // ==================== 标签 ====================
                AppleTextField(
                    value = tagsText,
                    onValueChange = { tagsText = it },
                    label = "标签",
                    placeholder = "用逗号分隔，如：工作, 重要, 紧急"
                )

                // ==================== 多媒体附件 ====================
                AppleSection(title = "多媒体附件") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AppleMediaButton(icon = Icons.Default.PhotoCamera, label = "相机", onClick = { showCameraDialog = true }, modifier = Modifier.weight(1f))
                        AppleMediaButton(icon = Icons.Default.Mic, label = "录音", onClick = { showRecordingDialog = true }, modifier = Modifier.weight(1f))
                        AppleMediaButton(icon = Icons.Default.PhotoLibrary, label = "相册", onClick = { pickFromAlbum() }, modifier = Modifier.weight(1f))
                    }

                    // 媒体预览
                    if (mediaItems.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(mediaItems) { item ->
                                AppleMediaPreviewItem(
                                    mediaItem = item,
                                    onRemove = { mediaItems = mediaItems.filter { it.id != item.id } }
                                )
                            }
                        }
                        Text(
                            text = "已添加 ${mediaItems.size} 个附件",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppleGray,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }

                // ==================== 提醒设置 ====================
                AppleSection(title = "提醒设置") {
                    // 检查通知权限（Android 13+）
                    val notificationPermissionGranted = remember {
                        mutableStateOf(
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            } else {
                                true
                            }
                        )
                    }
                    
                    // 通知权限请求
                    val notificationPermissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { isGranted ->
                        notificationPermissionGranted.value = isGranted
                    }
                    
                    // 精确闹钟权限检查
                    LaunchedEffect(Unit) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            // 尝试检查精确闹钟权限
                            try {
                                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                                if (!alarmManager.canScheduleExactAlarms()) {
                                    // 如果没有权限，显示提示
                                    snackbarHostState.showSnackbar("建议开启精确闹钟权限以确保提醒准时")
                                }
                            } catch (e: Exception) {
                                // 忽略
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AppleDateTimeCard(
                            icon = Icons.Default.CalendarToday,
                            label = "日期",
                            value = reminderDate?.let { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it) } ?: "选择日期",
                            onClick = {
                                // Android 13+ 需要通知权限
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationPermissionGranted.value) {
                                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    scope.launch { snackbarHostState.showSnackbar("需要通知权限才能使用提醒功能") }
                                    return@AppleDateTimeCard
                                }
                                showDatePicker = true
                            },
                            modifier = Modifier.weight(1f)
                        )
                        AppleDateTimeCard(
                            icon = Icons.Default.AccessTime,
                            label = "时间",
                            value = reminderDate?.let { SimpleDateFormat("HH:mm", Locale.getDefault()).format(it) } ?: "选择时间",
                            onClick = {
                                // Android 13+ 需要通知权限
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationPermissionGranted.value) {
                                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    scope.launch { snackbarHostState.showSnackbar("需要通知权限才能使用提醒功能") }
                                    return@AppleDateTimeCard
                                }
                                showTimePicker = true
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (reminderDate != null) {
                        TextButton(onClick = { reminderDate = null }, modifier = Modifier.fillMaxWidth()) {
                            Text("清除提醒", color = AppleRed)
                        }

                        // 提醒方式
                        ExposedDropdownMenuBox(
                            expanded = showReminderTypeMenu,
                            onExpandedChange = { showReminderTypeMenu = it }
                        ) {
                            OutlinedTextField(
                                value = when (reminderType) {
                                    ReminderType.NOTIFICATION -> "通知提醒"
                                    ReminderType.ALARM -> "闹钟"
                                    ReminderType.SILENT -> "静默提醒"
                                },
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showReminderTypeMenu) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AppleBlue,
                                    unfocusedBorderColor = AppleGray.copy(alpha = 0.3f)
                                )
                            )
                            ExposedDropdownMenu(expanded = showReminderTypeMenu, onDismissRequest = { showReminderTypeMenu = false }) {
                                listOf("通知提醒" to ReminderType.NOTIFICATION, "闹钟" to ReminderType.ALARM, "静默提醒" to ReminderType.SILENT).forEach { (label, type) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = { reminderType = type; showReminderTypeMenu = false }
                                    )
                                }
                            }
                        }

                        if (reminderType != ReminderType.SILENT) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(AppleLightGray).padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = AppleBlue, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("铃声", style = MaterialTheme.typography.bodyMedium)
                                }
                                Switch(checked = reminderSound, onCheckedChange = { reminderSound = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AppleBlue))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(AppleLightGray).padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Vibration, contentDescription = null, tint = AppleBlue, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("震动", style = MaterialTheme.typography.bodyMedium)
                                }
                                Switch(checked = reminderVibration, onCheckedChange = { reminderVibration = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AppleBlue))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== Apple 风格表单组件 ====================

@Composable
fun AppleSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = AppleBlue
        )
        content()
    }
}

@Composable
fun AppleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    isError: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, color = AppleGray) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppleBlue,
            unfocusedBorderColor = AppleGray.copy(alpha = 0.3f),
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            errorBorderColor = AppleRed
        ),
        isError = isError
    )
}

@Composable
fun AppleTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    minHeight: Int = 120
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, color = AppleGray) },
        modifier = Modifier.fillMaxWidth().heightIn(min = minHeight.dp),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppleBlue,
            unfocusedBorderColor = AppleGray.copy(alpha = 0.3f)
        ),
        maxLines = 10
    )
}

@Composable
fun AppleSelectableChip(
    label: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) color else AppleLightGray,
        contentColor = if (selected) Color.White else color
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(vertical = 10.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun AppleMediaButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(vertical = 10.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppleBlue)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun AppleDialogButton(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}

@Composable
fun AppleDateTimeCard(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = AppleLightGray
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = AppleBlue, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = label, style = MaterialTheme.typography.labelSmall, color = AppleGray)
                Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun AppleMediaPreviewItem(mediaItem: MediaItem, onRemove: () -> Unit) {
    Box(
        modifier = Modifier.size(90.dp).clip(RoundedCornerShape(10.dp))
    ) {
        when (mediaItem.type) {
            MediaType.IMAGE -> {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(mediaItem.uri).crossfade(true).build(),
                    contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            MediaType.VIDEO -> {
                Box(Modifier.fillMaxSize().background(AppleLightGray), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Videocam, contentDescription = null, tint = AppleBlue, modifier = Modifier.size(36.dp))
                }
            }
            MediaType.AUDIO -> {
                Box(Modifier.fillMaxSize().background(AppleLightGray), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = AppleBlue, modifier = Modifier.size(36.dp))
                }
            }
        }
        IconButton(
            onClick = onRemove,
            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp).background(AppleRed.copy(alpha = 0.85f), CircleShape)
        ) {
            Icon(Icons.Default.Close, contentDescription = "删除", tint = Color.White, modifier = Modifier.size(14.dp))
        }
    }
}

// 局部日期格式化函数（接受 Date，避免与其他文件的同名函数冲突）
private fun formatDate(date: Date): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        sdf.format(date)
    } catch (e: Exception) {
        ""
    }
}
