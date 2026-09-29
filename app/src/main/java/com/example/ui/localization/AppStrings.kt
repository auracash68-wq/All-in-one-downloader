package com.example.ui.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

interface AppStrings {
    // Navigation
    val navDownload: String
    val navDownloads: String
    val navBrowser: String
    val navSettings: String

    // Download / Home Screen
    val downloadTitle: String
    val downloadSubtitle: String
    val pasteVideoLinkHint: String
    val pasteButton: String
    val pasteMultipleLinkButton: String
    val mp4VideoTab: String
    val mp3AudioTab: String
    val downloadActionButton: String
    val fetchingVideoInfo: String

    // Format & Quality Dialog
    val chooseFormatAndQualityTitle: String
    val videoOptionsHeader: String
    val audioOptionsHeader: String
    val singleQualityAvailable: String
    val downloadButton: String
    val cancelButton: String
    val closeButton: String
    val okButton: String

    // Low Memory Dialog
    val lowMemoryTitle: String
    val lowMemoryWarningText: String
    val downloadStandardBtn: String
    val downloadAnywayBtn: String

    // Multiple URL Dialog
    val downloadMultipleVideosTitle: String
    val videoUrlsSubtitle: String
    val addUrlButton: String
    val downloadAllButton: String

    // Toasts & Alerts
    val alreadyDownloadedTitle: String
    val alreadyDownloadedMessage: String
    val pleaseEnterUrl: String
    val linkPasted: String
    val clipboardEmpty: String
    val downloadStarted: String
    val unsupportedUrl: String

    // Downloads Screen
    val downloadsTitle: String
    val downloadsSubtitle: String
    val storageUsed: String
    val filesCount: String
    val tabAll: String
    val tabVideo: String
    val tabAudio: String
    val searchDownloadsHint: String
    val noDownloadsYet: String
    val noDownloadsSubtitle: String
    val playInPlayer: String
    val addToPrivate: String
    val exportToGallery: String
    val share: String
    val delete: String
    val deleteConfirmTitle: String
    val deleteConfirmMessage: String

    // Browser Screen
    val searchOrTypeUrlHint: String
    val videoDetected: String
    val reload: String
    val home: String
    val desktopSite: String

    // Settings Screen
    val settingsTitle: String
    val settingsSubtitle: String
    val preferencesSection: String
    val interfaceLabel: String
    val appearanceItem: String
    val languageItem: String
    val notificationsItem: String
    val notificationsSubtitle: String
    val aboutSection: String
    val verifiedBuildLabel: String
    val aboutItem: String
    val aboutVersionSubtitle: String
    val latestBadge: String
    val privacyPolicyItem: String
    val privacyPolicySubtitle: String

    // Appearance Dialog
    val chooseAppearanceTitle: String
    val systemDefaultOption: String
    val lightOption: String
    val darkOption: String

    // Language Dialog
    val selectLanguageTitle: String
    val languageChangedToast: (String) -> String

    // About Dialog
    val aboutDialogTitle: String
    val aboutVersionText: String
    val aboutDescription: String
    val aboutBullet1: String
    val aboutBullet2: String
    val aboutBullet3: String
    val aboutCopyright: String

    // Privacy Dialog
    val privacyDialogTitle: String
    val privacyDialogText: String
}

object EnglishStrings : AppStrings {
    override val navDownload = "Download"
    override val navDownloads = "Downloads"
    override val navBrowser = "Browser"
    override val navSettings = "Settings"

    override val downloadTitle = "Download"
    override val downloadSubtitle = "Paste a video link to download"
    override val pasteVideoLinkHint = "Paste video link"
    override val pasteButton = "Paste"
    override val pasteMultipleLinkButton = "Paste Multiple Link"
    override val mp4VideoTab = "MP4 Video"
    override val mp3AudioTab = "MP3 Audio"
    override val downloadActionButton = "Download"
    override val fetchingVideoInfo = "Fetching video info..."

