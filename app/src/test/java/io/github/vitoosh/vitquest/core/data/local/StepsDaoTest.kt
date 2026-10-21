package io.github.vitoosh.vitquest.core.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vitoosh.vitquest.core.domain.steps.SensorReading
import io.github.vitoosh.vitquest.core.model.StepSource
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StepsDaoTest {

    private lateinit var database: VitQuestDatabase
    private lateinit var dao: StepsDao
    private val zone = ZoneOffset.ofHours(7)

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), VitQuestDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.stepsDao()
    }

    @After
    fun tearDown() = database.close()

    private fun reading(counter: Long, isoUtc: String) =
        SensorReading(bootCount = 3, counter = counter, readAt = Instant.parse(isoUtc), bootedAt = Instant.parse("2026-10-18T00:00:00Z"))

    @Test
    fun sensorReadingsAccumulateIntoTheDay() = runTest {
        // 01:00, 03:00 and 05:00 UTC are 08:00, 10:00 and 12:00 in UTC+7, all on 19 October.
        dao.applySensorReading(reading(10_000, "2026-10-19T01:00:00Z"), zone, Instant.parse("2026-10-19T01:00:00Z"))
        dao.applySensorReading(reading(11_200, "2026-10-19T03:00:00Z"), zone, Instant.parse("2026-10-19T03:00:00Z"))
        dao.applySensorReading(reading(12_000, "2026-10-19T05:00:00Z"), zone, Instant.parse("2026-10-19T05:00:00Z"))

        assertEquals(2_000, dao.getSteps("2026-10-19", StepSource.PHONE_SENSOR.name))
        assertEquals(12_000L, dao.getSensorState()?.counter)
    }

    @Test
    fun healthConnectRefreshReplacesStaleDays() = runTest {
        val from = LocalDate.parse("2026-10-18")
        val to = LocalDate.parse("2026-10-19")
        dao.replaceHealthConnectDays(
            from,
            to,
            listOf(
                DailyStepsEntity("2026-10-18", StepSource.HEALTH_CONNECT.name, 5_000, 1L),
                DailyStepsEntity("2026-10-19", StepSource.HEALTH_CONNECT.name, 1_000, 1L),
            ),
        )
        // The next sync no longer reports 18 October (e.g. data deleted in Health Connect).
        dao.replaceHealthConnectDays(from, to, listOf(DailyStepsEntity("2026-10-19", StepSource.HEALTH_CONNECT.name, 2_500, 2L)))

        assertNull(dao.getSteps("2026-10-18", StepSource.HEALTH_CONNECT.name))
        assertEquals(2_500, dao.getSteps("2026-10-19", StepSource.HEALTH_CONNECT.name))
    }

    @Test
    fun healthConnectRefreshKeepsOtherSourcesAndDaysOutsideRange() = runTest {
        dao.upsert(
            listOf(
                DailyStepsEntity("2026-10-19", StepSource.MANUAL.name, 9_999, 1L),
                DailyStepsEntity("2026-09-01", StepSource.HEALTH_CONNECT.name, 4_000, 1L),
            ),
        )
        dao.replaceHealthConnectDays(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-19"), emptyList())

        val rows = dao.observeRange("2026-09-01", "2026-10-19").first()
        assertEquals(2, rows.size)
    }
}
