package app.meeplebook.widget

import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import app.meeplebook.MainActivity

/**
 * Glance [ActionCallback] that opens [MainActivity] to the play details screen for
 * the currently active play.  Fired when the user taps the body of the widget while
 * a play is in progress.
 *
 * TODO: Navigate to a dedicated play-details screen once that feature is implemented.
 */
internal class OpenPlayAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val playId = parameters[QuickLogWidgetKeys.playIdActionParam]
        val intent = Intent(context, MainActivity::class.java).apply {
            action = MainActivity.ACTION_VIEW_PLAY
            playId?.let { putExtra(MainActivity.EXTRA_PLAY_ID, it) }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
    }
}
