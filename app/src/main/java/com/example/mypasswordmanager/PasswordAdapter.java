package com.example.mypasswordmanager;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class PasswordAdapter extends RecyclerView.Adapter<PasswordAdapter.PasswordViewHolder> {

    private List<PasswordEntry> passwordList;
    private Context context;
    private DatabaseHelper dbHelper;

    public PasswordAdapter(Context context, List<PasswordEntry> passwordList) {
        this.context = context;
        this.passwordList = passwordList;
        this.dbHelper = new DatabaseHelper(context); // Initialize it
    }

    @NonNull
    @Override
    public PasswordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_password, parent, false);
        return new PasswordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PasswordViewHolder holder, int position) {
        PasswordEntry entry = passwordList.get(position);

        holder.tvSiteName.setText(entry.getSiteName());
        holder.tvUsername.setText(entry.getUsername());

        // --- LOGIC FOR REAL IMAGE LOGO ---
        if (entry.getLogoData() != null && !entry.getLogoData().isEmpty()) {
            try {
                byte[] decodedString = android.util.Base64.decode(entry.getLogoData(), android.util.Base64.DEFAULT);
                android.graphics.Bitmap decodedByte = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                holder.ivLogo.setImageBitmap(decodedByte);
            } catch (Exception e) {
                holder.ivLogo.setImageResource(R.drawable.ic_launcher_foreground);
            }
        } else {
            holder.ivLogo.setImageResource(R.drawable.ic_launcher_foreground);
        }

        // --- LOGIC FOR NOTES ---
        if (entry.getNote() != null && !entry.getNote().isEmpty()) {
            holder.tvNote.setText("Note: " + entry.getNote());
            holder.tvNote.setVisibility(View.VISIBLE);
        } else {
            holder.tvNote.setVisibility(View.GONE);
        }

        holder.tvPassword.setText("••••••••");
        holder.btnToggle.setText("SHOW");

        holder.btnToggle.setOnClickListener(v -> {
            if (holder.btnToggle.getText().toString().equalsIgnoreCase("SHOW")) {
                holder.tvPassword.setText(EncryptionUtil.decrypt(entry.getPassword()));
                holder.btnToggle.setText("HIDE");
            } else {
                holder.tvPassword.setText("••••••••");
                holder.btnToggle.setText("SHOW");
            }
        });

        holder.btnCopy.setOnClickListener(v -> {
            // Get the clipboard service
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            // Decrypt the password and put it in the clipboard
            android.content.ClipData clip = android.content.ClipData.newPlainText("Saved Password", EncryptionUtil.decrypt(entry.getPassword()));
            clipboard.setPrimaryClip(clip);
            // Show a success message
            Toast.makeText(context, "Password Copied!", Toast.LENGTH_SHORT).show();
        });

        holder.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Delete")
                    .setMessage("Are you sure?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        dbHelper.deletePassword(entry.getId());
                        passwordList.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, passwordList.size());
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // --- EDIT BUTTON LOGIC ---
        holder.btnEdit.setOnClickListener(v -> {
            ((MainActivity) context).showEntryDialog(entry);
        });
    }

    @Override
    public int getItemCount() {
        return passwordList.size();
    }

    public static class PasswordViewHolder extends RecyclerView.ViewHolder {
        TextView tvSiteName, tvUsername, tvPassword, tvNote;
        android.widget.ImageView ivLogo;

        // 1.  DECLARE ALL 4 BUTTONS HERE (Including Copy!)
        TextView btnToggle, btnEdit, btnDelete, btnCopy;

        public PasswordViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSiteName = itemView.findViewById(R.id.tvItemSiteName);
            tvUsername = itemView.findViewById(R.id.tvItemUsername);
            tvPassword = itemView.findViewById(R.id.tvItemPassword);
            tvNote = itemView.findViewById(R.id.tvItemNote);
            ivLogo = itemView.findViewById(R.id.ivLogo);

            // 2.  LINK THEM TO THE XML IDs HERE
            btnToggle = itemView.findViewById(R.id.btnToggleVisibility);
            btnEdit = itemView.findViewById(R.id.btnEditEntry);
            btnDelete = itemView.findViewById(R.id.btnDeleteEntry);
            btnCopy = itemView.findViewById(R.id.btnCopyPassword); // <- The missing piece!
        }
    }
}