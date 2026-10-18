package com.example.recipe_app_compose.features.location.data.remote

import com.example.recipe_app_compose.core.util.Resource
import com.example.recipe_app_compose.features.location.domain.model.location.LocationData
import com.example.recipe_app_compose.features.location.domain.model.yelp.YelpSearchOrigin
import com.example.recipe_app_compose.features.location.domain.model.yelp.YelpSearchRequest
import com.example.recipe_app_compose.features.location.domain.model.yelp.YelpSearchResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class YelpRepositoryImplTest {
    @Test
    fun `coordinate request decodes businesses and snake case fields`() = runTest {
        val requests = mutableListOf<io.ktor.client.request.HttpRequestData>()
        val repository = repository(
            responseJson = YELP_RESPONSE,
            onRequest = requests::add,
        )

        val result = repository.searchShops(
            YelpSearchRequest(
                term = "restaurants",
                origin = YelpSearchOrigin.Coordinates(
                    LocationData(latitude = 28.18, longitude = -82.35),
                ),
                radiusMeters = 16_000,
            ),
        )

        assertIs<Resource.Success<YelpSearchResult>>(result)
        val shop = assertNotNull(result.data).shops.single()
        assertEquals("Recipe Cafe", shop.name)
        assertEquals(42u, shop.reviewCount)
        assertEquals("https://example.com/shop.jpg", shop.imageUrl)
        assertEquals("32541", shop.location.zipCode)
        assertEquals("New American • Breakfast & Brunch", shop.displayRestaurantStyle())
        assertEquals("(850) 555-1234", shop.displayPhoneNumber())

        val request = requests.single()
        assertEquals("Bearer test-key", request.headers[HttpHeaders.Authorization])
        assertEquals("restaurants", request.url.parameters["term"])
        assertEquals("28.18", request.url.parameters["latitude"])
        assertEquals("-82.35", request.url.parameters["longitude"])
        assertEquals("16000", request.url.parameters["radius"])
        assertNull(request.url.parameters["location"])
    }

    @Test
    fun `named location request omits coordinates`() = runTest {
        val requests = mutableListOf<io.ktor.client.request.HttpRequestData>()
        val repository = repository(
            responseJson = EMPTY_RESPONSE,
            onRequest = requests::add,
        )

        repository.searchShops(
            YelpSearchRequest(
                term = "coffee",
                origin = YelpSearchOrigin.NamedLocation("Austin, TX"),
            ),
        )

        val request = requests.single()
        assertEquals("Austin, TX", request.url.parameters["location"])
        assertNull(request.url.parameters["latitude"])
        assertNull(request.url.parameters["longitude"])
    }

    @Test
    fun `blank api key fails before making a request`() = runTest {
        var requestCount = 0
        val repository = repository(
            apiKey = "",
            responseJson = EMPTY_RESPONSE,
            onRequest = { requestCount++ },
        )

        val result = repository.searchShops(
            YelpSearchRequest(
                term = "restaurants",
                origin = YelpSearchOrigin.NamedLocation("Chicago"),
            ),
        )

        assertIs<Resource.Error<*>>(result)
        assertEquals("Yelp API key is not configured.", result.message)
        assertEquals(0, requestCount)
    }

    private fun repository(
        responseJson: String,
        apiKey: String = "test-key",
        onRequest: (io.ktor.client.request.HttpRequestData) -> Unit,
    ): YelpRepositoryImpl {
        val engine = MockEngine { request ->
            onRequest(request)
            respond(
                content = responseJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        return YelpRepositoryImpl(
            configuration = YelpApiConfiguration(
                apiKey = apiKey,
                baseUrl = "https://api.yelp.com/v3/",
            ),
            client = client,
        )
    }

    private companion object {
        const val EMPTY_RESPONSE = """{"total":0,"businesses":[]}"""
        val YELP_RESPONSE = """
            {
              "total": 1,
              "businesses": [{
                "id": "shop-1",
                "name": "Recipe Cafe",
                "rating": 4.5,
                "phone": "+18505551234",
                "is_closed": false,
                "review_count": 42,
                "image_url": "https://example.com/shop.jpg",
                "categories": [
                  {"alias": "newamerican", "title": "New American"},
                  {"alias": "breakfast_brunch", "title": "Breakfast & Brunch"},
                  {"alias": "cafes", "title": "Cafes"}
                ],
                "coordinates": {"latitude": 30.4, "longitude": -86.6},
                "location": {
                  "city": "Destin",
                  "country": "US",
                  "state": "FL",
                  "address1": "1 Main St",
                  "address2": "",
                  "address3": "",
                  "zip_code": "32541"
                }
              }]
            }
        """.trimIndent()
    }
}
