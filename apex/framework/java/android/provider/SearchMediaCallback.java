/*
 * Copyright (C) 2025 The Android Open Source Project
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

package android.provider;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.os.Bundle;

import com.android.providers.media.flags.Flags;

import java.util.List;

/**
 * @hide
 */
@SystemApi
@FlaggedApi(Flags.FLAG_ENABLE_MEDIA_SEARCH)
public interface SearchMediaCallback {
    /**
     * Called when the search service successfully retrieves results for a given search request.
     *
     * <p>
     * The results will be sorted by relevance score.
     * </p>
     *
     * <p>
     * <b>Expected keys for the {@code extras} Bundle:</b>
     * <ul>
     * <li><b>{@code EXTRA_NEXT_PAGE_TOKEN}</b> ({@code String}): A token required for
     * fetching the next page of search results. The caller should pass this token
     * as-is in the {@code searchParams} when querying for the next page.</li>
     * </ul>
     * </p>
     *
     * @param searchId        the unique ID that identifies the search request
     * @param searchResults   a list of results for the given search text
     * @param extras          a {@code Bundle} containing additional information about the results
     */
    void onSearchResultsSuccess(@NonNull String searchId,
            @NonNull List<SearchMediaResult> searchResults, @NonNull Bundle extras);

    /**
     * Called when the search service fails to retrieve results for a given search request.
     *
     * @param searchId        the unique ID that identifies the search request
     * @param errorCode       the error code for the failure
     * @param errorMessage    a human-readable message describing the error
     * @param retryable       a boolean indicating whether the caller should retry the query
     */
    void onSearchResultsFailure(@NonNull String searchId, int errorCode,
            @NonNull  String errorMessage, boolean retryable);
}
