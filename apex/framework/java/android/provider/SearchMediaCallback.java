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
import android.database.CursorWindow;
import android.os.Bundle;
import android.os.OutcomeReceiver;

import com.android.providers.media.flags.Flags;

/**
 * <p> A callback interface passed to {@link SearchMediaService} functions which will be
 * used to return results for the method
 * {@link SearchMediaService#onSearchMedia(String, String, Bundle, OutcomeReceiver)} </p>
 * @hide
 */
@SystemApi
@FlaggedApi(Flags.FLAG_ENABLE_MEDIA_SEARCH)
public interface SearchMediaCallback {
    /**
     * Called when the search service successfully retrieves results for a given search request.
     *
     * <p>
     * The results will be sorted by relevance score by default if no sort order is provided.
     * </p>
     * <p>
     * The returned {@link SearchMediaResultPage} object contains the original search ID,
     * the list of {@link CursorWindow} containing search results, and an extras Bundle that
     * may contain a {@link SearchMediaService#EXTRA_NEXT_PAGE_TOKEN}.
     * </p>
     *
     * <p>
     * Each row in each {@link CursorWindow} represents a single media result.
     * To access the data, iterate through the windows and rows and read the
     * data using the column indices defined in {@link SearchMediaResultPage}:
     * <ul>
     * <li>{@link SearchMediaResult#INDEX_COLUMN_ID}: (long) The media ID.
     * <li>{@link SearchMediaResult#INDEX_COLUMN_DATE_TAKEN}: (long) The date taken.
     * <li>{@link SearchMediaResult#INDEX_COLUMN_SCORE}: (double) The associated score.
     * <li>{@link SearchMediaResult#INDEX_COLUMN_MEDIA_TYPE}: (long) The media type.
     * </ul>
     * </p>
     *
     * @param searchMediaResultPage a search page of results
     */
    void onSearchResultsSuccess(@NonNull SearchMediaResultPage searchMediaResultPage);

    /**
     * Called when the search service fails to retrieve results for a given search request.
     *
     * @param searchMediaException An exception object containing the
     * error code, a human-readable message, and whether
     * the query is retryable.
     */
    void onSearchResultsFailure(@NonNull SearchMediaException searchMediaException);
}
