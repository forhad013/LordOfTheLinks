package com.greenrobotdev.lordofthelinks.bookmarks.components.picker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.greenrobotdev.lordofthelinks.design.foundation.icon.AppIcon

/** Inline field for creating or renaming a category at a given tree depth (design: add-sub / rename / new top-level). */
@Composable
internal fun InlineTreeField(
    name: String,
    onNameChange: (String) -> Unit,
    taken: Boolean,
    hint: String,
    errorHint: String,
    onDone: () -> Unit,
    depth: Int,
    icon: ImageVector = Icons.Filled.CreateNewFolder,
    modifier: Modifier = Modifier,
    placeholder: String = "Category name",
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        // Gutter matching SelectableRow's indent
        if (depth > 0) Box(modifier = Modifier.width((40 + (depth - 1) * 20).dp))
        Column(
            modifier = Modifier.weight(1f).padding(top = 4.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            com.greenrobotdev.lordofthelinks.design.components.textfield.AppTextFieldEnhanced(
                value = name,
                onValueChange = onNameChange,
                placeholder = placeholder,
                leadingIcon = icon,
                isError = taken,
                showClear = true,
                openStyle = true,
                textFieldModifier = Modifier.focusRequester(focus),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onDone() }),
            )
            if (taken) {
                Row(
                    modifier = Modifier.padding(start = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AppIcon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        errorHint,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            } else {
                Text(
                    hint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 14.dp),
                )
            }
        }
    }
}
