package com.localcast.core.network.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.LinkedList
import java.util.Queue

data class DiscoveredService(
    val serviceName: String,
    val host: InetAddress?,
    val port: Int,
    val deviceName: String = "",
    val deviceId: String = ""
)

class NsdAdvertiser(private val context: Context) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private var multicastLock: WifiManager.MulticastLock? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    var registeredServiceName: String? = null
        private set

    private var udpBroadcaster: UdpBeaconBroadcaster? = null

    companion object {
        const val SERVICE_TYPE = "_localcast._tcp"
        private const val TAG = "NsdAdvertiser"
    }

    fun startAdvertising(serviceName: String, port: Int, deviceName: String, deviceId: String, hostIp: String) {
        stopAdvertising()

        // Acquire MulticastLock to ensure mDNS broadcast packets leave the device
        try {
            multicastLock = wifiManager.createMulticastLock("LocalCast::AdvertiserMulticastLock").apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not acquire MulticastLock", e)
        }

        // Start UDP Beacon fallback
        udpBroadcaster = UdpBeaconBroadcaster()
        udpBroadcaster?.start(deviceName = deviceName, port = port, ip = hostIp)

        val serviceInfo = NsdServiceInfo().apply {
            this.serviceName = serviceName
            this.serviceType = SERVICE_TYPE
            this.port = port
            setAttribute("deviceName", deviceName)
            setAttribute("deviceId", deviceId)
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) {
                registeredServiceName = info.serviceName
                Log.d(TAG, "mDNS Service registered: ${info.serviceName} on port $port")
            }

            override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "mDNS Service registration failed: $errorCode")
            }

            override fun onServiceUnregistered(info: NsdServiceInfo) {
                Log.d(TAG, "mDNS Service unregistered: ${info.serviceName}")
                registeredServiceName = null
            }

            override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "mDNS Service unregistration failed: $errorCode")
            }
        }

        try {
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start advertising", e)
        }
    }

    fun stopAdvertising() {
        udpBroadcaster?.stop()
        udpBroadcaster = null

        registrationListener?.let {
            try {
                nsdManager.unregisterService(it)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to unregister NSD service", e)
            }
            registrationListener = null
            registeredServiceName = null
        }

        try {
            if (multicastLock?.isHeld == true) multicastLock?.release()
        } catch (_: Exception) {}
        multicastLock = null
    }
}

class NsdDiscoverer(private val context: Context) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private var multicastLock: WifiManager.MulticastLock? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    private val _services = MutableStateFlow<List<DiscoveredService>>(emptyList())
    val services: StateFlow<List<DiscoveredService>> = _services.asStateFlow()

    private val resolveQueue: Queue<NsdServiceInfo> = LinkedList()
    private var isResolving = false

    private var udpReceiver: UdpBeaconReceiver? = null

    companion object {
        private const val TAG = "NsdDiscoverer"
    }

    fun startDiscovery() {
        stopDiscovery()

        // Acquire MulticastLock so the TV Wi-Fi chip doesn't filter out mDNS & UDP packets
        try {
            multicastLock = wifiManager.createMulticastLock("LocalCast::DiscovererMulticastLock").apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not acquire MulticastLock on Discoverer", e)
        }

        // Start UDP Receiver fallback for routers that block mDNS
        udpReceiver = UdpBeaconReceiver { service ->
            addOrUpdateService(service)
        }
        udpReceiver?.start()

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d(TAG, "Service discovery started for: $regType")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Log.d(TAG, "Service found: ${service.serviceName}")
                queueResolve(service)
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Log.d(TAG, "Service lost: ${service.serviceName}")
                _services.value = _services.value.filter { it.serviceName != service.serviceName }
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(TAG, "Discovery stopped: $serviceType")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery failed to start: $errorCode")
                stopDiscovery()
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery failed to stop: $errorCode")
            }
        }

        try {
            nsdManager.discoverServices(NsdAdvertiser.SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to discover services", e)
        }
    }

    @Synchronized
    private fun queueResolve(service: NsdServiceInfo) {
        resolveQueue.add(service)
        processNextResolve()
    }

    @Synchronized
    @Suppress("DEPRECATION")
    private fun processNextResolve() {
        if (isResolving || resolveQueue.isEmpty()) return

        val nextService = resolveQueue.poll() ?: return
        isResolving = true

        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Resolve failed for ${serviceInfo.serviceName}: $errorCode")
                synchronized(this@NsdDiscoverer) {
                    isResolving = false
                    processNextResolve()
                }
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Resolve succeeded: ${serviceInfo.serviceName} at ${serviceInfo.host}:${serviceInfo.port}")
                val devName = serviceInfo.attributes["deviceName"]?.let { String(it) } ?: serviceInfo.serviceName
                val devId = serviceInfo.attributes["deviceId"]?.let { String(it) } ?: ""

                val discovered = DiscoveredService(
                    serviceName = serviceInfo.serviceName,
                    host = serviceInfo.host,
                    port = serviceInfo.port,
                    deviceName = devName,
                    deviceId = devId
                )

                addOrUpdateService(discovered)

                synchronized(this@NsdDiscoverer) {
                    isResolving = false
                    processNextResolve()
                }
            }
        }

        try {
            nsdManager.resolveService(nextService, resolveListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error initiating resolve", e)
            isResolving = false
            processNextResolve()
        }
    }

    fun addExternalService(discovered: DiscoveredService) {
        addOrUpdateService(discovered)
    }

    private fun addOrUpdateService(discovered: DiscoveredService) {
        _services.value = _services.value.filter {
            it.serviceName != discovered.serviceName && it.host?.hostAddress != discovered.host?.hostAddress
        } + discovered
    }

    fun stopDiscovery() {
        udpReceiver?.stop()
        udpReceiver = null

        discoveryListener?.let {
            try {
                nsdManager.stopServiceDiscovery(it)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to stop NSD discovery", e)
            }
            discoveryListener = null
        }

        try {
            if (multicastLock?.isHeld == true) multicastLock?.release()
        } catch (_: Exception) {}
        multicastLock = null

        synchronized(this) {
            resolveQueue.clear()
            isResolving = false
        }
    }
}

