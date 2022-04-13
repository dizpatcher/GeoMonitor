package com.example.geomonitor;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Matrix;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private int REQUEST_CODE = 1; //request code that used to get permission of access location
    private TrackerService myService = null;
    private boolean threadIsWorking;
    private Thread trackerThread;
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
        mapFragment.getMapAsync(this);
        LocalBroadcastManager.getInstance(this).registerReceiver(newLocationReceiver, new IntentFilter("AddPolyLine"));
        button = findViewById(R.id.StartOrStop);
        timeView = findViewById(R.id.time);
        distanceView = findViewById(R.id.distance);
    }

    private BroadcastReceiver newLocationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            LatLng lastLatLng = (LatLng) intent.getExtras().get("lastLocation");
            LatLng newLatLng = (LatLng) intent.getExtras().get("newLocation");
            mMap.addPolyline(new PolylineOptions().add(lastLatLng, newLatLng).width(10).color(Color.BLUE));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(newLatLng));

            float distance = (float) intent.getExtras().get("distance");
            distanceView.setText(Float.toString(distance) + " m"); //set text view with current running distance
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
            return;
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
            Log.d("g53mdp", e.toString());
        }
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
            Log.d("g53mdp", e.toString());
        }

        double latitude = lastKnownLocation.getLatitude();
        double longitude = lastKnownLocation.getLongitude();
        LatLng latLng = new LatLng(latitude, longitude); //create a new LatLng class
        return latLng;
    }

    /** Defines callbacks as the second parameter of bindService() */
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("g53mdp", "MainActivity onServiceConnected");
            TrackerService.MyBinder binder = ( TrackerService.MyBinder) service;
            myService = binder.getService();
            myService.startTracking();

            //move camera to current location when service is connected
            LatLng currentLatLng = getCurrentLatLng();
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 18));
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("g53mdp", "MainActivity onServiceDisconnected");
            myService.stopTracking();
            myService = null;
        }
    };

    public void onStartStopTracking(View v){
        String status = (String)button.getText();
        Log.d("g53mdp", status);
        if(status.equals("START")){
            final Intent intent = new Intent(this, TrackerService.class);
            startService(intent);
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
            mMap.clear();
            button.setText("STOP");
            timeView.setVisibility(View.VISIBLE);
            distanceView.setVisibility(View.VISIBLE);

            handler = new Handler();
            threadIsWorking = true;//make thread start working
            //a thread that updates the TextView of running time
            trackerThread = new Thread(new Runnable(){
                @Override
                public void run() {
                    int interval = 1000;//the interval of updating progress
                    int runTime = 0;
                    while(threadIsWorking){
                        final TextView timeView = findViewById(R.id.time);
                        try {
                            final int finalRunTime = runTime;
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    timeView.setText(stringForTime(finalRunTime));
                                }
                            });
                            Thread.sleep(interval);
                            runTime = runTime + interval;
                        }catch (InterruptedException e){
                            e.printStackTrace();
                        }
                    }
                }
            });
            trackerThread.start();
        }
        else if(status.equals("STOP")){
            if(myService!=null) {
                unbindService(serviceConnection);
            }
            final Intent intent = new Intent(this, TrackerService.class);
            stopService(intent);
            threadIsWorking = false;//make thread stop working
            button.setText("START");
            timeView.setVisibility(View.INVISIBLE);
            distanceView.setVisibility(View.INVISIBLE);
            distanceView.setText("0.0 m");

            // use toast to remind user that track has been saved
            Toast toast = Toast.makeText(getApplicationContext(), "Your running track has been saved successfully!", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
        else{
            Log.d("g53mdp", "something goes wrong");
        }
    }
    public void onShowingHistory(View v){
        Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
        startActivity(intent);
    }

    //function that return the time in string format given the time in milliseconds
    public String stringForTime(int timeMs){
        int totalSeconds = timeMs / 1000;
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds/3600;
        return Integer.toString(hours) + ":" + Integer.toString(minutes) + ":" + Integer.toString(seconds);
    }

    //function that ask user if they want to exit this app when back button is pressed
    @Override
    public void onBackPressed(){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(R.string.app_name);
        builder.setMessage("Do you want to exit running tracker?");
        builder.setIcon(R.drawable.ic_launcher_foreground);
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                finish();
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setMap();
            }
            else{
                finish();
            }
        }
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        Log.d("g53mdp", "MainActivity onPause");
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        Log.d("g53mdp", "MainActivity onResume");
    }

    @Override
    protected void onStart() {
        // TODO Auto-generated method stub
        super.onStart();
        Log.d("g53mdp", "MainActivity onStart");
    }

    @Override
    protected void onStop() {
        // TODO Auto-generated method stub
        super.onStop();
        Log.d("g53mdp", "MainActivity onStop");
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        Log.d("g53mdp", "MainActivity onDestroy");
        if(myService != null){ //still want to track when exit application
            unbindService(serviceConnection);
        }
    }
}