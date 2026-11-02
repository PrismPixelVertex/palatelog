package com.prismpixelvertex.palatelog

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

private const val TAG = "PalateLog_App"

@HiltAndroidApp
class PalateLogApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        try {
            com.mappls.sdk.maps.Mappls.getInstance(this)
            Log.d(TAG, "Mappls SDK initialized successfully")
            com.prismpixelvertex.palatelog.core.designsystem.MapProviderConfig.notifyMapplsInitialized(true)
        } catch (e: Exception) {
            Log.e(TAG, "Mappls SDK initialization failed", e)
            com.prismpixelvertex.palatelog.core.designsystem.MapProviderConfig.notifyMapplsInitialized(false)
        }
    }
}
