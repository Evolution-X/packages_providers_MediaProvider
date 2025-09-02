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

/**
* @hide
*/
oneway interface ISearchMediaCallback {

    /**
     * Called when media search results is available.
     * Search results will sorted by relevance score by default if no sort order is provided
     * while querying.
     *
     * @param searchId SearchId for which results are generated
     * @param results An array of CursorWindow containing searchResults.
     * Each row of CursorWindow would represent a single media items with following coloums.
     * id (index 0):         Long, id of the media item.
     * dateTaken (index 1):  Long, timestamp at which media item is created
     * score (index 2):      Double, the relevance score of the document
     * mediaType (index 3):  Long, mediaType of the file
     * @param extras A bundle containing additional information regarding search results.
     * Expected keys -
     * EXTRA_NEXT_PAGE_TOKEN: String, required for fetching next page of search results. Caller
     * should pass this string in the searchParams while querying for next page. If this key is
     * absent, it implies there are no more search results.
     */
    void onSearchResultsSuccess(in String searchId, in CursorWindow[] results, in Bundle extras);

    /**
     * Called if an error occurs during the media search operation.
     *
     * @param searchId SearchId for which results are generated
     * @param errorCode An integer representing the type of error (e.g.,
     * permission denied, invalid query).
     * @param errorMessage A human-readable string describing the error.
     * @param retryable Indicates whether the calling app should retry querying for results
     */
    void onSearchResultsFailure(in String searchId, int errorCode, String errorMessage, boolean retryable);

}
