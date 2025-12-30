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

package com.android.providers.media.localsearch;

import static com.android.providers.media.MediaProvider.MEDIAPROVIDER_PREFS;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.provider.MediaStore;
import android.provider.media.internal.flags.Flags;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.android.providers.media.R;
import com.android.providers.media.WorkManagerInitializer;

import java.util.concurrent.TimeUnit;

public class MediaProcessingWorkScheduler extends Worker {
    public static final String TAG = "MediaProcessingWorker";
    static final String PERIODIC_WORK_NAME = "MediaProcessingJob";
    private static final String DEFAULT_SEARCH_MEDIA_SERVICE_PACKAGE =
            "com.google.android.providers.media.module";

    private static final String LAST_GEN_MODIFIED_WITH_MEDIA_LABEL =
            "last_gen_modified_with_media_label";
    private static final String LAST_GEN_MODIFIED_WITH_LOCATION_LABEL =
            "last_gen_modified_with_location";
    private static final String LAST_GEN_MODIFIED_WITH_METADATA_LABEL =
            "last_gen_modified_with_metadata";
    private static final int DEFAULT_WORK_INTERVAL_HOURS = 6;


    private final Context mContext;

    public MediaProcessingWorkScheduler(@NonNull Context context,
            @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        mContext = context;
    }

    private int getWorkIntervalHoursConfig() {
        try {
            return mContext.getResources().getInteger(
                    R.integer.config_default_media_processing_job_interval_hours);
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Overlayable config for media processing job interval not found. Using "
                    + "default value " + DEFAULT_WORK_INTERVAL_HOURS, e);
            return DEFAULT_WORK_INTERVAL_HOURS;
        }
    }

    private PeriodicWorkRequest createPeriodicWorkRequest() {
        return new PeriodicWorkRequest.Builder(MediaProcessingWorkScheduler.class,
                getWorkIntervalHoursConfig(), TimeUnit.HOURS)
                .setConstraints(new Constraints.Builder()
                        .setRequiresCharging(true)
                        .setRequiresBatteryNotLow(true)
                        .setRequiresDeviceIdle(true)
                        .build())
                .build();
    }

    private boolean isMediaProcessingRequired() {
        if (!Flags.enableMediaProcessing()) {
            Log.v(TAG, "Media processing feature flag is disabled. Skip media processing.");
            return false;
        }

        // Skip scheduling work if a custom search media service is defined
        String searchMediaServicePackage = MediaStore.getPackageForSearchMediaService(
                mContext.getContentResolver());
        if (!DEFAULT_SEARCH_MEDIA_SERVICE_PACKAGE.equalsIgnoreCase(searchMediaServicePackage)) {
            Log.i(TAG, "Custom SearchMediaService defined. Skip media processing.");
            return false;
        }

        boolean disableMediaProcessing = mContext.getResources().getBoolean(
                R.bool.config_disable_media_processing_for_search);
        if (disableMediaProcessing) {
            Log.i(TAG, "Media processing disabled via overlayable configuration. Skip media "
                    + "processing");
            return false;
        }

        return true;
    }

    /**
     * Enqueue unique periodic work to schedule media processing tasks for local search.
     */
    public void enqueueWork() {
        if (isMediaProcessingRequired()) {
            // Use ExistingPeriodicWorkPolicy.REPLACE to accommodate changes in the work interval
            // overlayable config.
            WorkManagerInitializer.getWorkManager(mContext).enqueueUniquePeriodicWork(
                    PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.REPLACE,
                    createPeriodicWorkRequest());
        }
    }

    private long getRowTrackerFromSharedPreferences(String key) {
        SharedPreferences prefs = mContext.getSharedPreferences(MEDIAPROVIDER_PREFS,
                Context.MODE_PRIVATE);
        return prefs.getLong(key, 0);
    }

    private void updateRowTrackerInSharedPreferences(String key, long lastUpdatedRow) {
        SharedPreferences preferences = mContext.getSharedPreferences(MEDIAPROVIDER_PREFS,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(key, lastUpdatedRow);
        editor.apply();
    }

    @NonNull
    @Override
    public Result doWork() {
        long lastGenModWithMediaLabel = getRowTrackerFromSharedPreferences(
                LAST_GEN_MODIFIED_WITH_MEDIA_LABEL);
        long lastGenModWithLocation = getRowTrackerFromSharedPreferences(
                LAST_GEN_MODIFIED_WITH_LOCATION_LABEL);
        long lastGenModWithMetadata = getRowTrackerFromSharedPreferences(
                LAST_GEN_MODIFIED_WITH_METADATA_LABEL);

        //TODO : Add support for processing different labels in this periodic job.

        return Result.success();
    }
}
