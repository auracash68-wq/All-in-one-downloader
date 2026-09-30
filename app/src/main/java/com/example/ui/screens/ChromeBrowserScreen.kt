package com.example.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.http.SslError
import android.util.Log
import android.view.ViewGroup
import android.webkit.GeolocationPermissions
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.ui.components.AppPremiumBackgroundCanvas
import com.example.ui.localization.LocalAppStrings
import com.example.ui.theme.LocalStreamCleanDark
import com.example.ui.theme.AppBackground
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardSurface
import com.example.ui.theme.MintGreenLight
import com.example.ui.theme.MintGreenPillDarkText
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ChromeBrowserScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    initialUrl: String? = null,
    onBack: (() -> Unit)? = null,
    hazeState: HazeState? = null
) {
    val context = LocalContext.current
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf(initialUrl ?: "https://www.google.com") }
    var inputUrlText by remember { mutableStateOf(initialUrl ?: "https://www.google.com") }
    var pageTitle by remember { mutableStateOf("Google") }
    var pageProgress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(false) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    // Intercept hardware back button to navigate back in web history
    BackHandler(enabled = true) {
        if (canGoBack) {
            webViewInstance?.goBack()
        } else {
            onBack?.invoke()
        }
    }

    // Handle edge-to-edge system bars gracefully
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        window?.let { w ->
            val controller = WindowInsetsControllerCompat(w, view)
            controller.isAppearanceLightStatusBars = true
        }
        onDispose {}
    }

    fun navigateTo(queryOrUrl: String) {
        val trimmed = queryOrUrl.trim()
        val target = if (trimmed.startsWith("http://", ignoreCase = true)) {
            val lower = trimmed.lowercase()
            val isLocal = lower.startsWith("http://localhost") ||
                    lower.startsWith("http://127.0.0.1") ||
                    lower.startsWith("http://10.0.2.2")
            if (isLocal) trimmed else "https://" + trimmed.substring(7)
        } else if (trimmed.startsWith("https://", ignoreCase = true)) {
            trimmed
        } else if (trimmed.contains(".") && !trimmed.contains(" ")) {
            "https://$trimmed"
        } else {
            "https://www.google.com/search?q=" + java.net.URLEncoder.encode(trimmed, "UTF-8")
        }
        inputUrlText = target
        currentUrl = target
        webViewInstance?.loadUrl(target)
    }

    val boxModifier = if (hazeState != null) {
        modifier.fillMaxSize().hazeSource(state = hazeState)
    } else {
        modifier.fillMaxSize()
    }

    Box(
        modifier = boxModifier
    ) {
        AppPremiumBackgroundCanvas(isDark = LocalStreamCleanDark.current)
        Column(modifier = Modifier.fillMaxSize()) {
            // Chrome-Style Omnibox Top Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardSurface)
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Home Button
                    IconButton(
                        onClick = { navigateTo("https://www.google.com") },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Home,
                            contentDescription = "Browser Home",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Omnibox Address Input
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0xFFF1F5F2))
                            .border(1.dp, CardBorder, RoundedCornerShape(22.dp))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (currentUrl.startsWith("https://")) Icons.Outlined.Lock else Icons.Outlined.Search,
                                contentDescription = null,
                                tint = if (currentUrl.startsWith("https://")) PrimaryGreen else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))

                            BasicTextField(
                                value = inputUrlText,
                                onValueChange = { inputUrlText = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = TextPrimary,
                                    fontSize = 14.sp
                                ),
                                cursorBrush = SolidColor(PrimaryGreen),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Uri,
                                    imeAction = ImeAction.Go
                                ),
                                keyboardActions = KeyboardActions(
                                    onGo = { navigateTo(inputUrlText) }
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("browser_omnibox_input")
                            )

                            if (inputUrlText.isNotEmpty()) {
                                IconButton(
                                    onClick = { inputUrlText = "" },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Clear",
                                        tint = TextMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Reload / Stop Button
                    IconButton(
                        onClick = {
                            if (isLoading) {
                                webViewInstance?.stopLoading()
                            } else {
                                webViewInstance?.reload()
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isLoading) Icons.Outlined.Close else Icons.Outlined.Refresh,
                            contentDescription = if (isLoading) "Stop" else "Reload",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Slim Linear Page Progress Bar
            AnimatedVisibility(visible = isLoading) {
                LinearProgressIndicator(
                    progress = { pageProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp),
                    color = PrimaryGreen,
                    trackColor = Color.Transparent
                )
            }

            // WebView in AndroidView
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.databaseEnabled = true
                            settings.cacheMode = WebSettings.LOAD_DEFAULT
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.setSupportZoom(true)
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            settings.mediaPlaybackRequiresUserGesture = false
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            settings.allowFileAccess = false
                            settings.allowContentAccess = false
                            @Suppress("DEPRECATION")
                            settings.allowFileAccessFromFileURLs = false
                            @Suppress("DEPRECATION")
                            settings.allowUniversalAccessFromFileURLs = false
                            settings.setGeolocationEnabled(false)
                            settings.saveFormData = false

                            setDownloadListener { downloadUrl, _, _, mimetype, _ ->
                                if (downloadUrl.startsWith("https://") || downloadUrl.startsWith("http://")) {
                                    Log.i("ChromeBrowserScreen", "Direct download intercepted: $downloadUrl ($mimetype)")
                                    viewModel.onUrlChanged(downloadUrl)
                                    Toast.makeText(context, "URL sent to Downloader", Toast.LENGTH_SHORT).show()
                                    onBack?.invoke()
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    pageProgress = newProgress / 100f
                                    isLoading = newProgress in 1..99
                                }

                                override fun onReceivedTitle(view: WebView?, title: String?) {
                                    pageTitle = title ?: ""
                                }

                                override fun onGeolocationPermissionsShowPrompt(
                                    origin: String?,
                                    callback: GeolocationPermissions.Callback?
                                ) {
                                    callback?.invoke(origin, false, false)
                                }
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onReceivedSslError(
                                    view: WebView?,
                                    handler: SslErrorHandler?,
                                    error: SslError?
                                ) {
                                    Log.e("ChromeBrowserScreen", "SSL certificate error encountered: $error. Connection blocked.")
                                    handler?.cancel() // Never blindly proceed on certificate failure
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val uri = request?.url ?: return false
                                    val url = uri.toString()
                                    val scheme = uri.scheme?.lowercase() ?: ""

                                    // 1. Block dangerous internal and script schemes
                                    if (scheme == "javascript" || scheme == "file" || scheme == "content" || scheme == "data") {
                                        Log.w("ChromeBrowserScreen", "Blocked dangerous URL scheme navigation: $scheme")
                                        return true
                                    }

                                    // 2. Standard Web protocols (HTTPS / HTTP)
                                    if (scheme == "http" || scheme == "https") {
                                        // Prefer HTTPS: if unencrypted HTTP for external domains, upgrade
                                        if (scheme == "http") {
                                            val host = uri.host?.lowercase() ?: ""
                                            val isLocal = host == "localhost" || host == "127.0.0.1" || host == "10.0.2.2"
                                            if (!isLocal) {
                                                val secureUrl = url.replaceFirst("http://", "https://")
                                                view?.loadUrl(secureUrl)
                                                return true
                                            }
                                        }
                                        return false // Let WebView handle legitimate web URLs
                                    }

                                    // 3. Handle intent:// URLs safely
                                    if (scheme == "intent") {
                                        try {
                                            val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                                            if (intent != null) {
                                                // Security hardening for parsed intent:
                                                intent.addCategory(Intent.CATEGORY_BROWSABLE)
                                                intent.component = null
                                                intent.selector = null
                                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

                                                val context = view?.context ?: return true
                                                val pm = context.packageManager
                                                if (intent.resolveActivity(pm) != null) {
                                                    context.startActivity(intent)
                                                    return true
                                                } else {
                                                    val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                                                    if (!fallbackUrl.isNullOrEmpty()) {
                                                        val fallbackUri = android.net.Uri.parse(fallbackUrl)
                                                        val fallbackScheme = fallbackUri.scheme?.lowercase()
                                                        if (fallbackScheme == "https" || fallbackScheme == "http") {
                                                            view?.loadUrl(fallbackUrl)
                                                            return true
                                                        }
                                                    }
                                                }
                                            }
                                        } catch (e: Exception) {
                                            Log.w("ChromeBrowserScreen", "Error handling intent scheme: ${e.message}")
                                        }
                                        return true
                                    }

                                    // 4. Whitelisted safe external schemes (market:, tel:, mailto:, sms:)
                                    val safeExternalSchemes = setOf("market", "tel", "mailto", "sms")
                                    if (safeExternalSchemes.contains(scheme)) {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                                addCategory(Intent.CATEGORY_BROWSABLE)
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            val context = view?.context ?: return true
                                            if (intent.resolveActivity(context.packageManager) != null) {
                                                context.startActivity(intent)
                                                return true
                                            }
                                        } catch (e: Exception) {
                                            Log.w("ChromeBrowserScreen", "Error launching external scheme $scheme: ${e.message}")
                                        }
                                        return true
                                    }

                                    // Block all other unknown/unsupported schemes
                                    Log.w("ChromeBrowserScreen", "Blocked unsupported URL scheme: $scheme")
                                    return true
                                }

                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    isLoading = true
                                    url?.let {
                                        currentUrl = it
                                        inputUrlText = it
                                    }
                                    canGoBack = view?.canGoBack() == true
                                    canGoForward = view?.canGoForward() == true
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isLoading = false
                                    url?.let {
                                        currentUrl = it
                                        inputUrlText = it
                                    }
                                    canGoBack = view?.canGoBack() == true
                                    canGoForward = view?.canGoForward() == true
                                }

                                override fun onRenderProcessGone(
                                    view: WebView?,
                                    detail: RenderProcessGoneDetail?
                                ): Boolean {
                                    val parent = view?.parent as? ViewGroup
                                    parent?.removeView(view)
                                    view?.destroy()
                                    webViewInstance = null
                                    return true
                                }
                            }

                            loadUrl(currentUrl)
                            webViewInstance = this
                        }
                    },
                    update = { view ->
                        webViewInstance = view
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Bottom Navigation Toolbar for Browser (Back / Forward / Share)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardSurface)
                    .border(0.5.dp, CardBorder)
                    .then(if (onBack != null) Modifier.navigationBarsPadding() else Modifier.padding(bottom = 80.dp))
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { webViewInstance?.goBack() },
                    enabled = canGoBack,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = if (canGoBack) TextPrimary else TextMuted
                    )
                }

                IconButton(
                    onClick = { webViewInstance?.goForward() },
                    enabled = canGoForward,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = "Forward",
                        tint = if (canGoForward) TextPrimary else TextMuted
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Current Page Domain / Security Info
                Text(
                    text = try { java.net.URL(currentUrl).host.replace("www.", "") } catch (e: Exception) { "Search" },
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
