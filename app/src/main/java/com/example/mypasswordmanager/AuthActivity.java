package com.example.mypasswordmanager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);

        layoutSetup = findViewById(R.id.layoutSetup);
        layoutLogin = findViewById(R.id.layoutLogin);
        layoutRecovery = findViewById(R.id.layoutRecovery);

        checkUserState();
        setupClickListeners();
        setupRecoverySpinner(); // Setup the dropdown menu
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
        String[] questions = {
                "What city were you born in?",
                "What is your primary school name?",
                "What was the name of your first pet?"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, questions);
        spinner.setAdapter(adapter);
    }

    private void setupClickListeners() {
        // --- 1. SETUP LOGIC ---
        findViewById(R.id.btnSetupSave).setOnClickListener(v -> {
            String pass = ((EditText) findViewById(R.id.etSetupPassword)).getText().toString().trim();
            String ans1 = ((EditText) findViewById(R.id.etSetupQuestion1)).getText().toString().trim().toLowerCase();
            String ans2 = ((EditText) findViewById(R.id.etSetupQuestion2)).getText().toString().trim().toLowerCase();
            String ans3 = ((EditText) findViewById(R.id.etSetupQuestion3)).getText().toString().trim().toLowerCase();

            // CHECK 1: Length Validation
            if (pass.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters!", Toast.LENGTH_SHORT).show();
                return;
            }

            // CHECK 2: Empty Validation
            if (ans1.isEmpty() || ans2.isEmpty() || ans3.isEmpty()) {
                Toast.makeText(this, "Please answer all 3 security questions!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save all hashes to local storage
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

            if (EncryptionUtil.hashPassword(inputPass).equals(savedHash)) {
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
            int selectedQuestionIndex = spinner.getSelectedItemPosition(); // Will be 0, 1, or 2

            String inputAns = ((EditText) findViewById(R.id.etRecAnswer)).getText().toString().trim().toLowerCase();
            String newPass = ((EditText) findViewById(R.id.etRecNewPass)).getText().toString().trim();

            if (newPass.length() < 6) {
                Toast.makeText(this, "New password must be at least 6 characters!", Toast.LENGTH_SHORT).show();
                return;
            }

            String targetHash = "";
            if (selectedQuestionIndex == 0) targetHash = prefs.getString("ans1_hash", "");
            else if (selectedQuestionIndex == 1) targetHash = prefs.getString("ans2_hash", "");
            else if (selectedQuestionIndex == 2) targetHash = prefs.getString("ans3_hash", "");

            // Verify and Reset
            if (EncryptionUtil.hashPassword(inputAns).equals(targetHash)) {
                prefs.edit().putString("master_hash", EncryptionUtil.hashPassword(newPass)).apply();
                Toast.makeText(this, "Password Reset Successfully! Please Login.", Toast.LENGTH_LONG).show();
                checkUserState();
            } else {
                Toast.makeText(this, "Incorrect Answer for this question", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupBiometricAuth() {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                goToVault();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Password Manager")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();
        biometricPrompt.authenticate(promptInfo);
    }

    private void goToVault() {
        startActivity(new Intent(AuthActivity.this, MainActivity.class));
        finish();
    }
}