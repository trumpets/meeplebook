package app.meeplebook.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.meeplebook.core.timer.model.ActivePlayTimer
import java.time.Duration
import java.time.Instant

/**
 * Singleton Room row storing the persisted global play timer state.
 */
@Entity(tableName = "active_play_timer")
data class ActivePlayTimerEntity(
    @PrimaryKey val singletonId: Int = SINGLETON_ID,
    val playId: Long? = null,
    val startedAt: Instant? = null,
    val accumulatedMillis: Long = 0L,
    val isRunning: Boolean = false,
    val hasStarted: Boolean = false,
) {
    companion object {
        const val SINGLETON_ID: Int = 0
    }
}

fun ActivePlayTimerEntity.toModel(): ActivePlayTimer {
    return ActivePlayTimer(
        playId = playId,
        startedAt = startedAt,
        accumulated = Duration.ofMillis(accumulatedMillis),
        isRunning = isRunning,
        hasStarted = hasStarted,
    )
}

fun ActivePlayTimer.toEntity(): ActivePlayTimerEntity {
    return ActivePlayTimerEntity(
        playId = playId,
        startedAt = startedAt,
        accumulatedMillis = accumulated.toMillis(),
        isRunning = isRunning,
        hasStarted = hasStarted,
    )
}
