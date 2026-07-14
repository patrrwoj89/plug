package com.tvhub.skeleton

import com.tvhub.skeleton.data.MockDataSource
import org.junit.Assert.assertTrue
import org.junit.Test

class MockDataSourceTest {

    private val dataSource = MockDataSource()

    @Test
    fun `featured returns items`() {
        assertTrue(dataSource.featured().isNotEmpty())
    }

    @Test
    fun `search returns results for existing genre`() {
        val results = dataSource.search("Akcja")
        assertTrue(results.isNotEmpty())
    }

    @Test
    fun `search returns empty for blank query`() {
        assertTrue(dataSource.search("").isEmpty())
    }

    @Test
    fun `byId returns existing item`() {
        val featured = dataSource.featured().first()
        assertTrue(dataSource.byId(featured.id) != null)
    }
}
