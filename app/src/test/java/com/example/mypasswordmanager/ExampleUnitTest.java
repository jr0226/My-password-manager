package com.example.mypasswordmanager;

import org.junit.Test;
import static org.junit.Assert.*;

public class ExampleUnitTest {

    @Test
    public void testPasswordStrengthCalculation() {
        assertEquals(PasswordStrengthUtil.StrengthLevel.NONE, PasswordStrengthUtil.calculateStrength(""));
        assertEquals(PasswordStrengthUtil.StrengthLevel.NONE, PasswordStrengthUtil.calculateStrength(null));

        // Short / weak password
        assertEquals(PasswordStrengthUtil.StrengthLevel.WEAK, PasswordStrengthUtil.calculateStrength("12345"));
        assertEquals(PasswordStrengthUtil.StrengthLevel.WEAK, PasswordStrengthUtil.calculateStrength("abcdef"));

        // Fair password
        PasswordStrengthUtil.StrengthLevel fair = PasswordStrengthUtil.calculateStrength("Pass1234");
        assertTrue(fair == PasswordStrengthUtil.StrengthLevel.FAIR || fair == PasswordStrengthUtil.StrengthLevel.GOOD);

        // Strong password
        PasswordStrengthUtil.StrengthLevel strong = PasswordStrengthUtil.calculateStrength("P@ssw0rd!#Secure2026$");
        assertEquals(PasswordStrengthUtil.StrengthLevel.STRONG, strong);
    }

    @Test
    public void testPasswordGeneratorDefaults() {
        String generated = PasswordGenerator.generateDefault();
        assertNotNull(generated);
        assertEquals(16, generated.length());

        // Test custom length
        String custom = PasswordGenerator.generate(24, true, true, true, true);
        assertEquals(24, custom.length());
    }

    @Test
    public void testPasswordGeneratorConstraintEnforcement() {
        // Digits only
        String digitsOnly = PasswordGenerator.generate(10, false, false, true, false);
        assertEquals(10, digitsOnly.length());
        assertTrue(digitsOnly.matches("[0-9]+"));

        // Letters only
        String lettersOnly = PasswordGenerator.generate(12, true, true, false, false);
        assertEquals(12, lettersOnly.length());
        assertTrue(lettersOnly.matches("[a-zA-Z]+"));
    }
}
