package com.localcast.core.network.discovery

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

class SubnetFastProbe {

    companion object {
        private const val TAG = "SubnetFastProbe"
    }

    suspend fun probeSubnet(
        localIp: String,
        port: Int = 8844,
        onDeviceFound: (DiscoveredService) -> Unit
    ): List<DiscoveredService> = withContext(Dispatchers.IO) {
        val parts = localIp.split(".")
        if (parts.size != 4) return@withContext emptyList()

        val prefix = "${parts[0]}.${parts[1]}.${parts[2]}."
        val myLastOctet = parts[3].toIntOrNull() ?: -1

        val foundList = mutableListOf<DiscoveredService>()
        val semaphore = kotlinx.coroutines.sync.Semaphore(32)

        coroutineScope {
            val deferreds = (1..254).filter { it != myLastOctet }.map { i ->
                async {
                    semaphore.withPermit {
                        val targetIp = "$prefix$i"
                        try {
                            // Quick TCP probe with 600ms timeout
                            Socket().use { socket ->
                                socket.connect(InetSocketAddress(targetIp, port), 600)
                            }

                            // If TCP handshake succeeds, verify LocalCast service via /api/status HTTP endpoint
                            val url = URL("http://$targetIp:$port/api/status")
                            val conn = (url.openConnection() as HttpURLConnection).apply {
                                connectTimeout = 800
                                readTimeout = 800
                                requestMethod = "GET"
                            }
                            if (conn.responseCode == 200) {
                                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                                val json = JSONObject(responseText)
                                if (json.optString("service") == "LocalCastPhoneServer") {
                                    val devName = json.optString("deviceName").ifBlank { "Phone ($targetIp)" }
                                    val service = DiscoveredService(
                                        serviceName = "Subnet-$targetIp",
                                        host = InetAddress.getByName(targetIp),
                                        port = port,
                                        deviceName = devName,
                                        deviceId = "subnet_$targetIp"
                                    )
                                    synchronized(foundList) {
                                        foundList.add(service)
                                    }
                                    onDeviceFound(service)
                                    Log.d(TAG, "Subnet probe successfully found phone: $devName at $targetIp:$port")
                                }
                            }
                        } catch (_: Exception) {
                            // Host unassigned, port closed, or timed out
                        }
                    }
                }
            }
            deferreds.awaitAll()
        }

        foundList
    }
}
