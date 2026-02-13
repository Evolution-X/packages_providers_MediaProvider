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

package com.android.providers.media;

import static android.provider.SearchMediaService.EXTRA_NEXT_PAGE_TOKEN;
import static android.provider.SearchMediaService.EXTRA_SEARCH_RESULTS_PAGE_SIZE;
import static android.provider.SearchMediaService.EXTRA_SEARCH_RESULTS_SORT_ORDER;
import static android.provider.SearchMediaService.EXTRA_SORT_BY_RELEVANCE;
import static android.provider.SearchMediaService.EXTRA_SORT_BY_TIME;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.provider.ISearchMediaService;
import android.provider.MediaStore;
import android.provider.SearchMediaResult;
import android.provider.SearchMediaService;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appsearch.app.EmbeddingVector;
import androidx.appsearch.app.SearchResult;
import androidx.appsearch.app.SearchResults;
import androidx.appsearch.app.SearchSpec;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.providers.media.appsearch.AppSearchDbManager;
import com.android.providers.media.appsearch.MediaItem;
import com.android.providers.media.search.TestSearchMediaCallback;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
@RequiresFlagsEnabled({com.android.providers.media.flags.Flags.FLAG_ENABLE_MEDIA_SEARCH,
        com.android.providers.media.flags.Flags.FLAG_ENABLE_MEDIA_PROCESSING})
public class DefaultSearchMediaServiceTest {
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    private static final String TAG = DefaultSearchMediaServiceTest.class.getSimpleName();

    private static final Random sRandom = new Random();
    static final int EMBEDDING_DIMENSION = 2;
    static final String MODEL_SIG = "model_1";

    private final CountDownLatch mServiceLatch = new CountDownLatch(1);

    private ISearchMediaService mSearchMediaService;
    private Context mContext;
    private boolean mIsServiceConnected = false;
    private AppSearchDbManager mAppSearchDbManager;
    private boolean mAppsearchDbConnected;

    @Before
    public void setUp() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mContext = new IsolatedContext(context, TAG, /* asFuseThread */ false);

        try {
            mAppSearchDbManager = new AppSearchDbManager(mContext);
            mAppsearchDbConnected = true;
        } catch (UnsupportedOperationException ex) {
            // Required appSearch features are not supported.
            mAppsearchDbConnected = false;
        }
        assumeTrue(mAppsearchDbConnected);

