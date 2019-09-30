package com.mycelium.wallet.external.mediaflow.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class NewsSQLiteHelper extends SQLiteOpenHelper {
    public static final String NEWS = "news";
    private static final String DATABASE_NAME = NEWS + ".db";
    private static final int DATABASE_VERSION = 3;

    public NewsSQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + NEWS + " (id INTEGER PRIMARY KEY" +
                ", title TEXT" +
                ", content TEXT" +
                ", date INTEGER" +
                ", author TEXT" +
                ", short_URL TEXT" +
                ", category TEXT" +
                ", categories TEXT" +
                ", image TEXT" +
                ", read INTEGER" +
                ", excerpt TEXT" +
                ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + NEWS + " ADD COLUMN image TEXT");
            db.execSQL("ALTER TABLE " + NEWS + " ADD COLUMN read TEXT");
            db.execSQL("ALTER TABLE " + NEWS + " ADD COLUMN excerpt TEXT");
        }
    }
}
