package com.gigapingu.neon.core.data

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
