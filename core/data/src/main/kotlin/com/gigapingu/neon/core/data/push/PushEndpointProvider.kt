package com.gigapingu.neon.core.data.push

/**
 * Status of the underlying push distributor/transport on the device.
 */
sealed interface PushDistributorStatus {
    /** Push transport is available (e.g. FCM on GMS or active UnifiedPush distributor on FOSS). */
    data class Available(val distributorName: String? = null) : PushDistributorStatus

    /** UnifiedPush flavor with no distributor app (e.g. ntfy) installed on the device. */
    data object NotInstalled : PushDistributorStatus

    /** Multiple UnifiedPush distributors installed, user should select one. */
    data class Undecided(val distributors: List<String>) : PushDistributorStatus
}

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

    /** Returns current status of push distributor on the device. */
    fun getDistributorStatus(): PushDistributorStatus = PushDistributorStatus.Available()

    /** Selects a distributor by package name (relevant for UnifiedPush when multiple are installed). */
    fun selectDistributor(packageName: String) {}
}

