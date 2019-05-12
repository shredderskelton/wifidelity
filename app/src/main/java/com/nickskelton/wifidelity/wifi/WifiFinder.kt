package com.nickskelton.wifidelity.wifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableEmitter
import timber.log.Timber

interface WifiFinder {
    val availableNetworks: Flowable<List<String>>
}

class DummyWifiFinder : WifiFinder {
    override val availableNetworks: Flowable<List<String>>
        get() = Flowable.just(listOf("test", "nowifinocry", "no need for a backend"))
}

class AndroidWifiFinder(
    private val applicationContext: Context,
    private val wifiManager: WifiManager
) : WifiFinder {

    private var receiverWifi = WifiReceiver()

    private var ssidEmitter: FlowableEmitter<List<String>>? = null

    override val availableNetworks: Flowable<List<String>> by lazy {
        Flowable.create<List<String>>({ emitter ->
            emitter.onNext(emptyList())
            ssidEmitter = emitter
            register()
        }, BackpressureStrategy.LATEST)
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