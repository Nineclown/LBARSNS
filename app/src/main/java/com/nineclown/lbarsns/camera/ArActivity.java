package com.nineclown.lbarsns.camera;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.CompoundButton;
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
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.ar.sceneform.ux.TransformationSystem;
import com.nineclown.lbarsns.R;

public class ArActivity extends AppCompatActivity implements Scene.OnUpdateListener, Scene.OnPeekTouchListener, LocationListener {
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

    boolean isGPSEnabled = false;

    // 네트워크 사용유무
    boolean isNetworkEnabled = false;

    // GPS 상태값
    boolean isGetLocation = false;

    Location location;
    double lat; // 위도
    double lon; // 경도

    // 최소 GPS 정보 업데이트 거리 10미터
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 5;

    // 최소 GPS 정보 업데이트 시간 밀리세컨이므로 1분
    private static final long MIN_TIME_BW_UPDATES = 1000 * 5 * 1;

    protected LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_ar);
        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }


        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

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
        modeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                } else {
                    finish();
                    overridePendingTransition(0, 0);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
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

    ;

    public Location getLocation() {
        if (ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            //return null;
        }

        try {
            locationManager = (LocationManager) this
                    .getSystemService(LOCATION_SERVICE);

            // GPS 정보 가져오기
            if (locationManager == null) return null;
            isGPSEnabled = locationManager
                    .isProviderEnabled(LocationManager.GPS_PROVIDER);

            // 현재 네트워크 상태 값 알아오기
            isNetworkEnabled = locationManager
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGPSEnabled && !isNetworkEnabled) {
                // GPS 와 네트워크사용이 가능하지 않을때 소스 구현
                Log.d("gps network", ":");
            } else {
                this.isGetLocation = true;
                // 네트워크 정보로 부터 위치값 가져오기
                if (isNetworkEnabled) {
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

                    if (locationManager != null) {
                        location = locationManager
                                .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if (location != null) {
                            // 위도 경도 저장
                            lat = location.getLatitude();
                            lon = location.getLongitude();
                            Log.d("getLatitude", ":" + lat);
                            Log.d("getLongitude", ":" + lon);
                        }
                    }
                }

                if (isGPSEnabled) {
                    if (location == null) {
                        locationManager.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER,
                                MIN_TIME_BW_UPDATES,
                                MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                        if (locationManager != null) {
                            location = locationManager
                                    .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            if (location != null) {
                                lat = location.getLatitude();
                                lon = location.getLongitude();
                                Log.d("getLatitude", ":" + lat);
                                Log.d("getLatitude", ":" + lon);
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return location;
    }

    public String convertTagGPSFormat(double coordinate) // 인코딩하는 과정으로 GPS 정보를 매개변수로 받음
    {
        String strlatitude = Location.convert(coordinate, Location.FORMAT_SECONDS); // 인코딩하여 포멧을 갖춘다.
        Log.d("strlatitude", ":" + strlatitude);
        String[] arrlatitude = strlatitude.split(":");
        String[] arrstr = arrlatitude[2].split("\\.");
        String abc = null;
        if (arrstr[1].length() >= 3) abc = arrstr[1].substring(0, 3);
        else if (arrstr[1].length() == 2) abc = arrstr[1].substring(0, 2);
        else if (arrstr[1].length() == 1) abc = arrstr[1].substring(0, 1);
        String str = arrstr[0] + abc;
        String str0 = "1";
        for (int i = 0; i < abc.length(); i++) {
            str0 += "0";
        }
        StringBuilder sb = new StringBuilder(); // 갖춘 포멧을 분리하여 새로운 포멧을 적용한다.
        sb.append(arrlatitude[0]);
        sb.append("/1,");
        sb.append(arrlatitude[1]);
        sb.append("/1,");
        sb.append(str);
        sb.append("/" + str0);

        return sb.toString();
    }

    private void readfile(String srcUri) {
        if (srcUri != null) {
            try {
                ExifInterface srcExif = new ExifInterface(srcUri); // Exif 값이 존재하는 이미지로 ExifInterface 인스턴스 생성

                Log.d("srcExif", ":" + srcExif.getAttribute(ExifInterface.TAG_GPS_LATITUDE));
                Log.d("srcExif", ":" + srcExif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE));
                Log.d("srcExif", ":" + srcExif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE));
                srcExif.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
                srcExif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
                srcExif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE);
                Double DLatitude = convertToDegree(srcExif.getAttribute(ExifInterface.TAG_GPS_LATITUDE));
                Double DLongitude = convertToDegree(srcExif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE));
                String strAltitude = srcExif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE);
                Log.d("DLatitude", ":" + DLatitude);
                Log.d("DLongitude", ":" + DLongitude);
                Log.d("strAltitude", ":" + strAltitude);
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
//                    translation[0]=(float)xyz[0];
//                    translation[1]=(float)xyz[1];
//                    translation[2]=(float)xyz[2];
                quaternion[0] = 0;
                quaternion[1] = 0;
                quaternion[2] = 0;
                quaternion[3] = 0;

                Log.d("test translation[0]", ":" + translation[0]);
                Log.d("test translation[1]", ":" + translation[1]);
                Log.d("test translation[2]", ":" + translation[2]);

                ArSceneView arSceneView = arFragment.getArSceneView();
                Scene scene = arSceneView.getScene();


                Vector3 positon = new Vector3(translation[0], translation[1], translation[2]);

                Vector3 direction = new Vector3(0.f, 0.f, -1.f);

//                    TransformableNode node = new TransformableNode(arFragment.getTransformationSystem());
//                    node.setWorldPosition(positon);
//                    node.setLookDirection(direction);
//                    node.setParent(scene);
//                    node.setRenderable(andyRenderable);
//                    node.select();
//                    Log.d("nodeworld",":"+node.getWorldPosition());

//                    Node andy = new Node();
//                    andy.setParent(node);
//                    andy.setRenderable(andyRenderable);
//                    Log.d("andyworld",":"+andy.getWorldPosition());

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

    @Override
    public void onLocationChanged(Location location) {
        // TODO Auto-generated method stub

    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub

    }

    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub

    }

    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub

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

//    private void handleOnTouch(HitTestResult hitTestResult, MotionEvent motionEvent) {
//        // First call ArFragment's listener to handle TransformableNodes.
//        arFragment.onPeekTouch(hitTestResult, motionEvent);
//
//        // Check for touching a Sceneform node
//        if (hitTestResult.getNode() != null) {
//            return;
//        }
//
//        // Otherwise call gesture detector.
//        trackableGestureDetector.onTouchEvent(motionEvent);
//    }
//
//    private void onSingleTap(MotionEvent motionEvent) {
//        Frame frame = arFragment.getArSceneView().getArFrame();
//        if (frame != null && motionEvent != null && frame.getCamera().getTrackingState() == TrackingState.TRACKING) {
//            for (HitResult hit : frame.hitTest(motionEvent)) {
//                Trackable trackable = hit.getTrackable();
//                if (trackable instanceof Plane && ((Plane)trackable).isPoseInPolygon(hit.getHitPose())) {
//                    Plane plane = (Plane)trackable;
//
//                    // Handle plane hits.
//                    break;
//                } else if (trackable instanceof Point) {
//                    // Handle point hits
//                    Point point = (Point) trackable;
//
//                } else if (trackable instanceof AugmentedImage) {
//                    // Handle image hits.
//                    AugmentedImage image = (AugmentedImage) trackable;
//                }
//            }
//        }
//    }

    @Override
    public void onPeekTouch(HitTestResult hitTestResult, MotionEvent tap) {
        int action = tap.getAction();
        Camera camera = arFragment.getArSceneView().getScene().getCamera();
        Ray ray = camera.screenPointToRay(tap.getX(), tap.getY());
        //Vector3 drawPoint = ray.getPoint(DRAW_DISTANCE);

        if (action == MotionEvent.ACTION_DOWN) {
//            if (anchorNode == null) {
//                ArSceneView arSceneView = arFragment.getArSceneView();
//                com.google.ar.core.Camera coreCamera = arSceneView.getArFrame().getCamera();
//                if (coreCamera.getTrackingState() != TrackingState.TRACKING) {
//                    return;
//                }
//                Pose pose = coreCamera.getPose();
//                anchorNode = new AnchorNode(arSceneView.getSession().createAnchor(pose));
//                anchorNode.setParent(arSceneView.getScene());
//            }
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

//            currentStroke = new Stroke(anchorNode, material);
//            strokes.add(currentStroke);
//            currentStroke.add(drawPoint);
        } else if (action == MotionEvent.ACTION_MOVE && andyRenderable != null) {
            //currentStroke.add(drawPoint);
            Log.d("test2", ":" + arFragment.getArSceneView().getScene());
//            anchorNode.setParent(arFragment.getArSceneView().getScene());
//
//            // Create the transformable andy and add it to the anchor.
//            TransformableNode andy = new TransformableNode(arFragment.getTransformationSystem());
//            andy.setParent(anchorNode);
//            andy.setRenderable(andyRenderable);
//            andy.select();
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

    /**
     * Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
     * on this device.
     * <p>
     * <p>Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
     * <p>
     * <p>Finishes the activity if Sceneform can not run
     */
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
