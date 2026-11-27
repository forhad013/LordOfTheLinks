package com.greenrobotdev.lordofthelinks.bookmarks.screens.feed

import com.greenrobotdev.lordofthelinks.bookmarks.data.store.FeedFilterStore
import com.greenrobotdev.lordofthelinks.bookmarks.domain.provider.LinkMetadataProvider
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

class FeedViewModel : MoleculeViewModel<FeedEvent, FeedState>(), KoinComponent {

    private val linkRepository: LinkRepository by inject()
    private val categoryRepository: CategoryRepository by inject()
    private val tagRepository: TagRepository by inject()
    private val metadataProvider: LinkMetadataProvider by inject()
    private val filterStore = FeedFilterStore()

    @Composable
    override fun models(events: Flow<FeedEvent>): FeedState =
        FeedPresenter(events, linkRepository, categoryRepository, tagRepository, filterStore, metadataProvider)

    override fun initialValue(): FeedState = FeedState()
}
