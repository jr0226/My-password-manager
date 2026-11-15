package com.example.mypasswordmanager;

import android.graphics.Color;

public class PasswordStrengthUtil {

    public enum StrengthLevel {
        NONE("Enter a password", "#A0AEC0", 0),
        WEAK("Weak password", "#E53E3E", 25),
        FAIR("Fair password", "#DD6B20", 50),
        GOOD("Good password", "#3182CE", 75),
        STRONG("Strong password", "#38A169", 100);

        public final String label;
        public final String colorHex;
        public final int progressPercent;

        StrengthLevel(String label, String colorHex, int progressPercent) {
            this.label = label;
            this.colorHex = colorHex;
            this.progressPercent = progressPercent;
        }

        public int getColor() {
            return Color.parseColor(colorHex);
        }
    }

    public static StrengthLevel calculateStrength(String password) {
        if (password == null || password.isEmpty()) {
            return StrengthLevel.NONE;
        }

        int score = 0;
        int length = password.length();

        if (length >= 8) score++;
        if (length >= 12) score++;
        if (length >= 16) score++;

        boolean hasLower = false;
        boolean hasUpper = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (char c : password.toCharArray()) {
            if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else hasSpecial = true;
        }

        if (hasLower && hasUpper) score++;
        if (hasDigit) score++;
        if (hasSpecial) score++;

        if (length < 6) {
            return StrengthLevel.WEAK;
        }

        if (score <= 2) {
            return StrengthLevel.WEAK;
        } else if (score == 3 || score == 4) {
            return StrengthLevel.FAIR;
        } else if (score == 5) {
            return StrengthLevel.GOOD;
        } else {
            return StrengthLevel.STRONG;
        }
    }
}
