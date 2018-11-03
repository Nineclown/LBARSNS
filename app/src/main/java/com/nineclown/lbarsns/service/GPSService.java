package com.nineclown.lbarsns.service;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.FirebaseFirestore;
import com.nineclown.lbarsns.R;
import com.nineclown.lbarsns.model.TravelDTO;
import com.nineclown.lbarsns.sns.MainActivity;

public class GPSService extends Service {
    private LocationRequest request;
    private FusedLocationProviderClient client;
    private LocationCallback callbackBack;
    private LocationCallback callbackFore;

    private Location mLocation;
    private Double mEstimatedLatitude;
    private Double mEstimatedLongitude;
    private FirebaseFirestore mFirestore;
    private String travelId;

    private IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public GPSService getService() {
            return GPSService.this;
        }
    }

    public GPSService() {
    }

    @Override
    public void onCreate() {
        Log.d("GPS Service", "GPS Service 시작");
        super.onCreate();

        // 변수 초기화.
        mFirestore = FirebaseFirestore.getInstance();
        request = new LocationRequest();
        client = LocationServices.getFusedLocationProviderClient(this);
        mEstimatedLatitude = 0d;
        mEstimatedLongitude = 0d;
        callbackBack = null;
        callbackFore = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        requestLocationBackground();
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (callbackBack != null)
            client.removeLocationUpdates(callbackBack);
        return super.onUnbind(intent);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 여기는 바인드로 서비스 실행시키면 들어올 수 없는 영역. 그래서 우리 어플에서 이영역에 들어올 경우는 여행 경로를 녹화할 때 뿐이다.
        Log.d("GPS Service", "여긴 1번만 호출되고 끝날때 1번 호출되서 총 2번만 호출되야하는 곳");
        String requestCode = intent.getStringExtra("travel");
        String nameCode = intent.getStringExtra("name");
        if (requestCode.equals("start")) {
            requestLocationForeground(nameCode);
        } else if (requestCode.equals("end")) {
            if (callbackFore != null)
                client.removeLocationUpdates(callbackFore);
            stopSelf();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    // GPS 정보를 가져오는 메소드
    public void requestLocationBackground() {
        request.setInterval(8000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        callbackBack = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                Log.d("GPS Service", "난 백그라운드에서 Location 정보 받는 중.");
                if (location != null) {
                    //Save the location data to the database//
                    mLocation = location;
                }
            }
        };

        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permission == PackageManager.PERMISSION_GRANTED) {
            client.requestLocationUpdates(request, callbackBack, null);
        }
    }

    // 여행 경로 저장할 때 호출되는 메소드
    public void requestLocationForeground(String name) {
        LocationRequest request;
        FusedLocationProviderClient client;
        request = new LocationRequest();
        client = LocationServices.getFusedLocationProviderClient(this);

        // [Start setup Notification]
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        //RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_service);
        NotificationCompat.Builder builder;
        if (Build.VERSION.SDK_INT >= 26) {
            String CHANNEL_ID = "gps_service_channel";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "GPS Service Channel", NotificationManager.IMPORTANCE_DEFAULT);
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

            builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        } else {
            builder = new NotificationCompat.Builder(this);
        }
        builder.setSmallIcon(R.drawable.push_icon)
                .setContentTitle("Lbarsns")
                .setContentText("여행 경로 녹화중...")
                .setContentIntent(pendingIntent);

        startForeground(1, builder.build());
        // [End setup Notification]


        // [Start Upload Location data to Firebase]
        request.setInterval(20000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // 서버에서 name 에 해당하는 문서를 가져와야 함.
        mFirestore.collection("travels").whereEqualTo("travelId", name).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // 어차피 문서는 하나 가져오기 때문에.
                travelId = task.getResult().getDocuments().get(0).getId();
            }
        });

        callbackFore = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                //Get a reference to the database, so your app can perform read and write operations//
                Location location = locationResult.getLastLocation();
                Log.d("GPS Service", "포그라운드에서 Location 정보 받는 중.");
                if (location != null) {
                    //Save the location data to the database//
                    // 서버에 등록하는데 무조건 등록하는게 아니라 history 방식으로 넣어야 함.
                    compansateGPS(location);
                }
            }
        };

        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permission == PackageManager.PERMISSION_GRANTED) {
            client.requestLocationUpdates(request, callbackFore, null);
        }
        // [End Upload Location data to Firebase]
    }

    private void compansateGPS(Location sampleLocation) {
        Double tmpLatitude = sampleLocation.getLatitude();
        Double tmpLongitude = sampleLocation.getLongitude();
        Location estimatedLocation = new Location("estimated");
        // 처음에 값을 받아올 때,
        if (mEstimatedLongitude == 0d) {
            mEstimatedLatitude = tmpLatitude;
            mEstimatedLongitude = tmpLongitude;

            TravelDTO.LatLon latLon = new TravelDTO.LatLon();
            latLon.setLatitude(mEstimatedLatitude);
            latLon.setLongitude(mEstimatedLongitude);
            latLon.setTimestamp(System.currentTimeMillis());
            Log.d("Travel", "데이터 들어간다~");

            if (travelId != null) {
                mFirestore.collection("travels")
                        .document(travelId).collection("locations").document().set(latLon);
            }
        }
        // 이후에 값을 받아올 때,
        else {
            // 일단 history 의 lat, lon 을 Location 으로 바꿈
            estimatedLocation.setLatitude(mEstimatedLatitude);
            estimatedLocation.setLongitude(mEstimatedLongitude);

            // sample 과 estimated 를 비교해서 범위를 넘어서면 버린다.
            if (sampleLocation.distanceTo(estimatedLocation) > 150) {
                // nothing to do.
                Log.d("Travel", "여기에 오는 경우가 있음?");

            }
            // valid 값인 경우 값을 수정하고 이 값을 DB에 저장한다.
            else {
                Double alpha = 0.125d;
                mEstimatedLatitude = (1 - alpha) * mEstimatedLatitude + alpha * sampleLocation.getLatitude();
                mEstimatedLongitude = (1 - alpha) * mEstimatedLongitude + alpha * sampleLocation.getLongitude();

                TravelDTO.LatLon latLon = new TravelDTO.LatLon();
                latLon.setLatitude(mEstimatedLatitude);
                latLon.setLongitude(mEstimatedLongitude);
                latLon.setTimestamp(System.currentTimeMillis());
                Log.d("Travel", "데이터 들어간다~");
                if (travelId != null) {
                    mFirestore.collection("travels")
                            .document(travelId).collection("locations").document().set(latLon);
                }
            }
        }
    }

    // GPS 정보를 다은 컴포넌트에게 뿌려줄 때 사용하는 메소드(bind 된 경우에 사용)
    public Location getLocation() {
        return mLocation;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (callbackFore != null)
            client.removeLocationUpdates(callbackFore);
        if (callbackBack != null)
            client.removeLocationUpdates(callbackBack);
    }
}