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
package com.android.providers.media.search;

import android.database.CursorWindow;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.ISearchMediaCallback;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TestSearchMediaCallback extends ISearchMediaCallback.Stub {

    private boolean mErrored = false;
    private String mErrorMessage = null;
    private String mSearchId = null;
    private CursorWindow[] mCursorWindows = null;
    private final CountDownLatch mLatch = new CountDownLatch(1);

    @Override
    public void onSearchResultsSuccess(String searchId, CursorWindow[] results, Bundle extras)
            throws RemoteException {
        mSearchId = searchId;
        mCursorWindows = results;
        mLatch.countDown();
    }

    @Override
    public void onSearchResultsFailure(String searchId, int errorCode, String errorMessage,
            boolean retryable) throws RemoteException {
        mErrored = true;
        mSearchId = searchId;
        mErrorMessage = errorMessage;
        mLatch.countDown();
    }

    /**
     * Wait for search to complete, maximum for given time
     */
    public void await(int time, TimeUnit unit) throws InterruptedException {
        mLatch.await(time, unit);
    }

    public boolean isErrored() {
        return mErrored;
    }

    public String getSearchId() {
        return mSearchId;
    }

    public CursorWindow[] getCursorWindows() {
        return mCursorWindows;
    }

    public String getErrorMessage() {
        return mErrorMessage;
    }
}
