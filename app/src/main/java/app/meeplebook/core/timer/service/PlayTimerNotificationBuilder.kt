package app.meeplebook.core.timer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import app.meeplebook.MainActivity
import app.meeplebook.R
import app.meeplebook.core.timer.model.ActivePlayTimer
import app.meeplebook.core.timer.model.computeElapsed
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

/**
 * Builds the persistent notification for the global play timer.
 */
class PlayTimerNotificationBuilder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: Clock,
) {
    @Inject
    lateinit var notificationManager: NotificationManager

    fun buildPlaceholder(): Notification {
        ensureChannel()
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_play_timer_notification)
            .setContentTitle(context.getString(R.string.play_timer_notification_title_running))
            .setContentText(formatPlayTimerElapsed(Duration.ZERO))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppPendingIntent())
            .build()
    }

    fun build(
        timer: ActivePlayTimer,
        now: Instant = clock.instant(),
    ): Notification {
        ensureChannel()

        val elapsed = computeElapsed(timer, now)
        val titleRes =
            if (timer.isRunning) {
                R.string.play_timer_notification_title_running
            } else {
                R.string.play_timer_notification_title_paused
            }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_play_timer_notification)
            .setContentTitle(context.getString(titleRes))
            .setContentText(formatPlayTimerElapsed(elapsed))
            .setContentIntent(openAppPendingIntent())
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setOngoing(true)
            .addAction(
                if (timer.isRunning) {
                    buildPauseAction()
                } else {
                    buildResumeAction()
                },
            )
            .addAction(buildResetAction())
            .build()
    }

    private fun buildPauseAction(): NotificationCompat.Action {
        return NotificationCompat.Action.Builder(
            0,
            context.getString(R.string.play_timer_action_pause),
            actionPendingIntent(PlayTimerActionReceiver.ACTION_PAUSE),
        ).build()
    }

    private fun buildResumeAction(): NotificationCompat.Action {
        return NotificationCompat.Action.Builder(
            0,
            context.getString(R.string.play_timer_action_resume),
            actionPendingIntent(PlayTimerActionReceiver.ACTION_RESUME),
        ).build()
    }

    private fun buildResetAction(): NotificationCompat.Action {
        return NotificationCompat.Action.Builder(
            0,
            context.getString(R.string.play_timer_action_reset),
            actionPendingIntent(PlayTimerActionReceiver.ACTION_RESET),
        ).build()
    }

    private fun actionPendingIntent(action: String): PendingIntent {
        val intent = Intent(context, PlayTimerActionReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun openAppPendingIntent(): PendingIntent {
        val intent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        return PendingIntent.getActivity(
            context,
            OPEN_APP_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun ensureChannel() {
        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.play_timer_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description =
                    context.getString(R.string.play_timer_notification_channel_description)
            },
        )
    }

    companion object {
        const val CHANNEL_ID: String = "play_timer"
        const val NOTIFICATION_ID: Int = 7101
        private const val OPEN_APP_REQUEST_CODE: Int = 7102
    }
}
