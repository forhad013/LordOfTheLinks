package com.greenrobotdev.lordofthelinks.bookmarks.screens

import com.greenrobotdev.lordofthelinks.bookmarks.data.provider.HtmlLinkMetadataProvider
import com.greenrobotdev.lordofthelinks.bookmarks.domain.provider.LinkMetadataProvider
import com.greenrobotdev.lordofthelinks.testing.FakeCategoryRepository
import com.greenrobotdev.lordofthelinks.testing.FakeLinkRepository
import com.greenrobotdev.lordofthelinks.testing.FakeTagRepository
import com.greenrobotdev.lordofthelinks.testing.settle
import com.greenrobotdev.lordofthelinks.bookmarks.screens.add.AddLinkViewModel
import com.greenrobotdev.lordofthelinks.bookmarks.screens.categoryPicker.CategoryPickerEvent
import com.greenrobotdev.lordofthelinks.bookmarks.screens.categoryPicker.CategoryPickerViewModel
import com.greenrobotdev.lordofthelinks.bookmarks.screens.details.LinkDetailEvent
import com.greenrobotdev.lordofthelinks.bookmarks.screens.details.LinkDetailViewModel
import com.greenrobotdev.lordofthelinks.bookmarks.screens.library.LibraryTabViewModel
import com.greenrobotdev.lordofthelinks.bookmarks.screens.saved.SavedListEvent
import com.greenrobotdev.lordofthelinks.bookmarks.screens.saved.SavedListViewModel
import com.greenrobotdev.lordofthelinks.bookmarks.screens.tagPicker.TagPickerEvent
import com.greenrobotdev.lordofthelinks.bookmarks.screens.tagPicker.TagPickerViewModel
import com.greenrobotdev.lordofthelinks.model.Category
import com.greenrobotdev.lordofthelinks.model.Link
import com.greenrobotdev.lordofthelinks.model.SmartListCounts
import com.greenrobotdev.lordofthelinks.model.Tag
import com.greenrobotdev.lordofthelinks.model.repository.CategoryRepository
import com.greenrobotdev.lordofthelinks.model.repository.LinkRepository
import com.greenrobotdev.lordofthelinks.model.repository.TagRepository
import com.greenrobotdev.lordofthelinks.utils.MoleculeViewModel
import androidx.lifecycle.ViewModelStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * View models only wire Koin's repositories into their presenter (covered by the presenter tests).
 * These check that wiring: the right dependencies arrive, state flows out, events flow in.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelWiringTest {

    private val links = FakeLinkRepository(Link(id = "a", title = "Kotlin", url = "https://kotlinlang.org"))
    private val categories = FakeCategoryRepository(Category(id = "dev", name = "Dev"))
    private val tags = FakeTagRepository(Tag(id = "t", name = "ui", count = 0))
    private val scheduler = StandardTestDispatcher()
    private val store = ViewModelStore()
    private var started = 0

    /**
     * runTest that clears the view models at the end, like a closed screen. A view model left running
     * keeps its Molecule composition alive on this dead scheduler and breaks presenter tests run after it.
     */
    private fun test(body: suspend TestScope.() -> Unit): TestResult = runTest(scheduler) {
        try {
            body()
        } finally {
            store.clear()
            settle()
        }
    }

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(scheduler) // viewModelScope runs on Main
        startKoin {
            modules(module {
                single<LinkRepository> { links }
                single<CategoryRepository> { categories }
                single<TagRepository> { tags }
                single<LinkMetadataProvider> { HtmlLinkMetadataProvider(HttpClient(MockEngine { respondError(HttpStatusCode.NotFound) })) }
            })
        }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    /** Subscribes like a screen does (models only starts with a collector) and lets it load. */
    private suspend fun <E, S> TestScope.start(viewModel: MoleculeViewModel<E, S>): MoleculeViewModel<E, S> {
        store.put("${started++}", viewModel)
        backgroundScope.launch { viewModel.models.collect {} }
        settle()
        return viewModel
    }

    private suspend fun <E, S> MoleculeViewModel<E, S>.send(event: E) {
        take(event)
        settle()
    }

    @Test
    fun linkDetail() = test {
        val viewModel = start(LinkDetailViewModel("a"))
        assertEquals("Kotlin", viewModel.models.value.link?.title)

        viewModel.send(LinkDetailEvent.ToggleFavorite)

        assertTrue(links.link("a").isFavorite)
    }

    @Test
    fun addLinkStoresTheSharedUrl() = test {
        val viewModel = start(AddLinkViewModel("https://example.com/new"))

        assertEquals(viewModel.models.value.savedLinkId, links.links.value.single { it.url == "https://example.com/new" }.id)
    }

    @Test
    fun addLinkWithoutUrlAsksForOne() = test {
        val viewModel = start(AddLinkViewModel())

        assertTrue(viewModel.models.value.askForUrl)
        assertEquals(1, links.links.value.size)
    }

    @Test
    fun savedList() = test {
        val viewModel = start(SavedListViewModel())
        assertFalse(viewModel.models.value.isLoading)
        assertEquals(listOf("a"), viewModel.models.value.links.map { it.id })

        val before = links.link("a")
        viewModel.send(SavedListEvent.Archive("a"))
        assertTrue(links.link("a").isArchived)

        viewModel.send(SavedListEvent.Restore(before)) // the snackbar's Undo
        assertFalse(links.link("a").isArchived)

        viewModel.send(SavedListEvent.Opened("a"))
        assertTrue(links.link("a").isOpened)

        viewModel.send(SavedListEvent.Delete("a"))
        assertEquals(emptyList(), viewModel.models.value.links)
    }

    @Test
    fun libraryTabShowsCountsCategoriesAndTags() = test {
        categories.smartListCounts.value = SmartListCounts(unopened = 3, favorites = 2, archived = 1, uncategorized = 4)
        categories.linkCategories.value = mapOf("a" to "dev")

        val state = start(LibraryTabViewModel()).models.value

        assertEquals(listOf(3, 2, 1, 4), listOf(state.unopenedCount, state.favoritesCount, state.archivedCount, state.uncategorizedCount))
        assertEquals(1, state.categories.single().count)
        assertEquals(listOf("t"), state.tags.map { it.id })
    }

    @Test
    fun libraryTabFollowsChanges() = test {
        val viewModel = start(LibraryTabViewModel())

        categories.smartListCounts.value = SmartListCounts(unopened = 7, favorites = 0, archived = 0, uncategorized = 0)
        tags.tags.value = emptyList()
        settle()

        assertEquals(7, viewModel.models.value.unopenedCount)
        assertEquals(emptyList(), viewModel.models.value.tags)
    }

    @Test
    fun categoryPickerGetsItsRepositoryFromKoin() = test {
        val viewModel = start(CategoryPickerViewModel(linkId = "a"))
        assertEquals(listOf("dev"), viewModel.models.value.allCategories.map { it.id })

        viewModel.send(CategoryPickerEvent.Select("dev"))
        viewModel.send(CategoryPickerEvent.UseThis)

        assertEquals(mapOf("a" to "dev"), categories.linkCategories.value)
    }

    @Test
    fun tagPickerGetsItsRepositoryFromKoin() = test {
        val viewModel = start(TagPickerViewModel(multiSelect = true))
        assertEquals(listOf("t"), viewModel.models.value.tags.map { it.id })

        viewModel.send(TagPickerEvent.Tap("t"))

        assertEquals(setOf("t"), viewModel.models.value.checkedIds)
    }
}
