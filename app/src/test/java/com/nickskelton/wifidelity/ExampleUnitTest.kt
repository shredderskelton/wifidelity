package com.nickskelton.wifidelity

import android.content.Context
import com.nickskelton.wifidelity.di.modules
import org.junit.Test
import org.koin.android.ext.koin.with

import org.koin.standalone.StandAloneContext.startKoin
import org.koin.test.KoinTest
import org.koin.test.checkModules
import org.mockito.Mockito.mock

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest : KoinTest {
    @Test
    fun test_dryRun() {
        startKoin(modules) with (mock(Context::class.java))
        checkModules(modules)
    }
}
