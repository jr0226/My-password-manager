package com.example.mypasswordmanager;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
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
        this.dbHelper = new DatabaseHelper(context);
    }

    public void updateList(List<PasswordEntry> newList) {
        this.passwordList = newList;
        notifyDataSetChanged();
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

        // Display logo if available
        if (entry.getLogoData() != null && !entry.getLogoData().isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(entry.getLogoData(), Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                if (decodedByte != null) {
                    holder.ivLogo.setImageBitmap(decodedByte);
                } else {
                    holder.ivLogo.setImageResource(R.drawable.ic_launcher_foreground);
                }
            } catch (Exception e) {
                holder.ivLogo.setImageResource(R.drawable.ic_launcher_foreground);
            }
        } else {
            holder.ivLogo.setImageResource(R.drawable.ic_launcher_foreground);
        }

        // Display notes if available
        if (entry.getNote() != null && !entry.getNote().trim().isEmpty()) {
            holder.tvNote.setText("Note: " + entry.getNote().trim());
            holder.tvNote.setVisibility(View.VISIBLE);
        } else {
            holder.tvNote.setVisibility(View.GONE);
        }

        holder.tvPassword.setText("••••••••");
        holder.btnToggle.setText("SHOW");

        holder.btnToggle.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION || pos >= passwordList.size()) return;
            PasswordEntry current = passwordList.get(pos);

            if (holder.btnToggle.getText().toString().equalsIgnoreCase("SHOW")) {
                holder.tvPassword.setText(EncryptionUtil.decrypt(current.getPassword()));
                holder.btnToggle.setText("HIDE");
            } else {
                holder.tvPassword.setText("••••••••");
                holder.btnToggle.setText("SHOW");
            }
        });

        holder.btnCopy.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION || pos >= passwordList.size()) return;
            PasswordEntry current = passwordList.get(pos);

            String decrypted = EncryptionUtil.decrypt(current.getPassword());
            if (context instanceof MainActivity) {
                ((MainActivity) context).copyToClipboard("Password", decrypted);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION || pos >= passwordList.size()) return;
            PasswordEntry current = passwordList.get(pos);

            new AlertDialog.Builder(context)
                    .setTitle("Delete Entry")
                    .setMessage("Are you sure you want to delete '" + current.getSiteName() + "'?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        int currentPos = holder.getBindingAdapterPosition();
                        if (currentPos != RecyclerView.NO_POSITION && currentPos < passwordList.size()) {
                            dbHelper.deletePassword(current.getId());
                            if (context instanceof MainActivity) {
                                ((MainActivity) context).onEntryDeleted(current.getId());
                            } else {
                                passwordList.remove(currentPos);
                                notifyItemRemoved(currentPos);
                            }
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        holder.btnEdit.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION || pos >= passwordList.size()) return;
            PasswordEntry current = passwordList.get(pos);

            if (context instanceof MainActivity) {
                ((MainActivity) context).showEntryDialog(current);
            }
        });
    }

    @Override
    public int getItemCount() {
        return passwordList.size();
    }

    public static class PasswordViewHolder extends RecyclerView.ViewHolder {
        TextView tvSiteName, tvUsername, tvPassword, tvNote;
        ImageView ivLogo;
        TextView btnToggle, btnEdit, btnDelete, btnCopy;

        public PasswordViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSiteName = itemView.findViewById(R.id.tvItemSiteName);
            tvUsername = itemView.findViewById(R.id.tvItemUsername);
            tvPassword = itemView.findViewById(R.id.tvItemPassword);
            tvNote = itemView.findViewById(R.id.tvItemNote);
            ivLogo = itemView.findViewById(R.id.ivLogo);

            btnToggle = itemView.findViewById(R.id.btnToggleVisibility);
            btnEdit = itemView.findViewById(R.id.btnEditEntry);
            btnDelete = itemView.findViewById(R.id.btnDeleteEntry);
            btnCopy = itemView.findViewById(R.id.btnCopyPassword);
        }
    }
}
