package com.akashapplications.security360car.service;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.tedpark.tedpermission.rx2.TedRx2Permission;

import java.util.HashMap;
import java.util.Map;

public class LocationWatcher extends Service {

    double distnaceTravelledInMeters = 0;
    double lat = 0;
    double lon = 0;
    double safeDistance = 0;
    LocationManager locationManager;
    LocationListener locationListener;

    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference reference = database.getReference("parking").child("currentLocation");

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
            lat = extras.getDouble("lat");
            lon = extras.getDouble("lon");
            safeDistance = extras.getDouble("safeDistance");
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onCreate() {
        super.onCreate();

        Toast.makeText(getBaseContext(),"Location service starts", Toast.LENGTH_SHORT).show();

        distnaceTravelledInMeters = 0.0;

        Map<String, Object> locationMap = new HashMap<>();
        locationMap.put("longitude", 0);
        locationMap.put("latitude", 0);
        locationMap.put("distanceMoved", distnaceTravelledInMeters);
        locationMap.put("speed", 0);
        reference.updateChildren(locationMap);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {


                distnaceTravelledInMeters = distance(lat, lon, location.getLatitude(), location.getLongitude()) * 1000;
                Map<String, Object> stolen = new HashMap<>();
                if(distnaceTravelledInMeters > safeDistance)
                {
                    stolen.put("stolen","yes");
                    Toast.makeText(getBaseContext(),"Stolen "+safeDistance, Toast.LENGTH_SHORT).show();
                }
                else
                {
                    stolen.put("stolen","no");
                    Toast.makeText(getBaseContext(),"not Stolen "+safeDistance, Toast.LENGTH_SHORT).show();
                }

                database.getReference("parking").child("status").updateChildren(stolen);

                locationMap.put("longitude", location.getLongitude());
                locationMap.put("latitude", location.getLatitude());
                locationMap.put("distanceMoved", distnaceTravelledInMeters);
                locationMap.put("speed", location.getSpeed());
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
                                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, locationListener);
                            } else {
                                Toast.makeText(getBaseContext(),
                                        "Permission Denied\n" + permissionResult.getDeniedPermissions().toString(), Toast.LENGTH_SHORT)
                                        .show();
                            }
                        }, throwable -> {
                        }
                );

    }

    private double distance(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1))
                * Math.sin(deg2rad(lat2))
                + Math.cos(deg2rad(lat1))
                * Math.cos(deg2rad(lat2))
                * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        return (dist);
    }

    private double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    private double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }


    public class DistanceTravelledBinder extends Binder {
        DistanceTravelledBinder getBinder() {
            return  DistanceTravelledBinder.this;
        }
    }

    public  double getDistnaceTravelledInMeters()
    {
        return distnaceTravelledInMeters;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(getBaseContext(),"Location service stopped", Toast.LENGTH_SHORT).show();
        locationManager.removeUpdates(locationListener);
        locationManager = null;

    }
}
