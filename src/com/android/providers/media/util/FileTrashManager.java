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

import static com.android.providers.media.util.FileUtils.PREFIX_TRASHED;

import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Utility class for handling file trash operation
 */
public final class FileTrashManager {

    private static final String TAG = "FileTrashManager";


    /**
     * Interface for providing media scanning capabilities.
     * This allows the trashing logic to be decoupled from the specific MediaStore implementation.
     */
    public interface MediaScannerCallback {
        /**
         * Scans a given file to add or update it in the media store db.
         *
         * @param file The file to be scanned
         */
        void scanFile(File file);
    }

    /**
     * Interface for providing file renaming capabilities.
     * This allows the file operation logic to be decoupled from the specific MediaStore
     * implementation.
     */
    public interface FileRenameCallback {
        /**
         * Renames a file from an old path to a new path.
         *
         * @param oldPath The original path of the file.
         * @param newPath The new path for the file.
         * @return 0 on success, errno on failure.
         */
        int renameFile(String oldPath, String newPath);
    }

    /**
     * Trashes a file or directory by moving it to a designated trash location
     * on external storage and updates MediaStore via the provided callback.
     *
     * @param filePath             The absolute path of the file or directory to be trashed.
     * @param fileRenameCallback A callback to rename the file.
     * @return The absolute path of the trashed item in the trash directory.
     * @throws IllegalArgumentException if the file path is null or empty.
     * @throws FileNotFoundException    if the original file to be trashed does not exist.
     * @throws IllegalStateException    if the trash directory cannot be created or
     *                                  if the file cannot be trashed (renamed).
     */
    public static String trashFile(String filePath,
            FileRenameCallback fileRenameCallback) throws IllegalArgumentException,
            FileNotFoundException, IllegalStateException {
        File originalFile = getValidatedFilePath(filePath);

        File trashBaseDirectory = getOrCreateTrashBaseDirectory(originalFile);

        final long dateExpires =
                (System.currentTimeMillis() + FileUtils.DEFAULT_DURATION_TRASHED) / 1000;

        File destinationFile = prepareDestinationFile(originalFile, trashBaseDirectory,
                dateExpires);

        boolean isRenameSuccess = fileRenameCallback.renameFile(filePath,
                destinationFile.getAbsolutePath()) == 0;
        if (!isRenameSuccess) {
            throw new IllegalStateException("Failed to trash file: "
                    + filePath + " to " + destinationFile.getAbsolutePath());
        }

        return destinationFile.getAbsolutePath();
    }

    /**
     * Recursively prefixes the names of all children (files and directories)
     * within a given directory on disk, ensuring they reflect the trashed state.
     * This method is intended to be called on a directory that has just been
     * moved to trash, and its children need consistent naming.
     *
     * @param parentDir                The directory whose children need to be prefixed.
     * @param parentTrashedDateExpires The `dateExpires` timestamp (in seconds) of the parent
     *                                 trashed directory. Children will inherit this expiration.
     * @return 0 on success, or an errno value on failure.
     */
    public static int trashChildrenOnDisk(File parentDir, long parentTrashedDateExpires) {
        File[] children = parentDir.listFiles();
        if (children == null) {
            return 0;
        }

        for (File child : children) {
            final String newChildName = String.format(
                    Locale.US, ".%s-%d-%s", PREFIX_TRASHED, parentTrashedDateExpires,
                    child.getName());

            File renamedChildFile = new File(parentDir, newChildName);
            try {
                Os.rename(child.getAbsolutePath(), renamedChildFile.getAbsolutePath());
            } catch (ErrnoException e) {
                final String errorMessage = "Rename " + child.getAbsolutePath() + " to "
                        + renamedChildFile.getAbsolutePath() + " failed.";
                Log.e(TAG, errorMessage, e);
                return e.errno;
            }
            // If the renamedChildFile item is a directory, recurse into it
            if (renamedChildFile.isDirectory()) {
                int result = trashChildrenOnDisk(renamedChildFile, parentTrashedDateExpires);
                if (result != 0) {
                    // If a recursive call fails, propagate the error up
                    return result;
                }
            }
        }
        return 0;
    }

    /**
     * Generates a trashed path by prefixing each component of the relative path.
     *
     * @param parentPath   The parent path.
     * @param relativePath The relative path from the parent.
     * @param dateExpires  The expiration timestamp.
     * @return The fully constructed trashed path.
     */
    public static String getTrashedPath(String parentPath, String relativePath, long dateExpires) {
        String newRelativePath = Arrays.stream(relativePath.split("/"))
                .map(component -> String.format(
                        Locale.US, ".%s-%d-%s", FileUtils.PREFIX_TRASHED, dateExpires,
                        component))
                .collect(Collectors.joining("/"));
        return parentPath + "/" + newRelativePath;
    }

