package com.example

import com.example.model.InstagramMediaMetadata
import com.example.model.MediaItemDetail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InstaDLUnitTest {

    @Test
    fun testCarouselModel() {
        val items = listOf(
            MediaItemDetail("1", "image", "http://example.com/1.jpg", "http://example.com/1_thumb.jpg", 0),
            MediaItemDetail("2", "video", "http://example.com/2.mp4", "http://example.com/2_thumb.jpg", 1)
        )
        val meta = InstagramMediaMetadata(
            id = "test_carousel",
            title = "Test Post",
            caption = "Test Caption",
            author = "test_user",
            mediaType = "carousel",
            thumbnailUrl = "http://example.com/1_thumb.jpg",
            mediaItems = items
        )

        assertTrue(meta.isCarousel)
        assertFalse(meta.isSingleVideo)
        assertEquals(2, meta.mediaItems.size)
        assertTrue(meta.mediaItems[1].isVideo)
        assertFalse(meta.mediaItems[0].isVideo)
    }

    @Test
    fun testSingleReelModel() {
        val items = listOf(
            MediaItemDetail("reel_1", "video", "http://example.com/reel.mp4", "http://example.com/reel_thumb.jpg", 0)
        )
        val meta = InstagramMediaMetadata(
            id = "test_reel",
            title = "Test Reel",
            caption = "Reel Caption",
            author = "creator_handle",
            mediaType = "video",
            thumbnailUrl = "http://example.com/reel_thumb.jpg",
            mediaItems = items
        )

        assertFalse(meta.isCarousel)
        assertTrue(meta.isSingleVideo)
        assertEquals(1, meta.mediaItems.size)
        assertTrue(meta.mediaItems[0].isVideo)
    }
}
