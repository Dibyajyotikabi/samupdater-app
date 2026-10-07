package com.samupdater.app.data

import com.samupdater.app.data.fota.FotaApi
import com.samupdater.app.data.notes.ReleaseNotesApi
import com.samupdater.app.data.site.SiteApi
import com.samupdater.app.domain.Article
import com.samupdater.app.domain.Catalog
import com.samupdater.app.domain.FirmwareInfo
import com.samupdater.app.domain.NotesBlock
import com.samupdater.app.domain.TestBuildDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

data class RolloutEntry(val csc: String, val result: AppResult<FirmwareInfo>)

class FirmwareRepository(
    private val site: SiteApi,
    private val fota: FotaApi,
    private val notesApi: ReleaseNotesApi,
    private val bundledCatalog: () -> Catalog,
    private val testDecoder: TestBuildDecoder = TestBuildDecoder(),
) {
    private val firmwareCache = MemoryCache<String, FirmwareInfo>(FIRMWARE_TTL_MS)
    private val catalogCache = MemoryCache<Unit, Catalog>(CATALOG_TTL_MS)
    private val testCache = MemoryCache<String, TestBuildDecoder.Result>(FIRMWARE_TTL_MS)
    private val articleCache = MemoryCache<Unit, List<Article>>(CATALOG_TTL_MS)
    private val notesCache = MemoryCache<String, List<NotesBlock>>(CATALOG_TTL_MS)

    suspend fun firmware(model: String, csc: String, force: Boolean = false): AppResult<FirmwareInfo> =
        appCatching("Couldn't load firmware for $model ($csc). Check the model and CSC, then try again.") {
            firmwareCache.getOrLoad("$model|$csc", force) { site.firmware(model, csc) }
        }

    /** Curated catalog from samupdater.com. Falls back to the copy shipped with the app if the site is unreachable. */
    suspend fun catalog(force: Boolean = false): Catalog = try {
        catalogCache.getOrLoad(Unit, force) { site.catalog() }
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        android.util.Log.w("SamUpdater", "Catalog fetch failed, using bundled copy", e)
        bundledCatalog()
    }

    suspend fun rollout(model: String, cscs: List<String>, force: Boolean = false): List<RolloutEntry> = coroutineScope {
        val permits = Semaphore(ROLLOUT_CONCURRENCY)
        cscs.distinct().map { csc ->
            async { permits.withPermit { RolloutEntry(csc, firmware(model, csc, force)) } }
        }.awaitAll()
    }

    /** Decodes Samsung's hashed test builds. Needs the FOTA server, which can block some networks. */
    suspend fun testBuilds(model: String, csc: String, force: Boolean = false): AppResult<TestBuildDecoder.Result> =
        appCatching("Samsung's test server didn't respond. Test builds are unavailable right now.") {
            testCache.getOrLoad("$model|$csc", force) {
                val stableInfo = fota.stable(model, csc)
                val stable = stableInfo.latest ?: error("No stable build listed")
                val hashes = fota.test(model, csc).allEntries
                withContext(Dispatchers.Default) { testDecoder.decode(stable, hashes, stableInfo.allEntries) }
            }
        }

    /** Release notes with their headings and line breaks, read from Samsung's own notes page. */
    suspend fun releaseNotes(sourceUrl: String, build: String, force: Boolean = false): AppResult<List<NotesBlock>> =
        appCatching("Couldn't load Samsung's release notes.") {
            notesCache.getOrLoad("$sourceUrl|$build", force) { notesApi.notes(sourceUrl, build) }
        }

    suspend fun articles(force: Boolean = false): AppResult<List<Article>> =
        appCatching("Couldn't load articles from samupdater.com.") {
            articleCache.getOrLoad(Unit, force) { site.articles() }
        }

    private companion object {
        const val FIRMWARE_TTL_MS = 30 * 60 * 1000L
        const val CATALOG_TTL_MS = 6 * 60 * 60 * 1000L
        const val ROLLOUT_CONCURRENCY = 3
    }
}
