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
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.database.CursorWindow;

import com.android.providers.media.flags.Flags;

import java.util.Objects;

/**
 * @hide
 */
@FlaggedApi(Flags.FLAG_ENABLE_MEDIA_SEARCH)
@SystemApi
public final class SearchMediaResult {
    public static final int CURSOR_WINDOW_COLUMN_COUNT = 4;
    public static final int INDEX_COLUMN_ID = 0;
    public static final int INDEX_COLUMN_DATE_TAKEN = 1;
    public static final int INDEX_COLUMN_SCORE = 2;
    public static final int INDEX_COLUMN_MEDIA_TYPE = 3;

    public final long id;
    public final long dateTaken;
    public final double score;
    public final long mediaType;

    public SearchMediaResult(long id, long dateTaken, double score,
            long mediaType) {
        this.id = id;
        this.dateTaken = dateTaken;
        this.score = score;
        this.mediaType = mediaType;
    }
    void writeToCursorWindow(@NonNull CursorWindow window, int row) {
        window.putLong(this.id, row, INDEX_COLUMN_ID);
        window.putLong(this.dateTaken, row, INDEX_COLUMN_DATE_TAKEN);
        window.putDouble(this.score, row, INDEX_COLUMN_SCORE);
        window.putLong(this.mediaType, row, INDEX_COLUMN_MEDIA_TYPE);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        SearchMediaResult that = (SearchMediaResult) o;
        return dateTaken == that.dateTaken && Double.compare(score, that.score)
                == 0 && mediaType == that.mediaType && Objects.equals(id,
                that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, dateTaken, score, mediaType);
    }

    @Override
    public String toString() {
        return "SearchMediaResult{"
                + "id='" + id + '\''
                + ", lastModifiedDate=" + dateTaken
                + ", score=" + score
                + ", mediaType=" + mediaType
                + '}';
    }
}
