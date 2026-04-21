package com.example.eventnote.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.eventnote.util.AudioRecorderManager
import kotlinx.coroutines.delay

private val AppleBlue = Color(0xFF007AFF)
private val AppleRed = Color(0xFFFF3B30)
private val AppleGray = Color(0xFF8E8E93)

/**
 * 录音对话框
 * @param audioRecorderManager 由 CreateEditScreen 传入（使用 Activity Context）
 * @param onRequestRecordPermission 申请录音权限的回调
 */
@Composable
fun RecordingDialog(
    audioRecorderManager: AudioRecorderManager,
    onRequestRecordPermission: (onResult: (Boolean) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onRecordingComplete: (java.io.File) -> Unit
) {
    var isRecording by remember { mutableStateOf(false) }
    var recordingDurationMs by remember { mutableStateOf(0L) }
    var startFailed by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }

    // 计时器
    LaunchedEffect(isRecording) {
        if (isRecording) {
            val startTime = System.currentTimeMillis() - recordingDurationMs
            while (isRecording) {
                recordingDurationMs = System.currentTimeMillis() - startTime
                delay(100)
            }
        }
    }

    // 波形动画数据（使用平滑正弦波，避免抖动）
    val waveBars = remember { mutableStateListOf(0.3f, 0.4f, 0.5f, 0.4f, 0.3f) }
    LaunchedEffect(isRecording) {
        if (isRecording) {
            var time = 0L
            while (true) {
                val baseTime = time * 0.005f // 时间因子
                for (i in waveBars.indices) {
                    // 使用正弦波生成平滑的高度变化，每个条有相位差
                    val phase = i * 0.8f // 相位偏移
                    val smoothValue = (kotlin.math.sin(baseTime + phase) + 1f) / 2f // 0-1
                    waveBars[i] = 0.3f + smoothValue * 0.7f
                }
                time += 16 // 约 60fps
                delay(16L)
            }
        } else {
            // 重置为平静状态
            for (i in waveBars.indices) waveBars[i] = 0.3f
        }
    }

    // 权限被拒绝提示
    if (permissionDenied) {
        AlertDialog(
            onDismissRequest = { permissionDenied = false },
            icon = { Icon(Icons.Default.MicOff, contentDescription = null, tint = AppleRed) },
            title = { Text("需要录音权限") },
            text = { Text("录音功能需要麦克风权限，请在系统设置中开启。") },
            confirmButton = {
                TextButton(onClick = { permissionDenied = false }) {
                    Text("我知道了", color = AppleBlue)
                }
            }
        )
    }

    Dialog(
        onDismissRequest = { if (!isRecording) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = !isRecording, dismissOnClickOutside = !isRecording)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when {
                        isRecording -> "正在录音..."
                        startFailed -> "启动失败"
                        permissionDenied -> "权限被拒绝"
                        else -> "录音"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(28.dp))

                // 时长
                Text(
                    text = audioRecorderManager.formatDuration(recordingDurationMs),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isRecording) AppleRed else MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 波形条
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    waveBars.forEach { height ->
                        val animatedHeight by animateFloatAsState(targetValue = height, animationSpec = tween(180), label = "wave")
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .height((24 * animatedHeight).dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (isRecording) AppleRed.copy(alpha = 0.6f + animatedHeight * 0.4f)
                                    else AppleGray.copy(alpha = 0.3f)
                                )
                        )
                    }
                }

                if (isRecording) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val pulseAlpha by infiniteTransition.animateFloat(
                            initialValue = 1f, targetValue = 0.3f,
                            animationSpec = infiniteRepeatable(animation = tween(800), repeatMode = RepeatMode.Reverse),
                            label = "pulseAlpha"
                        )
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AppleRed.copy(alpha = pulseAlpha)))
                        Text(text = "录音中", style = MaterialTheme.typography.labelSmall, color = AppleRed.copy(alpha = pulseAlpha))
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (isRecording) {
                        // 取消
                        IconButton(
                            onClick = {
                                audioRecorderManager.cancelRecording()
                                isRecording = false
                                recordingDurationMs = 0L
                                onDismiss()
                            },
                            modifier = Modifier.size(52.dp).clip(CircleShape).background(AppleGray.copy(alpha = 0.15f))
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "取消", tint = AppleGray, modifier = Modifier.size(24.dp))
                        }

                        // 停止
                        IconButton(
                            onClick = {
                                val filePath = audioRecorderManager.stopRecording()
                                isRecording = false
                                if (filePath != null) {
                                    val file = java.io.File(filePath)
                                    if (file.exists()) onRecordingComplete(file)
                                }
                                onDismiss()
                            },
                            modifier = Modifier.size(72.dp).clip(CircleShape).background(AppleRed)
                        ) {
                            Box(modifier = Modifier.size(24.dp, 24.dp).clip(RoundedCornerShape(4.dp)).background(Color.White))
                        }

                        Spacer(modifier = Modifier.size(52.dp))
                    } else {
                        // 开始录音
                        IconButton(
                            onClick = {
                                onRequestRecordPermission { granted ->
                                    if (granted) {
                                        // 权限已授权，等 300ms 让系统完全生效后再启动录音
                                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                            audioRecorderManager.startRecording { success ->
                                                if (success) {
                                                    isRecording = true
                                                    recordingDurationMs = 0L
                                                    startFailed = false
                                                } else {
                                                    startFailed = true
                                                }
                                            }
                                        }, 300)
                                    } else {
                                        permissionDenied = true
                                    }
                                }
                            },
                            modifier = Modifier.size(72.dp).clip(CircleShape).background(AppleRed)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "开始录音", tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = when {
                        isRecording -> "点击方块停止录音"
                        startFailed -> "录音启动失败，请重试"
                        else -> "点击麦克风开始录音"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (startFailed) AppleRed else AppleGray
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (!isRecording) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = AppleGray)
                    }
                }
            }
        }
    }
}
