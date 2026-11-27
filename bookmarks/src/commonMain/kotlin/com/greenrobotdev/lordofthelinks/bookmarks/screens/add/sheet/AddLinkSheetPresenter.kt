package com.greenrobotdev.lordofthelinks.bookmarks.screens.add.sheet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.Flow

@Composable
fun AddLinkSheetPresenter(
    events: Flow<AddLinkSheetEvent>,
    clipboardUrl: String?,
    initialUrl: String?,
): AddLinkSheetState {
    var state by remember {
        mutableStateOf(AddLinkSheetState(url = initialUrl.orEmpty(), clipboardUrl = clipboardUrl))
    }
    LaunchedEffect(Unit) {
        events.collect { event ->
            when (event) {
                is AddLinkSheetEvent.UrlChanged -> state = state.copy(url = event.url)
            }
        }
    }
    return state
}
