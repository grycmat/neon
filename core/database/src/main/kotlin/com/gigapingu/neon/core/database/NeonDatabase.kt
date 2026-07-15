package com.gigapingu.neon.core.database

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction

/**
 * Cache layer, mirroring the Flutter sqflite schema: raw entity JSON keyed by
 * list + position so timelines, notifications and profiles render instantly
 * offline (cache-first, then network).
 */
@Entity(tableName = "list_cache", primaryKeys = ["list_key", "position"])
data class ListCacheRow(
    @ColumnInfo(name = "list_key") val listKey: String,
    val position: Int,
    @ColumnInfo(name = "entity_id") val entityId: String,
    val json: String,
)

@Entity(tableName = "entity_cache", primaryKeys = ["entity_key"])
data class EntityCacheRow(
    @ColumnInfo(name = "entity_key") val entityKey: String,
    val json: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Dao
interface CacheDao {

    /** Replaces the cached list under [listKey] (e.g. "timeline:home") with [rows]. */
    @Transaction
    suspend fun replaceList(listKey: String, rows: List<ListCacheRow>) {
        deleteList(listKey)
        insertListRows(rows)
    }

    @Query("DELETE FROM list_cache WHERE list_key = :listKey")
    suspend fun deleteList(listKey: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListRows(rows: List<ListCacheRow>)

    @Query("SELECT json FROM list_cache WHERE list_key = :listKey ORDER BY position ASC")
    suspend fun getList(listKey: String): List<String>

    /** Upserts a single entity (e.g. "account:123", "me"). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putEntity(row: EntityCacheRow)

    @Query("SELECT json FROM entity_cache WHERE entity_key = :entityKey LIMIT 1")
    suspend fun getEntity(entityKey: String): String?

    /** Wipes everything — called on logout. */
    @Transaction
    suspend fun clear() {
        clearLists()
        clearEntities()
    }

    @Query("DELETE FROM list_cache")
    suspend fun clearLists()

    @Query("DELETE FROM entity_cache")
    suspend fun clearEntities()
}

@Database(entities = [ListCacheRow::class, EntityCacheRow::class], version = 1, exportSchema = true)
abstract class NeonDatabase : RoomDatabase() {
    abstract fun cacheDao(): CacheDao
}
