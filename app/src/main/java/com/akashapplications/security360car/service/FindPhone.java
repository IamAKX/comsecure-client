package com.akashapplications.security360car.service;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.tedpark.tedpermission.rx2.TedRx2Permission;

import java.util.HashMap;
import java.util.Map;

public class FindPhone extends Service {

    LocationManager locationManager;
    LocationListener locationListener;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference reference = database.getReference("findMyPhone");

    String phone = "";
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        Bundle extras = intent.getExtras();
        if(extras != null){
            phone = extras.getString("phone");
        }
        reference =  database.getReference("findMyPhone").child(phone);
    }

    @SuppressLint({"CheckResult", "MissingPermission"})
    @Override
    public void onCreate() {
        super.onCreate();

        Toast.makeText(getBaseContext(),"Find you phone service starts", Toast.LENGTH_SHORT).show();

        Map<String, Object> locationMap = new HashMap<>();
        locationMap.put("longitude", 0);
        locationMap.put("latitude", 0);


        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                locationMap.put("longitude", location.getLongitude());
                locationMap.put("latitude", location.getLatitude());
                Log.i("check", locationMap.toString());
                reference.updateChildren(locationMap);
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);


        TedRx2Permission.with(getBaseContext())
                .setRationaleTitle("Can we read your Location?")
                .setRationaleMessage("We need your permission to access your current location")
                .setPermissions(Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION)
                .request()
                .subscribe(permissionResult -> {
                            if (permissionResult.isGranted()) {
                                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, locationListener);
                            } else {
                                Toast.makeText(getBaseContext(),
                                        "Permission Denied\n" + permissionResult.getDeniedPermissions().toString(), Toast.LENGTH_SHORT)
                                        .show();
                            }
                        }, throwable -> {
                        }
                );
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(getBaseContext(),"Find Phone service stopped", Toast.LENGTH_SHORT).show();
        locationManager.removeUpdates(locationListener);
        locationManager = null;

    }
}
