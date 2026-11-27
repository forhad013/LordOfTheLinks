package com.greenrobotdev.lordofthelinks.bookmarks.domain.provider

import io.ktor.client.request.get
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/** Looks up a page's title, description and images. */
interface LinkMetadataProvider {
    suspend fun fetchMetadata(url: String): Flow<Result<LinkMetadata>>
}

/** The site answered but refused the page (4xx/5xx), unlike a network failure that may pass on retry. */
class HttpStatusException(val status: Int, description: String) : Exception("HTTP $status: $description")

@Serializable
data class LinkMetadata(
    val url: String,
    val title: String,
    val description: String,
    val imageUrl: String? = null,
    val faviconUrl: String? = null,
    val siteName: String? = null
) {
    companion object {
        /**
         * Creates empty metadata for fallback scenarios
         */
        fun empty(url: String) = LinkMetadata(
            url = url,
            title = "",
            description = "",
            imageUrl = null,
            faviconUrl = null,
            siteName = null
        )
    }
}
