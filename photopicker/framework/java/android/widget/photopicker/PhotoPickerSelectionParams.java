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

    private PhotoPickerSelectionParams() {}

    /**
     * Reconstructs this object from a Parcel, maintaining the order in which fields
     * were written.
     */
    private PhotoPickerSelectionParams(Parcel in) {}


    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {}

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
     * A builder class used to construct and validate an immutable
     * {@link PhotoPickerSelectionParams} object.
     */
    public static final class Builder {

        public Builder() {}

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
         * Builds a new immutable {@link PhotoPickerSelectionParams} object.
         *
         * @return A new {@link PhotoPickerSelectionParams} object with the configured properties.
         * @throws IllegalArgumentException if any of the minimum values are greater than their
         *                                  corresponding maximum values.
         */
        @NonNull
        public PhotoPickerSelectionParams build() {

            return new PhotoPickerSelectionParams();
        }
    }
}
