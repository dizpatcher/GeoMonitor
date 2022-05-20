package com.example.geomonitor;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, View.OnClickListener {

    final String MY_TAG = "GEO_MONITOR";
    private final int REQUIRED_ACCURACY = 51; // качество GPS сигнала
    private GoogleMap mMap;
    private TrackerService myService = null;
    private boolean threadIsWorking;
    private Handler handler;
    private Button btnManager;
    private TextView tvTime;
    private TextView tvDistance;
    private TextView tvVelocity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // получаем фрагмент карты и ждём вызова метода onMapReady
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null : "Google-карты не загружаются...";
        mapFragment.getMapAsync(this);


        LocalBroadcastManager.getInstance(this).registerReceiver(newLocationReceiver, new IntentFilter("AddPolyLine"));
        Button btnHistory = findViewById(R.id.btnHistory);
        btnManager = findViewById(R.id.btnManager);
        tvTime = findViewById(R.id.tvTime);
        tvVelocity = findViewById(R.id.tvVelocity);
        tvDistance = findViewById(R.id.tvDistance);

        btnManager.setOnClickListener(this);
        btnHistory.setOnClickListener(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            setMap();
        }
    }


    // настройки карты при разрешении
    public void setMap() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        mMap.setMyLocationEnabled(true); // отображения нашего месторасположения
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID); // тип карты (гибрид, спутник и тп)
        mMap.getUiSettings().setZoomControlsEnabled(true); // + и - интерфейса карты

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE); // создаём объект системной службы определения местоположения
        Location lastKnownLocation = null;

        // узнаём локацию с помощью провайдера геопозиционирования GPS
        try {
            lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER); // узнаём локацию с помощью провайдера геопозиционирования GPS
        } catch (SecurityException e) {
            Log.d(MY_TAG, e.toString());
        }
        assert lastKnownLocation != null : "Последняя локация - null";
        double latitude = lastKnownLocation.getLatitude();
        double longitude = lastKnownLocation.getLongitude();
        LatLng currentLatLng = new LatLng(latitude, longitude); // класс широта+долгота для google-карт
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 18));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1 && grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setMap();
        } else {
            Toast.makeText(this, "Нет разрешения на использование геопозиции.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private final BroadcastReceiver newLocationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            float accuracy = (float) intent.getExtras().get("accuracy");
            if (accuracy < REQUIRED_ACCURACY) { // убеждаемся в хорошем качестве сигнала (заранее определён желательный уровень в глобальной переменной)
                LatLng lastLatLng = (LatLng) intent.getExtras().get("lastLocation");
                LatLng newLatLng = (LatLng) intent.getExtras().get("newLocation");
                Polyline polyline = mMap.addPolyline(new PolylineOptions().add(lastLatLng, newLatLng).width(9).color(Color.GREEN));
                polyline.setStartCap(new RoundCap());
                polyline.setJointType(JointType.ROUND);
                polyline.setEndCap(new RoundCap());
//            mMap.addPolyline();
                mMap.moveCamera(CameraUpdateFactory.newLatLng(newLatLng));

                float distance = (float) intent.getExtras().get("distance");
                tvDistance.setText(getString(R.string.cur_distance, distance)); // обновляем текущее расстояние
                float velocity = (float) intent.getExtras().get("velocity");
                if (velocity > 0) {
                    tvVelocity.setText(getString(R.string.cur_velocity, velocity));
                } else {
                    tvVelocity.setText(R.string.zero_velocity);
                }
            } else {
                Toast toast = Toast.makeText(getApplicationContext(), "Вы в помещении? Выйдите на улицу, чтобы улучшить приём сигнала GPS!", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }

        }
    };

    // Объект serviceConnection позволяет нам определить, когда мы подключились к сервису, и когда связь с ним была потеряна.
    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(MY_TAG, "MainActivity onServiceConnected");
            TrackerService.MyBinder binder = (TrackerService.MyBinder) service;
            myService = binder.getService();
            myService.startTracking();

            // устанавлием фокус на текущем местоположении
            LatLng currentLatLng = getCurrentLatLng();
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 18));
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(MY_TAG, "MainActivity onServiceDisconnected");
            myService.stopTracking();
            myService = null;
        }
    };

    // получаем объект долготы и широты из объекта Location
    public LatLng getCurrentLatLng(){
        Location lastKnownLocation = null;
        try {
            lastKnownLocation = myService.getLocationManager().getLastKnownLocation(LocationManager.GPS_PROVIDER);
        } catch (SecurityException e) {
            Log.d(MY_TAG, e.toString());
        }

        assert lastKnownLocation != null : "Последняя локация - null";
        double latitude = lastKnownLocation.getLatitude();
        double longitude = lastKnownLocation.getLongitude();
        return new LatLng(latitude, longitude);
    }

    public void onStartStopTracking(View v){
        String status = (String) btnManager.getText();
        Log.d(MY_TAG, status);
        if (status.equals(getString(R.string.start))) {

            final Intent intent = new Intent(this, TrackerService.class);
            startService(intent);
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
            mMap.clear();
            btnManager.setText(getString(R.string.stop));
            tvTime.setVisibility(View.VISIBLE);
            tvDistance.setVisibility(View.VISIBLE);
            tvVelocity.setVisibility(View.VISIBLE);

            handler = new Handler();
            threadIsWorking = true;
            // в отдельном потоке используя лямбду обновляем время
            Thread trackerThread = new Thread(() -> {
                int interval = 1000;
                int runTime = 0;
                while (threadIsWorking) {
                    try {
                        final int finalRunTime = runTime;
                        handler.post(() -> tvTime.setText(stringForTime(finalRunTime)));
                        Thread.sleep(interval);
                        runTime = runTime + interval;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            trackerThread.start();

        } else {
            if (myService!=null) {
                unbindService(serviceConnection);
            }
            final Intent intent = new Intent(this, TrackerService.class);
            stopService(intent);
            mMap.clear();
            threadIsWorking = false;
            btnManager.setText(getString(R.string.start));
            tvTime.setVisibility(View.INVISIBLE);
            tvDistance.setVisibility(View.INVISIBLE);
            tvVelocity.setVisibility(View.INVISIBLE);

            Toast toast = Toast.makeText(getApplicationContext(), "Ваша активность была успешно записана!", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.BOTTOM, 0, 120);
            toast.show();
        }
    }
    public void onShowingHistory(View v) {
        Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
        startActivity(intent);
    }

    // перевод времени из миллисекунд в строку с разъбиением
    public String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds/3600;
        return getString(R.string.cur_time, hours, minutes, seconds);
    }


    @Override
    public void onClick(View view) {
        final int managerId = R.id.btnManager;
        final int historyId = R.id.btnHistory;

        switch (view.getId()) {
            case managerId:
                onStartStopTracking(view);
                break;
            case historyId:
                onShowingHistory(view);
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(MY_TAG, "MainActivity onPause");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(MY_TAG, "MainActivity onResume");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(MY_TAG, "MainActivity onStart");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(MY_TAG, "MainActivity onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(MY_TAG, "MainActivity onDestroy");
        if (myService != null) { // разрываем соединение при выходе
            unbindService(serviceConnection);
        }
    }
}