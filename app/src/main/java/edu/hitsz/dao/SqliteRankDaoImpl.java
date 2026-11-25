package edu.hitsz.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import edu.hitsz.application.GameDifficulty;

/**
 * 基于 SQLite 的排行榜 DAO 实现，数据持久化于应用沙箱 databases/rank.db。
 */
public class SqliteRankDaoImpl implements RankDao {

    private static final String[] PROJECTION = {
            RankDbHelper.COL_ID,
            RankDbHelper.COL_NAME,
            RankDbHelper.COL_SCORE,
            RankDbHelper.COL_TIME,
            RankDbHelper.COL_DIFFICULTY
    };

    private static final String ORDER_BY_SCORE_DESC =
            RankDbHelper.COL_SCORE + " DESC, " + RankDbHelper.COL_ID + " ASC";

    private final RankDbHelper dbHelper;

    public SqliteRankDaoImpl(Context context) {
        this.dbHelper = new RankDbHelper(context.getApplicationContext());
    }

    @Override
    public long insert(RankRecord record) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = toContentValues(record);
        long id = db.insert(RankDbHelper.TABLE_RANK, null, values);
        if (id != -1L) {
            record.setId(id);
        }
        return id;
    }

    @Override
    public void insertAll(List<RankRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            for (RankRecord record : records) {
                long id = db.insert(RankDbHelper.TABLE_RANK, null, toContentValues(record));
                if (id != -1L) {
                    record.setId(id);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public List<RankRecord> findByDifficulty(GameDifficulty difficulty) {
        GameDifficulty target = (difficulty != null) ? difficulty : GameDifficulty.NORMAL;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<RankRecord> result = new ArrayList<>();
        try (Cursor cursor = db.query(
                RankDbHelper.TABLE_RANK,
                PROJECTION,
                RankDbHelper.COL_DIFFICULTY + "=?",
                new String[]{target.name()},
                null,
                null,
                ORDER_BY_SCORE_DESC)) {
            while (cursor.moveToNext()) {
                result.add(fromCursor(cursor));
            }
        }
        return result;
    }

    @Override
    public List<RankRecord> findAll() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<RankRecord> result = new ArrayList<>();
        try (Cursor cursor = db.query(
                RankDbHelper.TABLE_RANK,
                PROJECTION,
                null,
                null,
                null,
                null,
                ORDER_BY_SCORE_DESC)) {
            while (cursor.moveToNext()) {
                result.add(fromCursor(cursor));
            }
        }
        return result;
    }

    @Override
    public int update(RankRecord record) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.update(
                RankDbHelper.TABLE_RANK,
                toContentValues(record),
                RankDbHelper.COL_ID + "=?",
                new String[]{String.valueOf(record.getId())});
    }

    @Override
    public int deleteById(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(
                RankDbHelper.TABLE_RANK,
                RankDbHelper.COL_ID + "=?",
                new String[]{String.valueOf(id)});
    }

    private static ContentValues toContentValues(RankRecord record) {
        ContentValues values = new ContentValues();
        values.put(RankDbHelper.COL_NAME, record.getName());
        values.put(RankDbHelper.COL_SCORE, record.getScore());
        values.put(RankDbHelper.COL_TIME, record.getTime());
        GameDifficulty difficulty = record.getDifficulty() != null
                ? record.getDifficulty()
                : GameDifficulty.NORMAL;
        values.put(RankDbHelper.COL_DIFFICULTY, difficulty.name());
        return values;
    }

    private static RankRecord fromCursor(Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndexOrThrow(RankDbHelper.COL_ID));
        String name = cursor.getString(cursor.getColumnIndexOrThrow(RankDbHelper.COL_NAME));
        int score = cursor.getInt(cursor.getColumnIndexOrThrow(RankDbHelper.COL_SCORE));
        String time = cursor.getString(cursor.getColumnIndexOrThrow(RankDbHelper.COL_TIME));
        String diffName = cursor.getString(cursor.getColumnIndexOrThrow(RankDbHelper.COL_DIFFICULTY));
        GameDifficulty difficulty;
        try {
            difficulty = GameDifficulty.valueOf(diffName);
        } catch (IllegalArgumentException e) {
            difficulty = GameDifficulty.NORMAL;
        }
        return new RankRecord(id, name, score, time, difficulty);
    }
}
