package com.example

import android.app.Application
import com.example.data.remote.sync.AttendanceFeeSyncScheduler
import com.example.data.remote.sync.CloudSyncWorker
import com.google.firebase.appcheck.FirebaseAppCheck
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SchoolApplication : Application() {

    @Inject
    lateinit var appCheck: dagger.Lazy<FirebaseAppCheck?>

    override fun onCreate() {
        super.onCreate()
        try {
            appCheck.get()
        } catch (_: Exception) {}

        try {
            CloudSyncWorker.schedulePeriodicSync(this)
        } catch (_: Exception) {}

        try {
            AttendanceFeeSyncScheduler.schedulePeriodicSync(this)
        } catch (_: Exception) {}
    }
}
