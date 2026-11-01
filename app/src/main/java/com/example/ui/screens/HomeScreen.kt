package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.DownloadedMediaEntity
import com.example.model.InstagramMediaMetadata
import com.example.model.MediaItemDetail
import com.example.ui.components.FullscreenGalleryDialog
import com.example.ui.components.MediaPreviewCard
import com.example.ui.components.TopBarAndSearch
import com.example.ui.theme.PureBlack
import com.example.util.VibrationHelper
import java.io.File

@Composable
fun HomeScreen(
    urlInput: String,
    onUrlChanged: (String) -> Unit,
    onClearUrl: () -> Unit,
    detectedClipboardUrl: String?,
    onApplyClipboard: () -> Unit,
    onDismissClipboard: () -> Unit,
    isLoading: Boolean,
    statusMessage: String?,
    currentMedia: InstagramMediaMetadata?,
    downloadedItemIds: Set<String>,
    isDownloading: Boolean,
    downloadProgress: Float,
    downloads: List<DownloadedMediaEntity>,
    onDownload: () -> Unit,
    onDownloadSlide: (MediaItemDetail) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    modifier: Modifier = Modifier
) {
    var fullscreenPageIndex by remember { mutableIntStateOf(-1) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // Header & Search Bar
            item {
                TopBarAndSearch(
                    urlInput = urlInput,
                    onUrlChanged = onUrlChanged,
                    onClearUrl = onClearUrl,
                    detectedClipboardUrl = detectedClipboardUrl,
                    onApplyClipboard = onApplyClipboard,
                    onDismissClipboard = onDismissClipboard,
                    onNavigateToSettings = onNavigateToSettings
                )
            }

            // Sleek YTDLnis-style skeleton loading card
            if (isLoading) {
                item {
                    YtdlnisLoadingSkeleton()
                }
            }

            // Error or status banner
            if (!isLoading && statusMessage != null) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, Color(0xFF262933), RoundedCornerShape(10.dp)),
                        color = Color(0xFF141519)
                    ) {
                        Text(
                            text = statusMessage,
                            fontSize = 13.sp,
                            color = Color(0xFF9AA0B0),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }

            // Pre-Download Media Card Preview
            if (currentMedia != null && !isLoading) {
                item {
                    MediaPreviewCard(
                        media = currentMedia,
                        downloadedItemIds = downloadedItemIds,
                        isDownloading = isDownloading,
                        downloadProgress = downloadProgress,
                        onDownload = onDownload,
                        onOpenFullscreen = { page -> fullscreenPageIndex = page },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            } else if (!isLoading && currentMedia == null) {
                // Minimalist initial empty state
                item {
                    MinimalistEmptyState()
                }
            }

            // Recent Downloads strip
            if (downloads.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 18.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Recent Downloads",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFFFFF)
                            )

                            Row(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { onNavigateToDownloads() }
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "All (${downloads.size})",
                                    fontSize = 12.sp,
                                    color = Color(0xFF8B90A0)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint = Color(0xFF8B90A0),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(downloads.take(6)) { item ->
                                RecentDownloadCard(item = item)
                            }
                        }
                    }
                }
            }
        }

        // Fullscreen Gallery Modal
        if (fullscreenPageIndex >= 0 && currentMedia != null) {
            FullscreenGalleryDialog(
                media = currentMedia,
                initialIndex = fullscreenPageIndex,
                onDismiss = { fullscreenPageIndex = -1 },
                onDownloadSlide = onDownloadSlide
            )
        }
    }
}

@Composable
fun YtdlnisLoadingSkeleton() {
    val transition = rememberInfiniteTransition(label = "skeleton_pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, Color(0xFF22242B), RoundedCornerShape(14.dp)),
        color = Color(0xFF141519)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Skeleton Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF20232B).copy(alpha = alpha))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF20232B).copy(alpha = alpha))
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Skeleton Media Square
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1B1C22).copy(alpha = alpha)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = Color(0xFFFFFFFF),
                    strokeWidth = 2.5.dp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Skeleton Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF20232B).copy(alpha = alpha))
            )
        }
    }
}

@Composable
fun MinimalistEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF141519))
                    .border(1.dp, Color(0xFF22242B), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    tint = Color(0xFF8B90A0),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Paste an Instagram Link",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFFFFF)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Reels, carousels, and photos download in full quality",
                fontSize = 12.sp,
                color = Color(0xFF7A7F8E)
            )
        }
    }
}

@Composable
fun RecentDownloadCard(item: DownloadedMediaEntity) {
    val context = LocalContext.current

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF141519),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF22242B)),
        modifier = Modifier
            .width(120.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable {
                VibrationHelper.click(context)
                com.example.util.MediaUtils.openMediaFile(context, item)
            }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(PureBlack)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(if (File(item.localFilePath).exists()) File(item.localFilePath) else item.thumbnailUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.fileName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                if (item.isVideo) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(5.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0x99000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Video",
                            tint = Color(0xFFFFFFFF),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(7.dp)) {
                Text(
                    text = "@${item.author}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFFFFF),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = com.example.util.MediaUtils.formatFileSize(item.fileSizeBytes),
                    fontSize = 10.sp,
                    color = Color(0xFF7A7F8E)
                )
            }
        }
    }
}
