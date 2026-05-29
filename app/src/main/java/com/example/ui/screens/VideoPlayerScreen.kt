package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.example.ui.theme.CineGold
import com.example.ui.theme.CineRed

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    videoUrl: String,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    
    // Brightness management
    var brightness by remember { 
        val initB = activity?.window?.attributes?.screenBrightness ?: 0.5f
        mutableStateOf(if (initB < 0) 0.5f else initB) 
    }
    
    // Volume management
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat() }
    var volumeLevel by remember { mutableStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()) }

    var isBufferLoading by remember { mutableStateOf(true) }
    var isScreenLocked by remember { mutableStateOf(false) }
    
    // Indicator state during drag gestures
    var activeGestureType by remember { mutableStateOf<String?>(null) } // "brightness" or "volume"
    var gestureProgress by remember { mutableStateOf(0f) } // 0f to 1f
    
    // Save current width to calculate region for splits
    var screenWidthPx by remember { mutableStateOf(1000) }

    // Keep screen always illuminated during video playback
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Adjust system brightness reactively on state updates
    LaunchedEffect(brightness) {
        activity?.window?.let { window ->
            val attrs = window.attributes
            attrs.screenBrightness = brightness.coerceIn(0.01f, 1.0f)
            window.attributes = attrs
        }
    }

    // Setup and manage ExoPlayer safely in composition lifecycle
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    LaunchedEffect(videoUrl) {
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent("Mozilla/5.0 Media3/ExoPlayer (CinePremium VIP Scraper)")

        val mediaSource = if (videoUrl.contains(".m3u8")) {
            HlsMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(videoUrl))
        } else {
            DefaultMediaSourceFactory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(videoUrl))
        }

        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBufferLoading = (playbackState == Player.STATE_BUFFERING)
            }
        })
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("video_player_container")
            .onGloballyPositioned { coordinates ->
                screenWidthPx = coordinates.size.width
            }
            .pointerInput(isScreenLocked, screenWidthPx) {
                if (!isScreenLocked) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            // Divide screen vertically down the middle to map inputs
                            val side = if (offset.x < (screenWidthPx / 2)) "brightness" else "volume"
                            activeGestureType = side
                            gestureProgress = if (side == "brightness") brightness else (volumeLevel / maxVolume)
                        },
                        onDragEnd = {
                            activeGestureType = null
                        },
                        onDragCancel = {
                            activeGestureType = null
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            // Calculate change proportion (dragAmount is negative for dragging up, positive for dragging down)
                            val screenHeight = size.height
                            val delta = -(dragAmount / screenHeight) // Dragging up increases levels
                            
                            if (activeGestureType == "brightness") {
                                brightness = (brightness + delta).coerceIn(0.01f, 1.0f)
                                gestureProgress = brightness
                            } else if (activeGestureType == "volume") {
                                val currentVol = (volumeLevel / maxVolume)
                                val nextVol = (currentVol + delta).coerceIn(0.0f, 1.0f)
                                volumeLevel = (nextVol * maxVolume)
                                audioManager.setStreamVolume(
                                    AudioManager.STREAM_MUSIC,
                                    volumeLevel.toInt(),
                                    0
                                )
                                gestureProgress = nextVol
                            }
                        }
                    )
                }
            }
    ) {
        if (videoUrl.endsWith(".m3u8") || videoUrl.endsWith(".mp4")) {
            // Native ExoPlayer for extracted media streams
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = !isScreenLocked // Lock/Unlock controllers
                        setShowNextButton(false)
                        setShowPreviousButton(false)
                        setShowFastForwardButton(true)
                        setShowRewindButton(true)
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { playerView ->
                    playerView.useController = !isScreenLocked
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Web Player Fallback for aggregator sites like megacine.boats
            AndroidView(
                factory = { ctx ->
                    android.webkit.WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        webViewClient = android.webkit.WebViewClient()
                        webChromeClient = android.webkit.WebChromeClient()
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        loadUrl(videoUrl)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // LOADING SYSTEM FEEDBACK BUFFERING
        if (isBufferLoading) {
            CircularProgressIndicator(
                color = CineGold,
                strokeWidth = 3.dp,
                modifier = Modifier
                    .size(54.dp)
                    .align(Alignment.Center)
                    .testTag("video_player_buffer")
            )
        }

        // GESTURE PROGRESS REAL-TIME HUD OVERLAYS
        AnimatedVisibility(
            visible = activeGestureType != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            activeGestureType?.let { type ->
                val icon = if (type == "brightness") "Brilho" else "Volume"
                val percentText = "${(gestureProgress * 100).toInt()}%"
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.85f))
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = icon,
                            color = CineGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = percentText,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // Gold Progress line
                        Box(
                            modifier = Modifier
                                .width(90.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.3f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(gestureProgress)
                                    .background(CineGold)
                            )
                        }
                    }
                }
            }
        }

        // --- TOP AND CORNER HOVER CONTROL HUDS ---
        if (!isScreenLocked) {
            // Close button overlay
            IconButton(
                onClick = onCloseClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .size(44.dp)
                    .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                    .testTag("video_player_close")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Fechar Player de Vídeo",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Safe status bar lock status tag representing active encryption / keyframes
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isScreenLocked) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(CineGold.copy(0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Controles Bloqueados 🔒", color = CineGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // TRAVA DE TELA FLOATING ACTION BUTTON
                IconButton(
                    onClick = { isScreenLocked = !isScreenLocked },
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (isScreenLocked) CineGold else Color.Black.copy(alpha = 0.65f),
                            CircleShape
                        )
                        .testTag("video_player_lock_toggle")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Travar Tela",
                            tint = if (isScreenLocked) Color.Black else CineGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Brief tutorial help on bottom area if screen is unlocked
        if (!isScreenLocked) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 70.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "← Arraste p/ Brilho  |  Arraste p/ Volume →",
                    color = Color.White.copy(0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
