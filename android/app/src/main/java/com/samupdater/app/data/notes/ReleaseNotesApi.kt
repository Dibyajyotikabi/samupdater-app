package com.samupdater.app.data.notes

import com.samupdater.app.domain.NotesBlock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * Loads the formatted release notes from Samsung's own notes page. The site API only has them as one flat
 * line, so headings run into the text there.
 */
class ReleaseNotesApi(private val client: OkHttpClient) {

    suspend fun notes(sourceUrl: String, build: String): List<NotesBlock> = withContext(Dispatchers.IO) {
        val url = sourceUrl.toHttpUrlOrNull()
        // The URL comes from the site's JSON, so only follow it to Samsung's notes host.
        require(url != null && url.isHttps && url.host == SAMSUNG_NOTES_HOST) { "Not a Samsung release notes URL" }
        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Samsung notes page returned ${response.code}")
            ReleaseNotesParser.parse(response.body?.string().orEmpty(), build)
        }
    }

    private companion object {
        const val SAMSUNG_NOTES_HOST = "doc.samsungmobile.com"
    }
}
