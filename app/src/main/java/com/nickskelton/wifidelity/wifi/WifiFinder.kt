package com.nickskelton.wifidelity.wifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import timber.log.Timber

interface WifiFinder {
    val availableNetworks: Observable<List<String>>
}

class AndroidWifiFinder(
    private val applicationContext: Context,
    private val wifiManager: WifiManager
) : WifiFinder {

    private var receiverWifi = WifiReceiver()

    private var ssidEmitter: ObservableEmitter<List<String>>? = null

    override val availableNetworks: Observable<List<String>> by lazy {
        Observable.create<List<String>> { emitter ->
            emitter.onNext(emptyList())
            ssidEmitter = emitter
            register()
        }
    }

    internal inner class WifiReceiver : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) {
            val networks = wifiManager.scanResults
                .asSequence()
                .map { it.SSID }
                .toList()
            Timber.i("Found ${networks.size}")
            ssidEmitter?.onNext(
                networks
            )
        }
    }

    private fun register() {
        Timber.i("Registering for wifi scanning")
        applicationContext.registerReceiver(receiverWifi, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        wifiManager.startScan()
    }

    private fun unregister() {
        Timber.i("Unregistering wifi scanning")
        applicationContext.unregisterReceiver(receiverWifi)
    }
}