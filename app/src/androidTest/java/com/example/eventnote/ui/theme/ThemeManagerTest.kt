package com.example.eventnote.ui.theme

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * ThemeManager 单元测试
 * 测试主题模式的获取、保存和 Flow 监听功能
 */
class ThemeManagerTest {

    private lateinit var context: Context
    private lateinit var themeManager: ThemeManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // 清理之前的设置，确保测试独立
        val prefs = context.getSharedPreferences(ThemeManager.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        
        themeManager = ThemeManager.getInstance(context)
    }

    /**
     * 测试默认主题模式
     */
    @Test
    fun testDefaultThemeMode() {
        val defaultMode = themeManager.getCurrentThemeMode()
        assertEquals(ThemeMode.DESKTOP_INVERTED, defaultMode)
    }

    /**
     * 测试设置和获取所有主题模式
     */
    @Test
    fun testSetAndGetAllThemeModes() = runBlocking {
        val themeModes = listOf(
            ThemeMode.SYSTEM,
            ThemeMode.DESKTOP_INVERTED,
            ThemeMode.CLASSIC_MATERIAL,
            ThemeMode.DARK_OLED
        )

        for (mode in themeModes) {
            themeManager.setThemeMode(mode)
            val currentMode = themeManager.getCurrentThemeMode()
            assertEquals("主题模式设置失败: $mode", mode, currentMode)
        }
    }

    /**
     * 测试主题模式持久化
     */
    @Test
    fun testThemeModePersistence() = runBlocking {
        // 设置一个主题
        val testMode = ThemeMode.CLASSIC_MATERIAL
        themeManager.setThemeMode(testMode)

        // 重新获取 ThemeManager 实例（模拟应用重启）
        val newThemeManager = ThemeManager.getInstance(context)
        val persistedMode = newThemeManager.getCurrentThemeMode()

        assertEquals("主题模式持久化失败", testMode, persistedMode)
    }

    /**
     * 测试无效主题模式处理
     */
    @Test
    fun testInvalidThemeModeHandling() {
        // 手动保存无效的主题名称
        val prefs = context.getSharedPreferences(ThemeManager.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(ThemeManager.KEY_THEME_MODE, "INVALID_MODE").commit()

        val currentMode = themeManager.getCurrentThemeMode()
        assertEquals("无效主题模式应返回默认值", ThemeMode.DESKTOP_INVERTED, currentMode)
    }

    /**
     * 测试主题模式 Flow 发射初始值
     */
    @Test
    fun testThemeModeFlowEmitsInitialValue() = runBlocking {
        val initialMode = themeManager.themeModeFlow.first()
        assertEquals("Flow 应发射初始值", ThemeMode.DESKTOP_INVERTED, initialMode)
    }

    /**
     * 测试主题模式 Flow 监听变化
     */
    @Test
    fun testThemeModeFlowEmitsOnChange() = runBlocking {
        // 收集 Flow
        val flow = themeManager.themeModeFlow
        
        val collectedValues = mutableListOf<ThemeMode>()
        val job = launch {
            flow.collect { mode ->
                collectedValues.add(mode)
            }
        }

        // 更改主题模式
        themeManager.setThemeMode(ThemeMode.SYSTEM)
        // 等待一小段时间确保 Flow 处理
        delay(100)
        
        // 验证收集到了初始值和更改后的值
        assertTrue("Flow 应至少收集到 2 个值", collectedValues.size >= 2)
        assertEquals("最后收集到的值应为 SYSTEM", ThemeMode.SYSTEM, collectedValues.last())
        
        job.cancel()
    }

    /**
     * 测试多个实例返回同一对象（单例模式）
     */
    @Test
    fun testSingletonInstance() {
        val instance1 = ThemeManager.getInstance(context)
        val instance2 = ThemeManager.getInstance(context)
        assertEquals("ThemeManager 应为单例", instance1, instance2)
    }

    /**
     * 测试 ThemeMode displayName 非空
     */
    @Test
    fun testThemeModeDisplayNames() {
        ThemeMode.values().forEach { mode ->
            assertTrue("ThemeMode ${mode.name} 的显示名称不应为空", mode.displayName != null)
            assertEquals("ThemeMode ${mode.name} 的显示名称不应为空", true, mode.displayName.isNotEmpty())
        }
    }
}