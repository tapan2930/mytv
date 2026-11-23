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
fun TvSettingsScreen(viewModel: TvMainViewModel) {
    val preferredPlayer by viewModel.preferredPlayer.collectAsState(initial = SettingsRepository.PLAYER_BUILTIN)
    var showUnpairConfirmDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text("Settings & Preferences", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TvTextPrimary)
            Spacer(modifier = Modifier.height(20.dp))

            // Preference: Default Player Choice
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Default Player Engine", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TvTextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Choose whether to play streams using the built-in ExoPlayer or automatically launch VLC / MX Player on TV.",
                    fontSize = 14.sp,
                    color = TvTextSecondary
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    var isExoFocused by remember { mutableStateOf(false) }
                    val isExoSelected = preferredPlayer == SettingsRepository.PLAYER_BUILTIN
                    val exoScale by animateFloatAsState(
                        targetValue = if (isExoFocused) 1.04f else 1.0f,
                        animationSpec = tween(durationMillis = 110, easing = FastOutSlowInEasing),
                        label = "exo_scale"
                    )
                    val exoBorderColor by animateColorAsState(
                        targetValue = if (isExoFocused) Color.White else if (isExoSelected) TvAccentCyan else Color.White.copy(alpha = 0.12f),
                        animationSpec = tween(110),
                        label = "exo_border"
                    )

                    // ExoPlayer Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer {
                                scaleX = exoScale
                                scaleY = exoScale
                            }
                            .clip(RoundedCornerShape(12.dp))
                            .then(
                                if (isExoFocused) Modifier.background(TvBrandGradient)
                                else if (isExoSelected) Modifier.background(TvSurfaceVariant)
                                else Modifier.background(TvBackground)
                            )
                            .border(
                                width = if (isExoFocused) 2.5.dp else 1.5.dp,
                                color = exoBorderColor,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .onFocusChanged { isExoFocused = it.isFocused }
                            .focusable()
                            .clickable { viewModel.updatePreferredPlayer(SettingsRepository.PLAYER_BUILTIN) }
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Tv,
                                        contentDescription = null,
                                        tint = if (isExoFocused) Color.White else if (isExoSelected) TvAccentCyan else TvTextSecondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Built-in ExoPlayer",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isExoFocused) Color.White else if (isExoSelected) TvAccentCyan else TvTextPrimary
                                    )
                                }

                                if (isExoSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Selected",
                                        tint = if (isExoFocused) Color.White else TvAccentCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Hardware-accelerated TV player with seamless Netflix-style OSD, audio scrubbing, and subtitle controls.",
                                fontSize = 13.sp,
                                color = if (isExoFocused) Color.White.copy(alpha = 0.9f) else TvTextSecondary
                            )
                        }
                    }

                    var isExtFocused by remember { mutableStateOf(false) }
                    val isExtSelected = preferredPlayer == SettingsRepository.PLAYER_EXTERNAL
                    val extScale by animateFloatAsState(
                        targetValue = if (isExtFocused) 1.04f else 1.0f,
                        animationSpec = tween(durationMillis = 110, easing = FastOutSlowInEasing),
                        label = "ext_scale"
                    )
                    val extBorderColor by animateColorAsState(
                        targetValue = if (isExtFocused) Color.White else if (isExtSelected) TvAccentCyan else Color.White.copy(alpha = 0.12f),
                        animationSpec = tween(110),
                        label = "ext_border"
                    )

                    // External Player Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer {
                                scaleX = extScale
                                scaleY = extScale
                            }
                            .clip(RoundedCornerShape(12.dp))
                            .then(
                                if (isExtFocused) Modifier.background(TvBrandGradient)
                                else if (isExtSelected) Modifier.background(TvSurfaceVariant)
                                else Modifier.background(TvBackground)
                            )
                            .border(
                                width = if (isExtFocused) 2.5.dp else 1.5.dp,
                                color = extBorderColor,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .onFocusChanged { isExtFocused = it.isFocused }
                            .focusable()
                            .clickable { viewModel.updatePreferredPlayer(SettingsRepository.PLAYER_EXTERNAL) }
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = null,
                                        tint = if (isExtFocused) Color.White else if (isExtSelected) TvAccentCyan else TvTextSecondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "External Player",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isExtFocused) Color.White else if (isExtSelected) TvAccentCyan else TvTextPrimary
                                    )
                                }

                                if (isExtSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Selected",
                                        tint = if (isExtFocused) Color.White else TvAccentCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Launches VLC for Android, MX Player, or Kodi when a stream arrives for exotic codecs or custom filters.",
                                fontSize = 13.sp,
                                color = if (isExtFocused) Color.White.copy(alpha = 0.9f) else TvTextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Unpair Button
            var isUnpairFocused by remember { mutableStateOf(false) }
            val unpairScale by animateFloatAsState(
                targetValue = if (isUnpairFocused) 1.04f else 1.0f,
                animationSpec = tween(durationMillis = 110, easing = FastOutSlowInEasing),
                label = "unpair_scale"
            )
            val unpairBorderColor by animateColorAsState(
                targetValue = if (isUnpairFocused) Color.White else Color.Transparent,
                animationSpec = tween(110),
                label = "unpair_border"
            )

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = unpairScale
                        scaleY = unpairScale
                    }
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isUnpairFocused) Color.White else TvAccentRed)
                    .border(
                        width = if (isUnpairFocused) 2.dp else 1.dp,
                        color = unpairBorderColor,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .onFocusChanged { isUnpairFocused = it.isFocused }
                    .focusable()
                    .clickable { showUnpairConfirmDialog = true }
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Unpair / Disconnect from Saved Phone",
                    fontSize = 14.sp,
                    color = if (isUnpairFocused) TvBackground else Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                color = TvSurfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Connection Security:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TvAccentCyan)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "The TV client saves the phone's port and authentication token in encrypted preferences so you do not need to re-enter credentials every time.",
                        fontSize = 13.sp,
                        color = TvTextSecondary
                    )
                }
            }
        }

        // TV Unpair Modal Confirmation Dialog
        if (showUnpairConfirmDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = TvSurfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(2.dp, TvAccentRed),
                    modifier = Modifier.width(420.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = null,
                            tint = TvAccentRed,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Unpair from Saved Phone?",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "This will disconnect current sessions and clear saved authentication tokens. You will need to pair again with the QR code.",
                            fontSize = 13.sp,
                            color = TvTextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            var isConfirmFocused by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isConfirmFocused) Color.White else TvAccentRed)
                                    .border(
                                        width = if (isConfirmFocused) 2.dp else 1.dp,
                                        color = if (isConfirmFocused) Color.White else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .onFocusChanged { isConfirmFocused = it.isFocused }
                                    .focusable()
                                    .clickable {
                                        showUnpairConfirmDialog = false
                                        viewModel.unpairAndDisconnect()
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Unpair",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isConfirmFocused) TvBackground else Color.White
                                )
                            }

                            var isCancelFocused by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isCancelFocused) Color.White else TvSurface)
                                    .border(
                                        width = if (isCancelFocused) 2.dp else 1.dp,
                                        color = if (isCancelFocused) Color.White else Color.White.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .onFocusChanged { isCancelFocused = it.isFocused }
                                    .focusable()
                                    .clickable { showUnpairConfirmDialog = false }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Cancel",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCancelFocused) TvBackground else TvTextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
