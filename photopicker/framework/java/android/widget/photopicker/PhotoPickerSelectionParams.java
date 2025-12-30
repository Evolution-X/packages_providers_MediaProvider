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
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.providers.media.flags.Flags;

import java.util.ArrayList;
import java.util.List;
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
    private final long mMaxMediaItemResolutionInPixels;
    private final long mMinMediaItemResolutionInPixels;
    private final List<String> mMimeTypes;

    private PhotoPickerSelectionParams(
            long maxMediaItemSizeInBytes,
            long maxVideoDurationInSeconds,
            long minVideoDurationInSeconds,
            long maxMediaItemResolutionInPixels,
            long minMediaItemResolutionInPixels,
            List<String> mimeTypes
    ) {
        mMaxMediaItemSizeInBytes = maxMediaItemSizeInBytes;
        mMaxVideoDurationInSeconds = maxVideoDurationInSeconds;
        mMinVideoDurationInSeconds = minVideoDurationInSeconds;
        mMaxMediaItemResolutionInPixels = maxMediaItemResolutionInPixels;
        mMinMediaItemResolutionInPixels = minMediaItemResolutionInPixels;
        mMimeTypes = List.copyOf(mimeTypes);
    }

    /**
     * Reconstructs this object from a Parcel, maintaining the order in which fields
     * were written.
     */
    private PhotoPickerSelectionParams(Parcel in) {
        mMaxMediaItemSizeInBytes = in.readLong();
        mMaxVideoDurationInSeconds = in.readLong();
        mMinVideoDurationInSeconds = in.readLong();
        mMaxMediaItemResolutionInPixels = in.readLong();
        mMinMediaItemResolutionInPixels = in.readLong();
        List<String> mimeTypes = new ArrayList<>();
        in.readStringList(mimeTypes);
        mMimeTypes = mimeTypes;
    }


    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeLong(mMaxMediaItemSizeInBytes);
        dest.writeLong(mMaxVideoDurationInSeconds);
        dest.writeLong(mMinVideoDurationInSeconds);
        dest.writeLong(mMaxMediaItemResolutionInPixels);
        dest.writeLong(mMinMediaItemResolutionInPixels);
        dest.writeStringList(mMimeTypes);
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
     * Returns the maximum allowed resolution, in pixels, for a media item to be selectable.
     *
     * <p>If the maximum resolution is not set by the caller app using {@link
     * Builder#setMaxMediaItemResolutionInPixels(long)}, this method returns -1, indicating that the
     * photo picker will not restrict selection based on the maximum media item resolution.
     */
    public long getMaxMediaItemResolutionInPixels() {
        return mMaxMediaItemResolutionInPixels;
    }

    /**
     * Returns the minimum required resolution, in pixels, for a media item to be selectable.
     *
     * <p>If the minimum resolution is not set by the caller app using {@link
     * Builder#setMinMediaItemResolutionInPixels(long)}, this method returns -1, indicating that the
     * photo picker will not restrict selection based on the minimum media item resolution.
     */
    public long getMinMediaItemResolutionInPixels() {
        return mMinMediaItemResolutionInPixels;
    }

    /**
     * Returns the list of allowed MIME types that media items must match to be selectable.
     *
     * <p>If the allowed MIME types are not set by the caller app using {@link
     * Builder#setMimeTypes(List)}, this method returns an empty list, indicating that the
     * photo picker will not restrict selection based on the MIME type.
     */
    @NonNull
    public List<String> getMimeTypes() {
        return mMimeTypes;
    }

    /**
     * A builder class used to construct and validate an immutable
     * {@link PhotoPickerSelectionParams} object.
     */
    public static final class Builder {

        private long mMaxMediaItemSizeInBytes = -1;
        private long mMaxVideoDurationInSeconds = -1;
        private long mMinVideoDurationInSeconds = -1;
        private long mMaxMediaItemResolutionInPixels = -1;
        private long mMinMediaItemResolutionInPixels = -1;
        private List<String> mMimeTypes = new ArrayList<>();

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
         * Sets the maximum allowed resolution constraint for a media item to be selectable.
         *
         * <p>Items exceeding the provided resolution will be disabled for selection.
         *
         * <p>The maximum resolution should not be less than the minimum resolution, or an
         * exception will be thrown during the {@link #build()} process.
         *
         * <p>If it is not set, no maximum resolution constraint will be enforced on the media items
         * that the user can select.
         *
         * @param maxMediaItemResolutionInPixels The maximum media item resolution in pixels
         * @throws IllegalArgumentException if {@code maxMediaItemResolutionInPixels} is negative or
         *                                  zero
         */
        @NonNull
        public Builder setMaxMediaItemResolutionInPixels(long maxMediaItemResolutionInPixels) {
            if (maxMediaItemResolutionInPixels <= 0) {
                throw new IllegalArgumentException(
                        "Maximum media item resolution cannot be negative or zero.");
            }
            mMaxMediaItemResolutionInPixels = maxMediaItemResolutionInPixels;
            return this;
        }

        /**
         * Clears the maximum media item resolution constraint.
         *
         * <p>On calling this, the PhotoPicker will not enforce an upper limit on the total
         * resolution of individual media items.
         *
         * @see #setMaxMediaItemResolutionInPixels(long)
         */
        @NonNull
        public Builder clearMaxMediaItemResolution() {
            mMaxMediaItemResolutionInPixels = -1;
            return this;
        }

        /**
         * Sets the minimum allowed resolution constraint for a media item to be selectable.
         *
         * <p>Media items with a resolution lower than the provided threshold will be disabled for
         * selection.
         *
         * <p>The minimum resolution should not be greater than the maximum resolution, or an
         * exception will be thrown during the {@link #build()} process.
         *
         * <p>If it is not set, no minimum resolution constraint will be enforced on the media items
         * that the user can select.
         *
         * @param minMediaItemResolutionInPixels The minimum allowed resolution in total pixels.
         * @throws IllegalArgumentException if {@code minMediaItemResolutionInPixels} is negative.
         */
        @NonNull
        public Builder setMinMediaItemResolutionInPixels(long minMediaItemResolutionInPixels) {
            if (minMediaItemResolutionInPixels < 0) {
                throw new IllegalArgumentException(
                        "Minimum media item resolution cannot be negative");
            }
            mMinMediaItemResolutionInPixels = minMediaItemResolutionInPixels;
            return this;
        }

        /**
         * Clears the minimum media item resolution constraint.
         *
         * <p>On calling this, the PhotoPicker will not enforce a lower limit on the total
         * resolution of individual media items.
         *
         * @see #setMinMediaItemResolutionInPixels(long)
         */
        @NonNull
        public Builder clearMinMediaItemResolution() {
            mMinMediaItemResolutionInPixels = -1;
            return this;
        }

        /**
         * Sets the list of MIME types that are allowed for selection.
         *
         * <p>Media items that violate this constraint will be disabled for selection.
         *
         * <p>This parameter is different from the MIME types which can be specified in {@link
         * android.content.Intent#setType(String)} extra or {@link
         * android.content.Intent#EXTRA_MIME_TYPES}, when those are used to launch the photo picker,
         * they will filter out any media items which has a MIME type not added to them. While the
         * MIME Types defined by this API will still exist in photo picker media grid, but disabled
         * from selection.
         *
         * <p>Filter media items using the MIME Types defined in {@link
         * android.content.Intent#setType(String)} extra or {@link
         * android.content.Intent#EXTRA_MIME_TYPES} will happen first, before disabling the media
         * items based on the MIME Types passed to this API.
         *
         * <p>Callers must indicate the acceptable document MIME types. For example, to select
         * photos, use {@code image/*}.
         *
         * <p>If it is not set, no MIME type constraint will be enforced on the media items the user
         * can select.
         *
         * @param mimeTypes The list of allowed MIME types.
         * @throws IllegalArgumentException if {@code mimeTypes} is null, empty, or contains
         *                                  non-media types (types not starting with "image/" or
         *                                  "video/").
         */
        @NonNull
        public Builder setMimeTypes(@NonNull List<String> mimeTypes) {
            if (!validateMimeType(mimeTypes)) {
                throw new IllegalArgumentException("MimeTypes list must not be null or empty, "
                        + "and must only contain valid 'image/' or 'video/' types.");
            }
            mMimeTypes = new ArrayList<>(mimeTypes);
            return this;
        }

        /**
         * Clears the MIME type constraint.
         *
         * <p>On calling this, the builder will revert to its default state of allowing all
         * supported media MIME types (images and videos) for selection.
         *
         * @see #setMimeTypes(List)
         */
        @NonNull
        public Builder clearMimeTypes() {
            mMimeTypes.clear();
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
            validateMinMax(mMinMediaItemResolutionInPixels, mMaxMediaItemResolutionInPixels,
                    "media item resolution");

            return new PhotoPickerSelectionParams(
                    mMaxMediaItemSizeInBytes,
                    mMaxVideoDurationInSeconds,
                    mMinVideoDurationInSeconds,
                    mMaxMediaItemResolutionInPixels,
                    mMinMediaItemResolutionInPixels,
                    mMimeTypes);
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

        /**
         * Internal helper to perform validation, ensuring that the MIME types list is not null
         * or empty, and each entry is a valid media type.
         *
         * @param mimeTypes the list of MIME types to be validated
         * @return {@code true} if the list is valid, {@code false} otherwise
         */
        private boolean validateMimeType(List<String> mimeTypes) {
            if (mimeTypes == null) {
                Log.e(TAG,
                        "Mime type list must not be null. MIME type constraint will not be "
                                + "applied.");
                return false;
            }
            if (mimeTypes.isEmpty()) {
                Log.e(TAG, "Empty mime type list found. MIME type constraint will not be applied.");
                return false;
            }
            for (String mimeType : mimeTypes) {
                if (mimeType == null) {
                    Log.e(TAG, "Mime type must not be null. "
                            + "MIME type constraint will not be applied.");
                    return false;
                }
                if (!isMimeTypeMedia(mimeType)) {
                    Log.e(TAG, "Invalid mime type found. Only image/video mime types are "
                            + "supported. MIME type constraint will not be applied.");
                    return false;
                }
            }
            return true;
        }

        /**
         * Checks if the given string is an image or video mime type
         */
        private static boolean isMimeTypeMedia(@NonNull String mimeType) {
            return mimeType.toLowerCase(Locale.getDefault()).startsWith("image/")
                    || mimeType.toLowerCase(Locale.getDefault()).startsWith("video/");
        }
    }
}
