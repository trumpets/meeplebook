package app.meeplebook.core.sync

import app.meeplebook.core.database.dao.SyncDao
import app.meeplebook.core.database.entity.SyncStateEntity
import app.meeplebook.core.sync.model.SyncState
import app.meeplebook.core.sync.model.SyncType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant

class SyncTimeRepositoryImplTest {

    private lateinit var fakeSyncDao: FakeSyncDao
    private lateinit var repository: SyncTimeRepositoryImpl

    @Before
    fun setUp() {
        fakeSyncDao = FakeSyncDao()
        repository = SyncTimeRepositoryImpl(fakeSyncDao)
    }

    @Test
    fun `getSyncState returns default state when no row exists`() = runTest {
        assertEquals(SyncState(), repository.getSyncState(SyncType.COLLECTION))
    }

    @Test
    fun `getSyncState maps persisted row`() = runTest {
        val completedAt = Instant.parse("2024-01-15T12:00:00Z")
        fakeSyncDao.stateByType[SyncType.PLAYS] = SyncStateEntity(
            type = SyncType.PLAYS,
            isSyncing = true,
            lastSyncedAt = completedAt,
            errorMessage = "NetworkError"
        )

        assertEquals(
            SyncState(
                isSyncing = true,
                lastSyncedAt = completedAt,
                errorMessage = "NetworkError"
            ),
            repository.getSyncState(SyncType.PLAYS)
        )
    }

    private class FakeSyncDao : SyncDao {
        val stateByType = mutableMapOf<SyncType, SyncStateEntity>()
        private val flows = SyncType.entries.associateWith { MutableStateFlow<SyncStateEntity?>(null) }

        override fun observeSyncState(type: SyncType): Flow<SyncStateEntity?> = flows.getValue(type)

        override suspend fun getSyncState(type: SyncType): SyncStateEntity? = stateByType[type]

        override suspend fun upsertSyncState(state: SyncStateEntity) {
            stateByType[state.type] = state
            flows.getValue(state.type).value = state
        }

        override suspend fun markStarted(type: SyncType) = Unit

        override suspend fun markCompleted(type: SyncType, time: Instant) = Unit

        override suspend fun markIdle(type: SyncType) = Unit

        override suspend fun markFailed(type: SyncType, errorMessage: String?) = Unit

        override suspend fun clear() {
            stateByType.clear()
            SyncType.entries.forEach { type ->
                flows.getValue(type).value = null
            }
        }
    }
}
