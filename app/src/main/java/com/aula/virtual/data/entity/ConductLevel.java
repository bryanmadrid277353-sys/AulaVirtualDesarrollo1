package com.aula.virtual.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "conduct_levels")
public class ConductLevel {
    @PrimaryKey
    public int value;
    public String label;

    public ConductLevel() {}

    public ConductLevel(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public static String getLabelForValue(Integer val) {
        if (val == null) return "Sin evaluar";
        switch (val) {
            case 5: return "Excelente";
            case 4: return "Muy bueno";
            case 3: return "Bueno";
            case 2: return "Regular";
            case 1: return "Malo";
            default: return "Sin evaluar";
        }
    }

    public static Integer getValueForLabel(String label) {
        if (label == null) return null;
        String clean = normalizeString(label);
        switch (clean) {
            case "excelente":
            case "e":
                return 5;
            case "muybueno":
            case "m":
                return 4;
            case "bueno":
            case "b":
                return 3;
            case "regular":
            case "r":
                return 2;
            case "malo":
            case "o":
                return 1;
            default:
                return null;
        }
    }

    private static String normalizeString(String input) {
        if (input == null) return "";
        String normalized = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "")
                .toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z]", "");
    }
}
