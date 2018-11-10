package com.nickskelton.wifidelity.view

import android.app.Application
import android.net.ConnectivityManager
import android.net.NetworkInfo
import com.github.pwittchen.reactivenetwork.library.rx2.ConnectivityPredicate
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.nickskelton.wifidelity.viewmodel.ObservableViewModel
import com.nickskelton.wifidelity.viewmodel.toLiveData
import com.nickskelton.wifidelity.wifi.WifiConnector
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.koin.standalone.KoinComponent

class ConnectViewModel(
    app: Application,
    private val wifiConnector: WifiConnector,
    networkParam: String,
    passwordParam: String
) : ObservableViewModel(app), KoinComponent {

    fun connect() {
        wifiConnector.connectTo(network, password)
    }

    var network: String = networkParam

    var password: String = passwordParam

    val statusText by lazy {
        ReactiveNetwork
            .observeNetworkConnectivity(app)
            .subscribeOn(Schedulers.io())
            .filter(ConnectivityPredicate.hasState(NetworkInfo.State.CONNECTED))
            .filter(ConnectivityPredicate.hasType(ConnectivityManager.TYPE_WIFI))
            .observeOn(AndroidSchedulers.mainThread())
            .map {
                it.detailedState().toString()
            }.toLiveData()
    }
}