class UdpBeaconBroadcaster {
    private var job: Job? = null

    companion object {
        const val UDP_PORT = 8845
        private const val TAG = "UdpBeaconBroadcaster"
    }

    fun start(deviceName: String, port: Int, ip: String) {
        stop()
        job = CoroutineScope(Dispatchers.IO).launch {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket()
                socket.broadcast = true

                val payload = JSONObject().apply {
                    put("service", "LocalCast")
                    put("name", deviceName)
                    put("port", port)
                    put("ip", ip)
                }.toString()

                val data = payload.toByteArray(Charsets.UTF_8)
                val broadcastAddr = InetAddress.getByName("255.255.255.255")
                val packet = DatagramPacket(data, data.size, broadcastAddr, UDP_PORT)

                Log.d(TAG, "UDP Beacon started on port $UDP_PORT for $ip:$port")

                while (isActive) {
                    try {
                        socket.send(packet)
                    } catch (e: Exception) {
                        Log.w(TAG, "Error sending UDP beacon: ${e.message}")
                    }
                    delay(2000)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start UDP beacon", e)
            } finally {
                socket?.close()
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}

class UdpBeaconReceiver(private val onServiceReceived: (DiscoveredService) -> Unit) {
    private var job: Job? = null

    companion object {
        private const val TAG = "UdpBeaconReceiver"
    }

    fun start() {
        stop()
        job = CoroutineScope(Dispatchers.IO).launch {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket(UdpBeaconBroadcaster.UDP_PORT)
                socket.broadcast = true
                val buffer = ByteArray(2048)
                val packet = DatagramPacket(buffer, buffer.size)

                Log.d(TAG, "UDP Beacon receiver listening on ${UdpBeaconBroadcaster.UDP_PORT}")

                while (isActive) {
                    socket.receive(packet)
                    val text = String(packet.data, 0, packet.length, Charsets.UTF_8)
                    try {
                        val json = JSONObject(text)
                        if (json.optString("service") == "LocalCast") {
                            val name = json.optString("name", "LocalCast Phone")
                            val port = json.optInt("port", 8844)
                            val ip = json.optString("ip")
                            val host = if (ip.isNotBlank()) InetAddress.getByName(ip) else packet.address

                            val service = DiscoveredService(
                                serviceName = "LocalCast-$name",
                                host = host,
                                port = port,
                                deviceName = name,
                                deviceId = ip
                            )
                            onServiceReceived(service)
                        }
                    } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                Log.w(TAG, "UDP receiver stopped or error: ${e.message}")
            } finally {
                socket?.close()
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
