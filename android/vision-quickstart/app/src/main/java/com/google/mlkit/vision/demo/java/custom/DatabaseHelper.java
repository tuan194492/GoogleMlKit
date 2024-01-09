package com.google.mlkit.vision.demo.java.custom;// DatabaseHelper.java
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "face_data.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_NAME = "face_data";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_FACE_POINTS = "face_points";

    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_NAME + " TEXT, " +
                    COLUMN_FACE_POINTS + " TEXT);";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public void saveFaceData(String name, String faceMeshPoints) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_FACE_POINTS, faceMeshPoints);

        db.insert(TABLE_NAME, null, values);
        db.close();
    }
    public ArrayList<FaceData> getAllFaceData() {
        ArrayList<FaceData> faceDataList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String[] columns = {COLUMN_ID, COLUMN_NAME, COLUMN_FACE_POINTS};
        Cursor cursor = db.query(TABLE_NAME, columns, null, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {

                int id = cursor.getInt(cursor.getColumnIndex(COLUMN_ID));
                String name = cursor.getString(cursor.getColumnIndex(COLUMN_NAME));
                String faceMeshPoints = cursor.getString(cursor.getColumnIndex(COLUMN_FACE_POINTS));

                FaceData faceData = new FaceData(name, faceMeshPoints);
                faceDataList.add(faceData);
            } while (cursor.moveToNext());

            cursor.close();
        }

        db.close();

        return faceDataList;
    }
}
