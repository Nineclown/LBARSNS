package com.nineclown.lbarsns.camera;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.media.ExifInterface;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.ar.core.Anchor;
import com.google.ar.core.Pose;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.collision.Ray;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.ar.sceneform.ux.TransformationSystem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.nineclown.lbarsns.R;
import com.nineclown.lbarsns.model.ContentDTO;
import com.nineclown.lbarsns.service.GPSService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ArActivity extends AppCompatActivity implements Scene.OnUpdateListener, SensorEventListener {
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

    private final double earthR = 5d * 6371d;
    private final double minM = 0.00001d;
    private Vector3 vectorX;
    private Vector3 vectorZ;

    //////////////////////////////////////
    private ArrayList<AnchorNode> anchorNodeCollection = new ArrayList<AnchorNode>();
    private ArrayList<ViewRenderable> imageViewRenderableCollection = new ArrayList<ViewRenderable>();
    private ModelRenderable redBoxRenderable;
    private ModelRenderable blueBoxRenderable;
    private ModelRenderable greenBoxRenderable;
    private TextView pose_tv;
    private TextView displaypose_tv;
    private SensorManager mSensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private float[] mGravity;
    private float[] mGeomagnetic;
    private float[] rotationVals;
    private float[] rotationVector;
    private float[] rotationMatrix;
    private float[] quatenionAndroidPose;
    private float[] startTrackingStopQuaternion;
    private float[] startTrackingQuaternion;
    private Quaternion rotationQuaternionY;

    private ViewRenderable imageViewRenderable;

    private float[] orientation;
    private Float azimut; // View to draw a compass


    private FirebaseFirestore mFirestore;
    private FirebaseAuth mAuth;
    private String mUid;
    private ArrayList<String> imageUrls;
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

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

        if (!checkIsSupportedDeviceOrFinish(this))
            return;

        // [Start Firebase]
        mFirestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        mUid = mAuth.getCurrentUser().getUid();
        imageUrls = new ArrayList<>();
        contentDTOs = new ArrayList<>();
        // [End Firebase]


        // 서버에서 데이터를 받아오는 부분. contentDTO 안에 이미지의 url, 위도, 경도 값이 들어있다,

        imageListenerRegistration = mFirestore.collection("images")
                .whereEqualTo("uid", mUid).addSnapshotListener((queryDocumentSnapshots, e) -> {
                    contentDTOs.clear();
                    if (queryDocumentSnapshots == null) return;
                    for (DocumentSnapshot snapshot : queryDocumentSnapshots.getDocuments()) {
                        contentDTOs.add(snapshot.toObject(ContentDTO.class));
                    }
                });


        // [Start 새로 추가된 부분]
        CompletableFuture<ViewRenderable> viewStage = ViewRenderable.builder().setView(this, R.layout.image_view).build();
        CompletableFuture.allOf(viewStage)
                .handle((notUsed, throwable) -> {
                    if (throwable != null) {
                        return null;
                    }
                    try {
                        imageViewRenderable = viewStage.get();
                    } catch (InterruptedException | ExecutionException ex) {

                    }
                    return null;
                });

        pose_tv = findViewById(R.id.pose);
        displaypose_tv = findViewById(R.id.displaypose);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = mSensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager
                .getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_UI);


        arFragment = (ExArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

        MaterialFactory.makeOpaqueWithColor(this, new Color(android.graphics.Color.RED))
                .thenAccept(
                        material -> {
                            redBoxRenderable =
                                    ShapeFactory.makeCube(new Vector3(0.1f, 0.01f, 0.01f), new Vector3(0.05f, 0.0f, 0.0f), material);
                        });
        MaterialFactory.makeOpaqueWithColor(this, new Color(android.graphics.Color.GREEN))
                .thenAccept(
                        material -> {
                            greenBoxRenderable =
                                    ShapeFactory.makeCube(new Vector3(0.01f, 0.1f, 0.01f), new Vector3(0.0f, 0.05f, 0.0f), material);
                        });
        MaterialFactory.makeOpaqueWithColor(this, new Color(android.graphics.Color.BLUE))
                .thenAccept(
                        material -> {
                            blueBoxRenderable =
                                    ShapeFactory.makeCube(new Vector3(0.01f, 0.01f, 0.1f), new Vector3(0.0f, 0.0f, 0.05f), material);
                        });
        // [End 새로 추가된 부분]


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
        //arFragment.getArSceneView().getScene().addOnPeekTouchListener(this);

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


    private void createAndy(float[] translation, int position) {
        float[] quaternion = new float[]{0f, 0f, 0f, 0f};
        Vector3 vector3 = new Vector3(translation[0], translation[1], translation[2]);
        if (rotationQuaternionY != null) {
            vector3 = Quaternion.rotateVector(rotationQuaternionY, vector3);
        }

        translation[0] = vector3.x;
        translation[1] = vector3.y;
        translation[2] = vector3.z;
        //float[] quaternion = new float[]{orientation[0],orientation[1],orientation[2],orientation[3]};
        Pose testpose = new Pose(translation, quaternion);
        anchorNodeCollection.add(new AnchorNode(arFragment.getArSceneView().getSession().createAnchor(testpose)));

        ((AnchorNode) anchorNodeCollection.toArray()[anchorNodeCollection.size() - 1]).setParent(arFragment.getArSceneView().getScene());
        //Log.d("anchorNode",":"+anchorNode.getWorldRotation());
        if (andyRenderable == null) {
            return;
        }
        //displaypose_tv.setText(anchorNode.getWorldPosition().toString());
        TransformableNode andy = new TransformableNode(arFragment.getTransformationSystem());
        andy.setParent(((AnchorNode) anchorNodeCollection.toArray()[anchorNodeCollection.size() - 1]));
        andy.setRenderable(andyRenderable);
        andy.setName("test");
        //andy.select();

        imageViewRenderableCollection.add(imageViewRenderable.makeCopy());

        Node viewPanel = new Node();
        viewPanel.setParent(andy);
        Log.d("째현 망했어", ":" + imageViewRenderableCollection.size());
        viewPanel.setRenderable(((ViewRenderable) imageViewRenderableCollection.toArray()[imageViewRenderableCollection.size() - 1]));
        viewPanel.setLocalPosition(new Vector3(0.f, 0.25f, 0f));
        viewPanel.setEnabled(!viewPanel.isEnabled());
        View testView = ((ViewRenderable) imageViewRenderableCollection.toArray()[imageViewRenderableCollection.size() - 1]).getView();
        TextView textView = testView.findViewById(R.id.imagetext);
        textView.setText("test");
        ImageView imageView = testView.findViewById(R.id.imageview);
        //Uri uri = Uri.parse(name);
        //imageView.setImageURI(uri);
      /*  File f = new File(name);
        if (f.isFile()) {
            Log.d("망했어 재현~!~!", ":");
            Bitmap bitmap = BitmapFactory.decodeFile(name);
            ExifInterface exif = null;
            try {
                exif = new ExifInterface(name);
            } catch (IOException e) {
                e.printStackTrace();
            }
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
            Bitmap bmRotated = rotateBitmap(bitmap, orientation);
*/


      Log.d("TRAVELIMAGE", "머로 가져와?: " + contentDTOs.get(position).getImageUrl());

      //imageView.setImageResource(R.drawable.ic_favorite);

      Glide.with(this).load(contentDTOs.get(position).getImageUrl()).into(imageView);
        // imageView.setImageBitmap(bmRotated);
//            Uri uri = Uri.parse(name);
//            imageView.setImageURI(uri);
        //Log.d("imageheight", ":" + imageView.getMaxHeight());
//        }
        andy.setOnTapListener(((hitTestResult, motionEvent) -> {
            viewPanel.setEnabled(!viewPanel.isEnabled());
        }));
    }

    public void createCoordinate() {
        float[] quaternion = new float[]{0f, 0f, 0f, 0f};
        float[] translation = new float[]{0f, 0f, 0f};
        //float[] quaternion = new float[]{orientation[0],orientation[1],orientation[2],orientation[3]};
        Pose testpose = new Pose(translation, quaternion);
        anchorNode = new AnchorNode(arFragment.getArSceneView().getSession().createAnchor(testpose));


        anchorNode.setParent(arFragment.getArSceneView().getScene());
        Log.d("anchorNode", ":" + anchorNode.getWorldRotation());
        if (greenBoxRenderable == null || redBoxRenderable == null || blueBoxRenderable == null) {
            return;
        }
        //displaypose_tv.setText(anchorNode.getWorldPosition().toString());
        TransformableNode box = new TransformableNode(arFragment.getTransformationSystem());
        box.setParent(anchorNode);
        box.setRenderable(greenBoxRenderable);
        box = new TransformableNode(arFragment.getTransformationSystem());
        box.setParent(anchorNode);
        box.setRenderable(redBoxRenderable);
        box = new TransformableNode(arFragment.getTransformationSystem());
        box.setParent(anchorNode);
        box.setRenderable(blueBoxRenderable);
    }

    public Bitmap rotateBitmap(Bitmap bitmap, int orientation) {
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_NORMAL:
                return bitmap;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
            default:
                return bitmap;
        }
        try {
            Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            return bmRotated;
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }
    }

    private double[] convertGPSToOpenGL(Double Latitude, Double Longitude, Double Altitude) {
        getLocation();

        double x0 = earthR * Longitude;
        double z0 = earthR * Latitude;
        double x = earthR * location.getLongitude();
        double z = earthR * location.getLatitude();
        double x01 = earthR * (location.getLongitude() - minM);
        double z01 = earthR * location.getLatitude();
        double x02 = earthR * location.getLongitude();
        double z02 = earthR * (location.getLatitude() - minM);
        double[] xyz = new double[3];
        xyz[0] = (x0 - x);
        xyz[1] = 0;
        xyz[2] = -(z0 - z);
        vectorX = new Vector3((float) (x01 - x), (float) 0d, (float) (z01 - z));
        vectorZ = new Vector3((float) (x02 - x), (float) 0d, (float) (z02 - z));
        return xyz;
    }


    private void readfile(int position) {
        if (position != 0) {


            Double DLatitude = contentDTOs.get(position).getLatitude();
            Double DLongitude = contentDTOs.get(position).getLongitude();

            if (DLatitude == null || DLongitude == null) return;

            double[] xyz = convertGPSToOpenGL(DLatitude, DLongitude, 0d);

            float[] translation = new float[3];
//                    float[] quaternion = new float[4];
            translation[0] = (float) xyz[0];
            translation[1] = (float) xyz[1];
            translation[2] = (float) xyz[2];

            Log.d("test translation[0]", ":" + translation[0]);
            Log.d("test translation[1]", ":" + translation[1]);
            Log.d("test translation[2]", ":" + translation[2]);

            createAndy(translation, position);
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
            if (location == null) return;
            Log.d("GPS Service", "AR에서 GPS: lat, lon" + location.getLatitude() + ", " + location.getLongitude());
        }
    }

   /* @Override
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
*/

    @Override
    public void onUpdate(FrameTime frameTime) {
        com.google.ar.core.Camera camera = arFragment.getArSceneView().getArFrame().getCamera();
        getLocation();
//        pose_tv.setText(location.getLatitude()+"\n"+location.getLongitude());

        if (camera.getTrackingState() == TrackingState.TRACKING) {
            float[] matrix = new float[16];
            arFragment.getArSceneView().getArFrame().getCamera().getViewMatrix(matrix, 0);
            arFragment.getPlaneDiscoveryController().hide();
            Pose pose = arFragment.getArSceneView().getArFrame().getAndroidSensorPose();
            quatenionAndroidPose = pose.getRotationQuaternion();
            Vector3 vec3 = arFragment.getArSceneView().getScene().getCamera().getWorldPosition();
//            float angle = getAngleBetween3DVector(new Vector3(quatenionAndroidPose[0],quatenionAndroidPose[1],quatenionAndroidPose[2]),new Vector3(startTrackingQuaternion[1],startTrackingQuaternion[2],startTrackingQuaternion[3]));
//            pose_tv.setText(""+angle);
            //arFragment.getArSceneView().getArFrame().getUpdatedAnchors().size();

            displaypose_tv.setText(startTrackingQuaternion[1] + " " + startTrackingQuaternion[2] + " " + startTrackingQuaternion[3] + " " + startTrackingQuaternion[0] + "\n"
                    + quatenionAndroidPose[0] + " " + quatenionAndroidPose[1] + " " + quatenionAndroidPose[2] + " " + quatenionAndroidPose[3] + "\n" + arFragment.getArSceneView().getSession().getAllAnchors().size());

            Collection<Anchor> list = arFragment.getArSceneView().getSession().getAllAnchors();

            if (anchorNodeCollection != null) {
                for (AnchorNode anchorNode1 : anchorNodeCollection) {
                    //anchor.getPose().
                    Log.d("eeeeeeeeeeeeeeeee", ":" + anchorNodeCollection.size());
                    Vector3 vector3 = anchorNode1.getChildren().get(0).getWorldPosition();
                    anchorNode1.getChildren().get(0).getChildren().get(0).setWorldRotation(Quaternion.lookRotation(new Vector3(vec3.x - vector3.x, vec3.y - (vector3.y + 0.25f), vec3.z - vector3.z), new Vector3(0f, 1f, 0f)));

                }
            }

            //displaypose_tv.setText(""+quatenionAndroidPose[0]+" "+quatenionAndroidPose[1]+" "+quatenionAndroidPose[2]+" "+quatenionAndroidPose[3]);
            if (!istest) {
                //createCoordinate();

                quatenionAndroidPose = new float[4];
                pose = arFragment.getArSceneView().getArFrame().getAndroidSensorPose();
                quatenionAndroidPose = pose.getRotationQuaternion();
                startTrackingStopQuaternion = new float[4];
                startTrackingStopQuaternion[0] = startTrackingQuaternion[1];
                startTrackingStopQuaternion[1] = startTrackingQuaternion[2];
                startTrackingStopQuaternion[2] = startTrackingQuaternion[3];
                startTrackingStopQuaternion[3] = startTrackingQuaternion[0];
                float getAngle = -1000f;
                for (float i = 0.0f; i < 360f; i += 0.1f) {
                    Quaternion roqu = Quaternion.axisAngle(new Vector3(0f, 01, 0f), i);
                    Quaternion testQuaternion1 = new Quaternion(startTrackingStopQuaternion[0], startTrackingStopQuaternion[1], startTrackingStopQuaternion[2], startTrackingStopQuaternion[3]);
                    testQuaternion1 = Quaternion.multiply(testQuaternion1, roqu);
                    float min = 0.15f;
                    if (Math.abs(Math.abs(testQuaternion1.x) - Math.abs(quatenionAndroidPose[0])) <= min && Math.abs(Math.abs(testQuaternion1.y) - Math.abs(quatenionAndroidPose[1])) <= min
                            && Math.abs(Math.abs(testQuaternion1.z) - Math.abs(quatenionAndroidPose[2])) <= min && Math.abs(Math.abs(testQuaternion1.w) - Math.abs(quatenionAndroidPose[3])) <= min) {
                        getAngle = i;
                        break;
                    }
                }
                if (getAngle <= 0) {
                    pose_tv.setText("실패했어!!!!!!!!망했어!!!!!!!!!");
                } else {
                    pose_tv.setText("성공했어!!!!!!!!!! " + getAngle);
                    rotationQuaternionY = Quaternion.axisAngle(new Vector3(0f, 01, 0f), getAngle);
                }
//                pose_tv.setText(""+startTrackingStopQuaternion[0]+" "+startTrackingStopQuaternion[1]+" "+startTrackingStopQuaternion[2]+" "+startTrackingStopQuaternion[3]);
//                displaypose_tv.setText(startTrackingStopQuaternion[0]+" "+startTrackingStopQuaternion[1]+" "+startTrackingStopQuaternion[2]+" "+startTrackingStopQuaternion[3]+"\n"+quatenionAndroidPose[0]+" "+quatenionAndroidPose[1]+" "+quatenionAndroidPose[2]+" "+quatenionAndroidPose[3]);
                istest = true;
            }

            Pose dispose = arFragment.getArSceneView().getArFrame().getCamera().getDisplayOrientedPose();
            Pose camerapose = arFragment.getArSceneView().getArFrame().getCamera().getPose();
            //pose_tv.setText(azimut+"\n"+pose.toString());
            //displaypose_tv.setText("\n"+dispose.toString() + camerapose.toString());
            arFragment.getArSceneView().getArFrame().getCamera().getProjectionMatrix(matrix, 0, 0f, 0f);
//            displaypose_tv.setText(dispose.toString() + camerapose.toString()+"\n"+matrix[0]+" "+matrix[1]+" "+matrix[2]+" "+matrix[3]+"\n"+matrix[4]+" "+matrix[5]
//                    +" "+matrix[6]+" "+matrix[7]+"\n"+matrix[8]+" "+matrix[9]+" "+matrix[10]+" "+matrix[11]+"\n"+matrix[12]+" "+matrix[13]+" "+matrix[14]+" "+matrix[15]);

            if (!isstart) {
                Log.d("TRAVELIMAGE", "여기선 받아와야해: " + contentDTOs.size());
                for (int i = 0; i < contentDTOs.size(); i++) {
                    readfile(i);
                }
                isstart = true;
            }
        } else if (camera.getTrackingState() == TrackingState.PAUSED) {
//            Pose pose = arFragment.getArSceneView().getArFrame().getAndroidSensorPose();
//            Log.d("pose",":"+pose);
            //pose_tv.setText("PAUSED");
//            isstart=false;
//            istest=false;
        } else if (camera.getTrackingState() == TrackingState.STOPPED) {
            //pose_tv.setText("STOPPED");
        }
    }

    private float[] vector3ToTranslation(Vector3 v) {
        float[] translation = new float[3];
        translation[0] = v.x;
        translation[1] = v.y;
        translation[2] = v.z;
        return translation;
    }

    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ROTATION_VECTOR:
                rotationVals = event.values.clone();
                break;
        }
        startTrackingQuaternion = new float[4];
