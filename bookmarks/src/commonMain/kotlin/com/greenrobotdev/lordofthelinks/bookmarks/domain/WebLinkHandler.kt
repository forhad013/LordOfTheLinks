package com.greenrobotdev.lordofthelinks.bookmarks.domain

/** Pure URL rules: validate, parse and compare web links. No data access. */
object WebLinkHandler {
    /**
     * Validates a user-entered web address. Shared by the add-link sheet and clipboard detection.
     */
    fun isValid(url: String): Boolean {
        return Regex("^https?://\\S+$", RegexOption.IGNORE_CASE).matches(url.trim())
    }

    /** Host without scheme, "www." or path, e.g. "youtube.com". Empty if the URL has none. */
    fun hostOf(url: String): String =
        url.trim().substringAfter("://").substringBefore('/').substringBefore('?').removePrefix("www.").lowercase()

    private val videoHosts = listOf("youtube.com", "youtu.be", "vimeo.com", "twitch.tv", "tiktok.com", "dailymotion.com")

    private val socialHosts = listOf("twitter.com", "x.com", "instagram.com", "facebook.com", "threads.net", "reddit.com", "linkedin.com", "mastodon.social", "bsky.app")

    /** Link type stored on LinkEntity.type: video | social | article. Guessed from the host only. */
    fun typeOf(url: String): String {
        val host = hostOf(url)
        fun matches(list: List<String>) = list.any { host == it || host.endsWith(".$it") }
        return when {
            matches(videoHosts) -> "video"
            matches(socialHosts) -> "social"
            else -> "article"
        }
    }

    /**
     * A URL reduced to what identifies the page, so the same link saved twice is spotted: ignores
     * http/https, a leading "www.", letter case in the host, a trailing "/", the #fragment and
     * utm_* tracking parameters. Other query parameters stay (they often pick the page).
     */
    fun normalize(url: String): String {
        val noFragment = url.trim().substringBefore('#')
        val withoutScheme = noFragment.substringAfter("://", noFragment)
        val hostAndPath = withoutScheme.substringBefore('?')
        val host = hostAndPath.substringBefore('/').lowercase().removePrefix("www.")
        val path = hostAndPath.substring(hostAndPath.substringBefore('/').length).trimEnd('/')
        val query = withoutScheme.substringAfter('?', "").split('&')
            .filter { it.isNotBlank() && !it.startsWith("utm_", ignoreCase = true) }
            .sorted()
            .joinToString("&")
        return host + path + (if (query.isEmpty()) "" else "?$query")
    }

    fun isSame(a: String, b: String): Boolean = normalize(a) == normalize(b)
}
