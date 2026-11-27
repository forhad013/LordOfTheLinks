package com.greenrobotdev.lordofthelinks.bookmarks.screens.library

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.greenrobotdev.lordofthelinks.model.Category
import com.greenrobotdev.lordofthelinks.model.Tag

data class LibraryState(
    val unopenedCount: Int = 0,
    val favoritesCount: Int = 0,
    val archivedCount: Int = 0,
    val uncategorizedCount: Int = 0,
    val remindersCount: Int = 0,
    val categories: List<Category> = emptyList(),
    val tags: List<Tag> = emptyList(),
    /** categoryId -> tag names assigned to the category, for the chips on category rows. */
    val categoryTagNames: Map<String, Set<String>> = emptyMap()
)

sealed interface LibraryEvent