    override val chooseFormatAndQualityTitle = "Choose Format & Quality"
    override val videoOptionsHeader = "VIDEO FORMATS"
    override val audioOptionsHeader = "AUDIO FORMATS"
    override val singleQualityAvailable = "Direct High-Quality Conversion"
    override val downloadButton = "Download"
    override val cancelButton = "Cancel"
    override val closeButton = "Close"
    override val okButton = "OK"

    override val lowMemoryTitle = "Low Memory Warning"
    override val lowMemoryWarningText = "Your device has limited free RAM. Downloading high-resolution 720p may cause lag. We recommend Standard quality (360p/480p)."
    override val downloadStandardBtn = "Download Standard"
    override val downloadAnywayBtn = "Download Anyway"

    override val downloadMultipleVideosTitle = "Download Multiple Videos"
    override val videoUrlsSubtitle = "Enter 2 or more video URLs to download together in sequence."
    override val addUrlButton = "Add another link"
    override val downloadAllButton = "Download All Videos"

    override val alreadyDownloadedTitle = "Already Downloaded"
    override val alreadyDownloadedMessage = "Already Downloaded\nThis video is already available in Downloads. Please check your Downloads page."
    override val pleaseEnterUrl = "Please enter a valid video link"
    override val linkPasted = "Link pasted from clipboard"
    override val clipboardEmpty = "Clipboard is empty"
    override val downloadStarted = "Download started"
    override val unsupportedUrl = "Please enter a valid video link"

    override val downloadsTitle = "Downloads"
    override val downloadsSubtitle = "Manage your saved videos & audio"
    override val storageUsed = "Storage Used"
    override val filesCount = "Files"
    override val tabAll = "All"
    override val tabVideo = "Video"
    override val tabAudio = "Audio"
    override val searchDownloadsHint = "Search downloads..."
    override val noDownloadsYet = "No downloads yet"
    override val noDownloadsSubtitle = "Videos you download will appear here"
    override val playInPlayer = "Play in Player"
    override val addToPrivate = "Add to Private"
    override val exportToGallery = "Export to Gallery"
    override val share = "Share"
    override val delete = "Delete"
    override val deleteConfirmTitle = "Delete Download"
    override val deleteConfirmMessage = "Are you sure you want to delete this file?"

    override val searchOrTypeUrlHint = "Search or type URL"
    override val videoDetected = "Video Detected"
    override val reload = "Reload"
    override val home = "Home"
    override val desktopSite = "Desktop site"

    override val settingsTitle = "Settings"
    override val settingsSubtitle = "StreamClean preferences & interface configuration"
    override val preferencesSection = "PREFERENCES"
    override val interfaceLabel = "Interface"
    override val appearanceItem = "Appearance"
    override val languageItem = "Language"
    override val notificationsItem = "Notifications"
    override val notificationsSubtitle = "Download completion alerts"
    override val aboutSection = "ABOUT & LEGAL"
    override val verifiedBuildLabel = "Verified Build"
    override val aboutItem = "About StreamClean"
    override val aboutVersionSubtitle = "StreamClean V1.0.0"
    override val latestBadge = "Latest"
    override val privacyPolicyItem = "Privacy Policy"
    override val privacyPolicySubtitle = "Local data security & zero tracking"

    override val chooseAppearanceTitle = "Choose Appearance"
    override val systemDefaultOption = "System default"
    override val lightOption = "Light"
    override val darkOption = "Dark"

    override val selectLanguageTitle = "Select Language"
    override val languageChangedToast: (String) -> String = { lang -> "Language changed to $lang" }

    override val aboutDialogTitle = "About StreamClean"
    override val aboutVersionText = "StreamClean V1.0.0"
    override val aboutDescription = "StreamClean is a simple media downloader and player that processes supported media directly on your device."
    override val aboutBullet1 = "• Video and audio downloads"
    override val aboutBullet2 = "• On-device media processing"
    override val aboutBullet3 = "• Built with trusted open-source technologies"
    override val aboutCopyright = "© 2026 StreamClean. All rights reserved."

