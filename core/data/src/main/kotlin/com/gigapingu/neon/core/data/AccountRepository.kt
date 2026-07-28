package com.gigapingu.neon.core.data

import com.gigapingu.neon.core.model.Account
import com.gigapingu.neon.core.model.AccountField
import com.gigapingu.neon.core.model.Relationship
import com.gigapingu.neon.core.model.Status
import com.gigapingu.neon.core.network.ApiClient
import com.gigapingu.neon.core.network.FilePart
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/** Profiles, relationships, follow lists, and profile editing. */
@Singleton
class AccountRepository @Inject constructor(
    private val api: ApiClient,
    private val cache: CacheStore,
    private val json: Json,
) {
    /** Cached-first profile fetch. */
    suspend fun getCachedAccount(id: String): Account? =
        cache.getEntity("account:$id", Account.serializer())

    suspend fun getAccount(id: String): Account {
        val account = json.decodeFromString(Account.serializer(), api.get("/api/v1/accounts/$id"))
        cache.putEntity("account:$id", account, Account.serializer())
        return account
    }

    suspend fun getStatuses(
        id: String,
        maxId: String? = null,
        excludeReplies: Boolean = true,
        onlyMedia: Boolean = false,
        pinned: Boolean = false,
        limit: Int = 20,
    ): List<Status> {
        val query = buildMap {
            put("limit", limit)
            put("exclude_replies", excludeReplies)
            if (onlyMedia) put("only_media", true)
            if (pinned) put("pinned", true)
            maxId?.let { put("max_id", it) }
        }
        return json.decodeFromString(
            ListSerializer(Status.serializer()),
            api.get("/api/v1/accounts/$id/statuses", query),
        )
    }

    suspend fun getRelationship(id: String): Relationship? =
        json.decodeFromString(
            ListSerializer(Relationship.serializer()),
            api.get("/api/v1/accounts/relationships", mapOf("id[]" to id)),
        ).firstOrNull()

    suspend fun setFollowing(id: String, follow: Boolean): Relationship =
        json.decodeFromString(
            Relationship.serializer(),
            api.post("/api/v1/accounts/$id/${if (follow) "follow" else "unfollow"}"),
        )

    suspend fun getFollowers(id: String, maxId: String? = null): List<Account> =
        accountList("/api/v1/accounts/$id/followers", maxId)

    suspend fun getFollowing(id: String, maxId: String? = null): List<Account> =
        accountList("/api/v1/accounts/$id/following", maxId)

    suspend fun getBlocks(maxId: String? = null): List<Account> =
        accountList("/api/v1/blocks", maxId)

    suspend fun getMutes(maxId: String? = null): List<Account> =
        accountList("/api/v1/mutes", maxId)

    /**
     * favourited_by/reblogged_by only support Link-header pagination (favourite
     * and reblog IDs aren't exposed), so we fetch a single page at the API max.
     */
    suspend fun getFavouritedBy(statusId: String, limit: Int = 80): List<Account> =
        accountList("/api/v1/statuses/$statusId/favourited_by", maxId = null, limit = limit)

    suspend fun getRebloggedBy(statusId: String, limit: Int = 80): List<Account> =
        accountList("/api/v1/statuses/$statusId/reblogged_by", maxId = null, limit = limit)

    private suspend fun accountList(path: String, maxId: String?, limit: Int = 40): List<Account> {
        val query = buildMap {
            put("limit", limit)
            maxId?.let { put("max_id", it) }
        }
        return json.decodeFromString(ListSerializer(Account.serializer()), api.get(path, query))
    }

    /** PATCH /api/v1/accounts/update_credentials (multipart when files given). */
    suspend fun updateCredentials(
        displayName: String? = null,
        note: String? = null,
        locked: Boolean? = null,
        bot: Boolean? = null,
        fields: List<AccountField>? = null,
        avatar: FilePart? = null,
        header: FilePart? = null,
        defaultPrivacy: String? = null,
        defaultLanguage: String? = null,
    ): Account {
        val fieldMap = buildMap {
            displayName?.let { put("display_name", it) }
            note?.let { put("note", it) }
            locked?.let { put("locked", it.toString()) }
            bot?.let { put("bot", it.toString()) }
            fields?.forEachIndexed { i, field ->
                put("fields_attributes[$i][name]", field.name)
                put("fields_attributes[$i][value]", field.value)
            }
            defaultPrivacy?.let { put("source[privacy]", it) }
            defaultLanguage?.let { put("source[language]", it) }
        }
        val account = json.decodeFromString(
            Account.serializer(),
            api.multipart(
                method = "PATCH",
                path = "/api/v1/accounts/update_credentials",
                fields = fieldMap,
                files = listOfNotNull(avatar, header),
            ),
        )
        cache.putEntity("account:${account.id}", account, Account.serializer())
        cache.putEntity("me", account, Account.serializer())
        return account
    }

    suspend fun mute(id: String): Relationship =
        json.decodeFromString(
            Relationship.serializer(),
            api.post("/api/v1/accounts/$id/mute"),
        )

    suspend fun unmute(id: String): Relationship =
        json.decodeFromString(
            Relationship.serializer(),
            api.post("/api/v1/accounts/$id/unmute"),
        )

    suspend fun block(id: String): Relationship =
        json.decodeFromString(
            Relationship.serializer(),
            api.post("/api/v1/accounts/$id/block"),
        )

    suspend fun unblock(id: String): Relationship =
        json.decodeFromString(
            Relationship.serializer(),
            api.post("/api/v1/accounts/$id/unblock"),
        )

    suspend fun report(
        accountId: String,
        statusId: String? = null,
        comment: String? = null,
    ) {
        val body = kotlinx.serialization.json.buildJsonObject {
            put("account_id", accountId)
            statusId?.let {
                putJsonArray("status_ids") { add(it) }
            }
            comment?.let { put("comment", it) }
        }
        api.post("/api/v1/reports", body.toString())
    }
}
