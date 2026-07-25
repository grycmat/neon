package com.gigapingu.neon.core.data

import com.gigapingu.neon.core.model.FeaturedTag
import com.gigapingu.neon.core.model.TrendTag
import com.gigapingu.neon.core.network.ApiClient
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Hashtag follow state, and a profile's featured tags. */
@Singleton
class TagRepository @Inject constructor(
    private val api: ApiClient,
    private val json: Json,
) {
    suspend fun getFeaturedTags(accountId: String): List<FeaturedTag> = json.decodeFromString(
        ListSerializer(FeaturedTag.serializer()),
        api.get("/api/v1/accounts/$accountId/featured_tags"),
    )

    suspend fun getMyFeaturedTags(): List<FeaturedTag> = json.decodeFromString(
        ListSerializer(FeaturedTag.serializer()),
        api.get("/api/v1/featured_tags"),
    )

    suspend fun featureTag(name: String): FeaturedTag = json.decodeFromString(
        FeaturedTag.serializer(),
        api.post("/api/v1/featured_tags", buildJsonObject { put("name", name) }.toString()),
    )

    suspend fun unfeatureTag(id: String) {
        api.delete("/api/v1/featured_tags/$id")
    }

    suspend fun getTag(name: String): TrendTag =
        json.decodeFromString(TrendTag.serializer(), api.get("/api/v1/tags/$name"))

    suspend fun followTag(name: String): TrendTag =
        json.decodeFromString(TrendTag.serializer(), api.post("/api/v1/tags/$name/follow"))

    suspend fun unfollowTag(name: String): TrendTag =
        json.decodeFromString(TrendTag.serializer(), api.post("/api/v1/tags/$name/unfollow"))

    suspend fun getFollowedTags(maxId: String? = null, limit: Int = 40): List<TrendTag> {
        val query = buildMap {
            put("limit", limit)
            maxId?.let { put("max_id", it) }
        }
        return json.decodeFromString(ListSerializer(TrendTag.serializer()), api.get("/api/v1/followed_tags", query))
    }
}
