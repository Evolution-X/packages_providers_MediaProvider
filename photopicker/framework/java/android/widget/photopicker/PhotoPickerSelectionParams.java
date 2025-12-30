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

import java.util.Locale;

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
    private final long mMaxVideoDurationInSeconds;
    private final long mMinVideoDurationInSeconds;

    private PhotoPickerSelectionParams(
            long maxMediaItemSizeInBytes,
            long maxVideoDurationInSeconds,
            long minVideoDurationInSeconds
    ) {
        mMaxMediaItemSizeInBytes = maxMediaItemSizeInBytes;
        mMaxVideoDurationInSeconds = maxVideoDurationInSeconds;
        mMinVideoDurationInSeconds = minVideoDurationInSeconds;
    }

    /**
     * Reconstructs this object from a Parcel, maintaining the order in which fields
     * were written.
     */
    private PhotoPickerSelectionParams(Parcel in) {
        mMaxMediaItemSizeInBytes = in.readLong();
        mMaxVideoDurationInSeconds = in.readLong();
        mMinVideoDurationInSeconds = in.readLong();
    }


    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeLong(mMaxMediaItemSizeInBytes);
        dest.writeLong(mMaxVideoDurationInSeconds);
        dest.writeLong(mMinVideoDurationInSeconds);
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
     * Returns the maximum allowed duration, in seconds, for a video to be selectable.
     *
     * <p>If the maximum video duration is not set by the caller app using {@link
     * Builder#setMaxVideoDurationInSeconds(long)}, this method returns -1, indicating that the
     * photo picker will not restrict selection based on the maximum video duration.
     */
    public long getMaxVideoDurationInSeconds() {
        return mMaxVideoDurationInSeconds;
    }

    /**
     * Returns the minimum required duration, in seconds, for a video to be selectable.
     *
     * <p>If the minimum video duration is not set by the caller app using {@link
     * Builder#setMinVideoDurationInSeconds(long)}, this method returns -1, indicating that the
     * photo picker will not restrict selection based on the minimum video duration.
     */
    public long getMinVideoDurationInSeconds() {
        return mMinVideoDurationInSeconds;
    }

    /**
     * A builder class used to construct and validate an immutable
     * {@link PhotoPickerSelectionParams} object.
     */
    public static final class Builder {

        private long mMaxMediaItemSizeInBytes = -1;
        private long mMaxVideoDurationInSeconds = -1;
        private long mMinVideoDurationInSeconds = -1;

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
         * Sets the maximum allowed duration, in seconds, for a video media item to be selectable.
         *
         * <p>Videos exceeding this duration will be disabled for selection.
         *
         * <p>The maximum duration should not be less than minimum duration, or an
         * exception will be thrown during the {@link #build()} process.
         *
         * <p>If it is not set, no maximum video duration constraint will be enforced on the videos
         * that the user can select.
         *
         * @param maxVideoDurationInSeconds The maximum video duration in seconds.
         * @throws IllegalArgumentException if {@code maxVideoDurationInSeconds} is negative or zero
         */
        @NonNull
        public Builder setMaxVideoDurationInSeconds(long maxVideoDurationInSeconds) {
            if (maxVideoDurationInSeconds <= 0) {
                throw new IllegalArgumentException(
                        "Maximum video duration cannot be negative or zero.");
            }
            mMaxVideoDurationInSeconds = maxVideoDurationInSeconds;
            return this;
        }

        /**
         * Clears the maximum video duration constraint.
         *
         * <p>On calling this, the PhotoPicker will not enforce an upper limit on the duration of
         * video media items.
         *
         * @see #setMaxVideoDurationInSeconds(long)
         */
        @NonNull
        public Builder clearMaxVideoDuration() {
            mMaxVideoDurationInSeconds = -1;
            return this;
        }

        /**
         * Sets the minimum allowed duration, in seconds, for a video media item to be selectable.
         *
         * <p>Videos shorter than this duration will be disabled for selection.
         *
         * <p>The maximum duration should not be less than minimum duration, or an
         * exception will be thrown during the {@link #build()} process.
         *
         * <p>If it is not set, no minimum video duration constraint will be enforced on the videos
         * that the user can select.
         *
         * @param minVideoDurationInSeconds The minimum video duration in seconds.
         * @throws IllegalArgumentException if {@code minVideoDurationInSeconds} is negative
         */
        @NonNull
        public Builder setMinVideoDurationInSeconds(long minVideoDurationInSeconds) {
            if (minVideoDurationInSeconds < 0) {
                throw new IllegalArgumentException("Minimum video duration cannot be negative.");
            }
            mMinVideoDurationInSeconds = minVideoDurationInSeconds;
            return this;
        }

        /**
         * Clears the minimum video duration constraint.
         *
         * <p>On calling this, the PhotoPicker will not enforce a lower limit on the duration of
         * video media items.
         *
         * @see #setMinVideoDurationInSeconds(long)
         */
        @NonNull
        public Builder clearMinVideoDuration() {
            mMinVideoDurationInSeconds = -1;
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
            validateMinMax(mMinVideoDurationInSeconds, mMaxVideoDurationInSeconds,
                    "video duration");

            return new PhotoPickerSelectionParams(
                    mMaxMediaItemSizeInBytes,
                    mMaxVideoDurationInSeconds,
                    mMinVideoDurationInSeconds);
        }

        /**
         * Internal helper to perform validation, ensuring that a minimum value does not
         * exceed its corresponding maximum value.
         *
         * @param minValue minimum value of a set of params
         * @param maxValue maximum value of a set of params
         * @param param the set of param whose minimum and maximum values are being validated
         */
        private void validateMinMax(long minValue, long maxValue, @NonNull String param) {
            if (minValue != -1 && maxValue != -1 && minValue > maxValue) {
                throw new IllegalArgumentException(String.format(
                        Locale.ROOT,
                        "Min %s cannot be greater than the max %s.",
                        param, param));
            }
        }
    }
}
