package io.github.vitoosh.vitquest.feature.nutrition.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vitoosh.vitquest.core.ai.AiConfig
import io.github.vitoosh.vitquest.core.ai.AiSessionStore
import io.github.vitoosh.vitquest.core.ai.FoodPhotoAnalyzer
import io.github.vitoosh.vitquest.core.ai.HttpClient
import io.github.vitoosh.vitquest.core.ai.HttpResponse
import io.github.vitoosh.vitquest.core.ai.TokenCipher
import io.github.vitoosh.vitquest.core.data.local.VitQuestDatabase
import io.github.vitoosh.vitquest.core.data.photos.FoodPhotoStore
import io.github.vitoosh.vitquest.core.data.repository.OfflineNutritionRepository
import io.github.vitoosh.vitquest.core.model.MealType
import io.github.vitoosh.vitquest.testing.FakeUserPreferencesRepository
import io.github.vitoosh.vitquest.testing.FixedTimeSource
import io.github.vitoosh.vitquest.testing.MainDispatcherRule
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Runs as API 27 because Robolectric cannot decode through ImageDecoder from a ContentResolver;
 * that path (API 28+) is covered by ImagePrepInstrumentedTest on a device.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [27])
class FoodPhotoViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class PlainCipher : TokenCipher {
        override fun encrypt(plain: ByteArray) = plain
        override fun decrypt(data: ByteArray) = data
    }

    private class FakeHttp : HttpClient {
        val replies = ArrayDeque<HttpResponse>()
        var offline = false
        override suspend fun postJson(url: String, headers: Map<String, String>, body: String): HttpResponse {
            if (offline) throw IOException("offline")
            return replies.removeFirst()
        }
    }

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val time = FixedTimeSource(Instant.parse("2026-10-21T05:00:00Z"))
    private val day = LocalDate.parse("2026-10-21")
    private val http = FakeHttp()
    private val photos = FoodPhotoStore(context)
    private lateinit var database: VitQuestDatabase
    private lateinit var nutrition: OfflineNutritionRepository

    private val session = HttpResponse(200, """{"access_token":"a","refresh_token":"r","expires_in":3600}""")
    private val estimate = HttpResponse(
        200,
        """{"is_food":true,"note":null,"items":[
            {"name_id":"Nasi putih","name_en":"Steamed rice","grams":150,"kcal":195,"protein_g":3,"carbs_g":42,"fat_g":0.3,"confidence":"high"},
            {"name_id":"Ayam goreng","name_en":"Fried chicken","grams":100,"kcal":260,"confidence":"low"}]}""",
    )

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, VitQuestDatabase::class.java).allowMainThreadQueries().build()
        photos.directory.deleteRecursively()
        nutrition = OfflineNutritionRepository(database.foodDao(), photos, time)
    }

    @After
    fun tearDown() {
        database.close()
        photos.directory.deleteRecursively()
    }

    private fun viewModel(config: AiConfig = AiConfig("https://proj.supabase.co", "sb_publishable_test")) = FoodPhotoViewModel(
        SavedStateHandle(mapOf("date" to day.toString(), "meal" to MealType.LUNCH.name)),
        context,
        config,
        FoodPhotoAnalyzer(config, http, AiSessionStore(context, PlainCipher()).apply { clear() }, time),
        nutrition,
        FakeUserPreferencesRepository(),
    )

    private fun photo(): Uri {
        val file = File(context.cacheDir, "meal.jpg")
        val bitmap = Bitmap.createBitmap(2000, 1500, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(200, 150, 90)) }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        return Uri.fromFile(file)
    }

    private suspend fun FoodPhotoViewModel.settled(): FoodPhotoUiState =
        state.first { it !is FoodPhotoUiState.Analyzing && it !is FoodPhotoUiState.Idle }

    @Test
    fun aPhotoBecomesCheckedDiaryEntriesWithASmallCopy() = runTest {
        http.replies += listOf(session, estimate)
        val vm = viewModel()

        vm.onImage(photo())
        val review = vm.settled() as FoodPhotoUiState.Review
        // Shown in the app language (English here); both names are kept.
        assertEquals(listOf("Steamed rice", "Fried chicken"), review.items.map { it.name })
        assertEquals(listOf("Nasi putih", "Ayam goreng"), review.items.map { it.nameId })
        assertEquals(455, review.totalKcal)

        // Halving the rice halves its energy; the chicken is renamed and moved to dinner.
        vm.setGrams(0, "75")
        vm.setName(1, "Ayam bakar")
        vm.setMeal(MealType.DINNER)
        vm.save()
        vm.saved.first { it }

        val entries = nutrition.diary(day).first()
        assertEquals(listOf("Nasi putih", "Ayam bakar"), entries.map { it.nameId })
        assertEquals(listOf("Steamed rice", "Ayam bakar"), entries.map { it.nameEn })
        assertEquals(97.5, entries[0].nutrients.kcal, 0.5)
        assertEquals(1.5, entries[0].nutrients.proteinG!!, 0.01)
        assertTrue(entries.all { it.meal == MealType.DINNER })
        // The kept copy is small: at most 320 px on the longest side.
        val thumbnail = BitmapFactory.decodeFile(photos.file(entries[0].photoId!!).path)
        assertEquals(320 to 240, thumbnail.width to thumbnail.height)
    }

    @Test
    fun theFullCameraPhotoIsDeletedOnceTheSmallCopiesAreMade() = runTest {
        http.replies += listOf(session, estimate)
        val vm = viewModel()
        val capture = File(context.cacheDir, "camera/capture.jpg")
        assertTrue(vm.captureUri.toString().startsWith("content://${context.packageName}.camera/"))
        File(photo().path!!).renameTo(capture)

        vm.onCaptured()
        assertTrue(vm.settled() is FoodPhotoUiState.Review)
        assertFalse(capture.exists())
    }

    @Test
    fun invalidValuesAreMarkedAndNothingIsSaved() = runTest {
        http.replies += listOf(session, estimate)
        val vm = viewModel()
        vm.onImage(photo())
        vm.settled()

        vm.setGrams(1, "0")
        vm.setName(0, "  ")
        vm.save()

        val review = vm.state.value as FoodPhotoUiState.Review
        assertTrue(review.items.all { it.error })
        assertFalse(vm.saved.value)
        assertTrue(nutrition.diary(day).first().isEmpty())
    }

    @Test
    fun failuresAreExplained() = runTest {
        http.offline = true
        val vm = viewModel()
        vm.onImage(photo())
        assertEquals(FoodPhotoUiState.Failed(PhotoError.OFFLINE, canRetry = true), vm.settled())

        vm.onImage(Uri.fromFile(File(context.cacheDir, "missing.jpg")))
        assertEquals(FoodPhotoUiState.Failed(PhotoError.UNREADABLE_IMAGE, canRetry = false), vm.settled())

        assertEquals(FoodPhotoUiState.NotConfigured, viewModel(AiConfig("", "")).state.value)
    }
}
