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

package android.provider;

import android.annotation.FlaggedApi;
import android.database.CursorWindow;
import android.util.Log;

import com.android.providers.media.flags.Flags;

import java.util.ArrayList;
import java.util.List;

/**
 * @hide
 */
@FlaggedApi(Flags.FLAG_ENABLE_MEDIA_SEARCH)
public class SearchMediaUtils {
    private static final String TAG = SearchMediaUtils.class.getSimpleName();
    private static final String CURSOR_WINDOW_NAME_PREFIX = "search_media_cursor_window_";

    static CursorWindow[] convertToCursorWindows(List<SearchMediaResult> searchResults) {
        if (searchResults == null || searchResults.isEmpty()) {
            return new CursorWindow[0];
        }

        List<CursorWindow> completedWindows = new ArrayList<>();
        CursorWindow currentWindow = createNewCursorWindow(/* windowId */ completedWindows.size());
        completedWindows.add(currentWindow);
        int rowIndexInCurrentWindow = 0;

        for (SearchMediaResult result : searchResults) {
            if (result == null) {
                continue;
            }

            // Attempt to allocate a new row in the current window.
            if (!currentWindow.allocRow()) {
                // If we are unable to allocate a new row, create a new window.
                currentWindow =
                        createNewCursorWindow(/* windowId */ completedWindows.size());
                completedWindows.add(currentWindow);
                rowIndexInCurrentWindow = 0;

                if (!currentWindow.allocRow()) {
                    // This would happen row size is too big and window cannot hold a single row.
                    // We do not expect this to happen
                    Log.e(TAG, "Failed to allocate row in a new window. "
                            + "Data row may be too large.");
                    break;
                }
            }

            // Row is successfully allocated
            result.writeToCursorWindow(currentWindow, rowIndexInCurrentWindow);
            rowIndexInCurrentWindow++;
        }

        return completedWindows.toArray(new CursorWindow[0]);
    }

    private static CursorWindow createNewCursorWindow(int windowId) {
        CursorWindow window = new CursorWindow(CURSOR_WINDOW_NAME_PREFIX + windowId);
        // we add 1 to columns count to include rowId of the window
        window.setNumColumns(SearchMediaResult.CURSOR_WINDOW_COLUMN_COUNT + 1);
        return window;
    }
}
