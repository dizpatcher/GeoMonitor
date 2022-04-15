package com.example.geomonitor;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBHelper extends SQLiteOpenHelper {

    public DBHelper(Context context){
        super(context, "DB", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d("GEO_MONITOR_DB", "DBHelper onCreate");
        db.execSQL("CREATE TABLE ActivityHistory (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "date DATE, " +
                "startTime TIME, " +
                "endTime TIME, " +
                "distance FLOAT(10,2), " +
                "speed FLOAT(10,2)" +
                ");");
        db.execSQL("INSERT INTO ActivityHistory (date, startTime, endTime, distance, speed) VALUES ('2019-01-06', '09:00:00', '09:30:00', 5000, 1.39);");
        db.execSQL("INSERT INTO ActivityHistory (date, startTime, endTime, distance, speed) VALUES ('2019-01-07', '10:00:00', '10:05:00', 1000, 3.33);");
        db.execSQL("INSERT INTO ActivityHistory (date, startTime, endTime, distance, speed) VALUES ('2019-01-08', '11:00:00', '11:05:00', 1000, 3.33);");
        db.execSQL("INSERT INTO ActivityHistory (date, startTime, endTime, distance, speed) VALUES ('2019-01-08', '14:00:00', '15:10:00', 2000, 3.33);");
        db.execSQL("INSERT INTO ActivityHistory (date, startTime, endTime, distance, speed) VALUES ('2019-01-10', '08:00:00', '08:05:00', 5000, 1.39);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL("DROP TABLE IF EXISTS myRecipeBook");
        onCreate(db);
    }
}
