package io.github.vitoosh.vitquest.core.model

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val onboardingCompleted: Boolean = false,
)
