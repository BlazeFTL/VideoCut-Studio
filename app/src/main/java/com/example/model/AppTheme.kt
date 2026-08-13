package com.example.model

import androidx.compose.ui.graphics.Color

enum class AppTheme(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val isMixed: Boolean,
    val primaryColor: Color,
    val secondaryColor: Color,
    val primaryContainer: Color,
    val secondaryContainer: Color,
    val onPrimaryContainer: Color
) {
    // 6 Static Themes
    MODERN_INDIGO(
        id = "indigo_static",
        title = "Modern Indigo",
        subtitle = "Classic & Professional Studio Indigo",
        badge = "DEFAULT",
        isMixed = false,
        primaryColor = Color(0xFF6366F1),
        secondaryColor = Color(0xFF8B5CF6),
        primaryContainer = Color(0xFFEEF2FF),
        secondaryContainer = Color(0xFFF3E8FF),
        onPrimaryContainer = Color(0xFF312E81)
    ),
    EMERALD_GREEN(
        id = "emerald_static",
        title = "Emerald Green",
        subtitle = "Fresh & Vibrant Nature Emerald",
        badge = "POPULAR",
        isMixed = false,
        primaryColor = Color(0xFF10B981),
        secondaryColor = Color(0xFF059669),
        primaryContainer = Color(0xFFD1FAE5),
        secondaryContainer = Color(0xFFA7F3D0),
        onPrimaryContainer = Color(0xFF065F46)
    ),
    SUNSET_ORANGE(
        id = "orange_static",
        title = "Sunset Orange",
        subtitle = "Warm & Energetic Amber Glow",
        badge = "WARM",
        isMixed = false,
        primaryColor = Color(0xFFF97316),
        secondaryColor = Color(0xFFEA580C),
        primaryContainer = Color(0xFFFFEDD5),
        secondaryContainer = Color(0xFFFED7AA),
        onPrimaryContainer = Color(0xFF9A3412)
    ),
    ROYAL_PURPLE(
        id = "purple_static",
        title = "Royal Purple",
        subtitle = "Deep & Luxurious Violet",
        badge = "ELEGANT",
        isMixed = false,
        primaryColor = Color(0xFF8B5CF6),
        secondaryColor = Color(0xFF7C3AED),
        primaryContainer = Color(0xFFF3E8FF),
        secondaryContainer = Color(0xFFDDD6FE),
        onPrimaryContainer = Color(0xFF5B21B6)
    ),
    OCEAN_CYAN(
        id = "cyan_static",
        title = "Ocean Cyan",
        subtitle = "Crisp & Modern Sky Teal",
        badge = "FRESH",
        isMixed = false,
        primaryColor = Color(0xFF06B6D4),
        secondaryColor = Color(0xFF0284C7),
        primaryContainer = Color(0xFFCFFAFE),
        secondaryContainer = Color(0xFFA5F3FC),
        onPrimaryContainer = Color(0xFF155E75)
    ),
    CRIMSON_ROSE(
        id = "rose_static",
        title = "Crimson Rose",
        subtitle = "Bold & Punchy Crimson Accent",
        badge = "BOLD",
        isMixed = false,
        primaryColor = Color(0xFFF43F5E),
        secondaryColor = Color(0xFFE11D48),
        primaryContainer = Color(0xFFFFE4E6),
        secondaryContainer = Color(0xFFFECDD3),
        onPrimaryContainer = Color(0xFF9F1239)
    ),

    // 6 Mixed Accents Themes
    INDIGO_ROSE(
        id = "indigo_rose_mixed",
        title = "Indigo & Rose",
        subtitle = "Studio Indigo with striking Rose highlights",
        badge = "DUAL",
        isMixed = true,
        primaryColor = Color(0xFF6366F1),
        secondaryColor = Color(0xFFF43F5E),
        primaryContainer = Color(0xFFEEF2FF),
        secondaryContainer = Color(0xFFFFE4E6),
        onPrimaryContainer = Color(0xFF312E81)
    ),
    CYAN_AMBER(
        id = "cyan_amber_mixed",
        title = "Cyan & Amber",
        subtitle = "Cool Cyan paired with warm Sun Amber",
        badge = "DUAL",
        isMixed = true,
        primaryColor = Color(0xFF06B6D4),
        secondaryColor = Color(0xFFF59E0B),
        primaryContainer = Color(0xFFCFFAFE),
        secondaryContainer = Color(0xFFFEF3C7),
        onPrimaryContainer = Color(0xFF155E75)
    ),
    EMERALD_PURPLE(
        id = "emerald_purple_mixed",
        title = "Emerald & Purple",
        subtitle = "Emerald base with Royal Purple trims",
        badge = "DUAL",
        isMixed = true,
        primaryColor = Color(0xFF10B981),
        secondaryColor = Color(0xFF8B5CF6),
        primaryContainer = Color(0xFFD1FAE5),
        secondaryContainer = Color(0xFFF3E8FF),
        onPrimaryContainer = Color(0xFF065F46)
    ),
    VIOLET_GOLD(
        id = "violet_gold_mixed",
        title = "Violet & Gold",
        subtitle = "Deep Violet paired with Radiant Gold",
        badge = "DUAL",
        isMixed = true,
        primaryColor = Color(0xFF8B5CF6),
        secondaryColor = Color(0xFFEAB308),
        primaryContainer = Color(0xFFF3E8FF),
        secondaryContainer = Color(0xFFFEF9C3),
        onPrimaryContainer = Color(0xFF5B21B6)
    ),
    OCEAN_CORAL(
        id = "ocean_coral_mixed",
        title = "Ocean & Coral",
        subtitle = "Deep Ocean Blue with Coral accents",
        badge = "DUAL",
        isMixed = true,
        primaryColor = Color(0xFF2563EB),
        secondaryColor = Color(0xFFFF6B6B),
        primaryContainer = Color(0xFFDBEAFE),
        secondaryContainer = Color(0xFFFFE3E3),
        onPrimaryContainer = Color(0xFF1E40AF)
    ),
    TEAL_MAGENTA(
        id = "teal_magenta_mixed",
        title = "Teal & Magenta",
        subtitle = "Vibrant Teal with Electric Magenta punch",
        badge = "DUAL",
        isMixed = true,
        primaryColor = Color(0xFF14B8A6),
        secondaryColor = Color(0xFFEC4899),
        primaryContainer = Color(0xFFCCFBF1),
        secondaryContainer = Color(0xFFFCE7F3),
        onPrimaryContainer = Color(0xFF115E59)
    );

    companion object {
        fun fromId(id: String): AppTheme = values().firstOrNull { it.id == id } ?: MODERN_INDIGO
    }
}