    override val privacyDialogTitle = "Privacy Policy"
    override val privacyDialogText = "StreamClean is designed with zero cloud dependencies and zero tracking. All files and conversions are processed 100% locally on your device hardware."
}

object BengaliStrings : AppStrings {
    override val navDownload = "ডাউনলোড"
    override val navDownloads = "ডাউনলোডসমূহ"
    override val navBrowser = "ব্রাউজার"
    override val navSettings = "সেটিংস"

    override val downloadTitle = "ডাউনলোড"
    override val downloadSubtitle = "ডাউনলোড করতে একটি ভিডিও লিঙ্ক পেস্ট করুন"
    override val pasteVideoLinkHint = "ভিডিও লিঙ্ক পেস্ট করুন"
    override val pasteButton = "পেস্ট"
    override val pasteMultipleLinkButton = "একাধিক লিঙ্ক পেস্ট করুন"
    override val mp4VideoTab = "MP4 ভিডিও"
    override val mp3AudioTab = "MP3 অডিও"
    override val downloadActionButton = "ডাউনলোড করুন"
    override val fetchingVideoInfo = "ভিডিওর তথ্য সংগ্রহ করা হচ্ছে..."

    override val chooseFormatAndQualityTitle = "ফরম্যাট ও কোয়ালিটি নির্বাচন করুন"
    override val videoOptionsHeader = "ভিডিও ফরম্যাট"
    override val audioOptionsHeader = "অডিও ফরম্যাট"
    override val singleQualityAvailable = "সরাসরি উচ্চ মানের রূপান্তর"
    override val downloadButton = "ডাউনলোড"
    override val cancelButton = "বাতিল"
    override val closeButton = "বন্ধ করুন"
    override val okButton = "ঠিক আছে"

    override val lowMemoryTitle = "স্বল্প মেমরি সতর্কতা"
    override val lowMemoryWarningText = "আপনার ডিভাইসে পর্যাপ্ত খালি র‍্যাম নেই। উচ্চ রেজোলিউশনের ৭২০p ডাউনলোডে ধীরগতি হতে পারে। আমরা স্ট্যান্ডার্ড কোয়ালিটি (৩৬০p/৪৮০p) সুপারিশ করছি।"
    override val downloadStandardBtn = "স্ট্যান্ডার্ড ডাউনলোড করুন"
    override val downloadAnywayBtn = "যেকোনোভাবে ডাউনলোড করুন"

    override val downloadMultipleVideosTitle = "একাধিক ভিডিও ডাউনলোড করুন"
    override val videoUrlsSubtitle = "একসাথে ক্রমানুসারে ডাউনলোড করতে ২ বা ততোধিক ভিডিও URL লিখুন।"
    override val addUrlButton = "আরেকটি লিঙ্ক যোগ করুন"
    override val downloadAllButton = "সব ভিডিও ডাউনলোড করুন"

    override val alreadyDownloadedTitle = "ইতিমধ্যে ডাউনলোড করা হয়েছে"
    override val alreadyDownloadedMessage = "ইতিমধ্যে ডাউনলোড করা হয়েছে\nএই ভিডিওটি ইতিমধ্যে ডাউনলোডসে উপলব্ধ রয়েছে। অনুগ্রহ করে আপনার ডাউনলোডস পেজ দেখুন।"
    override val pleaseEnterUrl = "অনুগ্রহ করে একটি সঠিক ভিডিও লিঙ্ক প্রদান করুন"
    override val linkPasted = "ক্লিপবোর্ড থেকে লিঙ্ক পেস্ট করা হয়েছে"
    override val clipboardEmpty = "ক্লিপবোর্ড খালি"
    override val downloadStarted = "ডাউনলোড শুরু হয়েছে"
    override val unsupportedUrl = "অনুগ্রহ করে একটি সঠিক ভিডিও লিঙ্ক প্রদান করুন"

