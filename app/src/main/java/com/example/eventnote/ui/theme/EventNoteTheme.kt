package com.example.eventnote.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// 主题模式枚举（与 ThemeManager 保持一致）
enum class ThemeMode(val displayName: String) {
    SYSTEM("跟随系统"),
    DESKTOP_INVERTED("桌面反色"),
    CLASSIC_MATERIAL("经典 Material"),
    DARK_OLED("深色 OLED")
}

// ==================== Apple 风格颜色系统 ====================

// 亮色主题 - Apple iOS 风格
private val AppleLightColorScheme = lightColorScheme(
    // 主色 - iOS 蓝
    primary = Color(0xFF007AFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),

    // 次要色
    secondary = Color(0xFF5856D6),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE4DFFF),
    onSecondaryContainer = Color(0xFF12005E),

    // 第三色 - iOS 绿
    tertiary = Color(0xFF34C759),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDFF5E3),
    onTertiaryContainer = Color(0xFF002106),

    // 错误色 - iOS 红
    error = Color(0xFFFF3B30),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD4),
    onErrorContainer = Color(0xFF410002),

    // 背景 - iOS 系统白/灰
    background = Color(0xFFF2F2F7),
    onBackground = Color(0xFF1C1C1E),

    // 表面 - 卡片白色
    surface = Color.White,
    onSurface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFFE5E5EA),
    onSurfaceVariant = Color(0xFF3C3C43).copy(alpha = 0.6f),

    // 轮廓
    outline = Color(0xFF3C3C43).copy(alpha = 0.3f),
    outlineVariant = Color(0xFF3C3C43).copy(alpha = 0.15f),

    // 辅助表面
    surfaceTint = Color(0xFF007AFF),
    inverseSurface = Color(0xFF2C2C2E),
    inverseOnSurface = Color(0xFFF2F2F7),
    inversePrimary = Color(0xFFA9CFFF),

    // 覆盖层
    scrim = Color.Black.copy(alpha = 0.3f),
)

// 深色主题 - Apple iOS 深色模式风格
private val AppleDarkColorScheme = darkColorScheme(
    primary = Color(0xFF0A84FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF004A99),
    onPrimaryContainer = Color(0xFFD1E4FF),

    secondary = Color(0xFF7D7AFF),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF3634A3),
    onSecondaryContainer = Color(0xFFE4DFFF),

    tertiary = Color(0xFF30D158),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF1A5E2B),
    onTertiaryContainer = Color(0xFFDFF5E3),

    error = Color(0xFFFF453A),
    onError = Color.White,
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFFFDAD4),

    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),

    surface = Color(0xFF1C1C1E),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFFEBEBF5).copy(alpha = 0.6f),

    outline = Color(0xFFEBEBF5).copy(alpha = 0.2f),
    outlineVariant = Color(0xFFEBEBF5).copy(alpha = 0.1f),

    surfaceTint = Color(0xFF0A84FF),
    inverseSurface = Color(0xFFFFFFFF),
    inverseOnSurface = Color(0xFF000000),
    inversePrimary = Color(0xFF0055B3),

    scrim = Color.Black.copy(alpha = 0.5f),
)

// ==================== 桌面反色主题 ====================
// 高对比度亮色，反色风格，背景纯白/浅灰，文字纯黑/深灰
private val DesktopInvertedLightColorScheme = lightColorScheme(
    primary = Color(0xFF0066CC), // 深蓝主色
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB3D7FF),
    onPrimaryContainer = Color(0xFF001D36),

    secondary = Color(0xFF6200EE), // 紫色辅助
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8D6FF),
    onSecondaryContainer = Color(0xFF12005E),

    tertiary = Color(0xFF00C853), // 绿色第三色
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFB9F6CA),
    onTertiaryContainer = Color(0xFF002106),

    error = Color(0xFFD32F2F), // 错误红
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    background = Color(0xFFFFFFFF), // 纯白背景
    onBackground = Color(0xFF000000), // 纯黑文字

    surface = Color(0xFFF5F5F5), // 浅灰表面
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFE0E0E0),
    onSurfaceVariant = Color(0xFF333333).copy(alpha = 0.8f),

    outline = Color(0xFF666666).copy(alpha = 0.5f),
    outlineVariant = Color(0xFF999999).copy(alpha = 0.3f),

    surfaceTint = Color(0xFF0066CC),
    inverseSurface = Color(0xFF000000),
    inverseOnSurface = Color(0xFFFFFFFF),
    inversePrimary = Color(0xFF85B8FF),

    scrim = Color.Black.copy(alpha = 0.4f),
)

