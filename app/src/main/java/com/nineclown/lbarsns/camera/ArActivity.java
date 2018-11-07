package com.nineclown.lbarsns.camera;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.databinding.DataBindingUtil;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.ar.core.Pose;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.nineclown.lbarsns.R;
import com.nineclown.lbarsns.databinding.ActivityArBinding;
import com.nineclown.lbarsns.model.ContentDTO;
import com.nineclown.lbarsns.service.GPSService;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public class ArActivity extends AppCompatActivity implements Scene.OnUpdateListener, SensorEventListener {
    private static final String TAG = ArActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;

    private ActivityArBinding binding;

    private ArFragment arFragment;
    private ModelRenderable andyRenderable;
    private static final float DRAW_DISTANCE = 0.13f;
    private boolean istest = false;
    private boolean isstart = false;

    private final double earthR = 20d * 6371d;
    private final double minM = 0.00001d;

    //////////////////////////////////////
    private ArrayList<AnchorNode> anchorNodeCollection;
    private SensorManager mSensorManager;
    private float[] quatenionAndroidPose;
    private float[] startTrackingStopQuaternion;
    private float[] startTrackingQuaternion;
    private Quaternion rotationQuaternionY;

    private FirebaseFirestore mFirestore;
    private ArrayList<ContentDTO> contentDTOs;
    private ListenerRegistration imageListenerRegistration;

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, GPSService.class);
        bindService(intent, conn, BIND_AUTO_CREATE);
        Log.d("GPS Service", "AR이 service 시작");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_ar);
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

        if (!checkIsSupportedDeviceOrFinish(this))
            return;

        // [Start initiate Firebase & variable]
        mFirestore = FirebaseFirestore.getInstance();
        contentDTOs = new ArrayList<>();
        anchorNodeCollection = new ArrayList<>();
        // [End initiate Firebase & variable]


        // 서버에서 데이터를 받아오는 부분. contentDTO 안에 이미지의 url, 위도, 경도 값이 들어있다,
        imageListenerRegistration = mFirestore.collection("images")
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    contentDTOs.clear();
                    if (queryDocumentSnapshots == null) return;
                    for (DocumentSnapshot snapshot : queryDocumentSnapshots.getDocuments()) {
                        contentDTOs.add(snapshot.toObject(ContentDTO.class));
                    }
                });


        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_UI);


        arFragment = (ExArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

        ModelRenderable.builder()
                .setSource(this, R.raw.andy)
                .build()
                .thenAccept(renderable -> andyRenderable = renderable)
                .exceptionally(
                        throwable -> {
                            Toast toast =
                                    Toast.makeText(this, "Unable to load andy renderable", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            return null;
                        });
        arFragment.getPlaneDiscoveryController().hide();
        arFragment.getPlaneDiscoveryController().setInstructionView(null);
        arFragment.getArSceneView().getScene().addOnUpdateListener(this);

        binding.switchFocusMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
            } else {
                finish();
                overridePendingTransition(0, 0);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("GPS Service", "ArActivity onDestroy 나 죽어~ isService는? : " + isService);
    }

    private double[] convertGPSToOpenGL(Double Latitude, Double Longitude) {
        while (location == null) {
            getLocation();
        }

        double x0 = earthR * Longitude;
        double z0 = earthR * Latitude;
        double x = earthR * location.getLongitude();
        double z = earthR * location.getLatitude();
        double[] xyz = new double[3];
        xyz[0] = (x0 - x);
        xyz[1] = 0;
        xyz[2] = -(z0 - z);
        return xyz;
    }

    GPSService mGpsService;
    boolean isService = false;

    ServiceConnection conn = new ServiceConnection() {
        public void onServiceConnected(ComponentName name,
                                       IBinder service) {
            // 서비스와 연결되었을 때 호출되는 메서드
            // 서비스 객체를 전역변수로 저장
            GPSService.LocalBinder binder = (GPSService.LocalBinder) service;
            mGpsService = binder.getService(); // 서비스가 제공하는 메소드 호출하여
            // 서비스쪽 객체를 전달받을수 있슴
            isService = true;
        }

        public void onServiceDisconnected(ComponentName name) {
            // 서비스와 연결이 끊겼을 때 호출되는 메서드
            isService = false;
        }
    };

    private Location location;

    private void getLocation() {
        if (isService) {
            location = mGpsService.getLocation();
            if (location == null) return;
            Log.d("GPS Service", "AR에서 GPS: lat, lon" + location.getLatitude() + ", " + location.getLongitude());
        }
    }

    @Override
    public void onUpdate(FrameTime frameTime) {
        com.google.ar.core.Camera camera = arFragment.getArSceneView().getArFrame().getCamera();
        getLocation();

        if (camera.getTrackingState() == TrackingState.TRACKING) {
            arFragment.getPlaneDiscoveryController().hide();
            Pose pose = arFragment.getArSceneView().getArFrame().getAndroidSensorPose();
            quatenionAndroidPose = pose.getRotationQuaternion();
            Vector3 vec3 = arFragment.getArSceneView().getScene().getCamera().getWorldPosition();


            if (anchorNodeCollection != null) {
                for (AnchorNode anchorNode1 : anchorNodeCollection) {
                    if (anchorNode1.getChildren().size() == 0) break;
                    if (anchorNode1.getChildren().get(0).getChildren().size() == 0) break;
                    Vector3 vector3 = anchorNode1.getChildren().get(0).getWorldPosition();
                    anchorNode1
                            .getChildren().get(0)
                            .getChildren().get(0)
                            .setWorldRotation(Quaternion
                                    .lookRotation(new Vector3(vec3.x - vector3.x,
                                                    vec3.y - (vector3.y + 0.25f),
                                                    vec3.z - vector3.z),
                                            new Vector3(0f, 1f, 0f)));

                }
            }

            if (!istest) {
                quatenionAndroidPose = new float[4];
                pose = arFragment.getArSceneView().getArFrame().getAndroidSensorPose();
                quatenionAndroidPose = pose.getRotationQuaternion();
                startTrackingStopQuaternion = new float[4];
                startTrackingStopQuaternion[0] = startTrackingQuaternion[1];
                startTrackingStopQuaternion[1] = startTrackingQuaternion[2];
                startTrackingStopQuaternion[2] = startTrackingQuaternion[3];
                startTrackingStopQuaternion[3] = startTrackingQuaternion[0];


                Quaternion quaternion2;
                Quaternion quaternion3 = new Quaternion(startTrackingQuaternion[1],
                        startTrackingQuaternion[2],
                        startTrackingQuaternion[3],
                        startTrackingQuaternion[0]);
                quaternion2 = Quaternion.multiply((new Quaternion(quatenionAndroidPose[0],
                                quatenionAndroidPose[1],
                                quatenionAndroidPose[2],
                                quatenionAndroidPose[3])),
                        quaternion3.inverted());
                //Quaternion quaternion4 = Quaternion.multiply(Quaternion.multiply(var1, var2), var3);

                double getAngle;
                if (quaternion2.w < 0f) {
                    getAngle = (Math.PI - Math.asin(quaternion2.y)) * 2d;
                } else {
                    getAngle = Math.asin(quaternion2.y) * 2d;
                }
                if (getAngle == 0d) {
                    //pose_tv.setText("실패했어!!!!!!!!망했어!!!!!!!!!");
                } else {
                    //pose_tv.setText("성공했어!!!!!!!!!! " + (180d*getAngle)/Math.PI);
                    rotationQuaternionY = Quaternion.axisAngle(new Vector3(0f, 1f, 0f),
                            (float) (180d * getAngle) / (float) Math.PI);
                }
                istest = true;
            }

            if (!isstart) {
                Log.d("TRAVELIMAGE", "여기선 받아와야해: " + contentDTOs.size());
                for (int i = 0; i < contentDTOs.size(); i++) {
                    //readfile(i);
                    Double DLatitude = contentDTOs.get(i).getLatitude();
                    Double DLongitude = contentDTOs.get(i).getLongitude();

                    if (contentDTOs.get(i).getLatitude() == null || contentDTOs.get(i).getLongitude() == null)
                        return;

                    double[] xyz = convertGPSToOpenGL(DLatitude, DLongitude);

                    float[] translation = new float[3];
                    translation[0] = (float) xyz[0];
                    translation[1] = (float) xyz[1];
                    translation[2] = (float) xyz[2];


                    float[] quaternion = new float[]{0f, 0f, 0f, 0f};
                    Vector3 vector3 = new Vector3(translation[0], translation[1], translation[2]);
                    if (rotationQuaternionY != null) {
                        vector3 = Quaternion.rotateVector(rotationQuaternionY, vector3);
                    }

                    translation[0] = vector3.x;
                    translation[1] = vector3.y;
                    translation[2] = vector3.z;
                    Pose testpose = new Pose(translation, quaternion);
                    anchorNodeCollection.add(new AnchorNode(arFragment.getArSceneView().getSession().createAnchor(testpose)));

                    ((AnchorNode) anchorNodeCollection.toArray()[anchorNodeCollection.size() - 1]).
                            setParent(arFragment.getArSceneView().getScene());
                    if (andyRenderable == null) {
                        return;
                    }
                    TransformableNode andy = new TransformableNode(arFragment.getTransformationSystem());
                    andy.setParent(((AnchorNode) anchorNodeCollection.toArray()[anchorNodeCollection.size() - 1]));
                    andy.setRenderable(andyRenderable);
                    andy.setName("test");
                    int a = i;
                    Node viewPanel = new Node();
                    CompletableFuture<ViewRenderable> future = ViewRenderable.builder().setView(this, R.layout.image_view).build();
                    future.thenAccept(view -> {
                        view.getView().setMinimumHeight(100);
                        view.getView().setMinimumWidth(100);

                        TextView headline = view.getView().findViewById(R.id.imagetext);

                        String temp = Integer.toString(a);
                        headline.setText(temp);
                        ImageView imageView = view.getView().findViewById(R.id.imageview);


                        viewPanel.setRenderable(view);
                        viewPanel.setParent(andy);

                        viewPanel.setLocalPosition(new Vector3(0.f, 0.25f, 0f));
                        viewPanel.setEnabled(!viewPanel.isEnabled());
                        Glide.with(this).load(contentDTOs.get(a).getImageUrl()).into(imageView);
                    });
                    andy.setOnTapListener(((hitTestResult, motionEvent) -> {
                        viewPanel.setEnabled(!viewPanel.isEnabled());
                    }));
                }
                isstart = true;
            }
        }
    }

    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ROTATION_VECTOR:
                startTrackingQuaternion = new float[4];

                SensorManager.getQuaternionFromVector(startTrackingQuaternion, event.values);
                Quaternion testQuaternion = Quaternion.axisAngle(new Vector3(1f, 0f, 0f), -90f);
                Quaternion testQuaternion1 = new Quaternion(startTrackingQuaternion[1],
                        startTrackingQuaternion[2],
                        startTrackingQuaternion[3],
                        startTrackingQuaternion[0]);
                testQuaternion1 = Quaternion.multiply(testQuaternion1, testQuaternion);
                startTrackingQuaternion[1] = testQuaternion1.x;
                startTrackingQuaternion[2] = testQuaternion1.z;
                startTrackingQuaternion[3] = testQuaternion1.y;
                startTrackingQuaternion[0] = testQuaternion1.w;
                break;
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isService) {
            unbindService(conn);
            Log.d("GPS Service", "AR이 바인드 시킨 서비스는 여기서 죽는다 아마도");
            isService = false;
        }
        if (imageListenerRegistration != null)
            imageListenerRegistration.remove();
    }

    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        String openGlVersionString =
                ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                    .show();
            activity.finish();
            return false;
        }
        return true;
    }
}