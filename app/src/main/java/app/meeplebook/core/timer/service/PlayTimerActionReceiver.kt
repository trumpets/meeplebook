package app.meeplebook.core.timer.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.meeplebook.core.di.ApplicationScope
import app.meeplebook.core.timer.domain.PausePlayTimerUseCase
import app.meeplebook.core.timer.domain.ResetPlayTimerUseCase
import app.meeplebook.core.timer.domain.ResumePlayTimerUseCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Handles timer notification actions.
 */
@AndroidEntryPoint
class PlayTimerActionReceiver : BroadcastReceiver() {
    @Inject
    lateinit var pausePlayTimer: PausePlayTimerUseCase

    @Inject
    lateinit var resumePlayTimer: ResumePlayTimerUseCase

    @Inject
    lateinit var resetPlayTimer: ResetPlayTimerUseCase

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val pendingResult = goAsync()
        applicationScope.launch {
            try {
                when (intent.action) {
                    ACTION_PAUSE -> pausePlayTimer()
                    ACTION_RESUME -> resumePlayTimer()
                    ACTION_RESET -> resetPlayTimer()
                    else -> Unit
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_PAUSE: String = "app.meeplebook.action.PLAY_TIMER_PAUSE"
        const val ACTION_RESUME: String = "app.meeplebook.action.PLAY_TIMER_RESUME"
        const val ACTION_RESET: String = "app.meeplebook.action.PLAY_TIMER_RESET"
    }
}