// 桌面反色 - 暗色变体（高对比度，黑底白字）
private val DesktopInvertedDarkColorScheme = darkColorScheme(
    primary = Color(0xFF4D94FF), // 亮蓝主色
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF003366),
    onPrimaryContainer = Color(0xFFD6E8FF),

    secondary = Color(0xFF9C4DCA), // 亮紫辅助
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF4A0072),
    onSecondaryContainer = Color(0xFFE8D6FF),

    tertiary = Color(0xFF4CAF50), // 亮绿
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF1B5E20),
    onTertiaryContainer = Color(0xFFB9F6CA),

    error = Color(0xFFEF5350), // 亮红
    onError = Color.Black,
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFFFDAD6),

    background = Color(0xFF000000), // 纯黑背景
    onBackground = Color(0xFFFFFFFF), // 纯白文字

    surface = Color(0xFF121212), // 深灰表面
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFFE0E0E0).copy(alpha = 0.9f),

    outline = Color(0xFFCCCCCC).copy(alpha = 0.4f),
    outlineVariant = Color(0xFF888888).copy(alpha = 0.2f),

    surfaceTint = Color(0xFF4D94FF),
    inverseSurface = Color(0xFFFFFFFF),
    inverseOnSurface = Color(0xFF000000),
    inversePrimary = Color(0xFF003366),

    scrim = Color.Black.copy(alpha = 0.6f),
)

// ==================== 经典 Material 主题 ====================
// Material You 标准配色（紫粉色系）
private val ClassicMaterialLightColorScheme = lightColorScheme(
    primary = Color(0xFF6200EE),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8D6FF),
    onPrimaryContainer = Color(0xFF12005E),

    secondary = Color(0xFF03DAC6),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFCAFDF5),
    onSecondaryContainer = Color(0xFF006A5D),

    tertiary = Color(0xFF018786),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF97F0E9),
    onTertiaryContainer = Color(0xFF002B2C),

    error = Color(0xFFB00020),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    background = Color(0xFFFFFBFE), // Material 标准浅背景
    onBackground = Color(0xFF1C1B1F),

    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),

    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),

    surfaceTint = Color(0xFF6200EE),
    inverseSurface = Color(0xFF313033),
    inverseOnSurface = Color(0xFFF4F4F5),
    inversePrimary = Color(0xFFCDB2FF),

    scrim = Color.Black.copy(alpha = 0.4f),
)

private val ClassicMaterialDarkColorScheme = darkColorScheme(
    primary = Color(0xFFBB86FC),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF4A3788),
    onPrimaryContainer = Color(0xFFEADDFF),

    secondary = Color(0xFF03DAC6),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF1A8376),
    onSecondaryContainer = Color(0xFFCAFDF5),

    tertiary = Color(0xFF4DB6AC),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF1A4D47),
    onTertiaryContainer = Color(0xFF97F0E9),

    error = Color(0xFFCF6679),
    onError = Color.Black,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFBAB1),

    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),

    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),

    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),

    surfaceTint = Color(0xFFBB86FC),
    inverseSurface = Color(0xFFE6E1E5),
    inverseOnSurface = Color(0xFF313033),
    inversePrimary = Color(0xFF6200EE),

    scrim = Color.Black.copy(alpha = 0.6f),
)

// ==================== 深色 OLED 主题 ====================
// 纯黑背景，极致省电，高对比度
private val DarkOledLightColorScheme = lightColorScheme(
    // 亮色模式下仍保持较低亮度，避免刺眼
    primary = Color(0xFF0A84FF),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFFCAE4FF),
    onPrimaryContainer = Color(0xFF001D36),

    secondary = Color(0xFF5E5CE6),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFDEDCFF),
    onSecondaryContainer = Color(0xFF12005E),

    tertiary = Color(0xFF30D158),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFFCAFBD7),
    onTertiaryContainer = Color(0xFF002106),

    error = Color(0xFFFF453A),
    onError = Color.Black,
    errorContainer = Color(0xFFFFDAD4),
    onErrorContainer = Color(0xFF410002),

    background = Color(0xFF0A0A0A), // 深灰黑背景，避免纯黑在浅色模式下过于刺眼
    onBackground = Color(0xFFFFFFFF),

    surface = Color(0xFF1C1C1E),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFFCBCBD0).copy(alpha = 0.8f),

    outline = Color(0xFF8E8E93).copy(alpha = 0.4f),
    outlineVariant = Color(0xFF48484A),

    surfaceTint = Color(0xFF0A84FF),
    inverseSurface = Color(0xFFE5E5EA),
    inverseOnSurface = Color(0xFF000000),
    inversePrimary = Color(0xFF0055B3),

    scrim = Color.Black.copy(alpha = 0.5f),
)

