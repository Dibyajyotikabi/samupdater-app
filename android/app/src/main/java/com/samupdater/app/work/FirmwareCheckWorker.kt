package com.samupdater.app.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.samupdater.app.SamUpdaterApp
import com.samupdater.app.data.AppResult
import com.samupdater.app.data.getOrNull
import com.samupdater.app.domain.Catalog
import com.samupdater.app.domain.ChangeDetector
import com.samupdater.app.domain.Channel
import com.samupdater.app.domain.Observation
import com.samupdater.app.domain.TrackedDevice
import com.samupdater.app.widget.FirmwareWidget
import java.util.concurrent.TimeUnit

/** Checks every tracked device for new stable, beta and test builds, then notifies. */
class FirmwareCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as SamUpdaterApp).container
        val settings = container.settings.current()
        val devices = settings.allTracked
        if (devices.isEmpty()) return Result.success()

        val catalog = container.repository.catalog(force = true)
        val observed = devices.flatMap { observe(it, catalog) }
        val allowed = setOfNotNull(
            Channel.STABLE.takeIf { settings.alerts.stable },
            Channel.BETA.takeIf { settings.alerts.beta },
            Channel.TEST.takeIf { settings.alerts.test },
        )
        ChangeDetector.detect(settings.lastSeen, observed)
            .filter { it.channel in allowed }
            .forEach { Notifier.notify(applicationContext, it) }
        container.settings.setLastSeen(ChangeDetector.snapshot(observed))
        FirmwareWidget.refreshAll(applicationContext)

        val anyStableLoaded = observed.any { it.channel == Channel.STABLE }
        return if (anyStableLoaded || runAttemptCount >= MAX_RETRIES) Result.success() else Result.retry()
    }

    private suspend fun observe(device: TrackedDevice, catalog: Catalog): List<Observation> {
        val repo = (applicationContext as SamUpdaterApp).container.repository
        val stable = (repo.firmware(device.model, device.csc, force = true) as? AppResult.Success)?.value?.latest?.build
        val beta = catalog.betaFor(device.model).mapNotNull { it.latestBeta }.maxOrNull()
        val test = repo.testBuilds(device.model, device.csc, force = true).getOrNull()?.latestInTesting?.raw
        return listOfNotNull(
            stable?.let { Observation(device, Channel.STABLE, it) },
            beta?.let { Observation(device, Channel.BETA, it) },
            test?.let { Observation(device, Channel.TEST, it) },
        )
    }

    companion object {
        private const val PERIODIC_NAME = "firmware-check"
        private const val ONE_OFF_NAME = "firmware-check-now"
        private const val INTERVAL_HOURS = 8L
        private const val MAX_RETRIES = 3

        private val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<FirmwareCheckWorker>(INTERVAL_HOURS, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(PERIODIC_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun runNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<FirmwareCheckWorker>().setConstraints(constraints).build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(ONE_OFF_NAME, androidx.work.ExistingWorkPolicy.REPLACE, request)
        }
    }
}
