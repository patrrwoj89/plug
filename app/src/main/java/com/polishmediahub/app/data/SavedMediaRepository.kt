package com.polishmediahub.app.data

import com.polishmediahub.app.data.local.SavedMediaDao
import com.polishmediahub.app.data.local.SavedMediaEntity
import com.polishmediahub.app.model.MediaItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SavedMediaRepository @Inject constructor(
    private val dao: SavedMediaDao
) {

    fun observeLibrary(): Flow<List<MediaItem>> =
        dao.observeByType(TYPE_LIBRARY).map { it.map(::toModel) }

    fun observeWatchlist(): Flow<List<MediaItem>> =
        dao.observeByType(TYPE_WATCHLIST).map { it.map(::toModel) }

    fun isInLibrary(id: String): Flow<Boolean> = dao.isSaved(id, TYPE_LIBRARY)

    fun isInWatchlist(id: String): Flow<Boolean> = dao.isSaved(id, TYPE_WATCHLIST)

    suspend fun addToLibrary(item: MediaItem) = dao.insert(item.toEntity(TYPE_LIBRARY))

    suspend fun addToWatchlist(item: MediaItem) = dao.insert(item.toEntity(TYPE_WATCHLIST))

    suspend fun removeFromLibrary(id: String) = dao.deleteByIdAndType(id, TYPE_LIBRARY)

    suspend fun removeFromWatchlist(id: String) = dao.deleteByIdAndType(id, TYPE_WATCHLIST)

    private fun MediaItem.toEntity(listType: String) = SavedMediaEntity(
        id = id,
        title = title,
        subtitle = subtitle,
        description = description,
        posterUrl = posterUrl.orEmpty(),
        backdropUrl = backdropUrl.orEmpty(),
        year = year,
        duration = duration,
        rating = rating,
        videoUrl = videoUrl.orEmpty(),
        listType = listType
    )

    private fun toModel(entity: SavedMediaEntity) = MediaItem(
        id = entity.id,
        title = entity.title,
        subtitle = entity.subtitle,
        description = entity.description,
        posterUrl = entity.posterUrl,
        backdropUrl = entity.backdropUrl,
        year = entity.year,
        duration = entity.duration,
        rating = entity.rating,
        videoUrl = entity.videoUrl.takeIf { it.isNotBlank() }
    )

    companion object {
        const val TYPE_LIBRARY = "library"
        const val TYPE_WATCHLIST = "watchlist"
    }
}
