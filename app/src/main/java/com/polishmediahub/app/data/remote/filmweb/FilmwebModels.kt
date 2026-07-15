package com.polishmediahub.app.data.remote.filmweb

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FilmwebSearchResponse(
    val total: Int = 0,
    val searchHits: List<FilmwebSearchHit> = emptyList()
)

@Serializable
data class FilmwebSearchHit(
    val id: Int? = null,
    val type: String? = null,
    @SerialName("matchedTitle")
    val matchedTitle: String? = null,
    @SerialName("matchedLang")
    val matchedLang: String? = null
)

@Serializable
data class FilmwebInfo(
    val id: Int? = null,
    val title: String? = null,
    @SerialName("originalTitle")
    val originalTitle: String? = null,
    val year: Int? = null,
    val type: String? = null,
    val subType: String? = null,
    val posterPath: String? = null,
    val duration: Int? = null
)

@Serializable
data class FilmwebDescriptionResponse(
    val id: Int? = null,
    val synopsis: String? = null,
    val locale: String? = null
)

@Serializable
data class FilmwebRatingResponse(
    val count: Int? = null,
    val rate: Double? = null,
    @SerialName("countWantToSee")
    val countWantToSee: Int? = null
)

@Serializable
data class FilmwebPreviewResponse(
    val year: Int? = null,
    @SerialName("entityName")
    val entityName: String? = null,
    val plot: FilmwebPlot? = null,
    @SerialName("coverPhoto")
    val coverPhoto: FilmwebCoverPhoto? = null
)

@Serializable
data class FilmwebPlot(
    val synopsis: String? = null
)

@Serializable
data class FilmwebCoverPhoto(
    val id: Int? = null,
    val photo: FilmwebPhoto? = null
)

@Serializable
data class FilmwebPhoto(
    val id: Int? = null,
    val film: Int? = null,
    @SerialName("sourcePath")
    val sourcePath: String? = null,
    @SerialName("fileExtension")
    val fileExtension: String? = null
)
