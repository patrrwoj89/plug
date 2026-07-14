package com.tvhub.skeleton.data

import com.tvhub.skeleton.model.Category
import com.tvhub.skeleton.model.MediaItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockDataSource @Inject constructor() {

    private val placeholders = listOf(
        "https://picsum.photos/seed/%s/300/450",
        "https://picsum.photos/seed/%s/800/450"
    )

    // Sample public-domain / Creative Commons video for player demos.
    private val sampleVideo = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"

    fun featured(): List<MediaItem> = listOf(
        MediaItem(
            id = "f1",
            title = "Ostatni Jedi",
            subtitle = "Akcja, Sci-Fi • 2027",
            description = "Epicka opowieść o odrodzeniu galaktycznego ruchu oporu.",
            posterUrl = placeholder("f1"),
            backdropUrl = placeholderWide("f1"),
            year = "2027",
            duration = "2h 14m",
            rating = "8.4",
            videoUrl = sampleVideo,
            genres = listOf("Akcja", "Sci-Fi"),
            type = MediaItem.Type.MOVIE
        ),
        MediaItem(
            id = "f2",
            title = "Mroczne Lustro",
            subtitle = "Thriller • 2026",
            description = "Technologiczny thriller o granicach ludzkiej pamięci.",
            posterUrl = placeholder("f2"),
            backdropUrl = placeholderWide("f2"),
            year = "2026",
            duration = "1h 58m",
            rating = "7.9",
            genres = listOf("Thriller", "Drama"),
            type = MediaItem.Type.MOVIE
        )
    )

    fun categories(): List<Category> = listOf(
        Category(
            id = "continue",
            name = "Oglądaj dalej",
            items = items("c", 6)
        ),
        Category(
            id = "trending",
            name = "Na czasie",
            items = items("t", 12)
        ),
        Category(
            id = "movies",
            name = "Filmy",
            items = items("m", 10)
        ),
        Category(
            id = "series",
            name = "Seriale",
            items = items("s", 10)
        ),
        Category(
            id = "kids",
            name = "Dla dzieci",
            items = items("k", 8)
        )
    )

    fun search(query: String): List<MediaItem> {
        if (query.isBlank()) return emptyList()
        return items("q", 20).filter {
            it.title.contains(query, ignoreCase = true) ||
                it.genres.any { g -> g.contains(query, ignoreCase = true) }
        }
    }

    fun byId(id: String): MediaItem? = (featured() + categories().flatMap { it.items }).find { it.id == id }

    private fun items(prefix: String, count: Int): List<MediaItem> =
        (1..count).map { index ->
            val seed = "$prefix$index"
            MediaItem(
                id = seed,
                title = "Tytuł $seed",
                subtitle = "Gatunek • 202${index % 5 + 4}",
                description = "Przykładowy opis dla pozycji $seed. W realnej aplikacji będzie tu zawartość z Twojego źródła danych.",
                posterUrl = placeholder(seed),
                backdropUrl = placeholderWide(seed),
                year = "202${index % 5 + 4}",
                duration = "${index % 2 + 1}h ${(index * 7) % 60}m",
                rating = "${5 + (index % 40) / 10.0f}",
                videoUrl = if (index % 5 == 0) sampleVideo else null,
                genres = listOf("Dramat", "Komedia", "Akcja", "Dokument", "Sci-Fi").shuffled().take(2),
                type = if (index % 3 == 0) MediaItem.Type.SERIES else MediaItem.Type.MOVIE
            )
        }

    private fun placeholder(seed: String): String = placeholders[0].format(seed)
    private fun placeholderWide(seed: String): String = placeholders[1].format(seed)
}
