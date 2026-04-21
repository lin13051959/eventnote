package com.example.eventnote.ui.theme

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 主题管理器，负责存储和获取用户主题偏好
 */
class ThemeManager private constructor(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _themeModeFlow: MutableStateFlow<ThemeMode> = MutableStateFlow(getCurrentThemeMode())

    companion object {
        internal const val PREFS_NAME = "theme_settings"
        internal const val KEY_THEME_MODE = "theme_mode"
        private val DEFAULT_THEME_MODE = ThemeMode.DESKTOP_INVERTED

        @Volatile
        private var INSTANCE: ThemeManager? = null

        fun getInstance(context: Context): ThemeManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ThemeManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    init {
        // 注册 SharedPreferences 监听器
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_THEME_MODE) {
                _themeModeFlow.value = getCurrentThemeMode()
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    /**
     * 获取当前主题模式的 Flow
     */
    val themeModeFlow: Flow<ThemeMode> = _themeModeFlow.asStateFlow()

    /**
     * 保存主题模式
     */
    suspend fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeModeFlow.value = mode
    }

    /**
     * 获取当前主题模式（同步）
     */
    fun getCurrentThemeMode(): ThemeMode {
        return try {
            val modeName = prefs.getString(KEY_THEME_MODE, DEFAULT_THEME_MODE.name)
            ThemeMode.valueOf(modeName ?: DEFAULT_THEME_MODE.name)
        } catch (e: IllegalArgumentException) {
            DEFAULT_THEME_MODE
        }
    }
}