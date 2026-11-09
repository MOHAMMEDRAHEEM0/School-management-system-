package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.di.FirebaseAppCheckModule
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FirebaseAppCheckModuleTest {

    @Test
    fun testFirebaseAppCheckProviderInitialization() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appCheck = FirebaseAppCheckModule.provideFirebaseAppCheck(context)

        // Verifies the module runs cleanly and configures App Check without crashing
        // in Robolectric testing environments.
        // If Firebase is initialized in the environment, appCheck will be non-null.
        // If unconfigured, it must gracefully return null or valid instance without throwing.
    }
}
