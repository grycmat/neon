package com.gigapingu.neon.feature.notifications

import android.content.Context

/**
 * Persists the last distributor endpoint URL across process restarts, so
 * [UnifiedPushEndpointProvider.getEndpoint] has a fast path instead of waiting on a fresh
 * `onNewEndpoint` callback on every cold start. Not secret (a public push URL) — plain prefs.
 */
internal object UnifiedPushEndpointStore {
    private const val PREFS_NAME = "neon_unifiedpush"
    private const val KEY_ENDPOINT = "endpoint_url"

    fun save(context: Context, endpoint: String) {
        prefs(context).edit().putString(KEY_ENDPOINT, endpoint).apply()
    }

    fun get(context: Context): String? = prefs(context).getString(KEY_ENDPOINT, null)

    fun clear(context: Context) {
        prefs(context).edit().remove(KEY_ENDPOINT).apply()
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
