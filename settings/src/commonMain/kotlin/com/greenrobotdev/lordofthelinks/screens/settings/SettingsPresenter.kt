package com.greenrobotdev.lordofthelinks.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.greenrobotdev.lordofthelinks.utils.preferences.ThemePreferenceManager
import com.greenrobotdev.lordofthelinks.bookmarks.data.format.BackupReadException
import com.greenrobotdev.lordofthelinks.bookmarks.domain.manager.BackupManager
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Settings presenter: the saved theme, and saving a new one. */
@OptIn(ExperimentalTime::class)
@Composable
fun SettingsPresenter(
    events: Flow<SettingsEvent>,
    backupService: BackupManager,
    themePreferences: ThemePreferenceManager = remember { ThemePreferenceManager() },
): SettingsState {
    var state by remember {
        mutableStateOf(
            SettingsState(
                themeMode = when (themePreferences.getThemeMode()) {
                    is ThemePreferenceManager.ThemeMode.Light -> ThemeMode.Light
                    is ThemePreferenceManager.ThemeMode.Dark -> ThemeMode.Dark
                    is ThemePreferenceManager.ThemeMode.System -> ThemeMode.System
                },
            ),
        )
    }

    LaunchedEffect(Unit) {
        events.collect { event ->
            when (event) {
                is SettingsEvent.SetThemeMode -> {
                    themePreferences.setThemeMode(
                        when (event.themeMode) {
                            is ThemeMode.Light -> ThemePreferenceManager.ThemeMode.Light
                            is ThemeMode.Dark -> ThemePreferenceManager.ThemeMode.Dark
                            is ThemeMode.System -> ThemePreferenceManager.ThemeMode.System
                        },
                    )
                    state = state.copy(themeMode = event.themeMode)
                }
                SettingsEvent.Backup, SettingsEvent.ExportForBrowsers -> {
                    state = state.copy(isWorking = true, dataMessage = null)
                    state = runCatching {
                        if (event == SettingsEvent.Backup) backupService.backup(Clock.System.now().toEpochMilliseconds())
                        else backupService.browserExport()
                    }.fold(
                        onSuccess = { state.copy(pendingFile = it, isWorking = false) },
                        onFailure = { state.copy(isWorking = false, dataMessage = "Couldn't read your links. Nothing was saved.") },
                    )
                }
                is SettingsEvent.FileSaved -> state = state.copy(
                    pendingFile = null,
                    dataMessage = when {
                        !event.saved -> null
                        event.isBackup -> "Backed up ${count(event.linkCount, "link")}."
                        else -> "Exported ${count(event.linkCount, "link")} for browsers."
                    },
                )
                is SettingsEvent.Restore -> {
                    val text = event.text ?: return@collect
                    state = state.copy(isWorking = true, dataMessage = null)
                    val message = runCatching { backupService.restore(text) }.fold(
                        onSuccess = { r ->
                            buildList {
                                add("Added ${count(r.added, "link")}")
                                if (r.skipped > 0) add("skipped ${r.skipped} already saved")
                                if (r.categoriesCreated > 0) add("created ${count(r.categoriesCreated, "category", "categories")}")
                                if (r.tagsCreated > 0) add("created ${count(r.tagsCreated, "tag")}")
                            }.joinToString(", ") + "."
                        },
                        onFailure = { (it as? BackupReadException)?.message ?: "Couldn't restore this backup. Nothing was changed." },
                    )
                    state = state.copy(isWorking = false, dataMessage = message)
                }
            }
        }
    }
    return state
}

/** "1 link", "3 links". */
private fun count(n: Int, one: String, many: String = one + "s") = "$n ${if (n == 1) one else many}"
