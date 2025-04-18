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
package com.android.customization.model.lockfont;

import static com.android.customization.model.ResourceConstants.ANDROID_PACKAGE;
import static com.android.customization.model.ResourceConstants.CONFIG_CLOCK_FONT_FAMILY;
import static com.android.customization.model.ResourceConstants.OVERLAY_CATEGORY_LOCKFONT;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.Typeface;
import android.os.UserHandle;
import android.util.Log;

import com.android.customization.model.ResourceConstants;
import com.android.customization.model.theme.OverlayManagerCompat;
import com.android.themepicker.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LockFontOptionProvider {

    private static final String TAG = "LockFontOptionProvider";

    private Context mContext;
    private PackageManager mPm;
    private final List<String> mOverlayPackages;
    private final List<LockFontOption> mOptions = new ArrayList<>();
    private String mActiveOverlay;

    public LockFontOptionProvider(Context context, OverlayManagerCompat manager) {
        mContext = context;
        mPm = context.getPackageManager();
        mOverlayPackages = new ArrayList<>();
        mOverlayPackages.addAll(manager.getOverlayPackagesForCategory(OVERLAY_CATEGORY_LOCKFONT,
                UserHandle.myUserId(), ResourceConstants.getPackagesToOverlay(mContext)));
        mActiveOverlay = manager.getEnabledPackageName(ANDROID_PACKAGE, OVERLAY_CATEGORY_LOCKFONT);
    }

    public List<LockFontOption> getOptions(boolean reload) {
        if (reload) mOptions.clear();
        if (mOptions.isEmpty()) loadOptions();
        return mOptions;
    }

    private void loadOptions() {
        addDefault();
        List<LockFontOption> customOptions = new ArrayList<>();

        for (String overlayPackage : mOverlayPackages) {
            try {
                Resources overlayRes = mPm.getResourcesForApplication(overlayPackage);
                Typeface headlineFont = Typeface.create(
                        getFontFamily(overlayPackage, overlayRes, CONFIG_CLOCK_FONT_FAMILY),
                        Typeface.NORMAL);
                Typeface bodyFont = Typeface.create(
                        getFontFamily(overlayPackage, overlayRes, CONFIG_CLOCK_FONT_FAMILY),
                        Typeface.NORMAL);
                String label = mPm.getApplicationInfo(overlayPackage, 0).loadLabel(mPm).toString();
                customOptions.add(new LockFontOption(overlayPackage, label, headlineFont, bodyFont));
            } catch (NameNotFoundException | NotFoundException e) {
                Log.w(TAG, String.format("Couldn't load lockscreen font overlay %s, will skip it",
                        overlayPackage), e);
            }
        }

        customOptions.sort((o1, o2) -> o1.getTitle().compareToIgnoreCase(o2.getTitle()));
        mOptions.addAll(customOptions);
    }

    private void addDefault() {
        Resources system = Resources.getSystem();
        Typeface headlineFont = Typeface.create(system.getString(system.getIdentifier(
                ResourceConstants.CONFIG_CLOCK_FONT_FAMILY,"string", ANDROID_PACKAGE)),
                Typeface.NORMAL);
        Typeface bodyFont = Typeface.create(system.getString(system.getIdentifier(
                ResourceConstants.CONFIG_CLOCK_FONT_FAMILY,
                "string", ANDROID_PACKAGE)),
                Typeface.NORMAL);
        mOptions.add(new LockFontOption(null, mContext.getString(R.string.default_theme_title),
                headlineFont, bodyFont));
    }

    private String getFontFamily(String overlayPackage, Resources overlayRes, String configName) {
        return overlayRes.getString(overlayRes.getIdentifier(configName, "string", overlayPackage));
    }
}
