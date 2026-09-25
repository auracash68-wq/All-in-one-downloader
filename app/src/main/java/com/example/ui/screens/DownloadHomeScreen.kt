package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.WarningAmber
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.DownloadEntity
import com.example.engine.VideoFormatInfo
import com.example.engine.VideoMetadata
import com.example.ui.theme.AppBackground
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardSurface
import com.example.ui.theme.MintGreenLight
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
    val videoPreview by viewModel.videoPreview.collectAsState()
    val formatDialogMetadata by viewModel.formatDialogMetadata.collectAsState()
    val isLoadingFormats by viewModel.isLoadingFormats.collectAsState()

    // Real-time RAM & Network Speed (Features 1 & 2)
    val ramState by viewModel.ramState.collectAsState()
    val networkSpeedState by viewModel.networkSpeedState.collectAsState()

    // Dialog states (Features 3, 4, 5, 6, 7)
    val showMultipleUrlDialog by viewModel.showMultipleUrlDialog.collectAsState()
    val showLowMemoryDialog by viewModel.showLowMemoryDialog.collectAsState()

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
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
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Features 1 & 2: Real-time RAM & Internet Speed Indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // RAM Status Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(CardBorder))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(ramState.statusLevel.color)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = "RAM", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                            Text(text = ramState.displayText, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }
                }

                // Internet Speed Status Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(CardBorder))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(networkSpeedState.statusLevel.color)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = "Internet Speed", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                            Text(text = networkSpeedState.displayText, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }
                }
            }

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

            // Feature 4: "Paste Multiple Link" button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MintGreenLight)
                        .clickable { viewModel.openMultipleUrlDialog() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("paste_multiple_link_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlaylistAdd,
                        contentDescription = "Paste Multiple Link",
                        tint = MintGreenPillDarkText,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Paste Multiple Link",
                        color = MintGreenPillDarkText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

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
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
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

            Spacer(modifier = Modifier.height(28.dp))

            // Real Active Download / Preview / Completed Success Card
            val isDownloading = activeDownload != null && activeDownload!!.isRunning
            val isCompleted = activeDownload != null && activeDownload!!.isCompleted
            val isFailed = activeDownload != null && activeDownload!!.isFailed
            val hasPreview = videoPreview != null && activeDownload == null

            AnimatedVisibility(
                visible = isDownloading || isCompleted || isFailed || hasPreview,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val currentTitle = when {
                    activeDownload != null -> activeDownload!!.title.ifEmpty { activeDownload!!.fileName }
                    videoPreview != null -> videoPreview!!.title
                    else -> ""
                }

                val currentThumb = when {
                    activeDownload != null -> activeDownload!!.thumbnailUrl
                    videoPreview != null -> videoPreview!!.thumbnailUrl
                    else -> null
                }

                val currentDuration = when {
                    activeDownload != null -> activeDownload!!.duration
                    videoPreview != null -> videoPreview!!.duration
                    else -> ""
                }

                val progressInt = activeDownload?.progress ?: 0
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
                            // Large Original Thumbnail (16:9 / clear fit with duration badge)
                            Box(
                                modifier = Modifier
                                    .size(width = 84.dp, height = 58.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF222222)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!currentThumb.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = currentThumb,
                                        contentDescription = "Original Thumbnail",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (selectedFormatTab == "MP3 Audio") Icons.Outlined.MusicNote else Icons.Outlined.Movie,
                                        contentDescription = "Media Icon",
                                        tint = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                if (currentDuration.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(3.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.Black.copy(alpha = 0.8f))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = currentDuration,
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            // Filename & Status Info
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentTitle,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                when {
                                    isCompleted -> {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Filled.CheckCircle,
                                                contentDescription = "Completed",
                                                tint = PrimaryGreen,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Download complete • ${activeDownload?.formattedSize}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = PrimaryGreen
                                            )
                                        }
                                    }
                                    isFailed -> {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Filled.ErrorOutline,
                                                contentDescription = "Failed",
                                                tint = Color(0xFFDC2626),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Download failed",
                                                fontSize = 13.sp,
                                                color = Color(0xFFDC2626)
                                            )
                                        }
                                    }
                                    isDownloading -> {
                                        val spd = activeDownload?.speed.orEmpty()
                                        val queueInfo = if ((activeDownload?.queueTotal ?: 0) > 1) {
                                            "Queue ${activeDownload?.queueIndex}/${activeDownload?.queueTotal} • "
                                        } else ""
                                        val statusStr = if (spd.isNotEmpty()) {
                                            "${queueInfo}Downloading • $progressInt% ($spd)"
                                        } else {
                                            "${queueInfo}Downloading • $progressInt%"
                                        }
                                        Text(
                                            text = statusStr,
                                            fontSize = 13.sp,
                                            color = TextSecondary
                                        )
                                    }
                                    hasPreview -> {
                                        Text(
                                            text = "Ready to download • ${videoPreview?.duration}",
                                            fontSize = 13.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Action Button: Play for completed or Close/Cancel (X)
                            if (isCompleted) {
                                IconButton(
                                    onClick = {
                                        activeDownload?.let { dl ->
                                            viewModel.playVideo(
                                                DownloadEntity(
                                                    id = dl.id,
                                                    title = dl.title,
                                                    fileName = dl.fileName,
                                                    filePath = dl.filePath,
                                                    thumbnailUri = dl.thumbnailUrl,
                                                    duration = dl.duration,
                                                    mediaType = dl.mediaType
                                                )
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryGreen)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { viewModel.dismissActiveCard() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = {
                                        if (isDownloading) {
                                            viewModel.cancelActiveDownload()
                                        } else if (isFailed) {
                                            viewModel.dismissActiveCard()
                                        } else {
                                            viewModel.clearPreview()
                                        }
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cancel",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        if (isDownloading) {
                            Spacer(modifier = Modifier.height(14.dp))

                            // Real Progress Bar
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
        }

        // Single Download Quality Selection Dialog
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

        // Features 4, 5, 6, 7: Multiple URL Dialog
        if (showMultipleUrlDialog) {
            MultipleUrlDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.closeMultipleUrlDialog() }
            )
        }

        // Feature 3: Low Memory Safety Dialog
        if (showLowMemoryDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissLowMemoryDialog() },
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = "Low Memory Warning",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        text = "Low available memory",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                },
                text = {
                    Text(
                        text = "Your device currently has limited available memory. For a smoother download, please close unnecessary background apps or select a lower video quality.",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        lineHeight = 20.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.chooseLowerQualityFromWarning() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Choose Lower Quality", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.continueDownloadDespiteMemoryWarning() }) {
                        Text("Continue Anyway", color = TextSecondary)
                    }
                },
                shape = RoundedCornerShape(16.dp),
                containerColor = CardSurface
            )
        }
    }
}

/**
 * Features 5, 6, 7: Multiple URL Dialog
 */
@Composable
fun MultipleUrlDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val urls by viewModel.multipleUrls.collectAsState()
    val formatType by viewModel.multipleFormatType.collectAsState()
    val qualityPreset by viewModel.multipleQualityPreset.collectAsState()
    val duplicateIndices = remember(urls) { viewModel.getDuplicateIndices() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Paste Multiple Links",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimary
                )
                Text(
                    text = "${urls.size}/12 links",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Add between 2 and 12 video or audio links to download sequentially in the background.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                // Format Selector inside Dialog (Video / Audio)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFEBF1EB))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isVideo = formatType == "MP4 Video"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isVideo) PrimaryGreenDark else Color.Transparent)
                            .clickable { viewModel.setMultipleFormatType("MP4 Video") }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "MP4 Video",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isVideo) Color.White else TextPrimary
                        )
                    }

                    val isAudio = formatType == "MP3 Audio"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isAudio) PrimaryGreenDark else Color.Transparent)
                            .clickable { viewModel.setMultipleFormatType("MP3 Audio") }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "MP3 Audio",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isAudio) Color.White else TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quality Selector (Feature 7)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val highLabel = if (formatType == "MP4 Video") "High (720p max)" else "High Quality"
                    val lowLabel = if (formatType == "MP4 Video") "Low (144p - 360p)" else "Standard Quality"

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                1.dp,
                                if (qualityPreset == "HIGH") PrimaryGreen else CardBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { viewModel.setMultipleQualityPreset("HIGH") }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = qualityPreset == "HIGH",
                            onClick = { viewModel.setMultipleQualityPreset("HIGH") },
                            colors = RadioButtonDefaults.colors(selectedColor = PrimaryGreen),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = highLabel, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                1.dp,
                                if (qualityPreset == "LOW") PrimaryGreen else CardBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { viewModel.setMultipleQualityPreset("LOW") }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = qualityPreset == "LOW",
                            onClick = { viewModel.setMultipleQualityPreset("LOW") },
                            colors = RadioButtonDefaults.colors(selectedColor = PrimaryGreen),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = lowLabel, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // URL Input Rows (Min 2, Max 12)
                urls.forEachIndexed { index, urlText ->
                    val isDuplicate = duplicateIndices.contains(index)
                    Column(modifier = Modifier.padding(bottom = 10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF6F8F6))
                                .border(
                                    1.dp,
                                    if (isDuplicate) Color(0xFFEF4444) else CardBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}.",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                modifier = Modifier.width(22.dp)
                            )

                            BasicTextField(
                                value = urlText,
                                onValueChange = { viewModel.updateMultipleUrl(index, it) },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = TextPrimary,
                                    fontSize = 13.sp
                                ),
                                cursorBrush = SolidColor(PrimaryGreen),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("multiple_url_input_$index")
                            )

                            if (urlText.isEmpty()) {
                                IconButton(
                                    onClick = {
                                        val clip = clipboardManager.getText()?.text
                                        if (!clip.isNullOrBlank()) {
                                            viewModel.updateMultipleUrl(index, clip.trim())
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ContentPaste,
                                        contentDescription = "Paste",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            if (urls.size > 2) {
                                IconButton(
                                    onClick = { viewModel.removeMultipleUrlRow(index) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = "Remove",
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        if (isDuplicate) {
                            Text(
                                text = "Duplicate URL detected",
                                color = Color(0xFFEF4444),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(start = 26.dp, top = 2.dp)
                            )
                        }
                    }
                }

                // Add Link Button (if count < 12)
                if (urls.size < 12) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.addMultipleUrlRow() }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Link",
                            tint = PrimaryGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Add another link",
                            color = PrimaryGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        confirmButton = {
            val hasMinUrls = urls.count { it.isNotBlank() } >= 2
            val hasDuplicates = duplicateIndices.isNotEmpty()
            Button(
                onClick = { viewModel.confirmMultipleDownload(context) },
                enabled = hasMinUrls && !hasDuplicates,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen,
                    disabledContainerColor = Color(0xFFD1D5DB)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Continue Download", color = Color.White)
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

@Composable
fun FormatSelectionDialog(
    metadata: VideoMetadata,
    isAudio: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (VideoFormatInfo) -> Unit
) {
    val formats = if (isAudio) {
        if (metadata.audioFormats.isNotEmpty()) metadata.audioFormats else metadata.formats
    } else {
        if (metadata.videoFormats.isNotEmpty()) metadata.videoFormats else metadata.formats
    }

    var selectedFormat by remember(metadata, isAudio) {
        mutableStateOf(
            if (isAudio) {
                formats.find { it.formatId.contains("192") } ?: formats.firstOrNull() ?: VideoFormatInfo("192kbps", "192 kbps (Standard Quality)", "Recommended • MP3", "mp3", isAudio = true)
            } else {
                formats.find { it.resolution.contains("720") } ?: formats.firstOrNull() ?: VideoFormatInfo("720p", "720p (HD)", ext = "mp4")
            }
        )
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
                    val isChecked = format.formatId == selectedFormat.formatId && format.resolution == selectedFormat.resolution
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
                            val details = listOfNotNull(
                                format.note.takeIf { it.isNotBlank() },
                                format.filesizeApprox.takeIf { it.isNotBlank() }
                            ).joinToString(" • ")
                            if (details.isNotEmpty()) {
                                Text(
                                    text = details,
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