        Intent intent = new Intent(SearchMediaService.SERVICE_INTERFACE);
        intent.setClassName("com.android.providers.media.tests",
                "com.android.providers.media.TestDefaultSearchMediaService");
        mContext.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
        mServiceLatch.await(10, TimeUnit.SECONDS);
        assumeNotNull(mSearchMediaService);
        deleteAllDocuments();
    }

    @After
    public void tearDown() throws Exception {
        if (mAppsearchDbConnected) {
            deleteAllDocuments();
            mAppSearchDbManager.disconnect();
        }
        if (mIsServiceConnected) {
            mContext.unbindService(mServiceConnection);
        }
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mSearchMediaService = ISearchMediaService.Stub.asInterface(iBinder);
            mServiceLatch.countDown();
            mIsServiceConnected = true;
            Log.d(TAG, "service connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mSearchMediaService = null;
            mIsServiceConnected = false;
            Log.d(TAG, "service disconnected");
        }
    };

    @Test
    public void testSearchWhenSortedByTime() throws Exception {
        indexDocumentsForSortByTime();

        Bundle extras = new Bundle();
        extras.putLong(EXTRA_SEARCH_RESULTS_PAGE_SIZE, 20);
        extras.putString(EXTRA_SEARCH_RESULTS_SORT_ORDER, EXTRA_SORT_BY_TIME);

        List<SearchMediaResult> allSearchMediaResults = new ArrayList<>();

        allSearchMediaResults.addAll(makeSearchMediaCall(extras, /* expectedSearchResults */ 20));
        assertNotNull(extras.getString(EXTRA_NEXT_PAGE_TOKEN));

        allSearchMediaResults.addAll(makeSearchMediaCall(extras, /* expectedSearchResults */ 20));
        assertNotNull(extras.getString(EXTRA_NEXT_PAGE_TOKEN));

        allSearchMediaResults.addAll(makeSearchMediaCall(extras, /* expectedSearchResults */ 10));
        assertNull(extras.getString(EXTRA_NEXT_PAGE_TOKEN));

        for (int i = 0; i < allSearchMediaResults.size() - 1; i++) {
            long dateTaken1 = allSearchMediaResults.get(i).getDateTaken();
            long dateTaken2 = allSearchMediaResults.get(i + 1).getDateTaken();
            assertTrue("dateTaken should be in descending order.",
                    dateTaken1 > dateTaken2);
        }
    }

    @Test
    public void testSearchWhenSortedByRelevance() throws Exception {
        indexDocumentsForSortByRelevance();

        Bundle extras = new Bundle();
        extras.putLong(EXTRA_SEARCH_RESULTS_PAGE_SIZE, 20);
        extras.putString(EXTRA_SEARCH_RESULTS_SORT_ORDER, EXTRA_SORT_BY_RELEVANCE);

        List<SearchMediaResult> allSearchMediaResults = new ArrayList<>();

        allSearchMediaResults.addAll(makeSearchMediaCall(extras, /* expectedSearchResults */ 20));
        assertNotNull(extras.getString(EXTRA_NEXT_PAGE_TOKEN));

        allSearchMediaResults.addAll(makeSearchMediaCall(extras, /* expectedSearchResults */ 20));
        assertNotNull(extras.getString(EXTRA_NEXT_PAGE_TOKEN));

        allSearchMediaResults.addAll(makeSearchMediaCall(extras, /* expectedSearchResults */ 10));
        assertNull(extras.getString(EXTRA_NEXT_PAGE_TOKEN));

        for (int i = 0; i < allSearchMediaResults.size() - 1; i++) {
            double score1 = allSearchMediaResults.get(i).getScore();
            double score2 = allSearchMediaResults.get(i + 1).getScore();
            assertTrue("Scores should be in descending order.", score1 >= score2);
        }
    }

    private List<SearchMediaResult> makeSearchMediaCall(Bundle extras, int expectedSearchResults)
            throws Exception {
        TestSearchMediaCallback callback = new TestSearchMediaCallback();
        String searchId = String.valueOf(SystemClock.elapsedRealtime()); // any unique searchId
        mSearchMediaService.searchMedia("cat", searchId, extras, callback);
        callback.await(10, TimeUnit.SECONDS);

        List<SearchMediaResult> searchMediaResults =
                callback.getSearchMediaResultPage().getSearchResults();
        assertNotNull(searchMediaResults);
        assertEquals(expectedSearchResults, searchMediaResults.size());
        String pageToken = callback.getSearchMediaResultPage().getExtras().getString(
                EXTRA_NEXT_PAGE_TOKEN);
        extras.putString(EXTRA_NEXT_PAGE_TOKEN, pageToken);
        return searchMediaResults;
    }

    private void indexDocumentsForSortByTime() throws Exception {
        List<MediaItem> documents = new ArrayList<>();
        long fileId = 100;
        for (int i = 1; i <= 50; i++) {
            documents.add(createMediaItem("cat", i, fileId++));
        }

        for (int i = 51; i <= 100; i++) {
            documents.add(createMediaItem("dog", i, fileId++));
        }

        mAppSearchDbManager.insertDocuments(documents);
    }

    private void indexDocumentsForSortByRelevance() throws Exception {
        List<MediaItem> documents = new ArrayList<>();
        long fileId = 200;

        // Both semantic and full-text search should match for "cat".
        StringBuilder metadata = new StringBuilder("a photo of a happy cat");
        List<EmbeddingVector> embeddingVectorList = new ArrayList<>();
        embeddingVectorList.add(getEmbeddingForText("cat"));
        for (int i = 0; i < 20; i++) {
            documents.add(createRelevanceMediaItem(fileId++, metadata.toString(),
                    new ArrayList<>(embeddingVectorList)));
            metadata.append(" cat");
        }

        // Only full-text search should match for "cat".
        metadata = new StringBuilder("cat");
        embeddingVectorList.clear();
        embeddingVectorList.add(getEmbeddingForText("unrelated"));
        for (int i = 0; i < 20; i++) {
            documents.add(createRelevanceMediaItem(fileId++, metadata.toString(),
                    new ArrayList<>(embeddingVectorList)));
            metadata.append(" bar");
        }

        // Only semantic search should match for "cat".
        metadata = new StringBuilder("a flower picture");
        embeddingVectorList.clear();
        embeddingVectorList.add(getEmbeddingForText("cat"));
        for (int i = 0; i < 10; i++) {
            documents.add(createRelevanceMediaItem(fileId++, metadata.toString(),
                    new ArrayList<>(embeddingVectorList)));
            embeddingVectorList.add(getEmbeddingForText("foo"));
        }

        // No match for "cat".
        metadata = new StringBuilder("dog");
        embeddingVectorList.clear();
        embeddingVectorList.add(getEmbeddingForText("unrelated"));
        for (int i = 0; i < 50; i++) {
            documents.add(createRelevanceMediaItem(fileId++, metadata.toString(),
                    new ArrayList<>(embeddingVectorList)));
            metadata.append(" bar");
        }

        mAppSearchDbManager.insertDocuments(documents);
    }

    @NonNull
    private MediaItem createRelevanceMediaItem(long fileId, String metadata,
            List<EmbeddingVector> embeddingVectors) {
        MediaItem item = new MediaItem(fileId, 1, 20000 + fileId, MediaStore.VOLUME_EXTERNAL);
        item.setMetadataExtracted(metadata);
        item.setEmbeddings(embeddingVectors);
        return item;
    }

    @NonNull
    private MediaItem createMediaItem(String label, int repeat, long fileId) {
        MediaItem item = new MediaItem(fileId, 1, 10000 + fileId, MediaStore.VOLUME_EXTERNAL);
        item.setMetadataExtracted("foo ".repeat(repeat) + label);

        List<EmbeddingVector> embeddings = new ArrayList<>();
        embeddings.add(getEmbeddingForText("bar"));
        embeddings.add(getEmbeddingForText("foo"));
        item.setEmbeddings(embeddings);

        return item;
    }

    static EmbeddingVector getEmbeddingForText(String text) {
        float[] vector = new float[EMBEDDING_DIMENSION];
        if (text.contains("cat")) {
            vector[0] = 1.0f; vector[1] = 0.0f;
        } else if (text.contains("dog")) {
            vector[0] = 0.0f; vector[1] = 1.0f;
        } else if (text.contains("house")) {
            vector[0] = 0.8f; vector[1] = 0.1f;
        } else { // Unrelated
            vector[0] = -1.0f; vector[1] = -1.0f;
        }

        // Add some deviation to each vector so that we get distinct embeddings.
        float deviation = (sRandom.nextFloat() * 0.1f) - 0.05f;
        vector[0] += deviation;
        deviation = (sRandom.nextFloat() * 0.1f) - 0.05f;
        vector[1] += deviation;

        return new EmbeddingVector(vector, MODEL_SIG);
    }

    private void deleteAllDocuments() throws Exception {
        SearchSpec searchSpec = new SearchSpec.Builder()
                .addFilterNamespaces(AppSearchDbManager.NAMESPACE)
                .setResultCountPerPage(1000)
                .build();

        List<Long> fileIdsToDelete = new ArrayList<>();
        try (SearchResults searchResults = mAppSearchDbManager.searchDocuments("", searchSpec)) {
            List<SearchResult> results = retrieveAllSearchResults(searchResults);
            for (SearchResult result : results) {
                fileIdsToDelete.add(result.getGenericDocument().getPropertyLong(
                        MediaItem.PROPERTY_FILE_ID));
            }
        }

        if (!fileIdsToDelete.isEmpty()) {
            mAppSearchDbManager.deleteDocumentsByFileIds(fileIdsToDelete);
        }
    }

    private static @NonNull List<SearchResult> retrieveAllSearchResults(
            @NonNull SearchResults searchResults) throws Exception {
        List<SearchResult> page = searchResults.getNextPageAsync().get();
        List<SearchResult> results = new ArrayList<>();
        while (!page.isEmpty()) {
            results.addAll(page);
            page = searchResults.getNextPageAsync().get();
        }
        return results;
    }
}
