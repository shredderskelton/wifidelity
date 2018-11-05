package com.nickskelton.wifidelity.wifi

import android.content.Context
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import timber.log.Timber
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.content.Context.CONNECTIVITY_SERVICE
import androidx.core.content.ContextCompat.getSystemService



interface WifiStatus {
    val connectionStatus: String
}

fun WifiManager.ensureConnected() {
    if (!isWifiEnabled) {
        Timber.i("Enabling wifi")
        isWifiEnabled = true
    }
}