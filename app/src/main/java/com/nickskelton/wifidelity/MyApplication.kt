package com.nickskelton.wifidelity

import android.app.Application
import com.nickskelton.wifidelity.di.modules
import org.koin.android.ext.android.startKoin
import timber.log.Timber

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin(this, modules)
        initLogging()
    }

    private fun initLogging() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}