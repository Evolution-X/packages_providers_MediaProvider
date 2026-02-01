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

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.android.signature.data.SignatureDao
import com.android.signature.data.SignatureRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.FileOutputStream
import java.io.IOException

/**
 * A [ContentProvider] that exposes signature images to other applications.
 *
 * This provider allows external apps to read signature data (as PNG images) via content URIs.
 * It supports `openFile` to stream the image data.
 *
 * The authority for this provider is `com.android.signature.provider`.
 * Supported URIs:
 * - `content://com.android.signature.provider/signatures/{signatureId}`: Access a specific signature by ID.
 */
class SignatureProvider : ContentProvider() {

    private lateinit var signatureDao: SignatureDao
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SignatureProviderEntryPoint {
        fun signatureRepository(): SignatureRepository
    }

    override fun onCreate(): Boolean {
        val context = context ?: return false
        val entryPoint = EntryPointAccessors.fromApplication(context, SignatureProviderEntryPoint::class.java)
        signatureDao = entryPoint.signatureRepository().signatureDao
        return true
    }

    override fun shutdown() {
        super.shutdown()
        scope.cancel()
    }

    override fun getType(uri: Uri): String? {
        val signatureId = getSignatureId(uri) ?: return null
        // Verify signature exists
        val signature = runBlocking { signatureDao.getSignatureById(signatureId) }
        return if (signature != null) "image/png" else null
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        if (mode != "r") {
            throw IllegalArgumentException("Only read mode is supported")
        }

        val signatureId = getSignatureId(uri) ?: return null
        val (pipeRead, pipeWrite) = ParcelFileDescriptor.createPipe()

        scope.launch {
            try {
                FileOutputStream(pipeWrite.fileDescriptor).use { outputStream ->
                    val signature = signatureDao.getSignatureById(signatureId)
                        ?: throw IOException("Signature not found")

                    signature.imageData?.let { outputStream.write(it) }
                        ?: throw IOException("Signature image data is missing")
                }
            } catch (e: IOException) {
                Log.e("SignatureProvider", "Error writing to pipe", e)
                pipeWrite.closeWithError(e.message)
            }
        }
        return pipeRead
    }

    private fun getSignatureId(uri: Uri): String? {
        return if (uriMatcher.match(uri) == SIGNATURE_ID) {
            uri.lastPathSegment
        } else {
            null
        }
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        return null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return null
    }

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        return 0
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        return 0
    }

    companion object {
        private const val AUTHORITY = "com.android.signature.provider"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/signatures")

        private const val SIGNATURE_ID = 1
        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "signatures/*", SIGNATURE_ID)
        }
    }
}
