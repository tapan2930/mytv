package com.localcast.phone.data

import android.content.ContentUris
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.localcast.core.model.MediaItem
import com.localcast.core.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaRepository(private val context: Context) {

    private val videoExtensions = setOf("mp4", "mkv", "avi", "mov", "webm", "3gp", "flv", "wmv", "m4v")
    private val audioExtensions = setOf("mp3", "flac", "wav", "m4a", "aac", "ogg", "opus", "wma", "oga")
    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "gif", "heic", "heif", "bmp")
    private val docExtensions = setOf("pdf", "txt", "epub", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "csv", "json")

    private fun scanDirectoryFallback(
        dir: File,
        extensions: Set<String>,
        seenPaths: MutableSet<String>,
        mediaType: MediaType,
        maxDepth: Int = 3,
        mimeResolver: (ext: String) -> String
    ): List<MediaItem> {
        val list = mutableListOf<MediaItem>()
        try {
            if (dir.exists() && dir.isDirectory) {
                dir.walkTopDown().maxDepth(maxDepth).forEach { file ->
                    if (file.isFile && file.canRead()) {
                        val ext = file.extension.lowercase()
                        if (ext in extensions && seenPaths.add(file.absolutePath)) {
                            list.add(
                                MediaItem(
                                    id = file.absolutePath.hashCode().toString(),
                                    title = if (mediaType == MediaType.DOCUMENT) file.name else file.nameWithoutExtension,
                                    uri = Uri.fromFile(file).toString(),
                                    mimeType = mimeResolver(ext),
                                    mediaType = mediaType,
                                    sizeBytes = file.length()
                                )
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return list
    }

    private inline fun queryMediaStore(
        contentUri: Uri,
        projection: Array<String>,
        folderPath: String? = null,
        sortOrder: String = "${MediaStore.MediaColumns.DATE_ADDED} DESC",
        seenPaths: MutableSet<String>,
        customSelection: String? = null,
        customSelectionArgs: Array<String>? = null,
        crossinline itemMapper: (cursor: Cursor, id: Long, uri: String) -> MediaItem?
    ): List<MediaItem> {
        val list = mutableListOf<MediaItem>()
        val hasFolder = !folderPath.isNullOrBlank() && !folderPath.equals("ALL", ignoreCase = true)
        val selection = when {
            hasFolder && customSelection != null -> "($customSelection) AND ${MediaStore.MediaColumns.DATA} LIKE ?"
            hasFolder -> "${MediaStore.MediaColumns.DATA} LIKE ?"
            customSelection != null -> customSelection
            else -> null
        }
        val selectionArgs = when {
            hasFolder && customSelectionArgs != null -> customSelectionArgs + "$folderPath%"
            hasFolder -> arrayOf("$folderPath%")
            customSelectionArgs != null -> customSelectionArgs
            else -> null
        }

        try {
            val cursor = context.contentResolver.query(
                contentUri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndex(MediaStore.MediaColumns._ID)
                val dataCol = c.getColumnIndex(MediaStore.MediaColumns.DATA)

                while (c.moveToNext()) {
                    val id = if (idCol != -1) c.getLong(idCol) else 0L
                    val path = if (dataCol != -1) c.getString(dataCol) else null
                    if (!path.isNullOrBlank() && !seenPaths.add(path)) {
                        continue
                    }
                    val directFile = if (!path.isNullOrBlank()) File(path) else null
                    val uri = if (directFile != null && directFile.exists() && directFile.canRead()) {
                        Uri.fromFile(directFile).toString()
                    } else if (idCol != -1) {
                        ContentUris.withAppendedId(contentUri, id).toString()
                    } else {
                        path ?: ""
                    }

                    itemMapper(c, id, uri)?.let { list.add(it) }
                }
            }
        } catch (_: Exception) {}

        return list
    }

    suspend fun getVideos(folderPath: String? = null): List<MediaItem> = withContext(Dispatchers.IO) {
        val seenPaths = mutableSetOf<String>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATA
        )

        val list = queryMediaStore(
            contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection = projection,
            folderPath = folderPath,
            seenPaths = seenPaths
        ) { c, id, uri ->
            val titleCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
            val mimeCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val durationCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

            MediaItem(
                id = id.toString(),
                title = c.getString(titleCol) ?: "Video $id",
                uri = uri,
                mimeType = c.getString(mimeCol) ?: "video/mp4",
                mediaType = MediaType.VIDEO,
                sizeBytes = c.getLong(sizeCol),
                durationMs = c.getLong(durationCol)
            )
        }.toMutableList()

        val hasFolder = !folderPath.isNullOrBlank() && !folderPath.equals("ALL", ignoreCase = true)
        if (hasFolder) {
            list.addAll(
                scanDirectoryFallback(File(folderPath!!), videoExtensions, seenPaths, MediaType.VIDEO) { ext ->
                    "video/${if (ext == "mkv") "x-matroska" else ext}"
                }
            )
        }

        list
    }

    suspend fun getAudios(folderPath: String? = null): List<MediaItem> = withContext(Dispatchers.IO) {
        val seenPaths = mutableSetOf<String>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA
        )

        val list = queryMediaStore(
            contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection = projection,
            folderPath = folderPath,
            seenPaths = seenPaths
        ) { c, id, uri ->
            val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val mimeCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)

            MediaItem(
                id = id.toString(),
                title = c.getString(titleCol) ?: "Audio $id",
                uri = uri,
                mimeType = c.getString(mimeCol) ?: "audio/mpeg",
                mediaType = MediaType.AUDIO,
                sizeBytes = c.getLong(sizeCol),
                durationMs = c.getLong(durationCol),
                artist = c.getString(artistCol)
            )
        }.toMutableList()

        val hasFolder = !folderPath.isNullOrBlank() && !folderPath.equals("ALL", ignoreCase = true)
        if (hasFolder) {
            list.addAll(
                scanDirectoryFallback(File(folderPath!!), audioExtensions, seenPaths, MediaType.AUDIO) { ext ->
                    "audio/$ext"
                }
            )
        }

        list
    }

    suspend fun getPhotos(folderPath: String? = null): List<MediaItem> = withContext(Dispatchers.IO) {
        val seenPaths = mutableSetOf<String>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.TITLE,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.DATA
        )

        val list = queryMediaStore(
            contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection = projection,
            folderPath = folderPath,
            seenPaths = seenPaths
        ) { c, id, uri ->
            val titleCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.TITLE)
            val mimeCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val widthCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)

            MediaItem(
                id = id.toString(),
                title = c.getString(titleCol) ?: "Photo $id",
                uri = uri,
                mimeType = c.getString(mimeCol) ?: "image/jpeg",
                mediaType = MediaType.IMAGE,
                sizeBytes = c.getLong(sizeCol),
                width = c.getInt(widthCol),
                height = c.getInt(heightCol)
            )
        }.toMutableList()

        val hasFolder = !folderPath.isNullOrBlank() && !folderPath.equals("ALL", ignoreCase = true)
        if (hasFolder) {
            list.addAll(
                scanDirectoryFallback(File(folderPath!!), imageExtensions, seenPaths, MediaType.IMAGE) { ext ->
                    "image/${if (ext == "jpg") "jpeg" else ext}"
                }
            )
        }

        list
    }

    suspend fun getDocuments(folderPath: String? = null): List<MediaItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MediaItem>()
        val seenPaths = mutableSetOf<String>()
        val hasFolder = !folderPath.isNullOrBlank() && !folderPath.equals("ALL", ignoreCase = true)

        // 1. Direct file walk if folderPath exists or default Documents dirs
        if (hasFolder) {
            list.addAll(
                scanDirectoryFallback(File(folderPath!!), docExtensions, seenPaths, MediaType.DOCUMENT) { ext ->
                    getMimeTypeForDoc(ext)
                }
            )
        } else {
            val searchDirs = listOfNotNull(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                File(Environment.getExternalStorageDirectory(), "Documents")
            )
            for (dir in searchDirs) {
                list.addAll(
                    scanDirectoryFallback(dir, docExtensions, seenPaths, MediaType.DOCUMENT, maxDepth = 2) { ext ->
                        getMimeTypeForDoc(ext)
                    }
                )
            }
        }

        // 2. MediaStore.Files query
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.TITLE,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATA
        )
        val baseSelection = "${MediaStore.Files.FileColumns.MIME_TYPE} = 'application/pdf' OR ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'text/%'"

        list.addAll(
            queryMediaStore(
                contentUri = MediaStore.Files.getContentUri("external"),
                projection = projection,
                folderPath = folderPath,
                sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC",
                seenPaths = seenPaths,
                customSelection = baseSelection
            ) { c, id, uri ->
                val nameCol = c.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val titleCol = c.getColumnIndex(MediaStore.Files.FileColumns.TITLE)
                val mimeCol = c.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)
                val sizeCol = c.getColumnIndex(MediaStore.Files.FileColumns.SIZE)

                val name = (if (nameCol != -1) c.getString(nameCol) else null)
                    ?: (if (titleCol != -1) c.getString(titleCol) else null)
                    ?: "Document $id"
                val mime = (if (mimeCol != -1) c.getString(mimeCol) else null) ?: "application/pdf"
                val size = if (sizeCol != -1) c.getLong(sizeCol) else 0L

                MediaItem(
                    id = id.toString(),
                    title = name,
                    uri = uri,
                    mimeType = mime,
                    mediaType = MediaType.DOCUMENT,
                    sizeBytes = size
                )
            }
        )

        list
    }

    private fun getMimeTypeForDoc(extension: String): String {
        return when (extension.lowercase()) {
            "pdf" -> "application/pdf"
            "txt" -> "text/plain"
            "epub" -> "application/epub+zip"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "ppt" -> "application/vnd.ms-powerpoint"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "csv" -> "text/csv"
            "json" -> "application/json"
            else -> "application/octet-stream"
        }
    }

    suspend fun getStorageApkFiles(): List<MediaItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MediaItem>()
        val seenPaths = mutableSetOf<String>()
        val pm = context.packageManager

        // 1. Scan public Download and Documents directories
        val searchDirs = listOfNotNull(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            File(Environment.getExternalStorageDirectory(), "Download"),
            File(Environment.getExternalStorageDirectory(), "Downloads")
        )

        for (dir in searchDirs) {
            try {
                if (dir.exists() && dir.isDirectory) {
                    val apkFiles = dir.listFiles { file ->
                        file.isFile && file.name.endsWith(".apk", ignoreCase = true)
                    }
                    if (apkFiles != null) {
                        for (file in apkFiles) {
                            if (seenPaths.add(file.absolutePath)) {
                                createApkMediaItem(file, pm)?.let { list.add(it) }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // 2. Query MediaStore.Files for any other indexed APKs
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.SIZE
        )
        val selection = "${MediaStore.Files.FileColumns.DATA} LIKE '%.apk' OR ${MediaStore.Files.FileColumns.MIME_TYPE} = ?"
        val selectionArgs = arrayOf("application/vnd.android.package-archive")

        list.addAll(
            queryMediaStore(
                contentUri = MediaStore.Files.getContentUri("external"),
                projection = projection,
                folderPath = null,
                sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC",
                seenPaths = seenPaths,
                customSelection = selection,
                customSelectionArgs = selectionArgs
            ) { _, _, uri ->
                val file = if (uri.startsWith("file://")) File(Uri.parse(uri).path ?: "") else File(uri)
                if (file.exists() && file.canRead()) {
                    createApkMediaItem(file, pm)
                } else null
            }
        )

        list
    }

    private fun createApkMediaItem(file: File, pm: PackageManager): MediaItem? {
        return try {
            val pkgInfo = pm.getPackageArchiveInfo(file.absolutePath, 0)
            val appInfo = pkgInfo?.applicationInfo
            if (appInfo != null) {
                appInfo.sourceDir = file.absolutePath
                appInfo.publicSourceDir = file.absolutePath
            }
            val label = appInfo?.loadLabel(pm)?.toString()
                ?: pkgInfo?.packageName
                ?: file.nameWithoutExtension

            val packageName = pkgInfo?.packageName ?: file.nameWithoutExtension
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkgInfo?.longVersionCode ?: 1L
            } else {
                @Suppress("DEPRECATION")
                pkgInfo?.versionCode?.toLong() ?: 1L
            }
            val versionName = pkgInfo?.versionName ?: "1.0"

            MediaItem(
                id = "${packageName}_${file.name}_${file.length()}",
                title = label,
                uri = Uri.fromFile(file).toString(),
                mimeType = "application/vnd.android.package-archive",
                mediaType = MediaType.APK,
                sizeBytes = file.length(),
                artist = "v$versionName (Build $versionCode) • ${file.name}",
                album = packageName
            )
        } catch (_: Exception) {
            null
        }
    }

    fun getApkMediaItemFromUri(uri: Uri): MediaItem? {
        return try {
            val file = getFileFromUri(uri.toString(), forceFresh = true) ?: return null
            createApkMediaItem(file, context.packageManager)
        } catch (_: Exception) {
            null
        }
    }

    fun getFileFromUri(uriString: String, forceFresh: Boolean = false): File? {
        val uri = Uri.parse(uriString)
        if (uri.scheme == "file") {
            val f = File(uri.path ?: return null)
            if (f.exists() && f.canRead()) return f
        }

        // Try direct file path from ContentResolver
        try {
            val proj = arrayOf(MediaStore.MediaColumns.DATA)
            context.contentResolver.query(uri, proj, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                    if (idx != -1) {
                        val path = cursor.getString(idx)
                        if (!path.isNullOrBlank()) {
                            val directFile = File(path)
                            if (directFile.exists() && directFile.canRead()) {
                                return directFile
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // For SAF / content URIs: query display name and size from content resolver
        var displayName = uri.lastPathSegment?.replace('/', '_') ?: "file"
        var contentSize = -1L
        try {
            val proj = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.SIZE)
            context.contentResolver.query(uri, proj, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                    if (nameIdx != -1) cursor.getString(nameIdx)?.let { displayName = it }
                    if (sizeIdx != -1) contentSize = cursor.getLong(sizeIdx)
                }
            }
        } catch (_: Exception) {}

        val cleanName = displayName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        val sizePart = if (contentSize > 0) contentSize.toString() else uri.hashCode().toString()
        val fileName = "stream_${cleanName}_$sizePart.tmp"
        val cacheFile = File(context.cacheDir, fileName)

        if (!forceFresh && cacheFile.exists() && (contentSize <= 0 || cacheFile.length() == contentSize)) {
            return cacheFile
        }

        if (forceFresh && cacheFile.exists()) {
            cacheFile.delete()
        }

        // Copy to temp file, then rename atomically
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val tempFile = File(context.cacheDir, "$fileName.part")
            tempFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }
            if (tempFile.renameTo(cacheFile)) {
                cacheFile
            } else {
                tempFile
            }
        } catch (_: Exception) {
            null
        }
    }

    fun resolveTreeUriToPath(uri: Uri): Pair<String, String> {
        var path = ""
        var name = uri.lastPathSegment ?: "Selected Folder"
        try {
            if (DocumentsContract.isTreeUri(uri)) {
                val docId = DocumentsContract.getTreeDocumentId(uri)
                if (docId.startsWith("primary:")) {
                    val rel = docId.substringAfter("primary:")
                    path = if (rel.isEmpty()) {
                        Environment.getExternalStorageDirectory().absolutePath
                    } else {
                        File(Environment.getExternalStorageDirectory(), rel).absolutePath
                    }
                    name = if (rel.isEmpty()) "Entire Storage" else rel.substringAfterLast('/')
                } else if (docId.startsWith("raw:")) {
                    path = docId.substringAfter("raw:")
                    name = path.substringAfterLast('/')
                } else if (docId.contains(":")) {
                    val parts = docId.split(":")
                    val volumeId = parts[0]
                    val rel = parts.getOrElse(1) { "" }
                    val sdCard = File("/storage/$volumeId")
                    path = if (sdCard.exists()) File(sdCard, rel).absolutePath else "/storage/$volumeId/$rel"
                    name = if (rel.isNotEmpty()) rel.substringAfterLast('/') else volumeId
                }
            }
        } catch (_: Exception) {}
        if (path.isEmpty()) {
            path = uri.path ?: ""
        }
        return Pair(path, name)
    }
}
