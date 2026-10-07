package com.samupdater.app.data.site

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class SiteApiTest {

    private val server = MockWebServer()
    private lateinit var api: SiteApi
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    @Before
    fun setUp() {
        server.start()
        api = SiteApi(OkHttpClient(), server.url("/").toString(), json)
    }

    @After
    fun tearDown() = server.shutdown()

    private fun enqueue(body: String, code: Int = 200) =
        server.enqueue(MockResponse().setResponseCode(code).setBody(body))

    private suspend fun firmwareError(): IOException =
        runCatching { api.firmware("SM-S938B", "INS") }.exceptionOrNull() as IOException

    @Test
    fun `firmware posts the checker action and maps the reply`() = runTest {
        enqueue(
            """{"success":true,"data":{"device":"Galaxy S25 Ultra","model":"SM-S938B","csc":"INS",
              "build":"S938BXXU4BYJ2","android":"Android 16","one_ui":"8.5","release_date":"2025-10-06",
              "security_patch":"2025-10-01","history":[{"build":"S938BXXS3BYI1"}],"extra":1}}""",
        )

        val info = api.firmware("SM-S938B", "INS")

        val request = server.takeRequest()
        assertEquals("/wp-admin/admin-ajax.php", request.path)
        assertEquals("POST", request.method)
        assertEquals("action=samupdater_live_device_info&model=SM-S938B&csc=INS", request.body.readUtf8())
        assertEquals("S938BXXU4BYJ2", info.latest.build)
        assertEquals("8.5", info.oneUi)
        assertEquals(listOf("S938BXXS3BYI1"), info.history.map { it.build })
    }

    @Test
    fun `firmware surfaces the server error message`() = runTest {
        enqueue("""{"success":false,"data":{"message":"Unknown model."}}""")
        assertEquals("Unknown model.", firmwareError().message)
    }

    @Test
    fun `firmware handles admin-ajax zero and HTTP errors`() = runTest {
        enqueue("0")
        assertTrue(firmwareError().message.orEmpty().startsWith("No firmware found"))
        enqueue("oops", code = 503)
        assertEquals("Server returned 503", firmwareError().message)
    }

    @Test
    fun `catalog reads the app endpoint`() = runTest {
        enqueue("""{"updatedAt":"2026-10-01","devices":[{"modelPrefix":"SM-S938","name":"Galaxy S25 Ultra","launchAndroid":15,"osUpgrades":7}]}""")

        val catalog = api.catalog()

        assertEquals("/wp-json/samupdater-app/v1/catalog", server.takeRequest().path)
        assertEquals("Galaxy S25 Ultra", catalog.deviceFor("SM-S938B")?.name)
    }

    @Test
    fun `articles clean HTML from titles and excerpts`() = runTest {
        enqueue(
            """[{"link":"https://samupdater.com/a/","date":"2026-10-02T09:00:00",
              "title":{"rendered":"One UI 9 &#8211; what&#8217;s new"},
              "excerpt":{"rendered":"<p>Samsung started the beta &amp; more [&hellip;]</p>"}}]""",
        )

        val article = api.articles(limit = 5).single()

        assertTrue(server.takeRequest().path.orEmpty().contains("per_page=5"))
        assertEquals("One UI 9 - what's new", article.title)
        assertEquals("Samsung started the beta & more", article.excerpt)
        assertEquals("2026-10-02", article.date)
    }
}
