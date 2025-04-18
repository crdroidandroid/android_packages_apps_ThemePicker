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
import static com.android.customization.model.ResourceConstants.OVERLAY_CATEGORY_LOCKFONT;

import android.app.AlertDialog;
import android.content.Context;
import android.content.ContentResolver;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.android.customization.model.CustomizationManager;
import com.android.customization.model.theme.OverlayManagerCompat;

import java.util.Map;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

public class LockFontManager implements CustomizationManager<LockFontOption> {

    private static LockFontManager sLockFontOptionManager;
    private Context mContext;
    private LockFontOption mActiveOption;
    private OverlayManagerCompat mOverlayManager;
    private LockFontOptionProvider mProvider;
    private static final String TAG = "LockFontManager";
    private static final String KEY_STATE_CURRENT_SELECTION = "LockFontManager.currentSelection";

    LockFontManager(Context context, OverlayManagerCompat overlayManager, LockFontOptionProvider provider) {
        mContext = context;
        mProvider = provider;
        mOverlayManager = overlayManager;
    }

    @Override
    public boolean isAvailable() {
        return mOverlayManager.isAvailable();
    }

    @Override
    public void apply(LockFontOption option, @Nullable Callback callback) {
        if (!persistOverlay(option)) {
            Toast failed = Toast.makeText(mContext, "Failed to apply lockscreen font, reboot to try again.", Toast.LENGTH_SHORT);
            failed.show();
            if (callback != null) {
                callback.onError(null);
            }
            return;
        }
        if (option.getPackageName() == null) {
            if (mActiveOption.getPackageName() == null) return;
            for (String overlay : mOverlayManager.getOverlayPackagesForCategory(
                    OVERLAY_CATEGORY_LOCKFONT, UserHandle.myUserId(), ANDROID_PACKAGE)) {
                mOverlayManager.disableOverlay(overlay, UserHandle.myUserId());
            }
        } else {
            mOverlayManager.setEnabledExclusiveInCategory(option.getPackageName(), UserHandle.myUserId());
        }
        if (callback != null) {
            callback.onSuccess();
        }
        mActiveOption = option;
    }

    @Override
    public void fetchOptions(OptionsFetchedListener<LockFontOption> callback, boolean reload) {
        List<LockFontOption> options = mProvider.getOptions(reload);
        for (LockFontOption option : options) {
            if (isActive(option)) {
                mActiveOption = option;
                break;
            }
        }
        callback.onOptionsLoaded(options);
    }

    public OverlayManagerCompat getOverlayManager() {
        return mOverlayManager;
    }

    public boolean isActive(LockFontOption option) {
        String enabledPkg = mOverlayManager.getEnabledPackageName(ANDROID_PACKAGE, OVERLAY_CATEGORY_LOCKFONT);
        if (enabledPkg != null) {
            return enabledPkg.equals(option.getPackageName());
        } else {
            return option.getPackageName() == null;
        }
    }

    public void restartSystemUI() {
        ContentResolver resolver = mContext.getContentResolver();
        int currentValue = Settings.System.getInt(resolver, "system_ui_restart", 0);
        int newValue = (currentValue == 0) ? 1 : 0;
        Settings.System.putInt(resolver, "system_ui_restart", newValue);
    }

    private boolean persistOverlay(LockFontOption toPersist) {
        String value = Settings.Secure.getStringForUser(mContext.getContentResolver(),
                Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES, UserHandle.myUserId());
        JSONObject json;
        if (value == null) {
            json = new JSONObject();
        } else {
            try {
                json = new JSONObject(value);
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing current settings value:\n" + e.getMessage());
                return false;
            }
        }
        // removing all currently enabled overlays from the json
        json.remove(OVERLAY_CATEGORY_LOCKFONT);
        // adding the new ones
        try {
            json.put(OVERLAY_CATEGORY_LOCKFONT, toPersist.getPackageName());
        } catch (JSONException e) {
            Log.e(TAG, "Error adding new settings value:\n" + e.getMessage());
            return false;
        }
        // updating the setting
        Settings.Secure.putStringForUser(mContext.getContentResolver(),
                Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES,
                json.toString(), UserHandle.myUserId());
        // TODO: remove this until i find a way to rebuild keyguard blueprint views during theme changed
        restartSystemUI();
        return true;
    }

    public static LockFontManager getInstance(Context context, OverlayManagerCompat overlayManager) {
        if (sLockFontOptionManager == null) {
            Context applicationContext = context.getApplicationContext();
            sLockFontOptionManager = new LockFontManager(context, overlayManager, new LockFontOptionProvider(applicationContext, overlayManager));
        }
        return sLockFontOptionManager;
    }

}
