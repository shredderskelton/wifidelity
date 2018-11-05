package com.nickskelton.wifidelity.view

import android.app.Application
import android.graphics.drawable.BitmapDrawable
import com.nickskelton.wifidelity.wifi.WifiFinder
import com.nickskelton.wifidelity.model.BitmapRepository
import com.nickskelton.wifidelity.model.DetectionResult
import com.nickskelton.wifidelity.model.ImageProcessor
import com.nickskelton.wifidelity.model.WorkflowRepository
import com.nickskelton.wifidelity.view.adapter.BlockListItem
import com.nickskelton.wifidelity.viewmodel.ObservableViewModel
import com.nickskelton.wifidelity.viewmodel.toLiveData
import com.nickskelton.wifidelity.viewmodel.toLiveEvent
import com.nickskelton.wifidelity.wifi.WifiConnector
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import me.xdrop.fuzzywuzzy.FuzzySearch
import org.koin.core.parameter.parametersOf
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import timber.log.Timber

class ChoosePasswordViewModel(
    app: Application,
    private val workflowRepository: WorkflowRepository,
    private val wifiConnector: WifiConnector
) : ObservableViewModel(app), KoinComponent {

    private val actionNextRelay: PublishSubject<Pair<String, String>> = PublishSubject.create()

    val actionNext = actionNextRelay.toLiveEvent()

    val items = workflowRepository.results.map {
        BlockListItem(it.first, it.second, ::onItemSelected)
    }

    private fun onItemSelected(item: BlockListItem) {
        Timber.d("Selected ${item.foundText}")
        actionNextRelay.onNext(Pair(workflowRepository.networkName, item.foundText))
    }

    private fun connectTo(network: String, password: String) {
        wifiConnector.connectTo(network, password)
    }
}
