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
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import me.xdrop.fuzzywuzzy.FuzzySearch
import org.koin.core.parameter.parametersOf
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import timber.log.Timber

class ChooseNetworkViewModel(
    app: Application,
    private val bitmapRepository: BitmapRepository,
    private val workflowRepository: WorkflowRepository,
    wifiFinder: WifiFinder
) : ObservableViewModel(app), KoinComponent {

    private val imageProcessor: ImageProcessor by inject { parametersOf(bitmapRepository.bitmap!!) }

    private val detectionResult = imageProcessor.results

    private val actionNextRelay: PublishSubject<Unit> = PublishSubject.create()

    val actionNext = actionNextRelay.toLiveEvent()

    val loading = detectionResult.map {
        when (it) {
            is DetectionResult.Loading -> true
            else -> false
        }
    }.toLiveData()

    private val errorTextObservable = detectionResult.map {
        when (it) {
            is DetectionResult.Failed -> {
                "Failure: ${it.message}"
            }
            is DetectionResult.NothingFound -> {
                "Nothing found in picture, try again"
            }
            else -> ""
        }
    }

    val errorText = errorTextObservable.toLiveData()

    val errorTextVisible = errorTextObservable.map {
        it.isNotEmpty()
    }.toLiveData()

    val retryVisible = errorTextObservable.map {
        it.isNotEmpty()
    }.toLiveData()

    private val blocksObservable = detectionResult.map {
        when (it) {
            is DetectionResult.Success -> {
                Timber.d("Got some blocks successfully")
                workflowRepository.results = it.blocks
                it.blocks
            }
            else -> emptyList()
        }
    }

    private val networksObservable = wifiFinder.availableNetworks

    val items = Observables.combineLatest(
        blocksObservable, networksObservable
    )
    { blocks, networks ->
        Timber.d("Combining ${blocks.size} blocks with ${networks.size} networks")
        convert(blocks, networks)
    }
        .toLiveData()

    private fun onItemSelected(item: BlockListItem) {
        Timber.d("Selected ${item.foundText}")
        workflowRepository.networkName = item.foundText
        actionNextRelay.onNext(Unit)
    }

    private fun convert(blocks: List<Pair<BitmapDrawable, String>>, withNetworks: List<String>): List<BlockListItem> =
        blocks
            .sortedBy { block ->
                withNetworks
                    .asSequence()
                    .map {
                        FuzzySearch.ratio(block.second, it)
                    }.max()
            }
            .reversed()
            .map {
                BlockListItem(it.first, it.second, ::onItemSelected)
            }
}
