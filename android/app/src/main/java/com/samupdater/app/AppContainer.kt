package com.samupdater.app

import android.content.Context
import com.samupdater.app.data.FirmwareRepository
import com.samupdater.app.data.device.DeviceInfoReader
import com.samupdater.app.data.fota.FotaApi
import com.samupdater.app.data.notes.ReleaseNotesApi
import com.samupdater.app.data.prefs.SettingsStore
import com.samupdater.app.data.site.SiteApi
import com.samupdater.app.domain.Catalog
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

/** Manual dependency wiring. Small app, so no DI framework. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .cache(Cache(File(appContext.cacheDir, "http"), HTTP_CACHE_BYTES))
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().header("User-Agent", USER_AGENT).build())
        }
        .build()

    val settings = SettingsStore(appContext, json)
    val deviceInfo = DeviceInfoReader()

    val repository = FirmwareRepository(
        site = SiteApi(httpClient, BuildConfig.SITE_BASE_URL, json),
        fota = FotaApi(httpClient),
        notesApi = ReleaseNotesApi(httpClient),
        bundledCatalog = ::loadBundledCatalog,
    )

    private fun loadBundledCatalog(): Catalog = try {
        appContext.assets.open(BUNDLED_CATALOG).bufferedReader().use { json.decodeFromString(Catalog.serializer(), it.readText()) }
    } catch (e: Exception) {
        android.util.Log.e("SamUpdater", "Bundled catalog is unreadable", e)
        Catalog()
    }

    private companion object {
        const val HTTP_CACHE_BYTES = 10L * 1024 * 1024
        const val TIMEOUT_SECONDS = 20L
        const val BUNDLED_CATALOG = "catalog.json"
        val USER_AGENT = "SamUpdater-Android/${BuildConfig.VERSION_NAME}"
    }
}
