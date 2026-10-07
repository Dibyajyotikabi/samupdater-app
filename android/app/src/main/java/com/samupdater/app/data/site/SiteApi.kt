package com.samupdater.app.data.site

import com.samupdater.app.domain.Article
import com.samupdater.app.domain.Catalog
import com.samupdater.app.domain.FirmwareInfo
import com.samupdater.app.domain.FirmwareRelease
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/** Reads firmware data, the curated catalog and articles from samupdater.com. */
class SiteApi(
    private val client: OkHttpClient,
    baseUrl: String,
    private val json: Json,
) {
    private val base: HttpUrl = baseUrl.toHttpUrl()

    /** Same endpoint the website's checker uses. Data comes from Samsung's official release notes. */
    suspend fun firmware(model: String, csc: String): FirmwareInfo = withContext(Dispatchers.IO) {
        val body = FormBody.Builder()
            .add("action", "samupdater_live_device_info")
            .add("model", model)
            .add("csc", csc)
            .build()
        val request = Request.Builder().url(endpoint("wp-admin/admin-ajax.php")).post(body).build()
        val text = execute(request)
        val notFound = "No firmware found for $model ($csc)."
        // Check success first: error replies carry {"message": ...} in data, which isn't a LiveDeviceDto.
        if (!isSuccess(text)) throw IOException(errorMessage(text) ?: notFound)
        val dto = json.decodeFromString<LiveDeviceEnvelope>(text).data ?: throw IOException(notFound)
        dto.toDomain()
    }

    /** admin-ajax.php answers a bare "0" for unknown actions, so anything that isn't an object counts as failure. */
    private fun isSuccess(text: String): Boolean = runCatching {
        json.parseToJsonElement(text).jsonObject["success"]?.jsonPrimitive?.booleanOrNull == true
    }.getOrDefault(false)

    private fun errorMessage(text: String): String? =
        runCatching { json.decodeFromString<LiveDeviceError>(text).data?.message }.getOrNull()

    suspend fun catalog(): Catalog = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(endpoint("wp-json/samupdater-app/v1/catalog")).build()
        json.decodeFromString<Catalog>(execute(request))
    }

    suspend fun articles(limit: Int = 15): List<Article> = withContext(Dispatchers.IO) {
        val url = endpoint("wp-json/wp/v2/posts").newBuilder()
            .addQueryParameter("per_page", limit.toString())
            .addQueryParameter("_fields", "link,date,title,excerpt")
            .build()
        json.decodeFromString<List<WpPostDto>>(execute(Request.Builder().url(url).build())).map {
            Article(
                title = HtmlText.clean(it.title.rendered),
                link = it.link,
                date = it.date.take(10),
                excerpt = HtmlText.clean(it.excerpt.rendered),
            )
        }
    }

    /** Paths are fixed constants, so a failed resolve is a programming error, not a network one. */
    private fun endpoint(path: String): HttpUrl =
        requireNotNull(base.resolve(path)) { "Cannot resolve $path against $base" }

    private fun execute(request: Request): String = client.newCall(request).execute().use { response ->
        val body = response.body?.string().orEmpty()
        if (!response.isSuccessful) throw IOException("Server returned ${response.code}")
        body
    }
}

private fun LiveDeviceDto.toDomain(): FirmwareInfo {
    val latest = FirmwareRelease(build, android, releaseDate, securityPatch, notes)
    return FirmwareInfo(
        deviceName = device,
        model = model,
        csc = csc,
        latest = latest,
        oneUi = oneUi,
        history = history.map { FirmwareRelease(it.build, it.android, it.releaseDate, it.securityPatch, it.notes) },
        sourceUrl = sourceUrl,
        fetchedAt = fetchedAt,
    )
}

internal object HtmlText {
    private val TAGS = Regex("<[^>]+>")
    private val SPACES = Regex("\\s+")
    private val ENTITIES = mapOf(
        "&amp;" to "&", "&quot;" to "\"", "&#039;" to "'", "&#8217;" to "'", "&#8216;" to "'",
        "&#8220;" to "\"", "&#8221;" to "\"", "&#8211;" to "-", "&#8212;" to "-", "&nbsp;" to " ",
        "&hellip;" to "...", "&#8230;" to "...", "&lt;" to "<", "&gt;" to ">", "[&hellip;]" to "",
    )

    fun clean(html: String): String {
        var text = TAGS.replace(html, " ")
        ENTITIES.forEach { (entity, value) -> text = text.replace(entity, value) }
        return SPACES.replace(text, " ").trim().removeSuffix("[...]").trim()
    }
}
