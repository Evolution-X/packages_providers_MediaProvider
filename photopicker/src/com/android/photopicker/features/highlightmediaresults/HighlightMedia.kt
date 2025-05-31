/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.photopicker.features.highlightmediaresults

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.android.photopicker.R
import com.android.photopicker.core.components.MediaGridItem
import com.android.photopicker.core.components.defaultBuildMediaItem
import com.android.photopicker.core.components.defaultBuildSeparator
import com.android.photopicker.core.configuration.LocalPhotopickerConfiguration
import com.android.photopicker.core.features.LocationParams
import com.android.photopicker.core.navigation.LocalNavController
import com.android.photopicker.core.obtainViewModel
import com.android.photopicker.core.selection.LocalSelection
import com.android.photopicker.data.model.Group
import com.android.photopicker.extensions.navigateToAlbumMediaGridForCategories
import com.android.photopicker.features.albumgrid.AlbumGridViewModel
import com.android.photopicker.features.categorygrid.CategoryGridViewModel
import com.android.photopicker.features.highlightmediaresults.model.HighlightAlbum
import com.android.photopicker.features.highlightmediaresults.model.HighlightQuery
import com.android.photopicker.features.highlightmediaresults.model.HighlightQueryResultsParams
import com.android.photopicker.features.highlightmediaresults.model.QueryResultsHighlightType
import com.android.photopicker.features.search.SearchViewModel
import com.android.photopicker.util.LocalLocalizationHelper
import java.text.DateFormat
import kotlinx.coroutines.flow.Flow

val RECENTS_LABEL_PADDING = PaddingValues(top = 16.dp)
val HIGHLIGHT_TEXT_LABELS_PADDING = PaddingValues(bottom = 8.dp, start = 16.dp, end = 16.dp)
val MEASUREMENT_HIGHLIGHT_GRID_HEIGHT = 176.dp
val HIGHLIGHT_GRID_CONTENT_PADDING = PaddingValues(start = 16.dp, end = 16.dp, bottom = 8.dp)
val MEASUREMENT_HIGHLIGHT_GRID_CELL_ARRANGEMENT = 8.dp
const val HIGHLIGHT_GRID_CELL_COUNT = 10
const val HIGHLIGHT_GRID_ROW_COUNT = 1

/**
 * A composable function which displays a media highlight section based on the given input highlight
 * params.
 *
 * @param params [LocationParams.WithLongClickAction] type params defining the long click behavior
 *   of highlight media items.
 * @param modifier The modifier to be applied to the composable if any
 */
@Composable
fun HighlightMedia(params: LocationParams = LocationParams.None, modifier: Modifier = Modifier) {
    val highlightParams: HighlightQueryResultsParams =
        LocalPhotopickerConfiguration.current.highlightQueryResultsParams
    val highlightQuery: HighlightQuery = highlightParams.queryResultsHighlightQuery

    if (!checkHighlightParamsValidity(highlightParams)) {
        return
    }
    val longClickActionParams = params as? LocationParams.WithLongClickAction
    val onItemLongClick: (item: MediaGridItem) -> Unit = { item ->
        longClickActionParams?.onLongClick(item)
    }
    when (highlightQuery) {
        is HighlightQuery.Search ->
            HighlightSearchQueryMedia(
                highlightQuery = highlightQuery.searchQuery,
                modifier = modifier,
                onItemLongClick = onItemLongClick,
            )
        is HighlightQuery.Album -> {
            HighlightAlbumMedia(
                highlightAlbum = highlightQuery.album,
                onItemLongClick = onItemLongClick,
                modifier = modifier,
            )
        }
    }
}

/**
 * The highlight composable to be rendered in case the app wants to highlight an album
 *
 * @param highlightAlbum The input album of type [HighlightAlbum] mapped from the given input album
 *   string
 * @param viewModel [AlbumGridViewModel] object to communicate with the data layer
 * @param onItemLongClick Callback triggered when a media item is long-pressed.
 * @param modifier The modifier to be applied to the given composable
 */
