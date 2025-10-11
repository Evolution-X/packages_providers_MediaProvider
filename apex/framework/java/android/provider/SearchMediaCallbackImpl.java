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

import android.database.CursorWindow;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * @hide
 */
public class SearchMediaCallbackImpl implements SearchMediaCallback {
    private static final String TAG = SearchMediaCallbackImpl.class.getSimpleName();
    private final ISearchMediaCallback mISearchMediaCallback;

    public SearchMediaCallbackImpl(ISearchMediaCallback iSearchMediaCallback) {
        mISearchMediaCallback = iSearchMediaCallback;
    }

    @Override
    public void onSearchResultsSuccess(@NonNull String searchId,
            @NonNull List<SearchMediaResult> searchResults, @NonNull Bundle extras) {
        CursorWindow[] cursorWindows = SearchMediaUtils.convertToCursorWindows(searchResults);
        try {
            mISearchMediaCallback.onSearchResultsSuccess(searchId, cursorWindows, extras);
        } catch (RemoteException ex) {
            Log.e(TAG, "Unable to send back search results for searchId " + searchId, ex);
        }
    }

    @Override
    public void onSearchResultsFailure(@NonNull String searchId, int errorCode,
            @NonNull String errorMessage, boolean retryable) {
        try {
            mISearchMediaCallback.onSearchResultsFailure(searchId, errorCode, errorMessage,
                    retryable);
        } catch (RemoteException ex) {
            Log.e(TAG, "Unable to send back search error for searchId " + searchId, ex);
        }
    }
}
