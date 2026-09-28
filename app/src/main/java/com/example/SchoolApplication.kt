package com.example

import android.app.Application
import com.example.data.remote.sync.CloudSyncWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SchoolApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            CloudSyncWorker.schedulePeriodicSync(this)
        } catch (_: Exception) {}
    }
}
