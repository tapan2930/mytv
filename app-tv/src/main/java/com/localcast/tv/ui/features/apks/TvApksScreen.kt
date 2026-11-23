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
fun TvApkGridCard(
    apk: com.localcast.tv.ui.ReceivedApkInfo,
    onOpen: () -> Unit,
    onReinstall: () -> Unit,
    isInstalled: Boolean
) {
    var isCardFocused by remember { mutableStateOf(false) }

    val cardScale by animateFloatAsState(
        targetValue = if (isCardFocused) 1.04f else 1.0f,
        animationSpec = tween(durationMillis = 110, easing = FastOutSlowInEasing),
        label = "apk_card_scale"
    )
    val cardBorderColor by animateColorAsState(
        targetValue = if (isCardFocused) TvAccentGreen else Color.White.copy(alpha = 0.12f),
        animationSpec = tween(110),
        label = "apk_card_border"
    )
    val cardBorderWidth by animateDpAsState(
        targetValue = if (isCardFocused) 2.dp else 1.dp,
        animationSpec = tween(110),
        label = "apk_card_width"
    )
    val cardBgColor by animateColorAsState(
        targetValue = if (isCardFocused) TvSurfaceVariant else TvSurfaceVariant.copy(alpha = 0.6f),
        animationSpec = tween(110),
        label = "apk_card_bg"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .border(
                width = cardBorderWidth,
                color = cardBorderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .onFocusChanged { isCardFocused = it.hasFocus },
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Android Icon + App Name + Size Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isInstalled) TvAccentGreen.copy(alpha = 0.2f) else TvBrandIndigo.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Android,
                        contentDescription = null,
                        tint = if (isInstalled) TvAccentGreen else TvAccentCyan,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = apk.appName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TvTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val versionLabel = if (apk.versionName.isNotBlank() && apk.versionCode > 0) {
                        "v${apk.versionName} (Build ${apk.versionCode}) • ${apk.packageName}"
                    } else if (apk.versionCode > 0) {
                        "Build ${apk.versionCode} • ${apk.packageName}"
                    } else {
                        apk.packageName
                    }
                    Text(
                        text = versionLabel,
                        fontSize = 12.sp,
                        color = TvTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    color = TvBackground.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (apk.sizeBytes > 0) "${apk.sizeBytes / 1024 / 1024} MB" else "",
                        fontSize = 12.sp,
                        color = TvTextSecondary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Compatibility Pill & Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isArm64 = android.os.Build.SUPPORTED_ABIS.any { it.contains("arm64") }
                Surface(
                    color = Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (isArm64) "ARM64 Compatible ✓" else "Universal TV Compatible",
                        color = TvTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                val statusText = when {
                    apk.isDowngrade -> "OLDER BUILD (${apk.versionCode} < ${apk.installedVersionCode ?: "?"})"
                    isInstalled -> "INSTALLED ✓"
                    apk.status == InstallStatus.DOWNLOADING -> "DOWNLOADING ${(apk.downloadProgress * 100).toInt()}%"
                    apk.status == InstallStatus.VERIFYING -> "VERIFYING..."
                    apk.status == InstallStatus.PROMPTING_USER -> "PROMPTING INSTALL"
                    apk.status == InstallStatus.FAILED -> "INSTALL FAILED"
                    else -> apk.status.name
                }
                val statusColor = when {
                    apk.isDowngrade -> TvAccentRed
                    isInstalled || apk.status == InstallStatus.INSTALLED -> TvAccentGreen
                    apk.status == InstallStatus.DOWNLOADING || apk.status == InstallStatus.VERIFYING || apk.status == InstallStatus.PROMPTING_USER -> TvAccentCyan
                    apk.status == InstallStatus.FAILED -> TvAccentRed
                    else -> TvTextSecondary
                }

                Text(
                    text = statusText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }

            // Live Animated Download Progress Bar
            if (apk.status == InstallStatus.DOWNLOADING) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { apk.downloadProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = TvAccentCyan,
                    trackColor = TvBackground
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons Row (Open App / Re-install)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isInstalled) {
                    var isOpenFocused by remember { mutableStateOf(false) }
                    val openScale by animateFloatAsState(
                        targetValue = if (isOpenFocused) 1.06f else 1.0f,
                        animationSpec = tween(durationMillis = 110, easing = FastOutSlowInEasing),
                        label = "open_scale"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer {
                                scaleX = openScale
                                scaleY = openScale
                            }
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isOpenFocused) Color.White else TvAccentGreen)
                            .border(
                                width = if (isOpenFocused) 2.dp else 1.dp,
                                color = if (isOpenFocused) Color.White else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .onFocusChanged { isOpenFocused = it.isFocused }
                            .focusable()
                            .clickable { onOpen() }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Launch,
                                contentDescription = "Open App",
                                tint = TvBackground,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Open App",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TvBackground
                            )
                        }
                    }
                }

                var isInstallFocused by remember { mutableStateOf(false) }
                val installScale by animateFloatAsState(
                    targetValue = if (isInstallFocused) 1.06f else 1.0f,
                    animationSpec = tween(durationMillis = 110, easing = FastOutSlowInEasing),
                    label = "install_scale"
                )

                Box(
                    modifier = Modifier
                        .then(if (isInstalled) Modifier.weight(1f) else Modifier.fillMaxWidth())
                        .graphicsLayer {
                            scaleX = installScale
                            scaleY = installScale
                        }
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isInstallFocused) Color.White else if (!isInstalled) TvAccentCyan else TvBackground)
                        .border(
                            width = if (isInstallFocused) 2.dp else 1.dp,
                            color = if (isInstallFocused) Color.White else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .onFocusChanged { isInstallFocused = it.isFocused }
                        .focusable()
                        .clickable { onReinstall() }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Install Again",
                            tint = if (isInstallFocused || !isInstalled) TvBackground else TvTextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (apk.isDowngrade) "Force Downgrade" else if (isInstalled) "Re-install" else "Install Now",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isInstallFocused || !isInstalled) TvBackground else TvTextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TvApksScreen(viewModel: TvMainViewModel) {
    val apks by viewModel.receivedApks.collectAsState()

    if (apks.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(TvSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Android,
                        contentDescription = null,
                        tint = TvAccentGreen,
                        modifier = Modifier.size(44.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No APKs Received Yet",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TvTextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Select any APK in the phone app and tap 'Send to TV' to transfer & install over Wi-Fi.",
                    fontSize = 15.sp,
                    color = TvTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(0.65f)
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(apks, key = { it.filePath.ifBlank { "${it.packageName}_${it.timestamp}" } }) { apk ->
                TvApkGridCard(
                    apk = apk,
                    onOpen = { viewModel.launchApp(apk.packageName) },
                    onReinstall = { viewModel.reinstallApk(apk) },
                    isInstalled = viewModel.isAppInstalled(apk.packageName)
                )
            }
        }
    }
}

