package com.example.eventnote.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.setContent
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * EventNoteTheme 主题切换测试
 * 测试主题颜色方案的正确性和主题切换逻辑
 */
@RunWith(AndroidJUnit4::class)
class EventNoteThemeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * 测试 EventNoteTheme 正确应用 MaterialTheme
     */
    @Test
    fun testEventNoteThemeAppliesMaterialTheme() {
        composeTestRule.setContent {
            EventNoteTheme(themeMode = ThemeMode.DESKTOP_INVERTED, darkTheme = false) {
                val colorScheme = MaterialTheme.colorScheme
                assertNotNull("ColorScheme 不应为 null", colorScheme)
            }
        }
    }

    /**
     * 测试主题模式枚举显示名称
     */
    @Test
    fun testThemeModeDisplayNames() {
        val expectedNames = mapOf(
            ThemeMode.SYSTEM to "跟随系统",
            ThemeMode.DESKTOP_INVERTED to "桌面反色",
            ThemeMode.CLASSIC_MATERIAL to "经典 Material",
            ThemeMode.DARK_OLED to "深色 OLED"
        )

        for ((mode, expectedName) in expectedNames) {
            assertEquals("${mode.name} 显示名称应为 $expectedName", expectedName, mode.displayName)
        }
    }

    /**
     * 测试不同主题模式下的颜色方案
     */
    @Test
    fun testThemeModesHaveValidColorSchemes() {
        val themeModes = ThemeMode.values()

        for (mode in themeModes) {
            // 测试亮色模式的颜色方案
            composeTestRule.setContent {
                EventNoteTheme(themeMode = mode, darkTheme = false) {
                    val colorScheme = MaterialTheme.colorScheme
                    assertNotNull("$mode 亮色模式 colorScheme 不应为 null", colorScheme)
                    assertNotNull("$mode 亮色模式 primary", colorScheme.primary)
                    assertNotNull("$mode 亮色模式 background", colorScheme.background)
                    assertNotNull("$mode 亮色模式 surface", colorScheme.surface)
                }
            }

            // 对于非 OLED 主题，也测试暗色模式
            if (mode != ThemeMode.DARK_OLED) {
                composeTestRule.setContent {
                    EventNoteTheme(themeMode = mode, darkTheme = true) {
                        val colorScheme = MaterialTheme.colorScheme
                        assertNotNull("$mode 暗色模式 colorScheme 不应为 null", colorScheme)
                    }
                }
            }
        }
    }

    /**
     * 测试 DARK_OLED 主题强制暗色
     */
    @Test
    fun testDarkOledForcesDarkTheme() {
        composeTestRule.setContent {
            // 即使传入 darkTheme = false，DARK_OLED 也应该使用暗色方案
            EventNoteTheme(themeMode = ThemeMode.DARK_OLED, darkTheme = false) {
                val colorScheme = MaterialTheme.colorScheme
                // 验证是暗色方案的背景色（纯黑）
                assertEquals("DARK_OLED 背景应为纯黑", 
                    android.graphics.Color.BLACK, 
                    colorScheme.background.toArgb())
            }
        }
    }

    /**
     * 测试主题切换流畅性
     */
    @Test
    fun testThemeSwitching() {
        // 测试在不同主题间切换不会崩溃
        val themeModes = listOf(ThemeMode.SYSTEM, ThemeMode.DESKTOP_INVERTED, ThemeMode.CLASSIC_MATERIAL)

        for (mode in themeModes) {
            composeTestRule.setContent {
                EventNoteTheme(themeMode = mode, darkTheme = false) {
                    // 简单地渲染一些内容，确保主题应用成功
                    androidx.compose.foundation.layout.Box(
                        androidx.compose.foundation.layout.BoxDefaults.modifier
                    ) {}
                }
            }
        }
    }

    /**
     * 测试 ThemeManager 与 EventNoteTheme 的集成
     */
    @Test
    fun testThemeManagerIntegration() = runBlocking {
        val context = LocalContext.current
        val themeManager = ThemeManager.getInstance(context)

        // 测试默认主题
        val defaultTheme = themeManager.getCurrentThemeMode()
        assertEquals("默认主题应为 DESKTOP_INVERTED", ThemeMode.DESKTOP_INVERTED, defaultTheme)

        // 测试设置主题后 Flow 发射正确值
        themeManager.setThemeMode(ThemeMode.CLASSIC_MATERIAL)
        val flowTheme = themeManager.themeModeFlow.first()
        assertEquals("Flow 应发射设置的主题", ThemeMode.CLASSIC_MATERIAL, flowTheme)
    }
}