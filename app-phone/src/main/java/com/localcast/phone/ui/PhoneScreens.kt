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
fun PhoneMainScreen(viewModel: PhoneMainViewModel) {
    val context = LocalContext.current
    val selectedTab by viewModel.selectedTab.collectAsState()
    val isServerRunning by viewModel.isServerRunning.collectAsState()
    val serverIp by viewModel.serverIp.collectAsState()
    val serverPort by viewModel.serverPort.collectAsState()
    val isWifiConnected by viewModel.isWifiConnected.collectAsState()
    val connectedTv by viewModel.connectedTv.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val showDirDialog by viewModel.showDirectoryConfigDialog.collectAsState()

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    if (showDirDialog) {
        DirectoryConfigBottomSheet(
            viewModel = viewModel,
            onDismiss = { viewModel.closeDirectoryConfigDialog() }
        )
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            // Streamlined Compact Top Bar (~54dp) reclaiming ~70% screen height
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Brand & Live Status
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "My TV",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            if (connectedTv != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = AccentGreen.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "TV Connected",
                                        color = AccentGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        if (isServerRunning) {
                            Text(
                                text = if (connectedTv != null) {
                                    "${connectedTv?.deviceName} (${connectedTv?.model})"
                                } else {
                                    "${serverIp ?: "Connecting..."}:$serverPort • Waiting for TV"
                                },
                                fontSize = 11.sp,
                                color = if (connectedTv != null) AccentGreen else AccentCyan,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else if (!isWifiConnected) {
                            Text(
                                text = "Wi-Fi disconnected",
                                fontSize = 11.sp,
                                color = AccentRed,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Header Action Controls: Status Label beside Server Switch
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isServerRunning) "Streaming On" else "Streaming Off",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isServerRunning) AccentGreen else AccentRed
                    )

                    Switch(
                        checked = isServerRunning,
                        onCheckedChange = { viewModel.toggleServer() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AccentGreen,
                            checkedTrackColor = DarkSurfaceVariant,
                            uncheckedThumbColor = AccentRed,
                            uncheckedTrackColor = DarkSurfaceVariant
                        )
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = DarkSurface) {
                NavigationBarItem(
                    selected = selectedTab == PhoneTab.MEDIA,
                    onClick = { viewModel.selectTab(PhoneTab.MEDIA) },
                    icon = { Icon(Icons.Filled.Videocam, contentDescription = "Media") },
                    label = { Text("Media") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentPrimary,
                        indicatorColor = DarkSurfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == PhoneTab.REMOTE,
                    onClick = { viewModel.selectTab(PhoneTab.REMOTE) },
                    icon = { Icon(Icons.Filled.SettingsRemote, contentDescription = "Remote") },
                    label = { Text("Remote") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentPrimary,
                        indicatorColor = DarkSurfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == PhoneTab.APKS,
                    onClick = { viewModel.selectTab(PhoneTab.APKS) },
                    icon = { Icon(Icons.Filled.Android, contentDescription = "APKs") },
                    label = { Text("Install APK") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentGreen,
                        indicatorColor = DarkSurfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == PhoneTab.SETTINGS,
                    onClick = { viewModel.selectTab(PhoneTab.SETTINGS) },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TextPrimary,
                        indicatorColor = DarkSurfaceVariant
                    )
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                PhoneTab.MEDIA -> MediaScreen(viewModel)
                PhoneTab.REMOTE -> RemoteControlScreen(viewModel)
                PhoneTab.APKS -> ApkInstallScreen(viewModel)
                PhoneTab.SETTINGS -> SettingsScreen(viewModel)
            }
        }
    }
}