    override val downloadsTitle = "ডাউনলোডসমূহ"
    override val downloadsSubtitle = "আপনার সংরক্ষিত ভিডিও ও অডিও পরিচালনা করুন"
    override val storageUsed = "ব্যবহৃত স্টোরেজ"
    override val filesCount = "ফাইল"
    override val tabAll = "সকল"
    override val tabVideo = "ভিডিও"
    override val tabAudio = "অডিও"
    override val searchDownloadsHint = "ডাউনলোড খুঁজুন..."
    override val noDownloadsYet = "এখনও কোনো ডাউনলোড নেই"
    override val noDownloadsSubtitle = "আপনার ডাউনলোড করা ভিডিওগুলো এখানে প্রদর্শিত হবে"
    override val playInPlayer = "প্লেয়ারে চালান"
    override val addToPrivate = "প্রাইভেটে যোগ করুন"
    override val exportToGallery = "গ্যালারিতে সংরক্ষণ করুন"
    override val share = "শেয়ার করুন"
    override val delete = "মুছুন"
    override val deleteConfirmTitle = "ডাউনলোড মুছুন"
    override val deleteConfirmMessage = "আপনি কি নিশ্চিতভাবে এই ফাইলটি মুছে ফেলতে চান?"

    override val searchOrTypeUrlHint = "অনুসন্ধান বা URL লিখুন"
    override val videoDetected = "ভিডিও সনাক্ত হয়েছে"
    override val reload = "পুনরায় লোড করুন"
    override val home = "হোম"
    override val desktopSite = "ডেস্কটপ সাইট"

    override val settingsTitle = "সেটিংস"
    override val settingsSubtitle = "StreamClean পছন্দসমূহ ও ইন্টারফেস কনফিগারেশন"
    override val preferencesSection = "পছন্দসমূহ"
    override val interfaceLabel = "ইন্টারফেস"
    override val appearanceItem = "অ্যাপিয়ারেন্স"
    override val languageItem = "ভাষা"
    override val notificationsItem = "বিজ্ঞপ্তি"
    override val notificationsSubtitle = "ডাউনলোড সম্পন্ন হওয়ার অ্যালার্ট"
    override val aboutSection = "সম্পর্কে ও আইনি"
    override val verifiedBuildLabel = "যাচাইকৃত বিল্ড"
    override val aboutItem = "StreamClean সম্পর্কে"
    override val aboutVersionSubtitle = "StreamClean V1.0.0"
    override val latestBadge = "লেটেস্ট"
    override val privacyPolicyItem = "গোপনীয়তা নীতি"
    override val privacyPolicySubtitle = "স্থানীয় ডেটা নিরাপত্তা ও ট্র্যাকিংবিহীন"

    override val chooseAppearanceTitle = "অ্যাপিয়ারেন্স নির্বাচন করুন"
    override val systemDefaultOption = "সিস্টেম ডিফল্ট"
    override val lightOption = "লাইট"
    override val darkOption = "ডার্ক"

    override val selectLanguageTitle = "ভাষা নির্বাচন করুন"
    override val languageChangedToast: (String) -> String = { lang -> "ভাষা পরিবর্তন করে $lang করা হয়েছে" }

    override val aboutDialogTitle = "StreamClean সম্পর্কে"
    override val aboutVersionText = "StreamClean V1.0.0"
    override val aboutDescription = "StreamClean হলো একটি সহজ মিডিয়া ডাউনলোডার এবং প্লেয়ার যা সমর্থিত মিডিয়া সরাসরি আপনার ডিভাইসে প্রক্রিয়াজাত করে।"
    override val aboutBullet1 = "• ভিডিও এবং অডিও ডাউনলোড"
    override val aboutBullet2 = "• ডিভাইসেই মিডিয়া প্রসেসিং"
    override val aboutBullet3 = "• বিশ্বস্ত ওপেন-সোর্স প্রযুক্তির সাহায্যে নির্মিত"
    override val aboutCopyright = "© 2026 StreamClean. All rights reserved."

