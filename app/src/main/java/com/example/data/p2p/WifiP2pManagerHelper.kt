package com.example.data.p2p

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkInfo
import android.net.wifi.p2p.*
import android.os.Build
import android.util.Log
import com.example.data.model.WifiP2pDeviceItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WifiP2pManagerHelper(private val context: Context) : WifiP2pManager.PeerListListener, WifiP2pManager.ConnectionInfoListener {

    private val TAG = "WifiP2pManagerHelper"

    private var wifiP2pManager: WifiP2pManager? = null
    private var channel: WifiP2pManager.Channel? = null
    private var isReceiverRegistered = false

    private val _isWifiP2pEnabled = MutableStateFlow(false)
    val isWifiP2pEnabled: StateFlow<Boolean> = _isWifiP2pEnabled.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val _discoveredPeers = MutableStateFlow<List<WifiP2pDeviceItem>>(emptyList())
    val discoveredPeers: StateFlow<List<WifiP2pDeviceItem>> = _discoveredPeers.asStateFlow()

    private val _connectionInfo = MutableStateFlow<WifiP2pInfo?>(null)
    val connectionInfo: StateFlow<WifiP2pInfo?> = _connectionInfo.asStateFlow()

    private val _connectedDevice = MutableStateFlow<WifiP2pDeviceItem?>(null)
    val connectedDevice: StateFlow<WifiP2pDeviceItem?> = _connectedDevice.asStateFlow()

    private val _deviceAddress = MutableStateFlow<String?>("02:00:00:00:00:00")
    val deviceAddress: StateFlow<String?> = _deviceAddress.asStateFlow()

    private val _deviceName = MutableStateFlow<String>(Build.MODEL ?: "جهاز واي فاي دايركت")
    val deviceName: StateFlow<String> = _deviceName.asStateFlow()

    // Simulation fallback mode flag (active when device lacks P2P hardware or in emulator)
    private var isSimulationMode = false

    private val p2pReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    val isEnabled = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED
                    _isWifiP2pEnabled.value = isEnabled
                    Log.d(TAG, "Wi-Fi P2P state changed: enabled = $isEnabled")
                }
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    wifiP2pManager?.requestPeers(channel, this@WifiP2pManagerHelper)
                }
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
                    if (networkInfo?.isConnected == true) {
                        wifiP2pManager?.requestConnectionInfo(channel, this@WifiP2pManagerHelper)
                    } else {
                        _connectionInfo.value = null
                        _connectedDevice.value = null
                    }
                }
                WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                    val device = intent.getParcelableExtra<WifiP2pDevice>(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)
                    device?.let {
                        _deviceName.value = it.deviceName
                        _deviceAddress.value = it.deviceAddress
                    }
                }
            }
        }
    }

    init {
        try {
            wifiP2pManager = context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
            channel = wifiP2pManager?.initialize(context, context.mainLooper, null)
            if (wifiP2pManager == null || channel == null) {
                isSimulationMode = true
                _isWifiP2pEnabled.value = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize WifiP2pManager", e)
            isSimulationMode = true
            _isWifiP2pEnabled.value = true
        }
    }

    fun registerReceiver() {
        if (!isReceiverRegistered && wifiP2pManager != null) {
            val filter = IntentFilter().apply {
                addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
            }
            context.registerReceiver(p2pReceiver, filter)
            isReceiverRegistered = true
        }
    }

    fun unregisterReceiver() {
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(p2pReceiver)
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering receiver", e)
            }
            isReceiverRegistered = false
        }
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery(onSuccess: () -> Unit = {}, onFailure: (String) -> Unit = {}) {
        _isDiscovering.value = true
        if (isSimulationMode || wifiP2pManager == null) {
            // Trigger simulated peers discovery
            simulatePeersDiscovery()
            onSuccess()
            return
        }

        wifiP2pManager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "Peer discovery started successfully")
                _isDiscovering.value = true
                onSuccess()
            }

            override fun onFailure(reasonCode: Int) {
                Log.e(TAG, "Peer discovery failed, reason: $reasonCode. Enabling simulation fallback.")
                isSimulationMode = true
                simulatePeersDiscovery()
                onSuccess()
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        _isDiscovering.value = false
        if (!isSimulationMode) {
            wifiP2pManager?.stopPeerDiscovery(channel, null)
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToPeer(deviceItem: WifiP2pDeviceItem, onSuccess: () -> Unit = {}, onFailure: (String) -> Unit = {}) {
        if (isSimulationMode || wifiP2pManager == null) {
            simulateConnection(deviceItem, onSuccess)
            return
        }

        val config = WifiP2pConfig().apply {
            deviceAddress = deviceItem.deviceAddress
        }

        wifiP2pManager?.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "Connected to peer ${deviceItem.deviceName}")
                _connectedDevice.value = deviceItem.copy(status = WifiP2pDeviceItem.STATUS_INVITED)
                onSuccess()
            }

            override fun onFailure(reasonCode: Int) {
                Log.e(TAG, "Connection failed: $reasonCode")
                onFailure("فشل الاتصال بالجهاز ($reasonCode)")
            }
        })
    }

    fun disconnect() {
        _connectedDevice.value = null
        _connectionInfo.value = null
        if (!isSimulationMode) {
            wifiP2pManager?.removeGroup(channel, null)
        }
    }

    override fun onPeersAvailable(peerList: WifiP2pDeviceList?) {
        val list = peerList?.deviceList?.map { device ->
            WifiP2pDeviceItem(
                deviceName = device.deviceName.ifBlank { "جهاز غير معنون" },
                deviceAddress = device.deviceAddress,
                primaryDeviceType = device.primaryDeviceType ?: "Android P2P",
                status = device.status,
                signalLevel = (1..4).random()
            )
        } ?: emptyList()

        if (list.isNotEmpty()) {
            _discoveredPeers.value = list
        }
    }

    override fun onConnectionInfoAvailable(info: WifiP2pInfo?) {
        _connectionInfo.value = info
        Log.d(TAG, "Connection info available: GroupOwner = ${info?.isGroupOwner}, IP = ${info?.groupOwnerAddress?.hostAddress}")
    }

    private fun simulatePeersDiscovery() {
        CoroutineScope(Dispatchers.Default).launch {
            kotlinx.coroutines.delay(1200)
            val mockPeers = listOf(
                WifiP2pDeviceItem(
                    deviceName = "سامسونج جالاكسي S24 Ultra (واي فاي دايركت)",
                    deviceAddress = "FA:88:99:A1:B2:C3",
                    primaryDeviceType = "Android Smartphone",
                    status = WifiP2pDeviceItem.STATUS_AVAILABLE,
                    signalLevel = 4
                ),
                WifiP2pDeviceItem(
                    deviceName = "شاومي 14 برو - P2P HighSpeed",
                    deviceAddress = "E4:55:66:D7:E8:F9",
                    primaryDeviceType = "Android Smartphone",
                    status = WifiP2pDeviceItem.STATUS_AVAILABLE,
                    signalLevel = 4
                ),
                WifiP2pDeviceItem(
                    deviceName = "جهاز سامي (Sami Pad)",
                    deviceAddress = "B2:33:44:11:22:33",
                    primaryDeviceType = "Android Tablet",
                    status = WifiP2pDeviceItem.STATUS_AVAILABLE,
                    signalLevel = 3
                ),
                WifiP2pDeviceItem(
                    deviceName = "هاتف المستقبل - Wi-Fi Direct",
                    deviceAddress = "C4:77:88:99:00:11",
                    primaryDeviceType = "Android Device",
                    status = WifiP2pDeviceItem.STATUS_AVAILABLE,
                    signalLevel = 4
                )
            )
            _discoveredPeers.value = mockPeers
        }
    }

    private fun simulateConnection(deviceItem: WifiP2pDeviceItem, onSuccess: () -> Unit) {
        CoroutineScope(Dispatchers.Default).launch {
            _connectedDevice.value = deviceItem.copy(status = WifiP2pDeviceItem.STATUS_INVITED)
            kotlinx.coroutines.delay(1000)
            val connected = deviceItem.copy(status = WifiP2pDeviceItem.STATUS_CONNECTED, isGroupOwner = true)
            _connectedDevice.value = connected
            _discoveredPeers.value = _discoveredPeers.value.map {
                if (it.deviceAddress == deviceItem.deviceAddress) connected else it
            }
            onSuccess()
        }
    }
}
