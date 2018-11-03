package com.nineclown.lbarsns.camera;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.media.ExifInterface;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.Switch;
import android.widget.Toast;

import com.google.ar.core.Pose;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.collision.Ray;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.ar.sceneform.ux.TransformationSystem;
import com.nineclown.lbarsns.R;
import com.nineclown.lbarsns.service.GPSService;

public class ArActivity extends AppCompatActivity implements Scene.OnUpdateListener, Scene.OnPeekTouchListener {
    private static final String TAG = ArActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;

    private ArFragment arFragment;
    private ModelRenderable andyRenderable;
    private Switch modeSwitch;
    private AnchorNode anchorNode;
    private static final float DRAW_DISTANCE = 0.13f;
    private boolean istest = false;
    private boolean isstart = false;
    private GestureDetector trackableGestureDetector;


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

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

        if (!checkIsSupportedDeviceOrFinish(this))
            return;

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
        arFragment.getArSceneView().getScene().addOnPeekTouchListener(this);

        modeSwitch = (Switch) findViewById(R.id.switch_focus_mode);
        modeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("GPS Service", "ArActivity onDestroy 나 죽어~ isService는? : " + isService);


    }

    private Double convertToDegree(String stringDMS) {
        Double result = null;
        String[] DMS = stringDMS.split(",", 3);

        String[] stringD = DMS[0].split("/", 2);
        Double D0 = Double.valueOf(stringD[0]);
        Double D1 = Double.valueOf(stringD[1]);
        Double FloatD = D0 / D1;

        String[] stringM = DMS[1].split("/", 2);
        Double M0 = Double.valueOf(stringM[0]);
        Double M1 = Double.valueOf(stringM[1]);
        Double FloatM = M0 / M1;

        String[] stringS = DMS[2].split("/", 2);
        Double S0 = Double.valueOf(stringS[0]);
        Double S1 = Double.valueOf(stringS[1]);
        Double FloatS = S0 / S1;

        result = FloatD + (FloatM / (double) 60) + (FloatS / (double) 3600);

        return result;


    }

    private void readfile(String srcUri) {
        if (srcUri != null) {
            try {
                ExifInterface srcExif = new ExifInterface(srcUri); // Exif 값이 존재하는 이미지로 ExifInterface 인스턴스 생성
                srcExif.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
                srcExif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
                srcExif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE);
                Double DLatitude = convertToDegree(srcExif.getAttribute(ExifInterface.TAG_GPS_LATITUDE));
                Double DLongitude = convertToDegree(srcExif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE));
                String strAltitude = srcExif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE);
                String[] arrstr = strAltitude.split("/");
                Double DAltitude = Double.valueOf(arrstr[0]);
                Double dnum = Double.valueOf(arrstr[1]);
                DAltitude /= dnum;

                double[] xyz = convertGPSToOpenGL(DLatitude, DLongitude, DAltitude);

                float[] translation = new float[3];
                float[] quaternion = new float[4];
                translation[0] = (float) xyz[0] * 50000;
                translation[1] = (float) xyz[1] * 50000;
                translation[2] = (float) xyz[2] * 50000;
                quaternion[0] = 0;
                quaternion[1] = 0;
                quaternion[2] = 0;
                quaternion[3] = 0;

                Log.d("test translation[0]", ":" + translation[0]);
                Log.d("test translation[1]", ":" + translation[1]);
                Log.d("test translation[2]", ":" + translation[2]);

                ArSceneView arSceneView = arFragment.getArSceneView();
                Pose testpose = new Pose(translation, quaternion);
                anchorNode = new AnchorNode(arFragment.getArSceneView().getSession().createAnchor(testpose));
                anchorNode.setParent(arFragment.getArSceneView().getScene());

                if (andyRenderable == null) {
                    return;
                }

                TransformableNode andy = new TransformableNode(arFragment.getTransformationSystem());
                andy.setParent(anchorNode);
                andy.setRenderable(andyRenderable);
                andy.select();


            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
            Log.d("GPS Service", "AR에서 GPS: lat, lon" + location.getLatitude() + ", " + location.getLongitude());
        }
    }

    private double[] convertGPSToOpenGL(Double Latitude, Double Longitude, Double Altitude) {
        getLocation();
        Double Lati = 0d;
        Double Logi = 0d;
        Double Alti = 0d;
        if (location != null) {
            Lati = (Latitude - location.getLatitude());
            Logi = (Longitude - location.getLongitude());
            Alti = (Altitude - location.getAltitude());
        } else {
            Log.d("location", ": NULL");
        }
//        double x =Math.cos(Logi)*Math.cos(Lati);
//        double z =-Math.sin(Logi)*Math.cos(Lati);
//        double y =Math.sin(Lati)+Alti;
        double[] xyz = new double[3];
        xyz[0] = Lati;
        xyz[1] = Alti;
        xyz[2] = Logi;
        return xyz;
    }

    @Override
    public void onPeekTouch(HitTestResult hitTestResult, MotionEvent tap) {
        int action = tap.getAction();
        Camera camera = arFragment.getArSceneView().getScene().getCamera();
        Ray ray = camera.screenPointToRay(tap.getX(), tap.getY());
        //Vector3 drawPoint = ray.getPoint(DRAW_DISTANCE);

        if (action == MotionEvent.ACTION_DOWN) {
            getLocation();
            ArSceneView arSceneView = arFragment.getArSceneView();
            com.google.ar.core.Camera coreCamera = arSceneView.getArFrame().getCamera();
            if (coreCamera.getTrackingState() != TrackingState.TRACKING) {
                return;
            }

            Pose pose = coreCamera.getPose();
            float[] translation = coreCamera.getPose().getTranslation();
            Log.d("translation[0]", ":" + translation[0]);
            Log.d("translation[1]", ":" + translation[1]);
            Log.d("translation[2]", ":" + translation[2]);
            float[] quaternion = coreCamera.getPose().getRotationQuaternion();
            Log.d("quaternion[0]", ":" + quaternion[0]);
            Log.d("quaternion[1]", ":" + quaternion[1]);
            Log.d("quaternion[2]", ":" + quaternion[2]);
            Log.d("quaternion[3]", ":" + quaternion[3]);

            Ray ray1 = new Ray();
            Log.d("rayDirection", ":" + ray1.getDirection());
            Log.d("rayOrigin", ":" + ray1.getOrigin());
            Log.d("rayDirection", ":" + ray1.getPoint(1.f));


            translation[0] = 1;
            translation[1] = 1;
            translation[2] = 1;
            quaternion[0] = 0;
            quaternion[1] = 0;
            quaternion[2] = 0;
            quaternion[3] = 0;
            Pose testpose = new Pose(translation, quaternion);

            //arSceneView
            anchorNode = new AnchorNode(arSceneView.getSession().createAnchor(pose));
            anchorNode.setParent(arSceneView.getScene());

            Log.d("anchorNode", ":" + anchorNode.getWorldPosition());

            if (andyRenderable == null) {
                return;
            }
            TransformationSystem tr = arFragment.getTransformationSystem();
            //tr.get
            // Create the Anchor.
            //anchorNode.setParent(arFragment.getArSceneView().getScene());

            // Create the transformable andy and add it to the anchor.
            if (!istest) {
                TransformableNode andy = new TransformableNode(arFragment.getTransformationSystem());
                andy.setParent(anchorNode);
                andy.setRenderable(andyRenderable);
                andy.select();
                Log.d("test1", ":" + andy.getWorldPosition());
                istest = true;
            }

        } else if (action == MotionEvent.ACTION_MOVE && andyRenderable != null) {
            //currentStroke.add(drawPoint);
            Log.d("test2", ":" + arFragment.getArSceneView().getScene());

        } else if (action == MotionEvent.ACTION_UP) {
            istest = false;
        }
    }

    @Override
    public void onUpdate(FrameTime frameTime) {
        com.google.ar.core.Camera camera = arFragment.getArSceneView().getArFrame().getCamera();
        if (camera.getTrackingState() == TrackingState.TRACKING) {
            arFragment.getPlaneDiscoveryController().hide();
            if (!isstart) {
                String str = "/storage/emulated/0/TestAR/";
                for (int i = 1; i <= 3; i++) {
                    readfile(str + Integer.toString(i) + ".jpg");
                }
                isstart = true;
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isService) {
            unbindService(conn);
            Log.d("GPS Service", "AR이 바인드 시킨 서비스는 여기서 죽는다 아마도");
            isService = false;
        }
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
