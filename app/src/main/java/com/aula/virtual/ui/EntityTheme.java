package com.aula.virtual.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;

public class EntityTheme {

    @NonNull
    public static Context wrapContext(@NonNull Context baseContext, @NonNull EntityType type) {
        return new ContextThemeWrapper(baseContext, type.getThemeOverlayRes());
    }

    public static void applyToButton(@NonNull MaterialButton button, @NonNull EntityType type, boolean outlined) {
        int accent = type.getAccentColor(button.getContext());
        int soft = type.getSoftColor(button.getContext());
        int onAccent = type.getOnAccentColor(button.getContext());

        if (outlined) {
            button.setStrokeColor(ColorStateList.valueOf(accent));
            button.setTextColor(accent);
            button.setIconTint(ColorStateList.valueOf(accent));
            button.setBackgroundTintList(ColorStateList.valueOf(soft));
        } else {
            button.setBackgroundTintList(ColorStateList.valueOf(accent));
            button.setTextColor(onAccent);
            button.setIconTint(ColorStateList.valueOf(onAccent));
        }
    }

    public static void applyToChip(@NonNull TextView chipOrBadge, @NonNull EntityType type) {
        int accent = type.getAccentColor(chipOrBadge.getContext());
        int soft = type.getSoftColor(chipOrBadge.getContext());

        chipOrBadge.setTextColor(accent);
        chipOrBadge.setBackgroundTintList(ColorStateList.valueOf(soft));
        if (chipOrBadge instanceof Chip) {
            ((Chip) chipOrBadge).setChipIconTint(ColorStateList.valueOf(accent));
        }
    }

    public static void applyToAvatar(@NonNull View avatarView, @Nullable TextView initialsView, @NonNull EntityType type) {
        int accent = type.getAccentColor(avatarView.getContext());
        int onAccent = type.getOnAccentColor(avatarView.getContext());

        avatarView.setBackgroundTintList(ColorStateList.valueOf(accent));
        if (initialsView != null) {
            initialsView.setTextColor(onAccent);
        }
    }

    public static void applyToCardIndicator(@NonNull View indicatorView, @NonNull EntityType type) {
        int accent = type.getAccentColor(indicatorView.getContext());
        indicatorView.setBackgroundColor(accent);
    }

    public static void applyToCardBorder(@NonNull MaterialCardView card, @NonNull EntityType type) {
        int accent = type.getAccentColor(card.getContext());
        card.setStrokeColor(ColorStateList.valueOf(accent));
    }

    public static void applyToIcon(@NonNull ImageView imageView, @NonNull EntityType type) {
        int accent = type.getAccentColor(imageView.getContext());
        imageView.setImageTintList(ColorStateList.valueOf(accent));
    }

    public static void applyToTextView(@NonNull TextView textView, @NonNull EntityType type) {
        int accent = type.getAccentColor(textView.getContext());
        textView.setTextColor(accent);
    }
}
