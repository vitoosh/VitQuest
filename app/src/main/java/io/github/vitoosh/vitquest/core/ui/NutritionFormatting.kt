package io.github.vitoosh.vitquest.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import io.github.vitoosh.vitquest.R
import kotlin.math.abs
import kotlin.math.roundToInt

/** Picks the Indonesian or English variant of bundled content to match the UI language. */
@Composable
@ReadOnlyComposable
fun localized(indonesian: String, english: String): String =
    if (currentLocale().language in INDONESIAN_CODES) indonesian else english

@Composable
@ReadOnlyComposable
fun isIndonesian(): Boolean = currentLocale().language in INDONESIAN_CODES

private val INDONESIAN_CODES = setOf("in", "id")

@Composable
@ReadOnlyComposable
fun formatKcal(kcal: Double): String = stringResource(R.string.value_kcal, formatInteger(kcal.roundToInt()))

@Composable
@ReadOnlyComposable
fun formatKcal(kcal: Int): String = stringResource(R.string.value_kcal, formatInteger(kcal))

/** Grams with one decimal at most; a dash when unknown. */
@Composable
@ReadOnlyComposable
fun formatGrams(grams: Double?): String =
    grams?.let { stringResource(R.string.value_grams, formatDecimal(it)) } ?: "–"

/** Kitchen-friendly amounts: ¼, ½ and ¾ as fractions, otherwise up to one decimal. */
@Composable
@ReadOnlyComposable
fun formatQuantity(quantity: Double): String {
    val whole = quantity.toInt()
    val fraction = quantity - whole
    val symbol = FRACTIONS.entries.firstOrNull { abs(it.key - fraction) < 0.02 }?.value
    return when {
        abs(fraction) < 0.02 -> formatInteger(whole)
        symbol != null && whole == 0 -> symbol
        symbol != null -> "${formatInteger(whole)}$symbol"
        else -> formatDecimal(quantity)
    }
}

private val FRACTIONS = mapOf(0.25 to "¼", 0.5 to "½", 0.75 to "¾")
