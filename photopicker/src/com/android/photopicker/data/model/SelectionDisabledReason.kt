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

package com.android.photopicker.data.model

/**
 * Enum defining the reasons why a media item might be disabled for selection in the Photo Picker.
 *
 * These reasons map to the constraints defined in
 * [android.widget.photopicker.PhotoPickerSelectionParams].
 */
enum class SelectionDisabledReason {
    /** The media item's size exceeds the maximum allowed size in bytes. */
    EXCEEDS_MAX_SIZE,

    /** The video's duration exceeds the maximum allowed duration in seconds. */
    EXCEEDS_MAX_DURATION,

    /** The video's duration is below the minimum required duration in seconds. */
    FALLS_BELOW_MIN_DURATION,

    /** The media item's resolution exceeds the maximum allowed resolution in pixels. */
    EXCEEDS_MAX_RESOLUTION,

    /** The media item's resolution is below the minimum required resolution in pixels. */
    FALLS_BELOW_MIN_RESOLUTION,

    /** The media item's MIME type is not in the list of allowed MIME types. */
    MIME_TYPE_NOT_ALLOWED;

    companion object {
        /**
         * Returns the [SelectionDisabledReason] corresponding to the given name, or null if the
         * name is not recognized.
         */
        fun fromName(name: String?): SelectionDisabledReason? {
            return entries.find { it.name == name }
        }
    }
}
