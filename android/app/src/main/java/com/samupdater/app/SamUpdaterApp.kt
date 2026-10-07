package com.samupdater.app

import android.app.Application
import com.samupdater.app.work.FirmwareCheckWorker
import com.samupdater.app.work.Notifier

class SamUpdaterApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifier.createChannels(this)
        FirmwareCheckWorker.schedule(this)
    }
}
