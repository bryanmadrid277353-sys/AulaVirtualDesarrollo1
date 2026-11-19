package com.aula.virtual.ui;

import java.util.regex.Pattern;

public class ValidationUtils {
    public static boolean isValidCarnet(String carnet) {
        return carnet != null && carnet.length() == 10 && carnet.matches("\\d+");
    }

    /**
     * Valida la política de contraseñas requerida:
     * Mínimo 8 caracteres, al menos una letra y al menos un número.
     */
    public static boolean isValidPasswordPolicy(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) hasLetter = true;
            if (Character.isDigit(c)) hasDigit = true;
            if (hasLetter && hasDigit) return true;
        }
        return false;
    }

    public static PasswordStrength getPasswordStrength(String password) {
        if (!isValidPasswordPolicy(password)) {
            return PasswordStrength.WEAK;
        }

        boolean hasUppercase = !password.equals(password.toLowerCase());
        boolean hasSymbol = Pattern.compile("[!@#$%^&*(),.?\":{}|<>]").matcher(password).find();

        if (hasUppercase && hasSymbol && password.length() >= 10) {
            return PasswordStrength.STRONG;
        } else {
            return PasswordStrength.MEDIUM;
        }
    }

    public enum PasswordStrength {
        WEAK("Débil (mín. 8 caracteres, letra y número)", 0xFFFF4444),
        MEDIUM("Aceptable", 0xFFFFBB33),
        STRONG("Fuerte", 0xFF00C851);

        public final String label;
        public final int color;

        PasswordStrength(String label, int color) {
            this.label = label;
            this.color = color;
        }
    }
}
