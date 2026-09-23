package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.engine.VideoFormatInfo
import com.example.ui.theme.AppBackground
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardSurface
import com.example.ui.theme.MintGreenLight
import com.example.ui.theme.MintGreenPill
import com.example.ui.theme.MintGreenPillDarkText
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.PrimaryGreenDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainViewModel

@Composable
fun DownloadHomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val urlInput by viewModel.urlInput.collectAsState()
    val selectedFormatTab by viewModel.selectedFormatTab.collectAsState()
    val activeDownload by viewModel.activeDownload.collectAsState()
    val formatDialogMetadata by viewModel.formatDialogMetadata.collectAsState()
    val isLoadingFormats by viewModel.isLoadingFormats.collectAsState()

    // Flag for initial demo active card matching Image 4
    var showMockActiveDownload by remember { mutableStateOf(true) }

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Top App Brand Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_streamclean_logo),
                    contentDescription = "StreamClean Logo",
                    modifier = Modifier.size(34.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "StreamClean",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            // Screen Title & Subtitle
            Text(
                text = "Download",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.testTag("download_screen_title")
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Paste a video link to download",
                fontSize = 14.sp,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // URL Input Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CardSurface)
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Link,
                        contentDescription = "Link Icon",
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        if (urlInput.isEmpty()) {
                            Text(
                                text = "Paste video link",
                                color = TextMuted,
                                fontSize = 15.sp
                            )
                        }
                        BasicTextField(
                            value = urlInput,
                            onValueChange = { viewModel.onUrlChanged(it) },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = TextPrimary,
                                fontSize = 15.sp
                            ),
                            cursorBrush = SolidColor(PrimaryGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("url_input_field")
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Paste Button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MintGreenLight)
                            .clickable { viewModel.pasteFromClipboard(context) }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                            .testTag("paste_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentPaste,
                            contentDescription = "Paste",
                            tint = MintGreenPillDarkText,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Paste",
                            color = MintGreenPillDarkText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Format Selection Pill Container (MP4 Video / MP3 Audio)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFEBF1EB))
                    .padding(4.dp)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // MP4 Video Tab
                    val isVideoSelected = selectedFormatTab == "MP4 Video"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (isVideoSelected) PrimaryGreenDark else Color.Transparent)
                            .clickable { viewModel.selectFormatTab("MP4 Video") }
                            .testTag("tab_mp4_video"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Movie,
                                contentDescription = "MP4 Video",
                                tint = if (isVideoSelected) Color.White else TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "MP4 Video",
                                color = if (isVideoSelected) Color.White else TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // MP3 Audio Tab
                    val isAudioSelected = selectedFormatTab == "MP3 Audio"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (isAudioSelected) PrimaryGreenDark else Color.Transparent)
                            .clickable { viewModel.selectFormatTab("MP3 Audio") }
                            .testTag("tab_mp3_audio"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.MusicNote,
                                contentDescription = "MP3 Audio",
                                tint = if (isAudioSelected) Color.White else TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "MP3 Audio",
                                color = if (isAudioSelected) Color.White else TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Large Download Button
            Button(
                onClick = { viewModel.checkAndInitiateDownload(context) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("start_download_button")
            ) {
                if (isLoadingFormats) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Analyzing formats...",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.FileDownload,
                        contentDescription = "Download",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Download",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Active Download Card (Matching Image 4)
            // Displays real download state if active, or initial mockup demo matching Image 4
            val isDownloading = activeDownload != null && activeDownload!!.isRunning
            val hasActiveCard = isDownloading || showMockActiveDownload

            AnimatedVisibility(
                visible = hasActiveCard,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val filename = if (isDownloading) {
                    activeDownload?.fileName ?: "downloading_video.mp4"
                } else {
                    "nature_documentary_4k.mp4"
                }

                val progressInt = if (isDownloading) {
                    activeDownload?.progress ?: 0
                } else {
                    42
                }

                val progressFloat = progressInt / 100f

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(CardBorder)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("active_download_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Thumbnail
                            Image(
                                painter = painterResource(id = R.drawable.thumb_nature),
                                contentDescription = "Video Thumbnail",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            // Filename & Status
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = filename,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "Downloading • $progressInt%",
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                            }

                            // Close / Cancel (X) button
                            IconButton(
                                onClick = {
                                    if (isDownloading) {
                                        viewModel.cancelActiveDownload()
                                    } else {
                                        showMockActiveDownload = false
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel Download",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Progress Bar (Green indicator, light mint track)
                        LinearProgressIndicator(
                            progress = { progressFloat },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.5.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = PrimaryGreen,
                            trackColor = MintGreenLight,
                        )
                    }
                }
            }
        }

        // Quality Selection Dialog (fetching formats using yt-dlp -F)
        formatDialogMetadata?.let { metadata ->
            FormatSelectionDialog(
                metadata = metadata,
                isAudio = selectedFormatTab == "MP3 Audio",
                onDismiss = { viewModel.dismissFormatDialog() },
                onConfirm = { selectedFormat ->
                    viewModel.confirmDownload(selectedFormat)
                }
            )
        }
    }
}

@Composable
fun FormatSelectionDialog(
    metadata: com.example.engine.VideoMetadata,
    isAudio: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (VideoFormatInfo) -> Unit
) {
    val formats = metadata.formats
    var selectedFormat by remember {
        mutableStateOf(formats.find { it.formatId == "720p" } ?: formats.firstOrNull() ?: VideoFormatInfo("720p", "720p (HD)"))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = if (isAudio) "Select Audio Bitrate" else "Select Video Quality",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = metadata.title,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    maxLines = 2
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                formats.forEach { format ->
                    val isChecked = format.formatId == selectedFormat.formatId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedFormat = format }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isChecked,
                            onClick = { selectedFormat = format },
                            colors = RadioButtonDefaults.colors(selectedColor = PrimaryGreen)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = format.resolution,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                            if (format.note.isNotEmpty() || format.filesizeApprox.isNotEmpty()) {
                                Text(
                                    text = "${format.note} • ${format.filesizeApprox}",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedFormat) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Start Download", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = CardSurface
    )
}
