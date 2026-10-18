package com.example.recipe_app_compose.features.location.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.rememberAsyncImagePainter
import com.example.recipe_app_compose.core.components.AppHorizontalMediaCard
import com.example.recipe_app_compose.core.components.AppLoadingIndicator
import com.example.recipe_app_compose.features.location.domain.location.rememberLocationAccess
import com.example.recipe_app_compose.features.location.domain.model.yelp.YelpShop
import com.example.recipe_app_compose.features.location.domain.preferences.LocationPreferenceStore
import com.example.recipe_app_compose.features.location.domain.repo.YelpRepository
import com.example.recipe_app_compose.features.location.domain.states.YelpSearchArea
import com.example.recipe_app_compose.features.location.domain.states.YelpUiState
import com.example.recipe_app_compose.shared.generated.resources.Res
import com.example.recipe_app_compose.shared.generated.resources.choose_another_location
import com.example.recipe_app_compose.shared.generated.resources.choose_shop_location_message
import com.example.recipe_app_compose.shared.generated.resources.city_or_zip_code
import com.example.recipe_app_compose.shared.generated.resources.find_shops_near_you
import com.example.recipe_app_compose.shared.generated.resources.finding_nearby_shops
import com.example.recipe_app_compose.shared.generated.resources.location_permission_explanation
import com.example.recipe_app_compose.shared.generated.resources.location_unavailable
import com.example.recipe_app_compose.shared.generated.resources.location_unavailable_message
import com.example.recipe_app_compose.shared.generated.resources.nav_storefront
import com.example.recipe_app_compose.shared.generated.resources.near_your_location
import com.example.recipe_app_compose.shared.generated.resources.no_results_found
import com.example.recipe_app_compose.shared.generated.resources.open_app_settings
import com.example.recipe_app_compose.shared.generated.resources.or_enter_location_manually
import com.example.recipe_app_compose.shared.generated.resources.search_nearby_shops
import com.example.recipe_app_compose.shared.generated.resources.search_this_area
import com.example.recipe_app_compose.shared.generated.resources.searching_near_location
import com.example.recipe_app_compose.shared.generated.resources.try_again
import com.example.recipe_app_compose.shared.generated.resources.unable_to_load_shops
import com.example.recipe_app_compose.shared.generated.resources.use_my_location
import com.example.recipe_app_compose.ui.theme.AppSizes
import com.example.recipe_app_compose.ui.theme.AppSpacing
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/** Shared restaurant search, location-choice, and result-list experience. */
@Composable
fun SharedNearbyScreen(
    onShopSelected: (YelpShop) -> Unit,
    modifier: Modifier = Modifier,
) {
    val repository: YelpRepository = koinInject()
    val preferences: LocationPreferenceStore = koinInject()
    val locationAccess = rememberLocationAccess()
    val store = remember(repository, preferences, locationAccess) {
        NearbyStore(repository, preferences, locationAccess)
    }
    val uiState by store.uiState.collectAsState()
    val searchQuery by store.searchQuery.collectAsState()
    val manualLocationQuery by store.manualLocationQuery.collectAsState()

    LaunchedEffect(store) {
        store.restoreLocationPreference()
    }
    LaunchedEffect(store, locationAccess) {
        locationAccess.authorization.collect(store::onAuthorizationChanged)
    }
    DisposableEffect(store) {
        onDispose(store::close)
    }

    NearbyContent(
        uiState = uiState,
        searchQuery = searchQuery,
        manualLocationQuery = manualLocationQuery,
        onSearchQueryChange = store::onSearchTextChange,
        onManualLocationChange = store::onManualLocationChange,
        onSearchManualLocation = store::searchManualLocation,
        onUseCurrentLocation = store::useCurrentLocation,
        onOpenAppSettings = locationAccess::openAppSettings,
        onChooseAnotherLocation = store::chooseAnotherLocation,
        onRetry = store::retry,
        onShopSelected = onShopSelected,
        modifier = modifier,
    )
}

@Composable
private fun NearbyContent(
    uiState: YelpUiState,
    searchQuery: String,
    manualLocationQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onManualLocationChange: (String) -> Unit,
    onSearchManualLocation: () -> Unit,
    onUseCurrentLocation: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onChooseAnotherLocation: () -> Unit,
    onRetry: () -> Unit,
    onShopSelected: (YelpShop) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasSearchOrigin = uiState.searchArea == YelpSearchArea.CurrentLocation ||
        uiState.searchArea is YelpSearchArea.NamedLocation

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = AppSpacing.Medium),
    ) {
        if (hasSearchOrigin) {
            SearchAreaHeader(
                searchArea = uiState.searchArea,
                onChooseAnotherLocation = onChooseAnotherLocation,
            )
            NearbySearchField(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
            )
            Spacer(Modifier.height(AppSpacing.Small))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            when {
                uiState.searchArea == YelpSearchArea.RestoringPreference ->
                    AppLoadingIndicator(Modifier.align(Alignment.Center))

                uiState.searchArea == YelpSearchArea.LocationChoiceRequired ->
                    LocationFallbackContent(
                        title = stringResource(Res.string.find_shops_near_you),
                        message = stringResource(Res.string.choose_shop_location_message),
                        primaryLabel = stringResource(Res.string.use_my_location),
                        manualLocationQuery = manualLocationQuery,
                        onManualLocationChange = onManualLocationChange,
                        onSearchManualLocation = onSearchManualLocation,
                        onPrimaryAction = onUseCurrentLocation,
                        modifier = Modifier.align(Alignment.Center),
                    )

                uiState.searchArea == YelpSearchArea.PermissionRequired ->
                    LocationFallbackContent(
                        title = stringResource(Res.string.find_shops_near_you),
                        message = stringResource(Res.string.location_permission_explanation),
                        primaryLabel = stringResource(Res.string.open_app_settings),
                        manualLocationQuery = manualLocationQuery,
                        onManualLocationChange = onManualLocationChange,
                        onSearchManualLocation = onSearchManualLocation,
                        onPrimaryAction = onOpenAppSettings,
                        modifier = Modifier.align(Alignment.Center),
                    )

                uiState.searchArea == YelpSearchArea.ResolvingCurrentLocation ->
                    LocationLoadingContent(Modifier.align(Alignment.Center))

                uiState.searchArea == YelpSearchArea.LocationUnavailable ->
                    LocationFallbackContent(
                        title = stringResource(Res.string.location_unavailable),
                        message = stringResource(Res.string.location_unavailable_message),
                        primaryLabel = stringResource(Res.string.try_again),
                        manualLocationQuery = manualLocationQuery,
                        onManualLocationChange = onManualLocationChange,
                        onSearchManualLocation = onSearchManualLocation,
                        onPrimaryAction = onUseCurrentLocation,
                        modifier = Modifier.align(Alignment.Center),
                    )

                uiState.loading -> AppLoadingIndicator(Modifier.align(Alignment.Center))

                uiState.error != null -> SearchErrorContent(
                    message = uiState.error.orEmpty(),
                    onRetry = onRetry,
                    modifier = Modifier.align(Alignment.Center),
                )

                uiState.list.isEmpty() -> Text(
                    text = stringResource(Res.string.no_results_found),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center),
                )

                else -> NearbyShopList(uiState.list, onShopSelected)
            }
        }
    }
}

