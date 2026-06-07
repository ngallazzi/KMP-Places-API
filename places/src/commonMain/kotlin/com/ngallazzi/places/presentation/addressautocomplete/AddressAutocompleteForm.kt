package com.ngallazzi.places.presentation.addressautocomplete

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ngallazzi.places.presentation.PlaceAutoCompleteTextField

/**
 * A composable form component for address autocompletion using the Places API.
 *
 * This form allows users to input an address with autocomplete suggestions and view autofilled
 * city, country, and postal code fields. The form also includes a submit button that is only
 * enabled when the form state is valid.
 *
 * @param modifier Modifier for styling and layout customization.
 * @param addressLabel Label for the address input field. Defaults to "Please enter your address".
 * @param cityLabel Label for the city field. Defaults to "City".
 * @param countryLabel Label for the country field. Defaults to "Country".
 * @param postalCodeLabel Label for the postal code field. Defaults to "Postal code".
 * @param ctaLabel Label for the call-to-action (submit) button. Defaults to "Submit".
 * @param debounceMs Debounce delay in milliseconds before making the API call while typing.
 *        Defaults to 0 (no debounce). See [PlaceAutoCompleteTextField].
 * @param formVerticalSpacing Vertical spacing between form elements. Defaults to 16.dp.
 * @param submitButtonHeight Height of the submit button. Defaults to 32.dp.
 * @param onSubmit Callback triggered when the submit button is clicked.
 * Receives the current [AddressAutocompleteFormState].
 *
 * @see AddressAutocompleteFormState
 * @see AddressAutocompleteFormViewModel
 * @see PlaceAutoCompleteTextField
 */

private val VERTICAL_SPACING = 16.dp
private val BUTTON_HEIGHT = 48.dp

@Composable
fun AddressAutocompleteForm(
    modifier: Modifier = Modifier,
    addressLabel: String = "Please enter your address",
    cityLabel: String = "City",
    countryLabel: String = "Country",
    postalCodeLabel: String = "Postal code",
    ctaLabel: String = "Submit",
    debounceMs: Long = 0,
    formVerticalSpacing: Dp = VERTICAL_SPACING,
    submitButtonHeight: Dp = BUTTON_HEIGHT,
    onSubmit: (state: AddressAutocompleteFormState) -> Unit,
) {
    val viewModel: AddressAutocompleteFormViewModel =
        remember {
            AddressAutocompleteFormViewModel()
        }
    val state = viewModel.uiState.collectAsState().value

    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PlaceAutoCompleteTextField(
            modifier = Modifier.fillMaxWidth(),
            label = addressLabel,
            onSuggestionSelected = { placeDetails ->
                viewModel.onAddressSelected(placeDetails)
            },
            text = state.address,
            onClearText = {
                viewModel.onAddressCleared()
            },
            debounceMs = debounceMs,
        )
        Spacer(modifier = Modifier.height(formVerticalSpacing))
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            label = { Text(cityLabel) },
            readOnly = true,
            value = state.city,
            onValueChange = {},
        )
        Spacer(modifier = Modifier.height(formVerticalSpacing))
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            label = { Text(countryLabel) },
            readOnly = true,
            value = state.country,
            onValueChange = {},
        )
        Spacer(modifier = Modifier.height(formVerticalSpacing))
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            label = { Text(postalCodeLabel) },
            value = state.postalCode,
            onValueChange = {
                viewModel.onPostalCodeChanged(it)
            },
        )
        Spacer(modifier = Modifier.height(formVerticalSpacing * 2))
        Button(
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = submitButtonHeight),
            onClick = {
                onSubmit(state)
            },
            content = { Text(ctaLabel) },
            enabled = state.isValid(),
        )
    }
}
