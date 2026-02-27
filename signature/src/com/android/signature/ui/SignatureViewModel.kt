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

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.signature.data.Signature
import com.android.signature.data.SignatureFont
import com.android.signature.data.SignatureRepository
import com.android.signature.ui.create.PathState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.ByteArrayOutputStream
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
        private const val COMPRESSION_QUALITY = 100
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

    // UI State for CreateSignatureScreen
    private val _selectedTabIndex = MutableStateFlow(0)

    /**
     * The currently selected tab index in the Create Signature screen.
     */
    val selectedTabIndex: StateFlow<Int> = _selectedTabIndex.asStateFlow()

    private val _drawingPaths = MutableStateFlow<List<PathState>>(emptyList())

    /**
     * The current list of drawing paths in the Draw tab.
     */
    val drawingPaths: StateFlow<List<PathState>> = _drawingPaths.asStateFlow()

    private val _typedText = MutableStateFlow("")

    /**
     * The current text entered in the Type tab.
     */
    val typedText: StateFlow<String> = _typedText.asStateFlow()

    private val _selectedFont = MutableStateFlow<SignatureFont?>(null)

    /**
     * The currently selected font in the Type tab.
     */
    val selectedFont: StateFlow<SignatureFont?> = _selectedFont.asStateFlow()

    // UI State for SignaturePickerScreen
    private val _newSignatureId = MutableStateFlow<String?>(null)

    /**
     * The ID of a newly created signature, used to highlight and scroll to it in the picker.
     */
    val newSignatureId: StateFlow<String?> = _newSignatureId.asStateFlow()

    private val _signatureToDelete = MutableStateFlow<Signature?>(null)

    /**
     * The signature currently selected for deletion, triggering the confirmation dialog.
     */
    val signatureToDelete: StateFlow<Signature?> = _signatureToDelete.asStateFlow()

    /**
     * Sets the selected tab index.
     */
    fun setSelectedTabIndex(index: Int) {
        _selectedTabIndex.value = index
    }

    /**
     * Sets the drawing paths.
     */
    fun setDrawingPaths(paths: List<PathState>) {
        _drawingPaths.value = paths
    }

    /**
     * Sets the typed text.
     */
    fun setTypedText(text: String) {
        _typedText.value = text
        // Deselect font on text change to force re-selection if needed, or keep it.
        // The original UI logic cleared it.
        _selectedFont.value = null
    }

    /**
     * Sets the selected font.
     */
    fun setSelectedFont(font: SignatureFont) {
        _selectedFont.value = font
    }

    /**
     * Clears the state related to signature creation.
     */
    fun clearCreateSignatureState() {
        _selectedTabIndex.value = 0
        _drawingPaths.value = emptyList()
        _typedText.value = ""
        _selectedFont.value = null
    }

    /**
     * Sets the ID of the newly created signature.
     */
    fun setNewSignatureId(id: String?) {
        _newSignatureId.value = id
    }

    /**
     * Sets the signature to be deleted.
     */
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

    /**
     * Saves a drawn signature.
     *
     * @param bitmap The bitmap of the drawn signature.
     * @return The saved [Signature].
     */
    suspend fun saveDrawnSignature(bitmap: Bitmap): Signature {
        checkSignatureLimit()
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, COMPRESSION_QUALITY, stream)

        val paths = _drawingPaths.value
        val serializedPaths = if (paths.isNotEmpty()) {
            paths.joinToString("|") { pathState ->
                pathState.points.joinToString(";") { "${it.x},${it.y}" }
            }
        } else null

        val signature = Signature(
            type = Signature.TYPE_DRAWN,
            imageData = stream.toByteArray(),
            drawingPaths = serializedPaths
        )
        repository.saveSignature(signature)
        return signature
    }

    /**
     * Saves a typed signature.
     *
     * @param text The text entered.
     * @param fontName The name of the font used.
     * @param bitmap The bitmap of the typed signature.
     * @return The saved [Signature].
     */
    suspend fun saveTypedSignature(
        text: String,
        fontName: String,
        bitmap: Bitmap? = null
    ): Signature {
        checkSignatureLimit()
        val imageData = bitmap?.let {
            val stream = ByteArrayOutputStream()
            it.compress(Bitmap.CompressFormat.PNG, COMPRESSION_QUALITY, stream)
            stream.toByteArray()
        }

        val signature = Signature(
            type = Signature.TYPE_TYPED, textData = text, fontName = fontName, imageData = imageData
        )
        repository.saveSignature(signature)
        return signature
    }

    /**
     * Saves an uploaded signature.
     *
     * @param bitmap The uploaded bitmap.
     * @return The saved [Signature].
     */
    suspend fun saveUploadedSignature(bitmap: Bitmap): Signature {
        checkSignatureLimit()
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, COMPRESSION_QUALITY, stream)
        val signature = Signature(
            type = Signature.TYPE_UPLOADED, imageData = stream.toByteArray()
        )
        repository.saveSignature(signature)
        return signature
    }

    private suspend fun checkSignatureLimit() {
        val count = repository.getSignatureCount()
        if (count >= MAX_SIGNATURES) {
            throw IllegalStateException("Maximum number of signatures ($MAX_SIGNATURES) reached.")
        }
    }
}
