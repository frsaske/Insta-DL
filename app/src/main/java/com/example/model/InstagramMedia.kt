package com.example.model

data class MediaItemDetail(
    val id: String,
    val type: String, // "video" or "image"
    val url: String,
    val thumbnail: String,
    val index: Int = 0
) {
    val isVideo: Boolean get() = type.equals("video", ignoreCase = true)
}

data class InstagramMediaMetadata(
    val id: String,
    val title: String,
    val caption: String,
    val author: String,
    val mediaType: String, // "video", "image", "carousel"
    val thumbnailUrl: String,
    val mediaItems: List<MediaItemDetail>
) {
    val isCarousel: Boolean get() = mediaType.equals("carousel", ignoreCase = true) || mediaItems.size > 1
    val isSingleVideo: Boolean get() = mediaType.equals("video", ignoreCase = true) && mediaItems.size <= 1
}
