package app.meeplebook.core.timer.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.annotation.VisibleForTesting
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import app.meeplebook.core.timer.domain.ObserveActivePlayTimerUseCase
import app.meeplebook.core.timer.model.ActivePlayTimer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service that keeps the play timer notification up to date.
 */
@AndroidEntryPoint
class PlayTimerForegroundService : Service() {
    @Inject
    lateinit var observeActivePlayTimer: ObserveActivePlayTimerUseCase

    @Inject
    lateinit var notificationBuilder: PlayTimerNotificationBuilder

    @Inject
    lateinit var notificationManager: NotificationManager

    @VisibleForTesting
    internal var serviceScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        ServiceCompat.startForeground(
            this,
            PlayTimerNotificationBuilder.NOTIFICATION_ID,
            notificationBuilder.buildPlaceholder(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )

        serviceScope.launch {
            observeActivePlayTimer().collect { timer ->
                if (!timer.shouldShowPersistentNotification()) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return@collect
                }

                if (canPostNotifications()) {
                    updateNotification(timer)
                }
            }
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    @SuppressLint("NotificationPermission")
    private fun updateNotification(timer: ActivePlayTimer) {
        notificationManager.notify(
            PlayTimerNotificationBuilder.NOTIFICATION_ID,
            notificationBuilder.build(timer),
        )
    }

    private fun canPostNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
    }
}
