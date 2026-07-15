package com.polishmediahub.app.data.remote.anime

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KitsuAnimeResponse(
    val data: KitsuAnime,
    val included: List<KitsuIncluded> = emptyList()
)

@Serializable
data class KitsuAnimeListResponse(
    val data: List<KitsuAnime>,
    val included: List<KitsuIncluded> = emptyList()
)

@Serializable
data class KitsuAnime(
    val id: String,
    val type: String,
    val attributes: KitsuAnimeAttributes,
    val relationships: KitsuRelationships = KitsuRelationships()
)

@Serializable
data class KitsuAnimeAttributes(
    val canonicalTitle: String = "",
    @SerialName("titles") val titles: KitsuTitles = KitsuTitles(),
    val synopsis: String? = null,
    val posterImage: KitsuImage? = null,
    val coverImage: KitsuImage? = null,
    val episodeCount: Int? = null,
    val startDate: String? = null,
    val averageRating: String? = null,
    val ageRating: String? = null,
    val ageRatingGuide: String? = null,
    val nsfw: Boolean = false,
    val showType: String? = null,
    val subtype: String? = null,
    val slug: String? = null
)

@Serializable
data class KitsuTitles(
    val en: String? = null,
    @SerialName("en_jp") val enJp: String? = null,
    @SerialName("ja_jp") val jaJp: String? = null
)

@Serializable
data class KitsuImage(
    val tiny: String? = null,
    val small: String? = null,
    val medium: String? = null,
    val large: String? = null,
    val original: String? = null
)

@Serializable
data class KitsuRelationships(
    val mappings: KitsuRelationshipData = KitsuRelationshipData()
)

@Serializable
data class KitsuRelationshipData(
    val data: List<KitsuRelationshipItem> = emptyList()
)

@Serializable
data class KitsuRelationshipItem(
    val id: String,
    val type: String
)

@Serializable
data class KitsuIncluded(
    val id: String,
    val type: String,
    val attributes: KitsuMappingAttributes? = null
)

@Serializable
data class KitsuMappingAttributes(
    val externalSite: String? = null,
    val externalId: String? = null
)
