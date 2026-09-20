/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.wallpaper.customization.ui.binder

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.AdaptiveIconDrawable
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.customization.model.color.ColorOptionImpl
import com.android.customization.picker.clock.ui.binder.ClockFloatingSheetBinder
import com.android.customization.picker.clock.ui.view.ClockConstraintLayoutHostView
import com.android.customization.picker.clock.ui.view.ClockConstraintLayoutHostView.Companion.addClockViews
import com.android.customization.picker.clock.ui.view.ClockViewFactory
import com.android.customization.picker.color.ui.binder.ColorOptionIconBinder
import com.android.customization.picker.color.ui.binder.ColorsFloatingSheetBinder
import com.android.customization.picker.color.ui.compose.ColorFloatingSheet
import com.android.customization.picker.color.ui.view.ColorOptionIconView
import com.android.customization.picker.color.ui.viewmodel.ColorOptionIconViewModel
import com.android.customization.picker.grid.ui.binder.GridFloatingSheetBinder
import com.android.customization.picker.icon.ui.binder.AppIconFloatingSheetBinder
import com.android.customization.picker.icon.ui.binder.ShapeIconViewBinder
import com.android.customization.picker.icon.ui.util.IconStyleViewUtil
import com.android.customization.picker.quickaffordance.ui.binder.ShortcutFloatingSheetBinder
import com.android.customization.picker.quickaffordance.ui.compose.ShortcutsFloatingSheet
import com.android.customization.picker.settings.ui.binder.ColorContrastSectionViewBinder2
import com.android.systemui.plugins.keyguard.ui.clocks.ClockAxisStyle
import com.android.themepicker.R
import com.android.wallpaper.config.BaseFlags
import com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil.ThemePickerHomeCustomizationOption
import com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil.ThemePickerLockCustomizationOption
import com.android.wallpaper.customization.ui.viewmodel.ThemePickerCustomizationOptionsData
import com.android.wallpaper.customization.ui.viewmodel.ThemePickerCustomizationOptionsViewModel
import com.android.wallpaper.picker.common.icon.ui.viewbinder.IconViewBinder
import com.android.wallpaper.picker.common.text.ui.viewbinder.TextViewBinder
import com.android.wallpaper.picker.customization.ui.binder.ColorUpdateBinder
import com.android.wallpaper.picker.customization.ui.binder.CustomizationOptionsBinder
import com.android.wallpaper.picker.customization.ui.binder.DefaultCustomizationOptionsBinder
import com.android.wallpaper.picker.customization.ui.util.CustomizationOptionUtil.CustomizationOption
import com.android.wallpaper.picker.customization.ui.util.ViewAlphaAnimator.animateToAlpha
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationOptionsData
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationOptionsViewModel
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationPickerViewModel2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch

