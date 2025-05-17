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

package features.highlightmediaresults

import android.content.Intent
import android.os.Build
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.MediaStore
import androidx.test.filters.SdkSuppress
import com.android.photopicker.core.configuration.PhotopickerConfiguration
import com.android.photopicker.core.configuration.PhotopickerRuntimeEnv
import com.android.photopicker.core.configuration.TestPhotopickerConfiguration
import com.android.photopicker.core.features.PrefetchResultKey
import com.android.photopicker.features.highlightmediaresults.HighlightMediaResultsFeature
import com.android.photopicker.features.highlightmediaresults.model.HighlightAlbumName
import com.android.photopicker.features.highlightmediaresults.model.HighlightQuery
import com.android.photopicker.features.highlightmediaresults.model.HighlightQueryResultsParams
import com.android.photopicker.features.highlightmediaresults.model.QueryResultsHighlightType
import com.android.photopicker.features.search.model.GlobalSearchState
import com.android.providers.media.flags.Flags
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
class HighlightMediaResultsFeatureTest {

    @get:Rule() var setFlagsRule = SetFlagsRule()

    val deferredPrefetchResultsMap: Map<PrefetchResultKey, Deferred<Any?>> =
        mapOf(
            PrefetchResultKey.SEARCH_STATE to
                runBlocking {
                    async {
                        return@async GlobalSearchState.ENABLED
                    }
                }
        )

