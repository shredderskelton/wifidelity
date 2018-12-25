package com.nickskelton.wifidelity.view.network.text

import android.app.Application
import com.nickskelton.wifidelity.view.adapter.BlockListItem
import com.nickskelton.wifidelity.view.adapter.NetworkBlockListItem
import com.nickskelton.wifidelity.view.adapter.TextBlockListItem
import com.nickskelton.wifidelity.viewmodel.ObservableViewModel
import com.nickskelton.wifidelity.viewmodel.toLiveData
import com.nickskelton.wifidelity.viewmodel.toLiveEvent
import com.nickskelton.wifidelity.wifi.WifiFinder
import io.reactivex.subjects.PublishSubject
import me.xdrop.fuzzywuzzy.FuzzySearch
import org.koin.standalone.KoinComponent
import timber.log.Timber

class NetworkTextViewModel(
    app: Application,
    private val args: NetworkTextActivity.Args,
    wifiFinder: WifiFinder
) : ObservableViewModel(app), KoinComponent {

    private val actionNextRelay: PublishSubject<Unit> = PublishSubject.create()

    val actionNext = actionNextRelay.toLiveEvent()

    private val actionShowDialogRelay: PublishSubject<Unit> = PublishSubject.create()

    val actionShowDialog = actionShowDialogRelay.toLiveEvent()

    private val networksObservable = wifiFinder.availableNetworks
        .replay(1)
        .refCount()

    private val results = args.results

    private val items by lazy {
        networksObservable.map { networks ->
            val networksSequence = networks.asSequence()
            val items = results
                .map { text ->
                    TextBlockListItem(
                        text,
                        networksSequence.map { network ->
                            FuzzySearch.ratio(text, network)
                        }.max() ?: 0,
                        ::onItemSelected
                    )
                }
                .sortedBy {
                    it.strength
                }
                .reversed()
                .toMutableList<BlockListItem>()

            items.addAll(
                networks.map { network ->
                    NetworkBlockListItem(
                        network,
                        ::onItemSelected
                    )
                }
            )
            items.toList()
        }
    }

    val adapterItems by lazy {
        items.toLiveData()
    }

    private val _exactMatches by lazy {
        networksObservable.map { networks ->
            val networksSequence = networks.asSequence()
            results.filter { text ->
                networksSequence.map { network ->
                    FuzzySearch.ratio(text, network)
                }.max() == 100
            }
        }
    }

    val exactMatches by lazy {
        _exactMatches.toLiveData()
    }

    val exactMatchFound by lazy {
        _exactMatches.filter {
            it.isNotEmpty()
        }.map {
            Unit
        }.toLiveEvent()
    }

    private fun onItemSelected(item: BlockListItem) {
        Timber.d("Selected ${item.titleText}")
        actionNextRelay.onNext(Unit)
    }
}