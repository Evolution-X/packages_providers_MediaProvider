/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.photopicker.data.model

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.ui.graphics.vector.ImageVector
import com.android.photopicker.core.glide.GlideLoadable
import com.android.photopicker.core.glide.ParcelableGlideLoadable
import com.android.photopicker.core.glide.Resolution
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.signature.ObjectKey

sealed class Icon() : Parcelable {
    companion object {
        operator fun invoke(uri: Uri, mediaSource: MediaSource): GlideIcon {
            return GlideIcon(uri, mediaSource)
        }

        operator fun invoke(imageVector: ImageVector): VectorIcon {
            return VectorIcon(imageVector)
        }
    }
}

/**
 * An icon is a simple object which points to a media resource can be loaded by [Glide] because it
 * implements the [GlideLoadable] interface.
 */
data class GlideIcon(val uri: Uri, val mediaSource: MediaSource) : Icon(), ParcelableGlideLoadable {
    override fun getSignature(resolution: Resolution): ObjectKey {
        return ObjectKey("${uri}_$resolution")
    }

    override fun getLoadableUri(): Uri {
        return uri
    }

    override fun getDataSource(): DataSource {
        return when (mediaSource) {
            MediaSource.LOCAL -> DataSource.LOCAL
            MediaSource.REMOTE -> DataSource.REMOTE
        }
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
        out.writeString(uri.toString())
        out.writeString(mediaSource.name)
    }

    companion object CREATOR : Parcelable.Creator<GlideIcon> {

        override fun createFromParcel(parcel: Parcel): GlideIcon {
            return GlideIcon(
                uri = Uri.parse(parcel.readString() ?: ""),
                mediaSource = MediaSource.valueOf(parcel.readString() ?: "LOCAL"),
            )
        }

        override fun newArray(size: Int): Array<GlideIcon?> {
            return arrayOfNulls(size)
        }
    }
}

/**
 * An icon that is represented by an in-memory [ImageVector]. Since [ImageVector] is not parcelable,
 * this class relies on a static map to serialize and deserialize the object by name.
 */
data class VectorIcon(val imageVector: ImageVector) : Icon() {

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
        out.writeString(imageVector.name)
    }

    companion object CREATOR : Parcelable.Creator<VectorIcon> {

        override fun createFromParcel(parcel: Parcel): VectorIcon? {
            val imageName = parcel.readString()
            return when (imageName) {
                Icons.Outlined.FolderCopy.name -> VectorIcon(Icons.Outlined.FolderCopy)
                else -> return null
            }
        }

        override fun newArray(size: Int): Array<VectorIcon?> {
            return arrayOfNulls(size)
        }
    }
}
