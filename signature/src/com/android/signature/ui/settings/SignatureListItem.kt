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

package com.android.signature.ui.settings

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.android.signature.R
import com.android.signature.data.Signature
import com.android.signature.data.composeFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Composable function that displays a single signature item in the list.
 *
 * @param signature The [Signature] object to display.
 * @param onDelete Callback invoked when the delete button is clicked.
 * @param modifier Modifier to be applied to the root layout.
 */
@Composable
fun SignatureListItem(
    signature: Signature, onDelete: () -> Unit, modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier, elevation = CardDefaults.cardElevation(
            defaultElevation = dimensionResource(R.dimen.card_elevation)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
        ) {
            // Box for the signature content, centered
            Box(
                modifier = Modifier.fillMaxSize().padding(
                    horizontal = dimensionResource(R.dimen.padding_medium),
                    vertical = dimensionResource(R.dimen.padding_small)
                ), contentAlignment = Alignment.Center
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
                    Icons.Default.Delete, contentDescription = stringResource(
                        R.string.delete_signature_content_description
                    ), tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
