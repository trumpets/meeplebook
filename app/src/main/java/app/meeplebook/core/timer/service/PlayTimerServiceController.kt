package app.meeplebook.core.timer.service

/**
 * Starts and stops the foreground service that owns the timer notification.
 */
interface PlayTimerServiceController {
    fun ensureServiceRunning()

    fun stopService()
}
