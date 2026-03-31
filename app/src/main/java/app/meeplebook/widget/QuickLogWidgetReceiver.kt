package app.meeplebook.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * BroadcastReceiver entry point for the [QuickLogWidget] Glance app widget.
 * Registered in AndroidManifest.xml with the APPWIDGET_UPDATE intent filter.
 */
class QuickLogWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickLogWidget()
}
