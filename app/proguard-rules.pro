# ==============================================================================
# StreamClean ProGuard & R8 Optimization Rules
# ==============================================================================

# Preserve attributes required for stack traces, annotations, and reflection
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,*Annotation*

# ------------------------------------------------------------------------------
# 1. youtubedl-android & FFmpeg JNI / Native interfaces & Data mappers
# ------------------------------------------------------------------------------
# Preserve JNI methods so native C/C++ libraries can link dynamically
-keepclasseswithmembernames class * {
    native <methods>;
}

# Preserve youtubedl-android core API and DTO mapper classes
-keep class com.yausername.youtubedl_android.YoutubeDL { *; }
-keep class com.yausername.youtubedl_android.YoutubeDLException { *; }
-keep class com.yausername.youtubedl_android.YoutubeDLRequest { *; }
-keep class com.yausername.youtubedl_android.YoutubeDLResponse { *; }
-keep class com.yausername.youtubedl_android.mapper.** { *; }
-keep class com.yausername.ffmpeg.** { *; }

# ------------------------------------------------------------------------------
# 2. Room Database Entities, DAOs, and Database Classes
# ------------------------------------------------------------------------------
# Keep Room entities so reflection and column mappings are preserved
-keep class com.example.data.local.DownloadEntity { *; }
-keep interface com.example.data.local.DownloadDao { *; }
-keep class * extends androidx.room.RoomDatabase

# ------------------------------------------------------------------------------
# 3. Media3 / ExoPlayer Codec & Renderer Reflection
# ------------------------------------------------------------------------------
# Preserve constructor reflection for ExoPlayer renderers and audio/video decoders
-keep class androidx.media3.exoplayer.** {
    <init>(...);
}
-keep class androidx.media3.common.MediaItem { *; }
-keep class androidx.media3.common.PlaybackException { *; }
-keep class androidx.media3.common.PlaybackParameters { *; }
-keep class androidx.media3.ui.PlayerView { *; }

# ------------------------------------------------------------------------------
# 4. Coil Image Loading Library
# ------------------------------------------------------------------------------
-keep class coil.** { *; }

# ------------------------------------------------------------------------------
# 5. Application Services and FileProvider
# ------------------------------------------------------------------------------
-keep class com.example.service.DownloadService { *; }
-keep class androidx.core.content.FileProvider { *; }
-keep class com.example.MainActivity { *; }
