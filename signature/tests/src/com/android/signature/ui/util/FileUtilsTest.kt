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

package com.android.signature.ui.util

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class FileUtilsTest {
    @Test
    fun getFileSize_returnsSize() {
        val context = Mockito.mock(Context::class.java)
        val contentResolver = Mockito.mock(ContentResolver::class.java)
        val cursor = Mockito.mock(Cursor::class.java)
        val uri = Mockito.mock(Uri::class.java)

        whenever(context.contentResolver).thenReturn(contentResolver)
        whenever(
            contentResolver.query(
                eq(uri),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
            ),
        ).thenReturn(cursor)

        whenever(cursor.getColumnIndex(OpenableColumns.SIZE)).thenReturn(0)
        whenever(cursor.moveToFirst()).thenReturn(true)
        whenever(cursor.getLong(0)).thenReturn(1024L)

        val size = getFileSize(context, uri)

        assertEquals(1024L, size)
        cursor.close() // Should be closed by use block, but verifying logic
    }

    @Test
    fun getFileSize_cursorNull_returnsZero() {
        val context = Mockito.mock(Context::class.java)
        val contentResolver = Mockito.mock(ContentResolver::class.java)
        val uri = Mockito.mock(Uri::class.java)

        whenever(context.contentResolver).thenReturn(contentResolver)
        whenever(
            contentResolver.query(
                eq(uri),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
            ),
        ).thenReturn(null)

        val size = getFileSize(context, uri)

        assertEquals(0L, size)
    }

    @Test
    fun getFileSize_cursorEmpty_returnsZero() {
        val context = Mockito.mock(Context::class.java)
        val contentResolver = Mockito.mock(ContentResolver::class.java)
        val cursor = Mockito.mock(Cursor::class.java)
        val uri = Mockito.mock(Uri::class.java)

        whenever(context.contentResolver).thenReturn(contentResolver)
        whenever(
            contentResolver.query(
                eq(uri),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
            ),
        ).thenReturn(cursor)

        whenever(cursor.getColumnIndex(OpenableColumns.SIZE)).thenReturn(0)
        whenever(cursor.moveToFirst()).thenReturn(false)

        val size = getFileSize(context, uri)

        assertEquals(0L, size)
    }
}
