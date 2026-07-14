package com.polishmediahub.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Query("SELECT * FROM watched ORDER BY watchedAt DESC")
    fun observeAll(): Flow<List<WatchedEntity>>

    @Query("SELECT * FROM watched WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<WatchedEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WatchedEntity)

    @Query("DELETE FROM watched WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM watched")
    suspend fun clearAll()

    @Query("SELECT * FROM custom_lists ORDER BY createdAt DESC")
    fun observeCustomLists(): Flow<List<CustomListEntity>>

    @Query("SELECT * FROM custom_list_items WHERE listId = :listId ORDER BY addedAt DESC")
    fun observeCustomListItems(listId: String): Flow<List<CustomListItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomList(list: CustomListEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomListItem(item: CustomListItemEntity)

    @Query("DELETE FROM custom_lists WHERE listId = :listId")
    suspend fun deleteCustomList(listId: String)

    @Query("DELETE FROM custom_list_items WHERE listId = :listId AND mediaId = :mediaId")
    suspend fun deleteCustomListItem(listId: String, mediaId: String)

    @Transaction
    @Query("DELETE FROM custom_list_items WHERE listId = :listId")
    suspend fun deleteAllCustomListItems(listId: String)
}
