package app.meeplebook.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.wrapContentHeight
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import app.meeplebook.R

/**
 * Home-screen widget that shows a quick "Log Play" shortcut when no play is in progress,
 * or a live timer with pause/resume controls when a play is active.
 *
 * ## State management
 * Widget state is stored in [PreferencesGlanceStateDefinition] using [QuickLogWidgetKeys].
 * Call [updateActivePlay] from the app to transition the widget into active-play mode.
 * Call [clearActivePlay] to return it to the idle state.
 *
 * ## Timer updates
 * When a timer is running, schedule a periodic WorkManager task that calls
 * [updateActivePlay] every 30 s to keep [QuickLogWidgetKeys.elapsedSeconds] current.
 * (Timer feature is not yet implemented — stubbed for future work.)
 */
class QuickLogWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<Preferences> =
        PreferencesGlanceStateDefinition

    /**
     * Widget supports two responsive breakpoints:
     *  - compact (≥ 2 × 1 cells): icon + label stacked
     *  - expanded (≥ 2 × 2 cells): full active-play card
     */
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(110.dp, 50.dp),
            DpSize(110.dp, 110.dp),
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val hasActivePlay = prefs[QuickLogWidgetKeys.hasActivePlay] ?: false
            GlanceTheme {
                if (hasActivePlay) {
                    ActivePlayContent(prefs)
                } else {
                    NoActivePlayContent()
                }
            }
        }
    }

    companion object {
        /**
         * Updates every instance of this widget with the given active-play state and
         * requests a re-render. Call this from the app when a play session starts or
         * the timer tick fires.
         *
         * TODO: Wire up to timer feature once implemented.
         */
        suspend fun updateActivePlay(
            context: Context,
            playId: Long,
            gameName: String,
            elapsedSeconds: Long,
            isTimerRunning: Boolean,
        ) {
            val manager = GlanceAppWidgetManager(context)
            manager.getGlanceIds(QuickLogWidget::class.java).forEach { id ->
                updateAppWidgetState(context, id) { prefs ->
                    prefs[QuickLogWidgetKeys.hasActivePlay] = true
                    prefs[QuickLogWidgetKeys.playId] = playId
                    prefs[QuickLogWidgetKeys.gameName] = gameName
                    prefs[QuickLogWidgetKeys.elapsedSeconds] = elapsedSeconds
                    prefs[QuickLogWidgetKeys.isTimerRunning] = isTimerRunning
                }
                QuickLogWidget().update(context, id)
            }
        }

        /**
         * Resets every instance of this widget back to idle (no active play) state.
         * Call this from the app when a play session ends.
         *
         * TODO: Wire up to play-completion flow once implemented.
         */
        suspend fun clearActivePlay(context: Context) {
            val manager = GlanceAppWidgetManager(context)
            manager.getGlanceIds(QuickLogWidget::class.java).forEach { id ->
                updateAppWidgetState(context, id) { prefs ->
                    prefs[QuickLogWidgetKeys.hasActivePlay] = false
                }
                QuickLogWidget().update(context, id)
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Composable content
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun NoActivePlayContent() {
    val context = LocalContext.current
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .clickable(actionRunCallback<LogPlayAction>()),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = GlanceModifier.padding(12.dp),
        ) {
            Image(
                provider = ImageProvider(R.mipmap.ic_launcher),
                contentDescription = context.getString(R.string.app_name),
                modifier = GlanceModifier.size(40.dp),
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            Text(
                text = context.getString(R.string.widget_log_play),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                ),
            )
        }
    }
}

@Composable
private fun ActivePlayContent(prefs: Preferences) {
    val context = LocalContext.current
    val playId = prefs[QuickLogWidgetKeys.playId] ?: 0L
    val gameName = prefs[QuickLogWidgetKeys.gameName] ?: ""
    val elapsedSeconds = prefs[QuickLogWidgetKeys.elapsedSeconds] ?: 0L
    val isTimerRunning = prefs[QuickLogWidgetKeys.isTimerRunning] ?: false

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .clickable(
                actionRunCallback<OpenPlayAction>(
                    parameters = actionParametersOf(
                        QuickLogWidgetKeys.playIdActionParam to playId,
                    )
                )
            )
            .padding(12.dp),
    ) {
        Text(
            text = gameName,
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            ),
            maxLines = 1,
            modifier = GlanceModifier.fillMaxWidth(),
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = GlanceModifier.fillMaxWidth().wrapContentHeight(),
        ) {
            Text(
                text = formatElapsedTime(elapsedSeconds),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 14.sp,
                ),
                modifier = GlanceModifier.defaultWeight(),
            )
            // Pause/Resume button — tap event is isolated from the body's OpenPlayAction
            androidx.glance.Button(
                text = if (isTimerRunning) {
                    context.getString(R.string.widget_timer_pause)
                } else {
                    context.getString(R.string.widget_timer_resume)
                },
                onClick = actionRunCallback<PauseTimerAction>(
                    parameters = actionParametersOf(
                        QuickLogWidgetKeys.playIdActionParam to playId,
                    )
                ),
            )
        }
    }
}

private fun formatElapsedTime(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        "%d:%02d:%02d".format(h, m, s)
    } else {
        "%d:%02d".format(m, s)
    }
}
