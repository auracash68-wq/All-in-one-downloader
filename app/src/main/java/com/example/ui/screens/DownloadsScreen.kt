package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.DownloadEntity
import com.example.ui.theme.AppBackground
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardSurface
import com.example.ui.theme.ChipInactiveBg
import com.example.ui.theme.ChipInactiveText
import com.example.ui.theme.DividerColor
import com.example.ui.theme.MintGreenPill
import com.example.ui.theme.MintGreenPillDarkText
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainViewModel

@Composable
fun DownloadsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val filteredDownloads by viewModel.filteredDownloads.collectAsState()
    val allDownloads by viewModel.allDownloads.collectAsState()
    val selectedFilter by viewModel.filterMediaType.collectAsState()

    val fileCountText = "${filteredDownloads.size} files"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header Row: "Downloads" title + count badge on right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Downloads",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.testTag("downloads_screen_title")
                )

                // Pill Badge on right e.g. "3 files"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFE8ECE9))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = fileCountText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4B5563)
                    )
                }
            }

            // Filter Tabs: "Video" (active pill) and "Audio"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                // Video Pill
                val isVideo = selectedFilter == "VIDEO"
                val videoBg = if (isVideo) MintGreenPill else ChipInactiveBg
                val videoTextColor = if (isVideo) MintGreenPillDarkText else ChipInactiveText

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(videoBg)
                        .clickable { viewModel.setFilterMediaType("VIDEO") }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("filter_video"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Videocam,
                        contentDescription = "Video",
                        tint = videoTextColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Video",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = videoTextColor
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Audio Pill
                val isAudio = selectedFilter == "AUDIO"
                val audioBg = if (isAudio) MintGreenPill else ChipInactiveBg
                val audioTextColor = if (isAudio) MintGreenPillDarkText else ChipInactiveText

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(audioBg)
                        .clickable { viewModel.setFilterMediaType("AUDIO") }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("filter_audio"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MusicNote,
                        contentDescription = "Audio",
                        tint = audioTextColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Audio",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = audioTextColor
                    )
                }
            }

            // Downloads List Container Card
            if (filteredDownloads.isEmpty()) {
                // Empty state
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(CardBorder)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp, horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (selectedFilter == "VIDEO") Icons.Outlined.Videocam else Icons.Outlined.MusicNote,
                            contentDescription = "No files",
                            tint = TextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No $selectedFilter downloads yet",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Paste a video link in the Download tab to save offline",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(CardBorder)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("downloads_card_list")
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        filteredDownloads.forEachIndexed { index, download ->
                            DownloadListItemRow(
                                item = download,
                                onClick = { viewModel.playVideo(download) },
                                onDelete = { viewModel.deleteDownload(download) },
                                onExport = {
                                    viewModel.exportToGallery(download) { success ->
                                        val msg = if (success) "Exported to Gallery successfully" else "Failed to export"
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onShare = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = if (download.mediaType == "VIDEO") "video/*" else "audio/*"
                                        putExtra(Intent.EXTRA_SUBJECT, download.title)
                                        putExtra(Intent.EXTRA_TEXT, "Check out this downloaded video: ${download.fileName}")
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share with"))
                                }
                            )

                            if (index < filteredDownloads.size - 1) {
                                HorizontalDivider(
                                    thickness = 0.8.dp,
                                    color = DividerColor,
                                    modifier = Modifier.padding(horizontal = 14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadListItemRow(
    item: DownloadEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    // Match thumbnail to item title or mock id
    val thumbnailRes = when {
        item.fileName.contains("Drone", ignoreCase = true) || item.title.contains("Drone", ignoreCase = true) -> R.drawable.thumb_drone
        item.fileName.contains("Tutorial", ignoreCase = true) || item.title.contains("Tutorial", ignoreCase = true) -> R.drawable.thumb_tutorial
        item.fileName.contains("Motion", ignoreCase = true) || item.title.contains("Motion", ignoreCase = true) -> R.drawable.thumb_motion
        else -> R.drawable.thumb_nature
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("download_row_${item.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail with duration overlay badge
        Box(
            modifier = Modifier
                .size(width = 68.dp, height = 48.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            Image(
                painter = painterResource(id = thumbnailRes),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Duration Pill Overlay at bottom right
            if (item.duration.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(3.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = item.duration,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Title and Subtitle (size • relative date)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(3.dp))
            val subtitle = "${item.formattedSize} • ${item.relativeDate}"
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = TextSecondary
            )
        }

        // 3-dots Menu Button
        Box {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.background(CardSurface)
            ) {
                DropdownMenuItem(
                    text = { Text("Play in Player", color = TextPrimary) },
                    leadingIcon = {
                        Icon(Icons.Outlined.PlayArrow, contentDescription = null, tint = TextPrimary)
                    },
                    onClick = {
                        menuExpanded = false
                        onClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Export to Gallery", color = TextPrimary) },
                    leadingIcon = {
                        Icon(Icons.Outlined.SaveAlt, contentDescription = null, tint = TextPrimary)
                    },
                    onClick = {
                        menuExpanded = false
                        onExport()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Share", color = TextPrimary) },
                    leadingIcon = {
                        Icon(Icons.Outlined.IosShare, contentDescription = null, tint = TextPrimary)
                    },
                    onClick = {
                        menuExpanded = false
                        onShare()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = Color(0xFFDC2626)) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Delete, contentDescription = null, tint = Color(0xFFDC2626))
                    },
                    onClick = {
                        menuExpanded = false
                        onDelete()
                    }
                )
            }
        }
    }
}
