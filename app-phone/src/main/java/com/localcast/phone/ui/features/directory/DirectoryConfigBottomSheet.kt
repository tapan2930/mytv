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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectoryConfigBottomSheet(
    viewModel: PhoneMainViewModel,
    onDismiss: () -> Unit
) {
    val currentDirPath by viewModel.mediaScanDirPath.collectAsState("")
    val currentDirName by viewModel.mediaScanDirName.collectAsState("Entire Device")
    val doVideos by viewModel.scanVideos.collectAsState(true)
    val doAudios by viewModel.scanAudios.collectAsState(true)
    val doPhotos by viewModel.scanPhotos.collectAsState(true)
    val doDocs by viewModel.scanDocs.collectAsState(true)
    val hasConfigured by viewModel.hasConfiguredDir.collectAsState(false)

    var selectedPath by remember(currentDirPath) { mutableStateOf(currentDirPath) }
    var selectedName by remember(currentDirName) { mutableStateOf(currentDirName) }

    var videoChecked by remember(doVideos) { mutableStateOf(doVideos) }
    var audioChecked by remember(doAudios) { mutableStateOf(doAudios) }
    var photoChecked by remember(doPhotos) { mutableStateOf(doPhotos) }
    var docChecked by remember(doDocs) { mutableStateOf(doDocs) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            viewModel.setMediaScanDirectoryFromUri(it)
        }
    }

    val presetFolders = remember {
        listOf(
            "Entire Device" to "",
            "Downloads" to (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)?.absolutePath ?: ""),
            "Movies & Videos" to (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)?.absolutePath ?: ""),
            "DCIM / Photos" to (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)?.absolutePath ?: ""),
            "Music" to (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)?.absolutePath ?: ""),
            "Documents" to (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)?.absolutePath ?: "")
        )
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = {
            if (hasConfigured) onDismiss()
        },
        sheetState = sheetState,
        containerColor = DarkSurface,
        scrimColor = Color.Black.copy(alpha = 0.6f),
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = TextSecondary.copy(alpha = 0.5f))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column {
                Text(
                    text = "Media Library & Folder Setup",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Choose which folder and media categories to scan and stream to TV",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            // Section 1: Choose Folder
            Text(
                text = "1. Scan Directory",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = AccentCyan
            )

            // Currently selected badge
            Surface(
                color = DarkSurfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = AccentPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedName.ifBlank { "Entire Device" },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        if (selectedPath.isNotBlank()) {
                            Text(
                                text = selectedPath,
                                fontSize = 10.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Presets list
            Text("Popular Locations:", fontSize = 12.sp, color = TextSecondary)
            presetFolders.forEach { (name, path) ->
                val isSelected = selectedPath == path
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) AccentPrimary.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable {
                            selectedPath = path
                            selectedName = name
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Filled.Check else Icons.Filled.Folder,
                        contentDescription = null,
                        tint = if (isSelected) AccentPrimary else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = name,
                        fontSize = 13.sp,
                        color = if (isSelected) AccentPrimary else TextPrimary
                    )
                }
            }

            // Custom Folder Picker Button
            OutlinedButton(
                onClick = { folderPickerLauncher.launch(null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentPrimary)
            ) {
                Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Select Custom Folder...", fontSize = 12.sp)
            }

            // Section 2: Media Types Selection
            Text(
                text = "2. Media Categories to Scan",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = AccentCyan
            )

            // Category Checkboxes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { videoChecked = !videoChecked },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = videoChecked,
                    onCheckedChange = { videoChecked = it },
                    colors = CheckboxDefaults.colors(checkedColor = AccentPrimary)
                )
                Icon(Icons.Filled.Videocam, contentDescription = null, tint = AccentPrimary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Videos (MP4, MKV, AVI, etc.)", fontSize = 13.sp, color = TextPrimary)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { audioChecked = !audioChecked },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = audioChecked,
                    onCheckedChange = { audioChecked = it },
                    colors = CheckboxDefaults.colors(checkedColor = AccentPrimary)
                )
                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Music & Audio (MP3, FLAC, WAV)", fontSize = 13.sp, color = TextPrimary)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { photoChecked = !photoChecked },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = photoChecked,
                    onCheckedChange = { photoChecked = it },
                    colors = CheckboxDefaults.colors(checkedColor = AccentPrimary)
                )
                Icon(Icons.Filled.Photo, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Photos & Images (JPG, PNG, WebP)", fontSize = 13.sp, color = TextPrimary)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { docChecked = !docChecked },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = docChecked,
                    onCheckedChange = { docChecked = it },
                    colors = CheckboxDefaults.colors(checkedColor = AccentPrimary)
                )
                Icon(Icons.Filled.Description, contentDescription = null, tint = Color(0xFFFFA726), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Documents (PDF, TXT, Docs)", fontSize = 13.sp, color = TextPrimary)
            }

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (hasConfigured) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }
                }

                Button(
                    onClick = {
                        viewModel.setScanPreferences(videoChecked, audioChecked, photoChecked, docChecked)
                        viewModel.setMediaScanDirectory(selectedPath, selectedName)
                    },
                    modifier = Modifier.weight(if (hasConfigured) 1.5f else 1f),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Save & Scan Library", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

