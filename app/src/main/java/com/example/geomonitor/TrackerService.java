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

    final String MY_TAG = "GEO_MONITOR";
    private IBinder binder = null; // Binder given to the client
    private LocationManager locationManager;
    private MyLocationListener locationListener;
    private LinkedList locationList; // храним все локации в связном спике
    private Date startTime;
    private Date endTime;
    private float distance;
    private boolean isTrackerStart = false;

    // возрвращаем экземпляр службы для использования извне
    public class MyBinder extends Binder {
        TrackerService getService() {
            return TrackerService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        binder = new MyBinder();

        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        locationListener = new MyLocationListener();
        try{
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5, 20, locationListener); // провайдер, минимальное время обновления, минимальная дистанция, слушатель
        } catch(SecurityException e) {
            Log.d(MY_TAG, e.toString());
        }

        // настройки уведомления
        NotificationManager notificationManager = (NotificationManager)  getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "GEO MONITOR";
            String description = "Учёт физической активности на основе геолокации";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;

            String CHANNEL_ID = "100";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            notificationManager.createNotificationChannel(channel);
            Intent intent = new Intent(TrackerService.this, MainActivity.class);
            intent.setAction(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentTitle("GEO MONITOR")
                    .setContentText("Нажмитете, чтобы вернуться в приложение!")
                    .setContentIntent(pendingIntent)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
            int NOTIFICATION_ID = 001;
            notificationManager.notify(NOTIFICATION_ID, mBuilder.build());
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(MY_TAG, "service onBind");
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(MY_TAG, "service onStartCommand");
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(MY_TAG, "service onDestroy");
        super.onDestroy();
        locationManager.removeUpdates(locationListener);
        locationManager = null;
        locationListener = null;
    }

    @Override
    public void onRebind(Intent intent) {
        Log.d(MY_TAG, "service onRebind");
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(MY_TAG, "service onUnbind");
        stopTracking();
        return super.onUnbind(intent);
    }

    public class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            Log.d(MY_TAG, location.getLatitude() + " " + location.getLongitude());
            if (isTrackerStart && location.hasSpeed() ) {

                Intent intent = new Intent("AddPolyLine");
                Location lastLocation = (Location) locationList.getLast();
                double lastLatitude = lastLocation.getLatitude();
                double lastLongitude = lastLocation.getLongitude();
                LatLng lastLatLng = new LatLng(lastLatitude, lastLongitude);
                intent.putExtra("lastLocation", lastLatLng); // запоминаем предыдущую локацию для отображения
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                LatLng newLatLng = new LatLng(latitude, longitude);
                intent.putExtra("newLocation", newLatLng); // запоминаем текущую локацию
                intent.putExtra("distance", distance); //send current running distance
                LocalBroadcastManager.getInstance(TrackerService.this).sendBroadcast(intent); //send local broadcast when location changes
                locationList.add(location); // сохраняем предыдущую локацию
                distance += location.distanceTo(lastLocation);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // information about the signal, i.e. number of satellites
            Log.d(MY_TAG, "onStatusChanged: " + provider + " " + status);
        }

        @Override
        public void onProviderEnabled(String provider) {
            // the user enabled (for example) the GPS
            Log.d(MY_TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onProviderDisabled(String provider) {
            // the user disabled (for example) the GPS
            Log.d(MY_TAG, "onProviderDisabled: " + provider);
        }
    }

    public LocationManager getLocationManager(){
        return locationManager;
    }


    public void startTracking(){
        distance = 0;
        locationList = new LinkedList(); // список хранения локаций
        startTime = Calendar.getInstance().getTime();

        Location lastKnownLocation = null;
        try {
            lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        } catch (SecurityException e) {
            Log.d(MY_TAG, e.toString());
        }
        locationList.add(lastKnownLocation);
        isTrackerStart  = true;
    }

    // итоги активности
    public void stopTracking(){
        isTrackerStart  = false;
        endTime = Calendar.getInstance().getTime();

        // считаеем время активности
        float avgSpeed = TimeUnit.MILLISECONDS.toSeconds(endTime.getTime() - startTime.getTime());
        //
        float speed = distance / avgSpeed;
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