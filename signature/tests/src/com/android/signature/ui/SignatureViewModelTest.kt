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

package com.android.signature.ui

import com.android.signature.data.Signature
import com.android.signature.data.SignatureDao
import com.android.signature.data.SignatureRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class SignatureViewModelTest {

    @Mock
    private lateinit var signatureDao: SignatureDao

    private lateinit var repository: SignatureRepository
    private lateinit var viewModel: SignatureViewModel
    private val signaturesFlow = MutableStateFlow<List<Signature>>(emptyList())

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        whenever(signatureDao.getAllSignatures()).thenReturn(signaturesFlow)
        repository = SignatureRepository(signatureDao)
        viewModel = SignatureViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun signatures_initiallyEmpty() = runTest {
        val signatures = viewModel.signatures.first()
        assertEquals(emptyList<Signature>(), signatures)
    }

    @Test
    fun signatures_updatesFromRepository() = runTest {
        val signature = Signature(id = "1", type = Signature.TYPE_TYPED, textData = "Test")
        signaturesFlow.emit(listOf(signature))

        val signatures = viewModel.signatures.first()
        assertEquals(listOf(signature), signatures)
    }

    @Test
    fun deleteSignature_delegatesToRepository() = runTest {
        val signature = Signature(id = "1", type = Signature.TYPE_TYPED, textData = "Test")

        viewModel.deleteSignature(signature)

        verify(signatureDao).deleteSignature(signature)
    }

    @Test
    fun setNewSignatureId_updatesState() = runTest {
        assertNull(viewModel.newSignatureId.value)

        viewModel.setNewSignatureId("123")
        assertEquals("123", viewModel.newSignatureId.value)

        viewModel.setNewSignatureId(null)
        assertNull(viewModel.newSignatureId.value)
    }

    @Test
    fun setSignatureToDelete_updatesState() = runTest {
        assertNull(viewModel.signatureToDelete.value)

        val signature = Signature(id = "1", type = Signature.TYPE_TYPED, textData = "Test")
        viewModel.setSignatureToDelete(signature)
        assertEquals(signature, viewModel.signatureToDelete.value)

        viewModel.setSignatureToDelete(null)
        assertNull(viewModel.signatureToDelete.value)
    }

    @Test
    fun getSignatureUri_typedSignature_returnsCorrectUri() {
        val signature = Signature(
            id = "1", type = Signature.TYPE_TYPED, textData = "Hello", fontName = "Serif"
        )

        val uri = viewModel.getSignatureUri(signature)

        assertEquals("content", uri.scheme)
        assertEquals("com.android.signature.provider", uri.authority)
        assertEquals("/signatures/1", uri.path)
        assertEquals(Signature.TYPE_TYPED.toString(), uri.getQueryParameter("type"))
        assertEquals("Hello", uri.getQueryParameter("text"))
        assertEquals("Serif", uri.getQueryParameter("font"))
    }

    @Test
    fun getSignatureUri_drawnSignature_returnsCorrectUri() {
        val signature = Signature(
            id = "2", type = Signature.TYPE_DRAWN, drawingPaths = "path_data"
        )

        val uri = viewModel.getSignatureUri(signature)

        assertEquals("content", uri.scheme)
        assertEquals("com.android.signature.provider", uri.authority)
        assertEquals("/signatures/2", uri.path)
        assertEquals(Signature.TYPE_DRAWN.toString(), uri.getQueryParameter("type"))
        assertEquals("path_data", uri.getQueryParameter("paths"))
    }
}
