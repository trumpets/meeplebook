package app.meeplebook.core.timer

import app.meeplebook.core.database.dao.PlayTimerDao
import app.meeplebook.core.database.entity.toEntity
import app.meeplebook.core.database.entity.toModel
import app.meeplebook.core.timer.domain.PlayTimerStateMachine
import app.meeplebook.core.timer.model.ActivePlayTimer
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Room-backed implementation of [TimerRepository].
 */
class TimerRepositoryImpl @Inject constructor(
    private val playTimerDao: PlayTimerDao,
    private val clock: Clock,
) : TimerRepository {

    private val mutationMutex = Mutex()

    override fun observe(): Flow<ActivePlayTimer> =
        playTimerDao.observeTimer().map { entity ->
            entity?.toModel() ?: ActivePlayTimer()
        }

    override suspend fun get(): ActivePlayTimer {
        return playTimerDao.getTimer()?.toModel() ?: ActivePlayTimer()
    }

    override suspend fun start(playId: Long?) {
        mutationMutex.withLock {
            playTimerDao.upsertTimer(
                PlayTimerStateMachine.start(
                    playId = playId,
                    now = clock.instant(),
                ).toEntity()
            )
        }
    }

    override suspend fun pause() {
        mutate { timer ->
            PlayTimerStateMachine.pause(timer, now = clock.instant())
        }
    }

    override suspend fun resume() {
        mutate { timer ->
            PlayTimerStateMachine.resume(timer, now = clock.instant())
        }
    }

    override suspend fun reset() {
        mutate { timer ->
            PlayTimerStateMachine.reset(timer, now = clock.instant())
        }
    }

    private suspend fun mutate(
        transform: (ActivePlayTimer) -> ActivePlayTimer,
    ) {
        mutationMutex.withLock {
            val current = get()
            val updated = transform(current)
            if (updated == current) {
                return
            }

            playTimerDao.upsertTimer(updated.toEntity())
        }
    }
}
