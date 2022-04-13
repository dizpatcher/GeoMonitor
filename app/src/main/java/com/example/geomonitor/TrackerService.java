package com.example.geomonitor;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.maps.model.LatLng;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

public class TrackerService extends Service {
    private IBinder binder = null; // Binder given to the client
    private final String CHANNEL_ID = "100"; //channel for the notification
    private final int NOTIFICATION_ID = 001;
    private LocationManager locationManager;
    private MyLocationListener locationListener;
    private LinkedList locationList; //the ArrayList which stores all the location that user run through
    private Date startTime; //the start time of the running track
    private Date endTime; //the end time of the running track
    private float distance; //the total distance of a track
    private float speed; //the average speed
    private boolean isTrackerStart = false;

    /**
     * Class used for the client Binder
     */
    public class MyBinder extends Binder {
        TrackerService getService() {
            return TrackerService.this; //return this instance of service so clients can call public methods
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        binder = new MyBinder();

        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        locationListener = new MyLocationListener();
        try{
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5, 5, locationListener);
        }catch(SecurityException e){
            Log.d("g53mdp", e.toString());
        }

        //add notification
        NotificationManager notificationManager = (NotificationManager)  getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "My running tracker";
            String description = "This is my running tracker";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            notificationManager.createNotificationChannel(channel);
            Intent intent = new Intent(TrackerService.this, MainActivity.class);
            intent.setAction(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentTitle("Running Tracker")
                    .setContentText("Click this to return to running tracker!")
                    .setContentIntent(pendingIntent)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
            notificationManager.notify(NOTIFICATION_ID, mBuilder.build());
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("g53mdp", "service onBind");
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("g53mdp", "service onStartCommand");
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d("g53mdp", "service onDestroy");
        super.onDestroy();
        locationManager.removeUpdates(locationListener);
        locationManager = null;
        locationListener = null;
    }

    @Override
    public void onRebind(Intent intent) {
        Log.d("g53mdp", "service onRebind");
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d("g53mdp", "service onUnbind");
        stopTracking();
        return super.onUnbind(intent);
    }

    public class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            Log.d("g53mdp", location.getLatitude() + " " + location.getLongitude());
            if(isTrackerStart == true){
                Intent intent = new Intent("AddPolyLine");
                Location lastLocation = (Location)locationList.getLast();
                double lastLatitude = lastLocation.getLatitude();
                double lastLongitude = lastLocation.getLongitude();
                LatLng lastLatLng = new LatLng(lastLatitude, lastLongitude);
                intent.putExtra("lastLocation", lastLatLng); //send previous location
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                LatLng newLatLng = new LatLng(latitude, longitude);
                intent.putExtra("newLocation", newLatLng); //send latest location
                intent.putExtra("distance", distance); //send current running distance
                LocalBroadcastManager.getInstance(TrackerService.this).sendBroadcast(intent);//send local broadcast when location changes
                locationList.add(location); //add the latest location to locationList
                distance = distance + location.distanceTo(lastLocation);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // information about the signal, i.e. number of satellites
            Log.d("g53mdp", "onStatusChanged: " + provider + " " + status);
        }

        @Override
        public void onProviderEnabled(String provider) {
            // the user enabled (for example) the GPS
            Log.d("g53mdp", "onProviderEnabled: " + provider);
        }

        @Override
        public void onProviderDisabled(String provider) {
            // the user disabled (for example) the GPS
            Log.d("g53mdp", "onProviderDisabled: " + provider);
        }
    }

    public LocationManager getLocationManager(){
        return locationManager;
    }

    public MyLocationListener getLocationListener(){
        return locationListener;
    }

    public void startTracking(){
        distance = 0; //initiate total running distance
        locationList = new LinkedList(); //instantiate a new list to store all the locations that ran through
        startTime = Calendar.getInstance().getTime();

        //add the start location to locationList
        Location lastKnownLocation = null;
        try {
            lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        } catch (SecurityException e) {
            Log.d("g53mdp", e.toString());
        }
        locationList.add(lastKnownLocation);
        isTrackerStart  = true;
    }

    public void stopTracking(){
        isTrackerStart  = false;
        endTime = Calendar.getInstance().getTime();
        //the difference between start time and end time is the track's duration
        float diffInSec = TimeUnit.MILLISECONDS.toSeconds(endTime.getTime() - startTime.getTime());
        speed = distance/diffInSec;
        ContentValues newValues = new ContentValues();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
        newValues.put(HistoryProviderContract.DATE, dateFormat.format(startTime));
        newValues.put(HistoryProviderContract.STARTTIME,timeFormat.format(startTime));
        newValues.put(HistoryProviderContract.ENDTIME, timeFormat.format(endTime));
        newValues.put(HistoryProviderContract.DISTANCE, distance);
        newValues.put(HistoryProviderContract.SPEED, speed);
        getContentResolver().insert(HistoryProviderContract.HISTORY_URI, newValues);
    }
}