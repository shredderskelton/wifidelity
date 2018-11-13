package com.nickskelton.wifidelity.view

import android.app.Application
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.core.content.res.ResourcesCompat
import com.nickskelton.wifidelity.R
import com.nickskelton.wifidelity.extensions.toBitmap
import com.nickskelton.wifidelity.model.DetectionResult
import com.nickskelton.wifidelity.model.ImageProcessor
import com.nickskelton.wifidelity.model.SingleItemRepository
import com.nickskelton.wifidelity.model.WorkflowRepository
import com.nickskelton.wifidelity.view.adapter.BlockListItem
import com.nickskelton.wifidelity.viewmodel.ObservableViewModel
import com.nickskelton.wifidelity.viewmodel.toLiveData
import com.nickskelton.wifidelity.viewmodel.toLiveEvent
import com.nickskelton.wifidelity.wifi.WifiFinder
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.PublishSubject
import me.xdrop.fuzzywuzzy.FuzzySearch
import org.koin.core.parameter.parametersOf
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import timber.log.Timber

class ChooseNetworkViewModel(
    app: Application,
    private val bitmapId: String,
    private val bitmapRepository: SingleItemRepository<Bitmap>,
    private val workflowRepository: WorkflowRepository,
    wifiFinder: WifiFinder
) : ObservableViewModel(app), KoinComponent {

    private val questionMark: BitmapDrawable by lazy {
        val drawable = ResourcesCompat.getDrawable(app.resources, R.drawable.ic_help, null)!!
        BitmapDrawable(app.resources, drawable.toBitmap())
    }

    private val imageProcessor: ImageProcessor by inject { parametersOf(bitmapRepository.get(bitmapId)!!) }

    private val detectionResult = imageProcessor.results

    private val actionNextRelay: PublishSubject<Unit> = PublishSubject.create()

    val actionNext = actionNextRelay.toLiveEvent()

    private val actionShowDialogRelay: PublishSubject<Unit> = PublishSubject.create()

    val actionShowDialog = actionShowDialogRelay.toLiveEvent()

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
        Timber.i("Combining ${blocks.size} blocks with ${networks.size} networks")
        val results = mutableListOf<BlockListItem>()
        results.addAll(convert(blocks, networks))

        //add one at the bottom incase they dont find anything
        if (blocks.isNotEmpty())
            results.add(
                BlockListItem(
                    questionMark,
                    "Click here if you don't see your network?",
                    ::onNotFound
                )
            )

        results
    }.toLiveData()

    private fun onNotFound(item: BlockListItem) {
        actionShowDialogRelay.onNext(Unit)
    }

    private fun onItemSelected(item: BlockListItem) {
        Timber.d("Selected ${item.foundText}")
        workflowRepository.networkName = item.foundText
        actionNextRelay.onNext(Unit)
    }

    private fun convert(blocks: List<Pair<BitmapDrawable, String>>, withNetworks: List<String>): List<BlockListItem> {
        val networksSequence = withNetworks.asSequence()
        return blocks
            .sortedBy { block ->
                // Highest match with networks
                networksSequence.map { FuzzySearch.ratio(block.second, it) }.max()
            }
            .reversed()
            .map {
                BlockListItem(it.first, it.second, ::onItemSelected)
            }
    }
}
