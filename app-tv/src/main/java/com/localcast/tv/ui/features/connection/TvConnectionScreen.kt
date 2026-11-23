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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
fun TvDiscoveryRadar(
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    if (isConnected) {
        Box(
            modifier = modifier.size(150.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(TvAccentGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = TvBackground,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "radar_transition")
    val pulse1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_1"
    )
    val pulse2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, delayMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_2"
    )
    val pulse3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, delayMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_3"
    )

    Box(
        modifier = modifier.size(150.dp),
        contentAlignment = Alignment.Center
    ) {
        val baseColor = TvAccentCyan

        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.minDimension / 2f

            if (!isConnected) {
                listOf(pulse1, pulse2, pulse3).forEach { progress ->
                    val radius = maxRadius * progress
                    val alpha = (1f - progress).coerceIn(0f, 0.7f)
                    drawCircle(
                        color = baseColor.copy(alpha = alpha),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawCircle(
                        color = baseColor.copy(alpha = alpha * 0.1f),
                        radius = radius,
                        center = center
                    )
                }
            }

            drawCircle(
                color = baseColor.copy(alpha = 0.25f),
                radius = 32.dp.toPx(),
                center = center
            )
        }

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (isConnected) TvAccentGreen else TvAccentCyan),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isConnected) Icons.Filled.Check else Icons.Filled.Wifi,
                contentDescription = null,
                tint = TvBackground,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
