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

import android.content.Context
import android.content.pm.ProviderInfo
import android.net.Uri
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.signature.data.Signature
import com.android.signature.data.SignatureDao
import com.android.signature.data.SignatureRepository
import com.android.signature.di.DatabaseModule
import com.android.signature.flags.Flags
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.android.testing.UninstallModules
import java.io.FileNotFoundException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import org.mockito.kotlin.verifyNoInteractions

@HiltAndroidTest
@UninstallModules(DatabaseModule::class)
@RunWith(AndroidJUnit4::class)
class SignatureProviderTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val setFlagsRule = SetFlagsRule()

    private val signatureDao: SignatureDao = Mockito.mock(SignatureDao::class.java)
    private lateinit var provider: SignatureProvider
    private lateinit var context: Context

    @BindValue
    @JvmField
    val repository: SignatureRepository = SignatureRepository(signatureDao)

    @Before
    fun setup() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext<HiltTestApplication>()
        provider = SignatureProvider()
        val info = ProviderInfo().apply {
            authority = "com.android.signature.provider"
        }
        provider.attachInfo(context, info)
        // Delay calling provider.onCreate() until each test method
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SIGNATURE)
    fun onCreate_flagEnabled_returnsTrue() {
        assertTrue("onCreate should return true when flag is enabled", provider.onCreate())
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_SIGNATURE)
    fun onCreate_flagDisabled_returnsFalse() {
        assertFalse("onCreate should return false when flag is disabled", provider.onCreate())
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SIGNATURE)
    fun getType_signatureExists_returnsImagePng() {
        assertTrue(provider.onCreate())
        val signature = Signature(id = "1", type = Signature.TYPE_DRAWN, imageData = ByteArray(0))
        signatureDao.stub {
            onBlocking { getSignatureById("1") }.doReturn(signature)
        }

        val uri = Uri.parse("content://com.android.signature.provider/signatures/1")
        assertEquals("image/png", provider.getType(uri))
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SIGNATURE)
    fun getType_signatureNotExists_returnsNull() {
        assertTrue(provider.onCreate())
        signatureDao.stub {
            onBlocking { getSignatureById("999") }.doReturn(null)
        }
        val uri = Uri.parse("content://com.android.signature.provider/signatures/999")
        assertNull(provider.getType(uri))
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_SIGNATURE)
    fun getType_flagDisabled_returnsNullAndNoDaoInteraction() {
        assertFalse(provider.onCreate())
        val uri = Uri.parse("content://com.android.signature.provider/signatures/1")
        assertNull(provider.getType(uri))
        verifyNoInteractions(signatureDao)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SIGNATURE)
    fun openFile_signatureExists_returnsFileDescriptor() {
        assertTrue(provider.onCreate())
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
    @EnableFlags(Flags.FLAG_ENABLE_SIGNATURE)
    fun openFile_invalidMode_throwsIllegalArgumentException() {
        assertTrue(provider.onCreate())
        val uri = Uri.parse("content://com.android.signature.provider/signatures/1")
        try {
            provider.openFile(uri, "w")
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Only read mode is supported", e.message)
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SIGNATURE)
    fun openFile_invalidUri_throwsFileNotFoundException() {
        assertTrue(provider.onCreate())
        val uri = Uri.parse("content://com.android.signature.provider/invalid/1")
        try {
            provider.openFile(uri, "r")
            fail("Expected FileNotFoundException")
        } catch (e: FileNotFoundException) {
            assertEquals("Invalid URI", e.message)
        }
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_SIGNATURE)
    fun openFile_flagDisabled_throwsFileNotFoundAndNoDaoInteraction() {
        assertFalse(provider.onCreate())
        val uri = Uri.parse("content://com.android.signature.provider/signatures/1")
        try {
            provider.openFile(uri, "r")
            fail("Expected FileNotFoundException")
        } catch (e: FileNotFoundException) {
            assertEquals("Provider not available", e.message)
        }
        verifyNoInteractions(signatureDao)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SIGNATURE)
    fun query_flagEnabled_returnsNull() {
        assertTrue(provider.onCreate())
        val uri = Uri.parse("content://com.android.signature.provider/signatures/1")
        assertNull(provider.query(uri, null, null, null, null))
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_SIGNATURE)
    fun query_flagDisabled_returnsNull() {
        assertFalse(provider.onCreate())
        val uri = Uri.parse("content://com.android.signature.provider/signatures/1")
        assertNull(provider.query(uri, null, null, null, null))
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SIGNATURE)
    fun insert_flagEnabled_returnsNull() {
        assertTrue(provider.onCreate())
        val uri = Uri.parse("content://com.android.signature.provider/signatures")
        assertNull(provider.insert(uri, null))
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_SIGNATURE)
    fun insert_flagDisabled_returnsNull() {
        assertFalse(provider.onCreate())
        val uri = Uri.parse("content://com.android.signature.provider/signatures")
        assertNull(provider.insert(uri, null))
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SIGNATURE)
    fun delete_flagEnabled_returnsZero() {
        assertTrue(provider.onCreate())
        val uri = Uri.parse("content://com.android.signature.provider/signatures/1")
        assertEquals(0, provider.delete(uri, null, null))
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_SIGNATURE)
    fun delete_flagDisabled_returnsZero() {
        assertFalse(provider.onCreate())
        val uri = Uri.parse("content://com.android.signature.provider/signatures/1")
        assertEquals(0, provider.delete(uri, null, null))
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SIGNATURE)
    fun update_flagEnabled_returnsZero() {
        assertTrue(provider.onCreate())
        val uri = Uri.parse("content://com.android.signature.provider/signatures/1")
        assertEquals(0, provider.update(uri, null, null, null))
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_SIGNATURE)
    fun update_flagDisabled_returnsZero() {
        assertFalse(provider.onCreate())
        val uri = Uri.parse("content://com.android.signature.provider/signatures/1")
        assertEquals(0, provider.update(uri, null, null, null))
    }
}
