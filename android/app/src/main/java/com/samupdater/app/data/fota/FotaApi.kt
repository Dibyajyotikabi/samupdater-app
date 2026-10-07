package com.samupdater.app.data.fota

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/** Samsung's public FOTA listing. Used for test build hashes, which the release notes site doesn't show. */
class FotaApi(private val client: OkHttpClient, private val baseUrl: String = DEFAULT_BASE) {

    suspend fun stable(model: String, csc: String): FotaVersionInfo = fetch(model, csc, "version.xml")

    suspend fun test(model: String, csc: String): FotaVersionInfo = fetch(model, csc, "version.test.xml")

    private suspend fun fetch(model: String, csc: String, file: String) = withContext(Dispatchers.IO) {
        require(MODEL.matches(model) && CSC.matches(csc)) { "Invalid model or CSC" }
        val request = Request.Builder().url("$baseUrl/$csc/$model/$file").build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful || !body.contains("<versioninfo")) {
                throw IOException("Samsung server didn't return firmware data (${response.code}).")
            }
            VersionXmlParser.parse(body)
        }
    }

    companion object {
        const val DEFAULT_BASE = "https://fota-cloud-dn.ospserver.net/firmware"
        val MODEL = Regex("^SM-[A-Z0-9]{3,10}$")
        val CSC = Regex("^[A-Z0-9]{3}$")
    }
}
