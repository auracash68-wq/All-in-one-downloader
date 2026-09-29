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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.BrightnessMedium
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Translate
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
import com.example.R
import com.example.data.preferences.SettingsManager
import com.example.ui.components.ScreenBackground
import com.example.ui.localization.LocalAppStrings
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
import dev.chrisbanes.haze.HazeState

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val appearance by viewModel.appearance.collectAsState()
    val language by viewModel.language.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()

    var showAboutDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showAppearanceDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    val appearanceDisplay = when (appearance) {
        "Light" -> strings.lightOption
        "Dark" -> strings.darkOption
        else -> strings.systemDefaultOption
    }

    ScreenBackground(
        backgroundResId = R.drawable.settings_background,
        modifier = modifier,
        hazeState = hazeState
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .padding(bottom = 80.dp)
        ) {
            // Header: "Settings" + Subtitle
            Text(
                text = strings.settingsTitle,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.testTag("settings_screen_title")
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = strings.settingsSubtitle,
                fontSize = 14.sp,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // Section 1: PREFERENCES / Interface
            SectionHeader(title = strings.preferencesSection, rightLabel = strings.interfaceLabel)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(CardBorder)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Appearance (System default / Light / Dark)
                    SettingsItemRow(
                        icon = Icons.Outlined.BrightnessMedium,
                        title = strings.appearanceItem,
                        subtitle = appearanceDisplay,
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

                    // Language Switcher (Only English, বাংলা, हिन्दी)
                    val langCode = SettingsManager.getLanguageCodeDisplay(language)
                    SettingsItemRow(
                        icon = Icons.Outlined.Translate,
                        title = strings.languageItem,
                        subtitle = language,
                        onClick = { showLanguageDialog = true },
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = langCode,
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

                    // Notifications (Download completion alerts)
                    SettingsItemRow(
                        icon = Icons.Outlined.Notifications,
                        title = strings.notificationsItem,
                        subtitle = strings.notificationsSubtitle,
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

            // Section 2: ABOUT & LEGAL / Verified Build
            SectionHeader(title = strings.aboutSection, rightLabel = strings.verifiedBuildLabel)
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
                        title = strings.aboutItem,
                        subtitle = strings.aboutVersionSubtitle,
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
                                        text = strings.latestBadge,
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
                        title = strings.privacyPolicyItem,
                        subtitle = strings.privacyPolicySubtitle,
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

        // About Dialog - Exactly matching requirements
        if (showAboutDialog) {
            AlertDialog(
                onDismissRequest = { showAboutDialog = false },
                title = { Text(strings.aboutDialogTitle, fontWeight = FontWeight.Bold, color = TextPrimary) },
                text = {
                    Column {
                        Text(
                            text = strings.aboutVersionText,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = PrimaryGreen
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = strings.aboutDescription,
                            fontSize = 13.sp,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = strings.aboutBullet1,
                            fontSize = 13.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = strings.aboutBullet2,
                            fontSize = 13.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = strings.aboutBullet3,
                            fontSize = 13.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = strings.aboutCopyright,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAboutDialog = false }) {
                        Text(strings.okButton, color = PrimaryGreen, fontWeight = FontWeight.SemiBold)
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
                title = { Text(strings.privacyDialogTitle, fontWeight = FontWeight.Bold, color = TextPrimary) },
                text = {
                    Column {
                        Text(
                            text = strings.privacyDialogText,
                            fontSize = 13.sp,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPrivacyDialog = false }) {
                        Text(strings.closeButton, color = PrimaryGreen, fontWeight = FontWeight.SemiBold)
                    }
                },
                shape = RoundedCornerShape(16.dp),
                containerColor = CardSurface
            )
        }

        // Appearance Dialog (System default / Light / Dark)
        if (showAppearanceDialog) {
            val options = listOf(
                "System default" to strings.systemDefaultOption,
                "Light" to strings.lightOption,
                "Dark" to strings.darkOption
            )
            AlertDialog(
                onDismissRequest = { showAppearanceDialog = false },
                title = { Text(strings.chooseAppearanceTitle, fontWeight = FontWeight.Bold, color = TextPrimary) },
                text = {
                    Column {
                        options.forEach { (optKey, optLabel) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setAppearance(optKey)
                                        showAppearanceDialog = false
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = optKey == appearance,
                                    onClick = {
                                        viewModel.setAppearance(optKey)
                                        showAppearanceDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = PrimaryGreen)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(optLabel, fontSize = 15.sp, color = TextPrimary)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showAppearanceDialog = false }) {
                        Text(strings.cancelButton, color = TextSecondary)
                    }
                },
                shape = RoundedCornerShape(16.dp),
                containerColor = CardSurface
            )
        }

        // Language Switcher Dialog - STRICTLY ONLY English, বাংলা, हिन्दी
        if (showLanguageDialog) {
            val languages = SettingsManager.SUPPORTED_LANGUAGES
            AlertDialog(
                onDismissRequest = { showLanguageDialog = false },
                title = { Text(strings.selectLanguageTitle, fontWeight = FontWeight.Bold, color = TextPrimary) },
                text = {
                    Column {
                        languages.forEach { lang ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setLanguage(lang)
                                        showLanguageDialog = false
                                        val toastMsg = strings.languageChangedToast(lang)
                                        Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = lang == language,
                                    onClick = {
                                        viewModel.setLanguage(lang)
                                        showLanguageDialog = false
                                        val toastMsg = strings.languageChangedToast(lang)
                                        Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = PrimaryGreen)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(lang, fontSize = 15.sp, color = TextPrimary)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showLanguageDialog = false }) {
                        Text(strings.cancelButton, color = TextSecondary)
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
