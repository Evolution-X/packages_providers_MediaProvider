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

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.android.photopicker.core.configuration.PhotopickerConfiguration
import com.android.photopicker.core.events.RegisteredEventClass
import com.android.photopicker.core.features.FeatureManager
import com.android.photopicker.core.features.FeatureRegistration
import com.android.photopicker.core.features.FeatureToken
import com.android.photopicker.core.features.Location
import com.android.photopicker.core.features.LocationParams
import com.android.photopicker.core.features.PhotopickerUiFeature
import com.android.photopicker.core.features.PrefetchResultKey
import com.android.photopicker.core.features.Priority
import com.android.photopicker.features.highlightmediaresults.model.HighlightAlbumName
import com.android.photopicker.features.highlightmediaresults.model.HighlightQuery
import com.android.photopicker.features.search.SearchFeature
import kotlinx.coroutines.Deferred

/** Feature class for HighlightMediaResulst feature of the photopicker */
class HighlightMediaResultsFeature : PhotopickerUiFeature {

    companion object Registration : FeatureRegistration {

        override val TAG: String = "HighlightMediaResults"

        override fun isEnabled(
            config: PhotopickerConfiguration,
            deferredPrefetchResultsMap: Map<PrefetchResultKey, Deferred<Any?>>,
        ): Boolean {
            // Picker search should be enabled for highlight feature to be enabled
            // Then check for highlight feature flags. Both the feature flag and the API flag
            // should be enabled.
            // If search is not enabled, highlight feature should still be enabled for album
            // highlight.
            return isAlbumHighlightFeasible(config) ||
                isSearchHighlightFeasible(config, deferredPrefetchResultsMap)
        }

        override fun build(featureManager: FeatureManager) = HighlightMediaResultsFeature()

        private fun isAlbumHighlightFeasible(config: PhotopickerConfiguration): Boolean {
            val highlightQuery = config.highlightQueryResultsParams.queryResultsHighlightQuery
            return when (highlightQuery) {
                is HighlightQuery.Album ->
                    return highlightQuery.album != HighlightAlbumName.UNSET_HIGHLIGHT_ALBUM
                else -> false
            }
        }

        private fun isSearchHighlightFeasible(
            config: PhotopickerConfiguration,
            deferredPrefetchResultsMap: Map<PrefetchResultKey, Deferred<Any?>>,
        ): Boolean {
            if (SearchFeature.isEnabled(config, deferredPrefetchResultsMap)) {
                return config.flags.PICKER_HIGHLIGHT_MEDIA_APIS_ENABLED &&
                    config.flags.PICKER_HIGHLIGHT_MEDIA_FEATURE_ENABLED
            }
            return false
        }
    }

    override fun registerLocations(): List<Pair<Location, Int>> {
        return listOf(Pair(Location.HIGHLIGHT_MEDIA_CAROUSEL, Priority.HIGH.priority))
    }

    @Composable
    override fun compose(location: Location, modifier: Modifier, params: LocationParams) {
        when (location) {
            Location.HIGHLIGHT_MEDIA_CAROUSEL -> HighlightMedia()
            else -> {}
        }
    }

    override val token = FeatureToken.HIGHLIGHT_MEDIA_RESULTS.token

    override val eventsConsumed = setOf<RegisteredEventClass>()

    /** Events produced by the highlight media feature */
    override val eventsProduced = setOf<RegisteredEventClass>()
}