    // All highlight search feature enabled tests should be tested only for ACTION_PICK_IMAGES.
    // Any other action will throw an exception while parsing the intent.
    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_PICKER_HIGHLIGHT_SEARCH_RESULTS_APIS,
        Flags.FLAG_HIGHLIGHT_SEARCH_RESULTS_FEATURE,
    )
    @DisableFlags(Flags.FLAG_ENABLE_PHOTOPICKER_SEARCH)
    fun testHighlightMediaFeatureWhenSearchIsDisabled() {
        val testActionPickImagesConfiguration: PhotopickerConfiguration =
            TestPhotopickerConfiguration.build {
                action(MediaStore.ACTION_PICK_IMAGES)
                intent(Intent(MediaStore.ACTION_PICK_IMAGES))
            }
        assertWithMessage(
                "HighlightMediaResults feature should be disabled when search is disabled"
            )
            .that(
                HighlightMediaResultsFeature.isEnabled(
                    testActionPickImagesConfiguration,
                    deferredPrefetchResultsMap,
                )
            )
            .isEqualTo(false)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_PHOTOPICKER_SEARCH)
    @DisableFlags(
        Flags.FLAG_ENABLE_PICKER_HIGHLIGHT_SEARCH_RESULTS_APIS,
        Flags.FLAG_HIGHLIGHT_SEARCH_RESULTS_FEATURE,
    )
    fun testHighlightMediaFeatureWhenHighlightMediaFlagsAreDisabled() {
        val testActionPickImagesConfiguration: PhotopickerConfiguration =
            TestPhotopickerConfiguration.build {
                action(MediaStore.ACTION_PICK_IMAGES)
                intent(Intent(MediaStore.ACTION_PICK_IMAGES))
            }
        assertWithMessage(
                "HighlightMediaResults feature should be disabled when its flags are disabled"
            )
            .that(
                HighlightMediaResultsFeature.isEnabled(
                    testActionPickImagesConfiguration,
                    deferredPrefetchResultsMap,
                )
            )
            .isEqualTo(false)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_PHOTOPICKER_SEARCH, Flags.FLAG_HIGHLIGHT_SEARCH_RESULTS_FEATURE)
    @DisableFlags(Flags.FLAG_ENABLE_PICKER_HIGHLIGHT_SEARCH_RESULTS_APIS)
    fun testHighlightMediaFeatureWhenHighlightMediaApiFlagIsDisabled() {
        val testActionPickImagesConfiguration: PhotopickerConfiguration =
            TestPhotopickerConfiguration.build {
                action(MediaStore.ACTION_PICK_IMAGES)
                intent(Intent(MediaStore.ACTION_PICK_IMAGES))
            }
        assertWithMessage(
                "HighlightMediaResults feature should be disabled when API flag is disabled"
            )
            .that(
                HighlightMediaResultsFeature.isEnabled(
                    testActionPickImagesConfiguration,
                    deferredPrefetchResultsMap,
                )
            )
            .isEqualTo(false)
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_PHOTOPICKER_SEARCH,
        Flags.FLAG_ENABLE_PICKER_HIGHLIGHT_SEARCH_RESULTS_APIS,
    )
    @DisableFlags(Flags.FLAG_HIGHLIGHT_SEARCH_RESULTS_FEATURE)
    fun testHighlightMediaFeatureWhenHighlightMediaFeatureFlagIsDisabled() {
        val testActionPickImagesConfiguration: PhotopickerConfiguration =
            TestPhotopickerConfiguration.build {
                action(MediaStore.ACTION_PICK_IMAGES)
                intent(Intent(MediaStore.ACTION_PICK_IMAGES))
            }
        assertWithMessage(
                "HighlightMediaResults feature should be disabled when feature flag is disabled"
            )
            .that(
                HighlightMediaResultsFeature.isEnabled(
                    testActionPickImagesConfiguration,
                    deferredPrefetchResultsMap,
                )
            )
            .isEqualTo(false)
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_PHOTOPICKER_SEARCH,
        Flags.FLAG_ENABLE_PICKER_HIGHLIGHT_SEARCH_RESULTS_APIS,
        Flags.FLAG_HIGHLIGHT_SEARCH_RESULTS_FEATURE,
    )
    fun testHighlightMediaFeatureWhenSearchAndHighlightMediaFeatureFlagAreEnabled() {
        val testActionPickImagesConfiguration: PhotopickerConfiguration =
            TestPhotopickerConfiguration.build {
                action(MediaStore.ACTION_PICK_IMAGES)
                intent(Intent(MediaStore.ACTION_PICK_IMAGES))
            }
        assertWithMessage(
                "HighlightMediaResults feature should be enabled when search and highlight media flags " +
                    "are enabled"
            )
            .that(
                HighlightMediaResultsFeature.isEnabled(
                    testActionPickImagesConfiguration,
                    deferredPrefetchResultsMap,
                )
            )
            .isEqualTo(true)
    }

    @Test
    @EnableFlags(
        Flags.FLAG_ENABLE_PHOTOPICKER_SEARCH,
        Flags.FLAG_ENABLE_PICKER_HIGHLIGHT_SEARCH_RESULTS_APIS,
        Flags.FLAG_HIGHLIGHT_SEARCH_RESULTS_FEATURE,
        Flags.FLAG_ENABLE_EMBEDDED_PHOTOPICKER,
    )
    fun testHighlightMediaFeatureInEmbeddedWhenSearchAndHighlightMediaFeatureFlagAreEnabled() {
        val testActionPickImagesConfiguration: PhotopickerConfiguration =
            TestPhotopickerConfiguration.build {
                runtimeEnv(PhotopickerRuntimeEnv.EMBEDDED)
                action(MediaStore.ACTION_PICK_IMAGES)
                intent(Intent(MediaStore.ACTION_PICK_IMAGES))
            }
        assertWithMessage(
                "HighlightMediaResults feature should be enabled when search and highlight media flags " +
                    "are enabled in embedded mode"
            )
            .that(
                HighlightMediaResultsFeature.isEnabled(
                    testActionPickImagesConfiguration,
                    deferredPrefetchResultsMap,
                )
            )
            .isEqualTo(true)
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_PHOTOPICKER_SEARCH)
    fun testHighlightMediaFeatureWhenAlbumHighlightIsRequested() {
        val testActionPickImagesConfiguration: PhotopickerConfiguration =
            TestPhotopickerConfiguration.build {
                action(MediaStore.ACTION_PICK_IMAGES)
                intent(Intent(MediaStore.ACTION_PICK_IMAGES))
                highlightQueryResultsParams(
                    HighlightQueryResultsParams(
                        queryResultsHighlightQuery =
                            HighlightQuery.Album(album = HighlightAlbumName.HIGHLIGHT_ALBUM_CAMERA),
                        queryResultsHighlightType =
                            QueryResultsHighlightType.HIGHLIGHT_MEDIA_SECTION,
                    )
                )
            }
        assertWithMessage(
                "HighlightMediaResults feature should be disabled when its flags are disabled"
            )
            .that(
                HighlightMediaResultsFeature.isEnabled(
                    testActionPickImagesConfiguration,
                    deferredPrefetchResultsMap,
                )
            )
            .isEqualTo(true)
    }
}
