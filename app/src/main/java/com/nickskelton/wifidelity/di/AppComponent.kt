package com.nickskelton.wifidelity.di

import android.content.Context
import android.graphics.Bitmap
import android.net.wifi.WifiManager
import com.nickskelton.wifidelity.model.ImageProcessor
import com.nickskelton.wifidelity.model.SingleItemRepository
import com.nickskelton.wifidelity.model.VolatileBitmapRepository
import com.nickskelton.wifidelity.model.WorkflowRepository
import com.nickskelton.wifidelity.view.ChooseNetworkViewModel
import com.nickskelton.wifidelity.view.ChoosePasswordViewModel
import com.nickskelton.wifidelity.view.ConnectViewModel
import com.nickskelton.wifidelity.wifi.AndroidWifiConnector
import com.nickskelton.wifidelity.wifi.AndroidWifiFinder
import com.nickskelton.wifidelity.wifi.WifiConnector
import com.nickskelton.wifidelity.wifi.WifiFinder
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.experimental.builder.viewModel
import org.koin.androidx.viewmodel.ext.koin.viewModel
import org.koin.dsl.module.module

val apiModule = module {
    factory { androidContext().applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager }
    single<WifiFinder> { AndroidWifiFinder(androidContext().applicationContext, get()) }
    single<WifiConnector> { AndroidWifiConnector(get()) }
}

val pluginModule = module {
    single<SingleItemRepository<Bitmap>> { VolatileBitmapRepository() }
    single { WorkflowRepository() }
    factory { (bitmap: Bitmap) -> ImageProcessor(bitmap) }
}

val viewModelModule = module {
    viewModel { (bitmapId: String) ->
        ChooseNetworkViewModel(
            get(),
            bitmapId,
            get(),
            get(),
            get()
        )
    }
    viewModel<ChoosePasswordViewModel>()
    viewModel { (net: String, pwd: String) -> ConnectViewModel(get(), get(), net, pwd) }
//    factory { params -> MapPresenter(params[VIEW_PARAM]) }
//    factory { params -> MoneyPresenter(get(), params[VIEW_PARAM]) }
//    factory { params -> SettingsPresenter(params[VIEW_PARAM]) }
//    factory { params -> PresenterAnyDay(params[VIEW_PARAM], params["dayOfYear"], params["year"]) }
//    factory { params -> PresenterToday(params[VIEW_PARAM]) }
}

val modules = listOf(pluginModule, viewModelModule, apiModule)