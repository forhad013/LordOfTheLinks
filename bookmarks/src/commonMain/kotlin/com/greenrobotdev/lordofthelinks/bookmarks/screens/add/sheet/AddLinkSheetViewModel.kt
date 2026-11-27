package com.greenrobotdev.lordofthelinks.bookmarks.screens.add.sheet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.greenrobotdev.lordofthelinks.utils.MoleculeViewModel
import kotlinx.coroutines.flow.Flow

class AddLinkSheetViewModel(
    private val clipboardUrl: String?,
    private val initialUrl: String? = null,
) : MoleculeViewModel<AddLinkSheetEvent, AddLinkSheetState>() {

    @Composable
    override fun models(events: Flow<AddLinkSheetEvent>): AddLinkSheetState =
        AddLinkSheetPresenter(events, clipboardUrl, initialUrl)

    override fun initialValue(): AddLinkSheetState =
        AddLinkSheetState(url = initialUrl.orEmpty(), clipboardUrl = clipboardUrl)
}
