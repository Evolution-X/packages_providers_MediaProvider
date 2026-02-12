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

package com.android.signature.provider

import android.content.pm.ProviderInfo
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.signature.data.Signature
import com.android.signature.data.SignatureDao
import com.android.signature.data.SignatureRepository
import com.android.signature.di.DatabaseModule
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.android.testing.UninstallModules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub

@HiltAndroidTest
@UninstallModules(DatabaseModule::class)
@RunWith(AndroidJUnit4::class)
class SignatureProviderTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private val signatureDao: SignatureDao = Mockito.mock(SignatureDao::class.java)
    private lateinit var provider: SignatureProvider

    @BindValue
    @JvmField
    val repository: SignatureRepository = SignatureRepository(signatureDao)

    @Before
    fun setup() {
        hiltRule.inject()
        provider = SignatureProvider()
        val context = ApplicationProvider.getApplicationContext<HiltTestApplication>()
        val info = ProviderInfo().apply {
            authority = "com.android.signature.provider"
        }
        provider.attachInfo(context, info)
    }

    @Test
    fun getType_drawnSignature_returnsImagePng() {
        val signature = Signature(id = "1", type = Signature.TYPE_DRAWN, imageData = ByteArray(0))
        signatureDao.stub {
            onBlocking { getSignatureById("1") }.doReturn(signature)
        }

        val uri = Uri.parse("content://com.android.signature.provider/signatures/1")
        val type = provider.getType(uri)

        assertEquals("image/png", type)
    }

    @Test
    fun getType_uploadedSignature_returnsImagePng() {
        val signature =
            Signature(id = "3", type = Signature.TYPE_UPLOADED, imageData = ByteArray(0))
        signatureDao.stub {
            onBlocking { getSignatureById("3") }.doReturn(signature)
        }

        val uri = Uri.parse("content://com.android.signature.provider/signatures/3")
        val type = provider.getType(uri)

        assertEquals("image/png", type)
    }

    @Test
    fun getType_typedSignature_returnsImagePng() {
        val signature = Signature(id = "2", type = Signature.TYPE_TYPED, textData = "Test")
        signatureDao.stub {
            onBlocking { getSignatureById("2") }.doReturn(signature)
        }

        val uri = Uri.parse("content://com.android.signature.provider/signatures/2")
        val type = provider.getType(uri)

        assertEquals("image/png", type)
    }

    @Test
    fun openFile_drawnSignature_returnsFileDescriptor() {
        val data = byteArrayOf(1, 2, 3)
        val signature = Signature(id = "1", type = Signature.TYPE_DRAWN, imageData = data)
        signatureDao.stub {
            onBlocking { getSignatureById("1") }.doReturn(signature)
        }

        val uri = Uri.parse("content://com.android.signature.provider/signatures/1")
        val pfd = provider.openFile(uri, "r")

        assertNotNull(pfd)
        pfd?.close()
    }

    @Test
    fun openFile_uploadedSignature_returnsFileDescriptor() {
        val data = byteArrayOf(4, 5, 6)
        val signature = Signature(id = "3", type = Signature.TYPE_UPLOADED, imageData = data)
        signatureDao.stub {
            onBlocking { getSignatureById("3") }.doReturn(signature)
        }

        val uri = Uri.parse("content://com.android.signature.provider/signatures/3")
        val pfd = provider.openFile(uri, "r")

        assertNotNull(pfd)
        pfd?.close()
    }

    @Test
    fun openFile_typedSignature_returnsFileDescriptor() {
        val text = "Hello"
        val signature = Signature(id = "2", type = Signature.TYPE_TYPED, textData = text)
        signatureDao.stub {
            onBlocking { getSignatureById("2") }.doReturn(signature)
        }

        val uri = Uri.parse("content://com.android.signature.provider/signatures/2")
        val pfd = provider.openFile(uri, "r")

        assertNotNull(pfd)
        pfd?.close()
    }
}
