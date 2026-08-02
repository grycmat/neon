package com.gigapingu.neon.feature.widget

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt access for the widget's non-injectable entry points.
 *
 * `GlanceAppWidget` is instantiated by Glance and `ActionCallback` by the framework, so neither can
 * be `@AndroidEntryPoint` — they reach the graph through `EntryPointAccessors.fromApplication`.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface WidgetEntryPoint {
    fun widgetRepository(): NotificationWidgetRepository
}
