package com.gigapingu.neon.core.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gigapingu.neon.core.data.di.ApplicationScope
import com.gigapingu.neon.core.model.Account
import com.gigapingu.neon.core.network.ApiClient
import com.gigapingu.neon.core.network.ApiException
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.unifiedpush.android.connector.UnifiedPush

enum class AuthStatus { Unknown, Unauthenticated, Authenticated }

private val Context.credentialStore by preferencesDataStore(name = "neon_credentials")

/**
 * Owns the OAuth flow and the logged-in account.
 *
 * Flow (in-app WebView):
 *  1. [beginLogin] registers an app on the instance (POST /api/v1/apps)
 *     and returns the /oauth/authorize URL to load in the WebView.
 *  2. The WebView intercepts the [NeonConfig.OAUTH_REDIRECT_URI] navigation
 *     and hands the `code` to [finishLogin], which exchanges it for a token.
 *  3. Credentials are persisted in DataStore; ApiClient is configured.
 */
@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: ApiClient,
    private val cache: CacheStore,
    private val json: Json,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private object Keys {
        val instance = stringPreferencesKey("instance")
        val clientId = stringPreferencesKey("client_id")
        val clientSecret = stringPreferencesKey("client_secret")
        val token = stringPreferencesKey("access_token")
    }

    private val _status = MutableStateFlow(AuthStatus.Unknown)
    val status: StateFlow<AuthStatus> = _status.asStateFlow()

    private val _me = MutableStateFlow<Account?>(null)
    val me: StateFlow<Account?> = _me.asStateFlow()

    val instance: String? get() = api.instance
    val redirectUri: String get() = NeonConfig.OAUTH_REDIRECT_URI
    val defaultInstanceHint: String get() = NeonConfig.DEFAULT_INSTANCE

    private var pendingInstance: String? = null
    private var pendingClientId: String? = null
    private var pendingClientSecret: String? = null

    /** Restore a stored session on app start. */
    suspend fun restore() {
        val prefs = context.credentialStore.data.first()
        val instance = prefs[Keys.instance]
        val token = prefs[Keys.token]
        if (instance == null || token == null) {
            _status.value = AuthStatus.Unauthenticated
            return
        }
        api.configure(instance = instance, token = token)
        try {
            _me.value = json.decodeFromString(
                Account.serializer(),
                api.get("/api/v1/accounts/verify_credentials"),
            )
            _status.value = AuthStatus.Authenticated
        } catch (e: ApiException) {
            if (e.statusCode == 401 || e.statusCode == 403) {
                logout()
                return
            }
            restoreOffline()
        } catch (_: Exception) {
            // Offline — trust the stored token, use cached account if present.
            restoreOffline()
        }
        _me.value?.let { cache.putEntity("me", it, Account.serializer()) }
    }

    private suspend fun restoreOffline() {
        cache.getEntity("me", Account.serializer())?.let { _me.value = it }
        _status.value = AuthStatus.Authenticated
    }

    /** Step 1 — register the app and build the authorize URL. */
    suspend fun beginLogin(rawInstance: String): String {
        val instance = normalizeInstance(rawInstance)
        api.configure(instance = instance)
        val body = buildJsonObject {
            put("client_name", NeonConfig.APP_NAME)
            put("redirect_uris", redirectUri)
            put("scopes", NeonConfig.OAUTH_SCOPES)
            put("website", NeonConfig.APP_WEBSITE)
        }
        val app = json.parseToJsonElement(api.post("/api/v1/apps", body.toString())).jsonObject
        pendingInstance = instance
        pendingClientId = app.getValue("client_id").jsonPrimitive.content
        pendingClientSecret = app.getValue("client_secret").jsonPrimitive.content
        return api.buildUrl(
            "/oauth/authorize",
            mapOf(
                "client_id" to pendingClientId,
                "redirect_uri" to redirectUri,
                "response_type" to "code",
                "scope" to NeonConfig.OAUTH_SCOPES,
            ),
        ).toString()
    }

    /** Step 2 — exchange the authorization code for an access token. */
    suspend fun finishLogin(code: String) {
        val instance = checkNotNull(pendingInstance)
        val clientId = checkNotNull(pendingClientId)
        val clientSecret = checkNotNull(pendingClientSecret)
        val body = buildJsonObject {
            put("grant_type", "authorization_code")
            put("code", code)
            put("client_id", clientId)
            put("client_secret", clientSecret)
            put("redirect_uri", redirectUri)
            put("scope", NeonConfig.OAUTH_SCOPES)
        }
        val token = json.parseToJsonElement(api.post("/oauth/token", body.toString())).jsonObject
        val accessToken = token.getValue("access_token").jsonPrimitive.content
        api.configure(instance = instance, token = accessToken)
        val me = json.decodeFromString(
            Account.serializer(),
            api.get("/api/v1/accounts/verify_credentials"),
        )
        context.credentialStore.edit { prefs ->
            prefs[Keys.instance] = instance
            prefs[Keys.clientId] = clientId
            prefs[Keys.clientSecret] = clientSecret
            prefs[Keys.token] = accessToken
        }
        cache.putEntity("me", me, Account.serializer())
        _me.value = me
        _status.value = AuthStatus.Authenticated
    }

    /** Refreshes the cached self account (e.g. after a profile edit). */
    fun updateMe(account: Account) {
        _me.value = account
        scope.launch { cache.putEntity("me", account, Account.serializer()) }
    }

    suspend fun logout() {
        val prefs = context.credentialStore.data.first()
        val clientId = prefs[Keys.clientId]
        val clientSecret = prefs[Keys.clientSecret]
        val token = prefs[Keys.token]
        if (clientId != null && clientSecret != null && token != null) {
            runCatching {
                val body = buildJsonObject {
                    put("client_id", clientId)
                    put("client_secret", clientSecret)
                    put("token", token)
                }
                api.post("/oauth/revoke", body.toString())
            } // best effort
        }
        context.credentialStore.edit { it.clear() }
        cache.clear()
        api.reset()
        runCatching { UnifiedPush.unregister(context) }
        _me.value = null
        _status.value = AuthStatus.Unauthenticated
    }

    private fun normalizeInstance(raw: String): String {
        var s = raw.trim().lowercase()
        s = s.replaceFirst(Regex("^https?://"), "")
        if ('@' in s) s = s.substringAfterLast('@') // user pasted a handle
        return s.substringBefore('/')
    }
}
