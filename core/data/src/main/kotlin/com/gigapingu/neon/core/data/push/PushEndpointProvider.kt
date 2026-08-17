package com.gigapingu.neon.core.data.push

/**
 * Produces the Web Push subscription endpoint URL for whatever transport this
 * build flavor uses (FCM + the self-hosted relay for gms, a UnifiedPush
 * distributor's own endpoint for foss). One implementation per product flavor,
 * bound via a flavor-scoped Hilt module in feature/notifications.
 */
interface PushEndpointProvider {
    /**
     * Null if no endpoint is available yet (e.g. UnifiedPush registration is
     * still pending) — the flavor's receiver completes registration
     * asynchronously in that case, the same way FCM token rotation does.
     */
    suspend fun getEndpoint(): String?
}
