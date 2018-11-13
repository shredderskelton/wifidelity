package com.nickskelton.wifidelity.view

import android.app.Application
import android.net.ConnectivityManager
import android.net.NetworkInfo
import com.github.pwittchen.reactivenetwork.library.rx2.ConnectivityPredicate
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.nickskelton.wifidelity.viewmodel.ObservableViewModel
import com.nickskelton.wifidelity.viewmodel.toLiveData
import com.nickskelton.wifidelity.wifi.WifiConnector
import com.nickskelton.wifidelity.wifi.WifiFinder
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import me.xdrop.fuzzywuzzy.FuzzySearch
import org.koin.standalone.KoinComponent

class ConnectViewModel(
    app: Application,
    private val wifiConnector: WifiConnector,
    networkParam: String,
    passwordParam: String,
    wifiFinder: WifiFinder
) : ObservableViewModel(app), KoinComponent {

    fun connect() {
        wifiConnector.connectTo(network, password)
    }

    var network: String = networkParam
        set(value) {
            field = value
            notifyChange()
        }

    var password: String = passwordParam
        set(value) {
            field = value
            notifyChange()
        }

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
    private val networksObservable = wifiFinder.availableNetworks

    val networkSuggestions by lazy {
        networksObservable.map { networks ->
            networks
                .filter {
                    FuzzySearch.ratio(network, it) > 80
                }
                .sortedBy {
                    FuzzySearch.ratio(network, it)
                }
                .reversed()
        }.toLiveData()
    }

    //TODO password suggestions, common ocr mistakes, capital letters, sym801i23R library or something...
}
