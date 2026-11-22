package com.greenrobotdev.lordofthelinks.screens.settings

import com.greenrobotdev.lordofthelinks.bookmarks.backup.LinksBackupService
import com.greenrobotdev.lordofthelinks.model.Category
import com.greenrobotdev.lordofthelinks.model.Link
import com.greenrobotdev.lordofthelinks.model.Tag
import com.greenrobotdev.lordofthelinks.testing.FakeCategoryRepository
import com.greenrobotdev.lordofthelinks.testing.FakeLinkRepository
import com.greenrobotdev.lordofthelinks.testing.FakeTagRepository
import com.greenrobotdev.lordofthelinks.testing.present
import com.greenrobotdev.lordofthelinks.utils.preferences.ThemePreferenceManager
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsPresenterTest {

    private val storage = MapSettings()

    /** A library with one link filed under a category and tagged. */
    private fun library(): Triple<FakeLinkRepository, FakeCategoryRepository, FakeTagRepository> {
        val links = FakeLinkRepository(Link(id = "a", title = "Kotlin", url = "https://kotlinlang.org", tags = listOf("lang"), createdAt = 1))
        val categories = FakeCategoryRepository(Category(id = "dev", name = "Dev"))
        categories.linkCategories.value = mapOf("a" to "dev")
        val tags = FakeTagRepository(Tag(id = "t", name = "lang", count = 0))
        tags.linkTags.value = mapOf("a" to setOf("t"))
        return Triple(links, categories, tags)
    }

    private fun empty() = Triple(FakeLinkRepository(), FakeCategoryRepository(), FakeTagRepository())

    private suspend fun TestScope.open(repos: Triple<FakeLinkRepository, FakeCategoryRepository, FakeTagRepository> = library()) =
        present { events ->
            SettingsPresenter(events, LinksBackupService(repos.first, repos.second, repos.third), ThemePreferenceManager(storage))
        }

    @Test
    fun themeIsSavedAndReadBack() = runTest {
        val settings = open()
        assertEquals(ThemeMode.System, settings.state.themeMode)

        settings.send(SettingsEvent.SetThemeMode(ThemeMode.Dark))

        assertEquals(ThemeMode.Dark, settings.state.themeMode)
        assertEquals(ThemeMode.Dark, open().state.themeMode) // a new screen reads the saved choice
    }

    @Test
    fun backupHandsAFileToTheScreenThenReportsTheCount() = runTest {
        val settings = open()

        settings.send(SettingsEvent.Backup)
        val file = assertNotNull(settings.state.pendingFile)
        assertEquals("application/json", file.mimeType)
        assertEquals(1, file.linkCount)
        assertFalse(settings.state.isWorking)

        settings.send(SettingsEvent.FileSaved(saved = true, linkCount = file.linkCount, isBackup = true))
        assertNull(settings.state.pendingFile)
        assertEquals("Backed up 1 link.", settings.state.dataMessage)
    }

    @Test
    fun cancelledSaveShowsNoMessage() = runTest {
        val settings = open()

        settings.send(SettingsEvent.ExportForBrowsers)
        assertEquals("text/html", settings.state.pendingFile?.mimeType)
        settings.send(SettingsEvent.FileSaved(saved = false, linkCount = 1, isBackup = false))

        assertNull(settings.state.pendingFile)
        assertNull(settings.state.dataMessage)
    }

    @Test
    fun unreadableLibraryFailsTheBackupWithoutAFile() = runTest {
        val (links, categories, tags) = library()
        val settings = present { events ->
            SettingsPresenter(events, LinksBackupService(BrokenLinks(links), categories, tags), ThemePreferenceManager(storage))
        }

        settings.send(SettingsEvent.Backup)

        assertNull(settings.state.pendingFile)
        assertEquals("Couldn't read your links. Nothing was saved.", settings.state.dataMessage)
        assertFalse(settings.state.isWorking)
    }

    @Test
    fun restoreIntoAnEmptyLibraryAddsEverything() = runTest {
        val source = open()
        source.send(SettingsEvent.Backup)
        val backup = source.state.pendingFile!!.content
        val target = empty()

        val settings = open(target)
        settings.send(SettingsEvent.Restore(backup))

        assertEquals("Added 1 link, created 1 category, created 1 tag.", settings.state.dataMessage)
        assertEquals(listOf("https://kotlinlang.org"), target.first.links.value.map { it.url })
    }

    @Test
    fun restoringTheSameBackupAgainSkipsWhatIsSaved() = runTest {
        val settings = open()
        settings.send(SettingsEvent.Backup)
        val backup = settings.state.pendingFile!!.content

        settings.send(SettingsEvent.Restore(backup))

        assertEquals("Added 0 links, skipped 1 already saved.", settings.state.dataMessage)
    }

    @Test
    fun restoringSomethingElseIsRejected() = runTest {
        val target = empty()
        val settings = open(target)

        settings.send(SettingsEvent.Restore("""{"not": "a backup"}"""))

        assertNotNull(settings.state.dataMessage)
        assertTrue(target.first.links.value.isEmpty())
        assertFalse(settings.state.isWorking)
    }

    @Test
    fun cancelledPickerDoesNothing() = runTest {
        val settings = open()

        settings.send(SettingsEvent.Restore(null))

        assertNull(settings.state.dataMessage)
        assertFalse(settings.state.isWorking)
    }

    /** Reading links fails, like a corrupt database. */
    private class BrokenLinks(real: FakeLinkRepository) : com.greenrobotdev.lordofthelinks.model.repository.LinkRepository by real {
        override suspend fun getLinksWithTags() = kotlinx.coroutines.flow.flowOf(Result.failure<List<Link>>(IllegalStateException("db closed")))
    }
}
