package com.nickskelton.wifidelity.wifi

import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import timber.log.Timber

interface WifiConnector {
    fun connectTo(networkName: String, password: String)
}

class AndroidWifiConnector(private val wifiManager: WifiManager) : WifiConnector {
    override fun connectTo(networkName: String, password: String) {

        if (!wifiManager.isWifiEnabled) {
            Timber.i("Enabling wifi")
            wifiManager.isWifiEnabled = true
        }

        val conf = WifiConfiguration()
        conf.SSID = "\"$networkName\"" // Please note the quotes.

        // WEP
//        conf.wepKeys[0] = "\"" + networkPass + "\"";
//        conf.wepTxKeyIndex = 0;
//        conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
//        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40)

        // WPA2
        conf.preSharedKey = "\"$password\""

        Timber.i("Attempting to connect to $networkName")
        val networkId = wifiManager.addNetwork(conf)
//        wifiManager.disconnect()
        wifiManager.enableNetwork(networkId, true)
//        wifiManager.reconnect()
    }
}
