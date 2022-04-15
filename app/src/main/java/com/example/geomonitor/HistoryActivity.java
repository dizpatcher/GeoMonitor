package com.example.geomonitor;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    final String MY_TAG = "GEO_MONITOR";
    SimpleCursorAdapter dataAdapter;
    Handler h = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        Spinner spinner = findViewById(R.id.spinner);
        spinner.setOnItemSelectedListener(this);

        List<String> categories = new ArrayList<>();
        categories.add("date");
        categories.add("start time");
        categories.add("end time");
        categories.add("distance");
        categories.add("speed");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        queryHistory(null);
        // ContentObserver (обозреватель) наблюдает и захватывает изменения в базе данных
        getContentResolver().registerContentObserver(HistoryProviderContract.HISTORY_URI,true, new ChangeObserver(h));
    }

    public void queryHistory(String sortOrder) {
        String[] projection = new String[]{
                HistoryProviderContract.ID,
                HistoryProviderContract.DATE,
                HistoryProviderContract.STARTTIME,
                HistoryProviderContract.ENDTIME,
                HistoryProviderContract.DISTANCE,
                HistoryProviderContract.SPEED
        };

        // данные которые отображаем
        String[] from = new String[]{
                HistoryProviderContract.ID,
                HistoryProviderContract.DATE,
                HistoryProviderContract.STARTTIME,
                HistoryProviderContract.ENDTIME,
                HistoryProviderContract.DISTANCE,
                HistoryProviderContract.SPEED
        };

        // вью, куда отображаем данные
        int[] to = new int[] {
                R.id.idView,
                R.id.dateView,
                R.id.startTimeView,
                R.id.endTimeView,
                R.id.distanceView,
                R.id.speedView
        };

        // отображаем список в соответствии с сортировкой
        Cursor cursor = getContentResolver().query(HistoryProviderContract.HISTORY_URI, projection, null, null, sortOrder);
        dataAdapter = new SimpleCursorAdapter(this, R.layout.item_layout, cursor, from, to, 0);
        final ListView listView = findViewById(R.id.historyList);
        listView.setAdapter(dataAdapter);

        // переход на детальный обзор при клике на элемент
        listView.setOnItemClickListener((myAdapter, myView, position, id) -> {
            View v = listView.getChildAt(position);
            TextView idView = v.findViewById(R.id.idView);
            Bundle bundle = new Bundle();
            bundle.putString("id", idView.getText().toString());
            Intent intent = new Intent(HistoryActivity.this, HistoryDetailActivity.class);
            intent.putExtras(bundle);
            startActivity(intent);
        });
    }


    // опции сортировки
    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
        String item = adapterView.getItemAtPosition(pos).toString();
        switch (item){
            case "date":
                queryHistory(HistoryProviderContract.DATE);
                break;
            case "start time":
                queryHistory(HistoryProviderContract.STARTTIME);
                break;
            case "end time":
                queryHistory(HistoryProviderContract.ENDTIME);
                break;
            case "distance":
                queryHistory(HistoryProviderContract.DISTANCE);
                break;
            case "speed":
                queryHistory(HistoryProviderContract.SPEED);
                break;
            default:
                queryHistory(HistoryProviderContract.ID);
                break;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(MY_TAG, "HistoryActivity onStart");
        queryHistory(null); //change the listView when HistoryActivity restart
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    class ChangeObserver extends ContentObserver {

        public ChangeObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            this.onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            queryHistory(null);
        }
    }
}