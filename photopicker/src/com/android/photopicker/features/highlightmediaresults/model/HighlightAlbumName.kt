/*
 * Copyright 2025 The Android Open Source Project
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

package com.android.photopicker.features.highlightmediaresults.model

import android.provider.MediaStore

/**
 * An enum class representing the possible media albums that can be highlighted by an app in
 * [MediaStore.EXTRA_PICK_IMAGES_HIGHLIGHT_QUERY_RESULTS]
 */
enum class HighlightAlbumName {
    // Highlight the Favorites album
    HIGHLIGHT_ALBUM_FAVORITES,
    // Highlight the Camera album
    HIGHLIGHT_ALBUM_CAMERA,
    // Highlight the Screenshots album
    HIGHLIGHT_ALBUM_SCREENSHOTS,
    // Highlight the Videos album
    HIGHLIGHT_ALBUM_VIDEOS,
    // Highlight the Downloads album
    HIGHLIGHT_ALBUM_DOWNLOADS,
    UNSET_HIGHLIGHT_ALBUM;

    companion object {
        fun toHighlightAlbumName(name: String): HighlightAlbumName {
            return when (name) {
                MediaStore.PICK_IMAGES_HIGHLIGHT_ALBUM_FAVORITES -> HIGHLIGHT_ALBUM_FAVORITES
                MediaStore.PICK_IMAGES_HIGHLIGHT_ALBUM_CAMERA -> HIGHLIGHT_ALBUM_CAMERA
                MediaStore.PICK_IMAGES_HIGHLIGHT_ALBUM_SCREENSHOTS -> HIGHLIGHT_ALBUM_SCREENSHOTS
                MediaStore.PICK_IMAGES_HIGHLIGHT_ALBUM_VIDEOS -> HIGHLIGHT_ALBUM_VIDEOS
                MediaStore.PICK_IMAGES_HIGHLIGHT_ALBUM_DOWNLOADS -> HIGHLIGHT_ALBUM_DOWNLOADS
                else -> UNSET_HIGHLIGHT_ALBUM
            }
        }
    }
}
