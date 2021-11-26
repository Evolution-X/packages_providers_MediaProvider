/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.providers.media.photopicker.espresso;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static com.android.providers.media.photopicker.espresso.RecyclerViewTestUtils.longClickItem;

import static com.google.common.truth.Truth.assertThat;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.appcompat.widget.Toolbar;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.android.providers.media.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4ClassRunner.class)
public class PreviewSingleSelectTest extends PhotoPickerBaseTest {

    @Rule
    public ActivityScenarioRule<PhotoPickerTestActivity> mRule
            = new ActivityScenarioRule<>(PhotoPickerBaseTest.getSingleSelectionIntent());

    @Test
    public void testPreview_singleSelect_image() {
        onView(withId(PICKER_TAB_RECYCLERVIEW_ID)).check(matches(isDisplayed()));

        // Navigate to preview
        longClickItem(PICKER_TAB_RECYCLERVIEW_ID, IMAGE_1_POSITION, ICON_THUMBNAIL_ID);

        registerIdlingResourceAndWaitForIdle();

        // No dragBar in preview
        onView(withId(DRAG_BAR_ID)).check(matches(not(isDisplayed())));

        // Verify image is previewed
        assertSingleSelectCommonLayoutMatches();
        onView(withId(R.id.preview_imageView)).check(matches(isDisplayed()));
        // Verify no special format icon is previewed
        onView(withId(PREVIEW_MOTION_PHOTO_ID)).check(doesNotExist());
        onView(withId(PREVIEW_GIF_ID)).check(doesNotExist());

        // Navigate back to Photo grid
        onView(withContentDescription("Navigate up")).perform(click());

        onView(withId(PICKER_TAB_RECYCLERVIEW_ID)).check(matches(isDisplayed()));
        // Shows dragBar after we are back to Photos tab
        onView(withId(DRAG_BAR_ID)).check(matches(isDisplayed()));
    }

    @Test
    public void testPreview_singleSelect_video() {
        onView(withId(PICKER_TAB_RECYCLERVIEW_ID)).check(matches(isDisplayed()));

        // Navigate to preview
        longClickItem(PICKER_TAB_RECYCLERVIEW_ID, VIDEO_POSITION, ICON_THUMBNAIL_ID);

        registerIdlingResourceAndWaitForIdle();

        // Since there is no video in the video file, we get an error.
        onView(withText(android.R.string.ok)).perform(click());

        // Verify videoView is displayed
        assertSingleSelectCommonLayoutMatches();
        onView(withId(R.id.preview_videoView)).check(matches(isDisplayed()));
        // Verify no special format icon is previewed
        onView(withId(PREVIEW_MOTION_PHOTO_ID)).check(doesNotExist());
        onView(withId(PREVIEW_GIF_ID)).check(doesNotExist());
    }

