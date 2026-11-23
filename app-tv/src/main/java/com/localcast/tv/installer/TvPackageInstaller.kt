package com.localcast.tv.installer

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import com.localcast.core.model.InstallStatus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File

data class ApkInstallEvent(
    val packageName: String,
    val status: InstallStatus,
    val message: String? = null
)

object InstallEventsBus {
    private val _events = MutableSharedFlow<ApkInstallEvent>(extraBufferCapacity = 10)
    val events: SharedFlow<ApkInstallEvent> = _events.asSharedFlow()

    fun emit(event: ApkInstallEvent) {
        _events.tryEmit(event)
    }
}

class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        val packageName = intent.getStringExtra("packageName") ?: "unknown"

        Log.d("InstallResultReceiver", "PackageInstaller callback for $packageName: status=$status, message=$message")

        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                confirmIntent?.let {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(it)
                }
                InstallEventsBus.emit(ApkInstallEvent(packageName, InstallStatus.PROMPTING_USER, "Prompting user on TV screen"))
            }
            PackageInstaller.STATUS_SUCCESS -> {
                InstallEventsBus.emit(ApkInstallEvent(packageName, InstallStatus.INSTALLED, "Successfully installed!"))
            }
            else -> {
                InstallEventsBus.emit(ApkInstallEvent(packageName, InstallStatus.FAILED, message ?: "Installation failed (code $status)"))
            }
        }
    }
}

class TvPackageInstaller(private val context: Context) {

    companion object {
        private const val TAG = "TvPackageInstaller"
    }

    fun getInstalledVersionCode(packageName: String): Long? {
        return try {
            val pi = context.packageManager.getPackageInfo(packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pi.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pi.versionCode.toLong()
            }
        } catch (_: Exception) {
            null
        }
    }

    fun getApkVersionInfo(file: File): Pair<Long, String>? {
        return try {
            val pi = context.packageManager.getPackageArchiveInfo(file.absolutePath, 0) ?: return null
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pi.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pi.versionCode.toLong()
            }
            val name = pi.versionName ?: ""
            Pair(code, name)
        } catch (_: Exception) {
            null
        }
    }

    fun installApk(file: File, packageName: String): Boolean {
        return try {
            val packageInstaller = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                setAppPackageName(packageName)
                setSize(file.length())
            }
            val sessionId = packageInstaller.createSession(params)
            val session = packageInstaller.openSession(sessionId)

            file.inputStream().use { input ->
                session.openWrite("package_$packageName", 0, file.length()).use { output ->
                    input.copyTo(output)
                    session.fsync(output)
                }
            }

            val intent = Intent(context, InstallResultReceiver::class.java).apply {
                putExtra("packageName", packageName)
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getBroadcast(context, sessionId, intent, flags)

            InstallEventsBus.emit(ApkInstallEvent(packageName, InstallStatus.VERIFYING, "Verifying package on TV..."))
            session.commit(pendingIntent.intentSender)
            session.close()
            true
        } catch (e: Exception) {
            Log.e(TAG, "PackageInstaller session failed, falling back to Intent", e)
            installViaIntentFallback(file)
        }
    }

    fun installViaIntentFallback(file: File): Boolean {
        return try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Intent installation failed", e)
            false
        }
    }
}
