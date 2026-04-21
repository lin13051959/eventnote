package com.example.eventnote.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import java.io.File
import java.util.UUID

/**
 * 真实录音管理器
 */
class AudioRecorderManager(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var currentFilePath: String? = null
    private var _isRecording = false

    // 从 ContextWrapper 链中提取真实的 Activity
    private fun resolveActivity(): Activity? {
        var ctx: Context? = context
        while (ctx != null) {
            if (ctx is Activity) return ctx
            ctx = (ctx as? ContextWrapper)?.baseContext
        }
        return null
    }

    /**
     * 检查麦克风是否可用
     */
    fun isMicAvailable(): Boolean {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return am.mode == AudioManager.MODE_NORMAL
    }

    /**
     * 开始录音
     * @param onReady 结果回调（true=成功，false=失败）
     */
    fun startRecording(onReady: ((Boolean) -> Unit)? = null) {
        if (_isRecording) {
            onReady?.invoke(false)
            return
        }

        val activity = resolveActivity()
        if (activity == null) {
            onReady?.invoke(false)
            return
        }

        val id = UUID.randomUUID().toString()
        val audioDir = File(activity.filesDir, "event_audio")
        if (!audioDir.exists()) audioDir.mkdirs()
        val file = File(audioDir, "$id.m4a")
        currentFilePath = file.absolutePath

        // 尝试启动录音，如果失败则等待重试
        tryStartRecording(activity, retryCount = 0, onReady)
    }

    private fun tryStartRecording(activity: Activity, retryCount: Int, onReady: ((Boolean) -> Unit)?) {
        try {
            @Suppress("DEPRECATION")
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(activity)
            } else {
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(currentFilePath)
                prepare()
                start()
            }
            _isRecording = true
            onReady?.invoke(true)
        } catch (e: Exception) {
            mediaRecorder?.release()
            mediaRecorder = null
            // 最多重试 3 次，每次间隔 300ms
            if (retryCount < 3) {
                Handler(Looper.getMainLooper()).postDelayed({
                    tryStartRecording(activity, retryCount + 1, onReady)
                }, 300)
            } else {
                // 3 次都失败，清理
                currentFilePath?.let { File(it).delete() }
                currentFilePath = null
                _isRecording = false
                onReady?.invoke(false)
            }
        }
    }

    fun stopRecording(): String? {
        if (!_isRecording) return null
        val path = currentFilePath
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (_: Exception) {
            path?.let { File(it).delete() }
            mediaRecorder?.release()
            mediaRecorder = null
            _isRecording = false
            currentFilePath = null
            return null
        }
        mediaRecorder = null
        _isRecording = false
        return path
    }

    fun cancelRecording() {
        try { mediaRecorder?.stop() } catch (_: Exception) {}
        mediaRecorder?.release()
        mediaRecorder = null
        _isRecording = false
        currentFilePath?.let { File(it).delete() }
        currentFilePath = null
    }

    fun isRecording() = _isRecording

    fun formatDuration(millis: Long): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }
}
