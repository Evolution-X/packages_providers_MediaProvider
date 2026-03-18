/*
 * Copyright 2026 The Android Open Source Project
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

package com.android.photopicker.features.camera

import android.content.Intent
import android.provider.MediaStore
import com.android.photopicker.core.configuration.PhotopickerConfiguration
import com.android.photopicker.core.configuration.PhotopickerFlags
import com.android.photopicker.core.configuration.PhotopickerRuntimeEnv
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class CameraFeatureTest {

    private val testConfig: PhotopickerConfiguration =
        PhotopickerConfiguration(
            runtimeEnv = PhotopickerRuntimeEnv.ACTIVITY,
            action = MediaStore.ACTION_PICK_IMAGES,
            flags = PhotopickerFlags(POLAROID_ENABLED = true),
            sessionId = 1234,
        )

    @Test
    fun testCameraFeatureIsEnabled_withEligibleConfig() = runTest {
        val result = CameraFeature.Registration.isEnabled(testConfig, emptyMap())
        assertThat(result).isTrue()
    }

    @Test
    fun testCameraFeatureIsEnabled_withGetContentAction() = runTest {
        val config = testConfig.copy(action = Intent.ACTION_GET_CONTENT)
        val result = CameraFeature.Registration.isEnabled(config, emptyMap())
        assertThat(result).isTrue()
    }

    @Test
    fun testCameraFeatureIsDisabled_whenWrongRuntime() = runTest {
        val config = testConfig.copy(runtimeEnv = PhotopickerRuntimeEnv.EMBEDDED)
        val result = CameraFeature.Registration.isEnabled(config, emptyMap())
        assertThat(result).isFalse()
    }

    @Test
    fun testCameraFeatureIsDisabled_whenWrongAction() = runTest {
        val config = testConfig.copy(action = MediaStore.ACTION_USER_SELECT_IMAGES_FOR_APP)
        val result = CameraFeature.Registration.isEnabled(config, emptyMap())
        assertThat(result).isFalse()
    }

    @Test
    fun testCameraFeatureIsDisabled_whenFlagIsDisabled() = runTest {
        val config = testConfig.copy(flags = PhotopickerFlags(POLAROID_ENABLED = false))
        val result = CameraFeature.Registration.isEnabled(config, emptyMap())
        assertThat(result).isFalse()
    }
}
