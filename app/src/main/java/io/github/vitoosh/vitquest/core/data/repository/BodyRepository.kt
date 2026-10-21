package io.github.vitoosh.vitquest.core.data.repository

import io.github.vitoosh.vitquest.core.data.local.BodyDao
import io.github.vitoosh.vitquest.core.data.local.BodyMeasurementEntity
import io.github.vitoosh.vitquest.core.data.local.ProfileDao
import io.github.vitoosh.vitquest.core.data.local.WaterLogEntity
import io.github.vitoosh.vitquest.core.data.local.WeightEntryEntity
import io.github.vitoosh.vitquest.core.data.local.toModel
import io.github.vitoosh.vitquest.core.model.BodyMeasurement
import io.github.vitoosh.vitquest.core.model.WaterLog
import io.github.vitoosh.vitquest.core.model.WeightEntry
import io.github.vitoosh.vitquest.core.time.TimeSource
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class MeasurementInput(
    val waistCm: Double?,
    val hipCm: Double?,
    val chestCm: Double?,
    val armCm: Double?,
    val thighCm: Double?,
)

interface BodyRepository {
    fun weights(): Flow<List<WeightEntry>>
    /** Saves the weight for a day (replacing that day's value) and keeps the profile's current weight in sync. */
    suspend fun logWeight(date: LocalDate, weightKg: Double, note: String?)
    suspend fun deleteWeight(id: Long)

    fun measurements(): Flow<List<BodyMeasurement>>
    suspend fun saveMeasurement(date: LocalDate, input: MeasurementInput)
    suspend fun deleteMeasurement(id: Long)

    fun water(from: LocalDate, to: LocalDate): Flow<List<WaterLog>>
    suspend fun addWater(date: LocalDate, amountMl: Int)
    suspend fun deleteWater(id: Long)
}

@Singleton
class OfflineBodyRepository @Inject constructor(
    private val bodyDao: BodyDao,
    private val profileDao: ProfileDao,
    private val timeSource: TimeSource,
) : BodyRepository {

    override fun weights(): Flow<List<WeightEntry>> = bodyDao.observeWeights().map { rows -> rows.map { it.toModel() } }

    override suspend fun logWeight(date: LocalDate, weightKg: Double, note: String?) {
        val now = timeSource.now().toEpochMilli()
        val existing = bodyDao.getWeightOn(date.toString())
        bodyDao.upsertWeight(
            WeightEntryEntity(
                id = existing?.id ?: 0,
                date = date.toString(),
                weightKg = weightKg,
                note = note?.trim()?.takeIf { it.isNotEmpty() },
                updatedAtEpochMs = now,
            ),
        )
        syncProfileWeight()
    }

    override suspend fun deleteWeight(id: Long) {
        bodyDao.deleteWeight(id)
        syncProfileWeight()
    }

    /** The profile weight (used for calorie estimates) always follows the most recent entry. */
    private suspend fun syncProfileWeight() {
        val latest = bodyDao.getLatestWeight() ?: return
        profileDao.updateCurrentWeight(latest.weightKg, timeSource.now().toEpochMilli())
    }

    override fun measurements(): Flow<List<BodyMeasurement>> =
        bodyDao.observeMeasurements().map { rows -> rows.map { it.toModel() } }

    override suspend fun saveMeasurement(date: LocalDate, input: MeasurementInput) {
        val existing = bodyDao.getMeasurementOn(date.toString())
        bodyDao.upsertMeasurement(
            BodyMeasurementEntity(
                id = existing?.id ?: 0,
                date = date.toString(),
                waistCm = input.waistCm,
                hipCm = input.hipCm,
                chestCm = input.chestCm,
                armCm = input.armCm,
                thighCm = input.thighCm,
                updatedAtEpochMs = timeSource.now().toEpochMilli(),
            ),
        )
    }

    override suspend fun deleteMeasurement(id: Long) = bodyDao.deleteMeasurement(id)

    override fun water(from: LocalDate, to: LocalDate): Flow<List<WaterLog>> =
        bodyDao.observeWater(from.toString(), to.toString()).map { rows -> rows.map { it.toModel() } }

    override suspend fun addWater(date: LocalDate, amountMl: Int) {
        bodyDao.insertWater(WaterLogEntity(date = date.toString(), amountMl = amountMl, loggedAtEpochMs = timeSource.now().toEpochMilli()))
    }

    override suspend fun deleteWater(id: Long) = bodyDao.deleteWater(id)
}
