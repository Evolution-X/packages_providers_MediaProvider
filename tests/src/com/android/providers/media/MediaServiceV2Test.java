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

import static com.android.providers.media.scan.MediaScannerTest.stage;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.provider.MediaStore;

import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.work.WorkInfo;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
@RequiresFlagsEnabled(com.android.providers.media.flags.Flags.FLAG_ENABLE_MEDIA_SERVICE_V2)
public class MediaServiceV2Test {
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();
    private static final String WORK_INFO_STATE = "work_info_state";
    private static final String WAIT_FOR_SCAN_COMPLETION = "wait_for_scan_completion";
    private static final String VOLUME_NAME = "volume_name";
    private static final String WAIT_TIME_MILLIS = "wait_time_millis";
    private Context mContext;
    private File mDownloadsDir;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mDownloadsDir = new File(Environment.getExternalStorageDirectory(),
                Environment.DIRECTORY_DOWNLOADS);
        InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .adoptShellPermissionIdentity(Manifest.permission.WRITE_MEDIA_STORAGE);
    }

    @Test
    public void testScanVolume() throws Exception {
        File testFile = new File(mDownloadsDir,
                "testImage_" + SystemClock.elapsedRealtimeNanos() + ".jpg");
        stageNewFile(R.raw.test_image, testFile);

        try {
            Bundle extras = new Bundle();
            extras.putString(VOLUME_NAME, MediaStore.VOLUME_EXTERNAL_PRIMARY);
            extras.putBoolean(WAIT_FOR_SCAN_COMPLETION, true);
            extras.putLong(WAIT_TIME_MILLIS, 10000L);

            Bundle result = mContext.getContentResolver().call(MediaStore.AUTHORITY,
                    MediaStore.QUEUE_SCAN_VOLUME, /* arg */ null, extras);

            assertThat(result.getString(WORK_INFO_STATE))
                    .isEqualTo(WorkInfo.State.SUCCEEDED.toString());
            assertTrue(isFileScanned(testFile));
        } finally {
            testFile.delete();
        }
    }

    @Test
    public void testDuplicateScanVolumeWorkNotCreated() throws Exception {
        // Create 1000 files that we will scan. This will act as long running task and will be
        // executed by first scan volume work. The second scan volume work should not be created.
        List<File> files = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            File testFile = new File(mDownloadsDir,
                    i + "_" + SystemClock.elapsedRealtimeNanos() + ".jpg");
            stageNewFile(R.raw.test_image, testFile);
            files.add(testFile);
        }

        try {
            Bundle extras = new Bundle();
            extras.putString(VOLUME_NAME, MediaStore.VOLUME_EXTERNAL_PRIMARY);

            // We make 2 scan volume calls. The first scan volume should create work and start
            // scanning the images. The returned work state should not be null.
            Bundle resultForFirstScan = mContext.getContentResolver().call(MediaStore.AUTHORITY,
                    MediaStore.QUEUE_SCAN_VOLUME, /* arg */ null, extras);

            // The second scan volume call should not be appended as the first scan would be going
            // on. The returned work state should be null as no work is appended.
            Bundle resultForSecondScan = mContext.getContentResolver().call(MediaStore.AUTHORITY,
                    MediaStore.QUEUE_SCAN_VOLUME, /* arg */ null, extras);

            assertThat(resultForFirstScan.getString(WORK_INFO_STATE)).isNotNull();
            assertThat(resultForSecondScan.getString(WORK_INFO_STATE)).isNull();
        } finally {
            for (File file : files) {
                file.delete();
            }
        }
    }

    private boolean isFileScanned(File file) {
        Uri filesUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL);
        String selection = MediaStore.Files.FileColumns.DISPLAY_NAME + " = ?";
        String[] selectionArgs = new String[] { file.getName() };

        try (Cursor cursor = mContext.getContentResolver().query(
                filesUri,
                new String[] { MediaStore.Files.FileColumns._ID },
                selection,
                selectionArgs,
                null)) {

            return cursor != null && cursor.moveToFirst();
        }
    }

    private void stageNewFile(int resId, File file) throws IOException {
        file.createNewFile();
        stage(resId, file);
    }
}
