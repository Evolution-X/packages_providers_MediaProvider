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

package com.android.signature

import android.content.Intent
import android.platform.test.annotations.RequiresFlagsDisabled
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val SIGNATURE_FLAG = "com.android.signature.flags.enable_signature"

@RunWith(AndroidJUnit4::class)
class SignatureActivityTest {

    @get:Rule
    val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    @Test
    @RequiresFlagsEnabled(SIGNATURE_FLAG)
    fun testSignatureActivity_flagEnabled_activityExistsAndWorks() {
        val intent =
            Intent(ApplicationProvider.getApplicationContext(), SignatureActivity::class.java)
        ActivityScenario.launch<SignatureActivity>(intent).use { scenario ->
            val composeTestRule = createAndroidComposeRule<SignatureActivity>()
            composeTestRule.onNodeWithText("Hello World from Signature!").assertIsDisplayed()
        }
    }

    @Test
    @RequiresFlagsDisabled(SIGNATURE_FLAG)
    fun testSignatureActivity_flagDisabled_activityDoesNotExist() {
        val intent =
            Intent(ApplicationProvider.getApplicationContext(), SignatureActivity::class.java)

        val exception = assertThrows(RuntimeException::class.java) {
            ActivityScenario.launch<SignatureActivity>(intent)
        }

        assertThat(exception).hasMessageThat()
            .contains("Unable to resolve activity for: Intent { cmp=com.android.signature/.SignatureActivity }")
    }
}
