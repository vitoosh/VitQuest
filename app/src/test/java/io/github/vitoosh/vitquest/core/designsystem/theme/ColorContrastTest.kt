package io.github.vitoosh.vitquest.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Text colours must reach WCAG 2.1 AA contrast (4.5:1 for normal text) on the
 * backgrounds they are used on, in both themes.
 */
class ColorContrastTest {

    private fun contrast(foreground: Color, background: Color): Double {
        val lighter = maxOf(foreground.luminance(), background.luminance()) + 0.05
        val darker = minOf(foreground.luminance(), background.luminance()) + 0.05
        return lighter.toDouble() / darker
    }

    private fun pairs(scheme: ColorScheme) = mapOf(
        "onPrimary/primary" to (scheme.onPrimary to scheme.primary),
        "onPrimaryContainer/primaryContainer" to (scheme.onPrimaryContainer to scheme.primaryContainer),
        "onSecondaryContainer/secondaryContainer" to (scheme.onSecondaryContainer to scheme.secondaryContainer),
        "onTertiaryContainer/tertiaryContainer" to (scheme.onTertiaryContainer to scheme.tertiaryContainer),
        "onBackground/background" to (scheme.onBackground to scheme.background),
        "onSurface/surface" to (scheme.onSurface to scheme.surface),
        "onSurfaceVariant/surface" to (scheme.onSurfaceVariant to scheme.surface),
        "onSurfaceVariant/surfaceContainerLow" to (scheme.onSurfaceVariant to scheme.surfaceContainerLow),
        "onSurface/surfaceContainerHighest" to (scheme.onSurface to scheme.surfaceContainerHighest),
        "primary/surface" to (scheme.primary to scheme.surface),
        "primary/surfaceContainerLow" to (scheme.primary to scheme.surfaceContainerLow),
        "error/surface" to (scheme.error to scheme.surface),
        "onError/error" to (scheme.onError to scheme.error),
        "onErrorContainer/errorContainer" to (scheme.onErrorContainer to scheme.errorContainer),
    )

    private fun check(name: String, scheme: ColorScheme) {
        val failures = pairs(scheme).mapValues { (_, colors) -> contrast(colors.first, colors.second) }.filterValues { it < 4.5 }
        assertTrue("$name theme below 4.5:1: ${failures.mapValues { "%.2f".format(it.value) }}", failures.isEmpty())
    }

    @Test
    fun lightThemeTextIsReadable() = check("Light", LightColors)

    @Test
    fun darkThemeTextIsReadable() = check("Dark", DarkColors)
}
