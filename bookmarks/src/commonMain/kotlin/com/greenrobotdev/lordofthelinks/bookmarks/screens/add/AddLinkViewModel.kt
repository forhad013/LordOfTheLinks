package com.greenrobotdev.lordofthelinks.bookmarks.screens.add

import com.greenrobotdev.lordofthelinks.bookmarks.domain.provider.LinkMetadataProvider
import androidx.compose.runtime.Composable
import com.greenrobotdev.lordofthelinks.model.repository.CategoryRepository
import com.greenrobotdev.lordofthelinks.model.repository.LinkRepository
import com.greenrobotdev.lordofthelinks.model.repository.TagRepository
import com.greenrobotdev.lordofthelinks.utils.MoleculeViewModel
import kotlinx.coroutines.flow.Flow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AddLinkViewModel(
    private val initialUrl: String? = null
) : MoleculeViewModel<AddLinkEvent, AddLinkState>(), KoinComponent {

    private val linkRepository: LinkRepository by inject()
    private val tagRepository: TagRepository by inject()
    private val categoryRepository: CategoryRepository by inject()
    private val metadataProvider: LinkMetadataProvider by inject()

    
    @Composable
    override fun models(events: Flow<AddLinkEvent>): AddLinkState {
        return AddLinkPresenter(
            initialState = AddLinkState(url = initialUrl.orEmpty(), askForUrl = initialUrl.isNullOrBlank()),
            events = events,
            linkRepository = linkRepository,
            tagRepository = tagRepository,
            categoryRepository = categoryRepository,
            metadataProvider = metadataProvider,
            initialUrl = initialUrl
        )
    }

    
    override fun initialValue(): AddLinkState {
        return AddLinkState()
    }
}
