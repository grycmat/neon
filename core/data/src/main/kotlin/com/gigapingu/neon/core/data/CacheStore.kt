package com.gigapingu.neon.core.data

import com.gigapingu.neon.core.database.CacheDao
import com.gigapingu.neon.core.database.EntityCacheRow
import com.gigapingu.neon.core.database.ListCacheRow
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    // (De)serialization is CPU-bound and was otherwise running on whatever
    // dispatcher the caller happened to be on (typically Dispatchers.Main via
    // viewModelScope.launch) — Room's own suspend DAO calls hop to their own
    // executor internally, but that doesn't cover the json.encode/decode work
    // around them, so it's dispatched explicitly here.
    suspend fun <T : Any> putList(listKey: String, items: List<T>, serializer: KSerializer<T>, id: (T) -> String) {
        withContext(Dispatchers.Default) {
            val rows = items.mapIndexed { index, item ->
                ListCacheRow(
                    listKey = listKey,
                    position = index,
                    entityId = id(item),
                    json = json.encodeToString(serializer, item),
                )
            }
            dao.replaceList(listKey, rows)
        }
    }

    suspend fun <T : Any> getList(listKey: String, serializer: KSerializer<T>): List<T> {
        val raw = dao.getList(listKey)
        return withContext(Dispatchers.Default) {
            raw.mapNotNull { runCatching { json.decodeFromString(serializer, it) }.getOrNull() }
        }
    }

    suspend fun <T : Any> putEntity(entityKey: String, item: T, serializer: KSerializer<T>) {
        val row = withContext(Dispatchers.Default) {
            EntityCacheRow(
                entityKey = entityKey,
                json = json.encodeToString(serializer, item),
                updatedAt = System.currentTimeMillis(),
            )
        }
        dao.putEntity(row)
    }

    suspend fun <T : Any> getEntity(entityKey: String, serializer: KSerializer<T>): T? {
        val raw = dao.getEntity(entityKey) ?: return null
        return withContext(Dispatchers.Default) {
            runCatching { json.decodeFromString(serializer, raw) }.getOrNull()
        }
    }

    suspend fun clear() = dao.clear()
}
