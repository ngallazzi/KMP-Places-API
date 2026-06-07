package com.ngallazzi.places.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.window.PopupProperties
import com.ngallazzi.places.KMPPlaces
import com.ngallazzi.places.domain.PlaceDetails
import kotlinx.coroutines.launch

/**
 * A composable text field with autocomplete support for places using the Google Places API.
 *
 * Displays place suggestions in a dropdown as the user types. Upon selection, the full place
 * details are fetched and returned via [onSuggestionSelected]. Includes an optional trailing clear button.
 *
 * @param modifier Modifier to apply to the text field.
 * @param label Label displayed inside the text field. Defaults to an empty string.
 * @param text Initial text value. Used for setting up the view model.
 * @param keyboardOptions Software keyboard options for the text field. Defaults to [KeyboardOptions.Default].
 * @param keyboardActions Actions triggered from the IME. Defaults to [KeyboardActions.Default].
 * @param singleLine Whether the text field is a single line. Defaults to false.
 * @param maxLines Maximum number of lines. Defaults to unlimited unless [singleLine] is true.
 * @param minLines Minimum number of lines. Defaults to 1.
 * @param enabled Whether the text field is enabled. Defaults to true.
 * @param readOnly Whether the text field is read-only. Defaults to false.
 * @param leadingIcon Optional composable displayed at the start of the text field.
 * @param supportingText Optional composable displayed below the text field.
 * @param languageCode The BCP-47 language code to use for suggestions. Defaults to the current locale.
 * @param debounceMs Debounce delay in milliseconds before making the API call while typing.
 *        Defaults to 0 (no debounce, fires on every keystroke). Set to a higher value (e.g. 300)
 *        to reduce API calls while the user is typing.
 * @param textStyle The style of the input text. Defaults to [LocalTextStyle.current].
 * @param colors Colors used for the text field. Defaults to [OutlinedTextFieldDefaults.colors].
 * @param shape The shape of the text field. Defaults to [OutlinedTextFieldDefaults.shape].
 * @param onSuggestionSelected Callback invoked when a suggestion is selected. Provides the full [PlaceDetails].
 * @param onClearText Callback invoked when the clear (X) icon is clicked. Defaults to a no-op.
 *
 * @see PlaceAutoCompleteTextFieldModel
 * @see PlaceDetails
 */

@Composable
fun PlaceAutoCompleteTextField(
    modifier: Modifier = Modifier,
    label: String = "",
    text: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    languageCode: String = Locale.current.language,
    debounceMs: Long = 0,
    textStyle: TextStyle = LocalTextStyle.current,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
    shape: Shape = OutlinedTextFieldDefaults.shape,
    onSuggestionSelected: suspend (PlaceDetails) -> Unit,
    onClearText: () -> Unit = {},
    onError: (Throwable?) -> Unit = {},
) {
    val helper =
        remember {
            PlacesHelper(KMPPlaces.getApiKey())
        }
    val coroutineScope = rememberCoroutineScope()

    val viewModel =
        remember(key1 = text) {
            PlaceAutoCompleteTextFieldModel(
                helper = helper,
                languageCode = languageCode,
                initialText = text,
                debounceMs = debounceMs,
            )
        }
    val state = viewModel.uiState.collectAsState()

    val actualLabel = @Composable { Text(label) }
    val dropDownMenuContent: @Composable ColumnScope.() -> Unit = {
        state.value.suggestions.forEach { suggestion ->
            DropdownMenuItem(modifier = Modifier.fillMaxWidth(), onClick = {
                coroutineScope.launch {
                    viewModel.onSuggestionSelected(suggestion, onPlaceDetailsRetrieved = {
                        onSuggestionSelected(it)
                    })
                }
            }, text = { Text(suggestion.description) })
        }
    }

    Box {
        OutlinedTextField(
            modifier = modifier,
            value = state.value.textFieldValue,
            onValueChange = { newValue: TextFieldValue ->
                viewModel.onValueChange(newValue)
            },
            singleLine = true,
            label = actualLabel,
            keyboardOptions = keyboardOptions,
            textStyle = textStyle,
            colors = colors,
            shape = shape,
            keyboardActions = keyboardActions,
            maxLines = maxLines,
            minLines = minLines,
            enabled = enabled,
            readOnly = readOnly,
            leadingIcon = leadingIcon,
            trailingIcon = {
                if (text.isNotEmpty()) {
                    IconButton(onClick = {
                        viewModel.onClearText()
                        onClearText()
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear text")
                    }
                }
            },
            supportingText = supportingText,
        )
        DropdownMenu(
            properties = PopupProperties(focusable = false, dismissOnClickOutside = true),
            expanded = state.value.isSuggestionsPopupExpanded,
            onDismissRequest = {
                viewModel.onSuggestionPopupDismissRequested()
            },
            content = dropDownMenuContent,
        )
        if (state.value.error != null) {
            onError(state.value.error)
        }
    }
}
