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
    private static final int DATABASE_VERSION = 3;
    private static final String TABLE_PASSWORDS = "passwords";

    private static final String COLUMN_ID = "id";
    private static final String COLUMN_SITE = "siteName";
    private static final String COLUMN_USER = "username";
    private static final String COLUMN_PASS = "password";
    private static final String COLUMN_NOTE = "note";
    private static final String COLUMN_LOGO = "logoData";

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
                COLUMN_LOGO + " TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Safe migration if needed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PASSWORDS);
        onCreate(db);
    }

    public long addPassword(String site, String user, String pass, String note, String logoData) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SITE, site != null ? site : "");
        values.put(COLUMN_USER, user != null ? user : "");
        values.put(COLUMN_PASS, pass != null ? pass : "");
        values.put(COLUMN_NOTE, note != null ? note : "");
        values.put(COLUMN_LOGO, logoData != null ? logoData : "");
        return db.insert(TABLE_PASSWORDS, null, values);
    }

    public List<PasswordEntry> getAllPasswords() {
        List<PasswordEntry> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_PASSWORDS, null, null, null, null, null, COLUMN_ID + " DESC");
            if (cursor != null && cursor.moveToFirst()) {
                int idIdx = cursor.getColumnIndex(COLUMN_ID);
                int siteIdx = cursor.getColumnIndex(COLUMN_SITE);
                int userIdx = cursor.getColumnIndex(COLUMN_USER);
                int passIdx = cursor.getColumnIndex(COLUMN_PASS);
                int noteIdx = cursor.getColumnIndex(COLUMN_NOTE);
                int logoIdx = cursor.getColumnIndex(COLUMN_LOGO);

                do {
                    int id = idIdx != -1 ? cursor.getInt(idIdx) : cursor.getInt(0);
                    String site = siteIdx != -1 ? cursor.getString(siteIdx) : cursor.getString(1);
                    String user = userIdx != -1 ? cursor.getString(userIdx) : cursor.getString(2);
                    String pass = passIdx != -1 ? cursor.getString(passIdx) : cursor.getString(3);
                    String note = noteIdx != -1 ? cursor.getString(noteIdx) : cursor.getString(4);
                    String logo = logoIdx != -1 ? cursor.getString(logoIdx) : (cursor.getColumnCount() > 5 ? cursor.getString(5) : "");

                    list.add(new PasswordEntry(id, site, user, pass, note, logo));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return list;
    }

    public int updatePassword(int id, String site, String user, String pass, String note, String logoData) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SITE, site != null ? site : "");
        values.put(COLUMN_USER, user != null ? user : "");
        values.put(COLUMN_PASS, pass != null ? pass : "");
        values.put(COLUMN_NOTE, note != null ? note : "");
        values.put(COLUMN_LOGO, logoData != null ? logoData : "");
        return db.update(TABLE_PASSWORDS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public int deletePassword(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_PASSWORDS, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
    }
}