@Singleton
class ThemePickerCustomizationOptionsBinder
@Inject
constructor(private val defaultCustomizationOptionsBinder: DefaultCustomizationOptionsBinder) :
    CustomizationOptionsBinder {

    @OptIn(FlowPreview::class)
    override fun bind(
        customizationOptionsData: CustomizationOptionsData,
        view: View,
        lockScreenCustomizationOptionEntries: List<Pair<CustomizationOption, View>>,
        homeScreenCustomizationOptionEntries: List<Pair<CustomizationOption, View>>,
        customizationOptionFloatingSheetViewMap: Map<CustomizationOption, View>?,
        viewModel: CustomizationPickerViewModel2,
        colorUpdateViewModel: ColorUpdateViewModel,
        lifecycleOwner: LifecycleOwner,
        navigateToMoreLockScreenSettingsActivity: () -> Unit,
        navigateToColorContrastSettingsActivity: () -> Unit,
        navigateToLockScreenNotificationsSettingsActivity: () -> Unit,
        navigateToPackThemeActivity: (Intent) -> Unit,
        navigateToScreenSaverSettingsActivity: () -> Unit,
        iconStyleViewUtil: IconStyleViewUtil,
    ) {
        defaultCustomizationOptionsBinder.bind(
            customizationOptionsData,
            view,
            lockScreenCustomizationOptionEntries,
            homeScreenCustomizationOptionEntries,
            customizationOptionFloatingSheetViewMap,
            viewModel,
            colorUpdateViewModel,
            lifecycleOwner,
            navigateToMoreLockScreenSettingsActivity,
            navigateToColorContrastSettingsActivity,
            navigateToLockScreenNotificationsSettingsActivity,
            navigateToPackThemeActivity,
            navigateToScreenSaverSettingsActivity,
            iconStyleViewUtil,
        )

        customizationOptionsData as ThemePickerCustomizationOptionsData

        val isComposeRefactorEnabled = BaseFlags.get(view.context).isComposeRefactorEnabled()
        val isColorPickerUpdateEnabled = BaseFlags.get(view.context).isColorPickerUpdateEnabled()
        val isColorPickerComposeEnabled = BaseFlags.get(view.context).isColorPickerComposeEnabled()

        val showPackEntry =
            Settings.Secure.getInt(
                view.context.contentResolver,
                Settings.Secure.PACK_THEME_FEATURE_ENABLED,
                /* def= */ 0,
            ) == 1

        val optionsViewModel =
            viewModel.customizationOptionsViewModel as ThemePickerCustomizationOptionsViewModel

        val isOnMainScreen = { optionsViewModel.selectedOption.value == null }

        val allCustomizationOptionEntries =
            lockScreenCustomizationOptionEntries + homeScreenCustomizationOptionEntries
        allCustomizationOptionEntries.forEach { (_, view) ->
            ColorUpdateBinder.bind(
                setColor = { color ->
                    DrawableCompat.setTint(DrawableCompat.wrap(view.background), color)
                },
                color = colorUpdateViewModel.colorSurfaceBright,
                shouldAnimate = isOnMainScreen,
                lifecycleOwner = lifecycleOwner,
            )
            ColorUpdateBinder.bind(
                setColor = { color ->
                    view
                        .findViewById<ViewGroup>(R.id.option_entry_icon_container)
                        ?.background
                        ?.let { DrawableCompat.setTint(DrawableCompat.wrap(it), color) }
                },
                color = colorUpdateViewModel.colorSurfaceContainerHigh,
                shouldAnimate = isOnMainScreen,
                lifecycleOwner = lifecycleOwner,
            )
            ColorUpdateBinder.bind(
                setColor = { color ->
                    view.findViewById<TextView>(R.id.option_entry_title)?.setTextColor(color)
                },
                color = colorUpdateViewModel.colorOnSurface,
                shouldAnimate = isOnMainScreen,
                lifecycleOwner = lifecycleOwner,
            )
            ColorUpdateBinder.bind(
                setColor = { color ->
                    view.findViewById<TextView>(R.id.option_entry_description)?.setTextColor(color)
                },
                color = colorUpdateViewModel.colorOnSurfaceVariant,
                shouldAnimate = isOnMainScreen,
                lifecycleOwner = lifecycleOwner,
            )
        }

        val optionClock: View =
            lockScreenCustomizationOptionEntries
                .first { it.first == ThemePickerLockCustomizationOption.CLOCK }
                .second
        val optionClockIcon: ImageView = optionClock.requireViewById(R.id.option_entry_icon)

        val isKeyguardQuickAffordanceEnabled =
            BaseFlags.get(view.context).isKeyguardQuickAffordanceEnabled(view.context)
        var optionShortcut: View? = null
        var optionShortcutDescription: TextView? = null
        var optionShortcutIcon1: ImageView? = null
        var optionShortcutIcon2: ImageView? = null
        if (isKeyguardQuickAffordanceEnabled) {
            optionShortcut =
                lockScreenCustomizationOptionEntries
                    .first { it.first == ThemePickerLockCustomizationOption.SHORTCUTS }
                    .second
            optionShortcutDescription =
                optionShortcut.requireViewById(R.id.option_entry_description)
            optionShortcutIcon1 = optionShortcut.requireViewById(R.id.option_entry_icon_1)
            optionShortcutIcon2 = optionShortcut.requireViewById(R.id.option_entry_icon_2)
        }

        val optionLockScreenNotificationsSettings: View =
            lockScreenCustomizationOptionEntries
                .first { it.first == ThemePickerLockCustomizationOption.LOCK_SCREEN_NOTIFICATIONS }
                .second
        optionLockScreenNotificationsSettings.setOnClickListener {
            navigateToLockScreenNotificationsSettingsActivity.invoke()
        }

        val optionMoreLockScreenSettings: View =
            lockScreenCustomizationOptionEntries
                .first { it.first == ThemePickerLockCustomizationOption.MORE_LOCK_SCREEN_SETTINGS }
                .second
        optionMoreLockScreenSettings.setOnClickListener {
            navigateToMoreLockScreenSettingsActivity.invoke()
        }

        var optionPackThemeIconHome: ImageView? = null
        var optionPackThemeIconLock: ImageView? = null
        var optionPackThemeIconHomeDefault: ImageView? = null
        var optionPackThemeIconLockDefault: ImageView? = null
        var optionPackThemeHome: View? = null
        var optionPackThemeLock: View? = null
        if (BaseFlags.get(view.context).isPackThemeEnabled() && showPackEntry) {
            optionPackThemeHome =
                homeScreenCustomizationOptionEntries
                    .first { it.first == ThemePickerHomeCustomizationOption.PACK_THEME }
                    .second
            optionPackThemeIconHomeDefault =
                optionPackThemeHome.requireViewById(R.id.option_entry_icon_default)
            optionPackThemeIconHome = optionPackThemeHome.requireViewById(R.id.option_entry_icon)

            optionPackThemeLock =
                lockScreenCustomizationOptionEntries
                    .first { it.first == ThemePickerHomeCustomizationOption.PACK_THEME }
                    .second
            optionPackThemeIconLockDefault =
                optionPackThemeLock.requireViewById(R.id.option_entry_icon_default)
            optionPackThemeIconLock = optionPackThemeLock.requireViewById(R.id.option_entry_icon)
        }

        if (BaseFlags.get(view.context).shouldShowDesktopUi(view.context)) {
            val optionScreenSaverEntry: View =
                homeScreenCustomizationOptionEntries
                    .first { it.first == ThemePickerHomeCustomizationOption.SCREEN_SAVER }
                    .second
            optionScreenSaverEntry.setOnClickListener {
                navigateToScreenSaverSettingsActivity.invoke()
            }
        }

        val optionColors: View? =
            if (customizationOptionsData.isColorCustomizationAvailable) {
                homeScreenCustomizationOptionEntries
                    .first { it.first == ThemePickerHomeCustomizationOption.COLORS }
                    .second
            } else null
        val optionColorsIcon: ColorOptionIconView? =
            optionColors?.requireViewById(R.id.option_entry_icon)

        val optionAppIcons: View? =
            if (customizationOptionsData.isIconCustomizationAvailable) {
                homeScreenCustomizationOptionEntries
                    .first { it.first == ThemePickerHomeCustomizationOption.APP_ICONS }
                    .second
            } else null
        val optionAppIconsDescription: TextView? =
            optionAppIcons?.requireViewById(R.id.option_entry_description)
        val optionAppIconsIcon: ImageView? = optionAppIcons?.requireViewById(R.id.option_entry_icon)

        var optionGrid: View? = null
        var optionGridDescription: TextView? = null
        var optionGridIcon: ImageView? = null
        if (customizationOptionsData.isGridCustomizationAvailable) {
            optionGrid =
                homeScreenCustomizationOptionEntries
                    .first { it.first == ThemePickerHomeCustomizationOption.GRID }
                    .second
            optionGridDescription = optionGrid.requireViewById(R.id.option_entry_description)
            optionGridIcon = optionGrid.requireViewById(R.id.option_entry_icon)
        }

        val optionColorContrast: View =
            homeScreenCustomizationOptionEntries
                .first { it.first == ThemePickerHomeCustomizationOption.COLOR_CONTRAST }
                .second
        optionColorContrast.setOnClickListener { navigateToColorContrastSettingsActivity.invoke() }
        val backgroundScope =
            CoroutineScope(Dispatchers.IO + Job() + CoroutineName(BACKGROUND_CONTEXT))

        ColorUpdateBinder.bind(
            setColor = { color ->
                optionClockIcon.setColorFilter(color)
                if (isKeyguardQuickAffordanceEnabled) {
                    optionShortcutIcon1?.setColorFilter(color)
                    optionShortcutIcon2?.setColorFilter(color)
                }
                if (customizationOptionsData.isGridCustomizationAvailable) {
                    optionGridIcon?.setColorFilter(color)
                }
                if (BaseFlags.get(view.context).isPackThemeEnabled()) {
                    optionPackThemeIconLockDefault?.setColorFilter(color)
                    optionPackThemeIconHomeDefault?.setColorFilter(color)
                }
            },
            color = colorUpdateViewModel.colorOnSurfaceVariant,
            shouldAnimate = isOnMainScreen,
            lifecycleOwner = lifecycleOwner,
        )

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    optionsViewModel.onCustomizeClockClicked.collect {
                        optionClock.setOnClickListener { _ -> it?.invoke() }
                    }
                }

                launch {
                    optionsViewModel.clockPickerViewModel.selectedClock.collect {
                        optionClockIcon.setImageDrawable(it.thumbnail)
                    }
                }

                if (isKeyguardQuickAffordanceEnabled) {
                    launch {
                        optionsViewModel.onCustomizeShortcutClicked.collect {
                            optionShortcut?.setOnClickListener { _ -> it?.invoke() }
                        }
                    }
                }

                if (isKeyguardQuickAffordanceEnabled) {
                    launch {
                        optionsViewModel.keyguardQuickAffordancePickerViewModel2.summary.collect {
                            summary ->
                            optionShortcutDescription?.let {
                                TextViewBinder.bind(view = it, viewModel = summary.description)
                            }
                            summary.icon1?.let { icon ->
                                optionShortcutIcon1?.let {
                                    IconViewBinder.bind(view = it, viewModel = icon)
                                }
                            }
                            optionShortcutIcon1?.isVisible = summary.icon1 != null

                            summary.icon2?.let { icon ->
                                optionShortcutIcon2?.let {
                                    IconViewBinder.bind(view = it, viewModel = icon)
                                }
                            }
                            optionShortcutIcon2?.isVisible = summary.icon2 != null
                        }
                    }
                }

                if (customizationOptionsData.isColorCustomizationAvailable) {
                    launch {
                        optionsViewModel.onCustomizeColorsClicked.collect {
                            optionColors?.setOnClickListener { _ -> it?.invoke() }
                        }
                    }
                }

                if (customizationOptionsData.isIconCustomizationAvailable) {
                    launch {
                        optionsViewModel.onCustomizeIconsClicked.collect {
                            optionAppIcons?.setOnClickListener { _ -> it?.invoke() }
                        }
                    }

                    launch {
                        var disposableHandle: DisposableHandle? = null
                        if (BaseFlags.get(view.context).isExtendibleThemeManager()) {
                            combine(
                                    optionsViewModel.appIconPickerViewModel
                                        .iconStyleAndShapeSummary,
                                    colorUpdateViewModel.systemColorsUpdated,
                                ) { summary, _ ->
                                    summary
                                }
                                .collect { summary ->
                                    disposableHandle?.dispose()
                                    optionAppIcons?.let { view ->
                                        disposableHandle =
                                            iconStyleViewUtil.bindShapeIconPreview(
                                                view = view,
                                                iconStyleModel = summary.iconStyleModel,
                                                shapeIcon = summary.iconShape,
                                                colorUpdateViewModel = colorUpdateViewModel,
                                                shouldAnimateColor = isOnMainScreen,
                                                lifecycleOwner = lifecycleOwner,
                                            )
                                    }
                                    optionAppIconsDescription?.let {
                                        TextViewBinder.bind(
                                            view = it,
                                            viewModel = summary.description,
                                        )
                                    }
                                }
                        } else {
                            val previewIconPackageName =
                                view.context.resources.getString(R.string.camera_package)
                            combine(
                                    optionsViewModel.appIconPickerViewModel
                                        .shapeAndThemedIconSummary,
                                    colorUpdateViewModel.systemColorsUpdated,
                                ) { summary, _ ->
                                    summary
                                }
                                .collect { summary ->
                                    disposableHandle?.dispose()
                                    summary.iconShape?.let {
                                        val appIconDrawable =
                                            ShapeIconViewBinder.loadAppIcon(
                                                view.context,
                                                previewIconPackageName,
                                            )
                                        disposableHandle =
                                            optionAppIconsIcon?.let { it1 ->
                                                ShapeIconViewBinder
                                                    .bindShapeAndThemedIconPreviewIcon(
                                                        view = it1,
                                                        appIconDrawable =
                                                            appIconDrawable
                                                                as? AdaptiveIconDrawable,
                                                        shapeIcon = summary.iconShape,
                                                        isThemed = summary.isThemed,
                                                        colorUpdateViewModel = colorUpdateViewModel,
                                                        shouldAnimateColor = isOnMainScreen,
                                                        lifecycleOwner = lifecycleOwner,
                                                    )
                                            }
                                    }
                                    optionAppIconsDescription?.let {
                                        TextViewBinder.bind(
                                            view = it,
                                            viewModel = summary.description,
                                        )
                                    }
                                }
                        }
                    }
                }

                if (customizationOptionsData.isGridCustomizationAvailable) {
                    launch {
                        optionsViewModel.onCustomizeShapeGridClicked.collect {
                            optionGrid?.setOnClickListener { _ -> it?.invoke() }
                        }
                    }

                    launch {
                        optionsViewModel.gridPickerViewModel.selectedGridOption.collect { gridOption
                            ->
                            optionGridDescription?.let { TextViewBinder.bind(it, gridOption.text) }
                            gridOption.payload?.let { optionGridIcon?.setImageDrawable(it) }
                        }
                    }
                }

                launch {
                    var binding: ColorContrastSectionViewBinder2.Binding? = null
                    optionsViewModel.colorContrastSectionViewModel.contrast.collectLatest { contrast
                        ->
                        binding?.destroy()
                        binding =
                            ColorContrastSectionViewBinder2.bind(
                                view = optionColorContrast,
                                contrast = contrast,
                                colorUpdateViewModel = colorUpdateViewModel,
                                shouldAnimateColor = isOnMainScreen,
                                lifecycleOwner = lifecycleOwner,
                            )
                    }
                }

                if (customizationOptionsData.isColorCustomizationAvailable) {
                    launch {
                        var binding: ColorOptionIconBinder.Binding? = null
                        optionsViewModel.colorPickerViewModel2.selectedColorOption.collect {
                            colorOption ->
                            (colorOption as? ColorOptionImpl)?.let {
                                optionColorsIcon?.let {
                                    binding?.destroy()
                                    binding =
                                        ColorOptionIconBinder.bind(
                                            view = optionColorsIcon,
                                            viewModel =
                                                ColorOptionIconViewModel.fromColorOption(
                                                    colorOption
                                                ),
                                            colorUpdateViewModel = colorUpdateViewModel,
                                            shouldAnimateColor = isOnMainScreen,
                                            lifecycleOwner = lifecycleOwner,
                                        )
                                }
                            }
                        }
                    }

                    launch {
                        combine(
                                // Sample to reduce UI color updates during freeform slider updates
                                optionsViewModel.colorPickerViewModel2.tempOverridingColorOption
                                    .sample(100),
                                optionsViewModel.colorPickerViewModel2.selectedColorOption,
                                optionsViewModel.colorPickerViewModel2.overridingStyle,
                                optionsViewModel.colorPickerViewModel2.selectedStyle,
                                optionsViewModel.darkModeViewModel.overridingIsDarkMode,
                                ::Quintuple,
                            )
                            .collect {
                                (
                                    overridingColor,
                                    selectedColor,
                                    overridingStyle,
                                    selectedStyle,
                                    overridingIsDarkMode,
                                ) ->
                                if (
                                    overridingColor != null ||
                                        overridingStyle != null ||
                                        overridingIsDarkMode != null
                                ) {
                                    val previewColorOption = overridingColor ?: selectedColor
                                    val previewIsDarkMode =
                                        overridingIsDarkMode
                                            ?: view.resources.configuration.isNightModeActive
                                    val previewStyle =
                                        optionsViewModel.colorPickerViewModel2.getPreviewingStyle(
                                            selectedStyle,
                                            overridingStyle,
                                            selectedColor,
                                            overridingColor,
                                        )
                                    previewColorOption?.let {
                                        colorUpdateViewModel.previewColors(
                                            previewColorOption.seedColor,
                                            previewStyle ?: previewColorOption.style,
                                            previewIsDarkMode,
                                        )
                                    }
                                } else colorUpdateViewModel.resetPreview()
                            }
                    }
                }

                if (BaseFlags.get(view.context).isPackThemeEnabled()) {
                    launch {
                        optionsViewModel.packThemeViewModel.packThemeData.collect { packThemeData ->
                            optionPackThemeHome?.isEnabled = packThemeData.isEnabled
                            optionPackThemeLock?.isEnabled = packThemeData.isEnabled
                            val homeTitle =
                                optionPackThemeHome?.findViewById<TextView>(R.id.option_entry_title)
                            val lockTitle =
                                optionPackThemeLock?.findViewById<TextView>(R.id.option_entry_title)
                            val homeDescription =
                                optionPackThemeHome?.findViewById<TextView>(
                                    R.id.option_entry_description
                                )
                            val lockDescription =
                                optionPackThemeLock?.findViewById<TextView>(
                                    R.id.option_entry_description
                                )
                            if (packThemeData.isEnabled) {
                                homeTitle?.alpha = 1.0f
                                lockTitle?.alpha = 1.0f
                                homeDescription?.alpha = 1.0f
                                lockDescription?.alpha = 1.0f
                            } else {
                                homeTitle?.alpha = DISABLE_TEXT_ALPHA
                                lockTitle?.alpha = DISABLE_TEXT_ALPHA
                                homeDescription?.alpha = DISABLE_TEXT_ALPHA
                                lockDescription?.alpha = DISABLE_TEXT_ALPHA
                            }
                            if (packThemeData.currentThemePackInfo.title.isNotEmpty()) {
                                homeTitle?.text = packThemeData.currentThemePackInfo.title
                                lockTitle?.text = packThemeData.currentThemePackInfo.title
                            }
                            if (packThemeData.currentThemePackInfo.description.isNotEmpty()) {
                                homeDescription?.text =
                                    packThemeData.currentThemePackInfo.description
                                lockDescription?.text =
                                    packThemeData.currentThemePackInfo.description
                            }
                            if (packThemeData.currentThemePackInfo.thumbnailUri.isNotEmpty()) {
                                val uri = packThemeData.currentThemePackInfo.thumbnailUri.toUri()
                                val corner =
                                    (THUMBNAIL_CORNER_RADIUS *
                                            view.context.resources.displayMetrics.density)
                                        .toInt()
                                optionPackThemeIconHome?.let {
                                    Glide.with(view.context)
                                        .load(uri)
                                        .transform(RoundedCorners(corner))
                                        .into(it)
                                    it.colorFilter = null
                                }
                                optionPackThemeIconLock?.let {
                                    Glide.with(view.context)
                                        .load(uri)
                                        .transform(RoundedCorners(corner))
                                        .into(it)
                                    it.colorFilter = null
                                }
                                optionPackThemeIconLockDefault?.visibility = View.GONE
                                optionPackThemeIconHomeDefault?.visibility = View.GONE
                                optionPackThemeIconLock?.visibility = View.VISIBLE
                                optionPackThemeIconHome?.visibility = View.VISIBLE
                            } else {
                                optionPackThemeIconLockDefault?.visibility = View.VISIBLE
                                optionPackThemeIconHomeDefault?.visibility = View.VISIBLE
                                optionPackThemeIconLock?.visibility = View.GONE
                                optionPackThemeIconHome?.visibility = View.GONE
                            }
                        }
                    }
                    launch {
                        optionsViewModel.packThemeViewModel.startThemePackActivityIntent.collect {
                            intent ->
                            if (intent != null) {
                                optionPackThemeHome?.setOnClickListener {
                                    backgroundScope.launch {
                                        if (isActivityAvailable(view.context, intent)) {
                                            navigateToPackThemeActivity.invoke(intent)
                                        } else {
                                            showNoPackThemeIntentErrorMessage(
                                                lifecycleOwner,
                                                view,
                                                optionsViewModel,
                                            )
                                        }
                                    }
                                }
                                optionPackThemeLock?.setOnClickListener {
                                    backgroundScope.launch {
                                        if (isActivityAvailable(view.context, intent)) {
                                            navigateToPackThemeActivity.invoke(intent)
                                        } else {
                                            showNoPackThemeIntentErrorMessage(
                                                lifecycleOwner,
                                                view,
                                                optionsViewModel,
                                            )
                                        }
                                    }
                                }
                            } else {
                                optionPackThemeHome?.setOnClickListener({
                                    showNoPackThemeIntentErrorMessage(
                                        lifecycleOwner,
                                        view,
                                        optionsViewModel,
                                    )
                                })
                                optionPackThemeLock?.setOnClickListener({
                                    showNoPackThemeIntentErrorMessage(
                                        lifecycleOwner,
                                        view,
                                        optionsViewModel,
                                    )
                                })
                            }
                        }
                    }
                }
            }
        }

        customizationOptionFloatingSheetViewMap
            ?.get(ThemePickerLockCustomizationOption.CLOCK)
            ?.let {
                ClockFloatingSheetBinder.bind(
                    it,
                    optionsViewModel,
                    colorUpdateViewModel,
                    lifecycleOwner,
                )
            }
        if (isComposeRefactorEnabled) {
            customizationOptionFloatingSheetViewMap
                ?.get(ThemePickerLockCustomizationOption.SHORTCUTS)
                ?.let {
                    // TODO(b/409112907) Evaluate Compose performance before enabling flag
                    (it as ComposeView).setContent {
                        ShortcutsFloatingSheet(
                            optionsViewModel.keyguardQuickAffordancePickerViewModel2
                        )
                    }
                }
        } else {
            customizationOptionFloatingSheetViewMap
                ?.get(ThemePickerLockCustomizationOption.SHORTCUTS)
                ?.let {
                    ShortcutFloatingSheetBinder.bind(
                        it,
                        optionsViewModel,
                        colorUpdateViewModel,
                        lifecycleOwner,
                    )
                }
        }

        if (isColorPickerUpdateEnabled && isColorPickerComposeEnabled) {
            customizationOptionFloatingSheetViewMap
                ?.get(ThemePickerHomeCustomizationOption.COLORS)
                ?.let {
                    (it as ComposeView).apply {
                        // Make sure Composable lifecycle aligns with fragment lifecycle
                        setViewCompositionStrategy(
                            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(lifecycleOwner)
                        )
                        setContent {
                            ColorFloatingSheet(
                                optionsViewModel.darkModeViewModel,
                                optionsViewModel.colorPickerViewModel2,
                            )
                        }
                    }
                }
        } else {
            customizationOptionFloatingSheetViewMap
                ?.get(ThemePickerHomeCustomizationOption.COLORS)
                ?.let {
                    ColorsFloatingSheetBinder.bind(
                        it,
                        optionsViewModel,
                        colorUpdateViewModel,
                        lifecycleOwner,
                    )
                }
        }

        customizationOptionFloatingSheetViewMap
            ?.get(ThemePickerHomeCustomizationOption.APP_ICONS)
            ?.let {
                AppIconFloatingSheetBinder.bind(
                    it,
                    optionsViewModel,
                    iconStyleViewUtil,
                    colorUpdateViewModel,
                    lifecycleOwner,
                    Dispatchers.IO,
                )
            }

        customizationOptionFloatingSheetViewMap?.get(ThemePickerHomeCustomizationOption.GRID)?.let {
            GridFloatingSheetBinder.bind(
                it,
                optionsViewModel,
                colorUpdateViewModel,
                lifecycleOwner,
                Dispatchers.IO,
            )
        }
    }

    // Track the current show clock flag. If it turns from false to true, animate fade-in.
    private var isClockCurrentlyShown: Boolean? = null

    private suspend fun isActivityAvailable(context: Context, intent: Intent): Boolean {
        val packageManager: PackageManager = context.packageManager
        val activities =
            packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return activities.isNotEmpty()
    }

    private fun showNoPackThemeIntentErrorMessage(
        lifecycleOwner: LifecycleOwner,
        view: View,
        optionsViewModel: ThemePickerCustomizationOptionsViewModel,
    ) {
        lifecycleOwner.lifecycleScope.launch {
            Toast.makeText(
                    view.context,
                    optionsViewModel.packThemeViewModel.noAppErrorMessage,
                    Toast.LENGTH_SHORT,
                )
                .show()
        }
    }

    override fun bindClockPreview(
        context: Context,
        rootView: View,
        clockHostView: View,
        clockFaceClickDelegateView: View,
        viewModel: CustomizationPickerViewModel2,
        colorUpdateViewModel: ColorUpdateViewModel,
        lifecycleOwner: LifecycleOwner,
        clockViewFactory: ClockViewFactory,
    ) {
        clockHostView as ClockConstraintLayoutHostView
        val clockPickerViewModel =
            (viewModel.customizationOptionsViewModel as ThemePickerCustomizationOptionsViewModel)
                .clockPickerViewModel

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    combine(
                            clockPickerViewModel.previewingClock,
                            clockPickerViewModel.previewingClockSize,
                            clockPickerViewModel.showPickerClockControllerView,
                            ::Triple,
                        )
                        .collect { (clock, size, showClock) ->
                            clockHostView.removeAllViews()
                            if (showClock) {
                                clockViewFactory.getController(clock.clockId)?.run {
                                    val cs = ConstraintSet()
                                    clockHostView.addClockViews(this, size, cs)
                                    val cfg = clockPickerViewModel.buildPreviewConfig(context)
                                    largeClock.layout.applyPreviewConstraints(cfg, cs)
                                    smallClock.layout.applyPreviewConstraints(cfg, cs)
                                    cs.applyTo(clockHostView)
                                }
                                clockViewFactory.updateTimeFormat(clock.clockId)
                            }
                            val shouldFadeIn = (isClockCurrentlyShown == false) && showClock
                            if (shouldFadeIn) {
                                clockHostView.alpha = 0F
                                clockHostView.animateToAlpha(1F)
                            }
                            isClockCurrentlyShown = showClock
                        }
                }

                launch {
                    combine(
                            clockPickerViewModel.previewingSeedColor,
                            clockPickerViewModel.previewingClock,
                            clockPickerViewModel.previewingClockPresetIndexedStyle,
                            colorUpdateViewModel.systemColorsUpdated,
                            ::Quadruple,
                        )
                        .collect { quadruple ->
                            val (color, clock, clockPresetIndexedStyle, _) = quadruple
                            clockViewFactory.updateColor(clock.clockId, color)
                            clockViewFactory.updateFontAxes(
                                clock.clockId,
                                clockPresetIndexedStyle?.style ?: ClockAxisStyle(),
                            )
                        }
                }

                launch {
                    combine(
                            viewModel.customizationOptionsViewModel.selectedOption,
                            clockPickerViewModel.onClockFaceClicked,
                            clockPickerViewModel.previewingClockPresetIndexedStyle,
                            ::Triple,
                        )
                        .collect { (selectedOption, onClockFaceClicked, clockPresetIndexedStyle) ->
                            if (
                                selectedOption == ThemePickerLockCustomizationOption.CLOCK &&
                                    onClockFaceClicked != null
                            ) {
                                clockFaceClickDelegateView.isVisible = true
                                clockFaceClickDelegateView.setOnClickListener {
                                    onClockFaceClicked.invoke()
                                }
                            } else {
                                clockFaceClickDelegateView.isVisible = false
                                clockFaceClickDelegateView.setOnClickListener(null)
                            }
                            val clockStyle =
                                if (clockPresetIndexedStyle?.groupIndex == 0) {
                                    context.getString(R.string.clock_style_round_clock)
                                } else {
                                    context.getString(R.string.clock_style_sharp_clock)
                                }
                            clockFaceClickDelegateView.contentDescription =
                                context.getString(
                                    R.string.change_clock_style_content_description,
                                    clockStyle,
                                )
                        }
                }
            }
        }
    }

    override fun bindDiscardChangesDialog(
        customizationOptionsViewModel: CustomizationOptionsViewModel,
        lifecycleOwner: LifecycleOwner,
        activity: Activity,
    ) {
        defaultCustomizationOptionsBinder.bindDiscardChangesDialog(
            customizationOptionsViewModel,
            lifecycleOwner,
            activity,
        )
    }

    data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    data class Quintuple<A, B, C, D, E>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D,
        val fifth: E,
    )

    companion object {
        private const val THUMBNAIL_CORNER_RADIUS = 18
        private const val DISABLE_TEXT_ALPHA = 0.38f
        private const val BACKGROUND_CONTEXT = "backgroundContext"
    }
}
