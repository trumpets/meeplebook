package app.meeplebook.core.timer.service

import app.meeplebook.core.di.ApplicationScope
import app.meeplebook.core.timer.domain.ObserveActivePlayTimerUseCase
import app.meeplebook.core.timer.model.ActivePlayTimer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the foreground timer service aligned with persisted timer state.
 */
@Singleton
class PlayTimerServiceCoordinator @Inject constructor(
    observeActivePlayTimer: ObserveActivePlayTimerUseCase,
    private val serviceController: PlayTimerServiceController,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {
    init {
        applicationScope.launch {
            observeActivePlayTimer().collect { timer ->
                if (timer.shouldShowPersistentNotification()) {
                    serviceController.ensureServiceRunning()
                } else {
                    serviceController.stopService()
                }
            }
        }
    }
}

internal fun ActivePlayTimer.shouldShowPersistentNotification(): Boolean = hasStarted
