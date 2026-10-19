package com.aula.virtual.ui;

import java.util.regex.Pattern;

public class ValidationUtils {
    public static boolean isValidCarnet(String carnet) {
        return carnet != null && carnet.length() == 10 && carnet.matches("\\d+");
    }

    public static PasswordStrength getPasswordStrength(String password) {
        if (password == null || password.length() < 4) {
            return PasswordStrength.WEAK;
        }

        boolean hasUppercase = !password.equals(password.toLowerCase());
        boolean hasSymbol = Pattern.compile("[!@#$%^&*(),.?\":{}|<>]").matcher(password).find();

        if (hasUppercase && hasSymbol) {
            return PasswordStrength.STRONG;
        } else if (hasUppercase || hasSymbol || password.length() >= 6) {
            return PasswordStrength.MEDIUM;
        } else {
            return PasswordStrength.WEAK;
        }
    }

    public enum PasswordStrength {
        WEAK("Débil (mín. 4 caracteres)", 0xFFFF4444),
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
