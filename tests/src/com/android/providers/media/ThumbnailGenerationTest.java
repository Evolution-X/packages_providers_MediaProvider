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

package com.android.providers.media;

import static com.android.providers.media.scan.MediaScannerTest.stage;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.os.FileUtils;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.MediaStore;
import android.util.Size;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import com.android.providers.media.flags.Flags;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

@RunWith(AndroidJUnit4.class)
public class ThumbnailGenerationTest {

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    private ContentResolver mIsolatedResolver;
    private File mDir;

    @Before
    public void setUp() {
        final Context context = InstrumentationRegistry.getTargetContext();
        InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .adoptShellPermissionIdentity(Manifest.permission.LOG_COMPAT_CHANGE,
                        Manifest.permission.READ_COMPAT_CHANGE_CONFIG,
                        Manifest.permission.MANAGE_EXTERNAL_STORAGE);

        final File downloadDir = Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        mDir = new File(downloadDir, "test-" + System.nanoTime());
        mDir.mkdirs();

        FileUtils.deleteContents(mDir);

        Context isolatedContext = new IsolatedContext(context, "modern", /*asFuseThread*/ false);
        mIsolatedResolver = isolatedContext.getContentResolver();
    }

    @After
    public void tearDown() {
        if (mDir != null) {
            FileUtils.deleteContents(mDir);
            mDir.delete();
        }
        InstrumentationRegistry.getInstrumentation()
                .getUiAutomation().dropShellPermissionIdentity();
    }

    /**
     * Verifies thumbnail generation with the legacy synchronous flow.
     */
    @Test
    @DisableFlags(Flags.FLAG_ENABLE_ASYNC_THUMBNAIL_GENERATION)
    public void testThumbnail_Sync() throws Exception {
        verifyThumbnailWithScanning("sync_thumb.jpg");
    }

    /**
     * Verifies thumbnail generation with the new asynchronous flow.
     */
    @Test
    @EnableFlags(Flags.FLAG_ENABLE_ASYNC_THUMBNAIL_GENERATION)
    public void testThumbnail_Async() throws Exception {
        verifyThumbnailWithScanning("async_thumb.jpg");
    }

    private void verifyThumbnailWithScanning(String fileName) throws Exception {
        final File file = new File(mDir, fileName);
        // Stage the initial test image
        stage(R.raw.lg_g4_iso_800_jpg, file);
        final Uri uri = MediaStore.scanFile(mIsolatedResolver, file);

        // Request and verify the thumbnail
        Size size = new Size(100, 100);
        Bitmap bitmap = mIsolatedResolver.loadThumbnail(uri, size, null);
        assertNotNull("Thumbnail should not be null", bitmap);
        assertTrue(bitmap.getWidth() > 0);
        assertTrue(bitmap.getHeight() > 0);
    }

}
