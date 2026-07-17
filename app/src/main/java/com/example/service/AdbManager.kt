package com.example.service

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.InetAddress
import java.util.concurrent.CopyOnWriteArrayList

data class AdbServiceInfo(
    val name: String,
    val port: Int,
    val type: String, // "pairing" or "connect"
    val address: InetAddress? = null
)

class AdbManager(private val context: Context) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val localAdbExecutor = LocalAdbExecutor(context)
    
    private val _discoveredServices = MutableStateFlow<List<AdbServiceInfo>>(emptyList())
    val discoveredServices: StateFlow<List<AdbServiceInfo>> = _discoveredServices.asStateFlow()

    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()
    
    private val servicesList = CopyOnWriteArrayList<AdbServiceInfo>()

    private fun createDiscoveryListener(serviceTypeExpected: String) = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(regType: String) {
            Log.d("AdbManager", "Service discovery started for $regType")
        }

        override fun onServiceFound(service: NsdServiceInfo) {
            Log.d("AdbManager", "Service discovery success: $service")
            try {
                nsdManager.resolveService(service, createResolveListener())
            } catch (e: Exception) {
                Log.e("AdbManager", "Resolve failed to start", e)
            }
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            Log.e("AdbManager", "service lost: $service")
            val toRemove = servicesList.filter { it.name == service.serviceName }
            servicesList.removeAll(toRemove)
            _discoveredServices.value = servicesList.toList()
        }

        override fun onDiscoveryStopped(serviceType: String) {
            Log.i("AdbManager", "Discovery stopped: $serviceType")
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e("AdbManager", "Discovery failed: Error code:$errorCode")
            nsdManager.stopServiceDiscovery(this)
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e("AdbManager", "Stop discovery failed: Error code:$errorCode")
            nsdManager.stopServiceDiscovery(this)
        }
    }

    private fun createResolveListener() = object : NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            Log.e("AdbManager", "Resolve failed: $errorCode")
        }

        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            Log.d("AdbManager", "Resolve Succeeded. $serviceInfo")
            val type = if (serviceInfo.serviceType.contains("pairing")) "pairing" else "connect"
            val info = AdbServiceInfo(
                name = serviceInfo.serviceName,
                port = serviceInfo.port,
                type = type,
                address = serviceInfo.host
            )
            if (!servicesList.any { it.name == info.name && it.type == info.type }) {
                servicesList.add(info)
                _discoveredServices.value = servicesList.toList()
            }
        }
    }

    private var pairingListener: NsdManager.DiscoveryListener? = null
    private var connectListener: NsdManager.DiscoveryListener? = null

    fun startDiscovery() {
        stopDiscovery()
        servicesList.clear()
        _discoveredServices.value = emptyList()
        try {
            pairingListener = createDiscoveryListener("_adb-tls-pairing._tcp").also {
                nsdManager.discoverServices("_adb-tls-pairing._tcp", NsdManager.PROTOCOL_DNS_SD, it)
            }
            connectListener = createDiscoveryListener("_adb-tls-connect._tcp").also {
                nsdManager.discoverServices("_adb-tls-connect._tcp", NsdManager.PROTOCOL_DNS_SD, it)
            }
        } catch (e: Exception) {
            Log.e("AdbManager", "Error starting discovery", e)
        }
    }

    fun stopDiscovery() {
        try {
            pairingListener?.let { nsdManager.stopServiceDiscovery(it) }
            connectListener?.let { nsdManager.stopServiceDiscovery(it) }
        } catch (e: Exception) {
            Log.e("AdbManager", "Error stopping discovery", e)
        } finally {
            pairingListener = null
            connectListener = null
        }
    }

    fun pair(ip: String, port: Int, code: String) {
        _connectionStatus.value = "Pairing with $ip:$port..."
        
        CoroutineScope(Dispatchers.Main).launch {
            val (success, output) = localAdbExecutor.pairWireless(ip, port, code)
            if (success) {
                _connectionStatus.value = "Paired successfully! Ready to connect."
            } else {
                _connectionStatus.value = "Pairing failed: $output"
            }
        }
    }

    fun connect(ip: String, port: Int) {
        _connectionStatus.value = "Connecting to $ip:$port..."
        
        CoroutineScope(Dispatchers.Main).launch {
            val (success, output) = localAdbExecutor.connectWireless(ip, port)
            if (success) {
                _connectionStatus.value = "Connected! Granting permissions..."
                
                // Grant necessary permissions and enable Accessibility Service automatically
                val appId = "com.aistudio.spencommand.qwzpk"
                
                // Grant WRITE_SECURE_SETTINGS
                localAdbExecutor.executeAdbShell("pm grant $appId android.permission.WRITE_SECURE_SETTINGS", ip, port)
                
                // Enable Accessibility Service
                val serviceComponent = "$appId/com.example.service.SPenAccessibilityService"
                localAdbExecutor.executeAdbShell("settings put secure enabled_accessibility_services $serviceComponent", ip, port)
                localAdbExecutor.executeAdbShell("settings put secure accessibility_enabled 1", ip, port)
                
                _connectionStatus.value = "Connected & Permissions Granted!"
            } else {
                _connectionStatus.value = "Connection failed: $output"
            }
        }
    }

    fun getLogcatFlow(ip: String = "127.0.0.1", port: Int? = null) = localAdbExecutor.startLogcat(ip, port)
}
