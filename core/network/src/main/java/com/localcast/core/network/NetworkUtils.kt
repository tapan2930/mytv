package com.localcast.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

object NetworkUtils {

    fun isWifiConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    fun getWifiIpAddress(context: Context): String? {
        if (!isWifiConnected(context)) return null

        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (!intf.name.contains("wlan", ignoreCase = true) && !intf.name.contains("eth", ignoreCase = true)) {
                    continue
                }
                val addresses = Collections.list(intf.inetAddresses)
                for (addr in addresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (_: Exception) {
            // fallback
        }

        // Secondary fallback via ConnectivityManager LinkProperties or legacy WifiManager
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNetwork = cm?.activeNetwork
            val linkProps = activeNetwork?.let { cm.getLinkProperties(it) }
            val ipv4 = linkProps?.linkAddresses?.firstOrNull { it.address is Inet4Address && !it.address.isLoopbackAddress }?.address?.hostAddress
            if (!ipv4.isNullOrBlank()) {
                return ipv4
            }
            @Suppress("DEPRECATION")
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            @Suppress("DEPRECATION")
            val ipInt = wm?.connectionInfo?.ipAddress ?: 0
            if (ipInt != 0) {
                return String.format(
                    java.util.Locale.US,
                    "%d.%d.%d.%d",
                    ipInt and 0xff,
                    ipInt shr 8 and 0xff,
                    ipInt shr 16 and 0xff,
                    ipInt shr 24 and 0xff
                )
            }
        } catch (_: Exception) {}

        return null
    }

    fun isSameSubnet(ip1: String, ip2: String): Boolean {
        return try {
            val parts1 = ip1.split(".").map { it.toInt() }
            val parts2 = ip2.split(".").map { it.toInt() }
            if (parts1.size == 4 && parts2.size == 4) {
                // Check /24 subnet (first 3 octets match)
                parts1[0] == parts2[0] && parts1[1] == parts2[1] && parts1[2] == parts2[2]
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }
}
