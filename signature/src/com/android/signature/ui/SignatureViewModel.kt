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

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.signature.data.Signature
import com.android.signature.data.SignatureRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel to handle the business logic for creating, reading, and deleting signatures.
 *
 * @param repository The repository for accessing signature data.
 */
@HiltViewModel
class SignatureViewModel @Inject constructor(
    private val repository: SignatureRepository
) : ViewModel() {

    companion object {
        const val MAX_SIGNATURES = 5
        private const val PARAM_TYPE = "type"
        private const val PARAM_TEXT = "text"
        private const val PARAM_FONT = "font"
        private const val PARAM_PATHS = "paths"
    }

    /**
     * A stateful flow that holds the current list of all saved signatures.
     * It is collected by the UI to display the signatures.
     */
    val signatures: StateFlow<List<Signature>> = repository.getAllSignatures().stateIn(
            scope = viewModelScope,
            // Keep the flow active for 5 seconds after the last collector disappears.
            started = SharingStarted.WhileSubscribed(5000L), initialValue = emptyList()
        )

    // UI State for SignaturePickerScreen
    private val _newSignatureId = MutableStateFlow<String?>(null)
    val newSignatureId: StateFlow<String?> = _newSignatureId.asStateFlow()

    private val _signatureToDelete = MutableStateFlow<Signature?>(null)
    val signatureToDelete: StateFlow<Signature?> = _signatureToDelete.asStateFlow()

    fun setNewSignatureId(id: String?) {
        _newSignatureId.value = id
    }

    fun setSignatureToDelete(signature: Signature?) {
        _signatureToDelete.value = signature
    }

    /**
     * Deletes a signature from the repository.
     */
    fun deleteSignature(signature: Signature) {
        viewModelScope.launch {
            repository.deleteSignature(signature)
        }
    }

    /**
     * Gets a shareable content URI for a given signature.
     *
     * This method delegates to the [SignatureRepository] to construct a URI
     * that can be resolved by the app's content provider. This allows
     * secure sharing of signature data without exposing the underlying database
     * or creating temporary files.
     *
     * @param signature The signature for which to create a URI.
     * @return A content [Uri] that points to the signature data.
     */
    fun getSignatureUri(signature: Signature): Uri {
        var uri = repository.getSignatureUri(signature)
        val builder = uri.buildUpon()

        builder.appendQueryParameter(PARAM_TYPE, signature.type.toString())

        if (signature.type == Signature.TYPE_TYPED) {
            signature.textData?.let { builder.appendQueryParameter(PARAM_TEXT, it) }
            signature.fontName?.let { builder.appendQueryParameter(PARAM_FONT, it) }
        } else if (signature.type == Signature.TYPE_DRAWN) {
            signature.drawingPaths?.let { builder.appendQueryParameter(PARAM_PATHS, it) }
        }

        return builder.build()
    }
}
