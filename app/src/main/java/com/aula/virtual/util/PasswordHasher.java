package com.aula.virtual.util;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.aula.virtual.data.entity.User;
import com.aula.virtual.data.local.AppDatabase;
import java.util.List;

public class PasswordHasher {
    public static final int COST_FACTOR = 12;

    /**
     * Comprueba si una cadena tiene el formato de hash BCrypt ($2a$, $2b$ o $2y$ y 60 caracteres).
     */
    public static boolean isHashed(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        if (trimmed.length() != 60) {
            return false;
        }
        return trimmed.startsWith("$2a$") || trimmed.startsWith("$2b$") || trimmed.startsWith("$2y$");
    }

    /**
     * Genera un hash BCrypt seguro usando el factor de costo configurado (12).
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía.");
        }
        return BCrypt.withDefaults().hashToString(COST_FACTOR, plainPassword.toCharArray());
    }

    /**
     * Verifica una contraseña en texto plano contra un hash BCrypt almacenado.
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null || plainPassword.isEmpty() || hashedPassword.isEmpty()) {
            return false;
        }
        if (!isHashed(hashedPassword)) {
            return false;
        }
        try {
            BCrypt.Result result = BCrypt.verifyer().verify(plainPassword.toCharArray(), hashedPassword.trim());
            return result.verified;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Determina si el hash necesita actualizarse (si no es BCrypt o tiene costo menor al configurado).
     */
    public static boolean needsRehash(String hashedPassword) {
        if (!isHashed(hashedPassword)) {
            return true;
        }
        try {
            String[] parts = hashedPassword.trim().split("\\$");
            if (parts.length >= 3) {
                int cost = Integer.parseInt(parts[2]);
                return cost < COST_FACTOR;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    /**
     * Método utilitario opcional para migrar todas las contraseñas en texto plano
     * almacenadas en la base de datos local Room.
     * Debe ejecutarse en un hilo secundario (background executor).
     */
    public static void migrateAllPlainPasswords(AppDatabase db) {
        if (db == null) return;
        try {
            List<User> users = db.userDao().getAllUsers();
            if (users == null) return;
            for (User user : users) {
                if (user != null && user.password != null && !user.password.isEmpty()) {
                    if (!isHashed(user.password)) {
                        user.password = hashPassword(user.password);
                        db.userDao().insert(user);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }
}
