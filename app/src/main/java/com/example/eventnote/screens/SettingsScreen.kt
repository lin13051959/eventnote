package com.example.eventnote.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.eventnote.data.PasswordValidationResult
import com.example.eventnote.ui.viewmodel.EventNoteViewModel
import com.example.eventnote.BuildConfig
import com.example.eventnote.ui.theme.ThemeManager
import com.example.eventnote.ui.theme.ThemeMode
import kotlinx.coroutines.launch

private val AppleBlue = Color(0xFF007AFF)
private val AppleGreen = Color(0xFF34C759)
private val AppleRed = Color(0xFFFF3B30)
private val AppleOrange = Color(0xFFFF9500)
private val ApplePurple = Color(0xFF5856D6)
private val AppleGray = Color(0xFF8E8E93)
private val AppleLightGray = Color(0xFFF2F2F7)
private val AppleYellow = Color(0xFFFFCC00)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: EventNoteViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()

    // 主题管理器
    val themeManager = remember { ThemeManager.getInstance(context) }
    var currentThemeMode by remember { mutableStateOf(themeManager.getCurrentThemeMode()) }
    var showThemeDialog by remember { mutableStateOf(false) }

    // 对话框状态
    var showSetPasswordDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showPasswordInputDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    // 导入导出
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.data?.let { uri ->
            viewModel.exportData(uri) { result ->
                result.fold(
                    onSuccess = { count -> scope.launch { snackbarHostState.showSnackbar("成功导出 $count 条事件") } },
                    onFailure = { e -> scope.launch { snackbarHostState.showSnackbar("导出失败: ${e.message}") } }
                )
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.data?.let { uri ->
            viewModel.importData(uri) { result ->
                result.fold(
                    onSuccess = { ir -> scope.launch { snackbarHostState.showSnackbar("导入完成: 成功 ${ir.successCount} 条${if (ir.failCount > 0) ", 失败 ${ir.failCount} 条" else ""}") } },
                    onFailure = { e -> scope.launch { snackbarHostState.showSnackbar("导入失败: ${e.message}") } }
                )
            }
        }
    }

    // 对话框们
    if (showSetPasswordDialog) {
        SetPasswordDialog(
            onDismiss = { showSetPasswordDialog = false },
            onConfirm = { pwd, confirmPwd ->
                if (pwd != confirmPwd) {
                    scope.launch { snackbarHostState.showSnackbar("两次密码输入不一致") }
                    false
                } else if (pwd.length < 4) {
                    scope.launch { snackbarHostState.showSnackbar("密码至少4位") }
                    false
                } else {
                    val ok = viewModel.setPassword(pwd)
                    scope.launch { snackbarHostState.showSnackbar(if (ok) "密码设置成功" else "密码设置失败") }
                    ok
                }
            }
        )
    }

    if (showPasswordInputDialog) {
        PasswordInputDialog(
            onDismiss = { showPasswordInputDialog = false },
            onConfirm = { pwd ->
                val result = viewModel.verifyPassword(pwd)
                when (result) {
                    PasswordValidationResult.SUCCESS -> {
                        showPasswordInputDialog = false
                        showClearDataDialog = true
                        true
                    }
                    PasswordValidationResult.WRONG_PASSWORD -> {
                        scope.launch { snackbarHostState.showSnackbar("密码错误") }
                        false
                    }
                    else -> false
                }
            }
        )
    }

    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = AppleRed) },
            title = { Text("确认清空所有数据？") },
            text = { Text("此操作将删除所有事件记录，且无法恢复！") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllData { success ->
                            scope.launch { if (success) snackbarHostState.showSnackbar("所有数据已清除") }
                        }
                        showClearDataDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = AppleRed)
                ) { Text("确认清除") }
            },
            dismissButton = { TextButton(onClick = { showClearDataDialog = false }) { Text("取消") } }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            icon = { Icon(Icons.Outlined.Info, contentDescription = null, tint = AppleBlue) },
            title = { Text("关于 EventNote") },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EventNote, contentDescription = null, tint = AppleBlue, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("EventNote", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("版本: ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("一个简洁美观的事件记录应用", style = MaterialTheme.typography.bodySmall, color = AppleGray)
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = AppleGray.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("© 2024 EventNote", style = MaterialTheme.typography.labelSmall, color = AppleGray)
                }
            },
            confirmButton = { TextButton(onClick = { showAboutDialog = false }) { Text("好的", color = AppleBlue) } }
        )
    }

    // ==================== 主题选择对话框 ====================
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            icon = { Icon(Icons.Outlined.Palette, contentDescription = null, tint = AppleBlue) },
            title = { Text("选择主题") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    scope.launch {
                                        themeManager.setThemeMode(mode)
                                        currentThemeMode = mode
                                        showThemeDialog = false
                                        snackbarHostState.showSnackbar("已切换到「${mode.displayName}」主题")
                                    }
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentThemeMode == mode,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = AppleBlue)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = mode.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (currentThemeMode == mode) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (currentThemeMode == mode) AppleBlue else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = when (mode) {
                                        ThemeMode.SYSTEM -> "跟随系统暗色模式设置"
                                        ThemeMode.DESKTOP_INVERTED -> "高对比度反色风格（默认）"
                                        ThemeMode.CLASSIC_MATERIAL -> "Material Design 经典配色"
                                        ThemeMode.DARK_OLED -> "纯黑背景，适合 OLED 屏幕"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppleGray
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("取消", color = AppleGray)
                }
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
                    text = "设置",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(48.dp))
            }

            HorizontalDivider(color = AppleGray.copy(alpha = 0.15f))

            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ==================== 版本/升级卡片 ====================
                if (BuildConfig.FREE_MAX_EVENTS != Int.MAX_VALUE) {
                    // 免费版 - 升级卡片
                    AppleUpgradeCard(
                        currentLimit = BuildConfig.FREE_MAX_EVENTS,
                        onUpgrade = {
                            scope.launch { snackbarHostState.showSnackbar("请联系开发者获取专业版") }
                        }
                    )
                } else {
                    // 专业版标识
                    AppleProCard()
                }

                // ==================== 统计卡片 ====================
                AppleSettingsSection(title = "统计信息") {
                    val total = uiState.events.size
                    val completed = uiState.events.count { it.status == com.example.eventnote.data.local.entity.EventStatus.COMPLETED }
                    val inProgress = uiState.events.count { it.status == com.example.eventnote.data.local.entity.EventStatus.IN_PROGRESS }
                    val highPriority = uiState.events.count { it.priority == com.example.eventnote.data.local.entity.Priority.HIGH }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(AppleLightGray)
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        AppleStatItem(value = total.toString(), label = "总事件", color = AppleBlue)
                        AppleStatItem(value = completed.toString(), label = "已完成", color = AppleGreen)
                        AppleStatItem(value = inProgress.toString(), label = "进行中", color = AppleOrange)
                        AppleStatItem(value = highPriority.toString(), label = "高优先", color = AppleRed)
                    }
                }

                // ==================== 安全设置 ====================
                AppleSettingsSection(title = "安全") {
                    AppleSettingsItem(
                        icon = Icons.Outlined.Lock,
                        iconColor = AppleBlue,
                        title = "设置清除数据密码",
                        subtitle = if (viewModel.isPasswordSet()) "已设置密码" else "未设置密码",
                        onClick = { showSetPasswordDialog = true }
                    )
                }

                // ==================== 数据管理 ====================
                AppleSettingsSection(title = "数据管理") {
                    AppleSettingsItem(
                        icon = Icons.Outlined.Upload,
                        iconColor = AppleBlue,
                        title = "导出数据",
                        subtitle = "导出为 ZIP 文件（含媒体）",
                        onClick = {
                            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = "application/zip"
                                putExtra(Intent.EXTRA_TITLE, viewModel.generateZipExportFileName())
                            }
                            exportLauncher.launch(intent)
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = AppleGray.copy(alpha = 0.1f))
                    AppleSettingsItem(
                        icon = Icons.Outlined.Download,
                        iconColor = AppleGreen,
                        title = "导入数据",
                        subtitle = "从 ZIP 文件导入",
                        onClick = {
                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = "application/zip"
                            }
                            importLauncher.launch(intent)
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = AppleGray.copy(alpha = 0.1f))
                    AppleSettingsItem(
                        icon = Icons.Outlined.DeleteForever,
                        iconColor = AppleRed,
                        title = "清空所有数据",
                        subtitle = "删除所有事件记录",
                        titleColor = AppleRed,
                        onClick = {
                            if (viewModel.isPasswordSet()) {
                                showPasswordInputDialog = true
                            } else {
                                showClearDataDialog = true
                            }
                        }
                    )
                }

                // ==================== 外观 ====================
                AppleSettingsSection(title = "外观") {
                    AppleSettingsItem(
                        icon = Icons.Outlined.Palette,
                        iconColor = ApplePurple,
                        title = "主题",
                        subtitle = currentThemeMode.displayName,
                        onClick = { showThemeDialog = true }
                    )
                }

                // ==================== 关于 ====================
                AppleSettingsSection(title = "关于") {
                    AppleSettingsItem(
                        icon = Icons.Outlined.Info,
                        iconColor = AppleGray,
                        title = "关于 EventNote",
                        subtitle = "版本 ${BuildConfig.VERSION_NAME}",
                        onClick = { showAboutDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

// ==================== Apple 风格设置页组件 ====================

@Composable
fun AppleUpgradeCard(currentLimit: Int, onUpgrade: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppleBlue)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Upgrade, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "升级到专业版",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "免费版限制 $currentLimit 条事件",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
                Button(
                    onClick = onUpgrade,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = AppleBlue
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("升级", fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "专业版：无事件数量限制，享受完整功能",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun AppleProCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ApplePurple)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Verified, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "✓ 专业版已激活",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "无事件数量限制，享受完整功能",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }
        }
    }
}

@Composable
fun AppleSettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = AppleGray,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, AppleGray.copy(alpha = 0.1f))
        ) {
            Column {
                content()
            }
        }
    }
}

// 改进设置项的动态效果
@Composable
fun AppleSettingsItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer(
                    scaleX = 1.1f,
                    scaleY = 1.1f
                )
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = titleColor
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AppleStatItem(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AppleGray
        )
    }
}

// ==================== 对话框 ====================

@Composable
fun SetPasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Boolean
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = AppleBlue) },
        title = { Text("设置密码") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("输入密码（至少4位）") },
                    visualTransformation = if (showPassword) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null, tint = AppleGray)
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppleBlue)
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("确认密码") },
                    visualTransformation = if (showPassword) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppleBlue)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (onConfirm(password, confirmPassword)) {
                        onDismiss()
                    }
                }
            ) { Text("确认", color = AppleBlue) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun PasswordInputDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Boolean
) {
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = AppleRed) },
        title = { Text("请输入密码") },
        text = {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("密码") },
                visualTransformation = if (showPassword) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null, tint = AppleGray)
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppleBlue)
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (onConfirm(password)) {
                        onDismiss()
                    }
                }
            ) { Text("确认", color = AppleBlue) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
