package com.example.tagger.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.List;

/**
 * REPOSITORY PATTERN IMPLEMENTAT PENTRU A ABSTRACTIZA ACCESUL LA BAZA DE DATE
 * SI PENTRU A GESTIONA ISTORICUL SCANARILOR.
 */
public class ScanHistoryRepository {
    private ScanHistoryDbHelper dbHelper;
    
    public ScanHistoryRepository(Context context) {
        dbHelper = ScanHistoryDbHelper.getInstance(context);
    }
    
    /**
     * ADAUGA O NOUA SCANARE IN BAZA DE DATE
     */
    public long addScanToHistory(ScanHistoryItem scanItem) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(ScanHistoryDbHelper.HistoryEntry.COLUMN_DATE, scanItem.getDate());
        values.put(ScanHistoryDbHelper.HistoryEntry.COLUMN_BRAND, scanItem.getBrandName());
        values.put(ScanHistoryDbHelper.HistoryEntry.COLUMN_RESULT, scanItem.getResult());
        values.put(ScanHistoryDbHelper.HistoryEntry.COLUMN_CONFIDENCE, scanItem.getConfidence());
        values.put(ScanHistoryDbHelper.HistoryEntry.COLUMN_IMAGE_PATH, scanItem.getImagePath());
        
        return db.insert(ScanHistoryDbHelper.HistoryEntry.TABLE_NAME, null, values);
    }
    
    /**
     * OBTINE TOT ISTORICUL SCANARILOR, ORDONAT DUPA DATA (DESCENDENT - DE LA NOU LA VECHI)
     */
    public List<ScanHistoryItem> getAllScans() {
        List<ScanHistoryItem> scans = new ArrayList<>();
        
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        // DEFINIM PROIECTIA (COLOANELE CARE NE INTERESEAZA)
        String[] projection = {
            ScanHistoryDbHelper.HistoryEntry.COLUMN_ID,
            ScanHistoryDbHelper.HistoryEntry.COLUMN_DATE,
            ScanHistoryDbHelper.HistoryEntry.COLUMN_BRAND,
            ScanHistoryDbHelper.HistoryEntry.COLUMN_RESULT,
            ScanHistoryDbHelper.HistoryEntry.COLUMN_CONFIDENCE,
            ScanHistoryDbHelper.HistoryEntry.COLUMN_IMAGE_PATH
        };
        
        // SORTARE DESCRESCATOARE DUPA DATA
        String sortOrder = ScanHistoryDbHelper.HistoryEntry.COLUMN_DATE + " DESC";
        
        try (Cursor cursor = db.query(
                ScanHistoryDbHelper.HistoryEntry.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                sortOrder
        )) {
            // ITERAM PRIN TOATE RANDURILE
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(ScanHistoryDbHelper.HistoryEntry.COLUMN_ID));
                long date = cursor.getLong(cursor.getColumnIndexOrThrow(ScanHistoryDbHelper.HistoryEntry.COLUMN_DATE));
                String brandName = cursor.getString(cursor.getColumnIndexOrThrow(ScanHistoryDbHelper.HistoryEntry.COLUMN_BRAND));
                String result = cursor.getString(cursor.getColumnIndexOrThrow(ScanHistoryDbHelper.HistoryEntry.COLUMN_RESULT));
                float confidence = cursor.getFloat(cursor.getColumnIndexOrThrow(ScanHistoryDbHelper.HistoryEntry.COLUMN_CONFIDENCE));
                String imagePath = cursor.getString(cursor.getColumnIndexOrThrow(ScanHistoryDbHelper.HistoryEntry.COLUMN_IMAGE_PATH));
                
                scans.add(new ScanHistoryItem(id, date, brandName, result, confidence, imagePath));
            }
        }
        
        return scans;
    }
    
    /**
     * STERGE TOT ISTORICUL SCANARILOR
     */
    public int clearHistory() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(ScanHistoryDbHelper.HistoryEntry.TABLE_NAME, null, null);
    }
    
    /**
     * STERGE O SCANARE SPECIFICA DIN ISTORIC DUPA ID
     */
    public int deleteScan(long scanId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        String selection = ScanHistoryDbHelper.HistoryEntry.COLUMN_ID + " = ?";
        String[] selectionArgs = { String.valueOf(scanId) };
        
        return db.delete(ScanHistoryDbHelper.HistoryEntry.TABLE_NAME, selection, selectionArgs);
    }
    
    /**
     * VERIFICA DACA ISTORICUL ESTE GOL
     */
    public boolean isHistoryEmpty() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String countQuery = "SELECT COUNT(*) FROM " + ScanHistoryDbHelper.HistoryEntry.TABLE_NAME;
        
        Cursor cursor = db.rawQuery(countQuery, null);
        int count = 0;
        
        if (cursor != null) {
            cursor.moveToFirst();
            count = cursor.getInt(0);
            cursor.close();
        }
        
        return count == 0;
    }
} 