@Composable
fun HighlightAlbumMedia(
    highlightAlbum: HighlightAlbum,
    viewModel: CategoryGridViewModel = obtainViewModel(),
    onItemLongClick: (item: MediaGridItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Create the album object to fetch the album media. Fetching the album media requires only
    // the base album data: album id and its authority.
    val highlightBaseAlbum =
        Group.BaseAlbum(
            id = highlightAlbum.albumId,
            authority = viewModel.getLocalAlbumAuthority(),
            // TODO Add album name as a resource string for localisation: b/420605240
            displayName = highlightAlbum.albumId,
        )

    val albumMediaItems =
        viewModel.getHighlightAlbumMedia(highlightBaseAlbum).collectAsLazyPagingItems()
    val navController = LocalNavController.current

    Column(modifier = Modifier.fillMaxWidth()) {
        // Show the album name and the "See All" button
        // TODO Add album name as a resource string for localisation: b/420605240
        HighlightQueryAndSeeAllButton(
            highlightText = highlightBaseAlbum.displayName,
            onClick = {
                navController.navigateToAlbumMediaGridForCategories(album = highlightBaseAlbum)
            },
        )
        // Show the horizontal highlight grid
        HighlightMediaGrid(
            highlightItems = albumMediaItems,
            viewModel = viewModel,
            highlightAlbum = highlightBaseAlbum,
            onItemLongClick = onItemLongClick,
        )
        // Display the "Recents" label below the highlight grid
        RecentsLabel()
    }
}

/**
 * The highlight composable to be rendered in case an app wants to highlight media based on a search
 * query.
 *
 * @param highlightQuery The input highlight text query to highlight
 * @param viewModel [SearchViewModel] object to communicate with the data layer
 * @param onItemLongClick Callback triggered when a media item is long-pressed.
 * @param modifier The modifier to be applied if any
 */
@Composable
fun HighlightSearchQueryMedia(
    highlightQuery: String,
    viewModel: SearchViewModel = obtainViewModel(isActivityScoped = true),
    onItemLongClick: (item: MediaGridItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val highlightMediaItems =
        getSearchHighlightMediaItems(highlightQuery, viewModel).collectAsLazyPagingItems()

    Column(modifier = Modifier.fillMaxWidth()) {
        // Show the search query and the "See All" button
        HighlightQueryAndSeeAllButton(
            highlightText = highlightQuery,
            onClick = { setSearchParametersForHighlightMedia(highlightQuery, viewModel) },
        )

        // Show the horizontal highlight grid
        HighlightMediaGrid(
            highlightItems = highlightMediaItems,
            viewModel = viewModel,
            onItemLongClick = onItemLongClick,
        )
        // Display the "Recents" label below the highlight grid
        RecentsLabel()
    }
}

/** Displays the "Recents" label below the Highlight grid */
@Composable
private fun RecentsLabel() {
    Row(modifier = Modifier.padding(RECENTS_LABEL_PADDING)) {
        defaultBuildSeparator(
            MediaGridItem.SeparatorItem(
                label = stringResource(R.string.photopicker_hsr_recents_label)
            )
        )
    }
}

/**
 * Displays the highlight text which could either be the input search text query or the album name
 * along with the "See All" button in a horizontal row.
 *
 * @param highlightText The highlight text - could be the search text or the album name
 * @param onClick Defines the onClick behaviour of the See All button.
 */
@Composable
private fun HighlightQueryAndSeeAllButton(highlightText: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(HIGHLIGHT_TEXT_LABELS_PADDING),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = highlightText,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        TextButton(onClick = onClick) {
            Text(
                text = stringResource(R.string.photopicker_hsr_see_all_button_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/**
 * Displays a horizontally scrollable highlight media grid containing items based on the given
 * highlight query. The number of items in the grid is either [HIGHLIGHT_GRID_CELL_COUNT] or all
 * items based on the query, whichever is lower. The items in the grid are selectable.
 *
 * @param highlightItems The items to be displayed in the grid as [LazyPagingItems]
 * @param viewModel An instance of [ViewModel] passed down as [SearchViewModel] for search highlight
 *   or [AlbumGridViewModel] for album highlight.
 * @param highlightAlbum A [Group.Album] to highlight in case of album highlight.
 */
@Composable
private fun HighlightMediaGrid(
    highlightItems: LazyPagingItems<MediaGridItem.MediaItem>,
    viewModel: ViewModel,
    onItemLongClick: (item: MediaGridItem) -> Unit,
    highlightAlbum: Group.BaseAlbum? = null,
) {
    val state = rememberLazyGridState()
    val selectionLimit = LocalPhotopickerConfiguration.current.selectionLimit
    val selection by LocalSelection.current.flow.collectAsStateWithLifecycle()
    val selectionLimitExceededMessage =
        stringResource(R.string.photopicker_selection_limit_exceeded_snackbar, selectionLimit)
    val dateFormat =
        LocalLocalizationHelper.current.getLocalizedDateTimeFormatter(
            dateStyle = DateFormat.MEDIUM,
            timeStyle = DateFormat.SHORT,
        )

    LazyHorizontalGrid(
        rows = GridCells.Fixed(HIGHLIGHT_GRID_ROW_COUNT),
        modifier = Modifier.fillMaxWidth().height(MEASUREMENT_HIGHLIGHT_GRID_HEIGHT),
        state = state,
        contentPadding = HIGHLIGHT_GRID_CONTENT_PADDING,
        horizontalArrangement = Arrangement.spacedBy(MEASUREMENT_HIGHLIGHT_GRID_CELL_ARRANGEMENT),
        userScrollEnabled = true,
    ) {
        items(
            count = minOf(highlightItems.itemCount, HIGHLIGHT_GRID_CELL_COUNT),
            key = { index -> MediaGridItem.keyFactory(highlightItems.peek(index), index) },
        ) { index ->
            val highlightMediaItem: MediaGridItem? = highlightItems.get(index)
            if (highlightMediaItem != null && highlightMediaItem is MediaGridItem.MediaItem) {
                defaultBuildMediaItem(
                    highlightMediaItem,
                    isHighlightMediaItem = true,
                    isSelected = selection.contains(highlightMediaItem.media),
                    selectedPosition = selection.indexOf(highlightMediaItem.media),
                    onClick = {
                        when (viewModel) {
                            is SearchViewModel -> {
                                viewModel.handleGridItemSelection(
                                    item = highlightMediaItem.media,
                                    selectionLimitExceededMessage = selectionLimitExceededMessage,
                                )
                            }
                            is CategoryGridViewModel -> {
                                viewModel.handleAlbumMediaGridItemSelection(
                                    highlightMediaItem.media,
                                    selectionLimitExceededMessage,
                                    highlightAlbum!!,
                                )
                            }
                        }
                    },
                    onLongPress = { onItemLongClick(highlightMediaItem) },
                    dateFormat = dateFormat,
                    focusItem = null,
                )
            }
        }
    }
}

/** Returns a flow containing the media items based on the given input search query. */
private fun getSearchHighlightMediaItems(
    highlightQuery: String,
    viewModel: SearchViewModel,
): Flow<PagingData<MediaGridItem.MediaItem>> {
    return viewModel.getHighlightSearchResults(searchQuery = highlightQuery)
}

/** Sets the search params to show all the search results when the See All button is clicked */
private fun setSearchParametersForHighlightMedia(
    highlightQuery: String,
    viewModel: SearchViewModel,
) {
    viewModel.performSearch(query = highlightQuery)
    viewModel.setSearchBarFocusedState(true)
    viewModel.setSearchBarText(text = highlightQuery)
}

/**
 * Checks the validity of the input highlight params to make a final call on whether the highlight
 * section will be displayed or not. In case of empty highlight query or if the highlight type is
 * not [QueryResultsHighlightType.HIGHLIGHT_MEDIA_SECTION], we simply return.
 */
private fun checkHighlightParamsValidity(highlightParams: HighlightQueryResultsParams): Boolean {
    val highlightType = highlightParams.queryResultsHighlightType
    // HighlightMedia carousel should only be shown for HIGHLIGHT_MEDIA_SECTION highlight type
    val validHighlightType = highlightType == QueryResultsHighlightType.HIGHLIGHT_MEDIA_SECTION
    // An empty search query and UNSET album type won't show any results
    val validQuery =
        when (highlightParams.queryResultsHighlightQuery) {
            is HighlightQuery.Search -> {
                highlightParams.queryResultsHighlightQuery.searchQuery.isNotEmpty()
            }
            else -> true
        }
    return validHighlightType && validQuery
}
