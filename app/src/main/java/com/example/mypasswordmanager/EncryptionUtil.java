package com.example.mypasswordmanager;

import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionUtil {
    private static final String LEGACY_SECRET_KEY = "MySecr3tP@ssw0rd";
    private static final String GCM_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 12 bytes recommended for GCM
    private static final int GCM_TAG_LENGTH = 128; // in bits
    private static final String V2_PREFIX = "v2:";

    // Fixed internal vault key derivation seed (32 bytes AES-256)
    private static final byte[] MASTER_SALT = "Vault_GCM_Secure_Salt_2026#@!*".getBytes(StandardCharsets.UTF_8);

    private static SecretKey getAesGcmKey() {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            sha256.update(MASTER_SALT);
            byte[] keyBytes = sha256.digest(LEGACY_SECRET_KEY.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            byte[] fallback = new byte[32];
            Arrays.fill(fallback, (byte) 0x5A);
            return new SecretKeySpec(fallback, "AES");
        }
    }

    /**
     * Encrypts plaintext using authenticated AES-GCM with a random IV.
     * Prefixed with "v2:" to differentiate from legacy entries.
     */
    public static String encrypt(String rawPassword) {
        if (rawPassword == null) return "";
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(GCM_ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, getAesGcmKey(), spec);

            byte[] cipherBytes = cipher.doFinal(rawPassword.getBytes(StandardCharsets.UTF_8));

            // Combine IV and cipher bytes
            byte[] combined = new byte[iv.length + cipherBytes.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherBytes, 0, combined, iv.length, cipherBytes.length);

            return V2_PREFIX + Base64.encodeToString(combined, Base64.NO_WRAP);
        } catch (Exception e) {
            // Fallback to legacy encryption if GCM fails
            return encryptLegacy(rawPassword);
        }
    }

    /**
     * Decrypts ciphertext, transparently handling modern AES-GCM and legacy AES-ECB.
     */
    public static String decrypt(String encryptedPassword) {
        if (encryptedPassword == null || encryptedPassword.isEmpty()) return "";

        // Check if modern AES-GCM
        if (encryptedPassword.startsWith(V2_PREFIX)) {
            try {
                String payload = encryptedPassword.substring(V2_PREFIX.length());
                byte[] combined = Base64.decode(payload, Base64.NO_WRAP);
                if (combined.length > GCM_IV_LENGTH) {
                    byte[] iv = Arrays.copyOfRange(combined, 0, GCM_IV_LENGTH);
                    byte[] cipherBytes = Arrays.copyOfRange(combined, GCM_IV_LENGTH, combined.length);

                    Cipher cipher = Cipher.getInstance(GCM_ALGORITHM);
                    GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
                    cipher.init(Cipher.DECRYPT_MODE, getAesGcmKey(), spec);

                    byte[] decrypted = cipher.doFinal(cipherBytes);
                    return new String(decrypted, StandardCharsets.UTF_8);
                }
            } catch (Exception e) {
                // In case of error, fall through to legacy decrypt attempt
            }
        }

        // Legacy AES-ECB decryption
        return decryptLegacy(encryptedPassword);
    }

    private static String encryptLegacy(String rawPassword) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(LEGACY_SECRET_KEY.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encryptedBytes = cipher.doFinal(rawPassword.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP);
        } catch (Exception e) {
            return rawPassword;
        }
    }

    private static String decryptLegacy(String encryptedPassword) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(LEGACY_SECRET_KEY.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decodedBytes = Base64.decode(encryptedPassword, Base64.NO_WRAP);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return encryptedPassword;
        }
    }

    /**
     * Hashes master password or security question answers using SHA-256.
     */
    public static String hashPassword(String password) {
        if (password == null) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) {
            return password;
        }
    }
}