//        float[] test = new float[4];
//        test[0] = rotationVals[0];
//        test[1] = rotationVals[2];
//        test[2] = -rotationVals[1];
//        test[3] = rotationVals[0];

        SensorManager.getQuaternionFromVector(startTrackingQuaternion, event.values);
        Quaternion testQuaternion = Quaternion.axisAngle(new Vector3(1f, 0f, 0f), -90f);
        Quaternion testQuaternion1 = new Quaternion(startTrackingQuaternion[1], startTrackingQuaternion[2], startTrackingQuaternion[3], startTrackingQuaternion[0]);
        testQuaternion1 = Quaternion.multiply(testQuaternion1, testQuaternion);
        startTrackingQuaternion[1] = testQuaternion1.x;
        startTrackingQuaternion[2] = testQuaternion1.z;
        startTrackingQuaternion[3] = testQuaternion1.y;
        startTrackingQuaternion[0] = testQuaternion1.w;
        //SensorManager.getRotationMatrixFromVector();
        //displaypose_tv.setText("Azimuth:"+event.values[0]+"\n"+event.values[1]+"\n"+event.values[2]+"\n"+event.values[3]+"\n"+event.values[4]);
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            mGravity = event.values;
        }
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            mGeomagnetic = event.values;
        }
        if (mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I,
                    mGravity, mGeomagnetic);
            if (success) {
                orientation = new float[3];
                SensorManager.getOrientation(R, orientation);
                azimut = orientation[0]; // orientation contains: azimut, pitch and roll
                //displaypose_tv.setText("Azimuth:"+Math.toDegrees(orientation[0])+"\n"+"Pitch:"+Math.toDegrees(orientation[1])+"\n"+"Roll:"+Math.toDegrees(orientation[2])+"\n"+event.values[0]+"\n"+event.values[1]+"\n"+event.values[2]+"\n"+event.values[3]+"\n"+event.values[4]);

                //Log.d("azimut",":"+azimut);
            }
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
