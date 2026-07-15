package com.polishmediahub.app.data

import com.polishmediahub.app.data.local.CustomListEntity
import com.polishmediahub.app.data.local.CustomListItemEntity
import com.polishmediahub.app.data.local.HistoryDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomListsRepository @Inject constructor(
    private val historyDao: HistoryDao,
    private val profileRepository: ProfileRepository
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeLists(): Flow<List<CustomListEntity>> =
        currentProfileIdFlow().flatMapLatest { profileId ->
            historyDao.observeCustomLists(profileId)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeListItems(listId: String): Flow<List<CustomListItemEntity>> =
        currentProfileIdFlow().flatMapLatest { profileId ->
            historyDao.observeCustomListItems(profileId, listId)
        }

    suspend fun createList(name: String): String = withContext(Dispatchers.IO) {
        val profileId = currentProfileId() ?: return@withContext ""
        val id = UUID.randomUUID().toString()
        historyDao.insertCustomList(
            CustomListEntity(profileId = profileId, listId = id, name = name)
        )
        id
    }

    suspend fun addItem(listId: String, mediaId: String) = withContext(Dispatchers.IO) {
        val profileId = currentProfileId() ?: return@withContext
        historyDao.insertCustomListItem(
            CustomListItemEntity(profileId = profileId, listId = listId, mediaId = mediaId)
        )
    }

    suspend fun removeItem(listId: String, mediaId: String) = withContext(Dispatchers.IO) {
        val profileId = currentProfileId() ?: return@withContext
        historyDao.deleteCustomListItem(profileId, listId, mediaId)
    }

    suspend fun deleteList(listId: String) = withContext(Dispatchers.IO) {
        val profileId = currentProfileId() ?: return@withContext
        historyDao.deleteAllCustomListItems(profileId, listId)
        historyDao.deleteCustomList(profileId, listId)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun currentProfileIdFlow() = profileRepository.currentProfile.filterNotNull().map { it.id }

    private fun currentProfileId(): String? = profileRepository.currentProfile.value?.id
}
