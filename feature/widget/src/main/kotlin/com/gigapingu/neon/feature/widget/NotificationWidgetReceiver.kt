package com.gigapingu.neon.feature.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * AppWidget provider for [NotificationWidget].
 *
 * Intentionally has no `onUpdate` override. `GlanceAppWidgetReceiver.onUpdate` already claims the
 * receiver's single `goAsync()` PendingResult for its own composition work, so a second
 * `goAsync()` here would return null — and skipping `super` instead would lose the bookkeeping
 * `GlanceAppWidgetManager` needs to resolve ids for `updateAll`. The cold-path network fetch is
 * done by [NotificationWidgetRepository.refreshIfStale] inside `provideGlance` instead, which runs
 * within Glance's own keep-alive window.
 */
class NotificationWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NotificationWidget()
}