@Composable
private fun SearchAreaHeader(
    searchArea: YelpSearchArea,
    onChooseAnotherLocation: () -> Unit,
) {
    val label = when (searchArea) {
        YelpSearchArea.CurrentLocation -> stringResource(Res.string.near_your_location)
        is YelpSearchArea.NamedLocation ->
            stringResource(Res.string.searching_near_location, searchArea.value)
        else -> return
    }

    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f).padding(vertical = AppSpacing.Small),
        )
        TextButton(onClick = onChooseAnotherLocation) {
            Text(stringResource(Res.string.choose_another_location))
        }
    }
}

@Composable
private fun NearbySearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(Res.string.search_nearby_shops)) },
        singleLine = true,
        shape = RoundedCornerShape(AppSpacing.Large),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun LocationLoadingContent(modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        AppLoadingIndicator()
        Spacer(Modifier.height(AppSpacing.Medium))
        Text(
            text = stringResource(Res.string.finding_nearby_shops),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun LocationFallbackContent(
    title: String,
    message: String,
    primaryLabel: String,
    manualLocationQuery: String,
    onManualLocationChange: (String) -> Unit,
    onSearchManualLocation: () -> Unit,
    onPrimaryAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppSpacing.Large),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(AppSpacing.Large),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter = painterResource(Res.drawable.nav_storefront),
                contentDescription = null,
                modifier = Modifier.size(AppSpacing.ExtraLarge),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(AppSpacing.Medium))
            Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(AppSpacing.ExtraSmall))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(AppSpacing.Large))
            Button(onClick = onPrimaryAction, modifier = Modifier.fillMaxWidth()) {
                Text(primaryLabel)
            }
            Spacer(Modifier.height(AppSpacing.Medium))
            Text(
                text = stringResource(Res.string.or_enter_location_manually),
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(Modifier.height(AppSpacing.Small))
            ManualLocationField(
                value = manualLocationQuery,
                onValueChange = onManualLocationChange,
                onSearch = onSearchManualLocation,
                focusManager = focusManager,
            )
        }
    }
}

@Composable
private fun ManualLocationField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    focusManager: FocusManager,
) {
    fun submit() {
        if (value.isNotBlank()) {
            focusManager.clearFocus()
            onSearch()
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(Res.string.city_or_zip_code)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { submit() }),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(AppSpacing.Small))
    OutlinedButton(
        onClick = ::submit,
        enabled = value.isNotBlank(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(Res.string.search_this_area))
    }
}

@Composable
private fun SearchErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(AppSpacing.Large),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.unable_to_load_shops),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(AppSpacing.Small))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(AppSpacing.Large))
        Button(onClick = onRetry) { Text(stringResource(Res.string.try_again)) }
    }
}

@Composable
private fun NearbyShopList(
    shops: List<YelpShop>,
    onShopSelected: (YelpShop) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = AppSpacing.Small),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Medium),
    ) {
        items(shops, key = YelpShop::id) { shop ->
            val restaurantStyle = shop.displayRestaurantStyle()
            val phoneNumber = shop.displayPhoneNumber()

            AppHorizontalMediaCard(
                painter = rememberAsyncImagePainter(shop.imageUrl),
                imageDescription = shop.name,
                onClick = { onShopSelected(shop) },
                modifier = Modifier.fillMaxWidth(),
                imageSize = AppSizes.RestaurantCardImage,
            ) {
                Text(
                    text = shop.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = AppSpacing.ExtraSmall),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (restaurantStyle.isNotBlank()) {
                        Text(
                            text = restaurantStyle,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.tertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Text(
                        text = "${shop.displayRating()} ★",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = if (restaurantStyle.isNotBlank()) {
                            Modifier.padding(start = AppSpacing.Small)
                        } else {
                            Modifier
                        },
                    )
                }
                Text(
                    text = listOf(shop.location.address1, shop.location.city, shop.location.state)
                        .filter(String::isNotBlank)
                        .joinToString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = AppSpacing.Small),
                )
                if (phoneNumber.isNotBlank()) {
                    Text(
                        text = phoneNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = AppSpacing.ExtraSmall),
                    )
                }
            }
        }
    }
}
