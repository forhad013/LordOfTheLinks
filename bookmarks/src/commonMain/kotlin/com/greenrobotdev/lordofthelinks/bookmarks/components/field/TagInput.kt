package com.greenrobotdev.lordofthelinks.bookmarks.components.field

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.greenrobotdev.lordofthelinks.design.extensions.dashBorder

// Pieces of the "Save to Lords of Links" screen (design: Save screen, share sheet and Add link)

/** Dashed pill input: type a tag, Enter adds it (creating the tag if new). */
@Composable
internal fun TagInput(onAdd: (String) -> Unit, modifier: Modifier = Modifier) {
    var text by remember { mutableStateOf("") }
    val submit = {
        if (text.isNotBlank()) {
            onAdd(text); text = ""
        }
    }
    Box(
        modifier = modifier
            .widthIn(min = 200.dp)
            .heightIn(min = 36.dp)
            .dashBorder(MaterialTheme.colorScheme.outline)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = text,
            // Enter/comma also commit, for hardware keyboards
            onValueChange = { v ->
                if (v.endsWith("\n") || v.endsWith(",")) {
                    text = v.dropLast(1); submit()
                } else text = v
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            decorationBox = { inner ->
                if (text.isEmpty()) {
                    Text(
                        "Type a tag, press enter",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                inner()
            },
        )
    }
}
