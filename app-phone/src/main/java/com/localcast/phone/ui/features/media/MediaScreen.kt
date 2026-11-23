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
fun MediaScreen(viewModel: PhoneMainViewModel) {
    val subTab by viewModel.mediaSubTab.collectAsState()
    val videos by viewModel.videos.collectAsState()
    val audios by viewModel.audios.collectAsState()
    val photos by viewModel.photos.collectAsState()
    val documents by viewModel.documents.collectAsState()
    val currentDirName by viewModel.mediaScanDirName.collectAsState("Entire Device")
    val searchQuery by viewModel.searchQuery.collectAsState()

    val scanVideos by viewModel.scanVideos.collectAsState(true)
    val scanAudios by viewModel.scanAudios.collectAsState(true)
    val scanPhotos by viewModel.scanPhotos.collectAsState(true)
    val scanDocs by viewModel.scanDocs.collectAsState(true)

    var isSearchActive by remember { mutableStateOf(searchQuery.isNotBlank()) }
    val focusRequester = remember { FocusRequester() }

    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
        viewModel.setSearchQuery("")
    }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
        }
    }

    // Compute raw list by sub-tab
    val categoryList = when (subTab) {
        MediaSubTab.ALL -> videos + audios + photos + documents
        MediaSubTab.VIDEOS -> videos
        MediaSubTab.AUDIOS -> audios
        MediaSubTab.PHOTOS -> photos
        MediaSubTab.DOCUMENTS -> documents
    }

    // Apply instant search filter
    val filteredList = remember(categoryList, searchQuery) {
        if (searchQuery.isBlank()) {
            categoryList
        } else {
            val q = searchQuery.trim().lowercase()
            categoryList.filter {
                it.title.lowercase().contains(q) ||
                    (it.artist?.lowercase()?.contains(q) == true) ||
                    it.mimeType.lowercase().contains(q)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (isSearchActive) {
            // Active Search Screen / Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBackground)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        isSearchActive = false
                        viewModel.setSearchQuery("")
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Close Search",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .focusRequester(focusRequester),
                    placeholder = { Text("Search titles, artists, files...", fontSize = 13.sp, color = TextSecondary) },
                    singleLine = true,
                    shape = RoundedCornerShape(23.dp),
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentPrimary,
                        unfocusedBorderColor = DarkSurfaceVariant,
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurfaceVariant,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        } else {
            // Categories chips row with Search Icon on the left-most side and Filter Tune Icon on the right-most side
            val totalCount = videos.size + audios.size + photos.size + documents.size
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBackground)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left-most Search Icon Button
                Surface(
                    color = if (searchQuery.isNotBlank()) AccentPrimary else DarkSurfaceVariant,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.clickable { isSearchActive = true }
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search Media",
                                tint = if (searchQuery.isNotBlank()) Color.White else AccentPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Scrollable Category Chips
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        CategoryPill(
                            label = "All ($totalCount)",
                            isSelected = subTab == MediaSubTab.ALL,
                            onClick = { viewModel.selectMediaSubTab(MediaSubTab.ALL) }
                        )
                    }
                    if (scanVideos) {
                        item {
                            CategoryPill(
                                label = "Videos (${videos.size})",
                                isSelected = subTab == MediaSubTab.VIDEOS,
                                onClick = { viewModel.selectMediaSubTab(MediaSubTab.VIDEOS) }
                            )
                        }
                    }
                    if (scanAudios) {
                        item {
                            CategoryPill(
                                label = "Music (${audios.size})",
                                isSelected = subTab == MediaSubTab.AUDIOS,
                                onClick = { viewModel.selectMediaSubTab(MediaSubTab.AUDIOS) }
                            )
                        }
                    }
                    if (scanPhotos) {
                        item {
                            CategoryPill(
                                label = "Photos (${photos.size})",
                                isSelected = subTab == MediaSubTab.PHOTOS,
                                onClick = { viewModel.selectMediaSubTab(MediaSubTab.PHOTOS) }
                            )
                        }
                    }
                    if (scanDocs) {
                        item {
                            CategoryPill(
                                label = "Docs (${documents.size})",
                                isSelected = subTab == MediaSubTab.DOCUMENTS,
                                onClick = { viewModel.selectMediaSubTab(MediaSubTab.DOCUMENTS) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Right-most Folder & Media Filter Button
                Surface(
                    color = DarkSurfaceVariant,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.clickable { viewModel.openDirectoryConfigDialog() }
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = "Configure Folder & Media",
                                tint = AccentPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Media Item List or Empty State
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (searchQuery.isNotBlank()) Icons.Filled.Search else Icons.Filled.FolderOpen,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "No files match \"$searchQuery\"" else "No media found in $currentDirName",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "Try another keyword or clear search." else "Try choosing another folder or enable more media types in settings.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (searchQuery.isNotBlank()) {
                            Button(
                                onClick = {
                                    viewModel.setSearchQuery("")
                                    isSearchActive = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant)
                            ) {
                                Text("Clear Search", color = TextPrimary)
                            }
                        } else {
                            Button(
                                onClick = { viewModel.openDirectoryConfigDialog() },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                            ) {
                                Icon(Icons.Filled.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Change Folder")
                            }
                            Button(
                                onClick = { viewModel.loadMedia() },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant)
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Refresh", color = TextPrimary)
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredList, key = { it.id }) { item ->
                    MediaItemCard(
                        item = item,
                        onCast = { viewModel.castMedia(item) },
                        onAddToQueue = { viewModel.addToPlaylist(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) AccentPrimary else DarkSurfaceVariant,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else TextSecondary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun MediaItemCard(
    item: MediaItem,
    onCast: () -> Unit,
    onAddToQueue: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when (item.mediaType) {
                                MediaType.VIDEO -> AccentPrimary.copy(alpha = 0.15f)
                                MediaType.AUDIO -> AccentCyan.copy(alpha = 0.15f)
                                MediaType.IMAGE -> AccentGreen.copy(alpha = 0.15f)
                                MediaType.DOCUMENT -> Color(0xFFFFA726).copy(alpha = 0.15f)
                                else -> DarkSurfaceVariant
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (item.mediaType) {
                            MediaType.VIDEO -> Icons.Filled.Videocam
                            MediaType.AUDIO -> Icons.Filled.MusicNote
                            MediaType.IMAGE -> Icons.Filled.Photo
                            MediaType.DOCUMENT -> Icons.Filled.Description
                            else -> Icons.Filled.PlayArrow
                        },
                        contentDescription = null,
                        tint = when (item.mediaType) {
                            MediaType.VIDEO -> AccentPrimary
                            MediaType.AUDIO -> AccentCyan
                            MediaType.IMAGE -> AccentGreen
                            MediaType.DOCUMENT -> Color(0xFFFFA726)
                            else -> AccentPrimary
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = item.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val sizeMb = "%.1f MB".format(item.sizeBytes / (1024f * 1024f))
                    val durationStr = if (item.durationMs > 0) {
                        val totalSecs = item.durationMs / 1000
                        val mins = totalSecs / 60
                        val secs = totalSecs % 60
                        " • %02d:%02d".format(mins, secs)
                    } else ""
                    val extra = if (!item.artist.isNullOrBlank()) " • ${item.artist}" else ""
                    Text(
                        text = "$sizeMb$durationStr$extra",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Minimal Action Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Minimal Add to Queue Button
                IconButton(
                    onClick = onAddToQueue,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                        contentDescription = "Add to Queue",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Minimal Play Pill Button
                Surface(
                    color = AccentPrimary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.35f)),
                    modifier = Modifier.clickable { onCast() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Play",
                            tint = AccentPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Play",
                            fontSize = 12.sp,
                            color = AccentPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

