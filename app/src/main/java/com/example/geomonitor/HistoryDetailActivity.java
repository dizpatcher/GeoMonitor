package com.example.geomonitor;

import androidx.appcompat.app.AppCompatActivity;

import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class HistoryDetailActivity extends AppCompatActivity {

    private String id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_detail);
        id = getIntent().getExtras().getString("id");
        queryHistory();
    }

    private void queryHistory() {
        final TextView dateView = findViewById(R.id.DateText);
        final TextView timeView = findViewById(R.id.TimeText);
        final TextView durationView = findViewById(R.id.DurationText);
        final TextView distanceView = findViewById(R.id.DistanceText);
        final TextView speedView = findViewById(R.id.SpeedText);

        String[] projection = new String[]{
                HistoryProviderContract.ID,
                HistoryProviderContract.DATE,
                HistoryProviderContract.STARTTIME,
                HistoryProviderContract.ENDTIME,
                HistoryProviderContract.DISTANCE,
                HistoryProviderContract.SPEED
        };

        Cursor cursor = getContentResolver().query(HistoryProviderContract.HISTORY_URI, projection, "_id = ?", new String[]{id}, null);
        if(cursor.moveToFirst()){ //data exists
            dateView.setText(cursor.getString(1));
            timeView.setText("From " + cursor.getString(2) + " to " + cursor.getString(3));
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
            Date startDate = null;
            Date endDate = null;
            try {
                startDate = timeFormat.parse(cursor.getString(2));
                endDate = timeFormat.parse(cursor.getString(3));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            long duration = endDate.getTime() - startDate.getTime();
            if(duration < 0){ //if the endTime is the next day
                duration = duration + 86400000; //add total milliseconds for a day to running duration
            }
            durationView.setText(stringForTime(duration));
            distanceView.setText(cursor.getString(4) + " m");
            speedView.setText(cursor.getString(5) + " m/s");
        }
    }

    //function that return the time in string format given the time in milliseconds
    private String stringForTime(long duration) {
        int totalSeconds = (int) duration / 1000;
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds/3600;
        if(hours>0){ //if running time is more than an hour
            return Integer.toString(hours) + " hour" + Integer.toString(minutes)
                    + "minutes" + Integer.toString(seconds) + " seconds";
        }
        else if(minutes > 0){//if running time is more than one minute
            return Integer.toString(minutes) + " minutes " + Integer.toString(seconds) + " seconds";
        }
        else{
            return Integer.toString(seconds) + " seconds";
        }
    }

    public void onClickDelete(View v){
        //delete history given the specific id number
        getContentResolver().delete(HistoryProviderContract.HISTORY_URI, "_id=?", new String[]{id});
        finish();
    }
}