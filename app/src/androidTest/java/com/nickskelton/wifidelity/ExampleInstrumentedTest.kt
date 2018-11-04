package com.nickskelton.wifidelity

import android.support.test.runner.AndroidJUnit4
import com.nickskelton.wifidelity.di.modules

import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
//        // Context of the app under test.
//        val appContext = InstrumentationRegistry.getTargetContext()
//        assertEquals("com.nickskelton.wifidelity", appContext.packageName)
        checkModules(modules)
    }
}
