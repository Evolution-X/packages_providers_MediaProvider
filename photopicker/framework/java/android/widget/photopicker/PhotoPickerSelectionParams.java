/*
 * Copyright (C) 2026 The Android Open Source Project
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

package android.widget.photopicker;

import android.annotation.FlaggedApi;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.android.providers.media.flags.Flags;

/**
 * An immutable parcel that carries constraints to be applied to media items displayed in the
 * Photo Picker.
 *
 * <p>Media items that fail to satisfy these constraints will be disabled for selection.
 *
 * <p>Callers should use {@link Builder} to construct an instance of this class.
 */
@FlaggedApi(Flags.FLAG_ENABLE_PHOTOPICKER_SELECTION_PARAMS_API)
public final class PhotoPickerSelectionParams implements Parcelable {
    private static final String TAG = "PhotoPickerSelectionParams";

    private final long mMaxMediaItemSizeInBytes;

    private PhotoPickerSelectionParams(
            long maxMediaItemSizeInBytes
    ) {
        mMaxMediaItemSizeInBytes = maxMediaItemSizeInBytes;
    }

    /**
     * Reconstructs this object from a Parcel, maintaining the order in which fields
     * were written.
     */
    private PhotoPickerSelectionParams(Parcel in) {
        mMaxMediaItemSizeInBytes = in.readLong();
    }


    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeLong(mMaxMediaItemSizeInBytes);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @NonNull
    public static final Creator<PhotoPickerSelectionParams> CREATOR =
            new Creator<PhotoPickerSelectionParams>() {
                @Override
                public PhotoPickerSelectionParams createFromParcel(Parcel in) {
                    return new PhotoPickerSelectionParams(in);
                }

                @Override
                public PhotoPickerSelectionParams[] newArray(int size) {
                    return new PhotoPickerSelectionParams[size];
                }
            };

    /**
     * Returns the maximum allowed size, in bytes, for a media item to be selectable.
     *
     * <p>If the maximum media item size is not set by the caller app using {@link
     * Builder#setMaxMediaItemSizeInBytes(long)}, this method returns -1, indicating that the
     * photo picker will not restrict selection based on the maximum media item size.
     */
    public long getMaxMediaItemSizeInBytes() {
        return mMaxMediaItemSizeInBytes;
    }

    /**
     * A builder class used to construct and validate an immutable
     * {@link PhotoPickerSelectionParams} object.
     */
    public static final class Builder {

        private long mMaxMediaItemSizeInBytes = -1;

        public Builder() {
        }

        /**
         * Sets the maximum allowed size, in bytes, for any individual media item to be selectable.
         *
         * <p>The calling application can set this to limit the size of media returned by the
         * PhotoPicker. Items exceeding this limit will be disabled for selection.
         *
         * <p>If it is not set, no maximum size constraint will be enforced on the media items that
         * the user can select.
         *
         * @param maxMediaItemSizeInBytes The maximum size in bytes.
         * @throws IllegalArgumentException if {@code maxMediaItemSizeInBytes} is negative or zero
         */
        @NonNull
        public Builder setMaxMediaItemSizeInBytes(long maxMediaItemSizeInBytes) {
            if (maxMediaItemSizeInBytes <= 0) {
                throw new IllegalArgumentException(
                        "Maximum media item size cannot be negative or zero.");
            }
            mMaxMediaItemSizeInBytes = maxMediaItemSizeInBytes;
            return this;
        }

        /**
         * Clears the maximum media item size constraint.
         *
         * <p>On calling this, the PhotoPicker will not enforce an upper limit on the size of
         * individual media items.
         *
         * @see #setMaxMediaItemSizeInBytes(long)
         */
        @NonNull
        public Builder clearMaxMediaItemSize() {
            mMaxMediaItemSizeInBytes = -1;
            return this;
        }

        /**
         * Builds a new immutable {@link PhotoPickerSelectionParams} object.
         *
         * @return A new {@link PhotoPickerSelectionParams} object with the configured properties.
         * @throws IllegalArgumentException if any of the minimum values are greater than their
         *                                  corresponding maximum values.
         */
        @NonNull
        public PhotoPickerSelectionParams build() {

            return new PhotoPickerSelectionParams(
                    mMaxMediaItemSizeInBytes);
        }
    }
}
