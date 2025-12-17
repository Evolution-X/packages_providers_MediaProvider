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

package com.android.providers.media.util;

import static com.android.providers.media.scan.MediaScannerTest.stage;

import static com.google.common.truth.Truth.assertThat;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.format.DateUtils;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import com.android.providers.media.IsolatedContext;
import com.android.providers.media.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

@RunWith(AndroidJUnit4.class)
public class ExpiredItemsUtilsTest {
    private File mDir;
    private ContentResolver mIsolatedResolver;

    @Before
    public void setUp() throws Exception {
        InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .adoptShellPermissionIdentity(Manifest.permission.LOG_COMPAT_CHANGE,
                        Manifest.permission.READ_COMPAT_CHANGE_CONFIG,
                        // Deleting entries invokes Download#onMediaStoreDownloadsDeleted() which
                        // requires this permission.
                        Manifest.permission.WRITE_MEDIA_STORAGE,
                        // Adding this to use getUserHandles() api of UserManagerService which
                        // requires either MANAGE_USERS or CREATE_USERS. Since shell does not have
                        // MANAGER_USERS permissions, using CREATE_USERS in test. This works with
                        // MANAGE_USERS permission for MediaProvider module.
                        Manifest.permission.CREATE_USERS,
                        Manifest.permission.DUMP);

        resetIsolatedContext();
        File downloadsDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS);
        mDir = new File(downloadsDir, "test_" + System.nanoTime());
        mDir.mkdirs();
        FileUtils.deleteContents(mDir);
        // Previous tests may have left stale files, do an idle run first to clean them up.
        MediaStore.runIdleMaintenance(mIsolatedResolver);
        MediaStore.waitForIdle(mIsolatedResolver);
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.deleteContents(mDir);
        InstrumentationRegistry.getInstrumentation()
                .getUiAutomation().dropShellPermissionIdentity();
    }

    /**
     * Verifies that a trashed item expired for 2 days is deleted after idle maintenance.
     */
    @Test
    public void testDeleteExpiredTrashedItem() throws IOException {
        final long expiredTwoDaysAgo =
                (System.currentTimeMillis() - (2 * DateUtils.DAY_IN_MILLIS)) / 1000;
        final Uri uri = createExpiredItem(FileUtils.PREFIX_TRASHED, expiredTwoDaysAgo, "item1");

        MediaStore.runIdleMaintenance(mIsolatedResolver);
        MediaStore.waitForIdle(mIsolatedResolver);

        try (Cursor cursor = mIsolatedResolver.query(uri, null, null, null)) {
            assertThat(cursor.getCount()).isEqualTo(0);
        }
    }

    /**
     * Verifies that a pending item expired for 2 days is also deleted after idle maintenance.
     */
    @Test
    public void testDeleteExpiredPendingItem() throws IOException {
        final long expiredTwoDaysAgo =
                (System.currentTimeMillis() - (2 * DateUtils.DAY_IN_MILLIS)) / 1000;
        final Uri uri = createExpiredItem(FileUtils.PREFIX_PENDING, expiredTwoDaysAgo, "item1");

        MediaStore.runIdleMaintenance(mIsolatedResolver);
        MediaStore.waitForIdle(mIsolatedResolver);

        try (Cursor cursor = mIsolatedResolver.query(uri, null, null, null)) {
            assertThat(cursor.getCount()).isEqualTo(0);
        }
    }

    /**
     * Confirms that a trashed item expired for 8 days has its expiration date extended, rather than
     * being deleted.
     */
    @Test
    public void testExtendExpiredTrashedItem() throws IOException {
        final long expiredEightDaysAgo =
                (System.currentTimeMillis() - (8 * DateUtils.DAY_IN_MILLIS)) / 1000;
        final Uri uri = createExpiredItem(FileUtils.PREFIX_TRASHED, expiredEightDaysAgo, "item2");

        MediaStore.runIdleMaintenance(mIsolatedResolver);
        MediaStore.waitForIdle(mIsolatedResolver);

        final Bundle queryArgs = new Bundle();
        queryArgs.putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE);
        try (Cursor cursor = mIsolatedResolver.query(uri,
                new String[]{MediaStore.MediaColumns.DATE_EXPIRES},
                queryArgs, null)) {
            assertThat(cursor.moveToFirst()).isTrue();
            long newDateExpires = cursor.getLong(0);
            assertThat(newDateExpires).isGreaterThan(expiredEightDaysAgo);
        }
    }

    /**
     * Confirms that a pending item expired for 8 days also has its expiration date extended.
     */
    @Test
    public void testExtendExpiredPendingItem() throws IOException {
        final long expiredEightDaysAgo =
                (System.currentTimeMillis() - (8 * DateUtils.DAY_IN_MILLIS)) / 1000;
        final Uri uri = createExpiredItem(FileUtils.PREFIX_PENDING, expiredEightDaysAgo, "item2");

        MediaStore.runIdleMaintenance(mIsolatedResolver);
        MediaStore.waitForIdle(mIsolatedResolver);

        final Bundle queryArgs = new Bundle();
        queryArgs.putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE);
        try (Cursor cursor = mIsolatedResolver.query(uri,
                new String[]{MediaStore.MediaColumns.DATE_EXPIRES},
                queryArgs, null)) {
            assertThat(cursor.moveToFirst()).isTrue();
            long newDateExpires = cursor.getLong(0);
            assertThat(newDateExpires).isGreaterThan(expiredEightDaysAgo);
        }
    }

    /**
     * Ensures that the cleanup process does not alter a trashed item that has not yet expired.
     */
    @Test
    public void testNonExpiredTrashedItem_isNotTouched() throws IOException {
        final long notExpired = (System.currentTimeMillis() + DateUtils.DAY_IN_MILLIS) / 1000;
        final Uri uri = createExpiredItem(FileUtils.PREFIX_TRASHED, notExpired, "item3");

        MediaStore.runIdleMaintenance(mIsolatedResolver);
        MediaStore.waitForIdle(mIsolatedResolver);

        final Bundle queryArgs = new Bundle();
        queryArgs.putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE);
        try (Cursor cursor = mIsolatedResolver.query(uri,
                new String[]{MediaStore.MediaColumns.DATE_EXPIRES},
                queryArgs, null)) {
            assertThat(cursor.moveToFirst()).isTrue();
            long dateExpires = cursor.getLong(0);
            assertThat(dateExpires).isEqualTo(notExpired);
        }
    }

    /**
     * Ensures that a pending item that has not yet expired is also correctly ignored by the cleanup
     * process.
     */
    @Test
    public void testNonExpiredPendingItem_isNotTouched() throws IOException {
        final long notExpired = (System.currentTimeMillis() + DateUtils.DAY_IN_MILLIS) / 1000;
        final Uri uri = createExpiredItem(FileUtils.PREFIX_PENDING, notExpired, "item3");

        MediaStore.runIdleMaintenance(mIsolatedResolver);
        MediaStore.waitForIdle(mIsolatedResolver);

        final Bundle queryArgs = new Bundle();
        queryArgs.putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE);
        try (Cursor cursor = mIsolatedResolver.query(uri,
                new String[]{MediaStore.MediaColumns.DATE_EXPIRES},
                queryArgs, null)) {
            assertThat(cursor.moveToFirst()).isTrue();
            long dateExpires = cursor.getLong(0);
            assertThat(dateExpires).isEqualTo(notExpired);
        }
    }

    private void resetIsolatedContext() {
        if (mIsolatedResolver != null) {
            // This is necessary, we wait for all unfinished tasks to finish before we create a
            // new IsolatedContext.
            MediaStore.waitForIdle(mIsolatedResolver);
        }

        Context context = InstrumentationRegistry.getTargetContext();
        IsolatedContext isolatedContext = new IsolatedContext(context, "modern", /*asFuseThread*/
                false);
        mIsolatedResolver = isolatedContext.getContentResolver();
    }

    private Uri createExpiredItem(String prefix, long dateExpires, String displayName)
            throws IOException {
        final String fileName = String.format(Locale.US, ".%s-%d-%s.jpg", prefix, dateExpires,
                displayName);
        final File file = stage(R.raw.test_image, new File(mDir, fileName));
        final Uri uri = MediaStore.scanFile(mIsolatedResolver, file);
        MediaStore.waitForIdle(mIsolatedResolver);

        final String[] projection = new String[]{MediaStore.MediaColumns.DATE_EXPIRES};
        final Bundle queryArgs = new Bundle();
        if (prefix.equals(FileUtils.PREFIX_TRASHED)) {
            queryArgs.putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE);
        } else if (prefix.equals(FileUtils.PREFIX_PENDING)) {
            queryArgs.putInt(MediaStore.QUERY_ARG_MATCH_PENDING, MediaStore.MATCH_INCLUDE);
        }

        try (Cursor cursor = mIsolatedResolver.query(uri, projection, queryArgs, null)) {
            assertThat(cursor.getCount()).isEqualTo(1);
        }
        return uri;
    }
}
