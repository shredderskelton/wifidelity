package com.nickskelton.wifidelity.view

import android.app.Application
import com.nickskelton.wifidelity.model.WorkflowRepository
import com.nickskelton.wifidelity.view.adapter.BlockListItem
import com.nickskelton.wifidelity.viewmodel.ObservableViewModel
import com.nickskelton.wifidelity.viewmodel.toLiveEvent
import io.reactivex.subjects.PublishSubject
import org.koin.standalone.KoinComponent
import timber.log.Timber

class ChoosePasswordViewModel(
    app: Application,
    private val workflowRepository: WorkflowRepository
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
}
