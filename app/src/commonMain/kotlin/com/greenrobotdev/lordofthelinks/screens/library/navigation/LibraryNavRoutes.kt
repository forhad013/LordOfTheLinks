package com.greenrobotdev.lordofthelinks.screens.library.navigation

import androidx.navigation3.runtime.NavKey
import com.greenrobotdev.lordofthelinks.bookmarks.screens.saved.SavedScope

/**
 * Organize Tab Navigation Routes
 * Manages all screens within the Organize tab
 */
sealed class LibraryNavRoutes : NavKey {
    // Base organize screen - categoryNames and tags overview
    data object LibraryBase : LibraryNavRoutes()

    // Manage categories and tags (rename/move/delete)
    data object Manage : LibraryNavRoutes()

    // Saved links of one smart list, category (with subcategories) or tag
    data class SavedList(val scope: SavedScope) : LibraryNavRoutes()

    // A link's details, opened from a saved list
    data class LinkDetail(val linkId: String) : LibraryNavRoutes()
}
