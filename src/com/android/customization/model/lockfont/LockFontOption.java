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

import android.app.WallpaperManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.PorterDuff.Mode;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.android.customization.model.CustomizationManager;
import com.android.customization.model.CustomizationOption;
import com.android.customization.model.ResourceConstants;
import com.android.customization.model.theme.OverlayManagerCompat;
import com.android.themepicker.R;
import com.android.wallpaper.util.ResourceUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class LockFontOption implements CustomizationOption<LockFontOption> {

    private final Typeface mHeadlineFont;
    private final Typeface mBodyFont;
    private String mTitle;
    private String mOverlayPackage;

    public LockFontOption(String overlayPackage, String label, Typeface headlineFont, Typeface bodyFont) {
        mTitle = label;
        mHeadlineFont = headlineFont;
        mBodyFont = bodyFont;
        mOverlayPackage = overlayPackage;
    }

    @Override
    public void bindThumbnailTile(View view) {
        Resources res = view.getContext().getResources();
        TextView thumbnailText = view.findViewById(R.id.thumbnail_text);

        thumbnailText.setTypeface(mHeadlineFont);
        
        int colorFilter = ResourceUtils.getColorAttr(view.getContext(),
                        view.isActivated() 
                        ? (view.getId() == R.id.font_section_tile 
                                ? android.R.attr.textColorPrimary 
                                : android.R.attr.textColorPrimaryInverse)
                        : android.R.attr.textColorTertiary);
        
        thumbnailText.setTextColor(colorFilter);
        if (view.isActivated()) {
            view.setBackgroundTintList(ColorStateList.valueOf(
                ResourceUtils.getColorAttr(view.getContext(), android.R.attr.colorAccent)));
        } else {
            view.setBackgroundTintList(null);
        }

        view.setContentDescription(mTitle);
    }


    @Override
    public boolean isActive(CustomizationManager<LockFontOption> manager) {
        LockFontManager fontManager = (LockFontManager) manager;
        return fontManager.isActive(this);
    }

    @Override
    public int getLayoutResId() {
        return R.layout.theme_font_option;
    }

    @Override
    public String getTitle() {
        return mTitle;
    }

    public String getPackageName() {
        return mOverlayPackage;
    }

    public void bindPreview(ViewGroup container) {
        ViewGroup cardBody = container.findViewById(R.id.theme_preview_card_body_container);
        CardView cardView = container.findViewById(R.id.font_preview_card);

        if (cardBody.getChildCount() == 0) {
            LayoutInflater.from(container.getContext()).inflate(
                    R.layout.preview_card_lockfont_content, cardBody, true);
        }

        WallpaperManager wallpaperManager = WallpaperManager.getInstance(container.getContext());
        Drawable wallpaperDrawable = wallpaperManager.getDrawable();

        BitmapDrawable bitmapDrawable = (BitmapDrawable) wallpaperDrawable;
        Bitmap bitmap = bitmapDrawable.getBitmap();

        RoundedBitmapDrawable roundedDrawable = RoundedBitmapDrawableFactory.create(container.getResources(), bitmap);
        roundedDrawable.setCornerRadius(32);

        cardView.setBackgroundColor(Color.TRANSPARENT);
        cardView.setBackground(roundedDrawable);

        TextView title = container.findViewById(R.id.font_card_title);
        title.setTypeface(mHeadlineFont);
    }

}
