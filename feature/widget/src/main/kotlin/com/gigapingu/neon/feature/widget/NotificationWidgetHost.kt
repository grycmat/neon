package com.gigapingu.neon.feature.widget

import com.gigapingu.neon.core.data.NotificationWidgetBridge
import com.gigapingu.neon.core.data.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Installs this module as the implementation behind [NotificationWidgetBridge], so the data layer
 * can push widget updates without `core:data` depending on `feature:widget`.
 *
 * Initialized from `NeonApplication.onCreate`, mirroring `StatusActionService.init` — which means
 * it is in place for every process the system starts, including the one woken purely to deliver a
 * push, since `Application.onCreate` always runs before the receiver does.
 */
@Singleton
class NotificationWidgetHost @Inject constructor(
    private val repository: NotificationWidgetRepository,
    @ApplicationScope private val scope: CoroutineScope,
) : NotificationWidgetBridge.Host {

    fun install() {
        NotificationWidgetBridge.host = this
    }

    /** Callers are repository writes on the main thread — hand the render work off the caller. */
    override fun redraw() {
        scope.launch { repository.redraw() }
    }

    override suspend fun refresh() {
        repository.refresh()
    }
}
