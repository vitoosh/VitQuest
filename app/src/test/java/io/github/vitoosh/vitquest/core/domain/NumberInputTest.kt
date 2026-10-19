package io.github.vitoosh.vitquest.core.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class NumberInputTest {

    private val weightRange = InputLimits.WEIGHT_KG

    @Test
    fun blankOptionalFieldIsValidAndEmpty() {
        assertEquals(FieldResult.Valid<Double>(null), NumberInput.parseDecimal("  ", weightRange))
    }

    @Test
    fun blankRequiredFieldIsRejected() {
        assertEquals(
            FieldResult.Invalid(FieldError.REQUIRED),
            NumberInput.parseInt("", InputLimits.DAILY_STEPS, required = true),
        )
    }

    @Test
    fun decimalAcceptsCommaAndDot() {
        assertEquals(FieldResult.Valid(72.5), NumberInput.parseDecimal("72,5", weightRange))
        assertEquals(FieldResult.Valid(72.5), NumberInput.parseDecimal("72.5", weightRange))
        assertEquals(FieldResult.Valid(72.0), NumberInput.parseDecimal(" 72 ", weightRange))
    }

    @Test
    fun malformedDecimalsAreRejected() {
        listOf("72,5,1", "abc", "-70", "7e1", "72.", ",5", "1.234").forEach { input ->
            assertEquals(input, FieldResult.Invalid(FieldError.INVALID_NUMBER), NumberInput.parseDecimal(input, weightRange))
        }
    }

    @Test
    fun valuesOutsideRangeAreRejectedNotClamped() {
        assertEquals(FieldResult.Invalid(FieldError.OUT_OF_RANGE), NumberInput.parseDecimal("29.9", weightRange))
        assertEquals(FieldResult.Invalid(FieldError.OUT_OF_RANGE), NumberInput.parseDecimal("300.1", weightRange))
        assertEquals(FieldResult.Valid(30.0), NumberInput.parseDecimal("30", weightRange))
    }

    @Test
    fun integersAcceptThousandsSeparators() {
        assertEquals(FieldResult.Valid(8_000), NumberInput.parseInt("8.000", InputLimits.DAILY_STEPS))
        assertEquals(FieldResult.Valid(8_000), NumberInput.parseInt("8,000", InputLimits.DAILY_STEPS))
    }

    @Test
    fun invalidIntegersAreRejected() {
        assertEquals(FieldResult.Invalid(FieldError.INVALID_NUMBER), NumberInput.parseInt("12a", InputLimits.AGE_YEARS))
        assertEquals(FieldResult.Invalid(FieldError.OUT_OF_RANGE), NumberInput.parseInt("12", InputLimits.AGE_YEARS))
        assertEquals(FieldResult.Invalid(FieldError.INVALID_NUMBER), NumberInput.parseInt("9999999", InputLimits.DAILY_STEPS))
    }
}
