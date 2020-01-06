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
package com.android.customization.model.clock;

import android.content.ContentResolver;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.Log;

import com.android.customization.module.ThemesUserEventLogger;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * {@link CustomizationManager} for clock faces that implements apply by writing to secure settings.
 */
public class ClockManager extends BaseClockManager {

    private static final String CLOCK_FACE_SETTING = Secure.LOCK_SCREEN_CUSTOM_CLOCK_FACE;
    private static final String CLOCK_FIELD = "clock";
    private static final String TIMESTAMP_FIELD = "_applied_timestamp";
    private static final String TAG = "ClockManager";

    private final ContentResolver mContentResolver;
    private final ThemesUserEventLogger mEventLogger;

    public ClockManager(ContentResolver resolver, ClockProvider provider,
            ThemesUserEventLogger logger) {
        super(provider);
        mContentResolver = resolver;
        mEventLogger = logger;
    }

    @Override
    protected void handleApply(Clockface option, Callback callback) {
        String value = toJSON(option.getId());
        if (value == null) {
            callback.onError(null);
        } else {
            if (Secure.putString(mContentResolver, CLOCK_FACE_SETTING, value)) {
                mEventLogger.logClockApplied(option);
                callback.onSuccess();
            } else {
                callback.onError(null);
            }
        }
    }

    @Override
    protected String lookUpCurrentClock() {
        String value = Secure.getString(mContentResolver, CLOCK_FACE_SETTING);
        if (!TextUtils.isEmpty(value)) {
            return fromJSON(value);
        }
        return null;
    }

    private String toJSON(String value) {
        try {
            JSONObject json = new JSONObject();
            json.put(CLOCK_FIELD, value);
            json.put(TIMESTAMP_FIELD, System.currentTimeMillis());
            return json.toString();
        } catch (JSONException ex) {
            Log.e(TAG, "Failed migrating settings value to JSON format", ex);
        }
        return null;
    }

    private String fromJSON(String value) {
        JSONObject json;
        try {
            json = new JSONObject(value);
        } catch (JSONException ex) {
            Log.e(TAG, "Settings value is not valid JSON", ex);
            return value;
        }
        try {
            return json.getString(CLOCK_FIELD);
        } catch (JSONException ex) {
            Log.e(TAG, "JSON object does not contain clock field.", ex);
            return null;
        }
    }
}
