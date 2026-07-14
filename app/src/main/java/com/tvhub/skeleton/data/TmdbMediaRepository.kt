package com.tvhub.skeleton.data

import com.tvhub.skeleton.model.Category
import com.tvhub.skeleton.model.MediaItem

/**
 * Stub implementation for TMDB integration.
 * Replace the TODOs with real network calls to https://api.themoviedb.org/3
 * and provide your API key in BuildConfig or a secure config.
 */
class TmdbMediaRepository : MediaRepository {

    override suspend fun featured(): List<MediaItem> {
        // TODO: fetch /trending/movie/week and map to MediaItem
        return emptyList()
    }

    override suspend fun categories(): List<Category> {
        // TODO: fetch categories such as /movie/popular, /tv/popular, etc.
        return emptyList()
    }

    override suspend fun search(query: String): List<MediaItem> {
        // TODO: call /search/multi or /search/movie and map results
        return emptyList()
    }

    override suspend fun byId(id: String): MediaItem? {
        // TODO: call /movie/{id} or /tv/{id}
        return null
    }
}
