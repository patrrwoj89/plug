package com.polishmediahub.app.data.remote.debrid

import com.polishmediahub.app.model.MediaItem
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamRulesEngineTest {

    private fun mockMediaItem(
        title: String,
        subtitle: String = "",
        description: String = ""
    ): MediaItem {
        val item = mockk<MediaItem>(relaxed = true)
        every { item.title } returns title
        every { item.subtitle } returns subtitle
        every { item.description } returns description
        every { item.id } returns "id:$title"
        every { item.videoUrl } returns ""
        every { item.type } returns MediaItem.Type.MOVIE
        return item
    }

    @Test
    fun `disabled rules leave list unchanged`() {
        val items = listOf(
            mockMediaItem("Foo 1080p HEVC 2.5 GB"),
            mockMediaItem("Bar 720p 500 MB")
        )
        val rules = StreamRules(enabled = false)
        val result = StreamRulesEngine.apply(items, rules)
        assertEquals(items.size, result.size)
    }

    @Test
    fun `filters by size range`() {
        val items = listOf(
            mockMediaItem("Movie A 1080p HEVC 2.5 GB"),
            mockMediaItem("Movie B 4K 60 GB"),
            mockMediaItem("Movie C 720p 300 MB")
        )
        val rules = StreamRules(
            enabled = true,
            sizeMinMb = 1000,
            sizeMaxMb = 51200,
            resolutions = emptySet()
        )
        val result = StreamRulesEngine.apply(items, rules)
        assertEquals(1, result.size)
        assertTrue(result.first().title.contains("Movie A"))
    }

    @Test
    fun `filters by resolution`() {
        val items = listOf(
            mockMediaItem("Movie A 4K HDR 5GB"),
            mockMediaItem("Movie B 1080p 2GB"),
            mockMediaItem("Movie C 720p 1GB")
        )
        val rules = StreamRules(
            enabled = true,
            resolutions = setOf("1080p", "4K")
        )
        val result = StreamRulesEngine.apply(items, rules)
        assertEquals(2, result.size)
        assertTrue(result.none { it.title.contains("720p") })
    }

    @Test
    fun `requires and excludes tags`() {
        val items = listOf(
            mockMediaItem("Movie A 1080p HDR Atmos HEVC 2GB"),
            mockMediaItem("Movie B 1080p HEVC 2GB"),
            mockMediaItem("Movie C 1080p HDR H264 2GB")
        )
        val rules = StreamRules(
            enabled = true,
            resolutions = setOf("1080p"),
            requiredVideoTags = setOf("HDR"),
            requiredAudioTags = setOf("Atmos"),
            excludedEncoders = setOf("H264")
        )
        val result = StreamRulesEngine.apply(items, rules)
        assertEquals(1, result.size)
        assertTrue(result.first().title.contains("Movie A"))
    }

    @Test
    fun `prefers higher resolution and matched tags`() {
        val items = listOf(
            mockMediaItem("Movie 720p HDR 1GB"),
            mockMediaItem("Movie 4K HDR Atmos HEVC 8GB"),
            mockMediaItem("Movie 1080p HEVC 2GB")
        )
        val rules = StreamRules(
            enabled = true,
            preferredVideoTags = setOf("HDR"),
            preferredAudioTags = setOf("Atmos"),
            preferredEncoders = setOf("HEVC")
        )
        val result = StreamRulesEngine.apply(items, rules)
        assertTrue(result.first().title.contains("4K"))
    }

    @Test
    fun `maxResults limits output`() {
        val items = (1..10).map { mockMediaItem("Movie $it 1080p ${it}GB") }
        val rules = StreamRules(enabled = true, maxResults = 3)
        val result = StreamRulesEngine.apply(items, rules)
        assertEquals(3, result.size)
    }
}
