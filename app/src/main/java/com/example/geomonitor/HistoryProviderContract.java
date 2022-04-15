package com.example.geomonitor;

import android.net.Uri;

public class HistoryProviderContract {

    public static final String AUTHORITY = "com.example.geomonitor.HistoryProvider";
    public static final Uri HISTORY_URI = Uri.parse("content://"+AUTHORITY+"/ActivityHistory");

    public static final String ID = "_id";
    public static final String DATE = "date";
    public static final String STARTTIME = "startTime";
    public static final String ENDTIME = "endTime";
    public static final String DISTANCE = "distance";
    public static final String SPEED = "speed";

    public static final String CONTENT_TYPE_SINGLE = "vnd.android.cursor.item/HistoryProvider.data.text";
    public static final String CONTENT_TYPE_MULTIPLE = "vnd.android.cursor.dir/HistoryProvider.data.text";
}