    @Test
    public void testPreview_singleSelect_fromAlbumsPhoto() {
        // Navigate to Albums tab
        onView(allOf(withText(PICKER_ALBUMS_STRING_ID), withParent(withId(CHIP_CONTAINER_ID))))
                .perform(click());

        final int cameraStringId = R.string.picker_category_camera;
        // Navigate to photos in Camera album
        onView(allOf(withText(cameraStringId),
                isDescendantOfA(withId(PICKER_TAB_RECYCLERVIEW_ID)))).perform(click());

        // Verify that toolbar has the title as category name Camera
        onView(allOf(withText(cameraStringId), withParent(withId(R.id.toolbar))))
                .check(matches(isDisplayed()));

        // Navigate to preview
        longClickItem(PICKER_TAB_RECYCLERVIEW_ID, /* position */ 1, ICON_THUMBNAIL_ID);

        registerIdlingResourceAndWaitForIdle();

        // Verify image is previewed
        assertSingleSelectCommonLayoutMatches();
        onView(withId(R.id.preview_imageView)).check(matches(isDisplayed()));

        // Navigate back to Camera album
        onView(withContentDescription("Navigate up")).perform(click());

        // Verify that toolbar has the title as category name Camera
        onView(allOf(withText(cameraStringId), withParent(withId(R.id.toolbar))))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testPreview_noScrimLayerAndHasSolidColorInPortrait() {
        mRule.getScenario().onActivity(activity -> {
            activity.setRequestedOrientation(SCREEN_ORIENTATION_PORTRAIT);
        });

        mRule.getScenario().onActivity(activity -> {
            assertThat(activity.getResources().getConfiguration().orientation).isEqualTo(
                    Configuration.ORIENTATION_PORTRAIT);
        });

        onView(withId(PICKER_TAB_RECYCLERVIEW_ID)).check(matches(isDisplayed()));
        // Navigate to preview
        longClickItem(PICKER_TAB_RECYCLERVIEW_ID, IMAGE_1_POSITION, ICON_THUMBNAIL_ID);

        registerIdlingResourceAndWaitForIdle();

        onView(withId(R.id.preview_top_scrim)).check(matches(not(isDisplayed())));
        onView(withId(R.id.preview_bottom_scrim)).check(matches(not(isDisplayed())));

        mRule.getScenario().onActivity(activity -> {
            assertBackgroundColorOnToolbarAndBottomBar(activity, R.color.preview_scrim_solid_color);
        });
    }

    @Test
    public void testPreview_showScrimLayerInLandscape() {
        mRule.getScenario().onActivity(activity -> {
            activity.setRequestedOrientation(SCREEN_ORIENTATION_LANDSCAPE);
        });

        mRule.getScenario().onActivity(activity -> {
            assertThat(activity.getResources().getConfiguration().orientation).isEqualTo(
                    Configuration.ORIENTATION_LANDSCAPE);
        });

        onView(withId(PICKER_TAB_RECYCLERVIEW_ID)).check(matches(isDisplayed()));

        // Navigate to preview
        longClickItem(PICKER_TAB_RECYCLERVIEW_ID, IMAGE_1_POSITION, ICON_THUMBNAIL_ID);

        registerIdlingResourceAndWaitForIdle();

        onView(withId(R.id.preview_top_scrim)).check(matches(isDisplayed()));
        onView(withId(R.id.preview_bottom_scrim)).check(matches(isDisplayed()));

        mRule.getScenario().onActivity(activity -> {
            assertBackgroundColorOnToolbarAndBottomBar(activity, android.R.color.transparent);
        });
    }

    private void registerIdlingResourceAndWaitForIdle() {
        mRule.getScenario().onActivity((activity -> IdlingRegistry.getInstance().register(
                new ViewPager2IdlingResource(activity.findViewById(R.id.preview_viewPager)))));
        Espresso.onIdle();
    }

    private void assertBackgroundColorOnToolbarAndBottomBar(Activity activity, int colorResId) {
        final Toolbar toolbar = activity.findViewById(R.id.toolbar);
        final Drawable toolbarDrawable = toolbar.getBackground();

        assertThat(toolbarDrawable).isInstanceOf(ColorDrawable.class);

        final int expectedColor = activity.getColor(colorResId);
        assertThat(((ColorDrawable) toolbarDrawable).getColor()).isEqualTo(expectedColor);

        final View bottomBar = activity.findViewById(R.id.preview_bottom_bar);
        final Drawable bottomBarDrawable = bottomBar.getBackground();

        assertThat(bottomBarDrawable).isInstanceOf(ColorDrawable.class);
        assertThat(((ColorDrawable) bottomBarDrawable).getColor()).isEqualTo(expectedColor);
    }

    private void assertSingleSelectCommonLayoutMatches() {
        onView(withId(R.id.preview_viewPager)).check(matches(isDisplayed()));
        onView(withId(R.id.preview_select_check_button)).check(matches(not(isDisplayed())));
        onView(withId(R.id.preview_add_or_select_button)).check(matches(isDisplayed()));
        // Verify that the text in Add button
        onView(withId(R.id.preview_add_or_select_button)).check(matches(withText(R.string.add)));
    }
}