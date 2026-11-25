package edu.hitsz.dao;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * 排行榜 SQLite 数据库辅助类，管理 rank.db 的建表与版本升级。
 */
public class RankDbHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "rank.db";
    public static final int DB_VERSION = 1;

    public static final String TABLE_RANK = "rank";
    public static final String COL_ID = "_id";
    public static final String COL_NAME = "name";
    public static final String COL_SCORE = "score";
    public static final String COL_TIME = "time";
    public static final String COL_DIFFICULTY = "difficulty";

    public RankDbHelper(Context context) {
        super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableSql = "CREATE TABLE " + TABLE_RANK + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_NAME + " TEXT NOT NULL, "
                + COL_SCORE + " INTEGER NOT NULL, "
                + COL_TIME + " TEXT NOT NULL, "
                + COL_DIFFICULTY + " TEXT NOT NULL)";
        db.execSQL(createTableSql);

        String createIndexSql = "CREATE INDEX idx_rank_difficulty_score ON " + TABLE_RANK
                + " (" + COL_DIFFICULTY + ", " + COL_SCORE + " DESC)";
        db.execSQL(createIndexSql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RANK);
        onCreate(db);
    }
}
