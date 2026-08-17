package com.gigapingu.neon.core.data

/** Build-time app configuration (the Flutter app read these from .env). */
object NeonConfig {
    const val APP_NAME = "Neon"
    const val APP_WEBSITE = "https://gigapingu.com"
    const val OAUTH_REDIRECT_URI = "neon://oauth"
    const val OAUTH_SCOPES = "read write follow push"
    const val DEFAULT_INSTANCE = "mastodon.social"
}
