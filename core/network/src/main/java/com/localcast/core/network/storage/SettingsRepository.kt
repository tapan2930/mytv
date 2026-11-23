package com.localcast.core.network.storage

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

data class LastPlayedMedia(
    val id: String,
    val title: String,
    val streamUrl: String,
    val positionMs: Long,
    val durationMs: Long,
    val mimeType: String
)

@Serializable
data class PlayedMediaItem(
    val id: String,
    val title: String,
    val streamUrl: String,
    val positionMs: Long,
    val durationMs: Long,
    val mimeType: String,
    val serverHost: String = "",
    val serverName: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

private val Context.dataStore by preferencesDataStore(name = "local_cast_prefs")

class SettingsRepository(private val context: Context) {

    companion object {
        val KEY_PORT = intPreferencesKey("server_port")
        val KEY_PASSWORD = stringPreferencesKey("server_password")
        val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
        val KEY_SAVED_SERVER_HOST = stringPreferencesKey("saved_server_host")
        val KEY_SAVED_SERVER_PORT = intPreferencesKey("saved_server_port")
        val KEY_SAVED_SERVER_NAME = stringPreferencesKey("saved_server_name")
        val KEY_DEVICE_ID = stringPreferencesKey("device_id")
        val KEY_DEVICE_NAME = stringPreferencesKey("device_name")

        val KEY_AUTO_FULLSCREEN = booleanPreferencesKey("auto_fullscreen")
        val KEY_PREFERRED_PLAYER = stringPreferencesKey("preferred_player") // "builtin" or "external"

        val KEY_LAST_PLAYED_ID = stringPreferencesKey("last_played_id")
        val KEY_LAST_PLAYED_TITLE = stringPreferencesKey("last_played_title")
        val KEY_LAST_PLAYED_URL = stringPreferencesKey("last_played_url")
        val KEY_LAST_PLAYED_POS = longPreferencesKey("last_played_pos")
        val KEY_LAST_PLAYED_DURATION = longPreferencesKey("last_played_duration")
        val KEY_LAST_PLAYED_MIME = stringPreferencesKey("last_played_mime")

        val KEY_PLAYED_MEDIA_HISTORY = stringPreferencesKey("played_media_history_json")

        val KEY_MEDIA_SCAN_DIR_PATH = stringPreferencesKey("media_scan_dir_path")
        val KEY_MEDIA_SCAN_DIR_NAME = stringPreferencesKey("media_scan_dir_name")
        val KEY_MEDIA_SCAN_DIR_URI = stringPreferencesKey("media_scan_dir_uri")
        val KEY_HAS_CONFIGURED_DIR = booleanPreferencesKey("has_configured_dir")
        val KEY_SCAN_VIDEOS = booleanPreferencesKey("scan_videos")
        val KEY_SCAN_AUDIOS = booleanPreferencesKey("scan_audios")
        val KEY_SCAN_PHOTOS = booleanPreferencesKey("scan_photos")
        val KEY_SCAN_DOCS = booleanPreferencesKey("scan_docs")

        const val DEFAULT_PORT = 8844
        const val DEFAULT_PASSWORD = "cast123"
        const val PLAYER_BUILTIN = "builtin"
        const val PLAYER_EXTERNAL = "external"
    }

    val serverPort: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_PORT] ?: DEFAULT_PORT
    }

    val serverPassword: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_PASSWORD] ?: DEFAULT_PASSWORD
    }

    val authToken: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTH_TOKEN]
    }

    val savedServerHost: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_SAVED_SERVER_HOST]
    }

    val savedServerPort: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_SAVED_SERVER_PORT] ?: DEFAULT_PORT
    }

    val savedServerName: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_SAVED_SERVER_NAME]
    }

    suspend fun getServerPort(): Int = serverPort.first()
    suspend fun getServerPassword(): String = serverPassword.first()
    suspend fun getAuthToken(): String? = authToken.first()

    suspend fun saveServerPort(port: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PORT] = port
        }
    }

    suspend fun saveServerPassword(password: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PASSWORD] = password
        }
    }

    suspend fun saveAuthToken(token: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTH_TOKEN] = token
        }
    }

    suspend fun clearAuthToken() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_AUTH_TOKEN)
        }
    }

    suspend fun savePairedServer(host: String, port: Int, deviceName: String? = null) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SAVED_SERVER_HOST] = host
            prefs[KEY_SAVED_SERVER_PORT] = port
            if (deviceName != null) {
                prefs[KEY_SAVED_SERVER_NAME] = deviceName
            }
        }
    }

    suspend fun getOrCreateDeviceId(): String {
        val current = context.dataStore.data.map { it[KEY_DEVICE_ID] }.first()
        if (current != null) return current
        val newId = UUID.randomUUID().toString()
        context.dataStore.edit { it[KEY_DEVICE_ID] = newId }
        return newId
    }

    suspend fun getDeviceName(default: String): String {
        return context.dataStore.data.map { it[KEY_DEVICE_NAME] ?: default }.first()
    }

    val autoFullscreen: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_FULLSCREEN] ?: true
    }

    val preferredPlayer: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_PREFERRED_PLAYER] ?: PLAYER_BUILTIN
    }

    val lastPlayedMedia: Flow<LastPlayedMedia?> = context.dataStore.data.map { prefs ->
        val id = prefs[KEY_LAST_PLAYED_ID]
        val title = prefs[KEY_LAST_PLAYED_TITLE]
        val url = prefs[KEY_LAST_PLAYED_URL]
        val pos = prefs[KEY_LAST_PLAYED_POS] ?: 0L
        val dur = prefs[KEY_LAST_PLAYED_DURATION] ?: 0L
        val mime = prefs[KEY_LAST_PLAYED_MIME] ?: "video/mp4"

        if (!id.isNullOrBlank() && !title.isNullOrBlank() && !url.isNullOrBlank()) {
            LastPlayedMedia(id, title, url, pos, dur, mime)
        } else {
            null
        }
    }

    suspend fun saveAutoFullscreen(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_FULLSCREEN] = enabled }
    }

    suspend fun savePreferredPlayer(player: String) {
        context.dataStore.edit { it[KEY_PREFERRED_PLAYER] = player }
    }

    suspend fun savePlaybackProgress(item: PlayedMediaItem) {
        context.dataStore.edit { prefs ->
            // Save last played
            prefs[KEY_LAST_PLAYED_ID] = item.id
            prefs[KEY_LAST_PLAYED_TITLE] = item.title
            prefs[KEY_LAST_PLAYED_URL] = item.streamUrl
            prefs[KEY_LAST_PLAYED_POS] = item.positionMs
            prefs[KEY_LAST_PLAYED_DURATION] = item.durationMs
            prefs[KEY_LAST_PLAYED_MIME] = item.mimeType

            // Save in history list
            val jsonStr = prefs[KEY_PLAYED_MEDIA_HISTORY]
            val currentList = if (jsonStr.isNullOrBlank()) {
                mutableListOf()
            } else {
                try {
                    jsonHelper.decodeFromString<List<PlayedMediaItem>>(jsonStr).toMutableList()
                } catch (_: Exception) {
                    mutableListOf()
                }
            }

            // Remove any existing entry matching this media id and server
            currentList.removeAll { existing ->
                existing.id == item.id && (
                    (item.serverHost.isNotBlank() && existing.serverHost == item.serverHost) ||
                    (item.serverName.isNotBlank() && existing.serverName == item.serverName)
                )
            }
            // Put the most recently played item at the front
            currentList.add(0, item)
            val trimmed = if (currentList.size > 30) currentList.take(30) else currentList
            prefs[KEY_PLAYED_MEDIA_HISTORY] = jsonHelper.encodeToString(trimmed)
        }
    }

    suspend fun saveLastPlayed(id: String, title: String, url: String, pos: Long, duration: Long, mime: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_PLAYED_ID] = id
            prefs[KEY_LAST_PLAYED_TITLE] = title
            prefs[KEY_LAST_PLAYED_URL] = url
            prefs[KEY_LAST_PLAYED_POS] = pos
            prefs[KEY_LAST_PLAYED_DURATION] = duration
            prefs[KEY_LAST_PLAYED_MIME] = mime
        }
    }

    suspend fun clearLastPlayed() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_LAST_PLAYED_ID)
            prefs.remove(KEY_LAST_PLAYED_TITLE)
            prefs.remove(KEY_LAST_PLAYED_URL)
            prefs.remove(KEY_LAST_PLAYED_POS)
            prefs.remove(KEY_LAST_PLAYED_DURATION)
            prefs.remove(KEY_LAST_PLAYED_MIME)
        }
    }

    private val jsonHelper = Json { ignoreUnknownKeys = true; isLenient = true }

    val allPlayedMediaHistory: Flow<List<PlayedMediaItem>> = context.dataStore.data.map { prefs ->
        val jsonStr = prefs[KEY_PLAYED_MEDIA_HISTORY]
        if (jsonStr.isNullOrBlank()) {
            emptyList()
        } else {
            try {
                jsonHelper.decodeFromString<List<PlayedMediaItem>>(jsonStr)
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    fun getPlayedMediaHistory(serverHost: String?, serverName: String?): Flow<List<PlayedMediaItem>> {
        return allPlayedMediaHistory.map { list ->
            if (serverHost.isNullOrBlank() && serverName.isNullOrBlank()) {
                list
            } else {
                list.filter { item ->
                    (!serverHost.isNullOrBlank() && item.serverHost.equals(serverHost, ignoreCase = true)) ||
                    (!serverName.isNullOrBlank() && !item.serverName.isNullOrBlank() && item.serverName.equals(serverName, ignoreCase = true))
                }
            }
        }
    }

    suspend fun savePlayedMediaItem(item: PlayedMediaItem) {
        context.dataStore.edit { prefs ->
            val jsonStr = prefs[KEY_PLAYED_MEDIA_HISTORY]
            val currentList = if (jsonStr.isNullOrBlank()) {
                mutableListOf()
            } else {
                try {
                    jsonHelper.decodeFromString<List<PlayedMediaItem>>(jsonStr).toMutableList()
                } catch (_: Exception) {
                    mutableListOf()
                }
            }

            // Remove any existing entry matching this media id and server
            currentList.removeAll { existing ->
                existing.id == item.id && (
                    (item.serverHost.isNotBlank() && existing.serverHost == item.serverHost) ||
                    (item.serverName.isNotBlank() && existing.serverName == item.serverName)
                )
            }
            // Put the most recently played item at the front
            currentList.add(0, item)
            val trimmed = if (currentList.size > 30) currentList.take(30) else currentList
            prefs[KEY_PLAYED_MEDIA_HISTORY] = jsonHelper.encodeToString(trimmed)
        }
    }

    suspend fun clearPlayedMediaHistory(serverHost: String? = null, serverName: String? = null) {
        context.dataStore.edit { prefs ->
            if (serverHost.isNullOrBlank() && serverName.isNullOrBlank()) {
                prefs.remove(KEY_PLAYED_MEDIA_HISTORY)
            } else {
                val jsonStr = prefs[KEY_PLAYED_MEDIA_HISTORY]
                if (!jsonStr.isNullOrBlank()) {
                    try {
                        val currentList = jsonHelper.decodeFromString<List<PlayedMediaItem>>(jsonStr)
                        val filtered = currentList.filterNot { item ->
                            (!serverHost.isNullOrBlank() && item.serverHost.equals(serverHost, ignoreCase = true)) ||
                            (!serverName.isNullOrBlank() && !item.serverName.isNullOrBlank() && item.serverName.equals(serverName, ignoreCase = true))
                        }
                        prefs[KEY_PLAYED_MEDIA_HISTORY] = jsonHelper.encodeToString(filtered)
                    } catch (_: Exception) {
                        prefs.remove(KEY_PLAYED_MEDIA_HISTORY)
                    }
                }
            }
        }
    }

    val mediaScanDirPath: Flow<String> = context.dataStore.data.map { it[KEY_MEDIA_SCAN_DIR_PATH] ?: "" }
    val mediaScanDirName: Flow<String> = context.dataStore.data.map { it[KEY_MEDIA_SCAN_DIR_NAME] ?: "Entire Device" }
    val mediaScanDirUri: Flow<String> = context.dataStore.data.map { it[KEY_MEDIA_SCAN_DIR_URI] ?: "" }
    val hasConfiguredDir: Flow<Boolean> = context.dataStore.data.map { it[KEY_HAS_CONFIGURED_DIR] ?: false }
    val scanVideos: Flow<Boolean> = context.dataStore.data.map { it[KEY_SCAN_VIDEOS] ?: true }
    val scanAudios: Flow<Boolean> = context.dataStore.data.map { it[KEY_SCAN_AUDIOS] ?: true }
    val scanPhotos: Flow<Boolean> = context.dataStore.data.map { it[KEY_SCAN_PHOTOS] ?: true }
    val scanDocs: Flow<Boolean> = context.dataStore.data.map { it[KEY_SCAN_DOCS] ?: true }

    suspend fun saveMediaScanDir(path: String, name: String, uri: String = "") {
        context.dataStore.edit { prefs ->
            prefs[KEY_MEDIA_SCAN_DIR_PATH] = path
            prefs[KEY_MEDIA_SCAN_DIR_NAME] = name
            prefs[KEY_MEDIA_SCAN_DIR_URI] = uri
            prefs[KEY_HAS_CONFIGURED_DIR] = true
        }
    }

    suspend fun saveScanPreferences(videos: Boolean, audios: Boolean, photos: Boolean, docs: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SCAN_VIDEOS] = videos
            prefs[KEY_SCAN_AUDIOS] = audios
            prefs[KEY_SCAN_PHOTOS] = photos
            prefs[KEY_SCAN_DOCS] = docs
            prefs[KEY_HAS_CONFIGURED_DIR] = true
        }
    }

    suspend fun setHasConfiguredDir(configured: Boolean) {
        context.dataStore.edit { it[KEY_HAS_CONFIGURED_DIR] = configured }
    }

    suspend fun saveDeviceName(name: String) {
        context.dataStore.edit { it[KEY_DEVICE_NAME] = name }
    }
}
