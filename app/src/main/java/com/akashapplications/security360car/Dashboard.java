package com.akashapplications.security360car;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.akashapplications.security360car.service.FindBuddyService;
import com.akashapplications.security360car.service.FindPhone;
import com.akashapplications.security360car.service.LocationWatcher;
import com.akashapplications.security360car.utilities.Util;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Dashboard extends AppCompatActivity {
    CardView parking;
    TextView location, parkedLocation;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference PARKING_REF = database.getReference("parking");
    TextView parkingSwitch;
    double parkedLat = 0;
    double parkedLon = 0;
    double safeDistance = 0;
    EditText phoneNumber, userName;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        Util.makeStatusBarTextDark(getWindow());

        parking = findViewById(R.id.parking);
        location = findViewById(R.id.location);
        parkingSwitch = findViewById(R.id.indicator);
        parkedLocation = findViewById(R.id.parkedLocation);
        phoneNumber = findViewById(R.id.phone);
        userName = findViewById(R.id.name);

        parking.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("CheckResult")
            @Override
            public void onClick(View view) {

                if(parkedLocation.getText().toString().length() == 0)
                    return;
                if(!isServiceRunning(LocationWatcher.class))
                {
                    parkingSwitch.setBackgroundColor(Color.parseColor("#8BC34A"));
                    startService(new Intent(getBaseContext(), LocationWatcher.class).putExtra("lat",parkedLat).putExtra("lon",parkedLon).putExtra("safeDistance",safeDistance));
                }
                else
                {
                    parkingSwitch.setBackgroundColor(Color.parseColor("#F44336"));
                    stopService(new Intent(getBaseContext(), LocationWatcher.class));
                }
            }
        });

        PARKING_REF.child("parkedLocation").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                parkedLat = (double) dataSnapshot.child("latitude").getValue();
                parkedLon = (double) dataSnapshot.child("longitude").getValue();
                safeDistance = (double) dataSnapshot.child("safeDistance").getValue();
                parkedLocation.setText("latitude: "+parkedLat+"\nlongitude: "+parkedLon+"\nsafe distance: "+safeDistance);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        PARKING_REF.child("currentLocation").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String coord = "";
                for (DataSnapshot childDataSnapshot : dataSnapshot.getChildren()) {
                    Log.i("check",""+childDataSnapshot.getKey() + ":"+dataSnapshot.child(childDataSnapshot.getKey()).getValue());
                    coord += childDataSnapshot.getKey()+" : "+dataSnapshot.child(childDataSnapshot.getKey()).getValue()+"\n";
                }

                location.setText(coord);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        findViewById(R.id.done).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(phoneNumber.getText().toString().length() > 0)
                {
                    if(phoneNumber.getText().toString().length() == 10)
                    {
                        startService(new Intent(getBaseContext(), FindPhone.class).putExtra("phone",phoneNumber.getText().toString()));
                    }
                    else
                        phoneNumber.setError("Phone number invalid");
                }
                else
                    phoneNumber.setError("Enter phone number");
            }
        });
        findViewById(R.id.stopPhone).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopService(new Intent(getBaseContext(), FindPhone.class));
            }
        });

        findViewById(R.id.done2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(userName.getText().toString().length() == 0)
                {
                    userName.setError("Enter name");
                    return;
                }
                startService(new Intent(getBaseContext(), FindBuddyService.class).putExtra("name",userName.getText().toString().replace(" ","").toLowerCase()));
            }
        });

        findViewById(R.id.stopBuddy).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopService(new Intent(getBaseContext(), FindBuddyService.class));
            }
        });
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


}
