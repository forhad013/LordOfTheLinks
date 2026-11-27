package com.greenrobotdev.lordofthelinks.bookmarks.screens.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.greenrobotdev.lordofthelinks.model.repository.CategoryRepository
import com.greenrobotdev.lordofthelinks.model.repository.TagRepository
import com.greenrobotdev.lordofthelinks.utils.MoleculeViewModel
import kotlinx.coroutines.flow.Flow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class LibraryViewModel : MoleculeViewModel<LibraryEvent, LibraryState>(), KoinComponent {

    private val categoryRepository: CategoryRepository by inject()
    private val tagRepository: TagRepository by inject()

    @Composable
    override fun models(events: Flow<LibraryEvent>): LibraryState =
        LibraryPresenter(categoryRepository, tagRepository)

    override fun initialValue(): LibraryState = LibraryState()
}
