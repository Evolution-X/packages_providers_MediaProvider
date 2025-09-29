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
import static com.android.providers.media.util.FileUtils.DIRECTORY_TRASH_STORAGE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import com.android.providers.media.flags.Flags;
import com.android.providers.media.util.FileUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

@RunWith(AndroidJUnit4.class)
public class MediaStoreTrashedTest {

    private static final String TAG = "MediaStoreTrashedTest";

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    private File mTestDir;
    private File mTrashedDir;

    private static IsolatedContext sIsolatedContext;
    private static ContentResolver sIsolatedResolver;

    @Before
    public void setUp() {
        InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .adoptShellPermissionIdentity(Manifest.permission.LOG_COMPAT_CHANGE,
                        Manifest.permission.READ_COMPAT_CHANGE_CONFIG,
                        Manifest.permission.MANAGE_EXTERNAL_STORAGE);
        resetIsolatedContext();

        final File downloadDir = Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        mTestDir = new File(downloadDir, "test-" + System.nanoTime());
        mTestDir.mkdirs();

        mTrashedDir = new File(Environment.getExternalStorageDirectory(), DIRECTORY_TRASH_STORAGE);

        MediaStore.scanFile(sIsolatedResolver, mTestDir);
    }

    @After
    public void tearDown() {
        if (mTestDir != null) {
            FileUtils.deleteContents(mTestDir);
            if (mTrashedDir != null) {
                File testFolderUnderTrash = new File(mTrashedDir, mTestDir.getName());
                FileUtils.deleteContents(testFolderUnderTrash);
            }
        }
        InstrumentationRegistry.getInstrumentation()
                .getUiAutomation().dropShellPermissionIdentity();
    }

