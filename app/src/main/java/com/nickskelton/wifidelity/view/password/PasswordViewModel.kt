package com.nickskelton.wifidelity.view.password

import android.app.Application
import com.nickskelton.wifidelity.view.adapter.BlockListItem
import com.nickskelton.wifidelity.view.adapter.TextBlockListItem
import com.nickskelton.wifidelity.viewmodel.ObservableViewModel
import com.nickskelton.wifidelity.viewmodel.toLiveEvent
import io.reactivex.subjects.PublishSubject
import org.koin.standalone.KoinComponent
import timber.log.Timber

class PasswordViewModel(
    app: Application,
    private val args: PasswordActivity.Args
) : ObservableViewModel(app), KoinComponent {

    private val actionNextRelay: PublishSubject<String> = PublishSubject.create()

    val actionNext = actionNextRelay.toLiveEvent()

    private val actionShowDialogRelay: PublishSubject<Unit> = PublishSubject.create()

    val actionShowDialog = actionShowDialogRelay.toLiveEvent()

    private val results = args.capturedResults

    val items by lazy {
        results
            .map { text ->
                TextBlockListItem(
                    text,
                    0,
                    ::onItemSelected
                )
            }
    }

    private fun onItemSelected(item: BlockListItem) {
        Timber.d("Selected ${item.titleText}")
        actionNextRelay.onNext(item.titleText)
    }
}