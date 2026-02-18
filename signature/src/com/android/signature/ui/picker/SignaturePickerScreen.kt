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

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.android.signature.R
import com.android.signature.data.Signature
import com.android.signature.ui.SignatureViewModel
import com.android.signature.ui.common.DeleteSignatureDialog
import com.android.signature.ui.common.EmptyState
import com.android.signature.ui.common.SignatureCard

/**
 * Composable function that displays the Signature Picker screen.
 *
 * This screen is designed to be shown within a bottom sheet. It lists available signatures,
 * allows adding new ones, and handles selection and deletion.
 *
 * @param viewModel The [SignatureViewModel] used to manage signature data.
 * @param onAddSignature Callback invoked when the "Add New" button is clicked.
 * @param onSignatureSelected Callback invoked when a signature is selected.
 * @param onCancel Callback invoked when the cancel action is triggered (though currently unused in UI).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignaturePickerScreen(
    viewModel: SignatureViewModel,
    onAddSignature: () -> Unit,
    onSignatureSelected: (Signature, Uri) -> Unit,
    onCancel: () -> Unit
) {
    val signatures by viewModel.signatures.collectAsState()
    val signatureToDelete by viewModel.signatureToDelete.collectAsState()

    val listState = rememberLazyListState()

    // Confirmation dialog for deletion.
    signatureToDelete?.let { signature ->
        DeleteSignatureDialog(
            onConfirm = {
                viewModel.deleteSignature(signature)
                viewModel.setSignatureToDelete(null)
            },
            onDismiss = { viewModel.setSignatureToDelete(null) }
        )
    }

    // The main layout is a Column, suitable for a bottom sheet.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // Add padding to respect the system navigation bar.
            .navigationBarsPadding()
            .padding(bottom = dimensionResource(R.dimen.padding_medium))
    ) {
        // A custom header for the bottom sheet.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimensionResource(R.dimen.padding_medium),
                    vertical = dimensionResource(R.dimen.padding_small)
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.picker_title),
                style = MaterialTheme.typography.titleLarge
            )
            TextButton(onClick = onAddSignature) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.picker_add_new),
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.picker_add_new))
            }
        }

        // The main content area with a constrained height.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = dimensionResource(R.dimen.bottom_sheet_max_height)) // Constrain height for scrollable content.
        ) {
            if (signatures.isEmpty()) {
                EmptyState(
                    modifier = Modifier.padding(dimensionResource(R.dimen.padding_medium)),
                    text = stringResource(R.string.picker_no_signatures)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.testTag("SignaturePickerList"),
                    state = listState, // Assign the state to control scrolling
                    contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.padding_medium)),
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
                ) {
                    items(signatures, key = { it.id }) { signature ->
                        SignatureCard(
                            signature = signature,
                            onClick = {
                                val uri = viewModel.getSignatureUri(signature)
                                onSignatureSelected(signature, uri)
                            },
                            onDelete = {
                                viewModel.setSignatureToDelete(signature)
                            },
                            modifier = androidx.compose.ui.Modifier.height(dimensionResource(R.dimen.signature_item_height))
                        )
                    }
                }
            }
        }
    }
}
