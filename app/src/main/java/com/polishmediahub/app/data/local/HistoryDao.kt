package com.polishmediahub.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Query("SELECT * FROM watched WHERE profileId = :profileId ORDER BY watchedAt DESC")
    fun observeAll(profileId: String): Flow<List<WatchedEntity>>

    @Query("SELECT * FROM watched WHERE profileId = :profileId ORDER BY watchedAt DESC")
    suspend fun getAll(profileId: String): List<WatchedEntity>

    @Query("SELECT * FROM watched WHERE profileId = :profileId AND id = :id LIMIT 1")
    fun observeById(profileId: String, id: String): Flow<WatchedEntity?>

    @Query("SELECT * FROM watched WHERE profileId = :profileId AND id = :id LIMIT 1")
    suspend fun getById(profileId: String, id: String): WatchedEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WatchedEntity)

    @Query("DELETE FROM watched WHERE profileId = :profileId AND id = :id")
    suspend fun delete(profileId: String, id: String)

    @Query("DELETE FROM watched WHERE profileId = :profileId")
    suspend fun clearAll(profileId: String)

    @Query("SELECT * FROM custom_lists WHERE profileId = :profileId ORDER BY createdAt DESC")
    fun observeCustomLists(profileId: String): Flow<List<CustomListEntity>>

    @Query("SELECT * FROM custom_list_items WHERE profileId = :profileId AND listId = :listId ORDER BY addedAt DESC")
    fun observeCustomListItems(profileId: String, listId: String): Flow<List<CustomListItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomList(list: CustomListEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomListItem(item: CustomListItemEntity)

    @Query("DELETE FROM custom_lists WHERE profileId = :profileId AND listId = :listId")
    suspend fun deleteCustomList(profileId: String, listId: String)

    @Query("DELETE FROM custom_list_items WHERE profileId = :profileId AND listId = :listId AND mediaId = :mediaId")
    suspend fun deleteCustomListItem(profileId: String, listId: String, mediaId: String)

    @Transaction
    @Query("DELETE FROM custom_list_items WHERE profileId = :profileId AND listId = :listId")
    suspend fun deleteAllCustomListItems(profileId: String, listId: String)
}
