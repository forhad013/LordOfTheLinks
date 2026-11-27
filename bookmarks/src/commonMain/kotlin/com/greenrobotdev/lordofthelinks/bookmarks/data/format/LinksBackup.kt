package com.greenrobotdev.lordofthelinks.bookmarks.data.format

import com.greenrobotdev.lordofthelinks.bookmarks.screens.saved.title
import com.greenrobotdev.lordofthelinks.model.Category
import com.greenrobotdev.lordofthelinks.model.Link
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * A full backup of the library as JSON: every link field, the category tree, tags, and which link
 * is in which category / has which tags. [version] lets later app versions read old files.
 */
@Serializable
data class LinksBackup(
    val version: Int = CURRENT_VERSION,
    val app: String = "Lords of Links",
    val exportedAt: Long,
    val categories: List<BackupCategory> = emptyList(),
    val tags: List<String> = emptyList(),
    val links: List<BackupLink> = emptyList(),
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

@Serializable
data class BackupCategory(val id: String, val name: String, val parentId: String? = null)

@Serializable
data class BackupLink(
    val url: String,
    val title: String = "",
    /** The user's note. */
    val description: String = "",
    val fetchedDescription: String? = null,
    val imageUrl: String? = null,
    val type: String = "other",
    val isFavorite: Boolean = false,
    val isOpened: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: Long? = null,
    val tags: List<String> = emptyList(),
    val categoryId: String? = null,
)

private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = false }

fun buildBackup(
    links: List<Link>,
    categories: List<Category>,
    linkCategoryIds: Map<String, String>,
    tagNames: List<String>,
    nowMs: Long,
): LinksBackup = LinksBackup(
    exportedAt = nowMs,
    categories = categories.map { BackupCategory(it.id, it.name, it.parentId) },
    tags = tagNames.sorted(),
    links = links.map { link ->
        BackupLink(
            url = link.url, title = link.title, description = link.description,
            fetchedDescription = link.fetchedDescription, imageUrl = link.imageUrl, type = link.type,
            isFavorite = link.isFavorite, isOpened = link.isOpened, isArchived = link.isArchived,
            createdAt = link.createdAt, tags = link.tags, categoryId = linkCategoryIds[link.id],
        )
    },
)

fun LinksBackup.toJson(): String = json.encodeToString(LinksBackup.serializer(), this)

/** Thrown for a file that isn't a backup this version can read; the message is shown to the user. */
class BackupReadException(message: String) : Exception(message)

/** Reads a backup file. Untrusted input: anything that isn't a backup we understand is rejected. */
fun readBackup(text: String): LinksBackup {
    val backup = runCatching { json.decodeFromString(LinksBackup.serializer(), text) }
        .getOrElse { throw BackupReadException("This file isn't a Lords of Links backup.") }
    if (backup.version > LinksBackup.CURRENT_VERSION) {
        throw BackupReadException("This backup comes from a newer version of the app. Update the app, then try again.")
    }
    return backup
}
