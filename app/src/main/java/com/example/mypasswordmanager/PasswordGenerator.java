package com.example.mypasswordmanager;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PasswordGenerator {

    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String SYMBOLS = "!@#$%^&*()-_=+[]{}|;:,.<>?";

    public static String generate(int length, boolean useUpper, boolean useLower, boolean useDigits, boolean useSymbols) {
        if (length < 6) length = 6;
        if (length > 64) length = 64;

        StringBuilder pool = new StringBuilder();
        List<Character> mandatoryChars = new ArrayList<>();
        SecureRandom random = new SecureRandom();

        if (useLower) {
            pool.append(LOWER);
            mandatoryChars.add(LOWER.charAt(random.nextInt(LOWER.length())));
        }
        if (useUpper) {
            pool.append(UPPER);
            mandatoryChars.add(UPPER.charAt(random.nextInt(UPPER.length())));
        }
        if (useDigits) {
            pool.append(DIGITS);
            mandatoryChars.add(DIGITS.charAt(random.nextInt(DIGITS.length())));
        }
        if (useSymbols) {
            pool.append(SYMBOLS);
            mandatoryChars.add(SYMBOLS.charAt(random.nextInt(SYMBOLS.length())));
        }

        // If nothing was selected, default to lower + upper + digits
        if (pool.length() == 0) {
            pool.append(LOWER).append(UPPER).append(DIGITS);
            mandatoryChars.add(LOWER.charAt(random.nextInt(LOWER.length())));
            mandatoryChars.add(UPPER.charAt(random.nextInt(UPPER.length())));
            mandatoryChars.add(DIGITS.charAt(random.nextInt(DIGITS.length())));
        }

        List<Character> passwordChars = new ArrayList<>(mandatoryChars);
        while (passwordChars.size() < length) {
            passwordChars.add(pool.charAt(random.nextInt(pool.length())));
        }

        // Shuffle so mandatory characters aren't always at the start
        Collections.shuffle(passwordChars, random);

        StringBuilder result = new StringBuilder();
        for (char c : passwordChars) {
            result.append(c);
        }
        return result.toString();
    }

    public static String generateDefault() {
        return generate(16, true, true, true, true);
    }
}
