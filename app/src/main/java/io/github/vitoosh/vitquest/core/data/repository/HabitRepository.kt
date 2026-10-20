package io.github.vitoosh.vitquest.core.data.repository

import io.github.vitoosh.vitquest.core.data.local.BodyDao
import io.github.vitoosh.vitquest.core.data.local.FoodDao
import io.github.vitoosh.vitquest.core.data.local.HabitCheckInEntity
import io.github.vitoosh.vitquest.core.data.local.HabitDao
import io.github.vitoosh.vitquest.core.data.local.HabitEntity
import io.github.vitoosh.vitquest.core.data.local.WorkoutDao
import io.github.vitoosh.vitquest.core.data.local.toModel
import io.github.vitoosh.vitquest.core.domain.habits.DayRecord
import io.github.vitoosh.vitquest.core.model.CustomHabit
import io.github.vitoosh.vitquest.core.time.TimeSource
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

interface HabitRepository {
    fun customHabits(): Flow<List<CustomHabit>>
    suspend fun addHabit(name: String, targetDaysPerWeek: Int): Long
    suspend fun updateHabit(id: Long, name: String, targetDaysPerWeek: Int)
    suspend fun deleteHabit(id: Long)
    suspend fun setCheckIn(habitId: Long, date: LocalDate, done: Boolean)

    /** One record per day that has any data, gathered from steps, water, food and workouts. */
    fun dayRecords(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, DayRecord>>
}

@Singleton
class OfflineHabitRepository @Inject constructor(
    private val habitDao: HabitDao,
    private val bodyDao: BodyDao,
    private val foodDao: FoodDao,
    private val workoutDao: WorkoutDao,
    private val stepsRepository: StepsRepository,
    private val timeSource: TimeSource,
) : HabitRepository {

    override fun customHabits(): Flow<List<CustomHabit>> = habitDao.observeHabits().map { rows -> rows.map { it.toModel() } }

    override suspend fun addHabit(name: String, targetDaysPerWeek: Int): Long =
        habitDao.insertHabit(HabitEntity(name = name.trim(), targetDaysPerWeek = targetDaysPerWeek, createdAtEpochMs = timeSource.now().toEpochMilli()))

    override suspend fun updateHabit(id: Long, name: String, targetDaysPerWeek: Int) =
        habitDao.updateHabit(id, name.trim(), targetDaysPerWeek)

    override suspend fun deleteHabit(id: Long) = habitDao.deleteHabit(id)

    override suspend fun setCheckIn(habitId: Long, date: LocalDate, done: Boolean) {
        if (done) habitDao.insertCheckIn(HabitCheckInEntity(habitId, date.toString())) else habitDao.deleteCheckIn(habitId, date.toString())
    }

    override fun dayRecords(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, DayRecord>> {
        val start = from.toString()
        val end = to.toString()
        return combine(
            stepsRepository.observeDailySteps(from, to),
            bodyDao.observeWaterTotals(start, end),
            foodDao.observeLoggedDates(start, end),
            workoutDao.observeSessions(start, end),
        ) { steps, water, foodDates, sessions ->
            val waterByDate = water.associate { LocalDate.parse(it.date) to it.total }
            val foodSet = foodDates.map(LocalDate::parse).toSet()
            val sessionsByDate = sessions.groupBy { LocalDate.parse(it.session.date) }
            val dates = steps.keys + waterByDate.keys + foodSet + sessionsByDate.keys
            dates.associateWith { date ->
                val daySessions = sessionsByDate[date].orEmpty()
                DayRecord(
                    date = date,
                    steps = steps[date]?.steps,
                    waterMl = waterByDate[date] ?: 0,
                    foodLogged = date in foodSet,
                    workoutCount = daySessions.size,
                    workoutSeconds = daySessions.sumOf { it.session.activeSeconds },
                )
            }
        }
    }
}
