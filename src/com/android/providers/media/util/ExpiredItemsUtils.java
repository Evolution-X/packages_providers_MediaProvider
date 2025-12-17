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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.os.CancellationSignal;
import android.provider.MediaStore;
import android.provider.MediaStore.Files.FileColumns;
import android.provider.MediaStore.MediaColumns;
import android.text.format.DateUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A utility class for handling expired items in the MediaProvider.
 */
public final class ExpiredItemsUtils {
    private static final String TAG = "ExpiredItemsUtils";

    private ExpiredItemsUtils() {
        // Utility class
    }

    /**
     * Deletes expired items from the database.
     *
     * @param context      The context.
     * @param db           The SQLite database.
     * @param signal       A signal to cancel the operation in progress.
     * @param deletionHost A host to handle the deletion of files.
     * @return The number of items that were deleted.
     */
    public static int deleteExpiredItems(@NonNull Context context, @NonNull SQLiteDatabase db,
            @NonNull CancellationSignal signal, @NonNull ExpiredDeletionHost deletionHost) {
        final long expiredOneWeek =
                ((System.currentTimeMillis() - DateUtils.WEEK_IN_MILLIS) / 1000);
        final long now = (System.currentTimeMillis() / 1000);

        String dateSelection = FileColumns.DATE_EXPIRES + " > " + expiredOneWeek + " AND "
                + FileColumns.DATE_EXPIRES + " < " + now;
        String selection = buildFileSelection(context, dateSelection);

        final List<FileRow> itemsToDelete = new ArrayList<>();
        try (Cursor c = db.query(true, MediaStore.Files.TABLE, FileRow.PROJECTIONS, selection,
                null, null, null, null, null, signal)) {
            while (c.moveToNext()) {
                itemsToDelete.add(new FileRow(c));
            }
        }

        int deleteCount = 0;
        for (FileRow item : itemsToDelete) {
            deleteCount += deletionHost.deleteFile(item.mVolumeName, item.mId);
        }

        return deleteCount;
    }

    /**
     * Extends the expiration date of expired items.
     *
     * @param context       The context.
     * @param db            The SQLite database.
     * @param signal        A signal to cancel the operation in progress.
     * @param extensionHost A host to handle the extension of expired items.
     * @return The number of items whose expiration dates were extended.
     */
    public static int extendExpiredItems(@NonNull Context context, @NonNull SQLiteDatabase db,
            @NonNull CancellationSignal signal, @NonNull ExpiredExtensionHost extensionHost) {
        final long expiredOneWeek =
                ((System.currentTimeMillis() - DateUtils.WEEK_IN_MILLIS) / 1000);
        final long now = (System.currentTimeMillis() / 1000);
        final long expiredTime = now + (FileUtils.DEFAULT_DURATION_EXTENDED / 1000);
        String dateSelection = FileColumns.DATE_EXPIRES + " <= " + expiredOneWeek;
        String selection = buildFileSelection(context, dateSelection);

        final List<FileRow> itemsToExtend = new ArrayList<>();
        try (Cursor c = db.query(true, MediaStore.Files.TABLE, FileRow.PROJECTIONS, selection,
                null, null, null, null, null, signal)) {
            while (c.moveToNext()) {
                itemsToExtend.add(new FileRow(c));
            }
        }

        int extendCount = 0;
        int index = 0;
        for (FileRow item : itemsToExtend) {
            if (extendExpiredItem(db, item.mOriginalPath, item.mId, expiredTime,
                    expiredTime + index,
                    extensionHost)) {
                extendCount++;
            }
            index++;
        }

        return extendCount;
    }

    /**
     * Builds the full SQL selection clause for querying pending or trashed files
     * on external volumes using a specific date-based filter.
     */
    private static String buildFileSelection(@NonNull Context context,
            @NonNull String dateSelection) {
        return dateSelection
                + " AND (" + MediaColumns.IS_PENDING + "=1 OR " + MediaColumns.IS_TRASHED + "=1)"
                + " AND " + MediaColumns.VOLUME_NAME + " in " + DatabaseUtils.bindList(
                MediaStore.getExternalVolumeNames(context).toArray());
    }

