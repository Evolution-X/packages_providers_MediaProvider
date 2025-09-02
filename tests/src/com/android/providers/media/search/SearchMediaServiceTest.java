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

import static android.provider.SearchMediaResult.INDEX_COLUMN_DATE_TAKEN;
import static android.provider.SearchMediaResult.INDEX_COLUMN_MEDIA_TYPE;
import static android.provider.SearchMediaResult.INDEX_COLUMN_ID;
import static android.provider.SearchMediaResult.INDEX_COLUMN_SCORE;

import static com.android.providers.media.search.TestSearchMediaService.DEFAULT_ERROR_MESSAGE;
import static com.android.providers.media.search.TestSearchMediaService.DUMMY_SEARCH_RESULT_SIZE;
import static com.android.providers.media.search.TestSearchMediaService.SHOULD_THROW_ERROR;
import static com.android.providers.media.search.TestSearchMediaService.getSearchResults;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNotNull;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.CursorWindow;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.ISearchMediaService;
import android.provider.SearchMediaResult;
import android.provider.SearchMediaService;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.providers.media.flags.Flags;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
@EnableFlags(Flags.FLAG_ENABLE_MEDIA_SEARCH)
public class SearchMediaServiceTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private final CountDownLatch mServiceLatch = new CountDownLatch(1);
    private ISearchMediaService mSearchMediaService;
    private Context mContext;

    @Before
    public void setUp() throws Exception {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(SearchMediaService.SERVICE_INTERFACE);
        intent.setClassName("com.android.providers.media.tests",
                "com.android.providers.media.search.TestSearchMediaService");
        mContext.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
        mServiceLatch.await(3, TimeUnit.SECONDS);
        assumeNotNull(mSearchMediaService);
    }

    @After
    public void tearDown() throws Exception {
        mContext.unbindService(mServiceConnection);
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mSearchMediaService = ISearchMediaService.Stub.asInterface(iBinder);
            mServiceLatch.countDown();
        }
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mSearchMediaService = null;
        }
    };

    @Test
    public void testSearchMediaSuccessScenario() throws Exception {
        Bundle searchParams = new Bundle();
        searchParams.putBoolean(SHOULD_THROW_ERROR, false);
        searchParams.putLong(DUMMY_SEARCH_RESULT_SIZE, 1000L);
        TestSearchMediaCallback callback = new TestSearchMediaCallback();

        mSearchMediaService.searchMedia(/* searchText */ "abc", /* searchId */ "123", searchParams,
                callback);
        callback.await(3, TimeUnit.SECONDS);

        assertFalse(callback.isErrored());
        assertCursorWindowsNotNull(callback);
        assertCallbackHasAllSearchResults(callback, /* expectedRows */ 1000L, searchParams);
    }

    @Test
    public void testSearchMediaFailureScenario() throws Exception {
        Bundle searchParams = new Bundle();
        searchParams.putBoolean(SHOULD_THROW_ERROR, true);
        TestSearchMediaCallback callback = new TestSearchMediaCallback();

        mSearchMediaService.searchMedia(/* searchText */ "abc", /* searchId */ "123", searchParams,
                callback);
        callback.await(3, TimeUnit.SECONDS);

        assertTrue(callback.isErrored());
        assertEquals(DEFAULT_ERROR_MESSAGE, callback.getErrorMessage());
    }

    private static void assertCursorWindowsNotNull(TestSearchMediaCallback callback) {
        CursorWindow[] cursorWindows = callback.getCursorWindows();
        assertNotNull(cursorWindows);
    }

    private static void assertCallbackHasAllSearchResults(TestSearchMediaCallback callback,
            long expectedRows, Bundle searchParams) {
        CursorWindow[] cursorWindows = callback.getCursorWindows();
        int totalRows = 0;
        List<SearchMediaResult> searchResults = new ArrayList<>();

        for (CursorWindow cursorWindow : cursorWindows) {
            int numRowsInWindow = cursorWindow.getNumRows();
            for (int row = 0; row < numRowsInWindow; row++) {
                long id = cursorWindow.getLong(row, INDEX_COLUMN_ID);
                long dateTaken = cursorWindow.getLong(row, INDEX_COLUMN_DATE_TAKEN);
                double score = cursorWindow.getDouble(row, INDEX_COLUMN_SCORE);
                long mediaType = cursorWindow.getLong(row, INDEX_COLUMN_MEDIA_TYPE);
                searchResults.add(new SearchMediaResult(id, dateTaken, score, mediaType));
            }
            totalRows += numRowsInWindow;
        }

        assertEquals(expectedRows, totalRows);

        List<SearchMediaResult> expectedSearchResults = getSearchResults(searchParams);

        for (int i = 0; i < totalRows; i++) {
            assertEquals(expectedSearchResults.get(i), searchResults.get(i));
        }
    }
}
