package com.example.di

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt Dependency Injection module for Firebase App Check.
 * Configures the Play Integrity provider to attest application authenticity and integrity,
 * ensuring only legitimate, untampered instances of the app can communicate with Firebase
 * services (Firestore, Firebase Storage, Firebase Auth, and Cloud Functions).
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseAppCheckModule {

    private const val TAG = "FirebaseAppCheck"

    @Provides
    @Singleton
    fun provideFirebaseAppCheck(
        @ApplicationContext context: Context
    ): FirebaseAppCheck? {
        return runCatching {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }

            val appCheck = FirebaseAppCheck.getInstance()

            val providerFactory = if (BuildConfig.DEBUG) {
                // In debug builds, try Play Integrity provider first, with debug provider fallback for emulator testing
                runCatching {
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                }.getOrElse {
                    Log.w(TAG, "Play Integrity provider fallback in debug mode: ${it.message}")
                    DebugAppCheckProviderFactory.getInstance()
                }
            } else {
                // In production, strictly enforce Google Play Integrity provider
                PlayIntegrityAppCheckProviderFactory.getInstance()
            }

            appCheck.installAppCheckProviderFactory(providerFactory)
            Log.i(TAG, "Firebase App Check installed with Play Integrity provider.")
            appCheck
        }.onFailure { error ->
            Log.w(TAG, "Firebase App Check initialization deferred: ${error.message}")
        }.getOrNull()
    }
}
