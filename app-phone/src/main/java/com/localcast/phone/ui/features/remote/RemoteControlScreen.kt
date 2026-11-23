package com.localcast.phone.ui

import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localcast.core.model.InstallStatus
import com.localcast.core.model.MediaItem
import com.localcast.core.model.MediaType
import com.localcast.core.model.PlaybackCommand
import com.localcast.phone.ui.theme.AccentBlue
import com.localcast.phone.ui.theme.AccentCyan
import com.localcast.phone.ui.theme.AccentGreen
import com.localcast.phone.ui.theme.AccentPrimary
import com.localcast.phone.ui.theme.AccentRed
import com.localcast.phone.ui.theme.BrandGradient
import com.localcast.phone.ui.theme.DarkBackground
import com.localcast.phone.ui.theme.DarkSurface
import com.localcast.phone.ui.theme.DarkSurfaceVariant
import com.localcast.phone.ui.theme.TextPrimary
import com.localcast.phone.ui.theme.TextSecondary
import java.io.File


@Composable
fun RemoteControlScreen(viewModel: PhoneMainViewModel) {
    val nowPlaying by viewModel.nowPlaying.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val playlist by viewModel.playlist.collectAsState()
    val currentPlaylistIndex by viewModel.currentPlaylistIndex.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Now Playing Title
        Text(
            text = "Now Playing on TV",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = nowPlaying?.title ?: "No active stream",
            fontSize = 14.sp,
            color = AccentCyan
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Transport Controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = { viewModel.playPrevious() },
                modifier = Modifier
                    .size(42.dp)
                    .background(DarkSurfaceVariant, CircleShape)
            ) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous Track", tint = TextPrimary, modifier = Modifier.size(22.dp))
            }

            IconButton(
                onClick = { viewModel.seekRelative(-10000) },
                modifier = Modifier
                    .size(46.dp)
                    .background(DarkSurfaceVariant, CircleShape)
            ) {
                Icon(Icons.Filled.FastRewind, contentDescription = "Rewind 10s", tint = TextPrimary, modifier = Modifier.size(24.dp))
            }

            IconButton(
                onClick = {
                    if (playbackState.isPlaying) {
                        viewModel.sendControlCommand(PlaybackCommand.PAUSE)
                    } else {
                        viewModel.sendControlCommand(PlaybackCommand.PLAY)
                    }
                },
                modifier = Modifier
                    .size(62.dp)
                    .background(BrandGradient, CircleShape)
            ) {
                Icon(
                    imageVector = if (playbackState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }

            IconButton(
                onClick = { viewModel.seekRelative(10000) },
                modifier = Modifier
                    .size(46.dp)
                    .background(DarkSurfaceVariant, CircleShape)
            ) {
                Icon(Icons.Filled.FastForward, contentDescription = "Fast-Forward 10s", tint = TextPrimary, modifier = Modifier.size(24.dp))
            }

            IconButton(
                onClick = { viewModel.playNext() },
                modifier = Modifier
                    .size(42.dp)
                    .background(DarkSurfaceVariant, CircleShape)
            ) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Next Track", tint = TextPrimary, modifier = Modifier.size(22.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Virtual D-Pad for TV Navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Virtual TV D-Pad",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            IconButton(
                onClick = { viewModel.sendControlCommand(PlaybackCommand.BACK) },
                modifier = Modifier
                    .size(38.dp)
                    .background(DarkSurfaceVariant, CircleShape)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .size(180.dp)
                .background(DarkSurface, CircleShape)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = { viewModel.sendControlCommand(PlaybackCommand.DPAD_UP) },
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Up", tint = TextPrimary)
            }

            IconButton(
                onClick = { viewModel.sendControlCommand(PlaybackCommand.DPAD_DOWN) },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Down", tint = TextPrimary)
            }

            IconButton(
                onClick = { viewModel.sendControlCommand(PlaybackCommand.DPAD_LEFT) },
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Left", tint = TextPrimary)
            }

            IconButton(
                onClick = { viewModel.sendControlCommand(PlaybackCommand.DPAD_RIGHT) },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Right", tint = TextPrimary)
            }

            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(BrandGradient, CircleShape)
                    .clickable { viewModel.sendControlCommand(PlaybackCommand.DPAD_CENTER) },
                contentAlignment = Alignment.Center
            ) {
                Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Playlist Queue Card
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Playlist Queue (${playlist.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    if (playlist.isNotEmpty()) {
                        Button(
                            onClick = { viewModel.clearPlaylist() },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Clear Queue", fontSize = 11.sp, color = AccentRed)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (playlist.isEmpty()) {
                    Text(
                        text = "Queue is empty. Tap '+' on any video or song in the Media tab to build a playlist that auto-plays consecutively on TV.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        playlist.forEachIndexed { index, item ->
                            val isCurrent = index == currentPlaylistIndex
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isCurrent) DarkSurfaceVariant else Color.Transparent)
                                    .clickable { viewModel.playPlaylistItem(index) }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isCurrent) {
                                        Icon(
                                            Icons.Filled.PlayArrow,
                                            contentDescription = "Playing",
                                            tint = AccentGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else {
                                        Text(
                                            text = "${index + 1}.",
                                            fontSize = 12.sp,
                                            color = TextSecondary,
                                            modifier = Modifier.width(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = item.title,
                                        fontSize = 13.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isCurrent) AccentPrimary else TextPrimary,
                                        maxLines = 1
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.removeFromPlaylist(index) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Remove",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

