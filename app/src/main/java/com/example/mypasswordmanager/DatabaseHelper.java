package com.example.mypasswordmanager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "PasswordVault.db";
    private static final int DATABASE_VERSION = 3; // Upgraded to Version 3!
    private static final String TABLE_PASSWORDS = "passwords";

    private static final String COLUMN_ID = "id";
    private static final String COLUMN_SITE = "siteName";
    private static final String COLUMN_USER = "username";
    private static final String COLUMN_PASS = "password";
    private static final String COLUMN_NOTE = "note";
    private static final String COLUMN_LOGO = "logoData"; // NEW COLUMN

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_PASSWORDS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_SITE + " TEXT, " +
                COLUMN_USER + " TEXT, " +
                COLUMN_PASS + " TEXT, " +
                COLUMN_NOTE + " TEXT, " +
                COLUMN_LOGO + " TEXT)"; // Added logo column
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PASSWORDS);
        onCreate(db);
    }

    public void addPassword(String site, String user, String pass, String note, String logoData) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SITE, site);
        values.put(COLUMN_USER, user);
        values.put(COLUMN_PASS, pass);
        values.put(COLUMN_NOTE, note);
        values.put(COLUMN_LOGO, logoData); // Save the image text
        db.insert(TABLE_PASSWORDS, null, values);
        db.close();
    }

    public List<PasswordEntry> getAllPasswords() {
        List<PasswordEntry> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_PASSWORDS, null);

        if (cursor.moveToFirst()) {
            do {
                PasswordEntry entry = new PasswordEntry(
                        cursor.getInt(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getString(4),
                        cursor.getString(5) // Grab the image text
                );
                list.add(entry);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    public void updatePassword(int id, String site, String user, String pass, String note, String logoData) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SITE, site);
        values.put(COLUMN_USER, user);
        values.put(COLUMN_PASS, pass);
        values.put(COLUMN_NOTE, note);
        values.put(COLUMN_LOGO, logoData);
        db.update(TABLE_PASSWORDS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void deletePassword(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_PASSWORDS, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }
}