private val DarkOledDarkColorScheme = darkColorScheme(
    primary = Color(0xFF0A84FF),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF003366),
    onPrimaryContainer = Color(0xFFCAE4FF),

    secondary = Color(0xFF7D7AFF),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF2A2580),
    onSecondaryContainer = Color(0xFFDEDCFF),

    tertiary = Color(0xFF30D158),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF1A5E2B),
    onTertiaryContainer = Color(0xFFCAFBD7),

    error = Color(0xFFFF453A),
    onError = Color.Black,
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFFFDAD4),

    background = Color(0xFF000000), // 纯黑背景，OLED 省电
    onBackground = Color(0xFFFFFFFF),

    surface = Color(0xFF000000), // 表面也纯黑
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF1C1C1E),
    onSurfaceVariant = Color(0xFFCBCBD0).copy(alpha = 0.8f),

    outline = Color(0xFF8E8E93).copy(alpha = 0.3f),
    outlineVariant = Color(0xFF3A3A3C),

    surfaceTint = Color(0xFF0A84FF),
    inverseSurface = Color(0xFFE5E5EA),
    inverseOnSurface = Color(0xFF000000),
    inversePrimary = Color(0xFF0055B3),

    scrim = Color.Black.copy(alpha = 0.7f),
)

// ==================== 主题选择逻辑 ====================

// 添加全局颜色变量
val PrimaryColor = Color(0xFF007AFF)
val SecondaryColor = Color(0xFF5856D6)
val BackgroundLight = Color(0xFFF2F2F7)
val BackgroundDark = Color(0xFF000000)

// 定义深色模式和浅色模式的配色方案
val LightColorScheme = lightColorScheme(
    primary = PrimaryColor,
    secondary = SecondaryColor,
    background = BackgroundLight,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black
)

val DarkColorScheme = darkColorScheme(
    primary = PrimaryColor,
    secondary = SecondaryColor,
    background = BackgroundDark,
    surface = Color(0xFF1C1C1E),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

/**
 * 根据 ThemeMode 和系统暗色状态，选择合适的颜色方案
 */
private fun getColorSchemeForMode(themeMode: ThemeMode, isSystemDark: Boolean): androidx.compose.material3.ColorScheme {
    return when (themeMode) {
        ThemeMode.SYSTEM -> {
            if (isSystemDark) AppleDarkColorScheme else AppleLightColorScheme
        }
        ThemeMode.DESKTOP_INVERTED -> {
            if (isSystemDark) DesktopInvertedDarkColorScheme else DesktopInvertedLightColorScheme
        }
        ThemeMode.CLASSIC_MATERIAL -> {
            if (isSystemDark) ClassicMaterialDarkColorScheme else ClassicMaterialLightColorScheme
        }
        ThemeMode.DARK_OLED -> {
            // DARK_OLED 强制使用暗色方案，无论系统状态
            DarkOledDarkColorScheme
        }
    }
}

/**
 * 获取对应主题的状态栏颜色
 */
private fun getStatusBarColorForMode(themeMode: ThemeMode, isSystemDark: Boolean): Color {
    return when (themeMode) {
        ThemeMode.SYSTEM -> {
            if (isSystemDark) Color(0xFF000000) else Color(0xFFF2F2F7)
        }
        ThemeMode.DESKTOP_INVERTED -> {
            if (isSystemDark) Color(0xFF000000) else Color(0xFFFFFFFF)
        }
        ThemeMode.CLASSIC_MATERIAL -> {
            if (isSystemDark) Color(0xFF1C1B1F) else Color(0xFFFFFBFE)
        }
        ThemeMode.DARK_OLED -> {
            // 深色 OLED 使用纯黑
            Color(0xFF000000)
        }
    }
}

@Composable
fun EventNoteTheme(
    themeMode: ThemeMode = ThemeMode.DESKTOP_INVERTED, // 默认桌面反色
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // 根据主题模式确定最终使用的颜色方案
    val effectiveDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> darkTheme
        ThemeMode.DESKTOP_INVERTED -> darkTheme
        ThemeMode.CLASSIC_MATERIAL -> darkTheme
        ThemeMode.DARK_OLED -> true // 强制暗色
    }

    val colorScheme = getColorSchemeForMode(themeMode, effectiveDarkTheme)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // 状态栏颜色根据主题模式适配
            window.statusBarColor = getStatusBarColorForMode(themeMode, effectiveDarkTheme).toArgb()
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppleTypography,
        content = content
    )
}
