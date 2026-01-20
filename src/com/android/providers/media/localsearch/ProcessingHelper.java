/*
 * Copyright (C) 2026 The Android Open Source Project
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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.os.IBinder;
import android.provider.mediaprocessingservice.IMediaProcessingService;
import android.provider.mediaprocessingservice.MediaProcessingService;
import android.provider.mediaprocessingservice.MediaProcessingServiceWrapper;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.providers.media.R;
import com.android.providers.media.flags.Flags;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ProcessingHelper {
    private static final String TAG = "MediaProcessingHelper";

    private final Context mContext;
    private MediaProcessingServiceWrapper mMediaProcessingService;
    private static final int SERVICE_CONNECTION_TIMEOUT_SECONDS = 5;
    private static final int GET_PROCESSING_REQUESTED_TIMEOUT = 1;
    private static final int GET_PROCESSING_LIMIT_TIMEOUT = 1;

    /**
     * Default MediaProcessingService implementation package.
     */
    private final Optional<String> mMediaProcessingServicePackage;

    /**
     * Count down latch to process delay in connection to MediaProcessingService.
     */
    private CountDownLatch mCountDownLatchForProcessingServiceConnection = new CountDownLatch(1);

    public ProcessingHelper(Context context) {
        this.mContext = context;
        mMediaProcessingServicePackage = getMediaProcessingServicePackage(context);
    }

    private Optional<String> getMediaProcessingServicePackage(Context context) {
        try {
            String packageName = context.getResources().getString(
                    R.string.config_default_media_processing_service_package);
            if (packageName == null || packageName.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(packageName);
        } catch (Resources.NotFoundException e) {
            return Optional.empty();
        }
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder iBinder) {
            Log.i(TAG, "MediaProcessingService connected: " + name);
            IMediaProcessingService service = IMediaProcessingService.Stub.asInterface(iBinder);
            mMediaProcessingService = new MediaProcessingServiceWrapper(service);
            mCountDownLatchForProcessingServiceConnection.countDown();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.w(TAG, "MediaProcessingService disconnected: " + name);
            if (mMediaProcessingService != null) {
                mMediaProcessingService.shutdown();
            }
            mMediaProcessingService = null;
            mCountDownLatchForProcessingServiceConnection = new CountDownLatch(1);
        }

        @Override
        public void onBindingDied(ComponentName name) {
            Log.e(TAG, "MediaProcessingService binding died: " + name);
            mContext.unbindService(this);
            if (mMediaProcessingService != null) {
                mMediaProcessingService.shutdown();
            }
            mMediaProcessingService = null;
            mCountDownLatchForProcessingServiceConnection = new CountDownLatch(1);
        }
    };

    private synchronized void connectMediaProcessingService() {
        try {
            if (!Flags.enableMediaProcessingService()) {
                return;
            }

            if (mMediaProcessingServicePackage.isEmpty()) {
                Log.v(TAG, "No implementing package listed for MediaProcessingService");
                return;
            }

            if (mMediaProcessingService != null) {
                Log.i(TAG, "MediaProcessingService already connected");
                return;
            }

            Intent intent = new Intent(MediaProcessingService.SERVICE_INTERFACE);
            ResolveInfo resolveInfo = mContext.getPackageManager().resolveService(intent,
                    PackageManager.MATCH_ALL);
            if (resolveInfo == null || resolveInfo.serviceInfo == null
                    || resolveInfo.serviceInfo.packageName == null
                    || !mMediaProcessingServicePackage.get()
                    .equalsIgnoreCase(resolveInfo.serviceInfo.packageName)
                    || resolveInfo.serviceInfo.permission == null
                    || !resolveInfo.serviceInfo.permission.equalsIgnoreCase(
                    MediaProcessingService.BIND_MEDIA_PROCESSING_SERVICE_PERMISSION)) {
                Log.v(TAG, "No valid package found for MediaProcessingService");
                return;
            }

            ServiceInfo serviceInfo = resolveInfo.serviceInfo;
            intent.setComponent(new ComponentName(serviceInfo.packageName, serviceInfo.name));
            mContext.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
            mCountDownLatchForProcessingServiceConnection.await(
                    SERVICE_CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            Log.e(TAG, "Exception in connecting MediaProcessingService", e);
        }
    }

    @VisibleForTesting
    public MediaProcessingServiceWrapper getMediaProcessingService() {
        return mMediaProcessingService;
    }

    /**
     * Gets the map of processing types requested per media type from MediaProcessingService.
     */
    public Map<Integer, Integer> getProcessingRequestedPerMediaType() {
        try {
            if (mMediaProcessingServicePackage.isEmpty()) {
                return DefaultMediaLabelResolver.getProcessingRequestedPerMediaType();
            }

            if (mMediaProcessingService == null) {
                connectMediaProcessingService();
            }

            // Return empty if we are unable to connect to media processing service
            if (mMediaProcessingService == null) {
                return new HashMap<>();
            }

            return mMediaProcessingService.getProcessingRequestedPerMediaType(
                    /*serviceTimeoutInSeconds */ GET_PROCESSING_REQUESTED_TIMEOUT);
        } catch (Exception e) {
            Log.e(TAG, "Error in fetching requested processing from MediaProcessingService", e);
            return new HashMap<>();
        }
    }

    /**
     * Gets the map of processing types requested per media type from MediaProcessingService.
     */
    public int getProcessingLimitForMediaLabels() {
        try {
            if (mMediaProcessingServicePackage.isEmpty()) {
                return DefaultMediaLabelResolver.getProcessingLimit(mContext);
            }

            if (mMediaProcessingService == null) {
                connectMediaProcessingService();
            }

            // Return 0 if we are unable to connect to media processing service
            if (mMediaProcessingService == null) {
                return 0;
            }

            return mMediaProcessingService.getProcessingLimit(
                    /*serviceTimeoutInSeconds */ GET_PROCESSING_LIMIT_TIMEOUT);
        } catch (Exception e) {
            Log.e(TAG, "Error in fetching requested processing from MediaProcessingService", e);
            return 0;
        }
    }
}
