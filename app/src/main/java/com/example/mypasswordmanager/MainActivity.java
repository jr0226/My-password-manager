package com.example.mypasswordmanager;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.BufferedReader;
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
    private android.widget.ImageView dialogLogoPreview; // Links to the ImageView inside the dialog!

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        passwordList = new ArrayList<>();
        adapter = new PasswordAdapter(this, passwordList);

        recyclerView = findViewById(R.id.recyclerViewPasswords);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadPasswords();

        // 1. The Floating Add Button (Passes 'null' because it's a NEW entry)
        findViewById(R.id.fabAddPassword).setOnClickListener(v -> showEntryDialog(null));

        // 2. Setup Launchers (Keep your existing import/export launcher logic here)
        exportLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) writeCsvToFile(result.getData().getData());
        });

        importLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) readCsvFromFile(result.getData().getData());
        });

        // 3. The Image Picker Logic
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                try {
                    android.graphics.Bitmap bitmap = android.provider.MediaStore.Images.Media.getBitmap(this.getContentResolver(), result.getData().getData());
                    android.graphics.Bitmap scaledBitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, 150, 150, true);

                    if (dialogLogoPreview != null) {
                        dialogLogoPreview.setImageBitmap(scaledBitmap);
                    }

                    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                    scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, baos);
                    currentLogoBase64 = android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.DEFAULT);
                } catch (Exception e) {
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 4. The Sleek 3-Dot Options Menu!
        TextView btnMenu = findViewById(R.id.btnMenu);
        btnMenu.setOnClickListener(v -> {
            android.widget.PopupMenu popup = new android.widget.PopupMenu(MainActivity.this, btnMenu);
            popup.getMenu().add("Import CSV Backup");
            popup.getMenu().add("Export CSV Backup");
            popup.setOnMenuItemClickListener(item -> {
                if (item.getTitle().equals("Import CSV Backup")) {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("*/*");
                    importLauncher.launch(intent);
                } else if (item.getTitle().equals("Export CSV Backup")) {
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("text/csv");
                    intent.putExtra(Intent.EXTRA_TITLE, "MyPasswords_Backup.csv");
                    exportLauncher.launch(intent);
                }
                return true;
            });
            popup.show();
        });
    }

    private void loadPasswords() {
        passwordList.clear();
        passwordList.addAll(dbHelper.getAllPasswords());
        adapter.notifyDataSetChanged();
    }

    // --- CSV LOGIC ---

    private void writeCsvToFile(Uri uri) {
        try {
            OutputStream outputStream = getContentResolver().openOutputStream(uri);
            StringBuilder csvData = new StringBuilder();
            csvData.append("Site,Username,Password,Note\n"); // Added Note to header

            for (PasswordEntry entry : passwordList) {
                String safeNote = entry.getNote() != null ? entry.getNote().replace(",", " ") : ""; // Prevent commas in notes from breaking CSV
                csvData.append(entry.getSiteName()).append(",")
                        .append(entry.getUsername()).append(",")
                        .append(entry.getPassword()).append(",")
                        .append(safeNote).append("\n");
            }

            outputStream.write(csvData.toString().getBytes());
            outputStream.close();
            Toast.makeText(this, "Exported Successfully!", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Export Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void readCsvFromFile(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            int importCount = 0;

            while ((line = reader.readLine()) != null) {
                if (line.toLowerCase().contains("site,username,password")) continue;

                String[] columns = line.split(",");
                if (columns.length >= 3) {
                    String site = columns[0].trim();
                    String user = columns[1].trim();
                    String pass = columns[2].trim();
                    // Grab the note if it exists, otherwise leave it blank
                    String note = columns.length >= 4 ? columns[3].trim() : "";

                    // FIX: Passing 4 parameters now!
                    dbHelper.addPassword(site, user, pass, note, "");
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

    // 5. THE UNIVERSAL DIALOG (Replaces showAddPasswordDialog)
    // If entryToEdit is null, it creates a new password. If it has data, it edits it!
    public void showEntryDialog(PasswordEntry entryToEdit) {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_entry);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);

        TextView tvTitle = dialog.findViewById(R.id.tvDialogTitle);
        dialogLogoPreview = dialog.findViewById(R.id.ivDialogLogo);
        TextView btnChangeLogo = dialog.findViewById(R.id.btnChangeLogo);
        EditText etSite = dialog.findViewById(R.id.etDialogSite);
        EditText etUser = dialog.findViewById(R.id.etDialogUser);
        EditText etPass = dialog.findViewById(R.id.etDialogPass);
        EditText etNote = dialog.findViewById(R.id.etDialogNote);
        Button btnSave = dialog.findViewById(R.id.btnDialogSave);

        currentLogoBase64 = ""; // Reset the image

        if (entryToEdit != null) {
            tvTitle.setText("Edit Vault Entry");
            etSite.setText(entryToEdit.getSiteName());
            etUser.setText(entryToEdit.getUsername());
            etPass.setText(EncryptionUtil.decrypt(entryToEdit.getPassword()));
            etNote.setText(entryToEdit.getNote());
            currentLogoBase64 = entryToEdit.getLogoData();

            if (currentLogoBase64 != null && !currentLogoBase64.isEmpty()) {
                try {
                    byte[] decodedString = android.util.Base64.decode(currentLogoBase64, android.util.Base64.DEFAULT);
                    android.graphics.Bitmap decodedByte = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    dialogLogoPreview.setImageBitmap(decodedByte);
                } catch (Exception e) {}
            }
        }

        // TRIGGER THE IMAGE PICKER
        btnChangeLogo.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        // SAVE BUTTON LOGIC
        btnSave.setOnClickListener(v -> {
            String site = etSite.getText().toString().trim();
            String user = etUser.getText().toString().trim();
            String pass = etPass.getText().toString().trim();
            String note = etNote.getText().toString().trim();

            if (site.isEmpty() || user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please fill required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (entryToEdit == null) {
                // IT IS A NEW PASSWORD
                dbHelper.addPassword(site, user, EncryptionUtil.encrypt(pass), note, currentLogoBase64);
                Toast.makeText(MainActivity.this, "Saved Securely!", Toast.LENGTH_SHORT).show();
            } else {
                // WE ARE UPDATING AN EXISTING PASSWORD
                dbHelper.updatePassword(entryToEdit.getId(), site, user, EncryptionUtil.encrypt(pass), note, currentLogoBase64);
                Toast.makeText(MainActivity.this, "Updated Securely!", Toast.LENGTH_SHORT).show();
            }

            loadPasswords();
            dialog.dismiss();
        });

        dialog.show();
    }
}