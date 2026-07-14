package com.tvhub.skeleton.data

import com.tvhub.skeleton.data.local.CustomListEntity
import com.tvhub.skeleton.data.local.CustomListItemEntity
import com.tvhub.skeleton.data.local.HistoryDao
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomListsRepository @Inject constructor(
    private val historyDao: HistoryDao
) {

    fun observeLists(): Flow<List<CustomListEntity>> = historyDao.observeCustomLists()

    fun observeListItems(listId: String): Flow<List<CustomListItemEntity>> =
        historyDao.observeCustomListItems(listId)

    suspend fun createList(name: String): String {
        val id = UUID.randomUUID().toString()
        historyDao.insertCustomList(CustomListEntity(listId = id, name = name))
        return id
    }

    suspend fun addItem(listId: String, mediaId: String) {
        historyDao.insertCustomListItem(CustomListItemEntity(listId = listId, mediaId = mediaId))
    }

    suspend fun removeItem(listId: String, mediaId: String) {
        historyDao.deleteCustomListItem(listId, mediaId)
    }

    suspend fun deleteList(listId: String) {
        historyDao.deleteAllCustomListItems(listId)
        historyDao.deleteCustomList(listId)
    }
}
