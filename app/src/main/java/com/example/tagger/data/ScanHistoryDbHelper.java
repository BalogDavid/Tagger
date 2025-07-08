package com.example.tagger.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * HELPER CLASS PENTRU GESTIONAREA BAZEI DE DATE LOCALE SQLITE
 * CARE VA STOCA ISTORICUL SCANARILOR.
 */
public class ScanHistoryDbHelper extends SQLiteOpenHelper {
    // VERSIUNEA BAZEI DE DATE - CRESTEREA ACESTEI VERSIUNI VA FORTA UPDATE-URILE SCHEMEI
    private static final int DATABASE_VERSION = 1;
    
    // NUMELE BAZEI DE DATE
    private static final String DATABASE_NAME = "scanner_history.db";
    
    // SINGLETON INSTANCE
    private static ScanHistoryDbHelper sInstance;
    
    // NUME TABEL SI COLOANE
    public static final class HistoryEntry {
        public static final String TABLE_NAME = "scan_history";
        public static final String COLUMN_ID = "_id";
        public static final String COLUMN_DATE = "date";
        public static final String COLUMN_BRAND = "brand_name";
        public static final String COLUMN_RESULT = "result";
        public static final String COLUMN_CONFIDENCE = "confidence";
        public static final String COLUMN_IMAGE_PATH = "image_path";
    }
    
    // CONSTRUCTOR PRIVAT PENTRU SINGLETON
    private ScanHistoryDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    // METODA PENTRU A OBTINE SINGLETON INSTANCE
    public static synchronized ScanHistoryDbHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ScanHistoryDbHelper(context.getApplicationContext());
        }
        return sInstance;
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        // CREAREA TABELULUI
        final String SQL_CREATE_SCAN_HISTORY_TABLE = "CREATE TABLE " + 
                HistoryEntry.TABLE_NAME + " (" +
                HistoryEntry.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                HistoryEntry.COLUMN_DATE + " INTEGER NOT NULL, " + // TIMESTAMP IN MILISECUNDE
                HistoryEntry.COLUMN_BRAND + " TEXT NOT NULL, " +
                HistoryEntry.COLUMN_RESULT + " TEXT NOT NULL, " +
                HistoryEntry.COLUMN_CONFIDENCE + " REAL NOT NULL, " +
                HistoryEntry.COLUMN_IMAGE_PATH + " TEXT" + // POATE FI NULL
                ");";
                
        db.execSQL(SQL_CREATE_SCAN_HISTORY_TABLE);
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // LA O VERSIUNE NOUA, STERGEM TABELELE VECHI SI LE RECREAM
        db.execSQL("DROP TABLE IF EXISTS " + HistoryEntry.TABLE_NAME);
        onCreate(db);
    }
} 