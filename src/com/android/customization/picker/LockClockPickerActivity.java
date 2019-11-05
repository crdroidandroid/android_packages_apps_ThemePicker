/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.customization.picker;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.android.customization.model.clock.Clockface;
import com.android.customization.model.clock.ClockManager;
import com.android.customization.model.clock.ContentProviderClockProvider;
import com.android.customization.module.CustomizationInjector;
import com.android.customization.module.ThemesUserEventLogger;
import com.android.customization.picker.clock.ClockFragment;
import com.android.customization.picker.clock.ClockFragment.ClockFragmentHost;
import com.android.wallpaper.module.InjectorProvider;
import com.android.wallpaper.R;

/**
 * Activity allowing for the clock face picker to be linked to from other setup flows.
 *
 * This should be used with startActivityForResult. The resulting intent contains an extra
 * "clock_face_name" with the id of the picked clock face.
 */
public class LockClockPickerActivity extends FragmentActivity implements ClockFragmentHost {

    private ClockManager mClockManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clock_face_picker);

        CustomizationInjector injector = (CustomizationInjector) InjectorProvider.getInjector();
        ThemesUserEventLogger eventLogger = (ThemesUserEventLogger) injector.getUserEventLogger(
                this);

        mClockManager = new ClockManager(getContentResolver(),
                new ContentProviderClockProvider(this), eventLogger);
        if (!mClockManager.isAvailable()) {
            finish();
        } else {
            final FragmentManager fm = getSupportFragmentManager();
            final FragmentTransaction fragmentTransaction = fm.beginTransaction();
            final ClockFragment clockFragment = ClockFragment.newInstance(
                    getString(R.string.clock_title));
            fragmentTransaction.replace(R.id.fragment_container, clockFragment);
            fragmentTransaction.commitNow();
        }
    }

    @Override
    public ClockManager getClockManager() {
        return mClockManager;
    }
}