fun TvConnectionScreen(viewModel: TvMainViewModel) {
    val discoveredPhones by viewModel.discoveredPhones.collectAsState()
    val connState by viewModel.connectionState.collectAsState()
    val localIp by viewModel.localIp.collectAsState()
    val isProbingSubnet by viewModel.isProbingSubnet.collectAsState()

    // Default subnet prefix e.g. "192.168.1."
    val defaultSubnet = remember(localIp) {
        val parts = localIp?.split(".")
        if (parts != null && parts.size == 4) {
            "${parts[0]}.${parts[1]}.${parts[2]}."
        } else {
            "192.168."
        }
    }

    var manualIp by remember(defaultSubnet) { mutableStateOf(defaultSubnet) }
    var manualPort by remember { mutableStateOf("8844") }
    var manualPassword by remember { mutableStateOf("cast123") }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Left Column: Auto-Discovered Phones + 1-Click Subnet Probe
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Nearby Phones",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TvTextPrimary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // 1-Click Subnet Fast-Probe
                    var isFastProbeBtnFocused by remember { mutableStateOf(false) }
                    val fastProbeBtnScale by animateFloatAsState(
                        targetValue = if (isFastProbeBtnFocused) 1.06f else 1.0f,
                        animationSpec = tween(durationMillis = 110, easing = FastOutSlowInEasing),
                        label = "conn_probe_scale"
                    )
                    val fastProbeBtnBorder by animateColorAsState(
                        targetValue = if (isFastProbeBtnFocused) Color.White else TvAccentCyan.copy(alpha = 0.5f),
                        animationSpec = tween(110),
                        label = "conn_probe_border"
                    )

                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = fastProbeBtnScale
                                scaleY = fastProbeBtnScale
                            }
                            .clip(RoundedCornerShape(8.dp))
                            .then(
                                if (isFastProbeBtnFocused) Modifier.background(TvBrandGradient)
                                else Modifier.background(TvAccentCyan)
                            )
                            .border(
                                width = if (isFastProbeBtnFocused) 2.dp else 1.dp,
                                color = fastProbeBtnBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .onFocusChanged { isFastProbeBtnFocused = it.isFocused }
                            .focusable(!isProbingSubnet)
                            .clickable(enabled = !isProbingSubnet) { viewModel.probeSubnet() }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isProbingSubnet) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(13.dp),
                                    color = if (isFastProbeBtnFocused) Color.White else TvBackground,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Probing...",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFastProbeBtnFocused) Color.White else TvBackground
                                )
                            } else {
                                Icon(
                                    Icons.Filled.Search,
                                    contentDescription = "Fast Probe",
                                    modifier = Modifier.size(15.dp),
                                    tint = if (isFastProbeBtnFocused) Color.White else TvBackground
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    "Fast-Probe",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFastProbeBtnFocused) Color.White else TvBackground
                                )
                            }
                        }
                    }

                    // Standard mDNS Scan
                    var isMdnsBtnFocused by remember { mutableStateOf(false) }
                    val mdnsBtnScale by animateFloatAsState(
                        targetValue = if (isMdnsBtnFocused) 1.06f else 1.0f,
                        animationSpec = tween(durationMillis = 110, easing = FastOutSlowInEasing),
                        label = "conn_mdns_scale"
                    )
                    val mdnsBtnBorder by animateColorAsState(
                        targetValue = if (isMdnsBtnFocused) Color.White else Color.Transparent,
                        animationSpec = tween(110),
                        label = "conn_mdns_border"
                    )

                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = mdnsBtnScale
                                scaleY = mdnsBtnScale
                            }
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isMdnsBtnFocused) TvAccentCyan else TvSurfaceVariant)
                            .border(
                                width = if (isMdnsBtnFocused) 2.dp else 1.dp,
                                color = mdnsBtnBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .onFocusChanged { isMdnsBtnFocused = it.isFocused }
                            .focusable()
                            .clickable { viewModel.startDiscovery() }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = "Scan",
                                tint = if (isMdnsBtnFocused) TvBackground else TvTextPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                "mDNS",
                                fontSize = 13.sp,
                                color = if (isMdnsBtnFocused) TvBackground else TvTextPrimary,
                                fontWeight = if (isMdnsBtnFocused) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Animated Subnet Probe Status Banner
            if (isProbingSubnet) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = TvSurfaceVariant),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = TvAccentCyan, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Probing 254 hosts on subnet in parallel...",
                            fontSize = 13.sp,
                            color = TvAccentCyan
                        )
                    }
                }
            }

            if (discoveredPhones.isEmpty() && !isProbingSubnet) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(TvBackground)
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), RoundedCornerShape(10.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = TvAccentCyan, modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Searching on local Wi-Fi (mDNS & UDP)...",
                            fontSize = 15.sp,
                            color = TvTextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tip: Tap 'Fast-Probe' above to scan all 254 IPs directly.",
                            fontSize = 13.sp,
                            color = TvAccentCyan
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(discoveredPhones, key = { it.serviceName }) { phone ->
                        var isFocused by remember { mutableStateOf(false) }
                        val phoneCardScale by animateFloatAsState(
                            targetValue = if (isFocused) 1.04f else 1.0f,
                            animationSpec = tween(durationMillis = 110, easing = FastOutSlowInEasing),
                            label = "phone_card_scale"
                        )
                        val phoneCardBorderColor by animateColorAsState(
                            targetValue = if (isFocused) TvAccentCyan else Color.White.copy(alpha = 0.12f),
                            animationSpec = tween(110),
                            label = "phone_card_border"
                        )
                        val phoneCardBorderWidth by animateDpAsState(
                            targetValue = if (isFocused) 2.dp else 1.dp,
                            animationSpec = tween(110),
                            label = "phone_card_border_width"
                        )
                        val phoneCardBg by animateColorAsState(
                            targetValue = if (isFocused) TvSurfaceVariant else TvBackground,
                            animationSpec = tween(110),
                            label = "phone_card_bg"
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    scaleX = phoneCardScale
                                    scaleY = phoneCardScale
                                }
                                .clip(RoundedCornerShape(10.dp))
                                .background(phoneCardBg)
                                .border(
                                    width = phoneCardBorderWidth,
                                    color = phoneCardBorderColor,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .onFocusChanged { isFocused = it.isFocused }
                                .focusable()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = phone.deviceName.ifBlank { phone.serviceName },
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TvTextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    val isSubnet = phone.serviceName.startsWith("Subnet")
                                    Surface(
                                        color = if (isSubnet) TvAccentCyan.copy(alpha = 0.2f) else TvAccentGreen.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (isSubnet) "Subnet Probe" else "Wi-Fi Beacon",
                                            color = if (isSubnet) TvAccentCyan else TvAccentGreen,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${phone.host?.hostAddress}:${phone.port}",
                                    fontSize = 13.sp,
                                    color = TvTextSecondary
                                )
                            }

                            var isConnectBtnFocused by remember { mutableStateOf(false) }
                            val connectBtnScale by animateFloatAsState(
                                targetValue = if (isConnectBtnFocused) 1.06f else 1.0f,
                                animationSpec = tween(durationMillis = 110, easing = FastOutSlowInEasing),
                                label = "connect_btn_scale"
                            )
                            val connectBtnBorder by animateColorAsState(
                                targetValue = if (isConnectBtnFocused) Color.White else Color.Transparent,
                                animationSpec = tween(110),
                                label = "connect_btn_border"
                            )

                            Box(
                                modifier = Modifier
                                    .graphicsLayer {
                                        scaleX = connectBtnScale
                                        scaleY = connectBtnScale
                                    }
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isConnectBtnFocused) Color.White else TvAccentCyan)
                                    .border(
                                        width = if (isConnectBtnFocused) 2.dp else 1.dp,
                                        color = connectBtnBorder,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .onFocusChanged { isConnectBtnFocused = it.isFocused }
                                    .focusable()
                                    .clickable {
                                        phone.host?.hostAddress?.let { ip ->
                                            viewModel.connectToPhone(
                                                ip,
                                                phone.port,
                                                manualPassword,
                                                phone.deviceName.ifBlank { phone.serviceName }
                                            )
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Connect",
                                    fontSize = 13.sp,
                                    color = TvBackground,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Right Column: Direct Manual IP Connection Card
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            colors = CardDefaults.cardColors(containerColor = TvBackground),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        text = "Connect Manually by IP",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TvAccentCyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "If local Wi-Fi discovery does not find your phone, enter the IP shown in your phone app or notification.",
                        fontSize = 13.sp,
                        color = TvTextSecondary,
                        lineHeight = 18.sp
                    )
                }

                item {
                    Surface(
                        color = TvSurfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, TvAccentCyan.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TV IP: ${localIp ?: "Detecting..."}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TvTextPrimary
                            )
                            Text(
                                text = "Default Port: 8844",
                                fontSize = 13.sp,
                                color = TvTextSecondary
                            )
                        }
                    }
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(TvSurfaceVariant.copy(alpha = 0.45f))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = manualIp,
                            onValueChange = { manualIp = it },
                            label = { Text("Phone IP Address", fontSize = 13.sp) },
                            placeholder = { Text("e.g. 192.168.1.100", fontSize = 13.sp, color = TvTextSecondary.copy(alpha = 0.6f)) },
                            singleLine = true,
                            maxLines = 1,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TvTextPrimary,
                                unfocusedTextColor = TvTextPrimary,
                                focusedBorderColor = TvAccentCyan,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedLabelColor = TvAccentCyan,
                                unfocusedLabelColor = TvTextSecondary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = manualPort,
                                onValueChange = { manualPort = it },
                                label = { Text("Port", fontSize = 13.sp) },
                                singleLine = true,
                                maxLines = 1,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TvTextPrimary,
                                    unfocusedTextColor = TvTextPrimary,
                                    focusedBorderColor = TvAccentCyan,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    focusedLabelColor = TvAccentCyan,
                                    unfocusedLabelColor = TvTextSecondary
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = manualPassword,
                                onValueChange = { manualPassword = it },
                                label = { Text("Password", fontSize = 13.sp) },
                                singleLine = true,
                                maxLines = 1,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TvTextPrimary,
                                    unfocusedTextColor = TvTextPrimary,
                                    focusedBorderColor = TvAccentCyan,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    focusedLabelColor = TvAccentCyan,
                                    unfocusedLabelColor = TvTextSecondary
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        var isManualConnectFocused by remember { mutableStateOf(false) }
                        val connectScale by animateFloatAsState(
                            targetValue = if (isManualConnectFocused) 1.04f else 1.0f,
                            animationSpec = tween(durationMillis = 110, easing = FastOutSlowInEasing),
                            label = "manual_conn_scale"
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer { scaleX = connectScale; scaleY = connectScale }
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isManualConnectFocused) Color.White else TvAccentGreen)
                                .border(
                                    width = if (isManualConnectFocused) 2.dp else 1.dp,
                                    color = if (isManualConnectFocused) Color.White else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .onFocusChanged { isManualConnectFocused = it.isFocused }
                                .focusable()
                                .clickable {
                                    val port = manualPort.toIntOrNull() ?: 8844
                                    viewModel.connectToPhone(manualIp.trim(), port, manualPassword.trim())
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Connect to Phone (Saves & Auto-Reconnects)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TvBackground
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "Tip: When you connect manually, the TV saves this phone and will automatically reconnect whenever both devices are on your Wi-Fi network.",
                        fontSize = 12.sp,
                        color = TvTextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

