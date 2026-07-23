package com.gigapingu.neon.core.data

/** Build-time app configuration (the Flutter app read these from .env). */
object NeonConfig {
    const val APP_NAME = "Neon"
    const val APP_WEBSITE = "https://gigapingu.com"
    const val OAUTH_REDIRECT_URI = "neon://oauth"
    const val OAUTH_SCOPES = "read write follow push"
    const val DEFAULT_INSTANCE = "mastodon.social"

    /**
     * Base URL of the mastodon-fcm-relay deployment. The device's FCM token is
     * appended as `/push/<token>` to form the Web Push subscription endpoint that
     * Mastodon POSTs encrypted notifications to. See NOTI.md / RELAY.md.
     *
     * Sourced from `secrets.properties` (gitignored) via BuildConfig so the real
     * relay host is not committed to source control.
     */
    val RELAY_BASE_URL: String = BuildConfig.RELAY_BASE_URL
}
