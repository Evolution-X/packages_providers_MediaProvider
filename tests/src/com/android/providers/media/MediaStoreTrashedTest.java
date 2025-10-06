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
import android.content.ContentValues;
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
            mTestDir.delete();
            if (mTrashedDir != null) {
                final File downloadDir = Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File testFolderUnderTrash = new File(mTrashedDir,
                        downloadDir.getName() + File.separator + mTestDir.getName());
                FileUtils.deleteContents(testFolderUnderTrash);
                testFolderUnderTrash.delete();
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
        final Uri imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, originalName);
        values.put(MediaStore.MediaColumns.DATA, file.getAbsolutePath());
        values.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.MediaColumns.OWNER_PACKAGE_NAME, "com.android.providers.media.test");
        final Uri uri = sIsolatedResolver.insert(imageUri, values);

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
     * Verifies that when a file is trashed, its metadata is preserved.
     */
    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_TRASH_AND_RESTORE_BY_FILE_PATH_API)
    public void testTrashFile_preservesMetadata() throws Exception {
        final String originalFileName = "image.jpg";
        final File file = stage(R.raw.lg_g4_iso_800_jpg, new File(mTestDir, originalFileName));
        final Uri fileUri = insertToMediaStore(file);
        assertNotNull("Uri should not be null after insert", fileUri);
        final long originalFileId;
        final String originalOwnerPackageName;
        final int originalIsFavorite;
        try (Cursor c = sIsolatedResolver.query(fileUri, null, null, null)) {
            assertNotNull(c);
            assertEquals("Should have one entry for the new file", 1, c.getCount());
            assertTrue(c.moveToFirst());
            originalFileId = c.getLong(c.getColumnIndexOrThrow(MediaColumns._ID));
            originalOwnerPackageName = c.getString(
                    c.getColumnIndexOrThrow(MediaColumns.OWNER_PACKAGE_NAME));
            originalIsFavorite = c.getInt(c.getColumnIndexOrThrow(MediaColumns.IS_FAVORITE));
        }

        // Perform the trash action.
        String trashedFilePath;
        try {
            sIsolatedContext.setByPassTargetSdkCheckForTrash(true);
            trashedFilePath = MediaStore.trashFile(sIsolatedResolver, file.getPath());
        } finally {
            sIsolatedContext.setByPassTargetSdkCheckForTrash(false);
        }
        // Trigger a full scan to ensure that background operations (like metadata extraction)
        // are completed and the database reflects the final, persisted state of the trashed
        // item before we verify its metadata.
        MediaStore.waitForIdle(sIsolatedResolver);
        MediaStore.scanVolume(sIsolatedResolver, MediaStore.VOLUME_EXTERNAL_PRIMARY);

        // Verify the trashed file state.
        assertTrue("Trashed path should be in trash directory",
                trashedFilePath.startsWith(mTrashedDir.getPath()));
        assertFalse("Original file should not exist", file.exists());
        final File trashedFile = new File(trashedFilePath);
        assertTrue("Trashed file should exist", trashedFile.exists());
        // Verify the file is trashed and preserve the selected metadata.
        Bundle fileQueryArgs = new Bundle();
        fileQueryArgs.putString(ContentResolver.QUERY_ARG_SQL_SELECTION, MediaColumns._ID + "=?");
        fileQueryArgs.putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                new String[]{String.valueOf(originalFileId)});
        fileQueryArgs.putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE);
        try (Cursor c = sIsolatedResolver.query(MediaStore.Files.EXTERNAL_CONTENT_URI,
                null, fileQueryArgs, null)) {
            assertNotNull(c);
            assertEquals("Should find the trashed file by ID", 1, c.getCount());
            assertTrue(c.moveToFirst());
            assertEquals("IS_TRASHED should be 1 for file", 1,
                    c.getInt(c.getColumnIndexOrThrow(MediaColumns.IS_TRASHED)));
            assertEquals("Owner package name should be preserved", originalOwnerPackageName,
                    c.getString(c.getColumnIndexOrThrow(MediaColumns.OWNER_PACKAGE_NAME)));
            assertEquals("IS_FAVORITE should be preserved", originalIsFavorite,
                    c.getInt(c.getColumnIndexOrThrow(MediaColumns.IS_FAVORITE)));
        }
    }

    /**
     * Verifies that when a file is restored, its metadata is preserved.
     */
    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_TRASH_AND_RESTORE_BY_FILE_PATH_API)
    public void testRestoreFile_preservesMetadata() throws Exception {
        final String originalFileName = "image.jpg";
        final File file = stage(R.raw.lg_g4_iso_800_jpg, new File(mTestDir, originalFileName));
        final Uri fileUri = insertToMediaStore(file);
        assertNotNull("Uri should not be null after insert", fileUri);
        final long originalFileId;
        final String originalOwnerPackageName;
        final int originalIsFavorite;
        try (Cursor c = sIsolatedResolver.query(fileUri, null, null, null)) {
            assertNotNull(c);
            assertEquals("Should have one entry for the new file", 1, c.getCount());
            assertTrue(c.moveToFirst());
            originalFileId = c.getLong(c.getColumnIndexOrThrow(MediaColumns._ID));
            originalOwnerPackageName = c.getString(
                    c.getColumnIndexOrThrow(MediaColumns.OWNER_PACKAGE_NAME));
            originalIsFavorite = c.getInt(c.getColumnIndexOrThrow(MediaColumns.IS_FAVORITE));
        }

        String restoredFilePath;
        try {
            sIsolatedContext.setByPassTargetSdkCheckForTrash(true);
            String trashedFilePath = MediaStore.trashFile(sIsolatedResolver, file.getPath());
            restoredFilePath = MediaStore.restoreFileFromTrash(sIsolatedResolver,
                    trashedFilePath, null);
        } finally {
            sIsolatedContext.setByPassTargetSdkCheckForTrash(false);
        }
        // Trigger a full scan to ensure that background operations (like metadata extraction)
        // are completed and the database reflects the final, persisted state of the trashed
        // item before we verify its metadata.
        MediaStore.waitForIdle(sIsolatedResolver);
        MediaStore.scanVolume(sIsolatedResolver, MediaStore.VOLUME_EXTERNAL_PRIMARY);

        // Verify the restored state.
        assertEquals("Restored path should match original file path", file.getPath(),
                restoredFilePath);
        assertTrue("Restored file should exist", file.exists());
        // Verify the file is restored and preserve the selected metadata.
        try (Cursor c = sIsolatedResolver.query(fileUri, null, null, null)) {
            assertNotNull(c);
            assertTrue(c.moveToFirst());
            assertEquals("File ID should be unchanged after restore", originalFileId,
                    c.getLong(c.getColumnIndexOrThrow(MediaColumns._ID)));
            assertEquals("Owner package name should be preserved after restore",
                    originalOwnerPackageName,
                    c.getString(c.getColumnIndexOrThrow(MediaColumns.OWNER_PACKAGE_NAME)));
            assertEquals("IS_FAVORITE should be preserved after restore", originalIsFavorite,
                    c.getInt(c.getColumnIndexOrThrow(MediaColumns.IS_FAVORITE)));
        }
        assertTrue("Restored file should exist on disk", file.exists());
    }


    /**
     * Verifies that when a folder is trashed, its children's metadata is preserved.
     */
    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_TRASH_AND_RESTORE_BY_FILE_PATH_API)
    public void testTrashFolder_preservesMetadata() throws Exception {
        final File folder = new File(mTestDir, "TestFolder");
        assertTrue("Failed to create test folder", folder.mkdirs());
        final String originalFileName = "my_image.jpg";
        final File file = stage(R.raw.lg_g4_iso_800_jpg, new File(folder, originalFileName));
        final Uri fileUri = insertToMediaStore(file);
        assertNotNull("Uri should not be null after insert", fileUri);
        final long originalFileId;
        final String originalOwnerPackageName;
        final int originalIsFavorite;
        try (Cursor c = sIsolatedResolver.query(fileUri, null, null, null)) {
            assertNotNull(c);
            assertTrue(c.moveToFirst());
            originalFileId = c.getLong(c.getColumnIndexOrThrow(MediaColumns._ID));
            originalOwnerPackageName = c.getString(
                    c.getColumnIndexOrThrow(MediaColumns.OWNER_PACKAGE_NAME));
            originalIsFavorite = c.getInt(c.getColumnIndexOrThrow(MediaColumns.IS_FAVORITE));
        }

        String trashedFolderPath;
        try {
            sIsolatedContext.setByPassTargetSdkCheckForTrash(true);
            trashedFolderPath = MediaStore.trashFile(sIsolatedResolver, folder.getPath());
        } finally {
            sIsolatedContext.setByPassTargetSdkCheckForTrash(false);
        }
        // Trigger a full scan to ensure that background operations (like metadata extraction)
        // are completed and the database reflects the final, persisted state of the trashed
        // item before we verify its metadata.
        MediaStore.waitForIdle(sIsolatedResolver);
        MediaStore.scanVolume(sIsolatedResolver, MediaStore.VOLUME_EXTERNAL_PRIMARY);

        // Verify the trashed folder state.
        assertTrue("Trashed path should be in trash directory",
                trashedFolderPath.startsWith(mTrashedDir.getPath()));
        assertFalse("Original folder should not exist", folder.exists());
        final File trashedFolder = new File(trashedFolderPath);
        assertTrue("Trashed folder should exist", trashedFolder.exists());
        // Verify the file is trashed and preserve the selected metadata.
        Bundle fileQueryArgs = new Bundle();
        fileQueryArgs.putString(ContentResolver.QUERY_ARG_SQL_SELECTION, MediaColumns._ID + "=?");
        fileQueryArgs.putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                new String[]{String.valueOf(originalFileId)});
        fileQueryArgs.putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE);
        try (Cursor c = sIsolatedResolver.query(MediaStore.Files.EXTERNAL_CONTENT_URI,
                null, fileQueryArgs, null)) {
            assertNotNull(c);
            assertTrue(c.moveToFirst());
            assertEquals("Owner package name should be preserved", originalOwnerPackageName,
                    c.getString(c.getColumnIndexOrThrow(MediaColumns.OWNER_PACKAGE_NAME)));
            assertEquals("IS_FAVORITE should be preserved", originalIsFavorite,
                    c.getInt(c.getColumnIndexOrThrow(MediaColumns.IS_FAVORITE)));
            assertFalse("Original file should not exist on disk", file.exists());
        }
    }

    /**
     * Verifies that when a folder is restored, its metadata is preserved.
     */
    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_TRASH_AND_RESTORE_BY_FILE_PATH_API)
    public void testRestoreFolder_preservesMetadata() throws Exception {
        final File folder = new File(mTestDir, "TestFolder");
        assertTrue("Failed to create test folder", folder.mkdirs());
        final String originalFileName = "my_image.jpg";
        final File file = stage(R.raw.lg_g4_iso_800_jpg, new File(folder, originalFileName));
        final Uri fileUri = insertToMediaStore(file);
        assertNotNull("Uri should not be null after insert", fileUri);
        final long originalFileId;
        final String originalOwnerPackageName;
        final int originalIsFavorite;
        try (Cursor c = sIsolatedResolver.query(fileUri, null, null, null)) {
            assertNotNull(c);
            assertEquals("Should have one entry for the new file", 1, c.getCount());
            assertTrue(c.moveToFirst());
            originalFileId = c.getLong(c.getColumnIndexOrThrow(MediaColumns._ID));
            originalOwnerPackageName = c.getString(
                    c.getColumnIndexOrThrow(MediaColumns.OWNER_PACKAGE_NAME));
            originalIsFavorite = c.getInt(c.getColumnIndexOrThrow(MediaColumns.IS_FAVORITE));
        }

        String restoredFolderPath;
        try {
            sIsolatedContext.setByPassTargetSdkCheckForTrash(true);
            String trashedFolderPath = MediaStore.trashFile(sIsolatedResolver, folder.getPath());
            restoredFolderPath = MediaStore.restoreFileFromTrash(sIsolatedResolver,
                    trashedFolderPath, null);
        } finally {
            sIsolatedContext.setByPassTargetSdkCheckForTrash(false);
        }
        // Trigger a full scan to ensure that background operations (like metadata extraction)
        // are completed and the database reflects the final, persisted state of the trashed
        // item before we verify its metadata.
        MediaStore.waitForIdle(sIsolatedResolver);
        MediaStore.scanVolume(sIsolatedResolver, MediaStore.VOLUME_EXTERNAL_PRIMARY);

        // Verify the restored state.
        assertEquals("Restored path should match original folder path", folder.getPath(),
                restoredFolderPath);
        assertTrue("Restored folder should exist", folder.exists());
        // Verify the file is restored and preserve the selected metadata.
        try (Cursor c = sIsolatedResolver.query(fileUri, null, null, null)) {
            assertNotNull(c);
            assertTrue(c.moveToFirst());
            assertEquals("File ID should be unchanged after restore", originalFileId,
                    c.getLong(c.getColumnIndexOrThrow(MediaColumns._ID)));
            assertEquals("Owner package name should be preserved after restore",
                    originalOwnerPackageName,
                    c.getString(c.getColumnIndexOrThrow(MediaColumns.OWNER_PACKAGE_NAME)));
            assertEquals("IS_FAVORITE should be preserved after restore", originalIsFavorite,
                    c.getInt(c.getColumnIndexOrThrow(MediaColumns.IS_FAVORITE)));
        }
        assertTrue("Restored file should exist on disk", file.exists());
    }

    /**
     * Verifies that when a directory with a legacy trashed file is trashed, the legacy file is
     * moved to its corresponding location in the trash, and the now-empty directory is trashed
     * separately.
     */
    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_TRASH_AND_RESTORE_BY_FILE_PATH_API)
    public void testTrashFolderWithLegacyTrashedFile_movesFileSeparately() throws Exception {
        // Create a directory with a legacy trashed file inside.
        final File folderToTrash = new File(mTestDir, "Hello");
        assertTrue("Test folder should be created", folderToTrash.mkdirs());
        final File legacyTrashedFile = new File(folderToTrash, ".trashed-12345-image.jpg");
        final File file = stage(R.raw.lg_g4_iso_800_jpg, legacyTrashedFile);
        final Uri fileUri = insertToMediaStore(file);
        assertNotNull("Uri should not be null after insert", fileUri);

        // Trash the directory.
        String trashedFolderPath;
        try {
            sIsolatedContext.setByPassTargetSdkCheckForTrash(true);
            trashedFolderPath = MediaStore.trashFile(sIsolatedResolver, folderToTrash.getPath());
        } finally {
            sIsolatedContext.setByPassTargetSdkCheckForTrash(false);
        }
        // Trigger a full scan to ensure that background operations (like metadata extraction)
        // are completed and the database reflects the final, persisted state of the trashed
        // item before we verify its metadata.
        MediaStore.waitForIdle(sIsolatedResolver);
        MediaStore.scanVolume(sIsolatedResolver, MediaStore.VOLUME_EXTERNAL_PRIMARY);

        // Verify original items are gone.
        assertFalse("Original folder should not exist after being trashed", folderToTrash.exists());
        assertFalse("Legacy trashed file should not exist in its original location",
                legacyTrashedFile.exists());

        // Verify the folder was moved to the trash and is now empty.
        File trashedFolder = new File(trashedFolderPath);
        assertTrue("Trashed folder should exist in the trash directory", trashedFolder.exists());
        assertTrue("Trashed folder path should be in the trash storage directory",
                trashedFolderPath.startsWith(mTrashedDir.getPath()));
        assertEquals("Trashed folder should now be empty", 0, trashedFolder.list().length);

        // Verify the legacy file was moved separately, preserving its directory structure.
        final String relativePath = folderToTrash.getPath().substring(
                Environment.getExternalStorageDirectory().getPath().length());
        final File expectedLegacyFileParentInTrash = new File(mTrashedDir, relativePath);
        final File expectedLegacyFileInTrash = new File(expectedLegacyFileParentInTrash,
                legacyTrashedFile.getName());

        assertTrue("Parent directory for legacy file should be created in trash",
                expectedLegacyFileParentInTrash.exists());
        assertTrue("Legacy trashed file should exist separately in trash",
                expectedLegacyFileInTrash.exists());

        // Verify MediaStore records for both items.
        try (Cursor c = getCursorByPath(trashedFolderPath)) {
            assertNotNull(c);
            assertEquals(1, c.getCount());
            assertTrue(c.moveToFirst());
            assertEquals("Folder should be marked as trashed", 1, c.getInt(c.getColumnIndexOrThrow(
                    MediaColumns.IS_TRASHED)));
        }

        try (Cursor c = getCursorByPath(expectedLegacyFileInTrash.getPath())) {
            assertNotNull(c);
            assertEquals(1, c.getCount());
            assertTrue(c.moveToFirst());
            assertEquals("Legacy file should be marked as trashed", 1,
                    c.getInt(c.getColumnIndexOrThrow(
                            MediaColumns.IS_TRASHED)));
            assertEquals("Legacy file path should be updated", expectedLegacyFileInTrash.getPath(),
                    c.getString(c.getColumnIndexOrThrow(MediaColumns.DATA)));
        }
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
                        MediaStore.Files.EXTERNAL_CONTENT_URI,
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

    /**
     * Inserts a file into the MediaStore and returns its URI.
     *
     * @param file The file to insert.
     * @return The URI of the newly inserted item.
     */
    private Uri insertToMediaStore(File file) {
        final Uri imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, file.getName());
        values.put(MediaStore.MediaColumns.DATA, file.getAbsolutePath());
        values.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.MediaColumns.OWNER_PACKAGE_NAME, "com.android.providers.media.test");
        values.put(MediaStore.MediaColumns.IS_FAVORITE, 1);
        return sIsolatedResolver.insert(imageUri, values);
    }
}
