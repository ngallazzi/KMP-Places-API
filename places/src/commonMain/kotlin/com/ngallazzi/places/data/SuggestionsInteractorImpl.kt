package com.ngallazzi.places.data

import com.ngallazzi.places.domain.Address
import com.ngallazzi.places.domain.City
import com.ngallazzi.places.domain.Country
import com.ngallazzi.places.domain.PlacesDataSource
import com.ngallazzi.places.domain.SuggestionsInteractor

internal class SuggestionsInteractorImpl(
    private val placesDataSource: PlacesDataSource,
) : SuggestionsInteractor {
    override suspend fun getCountrySuggestions(
        search: String,
        languageCode: String
    ): Result<List<Country>> {
        return runCatching {
            val response = placesDataSource.searchCountry(name = search, languageCode).getOrThrow()
            val countries = response.predictions.map { Country(it.placeId, it.description) }
            countries
        }
    }

    override suspend fun getCitySuggestions(
        search: String,
        languageCode: String
    ): Result<List<City>> {
        return runCatching {
            val response =
                placesDataSource.searchCity(name = search, languageCode).getOrThrow()
            val cities =
                response.predictions.map { City(it.placeId, it.structuredFormatting.mainText) }
            cities
        }
    }

    override suspend fun getAddressSuggestions(
        search: String,
        languageCode: String
    ): Result<List<Address>> {
        return runCatching {
            val response =
                placesDataSource.searchAddress(address = search, languageCode)
                    .getOrThrow()
            val predictions =
                response.predictions.map { Address(it.placeId, it.structuredFormatting.mainText) }
            predictions
        }
    }
}