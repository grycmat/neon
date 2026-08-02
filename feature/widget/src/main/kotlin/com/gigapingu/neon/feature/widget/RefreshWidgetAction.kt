package com.gigapingu.neon.feature.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import dagger.hilt.android.EntryPointAccessors

/**
 * The refresh button in the widget header. Must stay public with a no-arg constructor — Glance
 * instantiates it reflectively from the class name it stored in the PendingIntent.
 */
class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        EntryPointAccessors
            .fromApplication(context, WidgetEntryPoint::class.java)
            .widgetRepository()
            .refresh()
    }
}
