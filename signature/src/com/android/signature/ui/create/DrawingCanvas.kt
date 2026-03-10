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

package com.android.signature.ui.create

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.android.signature.ui.util.createPathFromPoints

/**
 * A composable that provides a drawing canvas for the user to draw their signature.
 *
 * @param paths The list of existing paths to draw.
 * @param onDragStart Callback invoked when the user starts dragging.
 * @param onDragEnd Callback invoked when the user stops dragging. It provides the [PathState] if a path was created, or null otherwise.
 * @param strokeWidth The width of the stroke.
 * @param color The color of the stroke.
 */
@Composable
internal fun DrawingCanvas(
    paths: List<PathState>,
    onDragStart: () -> Unit = {},
    onDragEnd: (PathState?) -> Unit,
    strokeWidth: Float,
    color: Color = Color.Black,
) {
    val currentPoints = remember { mutableStateListOf<Offset>() }

    Canvas(
        modifier =
            Modifier.fillMaxSize().pointerInput(Unit) {
                detectDragGestures(onDragStart = { offset ->
                    onDragStart()
                    currentPoints.clear()
                    currentPoints.add(offset)
                }, onDrag = { change, _ ->
                    currentPoints.add(change.position)
                }, onDragEnd = {
                    val pathState =
                        if (currentPoints.isNotEmpty()) {
                            val path = createPathFromPoints(currentPoints)
                            PathState(
                                path = path,
                                color = color,
                                strokeWidth = strokeWidth,
                                points = currentPoints.toList(),
                            )
                        } else {
                            null
                        }
                    currentPoints.clear()
                    onDragEnd(pathState)
                })
            },
    ) {
        // Draw existing paths directly without transforming bounds
        paths.forEach { pathState ->
            drawPath(
                path = pathState.path,
                color = pathState.color,
                style =
                    Stroke(
                        width = pathState.strokeWidth,
                        cap = SignatureStrokeStyle.cap,
                        join = SignatureStrokeStyle.join,
                    ),
            )
        }

        // Draw current points (the active stroke being drawn)
        if (currentPoints.isNotEmpty()) {
            drawPoints(
                points = currentPoints,
                pointMode = PointMode.Polygon,
                color = color,
                strokeWidth = strokeWidth,
                cap = SignatureStrokeStyle.cap,
            )
        }
    }
}
