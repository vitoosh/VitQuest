package io.github.vitoosh.vitquest.ui

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.ai.EstimatedItem
import io.github.vitoosh.vitquest.core.designsystem.theme.VitQuestTheme
import io.github.vitoosh.vitquest.core.model.MealType
import io.github.vitoosh.vitquest.feature.nutrition.photo.FoodPhotoActions
import io.github.vitoosh.vitquest.feature.nutrition.photo.FoodPhotoScreen
import io.github.vitoosh.vitquest.feature.nutrition.photo.FoodPhotoUiState
import io.github.vitoosh.vitquest.feature.nutrition.photo.PhotoError
import io.github.vitoosh.vitquest.feature.nutrition.photo.ReviewItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "en")
class FoodPhotoScreenSmokeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun string(id: Int, vararg args: Any): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id, *args)

    private fun show(state: FoodPhotoUiState, consent: Boolean?, actions: FoodPhotoActions = FoodPhotoActions()) =
        composeRule.setContent { VitQuestTheme { FoodPhotoScreen(state, consent, onNavigateBack = {}, actions = actions) } }

    @Test
    fun nothingIsSentBeforeTheUserAgrees() {
        var agreed = false
        show(FoodPhotoUiState.Idle, consent = false, actions = FoodPhotoActions(giveConsent = { agreed = true }))
        composeRule.onNodeWithText(string(R.string.photo_consent_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.photo_take)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.photo_consent_agree)).performClick()
        assertTrue(agreed)
    }

    @Test
    fun aBuildWithoutServerSettingsSaysSo() {
        show(FoodPhotoUiState.NotConfigured, consent = true)
        composeRule.onNodeWithText(string(R.string.photo_not_configured)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.photo_take)).assertDoesNotExist()
    }

    @Test
    fun cameraAndGalleryAreOffered() {
        var took = false
        var picked = false
        show(FoodPhotoUiState.Idle, consent = true, actions = FoodPhotoActions(takePhoto = { took = true }, pickPhoto = { picked = true }))
        composeRule.onNodeWithText(string(R.string.photo_take)).performClick()
        composeRule.onNodeWithText(string(R.string.photo_pick)).performClick()
        assertTrue(took && picked)
    }

    @Test
    fun offlineCanBeRetriedButTheDailyLimitCannot() {
        var retried = 0
        show(FoodPhotoUiState.Failed(PhotoError.OFFLINE, canRetry = true), consent = true, actions = FoodPhotoActions(retry = { retried++ }))
        composeRule.onNodeWithText(string(R.string.photo_error_offline)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.action_retry)).performClick()
        assertEquals(1, retried)
    }

    @Test
    fun reviewShowsEveryItemAndSaves() {
        val removed = mutableListOf<Int>()
        var saved = false
        var meal: MealType? = null
        show(
            reviewState(),
            consent = true,
            actions = FoodPhotoActions(remove = { removed += it }, save = { saved = true }, setMeal = { meal = it }),
        )
        composeRule.onNodeWithText(string(R.string.photo_estimate_note)).assertIsDisplayed()
        composeRule.onNodeWithText("Nasi putih").assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.photo_confidence_low)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.photo_total, "455")).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription(string(R.string.photo_item_remove, "Ayam goreng")).performScrollTo().performClick()
        assertEquals(listOf(1), removed)
        composeRule.onNodeWithText(string(R.string.meal_dinner)).performScrollTo().performClick()
        assertEquals(MealType.DINNER, meal)
        composeRule.onNodeWithText(string(R.string.photo_save)).performScrollTo().performClick()
        assertTrue(saved)
    }

    @Test
    fun anEmptyReviewCannotBeSaved() {
        show(reviewState().copy(items = emptyList()), consent = true)
        composeRule.onNodeWithText(string(R.string.photo_no_items)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.photo_save)).performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun invalidItemsExplainWhatToFix() {
        val state = reviewState()
        show(state.copy(items = state.items.map { it.copy(grams = "0", error = true) }), consent = true)
        composeRule.onAllNodesWithText(string(R.string.photo_item_invalid)).assertCountEquals(2)
    }

    companion object {
        fun reviewState() = FoodPhotoUiState.Review(
            thumbnail = byteArrayOf(),
            meal = MealType.LUNCH,
            items = listOf(
                reviewItem(0, "Nasi putih", grams = 150, kcal = 195, EstimatedItem.Confidence.HIGH),
                reviewItem(1, "Ayam goreng", grams = 90, kcal = 260, EstimatedItem.Confidence.LOW),
            ),
            note = null,
        )

        private fun reviewItem(key: Int, name: String, grams: Int, kcal: Int, confidence: EstimatedItem.Confidence) = ReviewItem(
            key = key,
            name = name,
            originalName = name,
            nameId = name,
            nameEn = name,
            grams = grams.toString(),
            kcal = kcal.toString(),
            kcalPerGram = kcal.toDouble() / grams,
            proteinPerGram = null,
            carbsPerGram = null,
            fatPerGram = null,
            confidence = confidence,
        )
    }
}
