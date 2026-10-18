package com.example.recipe_app_compose.features.location.domain.model.yelp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class YelpSearchResult(
    val total: UInt = 0u,
    @SerialName("businesses") val shops: List<YelpShop> = emptyList(),
)

@Serializable
data class YelpShop(
    val rating: Double = 0.0,
    val phone: String? = null,
    val id: String = "",
    val alias: String = "",
    @SerialName("is_closed") val isClosed: Boolean = false,
    val categories: List<YelpCategories> = emptyList(),
    @SerialName("review_count") val reviewCount: UInt = 0u,
    val name: String = "",
    val url: String = "",
    val coordinates: YelpCoordinates,
    @SerialName("image_url") val imageUrl: String? = null,
    val location: YelpLocations,
    val distance: Double = 0.0,
) {
    fun displayRating(): String = rating.toString().removeSuffix(".0")

    fun displayRestaurantStyle(): String = categories
        .asSequence()
        .map(YelpCategories::title)
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinct()
        .take(MAX_DISPLAYED_CATEGORIES)
        .joinToString(" • ")

    fun displayPhoneNumber(): String {
        val originalPhone = phone.orEmpty()
        val digits = originalPhone.filter(Char::isDigit)
        val nationalNumber = when {
            digits.length == 11 && digits.startsWith('1') -> digits.drop(1)
            digits.length == 10 -> digits
            else -> return originalPhone
        }
        return "(${nationalNumber.take(3)}) ${nationalNumber.substring(3, 6)}-${nationalNumber.takeLast(4)}"
    }

    private companion object {
        const val MAX_DISPLAYED_CATEGORIES = 2
    }
}

@Serializable
data class YelpCategories(val alias: String = "", val title: String = "")

@Serializable
data class YelpCoordinates(val latitude: Double = 0.0, val longitude: Double = 0.0)

@Serializable
data class YelpLocations(
    val city: String = "",
    val country: String = "",
    val address2: String = "",
    val address3: String = "",
    val state: String = "",
    val address1: String = "",
    @SerialName("zip_code") val zipCode: String? = null,
)