    override val privacyDialogTitle = "গোপনীয়তা নীতি"
    override val privacyDialogText = "StreamClean কোনো ক্লাউড নির্ভরতা এবং ট্র্যাকিং ছাড়া ডিজাইন করা হয়েছে। সমস্ত ফাইল ও রূপান্তর সম্পূর্ণ স্থানীয়ভাবে আপনার ডিভাইসের হার্ডওয়্যারে প্রক্রিয়াজাত হয়।"
}

object HindiStrings : AppStrings {
    override val navDownload = "डाउनलोड"
    override val navDownloads = "डाउनलोड्स"
    override val navBrowser = "ब्राउज़र"
    override val navSettings = "सेटिंग्स"

    override val downloadTitle = "डाउनलोड"
    override val downloadSubtitle = "डाउनलोड करने के लिए वीडियो लिंक पेस्ट करें"
    override val pasteVideoLinkHint = "वीडियो लिंक पेस्ट करें"
    override val pasteButton = "पेस्ट"
    override val pasteMultipleLinkButton = "एकाधिक लिंक पेस्ट करें"
    override val mp4VideoTab = "MP4 वीडियो"
    override val mp3AudioTab = "MP3 ऑडियो"
    override val downloadActionButton = "डाउनलोड करें"
    override val fetchingVideoInfo = "वीडियो की जानकारी प्राप्त हो रही है..."

    override val chooseFormatAndQualityTitle = "फ़ॉर्मेट और क्वालिटी चुनें"
    override val videoOptionsHeader = "वीडियो फ़ॉर्मेट"
    override val audioOptionsHeader = "ऑडियो फ़ॉर्मेट"
    override val singleQualityAvailable = "सीधे उच्च-गुणवत्ता रूपांतरण"
    override val downloadButton = "डाउनलोड"
    override val cancelButton = "रद्द करें"
    override val closeButton = "बंद करें"
    override val okButton = "ठीक है"

    override val lowMemoryTitle = "कम मेमोरी चेतावनी"
    override val lowMemoryWarningText = "आपके डिवाइस में सीमित खाली रैम है। उच्च रिज़ॉल्यूशन 720p डाउनलोड करने से धीमापन आ सकता है। हम मानक गुणवत्ता (360p/480p) की अनुशंसा करते हैं।"
    override val downloadStandardBtn = "मानक डाउनलोड करें"
    override val downloadAnywayBtn = "फिर भी डाउनलोड करें"

    override val downloadMultipleVideosTitle = "एकाधिक वीडियो डाउनलोड करें"
    override val videoUrlsSubtitle = "क्रम में एक साथ डाउनलोड करने के लिए 2 या अधिक वीडियो URL दर्ज करें।"
    override val addUrlButton = "एक और लिंक जोड़ें"
    override val downloadAllButton = "सभी वीडियो डाउनलोड करें"

    override val alreadyDownloadedTitle = "पहले ही डाउनलोड किया जा चुका है"
    override val alreadyDownloadedMessage = "पहले ही डाउनलोड किया जा चुका है\nयह वीडियो पहले से डाउनलोड्स में उपलब्ध है। कृपया अपना डाउनलोड्स पेज देखें।"
    override val pleaseEnterUrl = "कृपया एक वैध वीडियो लिंक दर्ज करें"
    override val linkPasted = "क्लिपबोर्ड से लिंक पेस्ट किया गया"
    override val clipboardEmpty = "क्लिपबोर्ड खाली है"
    override val downloadStarted = "डाउनलोड शुरू हुआ"
    override val unsupportedUrl = "कृपया एक वैध वीडियो लिंक दर्ज करें"

