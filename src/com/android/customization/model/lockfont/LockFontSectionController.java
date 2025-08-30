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
package com.android.customization.model.lockfont;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.android.customization.model.CustomizationManager.Callback;
import com.android.customization.model.CustomizationManager.OptionsFetchedListener;
import com.android.customization.picker.lockfont.LockFontFragment;
import com.android.customization.picker.lockfont.LockFontSectionView;
import com.android.themepicker.R;
import com.android.wallpaper.model.CustomizationSectionController;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/** A {@link CustomizationSectionController} for lockscreen fonts. */

public class LockFontSectionController implements CustomizationSectionController<LockFontSectionView> {

    private static final String TAG = "LockFontSectionController";
    private static final String KEY_LOCK_SCREEN_CUSTOM_CLOCK_FACE = "lock_screen_custom_clock_face";

    private final LockFontManager mFontOptionsManager;
    private final CustomizationSectionNavigationController mSectionNavigationController;
    private final Callback mApplyFontCallback = new Callback() {
        @Override
        public void onSuccess() {
        }

        @Override
        public void onError(@Nullable Throwable throwable) {
        }
    };

    public LockFontSectionController(LockFontManager fontOptionsManager,
            CustomizationSectionNavigationController sectionNavigationController) {
        mFontOptionsManager = fontOptionsManager;
        mSectionNavigationController = sectionNavigationController;
    }

    @Override
    public boolean isAvailable(Context context) {
        return mFontOptionsManager.isAvailable();
    }

    @Override
    public LockFontSectionView createView(Context context) {
        LockFontSectionView fontSectionView = (LockFontSectionView) LayoutInflater.from(context)
                .inflate(R.layout.lockfont_section_view, /* root= */ null);

        TextView sectionDescription = fontSectionView.findViewById(R.id.font_section_description);
        View sectionTile = fontSectionView.findViewById(R.id.font_section_tile);

        mFontOptionsManager.fetchOptions(new OptionsFetchedListener<LockFontOption>() {
            @Override
            public void onOptionsLoaded(List<LockFontOption> options) {
                LockFontOption activeOption = getActiveOption(options);
                sectionDescription.setText(activeOption.getTitle());
                activeOption.bindThumbnailTile(sectionTile);
            }

            @Override
            public void onError(@Nullable Throwable throwable) {
                if (throwable != null) {
                    Log.e(TAG, "Error loading font options", throwable);
                }
                sectionDescription.setText(R.string.something_went_wrong);
                sectionTile.setVisibility(View.GONE);
            }
        }, /* reload= */ true);

        fontSectionView.setOnClickListener(v -> {
            if (fontSectionView.isEnabled()) {
                mSectionNavigationController.navigateTo(
                        LockFontFragment.newInstance(
                                context.getString(R.string.preview_name_lockfont)));
            }
        });

        updateEnabledStateFromClockFace(context, fontSectionView);

        final Uri clockFaceUri = Settings.Secure.getUriFor(KEY_LOCK_SCREEN_CUSTOM_CLOCK_FACE);
        final ContentObserver observer = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                updateEnabledStateFromClockFace(context, fontSectionView);
            }
        };

        context.getContentResolver().registerContentObserver(clockFaceUri, /* notifyForDescendants= */ false, observer);

        fontSectionView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                updateEnabledStateFromClockFace(context, fontSectionView);
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                try {
                    context.getContentResolver().unregisterContentObserver(observer);
                } catch (Throwable t) {
                    Log.w(TAG, "Failed to unregister ContentObserver", t);
                }
            }
        });

        return fontSectionView;
    }

    private LockFontOption getActiveOption(List<LockFontOption> options) {
        return options.stream()
                .filter(option -> mFontOptionsManager.isActive(option))
                .findAny()
                // For development only, as there should always be a grid set.
                .orElse(options.get(0));
    }

    /**
     * Enables the section when no custom clock is set (or clockId is DEFAULT),
     * otherwise disables it. This is called initially and whenever the setting changes.
     */
    private void updateEnabledStateFromClockFace(Context context, View sectionView) {
        String clockFaceJson = Settings.Secure.getString(
                context.getContentResolver(), KEY_LOCK_SCREEN_CUSTOM_CLOCK_FACE);

        boolean enable;
        if (TextUtils.isEmpty(clockFaceJson)) {
            enable = true;
        } else {
            enable = isDefaultClock(clockFaceJson);
        }

        sectionView.setEnabled(enable);
        sectionView.setClickable(enable);
        sectionView.setFocusable(enable);
        sectionView.setAlpha(enable ? 1f : 0.5f);
    }

    private boolean isDefaultClock(String clockFaceJson) {
        try {
            JSONObject clockFace = new JSONObject(clockFaceJson);
            String id = clockFace.optString("clockId", "DEFAULT");
            return "DEFAULT".equals(id);
        } catch (JSONException e) {
            Log.w(TAG, "Failed to parse clock face JSON: " + clockFaceJson, e);
            return false;
        }
    }
}
