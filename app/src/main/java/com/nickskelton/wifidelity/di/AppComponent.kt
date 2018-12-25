package com.nickskelton.wifidelity.di

import android.content.Context
import android.graphics.Bitmap
import android.net.wifi.WifiManager
import com.nickskelton.wifidelity.model.ImageProcessor
import com.nickskelton.wifidelity.model.SingleItemRepository
import com.nickskelton.wifidelity.model.VolatileBitmapRepository
import com.nickskelton.wifidelity.model.WorkflowRepository
import com.nickskelton.wifidelity.view.ChoosePasswordViewModel
import com.nickskelton.wifidelity.view.ConnectViewModel
import com.nickskelton.wifidelity.view.network.text.NetworkTextActivity
import com.nickskelton.wifidelity.view.network.text.NetworkTextViewModel
import com.nickskelton.wifidelity.view.password.PasswordActivity
import com.nickskelton.wifidelity.view.password.PasswordViewModel
import com.nickskelton.wifidelity.wifi.AndroidWifiConnector
import com.nickskelton.wifidelity.wifi.AndroidWifiFinder
import com.nickskelton.wifidelity.wifi.WifiConnector
import com.nickskelton.wifidelity.wifi.WifiFinder
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.experimental.builder.viewModel
import org.koin.androidx.viewmodel.ext.koin.viewModel
import org.koin.dsl.module.module
import se.gustavkarlsson.koptional.Optional

val apiModule = module {
    factory { androidContext().applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager }
    single<WifiFinder> { AndroidWifiFinder(androidContext().applicationContext, get()) }
    single<WifiConnector> { AndroidWifiConnector(get()) }
}

val pluginModule = module {
    single<SingleItemRepository<Bitmap>> { VolatileBitmapRepository() }
    single { WorkflowRepository() }
    factory { (bitmap: Optional<Bitmap>) -> ImageProcessor(bitmap) }
}

val viewModelModule = module {
    //    viewModel { (bitmapId: String, results: Array<String>) ->
//        ChooseNetworkViewModel(
//            get(),
//            bitmapId,
//            results,
//            get(),
//            get(),
//            get()
//        )
//    }

    viewModel<NetworkTextViewModel> { (args: NetworkTextActivity.Args) -> NetworkTextViewModel(get(), args, get()) }
    viewModel<PasswordViewModel> { (args: PasswordActivity.Args) -> PasswordViewModel(get(), args) }
    viewModel<ChoosePasswordViewModel>()
    viewModel { (net: String, pwd: String) -> ConnectViewModel(get(), get(), net, pwd, get()) }
//    factory { params -> MapPresenter(params[VIEW_PARAM]) }
//    factory { params -> MoneyPresenter(get(), params[VIEW_PARAM]) }
//    factory { params -> SettingsPresenter(params[VIEW_PARAM]) }
//    factory { params -> PresenterAnyDay(params[VIEW_PARAM], params["dayOfYear"], params["year"]) }
//    factory { params -> PresenterToday(params[VIEW_PARAM]) }
}

val modules = listOf(pluginModule, viewModelModule, apiModule)