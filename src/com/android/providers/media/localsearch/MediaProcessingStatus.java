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

/**
 * Manages the processing status of media files in the {@code MEDIA_PROCESSING_STATUS} table.
 * This class tracks the status of various processing types
 * (e.g., media labels, location, metadata) for each media file.
 */
public class MediaProcessingStatus {
    public static final String TAG = "MediaProcessingStatus";
    public static final String MEDIA_PROCESSING_STATUS_TABLE = "media_processing_status";
    // Column names for media_processing_status table
    public static final String FILE_ID_COLUMN = "file_id";
    public static final String MEDIA_TYPE = "media_type";
    public static final String GEN_MODIFIED = "generation_modified";

    /**
     * Label Status columns track the processing state or retry count for each processing type.
     */
    public static final String MEDIA_LABEL_STATUS = "is_media_label_processed";
    public static final String LOCATION_LABEL_STATUS = "is_location_label_processed";
    public static final String METADATA_LABEL_STATUS = "is_metadata_label_processed";
}