    /**
     * Verifies that when a file is trashed, its file path ({@link MediaColumns#DATA})
     * points to a location within the volume's trash directory, and when restored,
     * it points back to its original file path.
     */
    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_TRASH_AND_RESTORE_BY_FILE_PATH_API)
    public void testTrashFile_movesToTrashStorageDirectory() throws Exception {
        final String originalName = "image.jpg";
        File file = stage(R.raw.lg_g4_iso_800_jpg, new File(mTestDir, originalName));
        final Uri uri = MediaStore.scanFile(sIsolatedResolver, file);

        try (Cursor c = sIsolatedResolver.query(uri, null, null, null)) {
            assertNotNull(c);
            assertEquals(1, c.getCount());
            assertTrue(c.moveToFirst());
            final String data = c.getString(c.getColumnIndexOrThrow(MediaColumns.DATA));
            final String result = FileUtils.extractDisplayName(data);
            assertEquals(originalName, result);
        }

        // Trash the file
        String trashedPath;
        try {
            sIsolatedContext.setByPassTargetSdkCheckForTrash(true);
            trashedPath = MediaStore.trashFile(sIsolatedResolver, file.getPath());
        } finally {
            sIsolatedContext.setByPassTargetSdkCheckForTrash(false);
        }

        // Verify that the file path has been updated to the trash directory
        try (Cursor c = getCursorByPath(trashedPath)) {
            assertNotNull(c);
            assertEquals(1, c.getCount());
            assertTrue(c.moveToFirst());
            assertEquals(1, c.getInt(c.getColumnIndexOrThrow(MediaColumns.IS_TRASHED)));
            assertEquals(trashedPath, c.getString(c.getColumnIndexOrThrow(MediaColumns.DATA)));
            assertTrue(trashedPath.startsWith(mTrashedDir.getPath()));
        }

        // Verify the file system reflects the move
        File trashedFile = new File(trashedPath);
        assertTrue("Trashed file should exist on disk", trashedFile.exists());
        assertFalse("Original file should not exist on disk", file.exists());

        // Restore the file to its original location
        String restoredPath;
        try {
            sIsolatedContext.setByPassTargetSdkCheckForTrash(true);
            restoredPath = MediaStore.restoreFileFromTrash(sIsolatedResolver, trashedPath, null);
        } finally {
            sIsolatedContext.setByPassTargetSdkCheckForTrash(false);
        }

        // Verify the file is restored to its original path
        assertEquals("Restored path should match original path", file.getPath(), restoredPath);
        try (Cursor c = getCursorByPath(restoredPath)) {
            assertNotNull(c);
            assertEquals(1, c.getCount());
            assertTrue(c.moveToFirst());
            assertEquals(0, c.getInt(c.getColumnIndexOrThrow(MediaColumns.IS_TRASHED)));
            assertEquals(restoredPath, c.getString(c.getColumnIndexOrThrow(MediaColumns.DATA)));
        }

        // Verify the file system reflects the restore
        assertTrue("Restored file should exist at original path", file.exists());
        assertFalse("Trashed file should no longer exist", trashedFile.exists());
    }

    /**
     * Verifies that when a trashed item is restored, its parent directory within the trash
     * is also deleted if it becomes empty.
     */
    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_TRASH_AND_RESTORE_BY_FILE_PATH_API)
    public void testRestoreTrashedFile_deletesEmptyParentInTrash() throws Exception {
        // Setup a file in a unique subdirectory to ensure it's the only one
        final File subDir = new File(mTestDir, "testDir");
        subDir.mkdirs();
        final String originalName = "image_to_restore.jpg";
        File file = stage(R.raw.lg_g4_iso_800_jpg, new File(subDir, originalName));
        MediaStore.scanFile(sIsolatedResolver, file);

        // Trash the file
        String trashedPath;
        try {
            sIsolatedContext.setByPassTargetSdkCheckForTrash(true);
            trashedPath = MediaStore.trashFile(sIsolatedResolver, file.getPath());
        } finally {
            sIsolatedContext.setByPassTargetSdkCheckForTrash(false);
        }

        // Verify its parent directory exists in the trash storage
        File trashedFile = new File(trashedPath);
        File trashedParentDir = trashedFile.getParentFile();
        assertTrue("Parent directory in trash should exist after trashing",
                trashedParentDir.exists());

        // Restore the file (it's the last one in its trashed parent dir)
        try {
            sIsolatedContext.setByPassTargetSdkCheckForTrash(true);
            MediaStore.restoreFileFromTrash(sIsolatedResolver, trashedPath, null);
        } finally {
            sIsolatedContext.setByPassTargetSdkCheckForTrash(false);
        }

        // Verify the parent directory in trash has been cleaned up
        assertFalse("Parent directory in trash should be deleted after restoring last item",
                trashedParentDir.exists());
        assertTrue("Restored file should exist at original location", file.exists());
    }

    /**
     * Verifies that after a trashed item in a subdirectory is permanently deleted, its parent
     * directories that become empty are also removed.
     */
    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_TRASH_AND_RESTORE_BY_FILE_PATH_API)
    public void testDeleteTrashedFile_deletesEmptyParentInTrash() throws Exception {
        // Setup a file in a unique subdirectory to ensure it's the only one
        final File subDir = new File(mTestDir, "deleteTestDir");
        subDir.mkdirs();
        final String originalName = "image_to_delete.jpg";
        File file = stage(R.raw.lg_g4_iso_800_jpg, new File(subDir, originalName));
        MediaStore.scanFile(sIsolatedResolver, file);

        // Trash the file
        String trashedPath;
        try {
            sIsolatedContext.setByPassTargetSdkCheckForTrash(true);
            trashedPath = MediaStore.trashFile(sIsolatedResolver, file.getPath());
        } finally {
            sIsolatedContext.setByPassTargetSdkCheckForTrash(false);
        }

        // Get the trashed item for deletion and verify parent exists
        File trashedFile = new File(trashedPath);
        File trashedParentDir = trashedFile.getParentFile();
        assertTrue("Parent directory in trash should exist after trashing",
                trashedParentDir.exists());

        trashedFile.delete();
        MediaStore.scanFile(sIsolatedResolver, trashedFile);

        // Wait for the background jobs to be completed
        MediaStore.waitForIdle(sIsolatedResolver);

        // Verify the parent directory in trash has been cleaned up
        assertFalse("Parent directory in trash should be deleted after deleting the trashed item",
                trashedParentDir.exists());
        assertFalse("Trashed file should not exist after deletion", trashedFile.exists());
        assertFalse("Original file should also not exist", file.exists());
    }


    /**
     * Queries for a media item by its file path, including trashed items.
     *
     * @param path The file path to query for.
     * @return A cursor containing the query results.
     */
    private Cursor getCursorByPath(String path) {
        Bundle queryArgs = new Bundle();
        String[] selectionArgs = new String[]{path};
        queryArgs.putString(
                ContentResolver.QUERY_ARG_SQL_SELECTION,
                MediaColumns.DATA + " = ?");
        queryArgs.putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs);
        queryArgs.putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE);

        return sIsolatedResolver
                .query(
                        MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
                        null,
                        queryArgs,
                        null);
    }

    private static void resetIsolatedContext() {
        if (sIsolatedResolver != null) {
            // This is necessary, we wait for all unfinished tasks to finish before we create a
            // new IsolatedContext.
            MediaStore.waitForIdle(sIsolatedResolver);
        }

        Context context = InstrumentationRegistry.getTargetContext();
        sIsolatedContext = new IsolatedContext(context, "modern", /*asFuseThread*/ false);
        sIsolatedResolver = sIsolatedContext.getContentResolver();
    }
}
