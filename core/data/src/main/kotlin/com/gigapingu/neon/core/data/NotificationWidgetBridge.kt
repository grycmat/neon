package com.gigapingu.neon.core.data

/**
 * Bridge from the notification data layer to the home-screen widget.
 *
 * `core/*` must not depend on `feature/*`, so this mirrors the plain-singleton pattern already
 * used by `Navigator` / `StatusActionService` in core:ui: `feature:widget` installs the [Host]
 * from `NeonApplication.onCreate`, and while it is null (previews, tests, any build without the
 * widget module) every call no-ops.
 */
object NotificationWidgetBridge {

    interface Host {
        /**
         * Redraw the widgets from data that is already persisted. Fire-and-forget on an
         * app-lifetime scope — callers are in-app mutations on the main thread, which must not
         * wait on avatar decoding.
         */
        fun redraw()

        /**
         * Fetch the newest notifications, persist them, then redraw.
         *
         * Suspends until it is done on purpose: the caller is a push entry point holding a
         * `BroadcastReceiver.PendingResult`, and awaiting here is what keeps the process alive
         * long enough for the round-trip to finish.
         */
        suspend fun refresh()
    }

    @Volatile
    var host: Host? = null

    fun redraw() {
        host?.redraw()
    }

    suspend fun refresh() {
        host?.refresh()
    }
}
