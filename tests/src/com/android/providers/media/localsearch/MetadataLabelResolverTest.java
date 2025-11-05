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

package com.android.providers.media.localsearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.os.Build;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.provider.MediaStore.Files.FileColumns;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;

import com.android.providers.media.flags.Flags;
import com.android.providers.media.localsearch.MetadataLabelResolver.MetadataInfo;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
@RequiresFlagsEnabled(Flags.FLAG_ENABLE_MEDIA_PROCESSING)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
public class MetadataLabelResolverTest {
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    private MetadataLabelResolver mResolver;

    @Before
    public void setUp() {
        mResolver = new MetadataLabelResolver();
    }

    @Test
    public void testBuildMetadataLabel_allFieldsPopulated() {
        // Fixed timestamp: October 12, 2021 14:23:26 UTC
        long timestamp = 1634048606830L;

        MetadataInfo info = new MetadataInfo.Builder()
                .setId(1)
                .setDisplayName("Vacation.jpg")
                .setRelativePath("DCIM/Camera/")
                .setMediaType(FileColumns.MEDIA_TYPE_IMAGE)
                .setMimeType("image/jpeg")
                .setSpecialFormat(0) // None
                .setDateTaken(timestamp)
                .setDateAdded(timestamp + 1000) // Different to test dateTaken priority
                .setIsFavorite(1)
                .setIsDownload(0)
                .setArtist("Me")
                .setAlbum("Holidays")
                .setGenre("Travel")
                .build();

        String label = mResolver.buildMetadataLabel(info);

        String expected = "vacation jpg dcim camera image images image jpeg october 2021 "
                + "favorite favorites me holidays travel";
        assertEquals(expected, label);
    }

    @Test
    public void testBuildMetadataLabel_minimalFields() {
        // Only ID is set (others default to null or -1/0)
        MetadataInfo info = new MetadataInfo.Builder()
                .setId(100)
                .build();

        String label = mResolver.buildMetadataLabel(info);

        // Should be empty string as no valid metadata was added
        assertEquals("", label);
    }

    @Test
    public void testBuildMetadataLabel_formattingLogic() {
        MetadataInfo info = new MetadataInfo.Builder()
                .setId(2)
                .setDisplayName("Video_Clip.mp4")     // -> "video_clip mp4"
                .setRelativePath("/Movies/SciFi")     // -> "movies scifi"
                .setMediaType(FileColumns.MEDIA_TYPE_VIDEO) // -> "video"
                .setMimeType("video/mp4")             // -> "video mp4"
                .setIsDownload(1)                     // -> "download"
                .build();

        String label = mResolver.buildMetadataLabel(info);
        String expected = "video_clip mp4 movies scifi video videos video mp4 download downloads";

        assertEquals(expected, label);
    }

    @Test
    public void testBuildMetadataLabel_specialFormats() {
        MetadataInfo info = new MetadataInfo.Builder()
                .setId(3)
                .setMediaType(FileColumns.MEDIA_TYPE_IMAGE)
                .setSpecialFormat(FileColumns.SPECIAL_FORMAT_ANIMATED_WEBP) // -> "animated"
                .build();

        String label = mResolver.buildMetadataLabel(info);
        assertTrue(label.contains("animated"));

        MetadataInfo info2 = new MetadataInfo.Builder()
                .setId(4)
                .setSpecialFormat(FileColumns.SPECIAL_FORMAT_MOTION_PHOTO) // -> "motion photo"
                .build();
        assertTrue(mResolver.buildMetadataLabel(info2).contains("motion photo"));

        MetadataInfo info3 = new MetadataInfo.Builder()
                .setId(5)
                .setSpecialFormat(FileColumns.SPECIAL_FORMAT_GIF) // -> "gif"
                .build();
        assertTrue(mResolver.buildMetadataLabel(info3).contains("gif"));
    }