    /**
     * Extends the expiration date of an expired item.
     */
    private static boolean extendExpiredItem(@NonNull SQLiteDatabase db,
            @NonNull String originalPath,
            long id, long newExpiredTime, long adjustedExpiredTime,
            @NonNull ExpiredExtensionHost host) {
        String newPath = FileUtils.getAbsoluteExtendedPath(originalPath, newExpiredTime);
        if (newPath == null) {
            Log.e(TAG, "Couldn't compute path for " + originalPath + " and expired time "
                    + newExpiredTime);
            return false;
        }

        try {
            if (updateDatabaseForExpiredItem(db, newPath, id, newExpiredTime)) {
                return host.renameFileAndInvalidateCache(originalPath, newPath);
            }
            return false;
        } catch (SQLiteConstraintException e) {
            final String errorMessage =
                    "Update database _data from " + originalPath + " to " + newPath + " failed.";
            Log.d(TAG, errorMessage, e);
        }

        newPath = FileUtils.getAbsoluteExtendedPath(originalPath, adjustedExpiredTime);
        Log.i(TAG, "Retrying to extend expired item with the new path = " + newPath);
        try {
            if (updateDatabaseForExpiredItem(db, newPath, id, adjustedExpiredTime)) {
                return host.renameFileAndInvalidateCache(originalPath, newPath);
            }
        } catch (SQLiteConstraintException e) {
            final String errorMessage =
                    "Update database _data from " + originalPath + " to " + newPath + " failed.";
            Log.d(TAG, errorMessage, e);
        }

        return false;
    }

    /**
     * Updates the database with the new path and expiration time for an item.
     */
    private static boolean updateDatabaseForExpiredItem(@NonNull SQLiteDatabase db,
            @NonNull String path, long id, long expiredTime) {
        final String table = MediaStore.Files.TABLE;
        final String whereClause = MediaColumns._ID + "=?";
        final String[] whereArgs = new String[]{String.valueOf(id)};
        final ContentValues values = new ContentValues();
        values.put(FileColumns.DATA, path);
        values.put(FileColumns.DATE_EXPIRES, expiredTime);
        final int count = db.update(table, values, whereClause, whereArgs);
        return count == 1;
    }

    /**
     * Defines the contract for the host component that handles file deletions.
     */
    public interface ExpiredDeletionHost {
        /**
         * Permanently deletes the file associated with the given ID.
         *
         * @param volumeName The name of the volume where the file resides.
         * @param id         The ID of the file to delete.
         * @return The number of files deleted, which is 1 on success and 0 on failure.
         */
        int deleteFile(String volumeName, long id);
    }

    /**
     * Defines the contract for the host component that handles file extensions.
     */
    public interface ExpiredExtensionHost {
        /**
         * Renames a file from an original path to a new path and invalidates any related caches.
         *
         * @param originalPath The current path of the file to rename.
         * @param newPath      The destination path for the file.
         * @return {@code true} if the rename was successful, {@code false} otherwise.
         */
        boolean renameFileAndInvalidateCache(String originalPath, String newPath);
    }


    static final class FileRow {
        public static final String[] PROJECTIONS =
                new String[]{FileColumns._ID, FileColumns.VOLUME_NAME,
                        FileColumns.DATE_EXPIRES, FileColumns.DATA};
        final long mId;
        final String mVolumeName;
        final long mDateExpires;
        final String mOriginalPath;
        final boolean mIsDirectory;

        FileRow(Cursor c) {
            this.mId = c.getLong(c.getColumnIndexOrThrow(FileColumns._ID));
            this.mVolumeName = c.getString(c.getColumnIndexOrThrow(FileColumns.VOLUME_NAME));
            this.mDateExpires = c.getLong(c.getColumnIndexOrThrow(FileColumns.DATE_EXPIRES));
            this.mOriginalPath = c.getString(c.getColumnIndexOrThrow(FileColumns.DATA));
            this.mIsDirectory = new File(mOriginalPath).isDirectory();
        }
    }
}
