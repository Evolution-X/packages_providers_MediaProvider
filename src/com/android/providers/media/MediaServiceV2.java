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

import static com.android.providers.media.MediaService.ACTION_SCAN_VOLUME;
import static com.android.providers.media.MediaService.EXTRA_MEDIAVOLUME;
import static com.android.providers.media.MediaService.EXTRA_SCAN_REASON;
import static com.android.providers.media.scan.MediaScanner.REASON_UNKNOWN;

import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.os.Trace;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.android.modules.utils.build.SdkLevel;
import com.android.providers.media.flags.Flags;
import com.android.providers.media.photopicker.sync.WorkManagerInitializer;
import com.android.providers.media.photopicker.util.exceptions.RequestObsoleteException;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class MediaServiceV2 extends Worker {
    private static final String KEY_INTENT_ACTION = "intent_action";
    private static final String KEY_MEDIA_VOLUME_SERIALISED = "media_volume_serialised";
    private static final String KEY_SCAN_REASON = "scan_reason";
    private static final String TAG = MediaServiceV2.class.getSimpleName();
    private final Context mContext;

    public MediaServiceV2(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        mContext = context;
    }

    /**
     * Queues a volume scan operation. To be used for version S or higher.
     *
     * @param context The application context.
     * @param volume The {@link MediaVolume} to scan.
     * @param reason The reason for the scan.
     *
     * @return {@link UUID} UUID of work request for scan volume
     */
    public static Optional<UUID> queueVolumeScan(Context context, MediaVolume volume, int reason) {
        if (!SdkLevel.isAtLeastS()) {
            Log.i(TAG, "MediaServiceV2 is to be used for sdk version S or higher!");
            return Optional.empty();
        }

        Intent intent = new Intent(ACTION_SCAN_VOLUME);
        intent.putExtra(EXTRA_MEDIAVOLUME, volume);
        intent.putExtra(EXTRA_SCAN_REASON, reason);
        return enqueueWork(context, intent);
    }

    /**
     * Enqueues work for given given intent. This is currently implemented only for
     * ACTION_SCAN_VOLUME, more actions present in {@link MediaService} will be added in future.
     */
    public static Optional<UUID> enqueueWork(Context context, Intent intent) {
        if (!Flags.enableMediaServiceV2()) {
            Log.i(TAG, "enqueueWork was called but enable_media_service_v2 flag is disabled.");
            return Optional.empty();
        }

        Log.i(TAG, "Creating work for intent " + intent.toString());
        String action = intent.getAction();

        if (action == null) {
            Log.i(TAG, "Intent does not have action. No work created");
            return Optional.empty();
        }

        WorkManager workManager = WorkManagerInitializer.getWorkManager(context);

        if (ACTION_SCAN_VOLUME.equals(action)) {
            MediaVolume mediaVolume = intent.getParcelableExtra(EXTRA_MEDIAVOLUME);
            if (!shouldAppendWorkForScanVolume(workManager, mediaVolume.getName())) {
                Log.i(TAG, "Work already exists for " + intent);
                return Optional.empty();
            }
        }

        OneTimeWorkRequest workRequest = getWorkRequest(intent);
        workManager.enqueueUniqueWork(action, ExistingWorkPolicy.APPEND_OR_REPLACE, workRequest);
        Log.i(TAG, "Work appended for " + intent);

        return Optional.of(workRequest.getId());
    }

    /**
     * If there is a existing work for ACTION_SCAN_VOLUME and tag that is not completed (i.e, is in
     * PENDING or RUNNING stated), then do not enqueue new ACTION_SCAN_VOLUME work as it will
     * create duplicate work.
     * <p>
     * If there is a existing work for ACTION_SCAN_VOLUME and different tag, then we APPEND the new
     * work. This will ensure the newly appended work will be executed after the existing work is
     * completed.
     */
    private static boolean shouldAppendWorkForScanVolume(WorkManager workManager,
            String tagOfNewWork) {
        try {
            List<WorkInfo> workInfos =
                    workManager.getWorkInfosForUniqueWork(ACTION_SCAN_VOLUME).get();
            for (WorkInfo workInfo : workInfos) {
                if (!workInfo.getState().isFinished()) {
                    Set<String> tags = workInfo.getTags();
                    for (String tag : tags) {
                        if (tag.equalsIgnoreCase(tagOfNewWork)) {
                            return false;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.i(TAG, "Unable to check state of existing work.", e);
        }
        return true;
    }

    private static OneTimeWorkRequest getWorkRequest(Intent intent) {
        OneTimeWorkRequest.Builder workRequestBuilder =
                new OneTimeWorkRequest.Builder(MediaServiceV2.class);
        Data.Builder dataBuilder = new Data.Builder();
        String action = intent.getAction();

        dataBuilder.put(KEY_INTENT_ACTION, action);

        switch (action) {
            case ACTION_SCAN_VOLUME: {
                MediaVolume mediaVolume = intent.getParcelableExtra(EXTRA_MEDIAVOLUME);
                byte[] bytes = serializeMediaVolume(mediaVolume);
                dataBuilder.putByteArray(KEY_MEDIA_VOLUME_SERIALISED, bytes);
                int scanReason = intent.getIntExtra(EXTRA_SCAN_REASON, REASON_UNKNOWN);
                dataBuilder.putInt(KEY_SCAN_REASON, scanReason);
                workRequestBuilder.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST);
                workRequestBuilder.addTag(mediaVolume.getName());
                break;
            }
            default : {
                Log.i(TAG, "Unknown intent action " + action);
                break;
            }
        }

        Data inputData = dataBuilder.build();
        workRequestBuilder.setInputData(inputData);
        return workRequestBuilder.build();
    }

    @NonNull
    @Override
    public Result doWork() {
        Data data = getInputData();
        String action = data.getString(KEY_INTENT_ACTION);

        Log.i(TAG, "Work initiated for action [ " + action + " ]");

        try {
            Trace.beginSection("MediaServiceV2.handle [ " + action +  " ]");
            checkIsWorkerStopped();
            switch (action) {
                case ACTION_SCAN_VOLUME : {
                    byte[] bytes = data.getByteArray(KEY_MEDIA_VOLUME_SERIALISED);
                    final MediaVolume volume = deserializeMediaVolume(bytes);
                    int scanReason = data.getInt(KEY_SCAN_REASON, REASON_UNKNOWN);
                    Log.i(TAG, "Starting scan volume for " + volume.getName());
                    if (volume.isPublicVolume()) {
                        MediaService.recoverPublicVolumeIfNeeded(volume,
                                mContext.getContentResolver());
                    }
                    MediaService.onScanVolume(mContext, volume, scanReason);
                    break;
                }
                default: {
                    Log.i(TAG, "Unknown intent action [ " + action + " ]");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Work failed for action [" + action + " ]", e);
            return Result.failure();
        } finally {
            Log.i(TAG, "Work ended for action [ " + action + " ]");
            Trace.endSection();
        }

        return Result.success();
    }

    @Override
    public void onStopped() {
        Data data = getInputData();
        String action = data.getString(KEY_INTENT_ACTION);

        Log.i(TAG, "[onStopped] Work stopped for action: " + action);

        try {
            switch (action) {
                case ACTION_SCAN_VOLUME : {
                    byte[] bytes = data.getByteArray(KEY_MEDIA_VOLUME_SERIALISED);
                    final MediaVolume volume = deserializeMediaVolume(bytes);
                    try (ContentProviderClient cpc = mContext.getContentResolver()
                            .acquireContentProviderClient(MediaStore.AUTHORITY)) {
                        ((MediaProvider) cpc.getLocalContentProvider())
                                .onScanVolumeWorkStopped(volume);
                    }
                    break;
                }
                default: {
                    Log.i(TAG, "[onStopped] Unknown intent action received: " + action);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "[onStopped] Exception while handling stopped work for action: "
                    + action, e);
        } finally {
            Log.i(TAG, "[onStopped] Finished handling stop for action: " + action);
            Trace.endSection();
        }
    }

    /**
     * If the system crashes and calls onStopped(), the work is rescheduled afterwards. So if the
     * work is running, we stop it.
     */
    private void checkIsWorkerStopped() throws RequestObsoleteException {
        if (isStopped()) {
            throw new RequestObsoleteException("Work is stopped. Id: " + getId());
        }
    }

    private static byte[] serializeMediaVolume(MediaVolume mediaVolume) {
        Parcel parcel = Parcel.obtain();
        try {
            mediaVolume.writeToParcel(parcel, 0);
            return parcel.marshall();
        } finally {
            parcel.recycle();
        }
    }

    private static MediaVolume deserializeMediaVolume(byte[] bytes) {
        Parcel parcel = Parcel.obtain();
        try {
            parcel.unmarshall(bytes, 0, bytes.length);
            parcel.setDataPosition(0);
            return MediaVolume.CREATOR.createFromParcel(parcel);
        } finally {
            parcel.recycle();
        }
    }
}
