package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.InstagramMediaMetadata
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkGreyCard
import com.example.ui.theme.DarkGreySurface
import com.example.ui.theme.DarkPillHighlight
import com.example.ui.theme.DownloadSuccessGreen
import com.example.ui.theme.PureBlack
import com.example.ui.theme.TextPlaceholder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.VibrationHelper

@Composable
fun MediaPreviewCard(
    media: InstagramMediaMetadata,
    downloadedItemIds: Set<String>,
    isDownloading: Boolean,
    downloadProgress: Float,
    onDownload: () -> Unit,
    onOpenFullscreen: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { media.mediaItems.size })
    val currentSlideIndex = pagerState.currentPage
    val isAllDownloaded = media.mediaItems.all { downloadedItemIds.contains(it.id) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF141519))
            .border(1.dp, Color(0xFF22242B), RoundedCornerShape(14.dp)),
        color = Color(0xFF141519)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Author row: Avatar + @handle + format badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF20232B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccountCircle,
                            contentDescription = null,
                            tint = Color(0xFFA0A6B5),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "@${media.author}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFFFFF),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "Verified",
                        tint = Color(0xFF3897F0),
                        modifier = Modifier.size(13.dp)
                    )
                }

                // Clean pill badge (e.g. "REEL" or "CAROUSEL 4")
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF1E2129),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2E39))
                ) {
                    Text(
                        text = if (media.isCarousel) "${media.mediaItems.size} SLIDES" else media.mediaType.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA6ABB8),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Media Preview Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PureBlack)
                    .border(1.dp, Color(0xFF22242B), RoundedCornerShape(10.dp))
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val item = media.mediaItems.getOrNull(page)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                VibrationHelper.click(context)
                                onOpenFullscreen(page)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(item?.thumbnail?.ifBlank { item.url })
                                .crossfade(true)
                                .build(),
                            contentDescription = "Media slide",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        if (item?.isVideo == true) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x99000000))
                                    .border(1.dp, Color(0x55FFFFFF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play Video",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }

                        // Gradient bottom scrim
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color(0xCC000000))
                                    )
                                )
                        )
                    }
                }

                // Page indicator badge (bottom right corner like YouTube/YTDLnis)
                if (media.mediaItems.size > 1) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xCC000000),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "${currentSlideIndex + 1} / ${media.mediaItems.size}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Dots indicator
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        repeat(media.mediaItems.size) { i ->
                            Box(
                                modifier = Modifier
                                    .size(if (i == currentSlideIndex) 6.dp else 4.dp)
                                    .clip(CircleShape)
                                    .background(if (i == currentSlideIndex) TextPrimary else Color(0x66FFFFFF))
                            )
                        }
                    }
                }

                // Fullscreen button (top right)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(0x99000000))
                        .clickable {
                            VibrationHelper.click(context)
                            onOpenFullscreen(currentSlideIndex)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Fullscreen",
                        tint = TextPrimary,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            // Caption clamp
            if (media.caption.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = media.caption,
                    fontSize = 13.sp,
                    color = Color(0xFF9AA0B0),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Download Progress Bar
            if (isDownloading) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Downloading...",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFA6ABB8)
                        )
                        Text(
                            text = "${(downloadProgress * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { downloadProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape),
                        color = Color(0xFFFFFFFF),
                        trackColor = Color(0xFF22242B)
                    )
                }
            }

            // Single Full-Width YTDLnis Download Button
            Button(
                onClick = {
                    VibrationHelper.click(context)
                    onDownload()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("download_all_button"),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAllDownloaded) Color(0xFF1E2129) else Color(0xFFFFFFFF),
                    contentColor = if (isAllDownloaded) DownloadSuccessGreen else PureBlack
                ),
                enabled = !isDownloading
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = PureBlack,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Downloading...",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureBlack
                    )
                } else if (isAllDownloaded) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = DownloadSuccessGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Saved in Gallery",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = DownloadSuccessGreen
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = PureBlack,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (media.mediaItems.size > 1) "Download All (${media.mediaItems.size})" else "Download",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureBlack
                    )
                }
            }
        }
    }
}
