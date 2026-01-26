package com.pos.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordUtil {
    private static final String ALGO = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_LENGTH = 16;

    public static byte[] newSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    public static String hashToStoredValue(String password) {
        byte[] salt = newSalt();
        byte[] hash = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS);
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(hash);
        return "pbkdf2$" + ITERATIONS + "$" + saltB64 + "$" + hashB64;
    }

    public static boolean verify(String password, String storedValue) {
        if (password == null || storedValue == null) return false;
        if (!storedValue.startsWith("pbkdf2$")) return false;
        String[] parts = storedValue.split("\\$");
        if (parts.length != 4) return false;
        int iterations;
        try {
            iterations = Integer.parseInt(parts[1]);
        } catch (NumberFormatException nfe) {
            return false;
        }
        byte[] salt;
        byte[] expected;
        try {
            salt = Base64.getDecoder().decode(parts[2]);
            expected = Base64.getDecoder().decode(parts[3]);
        } catch (IllegalArgumentException iae) {
            return false;
        }
        byte[] actual = pbkdf2(password.toCharArray(), salt, iterations, expected.length * 8);
        return constantTimeEquals(expected, actual);
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBits) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGO);
            return skf.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("Password hashing failed", e);
        }
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null) return false;
        if (a.length != b.length) return false;
        int r = 0;
        for (int i = 0; i < a.length; i++) {
            r |= (a[i] ^ b[i]);
        }
        return r == 0;
    }
}
