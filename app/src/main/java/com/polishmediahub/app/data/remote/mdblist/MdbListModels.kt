package com.polishmediahub.app.data.remote.mdblist

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MdbListSummary(
    val id: Int,
    val name: String,
    val slug: String = "",
    val description: String = "",
    val mediatype: String = "",
    val items: Int = 0,
    val likes: Int = 0,
    @SerialName("user_id") val userId: Int? = null,
    @SerialName("user_name") val userName: String? = null,
    val dynamic: Boolean = false,
    val private: Boolean = false
)

@Serializable
data class MdbListItems(
    val movies: List<MdbListItem> = emptyList(),
    val shows: List<MdbListItem> = emptyList()
)

@Serializable
data class MdbListItem(
    val id: Int,
    val rank: Int = 0,
    val adult: Int = 0,
    val title: String,
    @SerialName("imdb_id") val imdbId: String? = null,
    @SerialName("tvdb_id") val tvdbId: Int? = null,
    val language: String = "",
    val mediatype: String = "",
    @SerialName("release_year") val releaseYear: Int = 0,
    @SerialName("spoken_language") val spokenLanguage: String = ""
)

@Serializable
data class MdbListSearchResult(
    val search: List<MdbListSearchItem> = emptyList(),
    val total: Int = 0
)

@Serializable
data class MdbListSearchItem(
    val title: String,
    val year: Int = 0,
    val score: Int = 0,
    @SerialName("score_average") val scoreAverage: Int = 0,
    val type: String = "",
    val ids: MdbListSearchIds = MdbListSearchIds()
)

@Serializable
data class MdbListSearchIds(
    val imdbid: String? = null,
    val tmdbid: Int? = null,
    val traktid: Int? = null,
    @SerialName("tvdbid") val tvdbId: Int? = null,
    val malid: Int? = null
)

@Serializable
data class MdbListMediaInfo(
    val title: String = "",
    val year: Int = 0,
    val released: String = "",
    val description: String = "",
    val runtime: Int = 0,
    val score: Int = 0,
    @SerialName("score_average") val scoreAverage: Int = 0,
    val ids: MdbListMediaIds = MdbListMediaIds(),
    val type: String = "",
    val poster: String? = null,
    val backdrop: String? = null
)

@Serializable
data class MdbListMediaIds(
    val imdb: String? = null,
    val trakt: Int? = null,
    val tmdb: Int? = null,
    @SerialName("tvdb") val tvdbId: Int? = null,
    val mal: Int? = null
)
