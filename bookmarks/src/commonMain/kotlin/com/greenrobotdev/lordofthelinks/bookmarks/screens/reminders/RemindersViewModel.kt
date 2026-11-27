package com.greenrobotdev.lordofthelinks.bookmarks.screens.reminders

import androidx.compose.runtime.Composable
import com.greenrobotdev.lordofthelinks.model.repository.LinkRepository
import com.greenrobotdev.lordofthelinks.utils.MoleculeViewModel
import kotlinx.coroutines.flow.Flow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RemindersViewModel : MoleculeViewModel<RemindersEvent, RemindersState>(), KoinComponent {

    private val linkRepository: LinkRepository by inject()

    @Composable
    override fun models(events: Flow<RemindersEvent>): RemindersState =
        RemindersPresenter(events, linkRepository)

    override fun initialValue(): RemindersState = RemindersState()
}
