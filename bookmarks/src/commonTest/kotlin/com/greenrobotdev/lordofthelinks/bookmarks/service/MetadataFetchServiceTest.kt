package com.greenrobotdev.lordofthelinks.bookmarks.service

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MetadataFetchServiceTest {

    private val pageUrl = "https://example.com/blog/post"
    private var requests = 0

    private fun serviceAnswering(html: String) = MetadataFetchService(HttpClient(MockEngine { requests++; respond(html) }))

    private suspend fun fetch(html: String) = serviceAnswering(html).fetchMetadata(pageUrl).first().getOrThrow()

    private fun page(head: String, body: String = "") = "<html><head>$head</head><body>$body</body></html>"

    @Test
    fun openGraphWinsOverTwitterAndHtmlTags() = runTest {
        val metadata = fetch(
            page(
                """
                <title>HTML title</title>
                <meta name="twitter:title" content="Twitter title">
                <meta property="og:title" content="OG title">
                <meta name="description" content="Meta description">
                <meta name="twitter:description" content="Twitter description">
                <meta property="og:description" content="OG description">
                <meta name="twitter:image" content="https://cdn.example.com/twitter.png">
                <meta property="og:image" content="https://cdn.example.com/og.png">
                <meta name="application-name" content="App name">
                <meta property="og:site_name" content="Example Blog">
                """,
            ),
        )

        assertEquals("OG title", metadata.title)
        assertEquals("OG description", metadata.description)
        assertEquals("https://cdn.example.com/og.png", metadata.imageUrl)
        assertEquals("Example Blog", metadata.siteName)
        assertEquals(pageUrl, metadata.url)
    }

    @Test
    fun twitterCardIsTheSecondChoice() = runTest {
        val metadata = fetch(
            page(
                """
                <title>HTML title</title>
                <meta name="twitter:title" content="Twitter title">
                <meta name="twitter:description" content="Twitter description">
                <meta name="twitter:image" content="https://cdn.example.com/twitter.png">
                """,
            ),
        )

        assertEquals("Twitter title", metadata.title)
        assertEquals("Twitter description", metadata.description)
        assertEquals("https://cdn.example.com/twitter.png", metadata.imageUrl)
    }

    @Test
    fun plainHtmlFallbacks() = runTest {
        val metadata = fetch(
            page(
                """
                <title> Plain title </title>
                <link rel="image_src" href="/img/cover.png">
                <meta name="application-name" content="App name">
                """,
                body = "<p>First <b>paragraph</b> text</p><p>Second</p>",
            ),
        )

        assertEquals("Plain title", metadata.title)
        assertEquals("First paragraph text", metadata.description)
        assertEquals("https://example.com/img/cover.png", metadata.imageUrl)
        assertEquals("App name", metadata.siteName)
    }

    @Test
    fun metaDescriptionBeatsTheFirstParagraph() = runTest {
        val metadata = fetch(page("""<meta name="description" content="Meta description">""", body = "<p>Paragraph</p>"))

        assertEquals("Meta description", metadata.description)
    }

    @Test
    fun pageWithoutMetadataGivesEmptyValues() = runTest {
        val metadata = fetch(page(""))

        assertEquals("", metadata.title)
        assertEquals("", metadata.description)
        assertNull(metadata.imageUrl)
        assertNull(metadata.faviconUrl)
        assertNull(metadata.siteName)
    }

    @Test
    fun longTitleAndParagraphAreCut() = runTest {
        val metadata = fetch(page("<title>${"t".repeat(500)}</title>", body = "<p>${"p".repeat(500)}</p>"))

        assertEquals(200, metadata.title.length)
        assertEquals(300, metadata.description.length)
    }

    @Test
    fun imageUrlsAreMadeAbsoluteAndUnescaped() = runTest {
        suspend fun image(src: String) = fetch(page("""<meta property="og:image" content="$src">""")).imageUrl

        assertEquals("https://cdn.example.com/a.png", image("//cdn.example.com/a.png"))
        assertEquals("https://example.com/a.png", image("/a.png"))
        assertEquals("https://example.com/blog/a.png", image("a.png"))
        assertEquals("https://example.com/a.png?w=1&h=2", image("/a.png?w=1&amp;h=2"))
    }

    @Test
    fun faviconPreference() = runTest {
        suspend fun favicon(head: String) = fetch(page(head)).faviconUrl

        val all = """
            <link rel="apple-touch-icon" href="/apple.png">
            <link rel="icon" href="/icon.png">
            <link rel="shortcut icon" href="/shortcut.ico">
        """
        assertEquals("https://example.com/shortcut.ico", favicon(all))
        assertEquals("https://example.com/icon.png", favicon(all.lines().filterNot { "shortcut" in it }.joinToString("\n")))
        assertEquals("https://example.com/apple.png", favicon("""<link rel="apple-touch-icon" href="/apple.png">"""))
    }

    @Test
    fun invalidUrlsFailWithoutARequest() = runTest {
        val service = serviceAnswering(page("<title>t</title>"))

        for (url in listOf("", "   ", "ftp://example.com/file", "example.com/page", "http://a.b")) {
            assertIs<IllegalArgumentException>(service.fetchMetadata(url).first().exceptionOrNull(), url)
        }
        assertEquals(0, requests)
    }

    @Test
    fun httpErrorBecomesHttpStatusException() = runTest {
        val service = MetadataFetchService(HttpClient(MockEngine { respondError(HttpStatusCode.NotFound) }))

        val error = service.fetchMetadata(pageUrl).first().exceptionOrNull()

        assertIs<HttpStatusException>(error)
        assertEquals(404, error.status)
    }

    @Test
    fun blankPageFails() = runTest {
        val result = serviceAnswering("   ").fetchMetadata(pageUrl).first()

        assertEquals("Empty response from server", result.exceptionOrNull()?.message)
    }

    @Test
    fun networkErrorsGetAReadableMessage() = runTest {
        suspend fun messageFor(cause: String) =
            MetadataFetchService(HttpClient(MockEngine { throw Exception(cause) }))
                .fetchMetadata(pageUrl).first().exceptionOrNull()?.message

        assertEquals("Request timed out. The server took too long to respond.", messageFor("java.net.SocketTimeoutException: read"))
        assertEquals("Unable to reach the server. Please check the URL.", messageFor("UnknownHostException: example.com"))
        assertTrue(messageFor("boom")!!.startsWith("Network error"))
    }
}
