package com.nineclown.lbarsns.service;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

public class GPSService extends Service {

    public class LocalBinder extends Binder {
        public GPSService getService() {
            return GPSService.this;
        }
    }

    private Location mLocation;

    private IBinder mBinder = new LocalBinder();


    public Location getLocation() {
        return mLocation;
    }

    private void setLocation(Location location) {
        mLocation = location;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public GPSService() {
    }


    @Override
    public void onCreate() {
        super.onCreate();
        requestLocationUpdates();

    }

    //Initiate the request to track the device's location//
    public void requestLocationUpdates() {
        LocationRequest request = new LocationRequest();

        //Specify how often your app should request the device’s location//
        // 기기의 위치 정보를 요청한다.
        request.setInterval(10000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // FusedLocationProviderClient Location 값을 가져오기 위해 사용하는 클래스.
        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(this);
        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);

        //If the app currently has access to the location permission...//
        // 권한 확인.
        if (permission == PackageManager.PERMISSION_GRANTED) {
            //...then request location updates//
            // 위치 정보 업데이트를 요청한다.
            client.requestLocationUpdates(request, new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    //Get a reference to the database, so your app can perform read and write operations//
                    Location location = locationResult.getLastLocation();
                    if (location != null) {

                        setLocation(location);
                        //Save the location data to the database//
                        /*System.out.println("Latitude:" + location.getLatitude());
                        System.out.println("Longitude:" + location.getLongitude());
                        System.out.println("Altitude:" + location.getAltitude());*/

                    }
                }
            }, null);
        }
    }


}