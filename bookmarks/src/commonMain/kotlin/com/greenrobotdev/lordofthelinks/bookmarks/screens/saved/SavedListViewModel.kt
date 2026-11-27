package com.greenrobotdev.lordofthelinks.bookmarks.screens.saved

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.greenrobotdev.lordofthelinks.model.repository.CategoryRepository
import com.greenrobotdev.lordofthelinks.model.repository.LinkRepository
import com.greenrobotdev.lordofthelinks.model.repository.TagRepository
import com.greenrobotdev.lordofthelinks.utils.MoleculeViewModel
import kotlinx.coroutines.flow.Flow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** One Library list (a smart list, category or tag). Live: edits elsewhere show up here at once. */
class SavedListViewModel : MoleculeViewModel<SavedListEvent, SavedListState>(), KoinComponent {

    private val linkRepository: LinkRepository by inject()
    private val categoryRepository: CategoryRepository by inject()
    private val tagRepository: TagRepository by inject()

    @Composable
    override fun models(events: Flow<SavedListEvent>): SavedListState =
        SavedListPresenter(events, linkRepository, categoryRepository, tagRepository)

    override fun initialValue(): SavedListState = SavedListState()
}
