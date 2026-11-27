package com.greenrobotdev.lordofthelinks.screens.feed.navigation

import androidx.navigation3.runtime.NavKey

/**
 * Library Tab Navigation Routes
 * Manages all screens within the Library tab
 */
sealed class FeedNavRoutes : NavKey {
    // Base library screen
    data object FeedBase : FeedNavRoutes()

    // Link detail and editing
    data class LinkDetail(val linkId: String) : FeedNavRoutes()
    data class AddLink(val initialUrl: String? = null) : FeedNavRoutes()

    // Categories and tags management
    data object Categories : FeedNavRoutes()
    data object Tags : FeedNavRoutes()
    data object AddTag : FeedNavRoutes()
}
