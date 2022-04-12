package com.example.geomonitor;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements LocListenerInterface {

    private LocationManager locationManager;
    private MyLocListener myLocListener;
    private Location lastLocation;
    private int distance;

    private TextView tvDistance;
    private TextView tvVelocity;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initLocServices();

        tvDistance = findViewById(R.id.tvDistance);
        tvVelocity = findViewById(R.id.tvVelocity);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setMax(10000);
    }

    private void initLocServices() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        myLocListener = new MyLocListener();
        myLocListener.setLocListenerInterface(this);
        checkPermissions();
    }

    private void showDialog() {
        AlertDialog.Builder adBuilder = new AlertDialog.Builder(this);
        adBuilder.setTitle(R.string.dialog_title);
        ConstraintLayout cl = (ConstraintLayout) getLayoutInflater().inflate(R.layout.dialog, null);
        adBuilder.setPositiveButton(R.string.dialog_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                AlertDialog ad = (AlertDialog) dialogInterface;
                EditText ed = ad.findViewById(R.id.ptReqDist);
                if (ed != null && ed.getText().toString().equals("")) {
                    progressBar.setMax(Integer.parseInt(ed.getText().toString()));
                }

            }
        });
        adBuilder.setView(cl);
        adBuilder.show();

    }

    public void onClickDistance(View view) {
        showDialog();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if ( requestCode == 1 && grantResults[0] == RESULT_OK ) {
            checkPermissions();
        } else {
            Toast.makeText(this, "Нет разрешения на использование геопозиции.", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkPermissions() {

        // проверяем 1) версию Android (нужно спрашивать при > 6) 2) не даны ли были разрешения ранее
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // запрашиваем разрешения (код 1 выносит нужные нам разрешения в группу 1)
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2, 100, myLocListener); // провайдер, минимальное время обновления, минимальная дистанция, слушатель
        }
    }

    @Override
    public void OnLocationChanged(Location location) {
        // проверяем есть ли скорость у устройства - движемся ли мы на самом деле
        if (location.hasSpeed() && location != null) {
            distance += lastLocation.distanceTo(location);
        }
        lastLocation = location;
        tvDistance.setText(String.valueOf((distance)));
        tvVelocity.setText(String.valueOf(location.getSpeed()));

    }
}