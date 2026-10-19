package io.github.vitoosh.vitquest.core.data.health

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.Period
import javax.inject.Inject
import javax.inject.Singleton

enum class HealthConnectAvailability {
    AVAILABLE,

    /** Health Connect app must be installed or updated (Android 9–13). */
    UPDATE_REQUIRED,

    /** Not supported on this device (for example Android 8). */
    NOT_SUPPORTED,
}

sealed interface HealthConnectReadResult {
    data class Success(val stepsByDate: Map<LocalDate, Long>) : HealthConnectReadResult
    data object PermissionDenied : HealthConnectReadResult
    data object Failed : HealthConnectReadResult
}

/** Read-only access to daily step totals in Health Connect. */
interface HealthConnectStepsSource {
    val stepsPermissions: Set<String>

    fun availability(): HealthConnectAvailability

    suspend fun hasStepsPermission(): Boolean

    /** Daily totals for [from]..[to] (inclusive, local days). Days without records are absent. */
    suspend fun readDailySteps(from: LocalDate, to: LocalDate): HealthConnectReadResult
}

@Singleton
class AndroidHealthConnectStepsSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : HealthConnectStepsSource {

    override val stepsPermissions: Set<String> = STEPS_PERMISSIONS

    override fun availability(): HealthConnectAvailability = when (HealthConnectClient.getSdkStatus(context)) {
        HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.AVAILABLE
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectAvailability.UPDATE_REQUIRED
        else -> HealthConnectAvailability.NOT_SUPPORTED
    }

    override suspend fun hasStepsPermission(): Boolean {
        val client = currentClient() ?: return false
        return runCatching { client.permissionController.getGrantedPermissions().containsAll(stepsPermissions) }
            .getOrDefault(false)
    }

    override suspend fun readDailySteps(from: LocalDate, to: LocalDate): HealthConnectReadResult {
        val client = currentClient() ?: return HealthConnectReadResult.Failed
        return try {
            val groups = client.aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    // Local date-times follow the device time zone, so each slice is one local day.
                    timeRangeFilter = TimeRangeFilter.between(from.atStartOfDay(), to.plusDays(1).atStartOfDay()),
                    timeRangeSlicer = Period.ofDays(1),
                ),
            )
            HealthConnectReadResult.Success(
                groups.mapNotNull { group ->
                    group.result[StepsRecord.COUNT_TOTAL]?.let { group.startTime.toLocalDate() to it }
                }.toMap(),
            )
        } catch (e: SecurityException) {
            HealthConnectReadResult.PermissionDenied
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            // RemoteException, IOException or IllegalStateException (e.g. rate limit) from the provider.
            HealthConnectReadResult.Failed
        }
    }

    // Re-checks availability each time so installing Health Connect while the app runs is picked up.
    private fun currentClient(): HealthConnectClient? =
        if (availability() == HealthConnectAvailability.AVAILABLE) HealthConnectClient.getOrCreate(context) else null

    companion object {
        private const val PROVIDER_PACKAGE = "com.google.android.apps.healthdata"

        /** Only step totals are read. VitQuest never writes to Health Connect. */
        val STEPS_PERMISSIONS: Set<String> = setOf(HealthPermission.getReadPermission(StepsRecord::class))

        fun permissionContract() = PermissionController.createRequestPermissionResultContract()

        /** Opens the Health Connect settings screen where users manage permissions and data. */
        fun settingsIntent(): Intent = Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)

        /** Play Store page that installs or updates Health Connect on Android 9–13. */
        fun installIntent(): Intent = Intent(
            Intent.ACTION_VIEW,
            "market://details?id=$PROVIDER_PACKAGE&url=healthconnect%3A%2F%2Fonboarding".toUri(),
        ).setPackage("com.android.vending")

        /** Browser fallback when the Play Store app is missing. */
        fun installWebIntent(): Intent = Intent(
            Intent.ACTION_VIEW,
            "https://play.google.com/store/apps/details?id=$PROVIDER_PACKAGE".toUri(),
        )
    }
}
