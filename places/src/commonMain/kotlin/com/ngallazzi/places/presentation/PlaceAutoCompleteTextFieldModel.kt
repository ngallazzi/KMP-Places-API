package com.ngallazzi.places.presentation

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ngallazzi.places.domain.Place
import com.ngallazzi.places.domain.PlaceDetails
import com.ngallazzi.places.domain.Suggestion
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
internal class PlaceAutoCompleteTextFieldModel(
    private val helper: PlacesHelper,
    private val languageCode: String,
    initialText: String,
    private val debounceMs: Long = 0,
) : ViewModel() {
    private val _uiState =
        MutableStateFlow(
            PlaceAutocompleteState(
                textFieldValue =
                    TextFieldValue(
                        text = initialText,
                        selection = TextRange(initialText.length),
                    ),
            ),
        )
    val uiState: StateFlow<PlaceAutocompleteState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            _searchQuery
                .debounce(debounceMs)
                .collectLatest { query ->
                    if (query.isEmpty()) return@collectLatest
                    getSuggestions(query, languageCode).fold(onSuccess = { places ->
                        _uiState.value =
                            _uiState.value.copy(
                                suggestions =
                                    places
                                        .map {
                                            val placeId = it.id
                                            Suggestion(
                                                placeId = placeId,
                                                description = it.extendedLabel,
                                                shortDescription = it.label,
                                            )
                                        }.distinct(),
                                isSuggestionsPopupExpanded = places.isNotEmpty(),
                                error = null,
                            )
                    }, onFailure = {
                        _uiState.value = _uiState.value.copy(error = it)
                    })
                }
        }
    }

    fun onValueChange(value: TextFieldValue) {
        if (value.text.isNotEmpty() && value.text != _uiState.value.textFieldValue.text) {
            _searchQuery.value = value.text
        } else if (value.text.isEmpty()) {
            _uiState.value =
                _uiState.value.copy(suggestions = listOf(), isSuggestionsPopupExpanded = false)
        }
        _uiState.value = _uiState.value.copy(textFieldValue = value)
    }

    private suspend fun getSuggestions(
        text: String,
        languageCode: String,
    ): Result<List<Place>> = helper.getAddressSuggestions(search = text, languageCode)

    suspend fun onSuggestionSelected(
        suggestion: Suggestion,
        onPlaceDetailsRetrieved: suspend (PlaceDetails) -> Unit,
    ) {
        viewModelScope.launch {
            _uiState.value =
                _uiState.value.copy(
                    textFieldValue =
                        _uiState.value.textFieldValue.copy(
                            text = suggestion.description,
                            selection = TextRange(suggestion.description.length),
                        ),
                    isSuggestionsPopupExpanded = false,
                    error = null,
                )
        }
        helper.getPlaceDetails(suggestion.placeId, languageCode).fold(onSuccess = {
            val placeDetails =
                PlaceDetails(
                    id = it.id,
                    shortAddress = it.shortAddress,
                    formattedAddress = it.formattedAddress,
                    postalCode = it.postalCode,
                    country = it.country,
                    city = it.city,
                )
            onPlaceDetailsRetrieved(placeDetails)
        }, onFailure = {
            _uiState.value = _uiState.value.copy(error = it)
        })
    }

    fun onSuggestionPopupDismissRequested() {
        _uiState.value = _uiState.value.copy(isSuggestionsPopupExpanded = false)
    }

    fun onClearText() {
        _searchQuery.value = ""
        _uiState.value = _uiState.value.copy(textFieldValue = TextFieldValue(""))
    }
}
