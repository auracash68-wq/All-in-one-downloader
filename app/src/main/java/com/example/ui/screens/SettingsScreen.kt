package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.BrightnessMedium
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppBackground
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardSurface
import com.example.ui.theme.DividerColor
import com.example.ui.theme.IconCircleBg
import com.example.ui.theme.IconTintGreen
import com.example.ui.theme.MintBadgeBg
import com.example.ui.theme.MintBadgeText
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val downloadLocation by viewModel.downloadLocation.collectAsState()
    val wifiOnly by viewModel.wifiOnly.collectAsState()
    val appearance by viewModel.appearance.collectAsState()
    val language by viewModel.language.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()

    var showAboutDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showAppearanceDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

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
            // Header: "Settings" + Subtitle
            Text(
                text = "Settings",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.testTag("settings_screen_title")
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "StreamClean preferences & storage configuration",
                fontSize = 14.sp,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Section 1: DOWNLOADS / Storage & Data
            SectionHeader(title = "DOWNLOADS", rightLabel = "Storage & Data")
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(CardBorder)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Download location
                    SettingsItemRow(
                        icon = Icons.Outlined.Folder,
                        title = "Download location",
                        subtitle = downloadLocation,
                        onClick = {
                            Toast.makeText(context, "Location: $downloadLocation", Toast.LENGTH_SHORT).show()
                        },
                        trailing = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )

                    HorizontalDivider(
                        thickness = 0.8.dp,
                        color = DividerColor,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    // Download over Wi-Fi only
                    SettingsItemRow(
                        icon = Icons.Outlined.Wifi,
                        title = "Download over Wi-Fi only",
                        subtitle = "Conserve cellular network data",
                        trailing = {
                            Switch(
                                checked = wifiOnly,
                                onCheckedChange = { viewModel.setWifiOnly(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = PrimaryGreen,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color(0xFFD1D5DB)
                                ),
                                modifier = Modifier.testTag("switch_wifi_only")
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 2: PREFERENCES / Interface
            SectionHeader(title = "PREFERENCES", rightLabel = "Interface")
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(CardBorder)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Appearance
                    SettingsItemRow(
                        icon = Icons.Outlined.BrightnessMedium,
                        title = "Appearance",
                        subtitle = appearance,
                        onClick = { showAppearanceDialog = true },
                        trailing = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )

                    HorizontalDivider(
                        thickness = 0.8.dp,
                        color = DividerColor,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    // Language
                    SettingsItemRow(
                        icon = Icons.Outlined.Translate,
                        title = "Language",
                        subtitle = language,
                        onClick = { showLanguageDialog = true },
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "EN",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF15803D),
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = Color(0xFF9CA3AF),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    )

                    HorizontalDivider(
                        thickness = 0.8.dp,
                        color = DividerColor,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    // Notifications
                    SettingsItemRow(
                        icon = Icons.Outlined.Notifications,
                        title = "Notifications",
                        subtitle = "Download completion alerts",
                        trailing = {
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { viewModel.setNotificationsEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = PrimaryGreen,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color(0xFFD1D5DB)
                                ),
                                modifier = Modifier.testTag("switch_notifications")
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 3: ABOUT & LEGAL / Verified Build
            SectionHeader(title = "ABOUT & LEGAL", rightLabel = "Verified Build")
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(CardBorder)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // About StreamClean
                    SettingsItemRow(
                        icon = Icons.Outlined.Info,
                        title = "About StreamClean",
                        subtitle = "Version 2.4.0 (Build 2408)",
                        onClick = { showAboutDialog = true },
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MintBadgeBg)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "Latest",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MintBadgeText
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = Color(0xFF9CA3AF),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    )

                    HorizontalDivider(
                        thickness = 0.8.dp,
                        color = DividerColor,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    // Privacy Policy
                    SettingsItemRow(
                        icon = Icons.Outlined.Security,
                        title = "Privacy Policy",
                        subtitle = "Local data security & zero tracking",
                        onClick = { showPrivacyDialog = true },
                        trailing = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }

        // About Dialog
        if (showAboutDialog) {
            AlertDialog(
                onDismissRequest = { showAboutDialog = false },
                title = { Text("About StreamClean", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("StreamClean v2.4.0 (Build 2408)")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "100% offline, local-processing media downloader & player. Bundles YoutubeDL, FFmpeg, and ExoPlayer for fast on-device conversions with single-download RAM management.",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAboutDialog = false }) {
                        Text("OK", color = PrimaryGreen)
                    }
                },
                shape = RoundedCornerShape(16.dp),
                containerColor = CardSurface
            )
        }

        // Privacy Dialog
        if (showPrivacyDialog) {
            AlertDialog(
                onDismissRequest = { showPrivacyDialog = false },
                title = { Text("Privacy Policy", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            "StreamClean is designed with zero cloud dependencies and zero tracking. All files and conversions are processed 100% locally on your device hardware.",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPrivacyDialog = false }) {
                        Text("Close", color = PrimaryGreen)
                    }
                },
                shape = RoundedCornerShape(16.dp),
                containerColor = CardSurface
            )
        }

        // Appearance Dialog
        if (showAppearanceDialog) {
            val options = listOf("System default", "Light", "Dark")
            AlertDialog(
                onDismissRequest = { showAppearanceDialog = false },
                title = { Text("Choose Appearance", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        options.forEach { opt ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setAppearance(opt)
                                        showAppearanceDialog = false
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = opt == appearance,
                                    onClick = {
                                        viewModel.setAppearance(opt)
                                        showAppearanceDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = PrimaryGreen)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(opt, fontSize = 15.sp)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showAppearanceDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                shape = RoundedCornerShape(16.dp),
                containerColor = CardSurface
            )
        }

        // Language Dialog
        if (showLanguageDialog) {
            val languages = listOf("English", "Español", "Français", "Deutsch", "日本語")
            AlertDialog(
                onDismissRequest = { showLanguageDialog = false },
                title = { Text("Select Language", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        languages.forEach { lang ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setLanguage(lang)
                                        showLanguageDialog = false
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = lang == language,
                                    onClick = {
                                        viewModel.setLanguage(lang)
                                        showLanguageDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = PrimaryGreen)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(lang, fontSize = 15.sp)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showLanguageDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                shape = RoundedCornerShape(16.dp),
                containerColor = CardSurface
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    rightLabel: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryGreen,
            letterSpacing = 0.5.sp
        )
        Text(
            text = rightLabel,
            fontSize = 12.sp,
            color = TextSecondary
        )
    }
}

@Composable
fun SettingsItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Soft green circular background icon
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(IconCircleBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = IconTintGreen,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Title and Subtitle
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextSecondary,
                maxLines = 1
            )
        }

        trailing()
    }
}
