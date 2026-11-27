package com.greenrobotdev.lordofthelinks.bookmarks.domain.manager

import com.greenrobotdev.lordofthelinks.bookmarks.data.format.BackupCategory
import com.greenrobotdev.lordofthelinks.bookmarks.data.format.BackupLink
import com.greenrobotdev.lordofthelinks.bookmarks.data.format.LinksBackup
import com.greenrobotdev.lordofthelinks.bookmarks.domain.WebLinkHandler
import com.greenrobotdev.lordofthelinks.model.Category
import com.greenrobotdev.lordofthelinks.model.Link

/** What a restore will do, worked out before anything is written. */
data class ImportPlan(
    /** Categories to create, parents before children; parentKey is the backup id of the parent. */
    val categoriesToCreate: List<BackupCategory>,
    /** Backup category id → existing local category id (matched by path). */
    val matchedCategories: Map<String, String>,
    /** Tag names to create (not present locally, ignoring case). */
    val tagsToCreate: List<String>,
    val linksToAdd: List<BackupLink>,
    /** Links skipped because the same link is already saved (or repeats within the file). */
    val skipped: Int,
)

/**
 * Merges [backup] into what's saved: links already saved (same link, see WebLinkHandler.isSame) are skipped,
 * categories are matched by their path ("Dev › Android", ignoring case) and missing ones created
 * with the same tree, tags are matched by name. Nothing saved is changed or removed.
 */
fun planImport(
    backup: LinksBackup,
    existingLinks: List<Link>,
    existingCategories: List<Category>,
    existingTags: List<String>,
): ImportPlan {
    val byId = backup.categories.associateBy { it.id }
    fun backupPath(c: BackupCategory): String {
        val names = mutableListOf(c.name)
        var parent = c.parentId?.let(byId::get)
        val seen = mutableSetOf(c.id)
        while (parent != null && seen.add(parent.id)) { // a broken file could loop
            names.add(0, parent.name)
            parent = parent.parentId?.let(byId::get)
        }
        return names.joinToString(" › ").lowercase()
    }
    val localByPath = existingCategories.associateBy { it.path(existingCategories).lowercase() }

    val matched = mutableMapOf<String, String>()
    val toCreate = mutableListOf<BackupCategory>()
    // Parents first: a category is placed once its parent is matched or created
    val pending = backup.categories.toMutableList()
    while (pending.isNotEmpty()) {
        val ready = pending.filter { c -> c.parentId == null || c.parentId !in byId || c.parentId in matched || toCreate.any { it.id == c.parentId } }
        if (ready.isEmpty()) break // a cycle in the file: leave those out
        ready.forEach { c ->
            val local = localByPath[backupPath(c)]
            if (local != null) matched[c.id] = local.id
            else toCreate += c.copy(parentId = c.parentId?.takeIf { it in byId })
        }
        pending.removeAll(ready)
    }

    val kept = mutableListOf<BackupLink>()
    var skipped = 0
    backup.links.filter { it.url.isNotBlank() }.forEach { link ->
        val already = existingLinks.any { WebLinkHandler.isSame(it.url, link.url) } || kept.any { WebLinkHandler.isSame(it.url, link.url) }
        if (already) skipped++ else kept += link
    }
    skipped += backup.links.count { it.url.isBlank() }

    val have = existingTags.map { it.lowercase() }.toSet()
    val tagsToCreate = kept.flatMap { it.tags }.filter { it.isNotBlank() }
        .distinctBy { it.lowercase() }.filter { it.lowercase() !in have }

    return ImportPlan(toCreate, matched, tagsToCreate, kept, skipped)
}
