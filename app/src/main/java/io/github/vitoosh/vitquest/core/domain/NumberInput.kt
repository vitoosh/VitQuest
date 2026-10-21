package io.github.vitoosh.vitquest.core.domain

/** Why a numeric form field was rejected. */
enum class FieldError { REQUIRED, INVALID_NUMBER, OUT_OF_RANGE }

/** Result of parsing a single form field. A blank optional field is valid with a null value. */
sealed interface FieldResult<out T> {
    data class Valid<T>(val value: T?) : FieldResult<T>
    data class Invalid(val error: FieldError) : FieldResult<Nothing>
}

val FieldResult<*>.errorOrNull: FieldError?
    get() = (this as? FieldResult.Invalid)?.error

/**
 * Parses numbers typed by users. Accepts both "," and "." as the decimal
 * separator because Indonesian keyboards commonly use a comma.
 */
object NumberInput {

    private val DECIMAL_PATTERN = Regex("""^\d{1,4}([.,]\d{1,2})?$""")
    private val INTEGER_PATTERN = Regex("""^\d{1,6}$""")

    fun parseDecimal(
        input: String,
        range: ClosedFloatingPointRange<Double>,
        required: Boolean = false,
    ): FieldResult<Double> {
        val text = input.trim()
        if (text.isEmpty()) return emptyResult(required)
        if (!DECIMAL_PATTERN.matches(text)) return FieldResult.Invalid(FieldError.INVALID_NUMBER)
        val value = text.replace(',', '.').toDouble()
        return if (value in range) FieldResult.Valid(value) else FieldResult.Invalid(FieldError.OUT_OF_RANGE)
    }

    fun parseInt(
        input: String,
        range: IntRange,
        required: Boolean = false,
    ): FieldResult<Int> {
        // Allow thousands separators such as "8.000" or "8,000".
        val text = input.trim().replace(".", "").replace(",", "")
        if (text.isEmpty()) return emptyResult(required)
        if (!INTEGER_PATTERN.matches(text)) return FieldResult.Invalid(FieldError.INVALID_NUMBER)
        val value = text.toInt()
        return if (value in range) FieldResult.Valid(value) else FieldResult.Invalid(FieldError.OUT_OF_RANGE)
    }

    private fun <T> emptyResult(required: Boolean): FieldResult<T> =
        if (required) FieldResult.Invalid(FieldError.REQUIRED) else FieldResult.Valid(null)
}
