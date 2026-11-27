package com.greenrobotdev.lordofthelinks.bookmarks.domain

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.serialization.Serializable

/** Phases of the swipe/confirm delete flow. SWIPING is UI-local drag progress; state stays CONFIRM. */
@Serializable
enum class DeletePhase { NONE, CONFIRM, SWIPING, UNDO }

/** Undo window for a swiped category (ms). */
const val DELETE_UNDO_MS = 5_000L
