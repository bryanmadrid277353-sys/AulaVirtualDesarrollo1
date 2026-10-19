package com.aula.virtual.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import com.aula.virtual.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.io.ByteArrayOutputStream;

public class ImageUtils {

    public interface OnImageCroppedListener {
        void onCropped(Bitmap croppedBitmap);
    }

    public static void showCropAndAdjustDialog(Context context, Bitmap rawBitmap, OnImageCroppedListener listener) {
        if (context == null || rawBitmap == null) return;

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_crop_image, null);
        CropImageView cropImageView = dialogView.findViewById(R.id.cropImageView);
        View btnRotate = dialogView.findViewById(R.id.btnRotate90);
        View btnCancel = dialogView.findViewById(R.id.btnCropCancel);
        View btnSave = dialogView.findViewById(R.id.btnCropSave);

        cropImageView.setImageBitmap(rawBitmap);

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setView(dialogView)
                .create();

        if (btnRotate instanceof MaterialButton) {
            MaterialButton mb = (MaterialButton) btnRotate;
            int accentColor = ThemeHelper.getSubjectColor(context, ThemeHelper.getAccentColorName(context));
            mb.setStrokeColor(ColorStateList.valueOf(accentColor));
            mb.setStrokeWidth((int) (2 * context.getResources().getDisplayMetrics().density));
            mb.setOnClickListener(v -> cropImageView.rotate90Degrees());
        } else if (btnRotate != null) {
            btnRotate.setOnClickListener(v -> cropImageView.rotate90Degrees());
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                Bitmap cropped = cropImageView.getCroppedBitmap();
                if (cropped != null && listener != null) {
                    listener.onCropped(cropped);
                }
                dialog.dismiss();
            });
        }

        dialog.show();
    }

    public static String bitmapToBase64(Bitmap bitmap) {
        if (bitmap == null) return null;
        
        // Resize to max 400px to save DB space
        int size = Math.min(bitmap.getWidth(), bitmap.getHeight());
        Bitmap cropped = Bitmap.createBitmap(bitmap, 0, 0, size, size);
        Bitmap scaled = Bitmap.createScaledBitmap(cropped, 400, 400, true);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        scaled.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    public static Bitmap base64ToBitmap(String base64Str) {
        if (base64Str == null || base64Str.trim().isEmpty()) return null;
        try {
            byte[] decodedBytes = Base64.decode(base64Str, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "AV";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        } else {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        }
    }

    public static Bitmap createInitialsBitmap(Context context, String name, int sizePx) {
        if (sizePx <= 0) sizePx = 200;
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        int accentColor = ThemeHelper.getSubjectColor(context, ThemeHelper.getAccentColorName(context));

        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(accentColor);
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, bgPaint);

        String initials = getInitials(name);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(sizePx * 0.42f);

        Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
        float baseline = (sizePx / 2f) - ((fontMetrics.descent + fontMetrics.ascent) / 2f);
        canvas.drawText(initials, sizePx / 2f, baseline, textPaint);

        return bitmap;
    }

    public static void setProfileImage(ImageView iv, String profileImage, String userName) {
        setProfileImage(iv, profileImage, userName, 0);
    }

    public static void setProfileImage(ImageView iv, String profileImage, int defaultPaddingPx) {
        setProfileImage(iv, profileImage, null, defaultPaddingPx);
    }

    public static void setProfileImage(ImageView iv, String profileImage, String userName, int defaultPaddingPx) {
        if (iv == null) return;
        Context context = iv.getContext();
        
        iv.setImageTintList(null);
        iv.clearColorFilter();
        iv.setPadding(0, 0, 0, 0);

        if (profileImage != null && !profileImage.trim().isEmpty()) {
            Bitmap bitmap = base64ToBitmap(profileImage);
            if (bitmap != null) {
                iv.setImageBitmap(bitmap);
                if (iv.getParent() instanceof CardView) {
                    CardView card = (CardView) iv.getParent();
                    card.setCardBackgroundColor(Color.WHITE);
                    card.setCardElevation(dpToPx(context, 2));
                }
                return;
            }
        }

        Bitmap initialsBitmap = createInitialsBitmap(context, userName, 200);
        iv.setImageBitmap(initialsBitmap);
        if (iv.getParent() instanceof CardView) {
            CardView card = (CardView) iv.getParent();
            card.setCardBackgroundColor(Color.TRANSPARENT);
            card.setCardElevation(0);
        }
    }

    private static int dpToPx(Context context, int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }
}
