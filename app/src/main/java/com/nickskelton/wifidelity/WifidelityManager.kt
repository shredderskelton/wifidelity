package com.nickskelton.wifidelity

//
// class WifidelityManager(val wifiManager: WifiManager) {
//
//    @SuppressLint("ServiceCast")
//
//    private var receiverWifi = WifiReceiver()
//    val connections = MutableLiveData<List<String>>()
//    val currentlyScanning = MutableLiveData<Boolean>()
//
//    internal inner class WifiReceiver : BroadcastReceiver() {
//        override fun onReceive(c: Context, intent: Intent) {
//            currentlyScanning.value = false
//            connections.value = wifiManager.scanResults
//                    .map { it.SSID }
//                    .toList()
//        }
//    }
//
//    fun connect(networkSSID: String, networkPass: String = "") {
//
//        if (!wifiManager.isWifiEnabled) {
//            Timber.i("Enabling wifi")
//            wifiManager.isWifiEnabled = true
//        }
//
//
//        val conf = WifiConfiguration()
//        conf.SSID = "\"$networkSSID\""   // Please note the quotes.
//
//        // WEP
// //        conf.wepKeys[0] = "\"" + networkPass + "\"";
// //        conf.wepTxKeyIndex = 0;
// //        conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
// //        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40)
//
//        // WPA2
//        conf.preSharedKey = "\"$networkPass\"";
//
//        Timber.i("Attempting to connect to $networkSSID")
//        val networkId = wifiManager.addNetwork(conf)
// //        wifiManager.disconnect()
//        wifiManager.enableNetwork(networkId, true)
// //        wifiManager.reconnect()
//    }
//
//    fun scan() {
//        wifiManager.startScan()
//        currentlyScanning.value = true
//    }
//
//    fun register() {
//        Timber.i("Registering for wifi scanning")
//        context.registerReceiver(receiverWifi, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
//        scan()
//    }
//
//    fun unregister() {
//        Timber.i("Unregistering wifi scanning")
//        context.unregisterReceiver(receiverWifi)
//    }
//
// }