    /**
     * Moves a legacy in-place trashed file to the trash directory structure.
     *
     * @param legacyTrashFilePath The path of the legacy in-place trashed file.
     * @param fileRenameCallback  A callback to perform the file rename operation.
     * @return {@code true} if the move was successful, {@code false} otherwise.
     * @throws IllegalStateException if the destination trash directory cannot be created.
     */
    public static boolean moveInPlaceTrashFileToTrashLocation(String legacyTrashFilePath,
            FileRenameCallback fileRenameCallback) throws IllegalStateException {
        File trashBaseDir = getOrCreateTrashBaseDirectory(new File(legacyTrashFilePath));
        String volumeRoot = FileUtils.extractVolumePath(legacyTrashFilePath);
        String relPath = legacyTrashFilePath.substring(volumeRoot.length());
        if (relPath.startsWith(File.separator)) {
            relPath = relPath.substring(1);
        }
        File destParent = new File(trashBaseDir, new File(relPath).getParent());
        if (!destParent.mkdirs()) {
            if (!destParent.exists()) {
                throw new IllegalStateException(
                        "Failed to create trash sub-directory: " + destParent.getAbsolutePath());
            }
        }
        File newTrashFile = new File(destParent, new File(relPath).getName());
        return fileRenameCallback.renameFile(legacyTrashFilePath, newTrashFile.getAbsolutePath())
                == 0;
    }

    /**
     * Validates the provided file path and returns the corresponding File object.
     *
     * @param filePath The absolute path of the file or directory.
     * @return The File object representing the original item.
     * @throws IllegalArgumentException if the file path is null or empty, or if the path is not
     *                                  allowed to be trashed.
     * @throws FileNotFoundException    if the original file does not exist.
     */
    private static File getValidatedFilePath(String filePath)
            throws IllegalArgumentException, FileNotFoundException {
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty.");
        }

        File original = new File(filePath);
        if (!original.exists()) {
            throw new FileNotFoundException("Original file not found: " + filePath);
        }

        if (!isAllowedToTrash(original)) {
            throw new IllegalArgumentException("This path is not allowed to trash: " + filePath);
        }

        return original;
    }

    /**
     * Prepares the full destination path for the file within the trash directory.
     *
     * @param originalFile       The file to be trashed.
     * @param trashBaseDirectory The base trash directory.
     * @return The File object representing the final destination of the trashed item.
     * @throws IllegalStateException if the destination parent directory cannot be created.
     */
    private static File prepareDestinationFile(File originalFile, File trashBaseDirectory,
            long dateExpires)
            throws IllegalStateException {
        final String displayName = originalFile.getName();

        final String updatedDisplayName = String.format(
                Locale.US, ".%s-%d-%s", PREFIX_TRASHED, dateExpires, displayName);

        String volumeRoot = FileUtils.extractVolumePath(originalFile.getAbsolutePath());
        String relPath = originalFile.getAbsolutePath().substring(volumeRoot.length());
        if (relPath.startsWith(File.separator)) {
            relPath = relPath.substring(1);
        }

        File destParent = new File(trashBaseDirectory, new File(relPath).getParent());

        if (!destParent.mkdirs()) {
            if (!destParent.exists()) {
                throw new IllegalStateException(
                        "Failed to create trash sub-directory: " + destParent.getAbsolutePath());
            }
        }

        return new File(destParent, updatedDisplayName);
    }

    /**
     * Determines and creates the base trash directory for the given file's volume.
     *
     * @param originalFile The file to be trashed.
     * @return The File object representing the trash base directory.
     * @throws IllegalStateException if the trash directory cannot be created.
     */
    private static File getOrCreateTrashBaseDirectory(File originalFile)
            throws IllegalStateException {
        String volumeRoot = FileUtils.extractVolumePath(originalFile.getAbsolutePath());
        File storageRoot = new File(volumeRoot);
        File trashBase = new File(storageRoot, FileUtils.DIRECTORY_TRASH_STORAGE);

        if (!trashBase.mkdirs()) {
            if (!trashBase.exists()) {
                throw new IllegalStateException(
                        "Failed to create trash directory: " + trashBase.getAbsolutePath());
            }
        }
        return trashBase;
    }

    private static boolean isAllowedToTrash(File file) {
        String relativePath = FileUtils.extractRelativePath(file.getAbsolutePath());
        // "/" if file is top-level file/folder
        if (relativePath == null || relativePath.equals("/")) {
            Log.w(TAG, "Cannot trash top-level files/folders");
            return false;
        }

        // should not be invisible path
        if (FileUtils.shouldBeInvisible(file.getAbsolutePath())) {
            Log.w(TAG, "Cannot trash restricted path");
            return false;
        }

        return true;
    }
}
