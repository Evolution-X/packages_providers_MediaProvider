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

package com.android.signature.ui.common

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.android.signature.R
import com.android.signature.data.Signature
import com.android.signature.data.composeFontFamily
import com.android.signature.ui.theme.SignatureTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A composable that displays an empty state message when no signatures are available.
 *
 * @param modifier Modifier to be applied to the layout.
 * @param text The text to display. Defaults to "No signatures saved."
 */
@Composable
fun EmptyState(
    modifier: Modifier = Modifier, text: String = stringResource(R.string.no_signatures_saved)
) {
    Box(
        modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center
    ) {
        Text(
            text = text, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge
        )
    }
}

/**
 * A dialog that asks for confirmation before deleting a signature.
 *
 * @param onConfirm Callback invoked when the user confirms deletion.
 * @param onDismiss Callback invoked when the user dismisses the dialog.
 */
@Composable
fun DeleteSignatureDialog(
    onConfirm: () -> Unit, onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_signature_title)) },
        text = { Text(stringResource(R.string.delete_signature_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.delete_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_action))
            }
        })
}

/**
 * A card displaying a signature (image or text) with a delete button.
 *
 * @param signature The signature to display.
 * @param onDelete Callback invoked when the delete button is clicked.
 * @param modifier Modifier to be applied to the card.
 * @param onClick Optional callback invoked when the card is clicked.
 * @param isHighlighted Whether to show a highlight border around the card.
 */
@Composable
fun SignatureCard(
    signature: Signature,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    isHighlighted: Boolean = false
) {
    Card(
        modifier = modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        elevation = CardDefaults.cardElevation(
            defaultElevation = dimensionResource(R.dimen.card_elevation)
        ),
        border = if (isHighlighted) BorderStroke(
            dimensionResource(R.dimen.card_elevation), MaterialTheme.colorScheme.primary
        ) else null
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
                    .padding(dimensionResource(R.dimen.padding_medium)),
                contentAlignment = Alignment.Center
            ) {
                when (signature.type) {
                    Signature.TYPE_DRAWN, Signature.TYPE_UPLOADED -> {
                        signature.imageData?.let { imageData ->
                            val bitmapState =
                                produceState<ImageBitmap?>(initialValue = null, imageData) {
                                    value = withContext(Dispatchers.IO) {
                                        BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
                                            ?.asImageBitmap()
                                    }
                                }

                            bitmapState.value?.let { bitmap ->
                                Image(
                                    bitmap = bitmap, contentDescription = stringResource(
                                        R.string.drawn_signature_content_description
                                    ), modifier = Modifier.height(
                                        dimensionResource(R.dimen.signature_image_height)
                                    ), contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }

                    Signature.TYPE_TYPED -> {
                        signature.textData?.let { text ->
                            Text(
                                text = text,
                                style = MaterialTheme.typography.headlineMedium,
                                fontFamily = signature.composeFontFamily
                            )
                        }
                    }
                }
            }

            // Delete button aligned to the top end
            IconButton(
                onClick = onDelete, modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete_signature_content_description),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewEmptyState() {
    SignatureTheme {
        EmptyState()
    }
}

@Preview
@Composable
fun PreviewDeleteSignatureDialog() {
    SignatureTheme {
        DeleteSignatureDialog(onConfirm = {}, onDismiss = {})
    }
}
