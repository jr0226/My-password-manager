package com.example.mypasswordmanager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import java.util.concurrent.Executor;

public class AuthActivity extends AppCompatActivity {

    private LinearLayout layoutSetup, layoutLogin, layoutRecovery;
    private SharedPreferences prefs;

    public static final String[] SECURITY_QUESTIONS = {
            "What city were you born in?",
            "What is your primary school name?",
            "What was the name of your first pet?"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Prevent screenshots and secure against recent app previews
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_auth);

        prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);

        layoutSetup = findViewById(R.id.layoutSetup);
        layoutLogin = findViewById(R.id.layoutLogin);
        layoutRecovery = findViewById(R.id.layoutRecovery);

        checkUserState();
        setupClickListeners();
        setupRecoverySpinner();
    }

    private void checkUserState() {
        if (prefs.getString("master_hash", null) == null) {
            layoutSetup.setVisibility(View.VISIBLE);
            layoutLogin.setVisibility(View.GONE);
            layoutRecovery.setVisibility(View.GONE);
        } else {
            layoutSetup.setVisibility(View.GONE);
            layoutLogin.setVisibility(View.VISIBLE);
            layoutRecovery.setVisibility(View.GONE);
        }
    }

    private void setupRecoverySpinner() {
        Spinner spinner = findViewById(R.id.spinnerRecQuestions);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, SECURITY_QUESTIONS);
        spinner.setAdapter(adapter);
    }

    private void setupClickListeners() {
        // --- 1. SETUP LOGIC ---
        findViewById(R.id.btnSetupSave).setOnClickListener(v -> {
            String pass = ((EditText) findViewById(R.id.etSetupPassword)).getText().toString().trim();
            String ans1 = ((EditText) findViewById(R.id.etSetupQuestion1)).getText().toString().trim().toLowerCase();
            String ans2 = ((EditText) findViewById(R.id.etSetupQuestion2)).getText().toString().trim().toLowerCase();
            String ans3 = ((EditText) findViewById(R.id.etSetupQuestion3)).getText().toString().trim().toLowerCase();

            // Length Validation
            if (pass.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Empty Validation
            if (ans1.isEmpty() || ans2.isEmpty() || ans3.isEmpty()) {
                Toast.makeText(this, "Please answer all 3 security questions!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save hashes to local storage
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("master_hash", EncryptionUtil.hashPassword(pass));
            editor.putString("ans1_hash", EncryptionUtil.hashPassword(ans1));
            editor.putString("ans2_hash", EncryptionUtil.hashPassword(ans2));
            editor.putString("ans3_hash", EncryptionUtil.hashPassword(ans3));
            editor.apply();

            Toast.makeText(this, "Setup Complete!", Toast.LENGTH_SHORT).show();
            goToVault();
        });

        // --- 2. LOGIN LOGIC ---
        findViewById(R.id.btnLogin).setOnClickListener(v -> {
            String inputPass = ((EditText) findViewById(R.id.etLoginPassword)).getText().toString().trim();
            String savedHash = prefs.getString("master_hash", "");

            if (!savedHash.isEmpty() && EncryptionUtil.hashPassword(inputPass).equals(savedHash)) {
                goToVault();
            } else {
                Toast.makeText(this, "Incorrect Password", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnLoginBio).setOnClickListener(v -> setupBiometricAuth());

        findViewById(R.id.tvForgot).setOnClickListener(v -> {
            layoutLogin.setVisibility(View.GONE);
            layoutRecovery.setVisibility(View.VISIBLE);
        });

        // --- 3. RECOVERY LOGIC ---
        findViewById(R.id.btnRecBack).setOnClickListener(v -> checkUserState());

        findViewById(R.id.btnRecReset).setOnClickListener(v -> {
            Spinner spinner = findViewById(R.id.spinnerRecQuestions);
            int selectedQuestionIndex = spinner.getSelectedItemPosition();

            String inputAns = ((EditText) findViewById(R.id.etRecAnswer)).getText().toString().trim().toLowerCase();
            String newPass = ((EditText) findViewById(R.id.etRecNewPass)).getText().toString().trim();

            if (newPass.length() < 6) {
                Toast.makeText(this, "New password must be at least 6 characters!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (inputAns.isEmpty()) {
                Toast.makeText(this, "Please enter your answer", Toast.LENGTH_SHORT).show();
                return;
            }

            String targetHash = "";
            if (selectedQuestionIndex == 0) targetHash = prefs.getString("ans1_hash", "");
            else if (selectedQuestionIndex == 1) targetHash = prefs.getString("ans2_hash", "");
            else if (selectedQuestionIndex == 2) targetHash = prefs.getString("ans3_hash", "");

            // Verify and Reset
            if (!targetHash.isEmpty() && EncryptionUtil.hashPassword(inputAns).equals(targetHash)) {
                prefs.edit().putString("master_hash", EncryptionUtil.hashPassword(newPass)).apply();
                Toast.makeText(this, "Password Reset Successfully! Please Login.", Toast.LENGTH_LONG).show();
                checkUserState();
            } else {
                Toast.makeText(this, "Incorrect Answer for this question", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupBiometricAuth() {
        BiometricManager biometricManager = BiometricManager.from(this);
        int canAuthenticate = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL
        );
        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(this, "Biometrics / Device Credential not available", Toast.LENGTH_SHORT).show();
            return;
        }

        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                goToVault();
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    Toast.makeText(AuthActivity.this, "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
                }
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Secure Vault")
                .setSubtitle("Use your biometric credential to unlock")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();

        try {
            biometricPrompt.authenticate(promptInfo);
        } catch (Exception e) {
            Toast.makeText(this, "Biometric authentication failed to start", Toast.LENGTH_SHORT).show();
        }
    }

    private void goToVault() {
        startActivity(new Intent(AuthActivity.this, MainActivity.class));
        finish();
    }
}
