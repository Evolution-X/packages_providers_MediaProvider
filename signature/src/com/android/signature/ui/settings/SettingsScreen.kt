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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.android.signature.R
import com.android.signature.data.Signature
import com.android.signature.ui.common.DeleteSignatureDialog
import com.android.signature.ui.common.EmptyState
import com.android.signature.ui.common.SignatureCard

/**
 * Composable function that displays the Settings screen.
 *
 * This screen lists all saved signatures and allows the user to delete them.
 * It observes the list of signatures from the [SettingsViewModel].
 *
 * @param viewModel The [SettingsViewModel] used to manage signature data.
 * @param onNavigateUp Callback invoked when the user clicks the back button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateUp: () -> Unit
) {
    val signatures by viewModel.signatures.collectAsState()
    var signatureToDelete by remember { mutableStateOf<Signature?>(null) }

    // Show confirmation dialog when a signature is selected for deletion
    signatureToDelete?.let { signature ->
        DeleteSignatureDialog(
            onConfirm = {
                viewModel.deleteSignature(signature)
                signatureToDelete = null
            },
            onDismiss = { signatureToDelete = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.manage_signatures_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_content_description)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (signatures.isEmpty()) {
            EmptyState(modifier = Modifier.padding(paddingValues))
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .testTag("SettingsList"),
                contentPadding = PaddingValues(dimensionResource(R.dimen.padding_medium)),
                verticalArrangement = Arrangement.spacedBy(
                    dimensionResource(R.dimen.spacing_medium)
                )
            ) {
                items(
                    items = signatures,
                    key = { signature: Signature -> signature.id }
                ) { signature: Signature ->
                    SignatureCard(
                        signature = signature,
                        onDelete = { signatureToDelete = signature }
                    )
                }
            }
        }
    }
}
