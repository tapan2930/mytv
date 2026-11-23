package com.localcast.tv.installer

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.localcast.core.model.SocketMessage
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

class TvApkDownloadManager(
    private val context: Context,
    private val httpClient: HttpClient
) {
    companion object {
        private const val TAG = "TvApkDownloadManager"
    }

    fun getDownloadDirectory(): File {
        val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (publicDir != null) {
            try {
                if (!publicDir.exists()) publicDir.mkdirs()
                val testFile = File(publicDir, ".probe_${System.currentTimeMillis()}")
                if (testFile.createNewFile()) {
                    testFile.delete()
                    return publicDir
                }
            } catch (_: Exception) {}
        }
        return File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir, "downloaded_apks").apply { mkdirs() }
    }

    private fun indexApkInPublicDownloads(file: File, displayName: String) {
        try {
            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                arrayOf("application/vnd.android.package-archive")
            ) { path, uri ->
                Log.d(TAG, "Indexed APK in MediaStore: $path -> $uri")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to scan file with MediaScannerConnection", e)
        }

        // On Android 10+ (Q+), if file was saved in app-private dir because public dir direct write failed,
        // copy into MediaStore.Downloads so user sees it in TV Download folder!
        val publicDownloadsPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)?.absolutePath
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && (publicDownloadsPath == null || !file.absolutePath.startsWith(publicDownloadsPath))) {
            try {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/vnd.android.package-archive")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        file.inputStream().use { input -> input.copyTo(out) }
                    }
                    Log.d(TAG, "Copied APK to public MediaStore.Downloads: $uri")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not insert into MediaStore.Downloads", e)
            }
        }
    }

    suspend fun downloadAndVerify(
        host: String,
        port: Int,
        offer: SocketMessage.ApkTransferOffer,
        onProgress: (progress: Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val downloadUrl = "http://$host:$port/api/apk/download/${offer.packageName}"
        val downloadDir = getDownloadDirectory()
        val cleanAppName = offer.appName.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
        val apkFileName = "${cleanAppName}_${offer.packageName}.apk"
        val targetFile = File(downloadDir, apkFileName)
        val tempFile = File(downloadDir, "${cleanAppName}_${offer.packageName}_${System.currentTimeMillis()}.tmp")

        try {
            downloadDir.listFiles { f -> f.name.endsWith(".tmp") }?.forEach { it.delete() }

            val response = httpClient.get(downloadUrl)
            val channel: ByteReadChannel = response.bodyAsChannel()
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)

            var totalBytes = 0L
            var lastUpdate = System.currentTimeMillis()

            channel.toInputStream().use { input ->
                FileOutputStream(tempFile).use { fos ->
                    var read = input.read(buffer)
                    while (read != -1) {
                        fos.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        totalBytes += read
                        val now = System.currentTimeMillis()
                        if (now - lastUpdate > 300 && offer.sizeBytes > 0) {
                            lastUpdate = now
                            val progress = (totalBytes.toFloat() / offer.sizeBytes.toFloat()).coerceIn(0.05f, 0.99f)
                            onProgress(progress)
                        }
                        read = input.read(buffer)
                    }
                    fos.flush()
                }
            }

            val actualSha = digest.digest().joinToString("") { "%02x".format(it) }
            if (offer.sha256.isNotBlank() && !offer.sha256.equals(actualSha, ignoreCase = true)) {
                tempFile.delete()
                return@withContext Result.failure(IllegalStateException("Checksum mismatch on TV (expected ${offer.sha256}, got $actualSha)"))
            }

            if (targetFile.exists()) {
                targetFile.delete()
            }
            val finalFile = if (tempFile.renameTo(targetFile)) {
                targetFile
            } else {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
                targetFile
            }

            indexApkInPublicDownloads(finalFile, apkFileName)

            Result.success(finalFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading APK", e)
            try { tempFile.delete() } catch (_: Exception) {}
            Result.failure(e)
        }
    }
}
