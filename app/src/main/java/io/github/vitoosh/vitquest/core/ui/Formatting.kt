package io.github.vitoosh.vitquest.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import java.text.NumberFormat
import java.util.Locale

/** The locale currently used for resources, which follows the per-app language. */
@Composable
@ReadOnlyComposable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

@Composable
@ReadOnlyComposable
fun formatInteger(value: Int): String = NumberFormat.getIntegerInstance(currentLocale()).format(value)

@Composable
@ReadOnlyComposable
fun formatDecimal(value: Double): String = NumberFormat.getNumberInstance(currentLocale()).apply {
    minimumFractionDigits = 0
    maximumFractionDigits = 1
}.format(value)
