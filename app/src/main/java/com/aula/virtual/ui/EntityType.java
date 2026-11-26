package com.aula.virtual.ui;

import android.content.Context;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.StyleRes;
import com.aula.virtual.R;

public enum EntityType {
    ENCARGADO("Encargado", R.drawable.ic_person_badge, R.style.ThemeOverlay_Aula_Encargado, R.color.entity_encargado, R.color.entity_encargado_soft, R.color.entity_encargado_on),
    ALUMNO("Estudiante", R.drawable.ic_person, R.style.ThemeOverlay_Aula_Alumno, R.color.entity_alumno, R.color.entity_alumno_soft, R.color.entity_alumno_on),
    PROFESOR("Profesor", R.drawable.ic_professor, R.style.ThemeOverlay_Aula_Profesor, R.color.entity_profesor, R.color.entity_profesor_soft, R.color.entity_profesor_on),
    MATERIA("Materia", R.drawable.ic_book, R.style.ThemeOverlay_Aula_Materia, R.color.entity_materia, R.color.entity_materia_soft, R.color.entity_materia_on),
    INSTALACION("Instalación", R.drawable.ic_building, R.style.ThemeOverlay_Aula_Instalacion, R.color.entity_instalacion, R.color.entity_instalacion_soft, R.color.entity_instalacion_on),
    ADMINISTRADOR("Administrador", R.drawable.ic_shield_check, R.style.ThemeOverlay_Aula_Administrador, R.color.entity_administrador, R.color.entity_administrador_soft, R.color.entity_administrador_on);

    private final String label;
    private final int iconRes;
    private final int themeOverlayRes;
    private final int colorRes;
    private final int colorSoftRes;
    private final int colorOnRes;

    EntityType(String label, @DrawableRes int iconRes, @StyleRes int themeOverlayRes, int colorRes, int colorSoftRes, int colorOnRes) {
        this.label = label;
        this.iconRes = iconRes;
        this.themeOverlayRes = themeOverlayRes;
        this.colorRes = colorRes;
        this.colorSoftRes = colorSoftRes;
        this.colorOnRes = colorOnRes;
    }

    public String getLabel() {
        return label;
    }

    @DrawableRes
    public int getIconRes() {
        return iconRes;
    }

    @StyleRes
    public int getThemeOverlayRes() {
        return themeOverlayRes;
    }

    public int getColorRes() {
        return colorRes;
    }

    public int getColorSoftRes() {
        return colorSoftRes;
    }

    public int getColorOnRes() {
        return colorOnRes;
    }

    @ColorInt
    public int getAccentColor(Context context) {
        return context.getColor(colorRes);
    }

    @ColorInt
    public int getSoftColor(Context context) {
        return context.getColor(colorSoftRes);
    }

    @ColorInt
    public int getOnAccentColor(Context context) {
        return context.getColor(colorOnRes);
    }

    public static EntityType fromRole(String role) {
        if (role == null) return ALUMNO;
        switch (role.toUpperCase()) {
            case "ADMIN":
            case "ADMINISTRADOR":
                return ADMINISTRADOR;
            case "PROFESSOR":
            case "PROFESOR":
            case "TEACHER":
                return PROFESOR;
            case "ENCARGADO":
            case "GUARDIAN":
                return ENCARGADO;
            case "STUDENT":
            case "ALUMNO":
            case "ESTUDIANTE":
            default:
                return ALUMNO;
        }
    }
}
