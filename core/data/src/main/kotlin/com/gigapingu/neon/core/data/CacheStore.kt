package com.gigapingu.neon.core.data

import com.gigapingu.neon.core.database.CacheDao
import com.gigapingu.neon.core.database.EntityCacheRow
import com.gigapingu.neon.core.database.ListCacheRow
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/**
 * Typed facade over the Room cache: (de)serializes models to the raw-JSON
 * rows the database stores. Decode failures degrade to an empty/null result
 * so a schema change never bricks startup.
 */
@Singleton
class CacheStore @Inject constructor(
    private val dao: CacheDao,
    private val json: Json,
) {
    suspend fun <T : Any> putList(listKey: String, items: List<T>, serializer: KSerializer<T>, id: (T) -> String) {
        dao.replaceList(
            listKey,
            items.mapIndexed { index, item ->
                ListCacheRow(
                    listKey = listKey,
                    position = index,
                    entityId = id(item),
                    json = json.encodeToString(serializer, item),
                )
            },
        )
    }

    suspend fun <T : Any> getList(listKey: String, serializer: KSerializer<T>): List<T> =
        dao.getList(listKey).mapNotNull { raw ->
            runCatching { json.decodeFromString(serializer, raw) }.getOrNull()
        }

    suspend fun <T : Any> putEntity(entityKey: String, item: T, serializer: KSerializer<T>) {
        dao.putEntity(
            EntityCacheRow(
                entityKey = entityKey,
                json = json.encodeToString(serializer, item),
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun <T : Any> getEntity(entityKey: String, serializer: KSerializer<T>): T? =
        dao.getEntity(entityKey)?.let { raw ->
            runCatching { json.decodeFromString(serializer, raw) }.getOrNull()
        }

    suspend fun clear() = dao.clear()
}
