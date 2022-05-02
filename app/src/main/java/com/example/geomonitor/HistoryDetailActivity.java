package com.example.geomonitor;

import androidx.appcompat.app.AppCompatActivity;

import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class HistoryDetailActivity extends AppCompatActivity implements View.OnClickListener {

    final String MY_TAG = "GEO_MONITOR";
    private String id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_detail);
        Button btnDelete = findViewById(R.id.btnDelete);
        btnDelete.setOnClickListener(this);

        id = getIntent().getExtras().getString("id");
        queryHistory();
    }

    // вывод данных
    private void queryHistory() {
        final TextView dateView = findViewById(R.id.DateText);
        final TextView timeView = findViewById(R.id.TimeText);
        final TextView durationView = findViewById(R.id.DurationText);
        final TextView distanceView = findViewById(R.id.DistanceText);
        final TextView speedView = findViewById(R.id.SpeedText);

        // описываем столбцы бд
        String[] projection = new String[]{
                HistoryProviderContract.ID,
                HistoryProviderContract.DATE,
                HistoryProviderContract.STARTTIME,
                HistoryProviderContract.ENDTIME,
                HistoryProviderContract.DISTANCE,
                HistoryProviderContract.SPEED
        };

        Cursor cursor = getContentResolver().query(HistoryProviderContract.HISTORY_URI, projection, "_id = ?", new String[]{id}, null);
        if (cursor.moveToFirst()) {
            dateView.setText(cursor.getString(1));
            timeView.setText("С " + cursor.getString(2) + " до " + cursor.getString(3));
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

            // на случай, если наступил следующий день
            if (duration < 0) {
                duration = duration + 86400000; // добавляем количество миллисекунд в дне
            }
            durationView.setText(stringForTime(duration));
            distanceView.setText(cursor.getString(4) + " м");
            speedView.setText(cursor.getString(5) + " м/с");
        }
    }

    // делаем красивый вывод времени из милллисекунд разбиваем на части
    private String stringForTime(long duration) {
        int totalSeconds = (int) duration / 1000;
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds/3600;

        if (hours>0) {
            return String.format("%s%s%s%s%s%s", hours, " ч ", minutes, " мин ", seconds, " с");
        } else if (minutes > 0) {
            return String.format("%s%s%s%s", minutes, " мин ", seconds, " с");
        } else{
            return String.format("%s%s", seconds, " с");
        }
    }

    public void deleteRecord(View v){
        Log.d(MY_TAG, "HISTORY DETAIL ON DELETE");
        getContentResolver().delete(HistoryProviderContract.HISTORY_URI, "_id=?", new String[]{id});
        finish();
    }

    @Override
    public void onClick(View view) {
        final int deleteId = R.id.btnDelete;

        switch (view.getId()) {
            case (deleteId):
                deleteRecord(view);
        }
    }
}