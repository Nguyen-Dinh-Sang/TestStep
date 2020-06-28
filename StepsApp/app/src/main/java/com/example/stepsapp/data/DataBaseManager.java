package com.example.stepsapp.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.util.Pair;

import com.example.stepsapp.util.DateUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DataBaseManager extends SQLiteOpenHelper {

    private final static String DATABASE_NAME = "stepsapp";
    private final static int DATABASE_VERSION = 1;

    private static DataBaseManager instance;
    private static final AtomicInteger openCounter = new AtomicInteger();

    private DataBaseManager(final Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized DataBaseManager getInstance(final Context c) {
        if (instance == null) {
            instance = new DataBaseManager(c.getApplicationContext());
        }
        openCounter.incrementAndGet();
        return instance;
    }

    @Override
    public void close() {
        if (openCounter.decrementAndGet() == 0) {
            super.close();
        }
    }

    @Override
    public void onCreate(final SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + DATABASE_NAME + " (date INTEGER, steps INTEGER)");
    }

    // hàm này được gọi khi version database =2
    @Override
    public void onUpgrade(final SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 1) {
            // drop PRIMARY KEY constraint
            db.execSQL("CREATE TABLE " + DATABASE_NAME + "2 (date INTEGER, steps INTEGER)");
            db.execSQL("INSERT INTO " + DATABASE_NAME + "2 (date, steps) SELECT date, steps FROM " +
                    DATABASE_NAME);
            db.execSQL("DROP TABLE " + DATABASE_NAME);
            db.execSQL("ALTER TABLE " + DATABASE_NAME + "2 RENAME TO " + DATABASE_NAME + "");
        }
    }

    public Cursor query(final String[] columns, final String selection,
                        final String[] selectionArgs, final String groupBy, final String having,
                        final String orderBy, final String limit) {
        return getReadableDatabase()
                .query(DATABASE_NAME, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
    }

    public void insertNewDay(long date, int steps) {
        getWritableDatabase().beginTransaction();
        try {
            Cursor c = getReadableDatabase().query(DATABASE_NAME, new String[]{"date"}, "date = ?",
                    new String[]{String.valueOf(date)}, null, null, null);
            if (c.getCount() == 0 && steps >= 0) {

                // add 'steps' to yesterdays count
                addToLastEntry(steps);

                // add today
                ContentValues values = new ContentValues();
                values.put("date", date);
                // use the negative steps as offset
                values.put("steps", -steps);
                getWritableDatabase().insert(DATABASE_NAME, null, values);
            }
            c.close();
            getWritableDatabase().setTransactionSuccessful();
        } finally {
            getWritableDatabase().endTransaction();
        }
    }

    public void addToLastEntry(int steps) {
        getWritableDatabase().execSQL("UPDATE " + DATABASE_NAME + " SET steps = steps + " + steps +
                " WHERE date = (SELECT MAX(date) FROM " + DATABASE_NAME + ")");
    }


    public boolean insertDayFromBackup(long date, int steps) {
        getWritableDatabase().beginTransaction();
        boolean newEntryCreated = false;
        try {
            ContentValues values = new ContentValues();
            values.put("steps", steps);
            int updatedRows = getWritableDatabase()
                    .update(DATABASE_NAME, values, "date = ?", new String[]{String.valueOf(date)});
            if (updatedRows == 0) {
                values.put("date", date);
                getWritableDatabase().insert(DATABASE_NAME, null, values);
                newEntryCreated = true;
            }
            getWritableDatabase().setTransactionSuccessful();
        } finally {
            getWritableDatabase().endTransaction();
        }
        return newEntryCreated;
    }

    public void logState() {

        Cursor c = getReadableDatabase()
                .query(DATABASE_NAME, null, null, null, null, null, "date DESC", "5");
        Log.d("STEP", "Data " + c);
        c.close();
    }

    // lấy tổng bước nhưng không lấy ngày hiện tại
    public int getTotalWithoutToday() {
        Cursor c = getReadableDatabase()
                .query(DATABASE_NAME, new String[]{"SUM(steps)"}, "steps > 0 AND date > 0 AND date < ?",
                        new String[]{String.valueOf(DateUtil.getToday())}, null, null, null);
        c.moveToFirst();
        int re = c.getInt(0);
        c.close();
        return re;
    }

    // lấy số bước đi nhiều nhất trong 1 ngày
    public int getRecord() {
        Cursor c = getReadableDatabase()
                .query(DATABASE_NAME, new String[]{"MAX(steps)"}, "date > 0", null, null, null, null);
        c.moveToFirst();
        int re = c.getInt(0);
        c.close();
        return re;
    }

    //lấy số bước đi nhiều nhất trong ngày kèm luôn ngày
    public Pair<Date, Integer> getRecordData() {
        Cursor c = getReadableDatabase()
                .query(DATABASE_NAME, new String[]{"date, steps"}, "date > 0", null, null, null,
                        "steps DESC", "1");
        c.moveToFirst();
        Pair<Date, Integer> p = new Pair<Date, Integer>(new Date(c.getLong(0)), c.getInt(1));
        c.close();
        return p;
    }

    //lấy số bước cho một ngày
    public int getSteps(final long date) {
        Cursor c = getReadableDatabase().query(DATABASE_NAME, new String[]{"steps"}, "date = ?",
                new String[]{String.valueOf(date)}, null, null, null);
        c.moveToFirst();
        int re;
        if (c.getCount() == 0) re = Integer.MIN_VALUE;
        else re = c.getInt(0);
        c.close();
        return re;
    }

    //lấy nhiều ngày
    public List<Pair<Long, Integer>> getLastEntries(int num) {
        Cursor c = getReadableDatabase()
                .query(DATABASE_NAME, new String[]{"date", "steps"}, "date > 0", null, null, null,
                        "date DESC", String.valueOf(num));
        int max = c.getCount();
        List<Pair<Long, Integer>> result = new ArrayList<>(max);
        if (c.moveToFirst()) {
            do {
                result.add(new Pair<>(c.getLong(0), c.getInt(1)));
            } while (c.moveToNext());
        }
        return result;
    }

    //lấy số bước trong một khoảng time
    public int getSteps(final long start, final long end) {
        Cursor c = getReadableDatabase()
                .query(DATABASE_NAME, new String[]{"SUM(steps)"}, "date >= ? AND date <= ?",
                        new String[]{String.valueOf(start), String.valueOf(end)}, null, null, null);
        int re;
        if (c.getCount() == 0) {
            re = 0;
        } else {
            c.moveToFirst();
            re = c.getInt(0);
        }
        c.close();
        return re;
    }

    //xóa các giá trị âm
    void removeNegativeEntries() {
        getWritableDatabase().delete(DATABASE_NAME, "steps < ?", new String[]{"0"});
    }

    //xóa giá trị quá lớn, ảo
    public void removeInvalidEntries() {
        getWritableDatabase().delete(DATABASE_NAME, "steps >= ?", new String[]{"200000"});
    }

    //đếm số ngày hợp lệ
    public int getDaysWithoutToday() {
        Cursor c = getReadableDatabase()
                .query(DATABASE_NAME, new String[]{"COUNT(*)"}, "steps > ? AND date < ? AND date > 0",
                        new String[]{String.valueOf(0), String.valueOf(DateUtil.getToday())}, null,
                        null, null);
        c.moveToFirst();
        int re = c.getInt(0);
        c.close();
        return re < 0 ? 0 : re;
    }

    //lấy số ngày kết quả > 1
    public int getDays() {
        // todays is not counted yet
        int re = this.getDaysWithoutToday() + 1;
        return re;
    }

    //lưu lại số bước hiện tại
    public void saveCurrentSteps(int steps) {
        ContentValues values = new ContentValues();
        values.put("steps", steps);
        if (getWritableDatabase().update(DATABASE_NAME, values, "date = -1", null) == 0) {
            values.put("date", -1);
            getWritableDatabase().insert(DATABASE_NAME, null, values);
        }
        Log.d("STEP", "Lưu vào database " + steps);
    }

    //lấy số bước hiện tại
    public int getCurrentSteps() {
        int re = getSteps(-1);
        return re == Integer.MIN_VALUE ? 0 : re;
    }
}
