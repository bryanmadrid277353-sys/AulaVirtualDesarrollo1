package com.aula.virtual.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "attendance_states")
public class AttendanceState {
    @PrimaryKey
    public int value;
    public String label;

    public AttendanceState() {}

    public AttendanceState(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public static String getLabelForValue(Integer val) {
        if (val == null) return "Sin evaluar";
        switch (val) {
            case 1: return "Asistió";
            case 2: return "No asistió";
            case 3: return "Justificado";
            case 0:
            default: return "Sin evaluar";
        }
    }

    public static Integer getValueForLabel(String label) {
        if (label == null) return 0;
        String clean = normalizeString(label);
        switch (clean) {
            case "asistio":
            case "asistió":
            case "a":
            case "presente":
            case "1":
                return 1;
            case "noasistio":
            case "noasistió":
            case "ausente":
            case "n":
            case "na":
            case "f":
            case "2":
                return 2;
            case "justificado":
            case "j":
            case "excused":
            case "3":
                return 3;
            case "sinevaluar":
            case "se":
            case "0":
            default:
                return 0;
        }
    }

    private static String normalizeString(String input) {
        if (input == null) return "";
        String normalized = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "")
                .toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }
}
