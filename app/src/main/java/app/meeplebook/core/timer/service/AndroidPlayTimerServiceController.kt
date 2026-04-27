package app.meeplebook.core.timer.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidPlayTimerServiceController @Inject constructor(
    @ApplicationContext private val appContext: Context,
) : PlayTimerServiceController {
    override fun ensureServiceRunning() {
        ContextCompat.startForegroundService(
            appContext,
            Intent(appContext, PlayTimerForegroundService::class.java),
        )
    }

    override fun stopService() {
        appContext.stopService(Intent(appContext, PlayTimerForegroundService::class.java))
    }
}