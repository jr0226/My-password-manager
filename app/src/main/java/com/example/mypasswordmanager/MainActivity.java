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
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final String[] CATEGORIES = {"General", "Work", "Social", "Finance", "Entertainment", "Other"};

    private RecyclerView recyclerView;
    private PasswordAdapter adapter;
    private List<PasswordEntry> allPasswordList;
    private List<PasswordEntry> filteredPasswordList;
    private DatabaseHelper dbHelper;

    private EditText etSearch;
    private TextView btnClearSearch;
    private ChipGroup chipGroupCategories;
    private LinearLayout layoutEmptyState;
    private TextView tvEmptyTitle, tvEmptySubtitle;

    private String selectedCategory = "All";
    private String currentSearchQuery = "";

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
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        allPasswordList = new ArrayList<>();
        filteredPasswordList = new ArrayList<>();
        adapter = new PasswordAdapter(this, filteredPasswordList);

        recyclerView = findViewById(R.id.recyclerViewPasswords);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        etSearch = findViewById(R.id.etSearch);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        chipGroupCategories = findViewById(R.id.chipGroupCategories);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle);
        tvEmptySubtitle = findViewById(R.id.tvEmptySubtitle);

        setupSearchAndFilters();
        loadPasswords();

        // Add Button
        findViewById(R.id.fabAddPassword).setOnClickListener(v -> showEntryDialog(null));

        // Launchers
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

        // Image Picker Launcher
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

        // Options Menu
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

    private void setupSearchAndFilters() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s != null ? s.toString().trim() : "";
                btnClearSearch.setVisibility(currentSearchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnClearSearch.setOnClickListener(v -> etSearch.setText(""));

        chipGroupCategories.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                selectedCategory = "All";
            } else {
                int checkedId = checkedIds.get(0);
                Chip chip = findViewById(checkedId);
                selectedCategory = chip != null ? chip.getText().toString() : "All";
            }
            applyFilters();
        });
    }

    private void applyFilters() {
        filteredPasswordList.clear();
        String query = currentSearchQuery.toLowerCase();

        for (PasswordEntry entry : allPasswordList) {
            boolean matchesCategory = selectedCategory.equalsIgnoreCase("All") ||
                    (entry.getCategory() != null && entry.getCategory().equalsIgnoreCase(selectedCategory));

            boolean matchesQuery = query.isEmpty() ||
                    entry.getSiteName().toLowerCase().contains(query) ||
                    entry.getUsername().toLowerCase().contains(query) ||
                    (entry.getNote() != null && entry.getNote().toLowerCase().contains(query));

            if (matchesCategory && matchesQuery) {
                filteredPasswordList.add(entry);
            }
        }

        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (filteredPasswordList.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            if (allPasswordList.isEmpty()) {
                tvEmptyTitle.setText("Your Vault is Empty");
                tvEmptySubtitle.setText("Tap the + button below to add your first password.");
            } else {
                tvEmptyTitle.setText("No Matches Found");
                tvEmptySubtitle.setText("Try adjusting your search query or category filter.");
            }
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void lockVault() {
        Intent intent = new Intent(MainActivity.this, AuthActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    public void loadPasswords() {
        allPasswordList.clear();
        allPasswordList.addAll(dbHelper.getAllPasswords());
        applyFilters();
    }

    public void onEntryDeleted(int id) {
        for (int i = 0; i < allPasswordList.size(); i++) {
            if (allPasswordList.get(i).getId() == id) {
                allPasswordList.remove(i);
                break;
            }
        }
        applyFilters();
    }

    public void copyToClipboard(String label, String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PersistableBundle extras = new PersistableBundle();
            extras.putBoolean("android.content.extra.IS_SENSITIVE", true);
            clip.getDescription().setExtras(extras);
        }
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, label + " Copied!", Toast.LENGTH_SHORT).show();
    }

    // --- CSV EXPORT & IMPORT ---
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
            csvData.append("Site,Category,Username,Password,Note\n");

            for (PasswordEntry entry : allPasswordList) {
                String plainPassword = EncryptionUtil.decrypt(entry.getPassword());
                csvData.append(escapeCsv(entry.getSiteName())).append(",")
                        .append(escapeCsv(entry.getCategory())).append(",")
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
                    continue;
                }

                List<String> cols = parseCsvLine(line);
                if (cols.size() >= 3) {
                    String site = cols.get(0);
                    String category = "General";
                    String user;
                    String pass;
                    String note = "";

                    // If 5 columns: Site, Category, User, Pass, Note
                    if (cols.size() >= 5) {
                        category = cols.get(1);
                        user = cols.get(2);
                        pass = cols.get(3);
                        note = cols.get(4);
                    } else if (cols.size() == 4) {
                        // Could be Site, User, Pass, Note
                        user = cols.get(1);
                        pass = cols.get(2);
                        note = cols.get(3);
                    } else {
                        // 3 columns: Site, User, Pass
                        user = cols.get(1);
                        pass = cols.get(2);
                    }

                    String encryptedPass = EncryptionUtil.encrypt(pass);
                    dbHelper.addPassword(site, user, encryptedPass, note, "", category);
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
        Spinner spinnerCategory = dialog.findViewById(R.id.spinnerDialogCategory);
        EditText etUser = dialog.findViewById(R.id.etDialogUser);
        EditText etPass = dialog.findViewById(R.id.etDialogPass);
        TextView btnTogglePass = dialog.findViewById(R.id.btnDialogTogglePass);
        EditText etNote = dialog.findViewById(R.id.etDialogNote);
        Button btnSave = dialog.findViewById(R.id.btnDialogSave);

        // Populate Category Spinner
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, CATEGORIES);
        spinnerCategory.setAdapter(catAdapter);

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

            // Set spinner category
            if (entryToEdit.getCategory() != null) {
                int pos = Arrays.asList(CATEGORIES).indexOf(entryToEdit.getCategory());
                if (pos >= 0) spinnerCategory.setSelection(pos);
            }

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
            String category = spinnerCategory.getSelectedItem().toString();
            String user = etUser.getText().toString().trim();
            String pass = etPass.getText().toString().trim();
            String note = etNote.getText().toString().trim();

            if (site.isEmpty() || user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please fill required fields (Site, Username, Password)", Toast.LENGTH_SHORT).show();
                return;
            }

            if (entryToEdit == null) {
                dbHelper.addPassword(site, user, EncryptionUtil.encrypt(pass), note, currentLogoBase64, category);
                Toast.makeText(MainActivity.this, "Saved Securely!", Toast.LENGTH_SHORT).show();
            } else {
                dbHelper.updatePassword(entryToEdit.getId(), site, user, EncryptionUtil.encrypt(pass), note, currentLogoBase64, category);
                Toast.makeText(MainActivity.this, "Updated Securely!", Toast.LENGTH_SHORT).show();
            }

            loadPasswords();
            dialog.dismiss();
        });

        dialog.show();
    }
}
