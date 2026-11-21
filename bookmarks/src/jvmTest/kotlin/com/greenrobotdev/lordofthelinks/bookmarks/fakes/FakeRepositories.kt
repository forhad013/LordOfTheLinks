package com.greenrobotdev.lordofthelinks.bookmarks.fakes

import com.greenrobotdev.lordofthelinks.model.Category
import com.greenrobotdev.lordofthelinks.model.Link
import com.greenrobotdev.lordofthelinks.model.SmartListCounts
import com.greenrobotdev.lordofthelinks.model.Tag
import com.greenrobotdev.lordofthelinks.model.repository.CategoryRepository
import com.greenrobotdev.lordofthelinks.model.repository.LinkRepository
import com.greenrobotdev.lordofthelinks.model.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * In-memory repositories for presenter and view model tests. Reads are live flows like Room's;
 * set [failWrites] to make every write return a failure with [FAILURE].
 */
const val FAILURE = "disk full"

abstract class FakeRepository {
    var failWrites = false
    private var nextId = 0
    protected fun newId(prefix: String) = "$prefix-${++nextId}"
    protected fun <T> write(block: () -> T): Flow<Result<T>> =
        flowOf(if (failWrites) Result.failure(Exception(FAILURE)) else runCatching(block))
}

class FakeLinkRepository(vararg links: Link) : FakeRepository(), LinkRepository {
    val links = MutableStateFlow(links.toList())

    fun link(id: String) = links.value.single { it.id == id }

    private fun change(id: String, update: (Link) -> Link) =
        links.update { all -> all.map { if (it.id == id) update(it) else it } }

    override suspend fun getLinks() = links.map { Result.success(it) }
    override suspend fun getLinksWithTags() = getLinks()
    override suspend fun toggleFavorite(linkId: String) = write { change(linkId) { it.copy(isFavorite = !it.isFavorite) }; link(linkId) }
    override suspend fun addLink(link: Link) = write {
        val saved = link.copy(id = link.id.ifEmpty { newId("link") })
        links.update { it + saved }
        saved
    }
    override suspend fun deleteLink(linkId: String) = write { links.update { all -> all.filterNot { it.id == linkId } } }
    override suspend fun updateLink(link: Link) = write { change(link.id) { link }; link }
    override suspend fun markOpened(linkId: String) = write { change(linkId) { it.copy(isOpened = true) } }
    override suspend fun setImageUrl(linkId: String, imageUrl: String) = write { change(linkId) { it.copy(imageUrl = imageUrl) } }
}

class FakeTagRepository(vararg tags: Tag) : FakeRepository(), TagRepository {
    val tags = MutableStateFlow(tags.toList())
    /** linkId → tag ids */
    val linkTags = MutableStateFlow<Map<String, Set<String>>>(emptyMap())

    private val withCounts = combine(this.tags, linkTags) { all, assigned ->
        all.map { tag -> tag.copy(count = assigned.values.count { tag.id in it }) }.sortedBy { it.name }
    }

    override suspend fun getTags() = getTagsWithCounts()
    override suspend fun getTagsWithCounts() = withCounts.map { Result.success(it) }
    override suspend fun getTagById(id: String) = tags.map { all -> Result.success(all.firstOrNull { it.id == id }) }
    override suspend fun addTag(tag: Tag) = write {
        val saved = tag.copy(id = tag.id.ifEmpty { newId("tag") })
        tags.update { it + saved }
        saved
    }
    override suspend fun updateTag(tag: Tag) = write { tags.update { all -> all.map { if (it.id == tag.id) tag else it } }; tag }
    override suspend fun deleteTag(tagId: String) = write {
        tags.update { all -> all.filterNot { it.id == tagId } }
        linkTags.update { all -> all.mapValues { it.value - tagId } }
    }
    override suspend fun getTagsForLink(linkId: String) =
        combine(tags, linkTags) { all, assigned -> Result.success(all.filter { it.id in assigned[linkId].orEmpty() }) }
    override suspend fun assignTagToLink(linkId: String, tagId: String) =
        write { linkTags.update { it + (linkId to (it[linkId].orEmpty() + tagId)) } }
    override suspend fun removeTagFromLink(linkId: String, tagId: String) =
        write { linkTags.update { it + (linkId to (it[linkId].orEmpty() - tagId)) } }
}

class FakeCategoryRepository(vararg categories: Category) : FakeRepository(), CategoryRepository {
    val categories = MutableStateFlow(categories.toList())
    /** linkId → categoryId */
    val linkCategories = MutableStateFlow<Map<String, String>>(emptyMap())
    var recent: List<Category> = emptyList()

    private fun descendantsOf(id: String): Set<String> =
        categories.value.filter { it.parentId == id }.flatMap { descendantsOf(it.id) + it.id }.toSet()

    private fun remove(ids: Set<String>) {
        categories.update { all -> all.filterNot { it.id in ids } }
        linkCategories.update { all -> all.filterValues { it !in ids } }
    }

    override suspend fun getCategories() = categories.map { Result.success(it) }
    override suspend fun getCategoryById(id: String) = categories.map { all -> Result.success(all.firstOrNull { it.id == id }) }
    override suspend fun addCategory(category: Category) = write {
        val saved = category.copy(id = category.id.ifEmpty { newId("category") })
        categories.update { it + saved }
        saved
    }
    override suspend fun renameCategory(categoryId: String, newName: String) =
        write { categories.update { all -> all.map { if (it.id == categoryId) it.copy(name = newName) else it } } }
    override suspend fun moveCategory(categoryId: String, newParentId: String?) =
        write { categories.update { all -> all.map { if (it.id == categoryId) it.copy(parentId = newParentId) else it } } }
    override suspend fun deleteCategory(categoryId: String) = write {
        check(descendantsOf(categoryId).isEmpty()) { "Category has subcategories" }
        remove(setOf(categoryId))
    }
    override suspend fun deleteCategoryKeepChildren(categoryId: String) = write {
        val parentId = categories.value.single { it.id == categoryId }.parentId
        categories.update { all -> all.map { if (it.parentId == categoryId) it.copy(parentId = parentId) else it } }
        remove(setOf(categoryId))
    }
    override suspend fun deleteCategoryTree(categoryId: String) = write { remove(descendantsOf(categoryId) + categoryId) }
    override suspend fun getCategoryForLink(linkId: String) =
        flowOf(Result.success(categories.value.firstOrNull { it.id == linkCategories.value[linkId] }))
    override suspend fun assignLinkToCategory(linkId: String, categoryId: String) =
        write { linkCategories.update { it + (linkId to categoryId) } }
    override suspend fun clearLinkCategory(linkId: String) = write { linkCategories.update { it - linkId } }
    override suspend fun getRecentCategories() = flowOf(Result.success(recent))
    override fun getLinkCategoryIds() = linkCategories.map { Result.success(it) }
    override suspend fun getCategoriesWithCounts() = combine(categories, linkCategories) { all, assigned ->
        Result.success(all.map { c -> c.copy(count = assigned.values.count { it == c.id }) })
    }
    override suspend fun getSmartListCounts() = flowOf(Result.success(SmartListCounts(0, 0, 0, 0)))
}
