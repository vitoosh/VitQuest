package io.github.vitoosh.vitquest.core.data.repository

import io.github.vitoosh.vitquest.core.data.local.BudgetEntryEntity
import io.github.vitoosh.vitquest.core.data.local.ObtainOutcome
import io.github.vitoosh.vitquest.core.data.local.RewardDao
import io.github.vitoosh.vitquest.core.data.local.RewardEntity
import io.github.vitoosh.vitquest.core.data.local.toEntity
import io.github.vitoosh.vitquest.core.data.local.toLine
import io.github.vitoosh.vitquest.core.data.local.toModel
import io.github.vitoosh.vitquest.core.data.preferences.UserPreferencesDataSource
import io.github.vitoosh.vitquest.core.domain.habits.Achievements
import io.github.vitoosh.vitquest.core.domain.rewards.LedgerReconciler
import io.github.vitoosh.vitquest.core.domain.rewards.RewardRules
import io.github.vitoosh.vitquest.core.model.BudgetEntry
import io.github.vitoosh.vitquest.core.model.BudgetEntryKind
import io.github.vitoosh.vitquest.core.model.ObtainedReward
import io.github.vitoosh.vitquest.core.model.Reward
import io.github.vitoosh.vitquest.core.model.RewardTransaction
import io.github.vitoosh.vitquest.core.model.RewardType
import io.github.vitoosh.vitquest.core.time.TimeSource
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class RewardInput(
    val name: String,
    val description: String?,
    val type: RewardType,
    val priceRupiah: Long?,
)

data class BudgetEntryInput(
    val date: LocalDate,
    val kind: BudgetEntryKind,
    val amountRupiah: Long,
    val rewardId: Long?,
    val note: String?,
)

interface RewardRepository {
    val xp: Flow<Int>
    fun transactions(): Flow<List<RewardTransaction>>

    /** Brings the ledger up to date with the stored records. Safe to call any number of times. */
    suspend fun sync()

    fun rewards(): Flow<List<Reward>>
    suspend fun saveReward(id: Long?, input: RewardInput)
    suspend fun setWishlist(id: Long, wishlist: Boolean)
    suspend fun deleteReward(id: Long)

    /** Rewards the user actually got, newest first. */
    fun history(): Flow<List<ObtainedReward>>

    /**
     * Records that the user got [rewardId] in real life. [requestId] identifies one
     * confirmation, so repeating it never records (or spends) twice. A [pricePaidRupiah]
     * also adds a spending entry to the budget.
     */
    suspend fun markObtained(requestId: String, rewardId: Long, pricePaidRupiah: Long?): ObtainOutcome
    suspend fun undoObtained(id: Long): Boolean

    fun budgetEntries(): Flow<List<BudgetEntry>>
    suspend fun addBudgetEntry(input: BudgetEntryInput)
    suspend fun deleteBudgetEntry(id: Long)
    val monthlyBudget: Flow<Long?>
    suspend fun setMonthlyBudget(rupiah: Long?)
}

@Singleton
class OfflineRewardRepository @Inject constructor(
    private val rewardDao: RewardDao,
    private val habitRepository: HabitRepository,
    private val workoutRepository: WorkoutRepository,
    private val bodyRepository: BodyRepository,
    private val profileRepository: ProfileRepository,
    private val preferences: UserPreferencesDataSource,
    private val timeSource: TimeSource,
) : RewardRepository {

    private val syncMutex = Mutex()

    override val xp: Flow<Int> = rewardDao.observeXp()

    override fun transactions(): Flow<List<RewardTransaction>> =
        rewardDao.observeTransactions().map { rows -> rows.mapNotNull { it.toModel() } }

    override suspend fun sync() = syncMutex.withLock {
        val today = timeSource.today()
        val windowStart = RewardRules.windowStart(today)
        val history = habitRepository.dayRecords(HISTORY_START, today).first()
        val sessions = workoutRepository.sessions().first()
        val goals = profileRepository.goals.first()
        val customs = habitRepository.customHabits().first()
        val events = RewardRules.events(
            today = today,
            records = history.filterKeys { !it.isBefore(windowStart) },
            sessions = sessions,
            weights = bodyRepository.weights().first(),
            customHabits = customs,
            goals = goals,
            achievements = Achievements.evaluate(history.values, sessions, goals, customs),
        )
        val now = timeSource.now().toEpochMilli()
        rewardDao.appendPlanned { existing ->
            LedgerReconciler.reconcile(events, existing.mapNotNull { it.toLine() }, windowStart).map { it.toEntity(now) }
        }
        Unit
    }

    override fun rewards(): Flow<List<Reward>> = rewardDao.observeRewards().map { rows -> rows.map { it.toModel() } }

    override suspend fun saveReward(id: Long?, input: RewardInput) {
        val existing = id?.let { rewardDao.getReward(it) }
        val entity = RewardEntity(
            id = existing?.id ?: 0,
            seedKey = existing?.seedKey,
            name = input.name.trim(),
            description = input.description?.trim()?.takeIf { it.isNotEmpty() },
            type = input.type.name,
            priceRupiah = input.priceRupiah,
            wishlist = existing?.wishlist ?: false,
            createdAtEpochMs = existing?.createdAtEpochMs ?: timeSource.now().toEpochMilli(),
        )
        if (existing == null) rewardDao.insertReward(entity) else rewardDao.updateReward(entity)
    }

    override suspend fun setWishlist(id: Long, wishlist: Boolean) = rewardDao.setWishlist(id, wishlist)

    // History keeps its name snapshot; budget entries just lose the link.
    override suspend fun deleteReward(id: Long) = rewardDao.deleteReward(id)

    override fun history(): Flow<List<ObtainedReward>> = rewardDao.observeHistory().map { rows -> rows.map { it.toModel() } }

    override suspend fun markObtained(requestId: String, rewardId: Long, pricePaidRupiah: Long?): ObtainOutcome =
        rewardDao.markObtained(requestId, rewardId, pricePaidRupiah, timeSource.today().toString(), timeSource.now().toEpochMilli())

    override suspend fun undoObtained(id: Long): Boolean = rewardDao.undoObtained(id)

    override fun budgetEntries(): Flow<List<BudgetEntry>> = rewardDao.observeBudgetEntries().map { rows -> rows.map { it.toModel() } }

    override suspend fun addBudgetEntry(input: BudgetEntryInput) {
        rewardDao.insertBudgetEntry(
            BudgetEntryEntity(
                date = input.date.toString(),
                kind = input.kind.name,
                amountRupiah = input.amountRupiah,
                rewardId = input.rewardId,
                note = input.note?.trim()?.takeIf { it.isNotEmpty() },
                createdAtEpochMs = timeSource.now().toEpochMilli(),
            ),
        )
    }

    override suspend fun deleteBudgetEntry(id: Long) = rewardDao.deleteBudgetEntry(id)

    override val monthlyBudget: Flow<Long?> = preferences.monthlyRewardBudget

    override suspend fun setMonthlyBudget(rupiah: Long?) = preferences.setMonthlyRewardBudget(rupiah)

    private companion object {
        val HISTORY_START: LocalDate = LocalDate.of(2000, 1, 1)
    }
}
