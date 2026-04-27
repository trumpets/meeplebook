package app.meeplebook.core.timer.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.meeplebook.core.di.ApplicationScope
import app.meeplebook.core.timer.domain.GetActivePlayTimerUseCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Restores the timer foreground service after device reboot.
 */
@AndroidEntryPoint
class PlayTimerBootReceiver : BroadcastReceiver() {
    @Inject
    lateinit var getActivePlayTimer: GetActivePlayTimerUseCase

    @Inject
    lateinit var serviceController: PlayTimerServiceController

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        val pendingResult = goAsync()
        applicationScope.launch {
            if (getActivePlayTimer().shouldShowPersistentNotification()) {
                serviceController.ensureServiceRunning()
            }
            pendingResult.finish()
        }
    }
}
