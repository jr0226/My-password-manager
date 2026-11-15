package com.example.mypasswordmanager;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.text.InputType;
import android.util.Base64;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PasswordAdapter adapter;
    private List<PasswordEntry> passwordList;
    private DatabaseHelper dbHelper;

    // Launchers for opening the phone's file browser safely
    private ActivityResultLauncher<Intent> exportLauncher;
    private ActivityResultLauncher<Intent> importLauncher;

    // Variables for the Image Picker
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private String currentLogoBase64 = "";
    private ImageView dialogLogoPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Prevent screenshots and secure recent app switcher
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        passwordList = new ArrayList<>();
        adapter = new PasswordAdapter(this, passwordList);

        recyclerView = findViewById(R.id.recyclerViewPasswords);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadPasswords();

        // 1. The Floating Add Button
        findViewById(R.id.fabAddPassword).setOnClickListener(v -> showEntryDialog(null));

        // 2. Setup Launchers
        exportLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                writeCsvToFile(result.getData().getData());
            }
        });

        importLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                readCsvFromFile(result.getData().getData());
            }
        });

        // 3. Image Picker Launcher
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                try {
                    InputStream imageStream = getContentResolver().openInputStream(result.getData().getData());
                    Bitmap bitmap = BitmapFactory.decodeStream(imageStream);
                    if (bitmap != null) {
                        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 128, 128, true);

                        if (dialogLogoPreview != null) {
                            dialogLogoPreview.setImageBitmap(scaledBitmap);
                        }

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                        currentLogoBase64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 4. Options Menu
        TextView btnMenu = findViewById(R.id.btnMenu);
        btnMenu.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(MainActivity.this, btnMenu);
            popup.getMenu().add("Import CSV Backup");
            popup.getMenu().add("Export CSV Backup");
            popup.getMenu().add("Lock Vault");
            popup.setOnMenuItemClickListener(item -> {
                if ("Import CSV Backup".equals(item.getTitle())) {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("*/*");
                    importLauncher.launch(intent);
                } else if ("Export CSV Backup".equals(item.getTitle())) {
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("text/csv");
                    intent.putExtra(Intent.EXTRA_TITLE, "MyPasswords_Backup.csv");
                    exportLauncher.launch(intent);
                } else if ("Lock Vault".equals(item.getTitle())) {
                    lockVault();
                }
                return true;
            });
            popup.show();
        });
    }

    private void lockVault() {
        Intent intent = new Intent(MainActivity.this, AuthActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    public void loadPasswords() {
        passwordList.clear();
        passwordList.addAll(dbHelper.getAllPasswords());
        adapter.notifyDataSetChanged();
    }

    public void onEntryDeleted(int id) {
        for (int i = 0; i < passwordList.size(); i++) {
            if (passwordList.get(i).getId() == id) {
                passwordList.remove(i);
                adapter.notifyItemRemoved(i);
                adapter.notifyItemRangeChanged(i, passwordList.size());
                break;
            }
        }
    }

    public void copyToClipboard(String label, String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        // Mark as sensitive on Android 13+ (API 33) to prevent clipboard preview overlays
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PersistableBundle extras = new PersistableBundle();
            extras.putBoolean("android.content.extra.IS_SENSITIVE", true);
            clip.getDescription().setExtras(extras);
        }
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, label + " Copied!", Toast.LENGTH_SHORT).show();
    }

    // --- CSV EXPORT & IMPORT (RFC 4180 COMPLIANT) ---

    private String escapeCsv(String value) {
        if (value == null) return "\"\"";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private void writeCsvToFile(Uri uri) {
        try {
            OutputStream outputStream = getContentResolver().openOutputStream(uri);
            if (outputStream == null) {
                Toast.makeText(this, "Cannot write to selected file", Toast.LENGTH_SHORT).show();
                return;
            }
            StringBuilder csvData = new StringBuilder();
            csvData.append("Site,Username,Password,Note\n");

            for (PasswordEntry entry : passwordList) {
                String plainPassword = EncryptionUtil.decrypt(entry.getPassword());
                csvData.append(escapeCsv(entry.getSiteName())).append(",")
                        .append(escapeCsv(entry.getUsername())).append(",")
                        .append(escapeCsv(plainPassword)).append(",")
                        .append(escapeCsv(entry.getNote() != null ? entry.getNote() : "")).append("\n");
            }

            outputStream.write(csvData.toString().getBytes());
            outputStream.close();
            Toast.makeText(this, "Exported Successfully!", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Export Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private List<String> parseCsvLine(String line) {
        List<String> columns = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '\"') {
                    cur.append('\"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                columns.add(cur.toString().trim());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        columns.add(cur.toString().trim());
        return columns;
    }

    private void readCsvFromFile(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                Toast.makeText(this, "Cannot open selected file", Toast.LENGTH_SHORT).show();
                return;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            int importCount = 0;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                if (line.toLowerCase().startsWith("\"site\"") || line.toLowerCase().startsWith("site,")) {
                    continue; // Skip header
                }

                List<String> cols = parseCsvLine(line);
                if (cols.size() >= 3) {
                    String site = cols.get(0);
                    String user = cols.get(1);
                    String pass = cols.get(2);
                    String note = cols.size() >= 4 ? cols.get(3) : "";

                    // Encrypt password before storing
                    String encryptedPass = EncryptionUtil.encrypt(pass);
                    dbHelper.addPassword(site, user, encryptedPass, note, "");
                    importCount++;
                }
            }
            reader.close();
            loadPasswords();
            Toast.makeText(this, "Imported " + importCount + " passwords!", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Import Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // --- UNIVERSAL ADD / EDIT DIALOG ---
    public void showEntryDialog(PasswordEntry entryToEdit) {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_entry);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvTitle = dialog.findViewById(R.id.tvDialogTitle);
        dialogLogoPreview = dialog.findViewById(R.id.ivDialogLogo);
        TextView btnChangeLogo = dialog.findViewById(R.id.btnChangeLogo);
        EditText etSite = dialog.findViewById(R.id.etDialogSite);
        EditText etUser = dialog.findViewById(R.id.etDialogUser);
        EditText etPass = dialog.findViewById(R.id.etDialogPass);
        TextView btnTogglePass = dialog.findViewById(R.id.btnDialogTogglePass);
        EditText etNote = dialog.findViewById(R.id.etDialogNote);
        Button btnSave = dialog.findViewById(R.id.btnDialogSave);

        currentLogoBase64 = "";

        // Toggle password mask
        final boolean[] isPasswordVisible = {false};
        btnTogglePass.setOnClickListener(v -> {
            isPasswordVisible[0] = !isPasswordVisible[0];
            if (isPasswordVisible[0]) {
                etPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                btnTogglePass.setText("HIDE");
            } else {
                etPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                btnTogglePass.setText("SHOW");
            }
            etPass.setSelection(etPass.getText().length());
        });

        if (entryToEdit != null) {
            tvTitle.setText("Edit Vault Entry");
            etSite.setText(entryToEdit.getSiteName());
            etUser.setText(entryToEdit.getUsername());
            etPass.setText(EncryptionUtil.decrypt(entryToEdit.getPassword()));
            etNote.setText(entryToEdit.getNote());
            currentLogoBase64 = entryToEdit.getLogoData();

            if (currentLogoBase64 != null && !currentLogoBase64.isEmpty()) {
                try {
                    byte[] decodedString = Base64.decode(currentLogoBase64, Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    if (decodedByte != null) {
                        dialogLogoPreview.setImageBitmap(decodedByte);
                    }
                } catch (Exception ignored) {}
            }
        }

        // Trigger Image Picker
        btnChangeLogo.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        // Save Button Logic
        btnSave.setOnClickListener(v -> {
            String site = etSite.getText().toString().trim();
            String user = etUser.getText().toString().trim();
            String pass = etPass.getText().toString().trim();
            String note = etNote.getText().toString().trim();

            if (site.isEmpty() || user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please fill required fields (Site, Username, Password)", Toast.LENGTH_SHORT).show();
                return;
            }

            if (entryToEdit == null) {
                // New Password Entry
                dbHelper.addPassword(site, user, EncryptionUtil.encrypt(pass), note, currentLogoBase64);
                Toast.makeText(MainActivity.this, "Saved Securely!", Toast.LENGTH_SHORT).show();
            } else {
                // Update Existing Password
                dbHelper.updatePassword(entryToEdit.getId(), site, user, EncryptionUtil.encrypt(pass), note, currentLogoBase64);
                Toast.makeText(MainActivity.this, "Updated Securely!", Toast.LENGTH_SHORT).show();
            }

            loadPasswords();
            dialog.dismiss();
        });

        dialog.show();
    }
}
