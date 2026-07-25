package com.gigapingu.neon.core.data

import com.gigapingu.neon.core.model.Account
import com.gigapingu.neon.core.model.MastodonList
import com.gigapingu.neon.core.network.ApiClient
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/** User-curated lists (https://docs.joinmastodon.org/methods/lists/). */
@Singleton
class ListRepository @Inject constructor(
    private val api: ApiClient,
    private val json: Json,
) {
    suspend fun getLists(): List<MastodonList> =
        json.decodeFromString(ListSerializer(MastodonList.serializer()), api.get("/api/v1/lists"))

    suspend fun getList(id: String): MastodonList =
        json.decodeFromString(MastodonList.serializer(), api.get("/api/v1/lists/$id"))

    /** Lists this account belongs to, among the lists owned by the logged-in user. */
    suspend fun getListsContaining(accountId: String): List<MastodonList> = json.decodeFromString(
        ListSerializer(MastodonList.serializer()),
        api.get("/api/v1/accounts/$accountId/lists"),
    )

    suspend fun createList(title: String, repliesPolicy: String = "list"): MastodonList = json.decodeFromString(
        MastodonList.serializer(),
        api.post("/api/v1/lists", listBody(title, repliesPolicy)),
    )

    suspend fun updateList(id: String, title: String, repliesPolicy: String = "list"): MastodonList =
        json.decodeFromString(
            MastodonList.serializer(),
            api.put("/api/v1/lists/$id", listBody(title, repliesPolicy)),
        )

    suspend fun deleteList(id: String) {
        api.delete("/api/v1/lists/$id")
    }

    suspend fun getListAccounts(id: String, maxId: String? = null, limit: Int = 40): List<Account> {
        val query = buildMap {
            put("limit", limit)
            maxId?.let { put("max_id", it) }
        }
        return json.decodeFromString(ListSerializer(Account.serializer()), api.get("/api/v1/lists/$id/accounts", query))
    }

    suspend fun addAccount(listId: String, accountId: String) {
        api.post("/api/v1/lists/$listId/accounts", accountIdsBody(accountId))
    }

    suspend fun removeAccount(listId: String, accountId: String) {
        api.delete("/api/v1/lists/$listId/accounts", accountIdsBody(accountId))
    }

    private fun listBody(title: String, repliesPolicy: String) = buildJsonObject {
        put("title", title)
        put("replies_policy", repliesPolicy)
    }.toString()

    private fun accountIdsBody(accountId: String) = buildJsonObject {
        putJsonArray("account_ids") { add(accountId) }
    }.toString()
}
