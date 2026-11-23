package com.localcast.tv.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.localcast.core.model.InstallStatus
import com.localcast.core.model.MediaType
import com.localcast.core.network.client.ConnectionState
import com.localcast.core.network.storage.LastPlayedMedia
import com.localcast.core.network.storage.PlayedMediaItem
import com.localcast.core.network.storage.SettingsRepository
import com.localcast.tv.playback.AudioTrackInfo
import com.localcast.tv.ui.theme.TvAccentBlue
import com.localcast.tv.ui.theme.TvAccentCyan
import com.localcast.tv.ui.theme.TvAccentGreen
import com.localcast.tv.ui.theme.TvAccentPrimary
import com.localcast.tv.ui.theme.TvAccentRed
import com.localcast.tv.ui.theme.TvBackground
import com.localcast.tv.ui.theme.TvBrandGradient
import com.localcast.tv.ui.theme.TvBrandIndigo
import com.localcast.tv.ui.theme.TvBrandPurple
import com.localcast.tv.ui.theme.TvCardGradient
import com.localcast.tv.ui.theme.TvStreamGradient
import com.localcast.tv.ui.theme.TvSurface
import com.localcast.tv.ui.theme.TvSurfaceVariant
import com.localcast.tv.ui.theme.TvTextPrimary
import com.localcast.tv.ui.theme.TvTextSecondary


@Composable
fun TvClockWidget() {
    var currentTime by remember {
        mutableStateOf(
            java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date())
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date())
            kotlinx.coroutines.delay(10000)
        }
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(TvSurfaceVariant)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = currentTime,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = TvAccentCyan
        )
    }
}

@Composable
fun TvNavRailItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    isExpanded: Boolean = false,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1.0f,
        animationSpec = tween(durationMillis = 110, easing = FastOutSlowInEasing),
        label = "nav_item_scale"
    )

    val itemBg by animateColorAsState(
        targetValue = when {
            isFocused -> Color.Transparent
            isSelected -> Color.White.copy(alpha = 0.12f)
            else -> Color.Transparent
        },
        animationSpec = tween(110),
        label = "nav_item_bg"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) Color.White else if (isSelected) TvAccentCyan.copy(alpha = 0.35f) else Color.Transparent,
        animationSpec = tween(110),
        label = "nav_item_border"
    )

    Box(
        modifier = Modifier
            .size(46.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .then(
                if (isFocused) Modifier.background(TvBrandGradient)
                else Modifier.background(itemBg)
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = borderColor,
                shape = CircleShape
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = when {
                isFocused -> Color.White
                isSelected -> TvAccentCyan
                else -> TvTextSecondary
            },
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun TvNavRail(
    selectedTab: TvTab,
    onTabSelected: (TvTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(68.dp),
        color = TvSurfaceVariant.copy(alpha = 0.75f),
        shape = RectangleShape,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 24.dp, horizontal = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(TvBrandGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tv,
                            contentDescription = "My TV",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TvNavRailItem(
                    title = "Now Playing",
                    icon = Icons.Filled.PlayArrow,
                    isSelected = selectedTab == TvTab.PLAYER,
                    isExpanded = false,
                    onClick = { onTabSelected(TvTab.PLAYER) }
                )

                TvNavRailItem(
                    title = "Received APKs",
                    icon = Icons.Filled.Android,
                    isSelected = selectedTab == TvTab.APKS,
                    isExpanded = false,
                    onClick = { onTabSelected(TvTab.APKS) }
                )

                TvNavRailItem(
                    title = "Pairing",
                    icon = Icons.Filled.Wifi,
                    isSelected = selectedTab == TvTab.CONNECTION,
                    isExpanded = false,
                    onClick = { onTabSelected(TvTab.CONNECTION) }
                )

                TvNavRailItem(
                    title = "Settings",
                    icon = Icons.Filled.Settings,
                    isSelected = selectedTab == TvTab.SETTINGS,
                    isExpanded = false,
                    onClick = { onTabSelected(TvTab.SETTINGS) }
                )
            }

            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(TvAccentCyan.copy(alpha = 0.5f))
            )
        }
    }
}


@Composable
fun ConnectionStatusPill(
    connState: ConnectionState,
    connectedDeviceName: String? = null,
    onRetryClick: (() -> Unit)? = null
) {
    val isConnected = connState is ConnectionState.Connected
    val isConnecting = connState is ConnectionState.Connecting || connState is ConnectionState.Authenticating
    val dotColor = when {
        isConnected -> TvAccentGreen
        isConnecting -> TvAccentCyan
        else -> TvAccentRed
    }
    val statusText = when {
        isConnected -> (connState as ConnectionState.Connected).deviceName?.takeIf { it.isNotBlank() }
            ?: connectedDeviceName?.takeIf { it.isNotBlank() }
            ?: "Phone"
        isConnecting -> "Connecting..."
        else -> "Disconnected"
    }

    var isRetryFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .background(TvSurfaceVariant, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(dotColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = statusText,
            fontSize = 13.sp,
            color = if (isConnected) TvTextPrimary else if (isConnecting) TvAccentCyan else TvTextSecondary,
            fontWeight = FontWeight.Medium
        )

        if (!isConnected && onRetryClick != null) {
            val retryScale by animateFloatAsState(
                targetValue = if (isRetryFocused) 1.10f else 1.0f,
                animationSpec = tween(durationMillis = 110, easing = FastOutSlowInEasing),
                label = "retry_scale"
            )
            val retryBorderColor by animateColorAsState(
                targetValue = if (isRetryFocused) Color.White else Color.Transparent,
                animationSpec = tween(110),
                label = "retry_border"
            )

            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = retryScale
                        scaleY = retryScale
                    }
                    .size(24.dp)
                    .clip(CircleShape)
                    .then(
                        if (isRetryFocused) Modifier.background(TvBrandGradient)
                        else Modifier.background(Color.Transparent)
                    )
                    .border(
                        width = if (isRetryFocused) 1.5.dp else 0.dp,
                        color = retryBorderColor,
                        shape = CircleShape
                    )
                    .onFocusChanged { isRetryFocused = it.isFocused }
                    .focusable()
                    .clickable { onRetryClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Retry Connection",
                    tint = if (isRetryFocused) Color.White else TvTextSecondary,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

