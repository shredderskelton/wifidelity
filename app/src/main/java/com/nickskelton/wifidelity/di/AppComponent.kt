package com.nickskelton.wifidelity.di

import android.content.Context
import android.graphics.Bitmap
import android.net.wifi.WifiManager
import com.nickskelton.wifidelity.wifi.AndroidWifiFinder
import com.nickskelton.wifidelity.wifi.WifiFinder
import com.nickskelton.wifidelity.model.BitmapRepository
import com.nickskelton.wifidelity.model.ImageProcessor
import com.nickskelton.wifidelity.model.WorkflowRepository
import com.nickskelton.wifidelity.view.ChooseNetworkViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.experimental.builder.viewModel
import org.koin.dsl.module.module

val apiModule = module {
    factory { androidContext().applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager }
    single<WifiFinder> { AndroidWifiFinder(androidContext().applicationContext, get()) }
}

val pluginModule = module {
    single { BitmapRepository() }
    single { WorkflowRepository() }
    factory { (bitmap: Bitmap) -> ImageProcessor(bitmap) }
}

val viewModelModule = module {
    viewModel<ChooseNetworkViewModel>()
//    factory { params -> MapPresenter(params[VIEW_PARAM]) }
//    factory { params -> MoneyPresenter(get(), params[VIEW_PARAM]) }
//    factory { params -> SettingsPresenter(params[VIEW_PARAM]) }
//    factory { params -> PresenterAnyDay(params[VIEW_PARAM], params["dayOfYear"], params["year"]) }
//    factory { params -> PresenterToday(params[VIEW_PARAM]) }
}

val modules = listOf(pluginModule, viewModelModule, apiModule)