    override val downloadsTitle = "डाउनलोड्स"
    override val downloadsSubtitle = "अपने सहेजे गए वीडियो और ऑडियो प्रबंधित करें"
    override val storageUsed = "प्रयुक्त स्टोरेज"
    override val filesCount = "फ़ाइलें"
    override val tabAll = "सभी"
    override val tabVideo = "वीडियो"
    override val tabAudio = "ऑडियो"
    override val searchDownloadsHint = "डाउनलोड खोजें..."
    override val noDownloadsYet = "अभी तक कोई डाउनलोड नहीं"
    override val noDownloadsSubtitle = "आपके डाउनलोड किए गए वीडियो यहाँ दिखाई देंगे"
    override val playInPlayer = "प्लेयर में चलाएं"
    override val addToPrivate = "प्राइवेट में जोड़ें"
    override val exportToGallery = "गैलरी में सहेजें"
    override val share = "शेयर करें"
    override val delete = "हटाएं"
    override val deleteConfirmTitle = "डाउनलोड हटाएं"
    override val deleteConfirmMessage = "क्या आप वाकई इस फ़ाइल को हटाना चाहते हैं?"

    override val searchOrTypeUrlHint = "सर्च करें या URL लिखें"
    override val videoDetected = "वीडियो का पता चला"
    override val reload = "पुनः लोड करें"
    override val home = "होम"
    override val desktopSite = "डेस्कटॉप साइट"

    override val settingsTitle = "सेटिंग्स"
    override val settingsSubtitle = "StreamClean प्राथमिकताएं और इंटरफ़ेस कॉन्फ़िगरेशन"
    override val preferencesSection = "प्राथमिकताएं"
    override val interfaceLabel = "इंटरफ़ेस"
    override val appearanceItem = "दिखावट"
    override val languageItem = "भाषा"
    override val notificationsItem = "सूचनाएं"
    override val notificationsSubtitle = "डाउनलोड पूरा होने के अलर्ट"
    override val aboutSection = "जानकारी और कानूनी"
    override val verifiedBuildLabel = "सत्यापित बिल्ड"
    override val aboutItem = "StreamClean के बारे में"
    override val aboutVersionSubtitle = "StreamClean V1.0.0"
    override val latestBadge = "नवीनतम"
    override val privacyPolicyItem = "गोपनीयता नीति"
    override val privacyPolicySubtitle = "स्थानीय डेटा सुरक्षा और ज़ीरो ट्रैकिंग"

    override val chooseAppearanceTitle = "दिखावट चुनें"
    override val systemDefaultOption = "सिस्टम डिफ़ॉल्ट"
    override val lightOption = "लाइट"
    override val darkOption = "डार्क"

    override val selectLanguageTitle = "भाषा चुनें"
    override val languageChangedToast: (String) -> String = { lang -> "भाषा बदलकर $lang कर दी गई" }

    override val aboutDialogTitle = "StreamClean के बारे में"
    override val aboutVersionText = "StreamClean V1.0.0"
    override val aboutDescription = "StreamClean एक सरल मीडिया डाउनलोडर और प्लेयर है जो समर्थित मीडिया को सीधे आपके डिवाइस पर संसाधित करता है।"
    override val aboutBullet1 = "• वीडियो और ऑडियो डाउनलोड"
    override val aboutBullet2 = "• डिवाइस पर ही मीडिया प्रोसेसिंग"
    override val aboutBullet3 = "• विश्वसनीय ओपन-सोर्स तकनीकों से निर्मित"
    override val aboutCopyright = "© 2026 StreamClean. All rights reserved."

    override val privacyDialogTitle = "गोपनीयता नीति"
    override val privacyDialogText = "StreamClean को शून्य क्लाउड निर्भरता और शून्य ट्रैकिंग के साथ डिज़ाइन किया गया है। सभी फ़ाइलें और रूपांतरण 100% स्थानीय रूप से आपके डिवाइस हार्डवेयर पर संसाधित होते हैं।"
}

val LocalAppStrings = staticCompositionLocalOf<AppStrings> { EnglishStrings }

fun getAppStrings(language: String): AppStrings {
    return when (language) {
        "বাংলা" -> BengaliStrings
        "हिन्दी" -> HindiStrings
        else -> EnglishStrings
    }
}