    @Test
    public void testBuildMetadataLabel_audioAndTimestamps() {
        // Timestamp: January 2025
        long dateTaken = 1735689600000L; // Jan 1 2025 UTC

        MetadataInfo info = new MetadataInfo.Builder()
                .setId(5)
                .setMediaType(FileColumns.MEDIA_TYPE_AUDIO) // -> "audio"
                .setDateTaken(dateTaken)                    // -> "january 2025"
                .setArtist("Beethoven")
                .build();

        String label = mResolver.buildMetadataLabel(info);
        String expected = "audio audios january 2025 beethoven";
        assertEquals(expected, label);
    }

    @Test
    public void testBuildMetadataLabel_timestampFallback() {
        // Timestamp: January 2025
        long dateAdded = 1735689600000L; // Jan 1 2025 UTC

        // No Date Taken, should use Date Added
        MetadataInfo info1 = new MetadataInfo.Builder()
                .setId(6)
                .setDateAdded(dateAdded)
                .build();
        assertTrue(mResolver.buildMetadataLabel(info1).contains("january 2025"));

        // Invalid Date Taken, should use Date Added
        MetadataInfo info2 = new MetadataInfo.Builder()
                .setId(7)
                .setDateTaken(0)
                .setDateAdded(dateAdded)
                .build();
        assertTrue(mResolver.buildMetadataLabel(info2).contains("january 2025"));

        // Both valid, should use Date Taken (December 2024 vs January 2025)
        long dateTaken = 1733011200000L; // Dec 1 2024 UTC
        MetadataInfo info3 = new MetadataInfo.Builder()
                .setId(8)
                .setDateTaken(dateTaken)
                .setDateAdded(dateAdded)
                .build();
        assertTrue(mResolver.buildMetadataLabel(info3).contains("december 2024"));
        assertFalse(mResolver.buildMetadataLabel(info3).contains("january 2025"));
    }

    @Test
    public void testGenerateMetadataLabels_batch() {
        MetadataInfo item1 = new MetadataInfo.Builder()
                .setId(10)
                .setDisplayName("A.jpg")
                .setMediaType(FileColumns.MEDIA_TYPE_IMAGE)
                .build(); // Label: "a jpg image"

        MetadataInfo item2 = new MetadataInfo.Builder()
                .setId(20)
                .setDisplayName("B.mp4")
                .setMediaType(FileColumns.MEDIA_TYPE_VIDEO)
                .build(); // Label: "b mp4 video"

        List<MetadataInfo> batch = Arrays.asList(item1, item2);

        Map<Long, String> result = mResolver.generateMetadataLabels(batch);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("a jpg image images", result.get(10L));
        assertEquals("b mp4 video videos", result.get(20L));
    }

    @Test
    public void testGenerateMetadataLabels_emptyList() {
        Map<Long, String> result = mResolver.generateMetadataLabels(Collections.emptyList());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testProcessDisplayName_edgeCases() {
        // These rely on private methods, but we test them via the public API

        // 1. No extension
        MetadataInfo infoNoExt = new MetadataInfo.Builder().setDisplayName("README").build();
        assertEquals("readme", mResolver.buildMetadataLabel(infoNoExt));

        // 2. Dot at start
        MetadataInfo infoDotStart = new MetadataInfo.Builder().setDisplayName(".hidden").build();
        assertEquals(".hidden", mResolver.buildMetadataLabel(infoDotStart));

        // 3. Null (handled in minimal fields test, but explicitly here)
        MetadataInfo infoNull = new MetadataInfo.Builder().setDisplayName(null).build();
        assertEquals("", mResolver.buildMetadataLabel(infoNull));
    }

    @Test
    public void testProcessTimestamp_invalid() {
        // 0 is invalid
        MetadataInfo infoZero = new MetadataInfo.Builder().setDateTaken(0).build();
        assertEquals("", mResolver.buildMetadataLabel(infoZero));

        // Negative is invalid
        MetadataInfo infoNeg = new MetadataInfo.Builder().setDateTaken(-1000L).build();
        assertEquals("", mResolver.buildMetadataLabel(infoNeg));
    }
}
