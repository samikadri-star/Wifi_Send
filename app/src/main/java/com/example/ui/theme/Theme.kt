package com.example.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

enum class AppThemeStyle(
    val titleArabic: String,
    val descriptionArabic: String,
    val primaryColor: Color,
    val surfaceColor: Color,
    val backgroundColor: Color,
    val isDark: Boolean
) {
    IMMERSIVE_CYAN(
        titleArabic = "أزرق ثلجي مستقبلي",
        descriptionArabic = "أسلوب غامر مستوحى من تقنية Wi-Fi Direct البرّاقة",
        primaryColor = PrimaryCyan,
        surfaceColor = DarkSurface,
        backgroundColor = DarkBackground,
        isDark = true
    ),
    NEON_VIOLET(
        titleArabic = "بنفسجي نيون غامر",
        descriptionArabic = "طابع عصري بألوان البنفسجي المضيء والليلي",
        primaryColor = VioletPrimary,
        surfaceColor = VioletSurface,
        backgroundColor = VioletBackground,
        isDark = true
    ),
    EMERALD_FUTURE(
        titleArabic = "زمردي سايبر",
        descriptionArabic = "نمط زمردي أنيق وعالي التباين لرواد التقنية",
        primaryColor = EmeraldPrimary,
        surfaceColor = EmeraldSurface,
        backgroundColor = EmeraldBackground,
        isDark = true
    ),
    SUNSET_GOLD(
        titleArabic = "ذهبي دافئ غامر",
        descriptionArabic = "مظهر ذهبي دافئ وفاخر ينبض بالحيوية",
        primaryColor = GoldPrimary,
        surfaceColor = GoldSurface,
        backgroundColor = GoldBackground,
        isDark = true
    ),
    NEON_RUBY(
        titleArabic = "ياقوتي فاخر",
        descriptionArabic = "لمسات ياقوتية نيونية جذابة وشديدة التباين",
        primaryColor = RubyPrimary,
        surfaceColor = RubySurface,
        backgroundColor = RubyBackground,
        isDark = true
    ),
    CLEAN_LIGHT(
        titleArabic = "أبيض ناصع عصري",
        descriptionArabic = "تصميم فاتح مريح للعين مع ألوان تباين ممتازة",
        primaryColor = LightPrimary,
        surfaceColor = LightSurface,
        backgroundColor = LightBackground,
        isDark = false
    )
}

fun getCustomColorScheme(themeStyle: AppThemeStyle): ColorScheme {
    return when (themeStyle) {
        AppThemeStyle.IMMERSIVE_CYAN -> darkColorScheme(
            primary = PrimaryCyan,
            onPrimary = DarkBackground,
            primaryContainer = PrimaryBlue,
            onPrimaryContainer = TextPrimaryDark,
            secondary = SecondaryBlue,
            onSecondary = DarkBackground,
            background = DarkBackground,
            onBackground = TextPrimaryDark,
            surface = DarkSurface,
            onSurface = TextPrimaryDark,
            surfaceVariant = DarkSurfaceVariant,
            onSurfaceVariant = TextSecondaryDark,
            error = AccentRed,
            tertiary = AccentNeonGreen
        )
        AppThemeStyle.NEON_VIOLET -> darkColorScheme(
            primary = VioletPrimary,
            onPrimary = VioletBackground,
            primaryContainer = VioletContainer,
            onPrimaryContainer = TextPrimaryDark,
            secondary = VioletSecondary,
            onSecondary = VioletBackground,
            background = VioletBackground,
            onBackground = TextPrimaryDark,
            surface = VioletSurface,
            onSurface = TextPrimaryDark,
            surfaceVariant = VioletSurfaceVariant,
            onSurfaceVariant = TextSecondaryDark,
            error = AccentRed,
            tertiary = AccentNeonGreen
        )
        AppThemeStyle.EMERALD_FUTURE -> darkColorScheme(
            primary = EmeraldPrimary,
            onPrimary = EmeraldBackground,
            primaryContainer = EmeraldContainer,
            onPrimaryContainer = TextPrimaryDark,
            secondary = EmeraldSecondary,
            onSecondary = EmeraldBackground,
            background = EmeraldBackground,
            onBackground = TextPrimaryDark,
            surface = EmeraldSurface,
            onSurface = TextPrimaryDark,
            surfaceVariant = EmeraldSurfaceVariant,
            onSurfaceVariant = TextSecondaryDark,
            error = AccentRed,
            tertiary = AccentNeonGreen
        )
        AppThemeStyle.SUNSET_GOLD -> darkColorScheme(
            primary = GoldPrimary,
            onPrimary = GoldBackground,
            primaryContainer = GoldContainer,
            onPrimaryContainer = TextPrimaryDark,
            secondary = GoldSecondary,
            onSecondary = GoldBackground,
            background = GoldBackground,
            onBackground = TextPrimaryDark,
            surface = GoldSurface,
            onSurface = TextPrimaryDark,
            surfaceVariant = GoldSurfaceVariant,
            onSurfaceVariant = TextSecondaryDark,
            error = AccentRed,
            tertiary = AccentNeonGreen
        )
        AppThemeStyle.NEON_RUBY -> darkColorScheme(
            primary = RubyPrimary,
            onPrimary = RubyBackground,
            primaryContainer = RubyContainer,
            onPrimaryContainer = TextPrimaryDark,
            secondary = RubySecondary,
            onSecondary = RubyBackground,
            background = RubyBackground,
            onBackground = TextPrimaryDark,
            surface = RubySurface,
            onSurface = TextPrimaryDark,
            surfaceVariant = RubySurfaceVariant,
            onSurfaceVariant = TextSecondaryDark,
            error = AccentRed,
            tertiary = AccentNeonGreen
        )
        AppThemeStyle.CLEAN_LIGHT -> lightColorScheme(
            primary = LightPrimary,
            onPrimary = LightSurface,
            primaryContainer = LightContainer,
            onPrimaryContainer = Color(0xFF0369A1),
            secondary = Color(0xFF38BDF8),
            onSecondary = LightSurface,
            background = LightBackground,
            onBackground = TextPrimaryLight,
            surface = LightSurface,
            onSurface = TextPrimaryLight,
            surfaceVariant = LightSurfaceVariant,
            onSurfaceVariant = TextSecondaryLight,
            error = AccentRed,
            tertiary = AccentNeonGreen
        )
    }
}

@Composable
fun MyApplicationTheme(
    themeStyle: AppThemeStyle = AppThemeStyle.IMMERSIVE_CYAN,
    content: @Composable () -> Unit
) {
    val colorScheme = getCustomColorScheme(themeStyle)

    // Enforce RTL Layout Direction for Arabic Interface
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

