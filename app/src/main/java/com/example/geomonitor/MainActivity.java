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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    final String MY_TAG = "GEO_MONITOR";
    private GoogleMap mMap;
    private final int REQUEST_CODE = 1; // код успешного получения разрешения
    private TrackerService myService = null;
    private boolean threadIsWorking;
    private Handler handler;
    private Button button;
    private TextView timeView;
    private TextView distanceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        assert mapFragment != null : "Google-карты не загружаются...";
        mapFragment.getMapAsync(this);
        LocalBroadcastManager.getInstance(this).registerReceiver(newLocationReceiver, new IntentFilter("AddPolyLine"));
        button = findViewById(R.id.StartOrStop);
        timeView = findViewById(R.id.time);
        distanceView = findViewById(R.id.distance);
    }

    private final BroadcastReceiver newLocationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            LatLng lastLatLng = (LatLng) intent.getExtras().get("lastLocation");
            LatLng newLatLng = (LatLng) intent.getExtras().get("newLocation");
            mMap.addPolyline(new PolylineOptions().add(lastLatLng, newLatLng).width(10).color(Color.BLUE));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(newLatLng));

            float distance = (float) intent.getExtras().get("distance");
            distanceView.setText(getString(R.string.cur_distance, distance)); //set text view with current running distance
        }
    };

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]
                    {Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
        } else {
            setMap();
        }
    }

    //set map when permission is granted
    public void setMap() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Location lastKnownLocation = null;
        try {
            lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        } catch (SecurityException e) {
            Log.d(MY_TAG, e.toString());
        }
        assert lastKnownLocation != null : "Последняя локация - null";
        double latitude = lastKnownLocation.getLatitude();
        double longitude = lastKnownLocation.getLongitude();
        LatLng currentLatLng = new LatLng(latitude, longitude); //create a new LatLng class
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 18));
    }

    // function that get the current location and return it in LatLng form
    public LatLng getCurrentLatLng(){
        Location lastKnownLocation = null;
        try {
            lastKnownLocation = myService.getLocationManager().getLastKnownLocation(LocationManager.GPS_PROVIDER);
        } catch (SecurityException e) {
            Log.d(MY_TAG, e.toString());
        }

        assert lastKnownLocation != null;
        double latitude = lastKnownLocation.getLatitude();
        double longitude = lastKnownLocation.getLongitude();
        return new LatLng(latitude, longitude);
    }

    /** Defines callbacks as the second parameter of bindService() */
    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(MY_TAG, "MainActivity onServiceConnected");
            TrackerService.MyBinder binder = ( TrackerService.MyBinder) service;
            myService = binder.getService();
            myService.startTracking();

            //move camera to current location when service is connected
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

    public void onStartStopTracking(View v){
        String status = (String) button.getText();
        Log.d(MY_TAG, status);
        if (status.equals(getString(R.string.start))) {

            final Intent intent = new Intent(this, TrackerService.class);
            startService(intent);
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
            mMap.clear();
            button.setText(getString(R.string.stop));
            timeView.setVisibility(View.VISIBLE);
            distanceView.setVisibility(View.VISIBLE);

            handler = new Handler();
            threadIsWorking = true;//make thread start working
            //a thread that updates the TextView of running time
            //the interval of updating progress
            Thread trackerThread = new Thread(() -> { // лямбда функцией создаём поток для времени
                int interval = 1000;//the interval of updating progress
                int runTime = 0;
                while (threadIsWorking) {
                    final TextView timeView = findViewById(R.id.time);
                    try {
                        final int finalRunTime = runTime;
                        handler.post(() -> timeView.setText(stringForTime(finalRunTime)));
                        Thread.sleep(interval);
                        runTime = runTime + interval;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            trackerThread.start();
        }
        else {
            if (myService!=null) {
                unbindService(serviceConnection);
            }
            final Intent intent = new Intent(this, TrackerService.class);
            stopService(intent);
            threadIsWorking = false;//make thread stop working
            button.setText(getString(R.string.start));
            timeView.setVisibility(View.INVISIBLE);
            distanceView.setVisibility(View.INVISIBLE);
            distanceView.setText(getString(R.string.cur_distance));

            // use toast to remind user that track has been saved
            Toast toast = Toast.makeText(getApplicationContext(), "Ваша активность была успешно записа!", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
    }
    public void onShowingHistory(View v){
        Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
        startActivity(intent);
    }

    // function that return the time in string format given the time in milliseconds
    public String stringForTime(int timeMs){
        int totalSeconds = timeMs / 1000;
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds/3600;
        return getString(R.string.cur_time, hours, minutes, seconds);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setMap();
            }
            else{
                Log.d(MY_TAG, "NOT REQUEST CODE");
                finish();
            }
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
        if (myService != null) { //still want to track when exit application
            unbindService(serviceConnection);
        }
    }
}