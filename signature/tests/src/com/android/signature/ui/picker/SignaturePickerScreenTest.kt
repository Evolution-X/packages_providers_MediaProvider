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

package com.android.signature.ui.picker

import android.content.Context
import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.android.signature.HiltTestActivity
import com.android.signature.R
import com.android.signature.data.Signature
import com.android.signature.data.SignatureDao
import com.android.signature.data.SignatureRepository
import com.android.signature.ui.SignatureViewModel
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.whenever

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SignaturePickerScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val signatureDao: SignatureDao = Mockito.mock(SignatureDao::class.java).apply {
        whenever(getAllSignatures()).thenReturn(flowOf(emptyList()))
    }

    private val repository: SignatureRepository = SignatureRepository(signatureDao)

    @Before
    fun setup() {
        hiltRule.inject()
        val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        try {
            uiDevice.wakeUp()
            uiDevice.executeShellCommand("wm dismiss-keyguard")
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun launchActivityAndSetContent(
        content: @androidx.compose.runtime.Composable () -> Unit
    ) {
        ActivityScenario.launch(HiltTestActivity::class.java).onActivity { activity ->
            activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity.setShowWhenLocked(true)
            activity.setTurnScreenOn(true)
            activity.setContent(content = content)
        }
    }

    @Test
    fun signaturePickerScreen_emptyList_displaysPlaceholder() {
        // Re-stub if needed (already empty)
        val viewModel = SignatureViewModel(repository)

        launchActivityAndSetContent {
            SignaturePickerScreen(
                viewModel = viewModel,
                onAddSignature = {},
                onSignatureSelected = { _, _ -> },
                onCancel = {})
        }

        composeTestRule.onNodeWithText(context.getString(R.string.picker_no_signatures))
            .assertIsDisplayed()
    }

    @Test
    fun signaturePickerScreen_populatedList_displaysSignatures() {
        val signature1 = Signature(id = "1", type = Signature.TYPE_TYPED, textData = "Sig 1")
        val signature2 = Signature(id = "2", type = Signature.TYPE_TYPED, textData = "Sig 2")
        whenever(signatureDao.getAllSignatures()).thenReturn(
            flowOf(listOf(signature1, signature2))
        )
        val viewModel = SignatureViewModel(repository)

        launchActivityAndSetContent {
            SignaturePickerScreen(
                viewModel = viewModel,
                onAddSignature = {},
                onSignatureSelected = { _, _ -> },
                onCancel = {})
        }

        composeTestRule.onNodeWithText("Sig 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sig 2").assertIsDisplayed()
    }

    @Test
    fun signaturePickerScreen_addNew_triggersCallback() {
        val viewModel = SignatureViewModel(repository)
        var addClicked = false

        launchActivityAndSetContent {
            SignaturePickerScreen(
                viewModel = viewModel,
                onAddSignature = { addClicked = true },
                onSignatureSelected = { _, _ -> },
                onCancel = {})
        }

        composeTestRule.onNodeWithText(context.getString(R.string.picker_add_new)).performClick()
        assert(addClicked)
    }

    @Test
    fun signaturePickerScreen_selectSignature_triggersCallback() {
        val signature = Signature(id = "1", type = Signature.TYPE_TYPED, textData = "Sig 1")
        whenever(signatureDao.getAllSignatures()).thenReturn(flowOf(listOf(signature)))

        val viewModel = SignatureViewModel(repository)
        var selectedSignature: Signature? = null

        launchActivityAndSetContent {
            SignaturePickerScreen(
                viewModel = viewModel,
                onAddSignature = {},
                onSignatureSelected = { sig, _ -> selectedSignature = sig },
                onCancel = {})
        }

        composeTestRule.onNodeWithText("Sig 1").performClick()
        assert(selectedSignature == signature)
    }

    @Test
    fun signaturePickerScreen_deleteSignature_showsDialog() {
        val signature = Signature(id = "1", type = Signature.TYPE_TYPED, textData = "Sig 1")
        whenever(signatureDao.getAllSignatures()).thenReturn(flowOf(listOf(signature)))
        val viewModel = SignatureViewModel(repository)

        launchActivityAndSetContent {
            SignaturePickerScreen(
                viewModel = viewModel,
                onAddSignature = {},
                onSignatureSelected = { _, _ -> },
                onCancel = {})
        }

        // Click delete icon
        composeTestRule.onNodeWithContentDescription(
            context.getString(R.string.delete_signature_content_description)
        ).performClick()

        // Verify dialog appears
        composeTestRule.onNodeWithText(context.getString(R.string.delete_signature_title))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.delete_signature_message))
            .assertIsDisplayed()
    }

    @Test
    fun signaturePickerScreen_scrollableList() {
        // Create enough signatures to overflow the screen
        val signatures = List(20) { i ->
            Signature(id = "$i", type = Signature.TYPE_TYPED, textData = "Signature $i")
        }
        whenever(signatureDao.getAllSignatures()).thenReturn(flowOf(signatures))
        val viewModel = SignatureViewModel(repository)

        launchActivityAndSetContent {
            SignaturePickerScreen(
                viewModel = viewModel,
                onAddSignature = {},
                onSignatureSelected = { _, _ -> },
                onCancel = {})
        }

        // Verify first item is displayed
        composeTestRule.onNodeWithText("Signature 0").assertIsDisplayed()

        // Scroll to the last item using the list's test tag
        composeTestRule.onNodeWithTag("SignaturePickerList")
            .performScrollToNode(hasText("Signature 19"))

        // Verify last item is displayed
        composeTestRule.onNodeWithText("Signature 19").assertIsDisplayed()
    }
}
