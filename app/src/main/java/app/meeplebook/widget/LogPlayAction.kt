package app.meeplebook.widget

import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import app.meeplebook.MainActivity

/**
 * Glance [ActionCallback] that launches [MainActivity] with [MainActivity.ACTION_ADD_PLAY]
 * so the user can log a new play.  Fired when the widget is tapped while no play is active.
 */
internal class LogPlayAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = MainActivity.ACTION_ADD_PLAY
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
    }
}
