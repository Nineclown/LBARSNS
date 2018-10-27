/*
 * Copyright 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nineclown.lbarsns.camera;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.nineclown.lbarsns.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static android.content.Context.LOCATION_SERVICE;

public class CameraFragment extends Fragment
        implements View.OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback, View.OnTouchListener, LocationListener {

    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final String FRAGMENT_DIALOG = "dialog";

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    /**
     * Tag for the {@link Log}.
     */
    private static final String TAG = "Camera2BasicFragment";

    /**
     * Camera state: Showing camera preview.
     */
    private static final int STATE_PREVIEW = 0;

    /**
     * Camera state: Waiting for the focus to be locked.
     */
    private static final int STATE_WAITING_LOCK = 1;

    /**
     * Camera state: Waiting for the exposure to be precapture state.
     */
    private static final int STATE_WAITING_PRECAPTURE = 2;

    /**
     * Camera state: Waiting for the exposure state to be something other than precapture.
     */
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;

    /**
     * Camera state: Picture was taken.
     */
    private static final int STATE_PICTURE_TAKEN = 4;

    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    public float finger_spacing = 0;
    public int zoom_level = 1;
    public Rect zoom;
    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.
     */
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }

    };

    /**
     * ID of the current {@link CameraDevice}.
     */
    private String mCameraId;

    /**
     * An {@link AutoFitTextureView} for camera preview.
     */
    private AutoFitTextureView mTextureView;

    /**
     * A {@link CameraCaptureSession } for camera preview.
     */
    private CameraCaptureSession mCaptureSession;

    /**
     * A reference to the opened {@link CameraDevice}.
     */
    private CameraDevice mCameraDevice;

    /**
     * The {@link android.util.Size} of camera preview.
     */
    private Size mPreviewSize;

    public static Size[] previewSizes;
    public static Size[] videoSizes;
    public static Size[] pictureSizes;

    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its state.
     */
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            Activity activity = getActivity();
            if (null != activity) {
                activity.finish();
            }
        }

    };

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread mBackgroundThread;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler mBackgroundHandler;

    /**
     * An {@link ImageReader} that handles still image capture.
     */
    private ImageReader mImageReader;

    /**
     * This is the output file for our picture.
     */
    private File mFile;
    private File m_file;

    /**
     * This a callback object for the {@link ImageReader}. "onImageAvailable" will be called when a
     * still image is ready to be saved.
     */
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            mBackgroundHandler.post(new ImageSaver(reader.acquireNextImage(), mFile));
            //setExifInfo(mFile.toURI());
        }

    };

    public static final String CAMERA_FRONT = "1";
    public static final String CAMERA_BACK = "0";

    private String cameraId = CAMERA_BACK;
    private boolean isTorchOn;

    private Switch modeSwitch;

    /**
     * {@link CaptureRequest.Builder} for the camera preview
     */
    private CaptureRequest.Builder mPreviewRequestBuilder;

    /**
     * {@link CaptureRequest} generated by {@link #mPreviewRequestBuilder}
     */
    private CaptureRequest mPreviewRequest;

    /**
     * The current state of camera state for taking pictures.
     *
     * @see #mCaptureCallback
     */
    private int mState = STATE_PREVIEW;

    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    /**
     * Whether the current camera device supports Flash or not.
     */
    private boolean mFlashSupported;

    /**
     * Orientation of the camera sensor
     */
    private int mSensorOrientation;

    /**
     * A {@link CameraCaptureSession.CaptureCallback} that handles events related to JPEG capture.
     */
    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result) {
            switch (mState) {
                case STATE_PREVIEW: {
                    // We have nothing to do when the camera preview is working normally.
                    break;
                }
                case STATE_WAITING_LOCK: {
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState == null) {
                        captureStillPicture();
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                        // CONTROL_AE_STATE can be null on some devices
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null ||
                                aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = STATE_PICTURE_TAKEN;
                            captureStillPicture();
                        } else {
                            runPrecaptureSequence();
                        }
                    }
                    break;
                }
                case STATE_WAITING_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        mState = STATE_WAITING_NON_PRECAPTURE;
                    }
                    break;
                }
                case STATE_WAITING_NON_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        mState = STATE_PICTURE_TAKEN;
                        captureStillPicture();
                    }
                    break;
                }
            }
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            process(result);
        }

    };

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


    /**
     * Shows a {@link Toast} on the UI thread.
     *
     * @param text The message to show
     */
    private void showToast(final String text) {
        final Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                          int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            Log.d("option.x", ":" + option.getWidth());
            Log.d("option.y", ":" + option.getHeight());
            Log.d("--------", "------------------");
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    public static CameraFragment newInstance() {
        return new CameraFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera, container, false);
    }


    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {

        view.findViewById(R.id.picture).setOnClickListener(this);
        view.findViewById(R.id.texture).setOnTouchListener(this);
        modeSwitch = (Switch) getView().findViewById(R.id.switch_focus_mode);
        //BusProvider.getInstance().register(this);
        modeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Log.d("test", ":true");
                    Intent intent1 = new Intent(getActivity(), ArActivity.class);
                    startActivity(intent1);
                    getActivity().overridePendingTransition(0, 0);
                    onDestroy();


                } else {
                }
            }
        });
        mTextureView = (AutoFitTextureView) view.findViewById(R.id.texture);
    }

    public void onDestroy() {
        super.onDestroy();
        modeSwitch.setChecked(false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
//        String sdcard = Environment.getExternalStorageState();
//        if(!sdcard.equals(Environment.MEDIA_MOUNTED))
//        {
//            mFile = Environment.getRootDirectory();
//        }
//        else
//        {
//            mFile = Environment.getExternalStorageDirectory();
//        }
//        String TimeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
//        TimeStamp = TimeStamp +".jpg";
//        //TimeStamp = "/DCIM/" + TimeStamp;
//        //mFile = new File(Environment.getExternalStorageDirectory(), TimeStamp);
//        mFile = new File(getActivity().getExternalFilesDir(null), TimeStamp);
        String sdcard = Environment.getExternalStorageState();
        m_file = null;
        if (!sdcard.equals(Environment.MEDIA_MOUNTED)) {
            m_file = Environment.getRootDirectory();
        } else {
            m_file = Environment.getExternalStorageDirectory();
        }


        m_file = new File(Environment.getExternalStorageDirectory(), "/Pictures/Lbarsns");
        if (!m_file.exists()) {
            // 디렉토리가 존재하지 않으면 디렉토리 생성
            m_file.mkdirs();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        int uiOptions = getActivity().getWindow().getDecorView().getSystemUiVisibility();
        int newUiOptions = uiOptions;
        boolean isImmersiveModeEnabled = ((uiOptions | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY) == uiOptions);
        if (isImmersiveModeEnabled) {
            Log.i("Is on?", "Turning immersive mode mode off. ");
        } else {
            Log.i("Is on?", "Turning immersive mode mode on.");
        }

        newUiOptions ^= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        newUiOptions ^= View.SYSTEM_UI_FLAG_FULLSCREEN;
        newUiOptions ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        getActivity().getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
        startBackgroundThread();

        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
        int uiOptions = getActivity().getWindow().getDecorView().getSystemUiVisibility();
        int newUiOptions = uiOptions;
        boolean isImmersiveModeEnabled = ((uiOptions | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY) == uiOptions);
        if (isImmersiveModeEnabled) {
            Log.i("Is on?", "Turning immersive mode mode off. ");
        } else {
            Log.i("Is on?", "Turning immersive mode mode on.");
        }

        newUiOptions ^= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        newUiOptions ^= View.SYSTEM_UI_FLAG_FULLSCREEN;
        newUiOptions ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        getActivity().getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
    }

    private void requestCameraPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            new ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG);
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
//                                           @NonNull int[] grantResults) {
//        if (requestCode == REQUEST_CAMERA_PERMISSION) {
//            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
//                ErrorDialog.newInstance(getString(R.string.request_permission))
//                        .show(getChildFragmentManager(), FRAGMENT_DIALOG);
//            }
//        } else {
//            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        }
//    }

    /**
     * Sets up member variables related to camera.
     *
     * @param width  The width of available size for camera preview
     * @param height The height of available size for camera preview
     */
    @SuppressWarnings("SuspiciousNameCombination")
    private void setUpCameraOutputs(int width, int height) {
        Activity activity = getActivity();
        if (activity == null) return;
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {

            if (manager == null) return;
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics
                        = manager.getCameraCharacteristics(cameraId);

                // We don't use a front facing camera in this sample.
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }

                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }

                // For still image captures, we use the largest available size.
                Size largest = Collections.max(
                        Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new CompareSizesByArea());
                mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
                        ImageFormat.JPEG, /*maxImages*/2);
                mImageReader.setOnImageAvailableListener(
                        mOnImageAvailableListener, mBackgroundHandler);
                Log.d("mImageReader.x", ":" + mImageReader.getWidth());
                Log.d("mImageReader.y", ":" + mImageReader.getHeight());
                // Find out if we need to swap dimension to get the preview size relative to sensor
                // coordinate.
                int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
                //noinspection ConstantConditions
                mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                boolean swappedDimensions = false;
                switch (displayRotation) {
                    case Surface.ROTATION_0:
                    case Surface.ROTATION_180:
                        if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                            swappedDimensions = true;
                        }
                        break;
                    case Surface.ROTATION_90:
                    case Surface.ROTATION_270:
                        if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                            swappedDimensions = true;
                        }
                        break;
                    default:
                        Log.e(TAG, "Display rotation is invalid: " + displayRotation);
                }

                Point displaySize = new Point();
                activity.getWindowManager().getDefaultDisplay().getSize(displaySize);
                int rotatedPreviewWidth = width;
                int rotatedPreviewHeight = height;
                int maxPreviewWidth = displaySize.x;
                int maxPreviewHeight = displaySize.y;
                Log.d("largest.x", ":" + largest.getWidth());
                Log.d("largest.y", ":" + largest.getHeight());
                Log.d("rotatedPreviewWidth", ":" + rotatedPreviewWidth);
                Log.d("rotatedPreviewHeight", ":" + rotatedPreviewHeight);
                Log.d("displaySize.x", ":" + displaySize.x);
                Log.d("displaySize.y", ":" + displaySize.y);
                if (swappedDimensions) {
                    rotatedPreviewWidth = height;
                    rotatedPreviewHeight = width;
                    maxPreviewWidth = displaySize.y;
                    maxPreviewHeight = displaySize.x;
                }

                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                    maxPreviewWidth = MAX_PREVIEW_WIDTH;
                }

                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                    maxPreviewHeight = MAX_PREVIEW_HEIGHT;
                }

                // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
                // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
                // garbage capture data.
                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                        rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                        maxPreviewHeight, largest);

//                mPreviewSize = new Size(height,width);
                Log.d("mPreviewSize.x", ":" + mPreviewSize.getWidth());
                Log.d("mPreviewSize.y", ":" + mPreviewSize.getHeight());

                // We fit the aspect ratio of TextureView to the size of preview we picked.
                int orientation = getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    mTextureView.setAspectRatio(
                            mPreviewSize.getWidth(), mPreviewSize.getHeight());
                } else {
                    mTextureView.setAspectRatio(
                            mPreviewSize.getHeight(), mPreviewSize.getWidth());
                }

                // Check if the flash is supported.
                Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                mFlashSupported = available == null ? false : available;

                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            ErrorDialog.newInstance(getString(R.string.camera_error))
                    .show(getChildFragmentManager(), FRAGMENT_DIALOG);
        }
    }

    /**
     * Opens the camera specified by {@link CameraFragment#mCameraId}.
     */
    private void openCamera(int width, int height) {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
            return;
        }
        setUpCameraOutputs(width, height);
        configureTransform(width, height);
        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    /**
     * Closes the current {@link CameraDevice}.
     */
    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a new {@link CameraCaptureSession} for camera preview.
     */
    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            Log.d("mTextureView.x", ":" + mTextureView.getWidth());
            Log.d("mTextureView.y", ":" + mTextureView.getHeight());
            Log.d("mPreviewSize.x", ":" + mPreviewSize.getWidth());
            Log.d("mPreviewSize.y", ":" + mPreviewSize.getHeight());

            // This is the output Surface we need to start preview.
            Surface surface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            mCaptureSession = cameraCaptureSession;
                            try {
                                // Auto focus should be continuous for camera preview.
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                // Flash is automatically enabled when necessary.
                                setAutoFlash(mPreviewRequestBuilder);

                                // Finally, we start displaying the camera preview.
                                mPreviewRequest = mPreviewRequestBuilder.build();
                                mCaptureSession.setRepeatingRequest(mPreviewRequest,
                                        mCaptureCallback, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                            showToast("Failed");
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Configures the necessary {@link android.graphics.Matrix} transformation to `mTextureView`.
     * This method should be called after the camera preview size is determined in
     * setUpCameraOutputs and also the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = getActivity();
        if (null == mTextureView || null == mPreviewSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Point point = new Point();
        activity.getWindowManager().getDefaultDisplay().getRealSize(point);
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(((viewWidth - point.x) / 2), 0, viewWidth - ((viewWidth - point.x) / 2), viewHeight);
        Log.d("viewWidth-point", ":" + ((viewWidth - point.x) / 2));
        RectF bufferRect = new RectF(0, 0, point.x, point.y);
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
//        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
//            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
//            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
//            float scale = Math.max(
//                    (float) viewHeight / mPreviewSize.getHeight(),
//                    (float) viewWidth / mPreviewSize.getWidth());
//            matrix.postScale(scale, scale, centerX, centerY);
//            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
//        } else if (Surface.ROTATION_180 == rotation) {
//            matrix.postRotate(180, centerX, centerY);
//        }
//        bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
//        matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
//        float scale = Math.max(
//                (float) viewHeight / mPreviewSize.getHeight(),
//                (float) viewWidth / mPreviewSize.getWidth());
//        Log.d("scale",":" + scale);
        //matrix.postScale(scale, scale, centerX, centerY);
//        matrix.postRotate(0, centerX, centerY);
        Log.d("bufferRectWidth", ":" + bufferRect.left);
        Log.d("bufferRectHeight", ":" + bufferRect.right);
        Log.d("bufferRectWidth", ":" + bufferRect.top);
        Log.d("bufferRectHeight", ":" + bufferRect.bottom);
        Log.d("centerX-bufferRect", ":" + (centerX - bufferRect.centerX()));
        Log.d("centerY-bufferRect", ":" + (centerY - bufferRect.centerY()));
        bufferRect.offset(0, centerY - bufferRect.centerY());
        Log.d("bufferRectWidth", ":" + bufferRect.left);
        Log.d("bufferRectHeight", ":" + bufferRect.right);
        Log.d("bufferRectWidth", ":" + bufferRect.top);
        Log.d("bufferRectHeight", ":" + bufferRect.bottom);
        matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
        mTextureView.setTransform(matrix);

        Log.d("viewWidth", ":" + viewWidth);
        Log.d("viewHeight", ":" + viewHeight);
        Log.d("mTextureView.x", ":" + mTextureView.getWidth());
        Log.d("mTextureView.y", ":" + mTextureView.getHeight());
        Log.d("mPreviewSize.x", ":" + mPreviewSize.getWidth());
        Log.d("mPreviewSize.y", ":" + mPreviewSize.getHeight());

    }

    /**
     * Initiate a still image capture.
     */
    private void takePicture() {
        lockFocus();
//        mTextureView.buildDrawingCache();
//        Bitmap captureView = mTextureView.getDrawingCache();
//        FileOutputStream fos;
//        try{
//            fos = new FileOutputStream(Environment.getDownloadCacheDirectory().toString()+"/picpic.jpeg");
//            captureView.compress(Bitmap.CompressFormat.JPEG,100,fos);
//        }catch(FileNotFoundException e)
//        {
//            e.printStackTrace();
//        }
    }

    /**
     * Lock the focus as the first step for a still image capture.
     */
    private void lockFocus() {

        String TimeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        TimeStamp = TimeStamp + ".jpg";
        //TimeStamp = "/TestCamera/" + TimeStamp;
        mFile = new File(m_file, TimeStamp);
        //mFile = new File(getActivity().getExternalFilesDir(null), TimeStamp);
//        MediaActionSound sound = new MediaActionSound();// 카메라 찍을때 소리 나게 하는거
//        sound.play(MediaActionSound.SHUTTER_CLICK);
        try {
            // This is how to tell the camera to lock focus.
            Activity activity = getActivity();
            CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
            CameraCharacteristics characteristics
                    = manager.getCameraCharacteristics(mCameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            previewSizes = map.getOutputSizes(SurfaceTexture.class);
            videoSizes = map.getOutputSizes(MediaRecorder.class);
            pictureSizes = map.getOutputSizes(ImageFormat.PRIVATE);

            Size largest = Collections.max(
                    Arrays.asList(map.getOutputSizes(SurfaceTexture.class)),
                    new CompareSizesByArea());

            Size largest1 = Collections.max(
                    Arrays.asList(map.getOutputSizes(MediaRecorder.class)),
                    new CompareSizesByArea());

            Size largest2 = Collections.max(
                    Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                    new CompareSizesByArea());

            for (Size option : previewSizes) {
                Log.d("previewSizes.x", ":" + option.getWidth());
                Log.d("previewSizes.y", ":" + option.getHeight());
                Log.d("--------", "------------------");
            }
            for (Size option : videoSizes) {
                Log.d("videoSizes.x", ":" + option.getWidth());
                Log.d("videoSizes.y", ":" + option.getHeight());
                Log.d("--------", "------------------");
            }
            for (Size option : pictureSizes) {
                Log.d("pictureSizes.x", ":" + option.getWidth());
                Log.d("pictureSizes.y", ":" + option.getHeight());
                Log.d("--------", "------------------");
            }

            Log.d("text1", ":" + largest);
            Log.d("text2", ":" + largest1);
            Log.d("text3", ":" + largest2);
            Size testsize = selectImageSize();
            Log.d("testsize", ":" + testsize);

            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the lock.
            mState = STATE_WAITING_LOCK;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private Size selectImageSize() {
        CameraCharacteristics cameraCharacteristics = null;
        CameraManager cameraManager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraCharacteristics = cameraManager.getCameraCharacteristics(mCameraId);
        } catch (CameraAccessException e) {
            return null;
        }

        StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (streamConfigurationMap == null) {
            return null;
        }
        Size[] jpegSizes;
        jpegSizes = streamConfigurationMap.getOutputSizes(ImageFormat.JPEG);

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int screenLength = size.x;
        if (screenLength < size.y) {
            screenLength = size.y;
        }

        int index = 0;
        int finalIndex = 0;
        int finalJpegLength = Integer.MAX_VALUE;
        for (Size jpegSize : jpegSizes) {
            int jpegLength = jpegSize.getWidth();
            if (jpegLength < jpegSize.getHeight()) {
                jpegLength = jpegSize.getHeight();
            }
            if (jpegLength >= screenLength) {
                if (jpegLength < finalJpegLength) {
                    finalJpegLength = jpegLength;
                    finalIndex = index;
                }
            }
            index++;
        }

        Size returnSize = new Size(jpegSizes[finalIndex].getWidth(), jpegSizes[finalIndex].getHeight());
        return returnSize;
    }

    /**
     * Run the precapture sequence for capturing a still image. This method should be called when
     * we get a response in {@link #mCaptureCallback} from {@link #lockFocus()}.
     */
    private void runPrecaptureSequence() {
        try {
            // This is how to tell the camera to trigger.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the precapture sequence to be set.
            mState = STATE_WAITING_PRECAPTURE;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Capture a still picture. This method should be called when we get a response in
     * {@link #mCaptureCallback} from both {@link #lockFocus()}.
     */
    private void captureStillPicture() {
        try {
            final Activity activity = getActivity();
            if (null == activity || null == mCameraDevice) {
                return;
            }

            Log.d("mImageReader1.x", ":" + mImageReader.getWidth());
            Log.d("mImageReader1.y", ":" + mImageReader.getHeight());
//            mImageReader=ImageReader.newInstance(1440,2960,ImageFormat.JPEG,2);
//            mImageReader.setOnImageAvailableListener(
//                    mOnImageAvailableListener, mBackgroundHandler);
            // This is the CaptureRequest.Builder that we use to take a picture.
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());

            // Use the same AE and AF modes as the preview.
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            setAutoFlash(captureBuilder);

            if (zoom != null) {
                captureBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoom);
            }

            // Orientation
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));

            CameraCaptureSession.CaptureCallback CaptureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    showToast("Saved: " + mFile);
                    Log.d(TAG, mFile.toString());
                    setExifInfo(mFile.toString());
                    unlockFocus();
                }
            };

            mCaptureSession.stopRepeating();
            mCaptureSession.abortCaptures();
            mCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves the JPEG orientation from the specified screen rotation.
     *
     * @param rotation The screen rotation.
     * @return The JPEG orientation (one of 0, 90, 270, and 360)
     */
    private int getOrientation(int rotation) {
        // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
        // We have to take that into account and rotate JPEG properly.
        // For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
        // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
        return (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;
    }

    /**
     * Unlock the focus. This method should be called when still image capture sequence is
     * finished.
     */
    private void unlockFocus() {
        try {
            // Reset the auto-focus trigger
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            setAutoFlash(mPreviewRequestBuilder);
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
            // After this, the camera will go back to the normal state of preview.
            mState = STATE_PREVIEW;
            mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.picture: {
                takePicture();
                break;
            }
            case R.id.info: {
//                Activity activity = getActivity();
//                if (null != activity) {
//                    new AlertDialog.Builder(activity)
//                            .setMessage(R.string.intro_message)
//                            .setPositiveButton(android.R.string.ok, null)
//                            .show();
//
//                }
                if (mFlashSupported) {
                    Log.d("flash", ":");
                    //switchFlash();
                    //setExifInfo(mFile.toString());
//                    try {
//                        ExifInterface exif = new ExifInterface("/storage/emulated/0/LBARSNS/20181025_221019.jpg");
//                        Log.d("strlatitude0",":"+exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE));
//                        Log.d("strlongitude0",":"+exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE));
//                    } catch (IOException e)
//                    {
//                        e.printStackTrace();
//                    }
                }
                break;
            }
        }
    }


    private void setAutoFlash(CaptureRequest.Builder requestBuilder) {
        if (mFlashSupported && !isTorchOn) {
            requestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        }
    }

    /**
     * Saves a JPEG {@link Image} into the specified {@link File}.
     */
    private static class ImageSaver implements Runnable {

        /**
         * The JPEG image
         */
        private final Image mImage;
        /**
         * The file we save the image into.
         */
        private final File mFile;

        ImageSaver(Image image, File file) {
            mImage = image;
            mFile = file;
        }

        @Override
        public void run() {
//            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
//            byte[] bytes = new byte[buffer.remaining()];
//            buffer.get(bytes);
//            FileOutputStream output = null;
//            try {
//                output = new FileOutputStream(mFile);
//                output.write(bytes);
//            } catch (IOException e) {
//                e.printStackTrace();
//            } finally {
//                mImage.close();
//                if (null != output) {
//                    try {
//                        output.close();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
            //mImage.setCropRect(new Rect(0,0,4032,3024));
            Log.d("mImage.width", ":" + mImage.getWidth());
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(mFile);
                output.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }

    private void setExifInfo(String ImageUri) {
        Log.d("URI1234", ":" + ImageUri);
        getLocation();
        //copyExifInfo("/storage/emulated/0/LBARSNS/20181025_205520.jpg",ImageUri);
        if (ImageUri != null && location != null) {
            String strlatitude = convertTagGPSFormat(location.getLatitude());
            String strlongitude = convertTagGPSFormat(location.getLongitude());
            String straltitude = Double.toString(location.getAltitude());
            String[] arrstr = straltitude.split("\\.");
            String abc = null;
            if (arrstr[1].length() >= 3) abc = arrstr[1].substring(0, 3);
            else if (arrstr[1].length() == 2) abc = arrstr[1].substring(0, 2);
            else if (arrstr[1].length() == 1) abc = arrstr[1].substring(0, 1);
            String str = arrstr[0] + abc;
            String str0 = "1";
            for (int i = 0; i < abc.length(); i++) {
                str0 += "0";
            }
            str += "/" + str0;
            Log.d("straltitude", ":" + straltitude);
            straltitude += "/1";
            try {
                ExifInterface exif = new ExifInterface(ImageUri);
                Log.d("strlatitude0", ":" + exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE));
                Log.d("strlongitude0", ":" + exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE));
                Log.d("straltitude0", ":" + exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE));
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, strlatitude);
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, strlongitude);
                exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, str);
                Log.d("strlatitude1", ":" + exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE));
                Log.d("strlongitude1", ":" + exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE));
                Log.d("straltitude1", ":" + exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE));
                exif.saveAttributes();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                ExifInterface exif = new ExifInterface(ImageUri);
                Log.d("strlatitude0", ":" + exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE));
                Log.d("strlongitude0", ":" + exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE));
                Log.d("straltitude0", ":" + exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void copyExifInfo(String srcUri, String desUri) { // desUri -> Exif 값이 존재하는 이미지의 전체 경로, srcUri -> Exif 값이 없는 이미지의 전체 경로
        if (desUri != null && srcUri != null) {
            try {
                ExifInterface srcExif = new ExifInterface(srcUri); // Exif 값이 존재하는 이미지로 ExifInterface 인스턴스 생성
                ExifInterface desExif = new ExifInterface(desUri); // Exif 값이 없는 이미지로 ExifInterface 인스턴스 생성

                if (srcExif != null && desExif != null) {
                    desExif.setAttribute(ExifInterface.TAG_DATETIME, srcExif.getAttribute(ExifInterface.TAG_DATETIME)); // 시간 기록
                    desExif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, srcExif.getAttribute(ExifInterface.TAG_GPS_LATITUDE)); // GPS 정보 기록
                    desExif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, srcExif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE));
                    Log.d("srcExif", ":" + srcExif.getAttribute(ExifInterface.TAG_GPS_LATITUDE));
                    Log.d("srcExif", ":" + srcExif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE));
                    desExif.saveAttributes(); // 저장
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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

//    private String convertTagGPSFormat(double coordinate) {
//
//        if (coordinate < -180.0 || coordinate > 180.0 ||
//
//                Double.isNaN(coordinate)) {
//
//            throw new IllegalArgumentException("coordinate=" + coordinate);
//
//        }
//
//
//
//        StringBuilder sb = new StringBuilder();
//
//        if (coordinate < 0) {
//
//            sb.append('-');
//
//            coordinate = -coordinate;
//
//        }
//
//
//
//        int degrees = (int) Math.floor(coordinate);
//
//        sb.append(degrees);
//
//        sb.append("/1,");
//
//        coordinate -= degrees;
//
//        coordinate *= 60.0;
//
//        int minutes = (int) Math.floor(coordinate);
//
//        sb.append(minutes);
//
//        sb.append("/1,");
//
//        coordinate -= minutes;
//
//        coordinate *= 60.0;
//
//        sb.append(coordinate);
//
//        sb.append("/1");
//
//
//
//        return sb.toString();
//
//    }

    private Float convertToDegree(String stringDMS) {
        Float result = null;
        String[] DMS = stringDMS.split(",", 3);

        String[] stringD = DMS[0].split("/", 2);
        Double D0 = new Double(stringD[0]);
        Double D1 = new Double(stringD[1]);
        Double FloatD = D0 / D1;

        String[] stringM = DMS[1].split("/", 2);
        Double M0 = new Double(stringM[0]);
        Double M1 = new Double(stringM[1]);
        Double FloatM = M0 / M1;

        String[] stringS = DMS[2].split("/", 2);
        Double S0 = new Double(stringS[0]);
        Double S1 = new Double(stringS[1]);
        Double FloatS = S0 / S1;

        result = new Float(FloatD + (FloatM / 60) + (FloatS / 3600));

        return result;


    }

    ;

    public Location getLocation() {
        if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(
                        getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                        getActivity(), android.Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

            //return null;
        }

        try {
            locationManager = (LocationManager) getActivity()
                    .getSystemService(LOCATION_SERVICE);

            // GPS 정보 가져오기
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
                            Log.d("lat", ":" + lat);
                            Log.d("lon", ":" + lon);
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

//    /**
//     * GPS 종료
//     * */
//    public void stopUsingGPS(){
//        if(locationManager != null){
//            locationManager.removeUpdates(Camera2BasicFragment.this);
//        }
//    }
//
//    /**
//     * 위도값을 가져옵니다.
//     * */
//    public double getLatitude(){
//        if(location != null){
//            lat = location.getLatitude();
//        }
//        return lat;
//    }
//
//    /**
//     * 경도값을 가져옵니다.
//     * */
//    public double getLongitude(){
//        if(location != null){
//            lon = location.getLongitude();
//        }
//        return lon;
//    }
//
//    /**
//     * GPS 나 wife 정보가 켜져있는지 확인합니다.
//     * */
//    public boolean isGetLocation() {
//        return this.isGetLocation;
//    }
//
//    /**
//     * GPS 정보를 가져오지 못했을때
//     * 설정값으로 갈지 물어보는 alert 창
//     * */
//    public void showSettingsAlert(){
//        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
//
//        alertDialog.setTitle("GPS 사용유무셋팅");
//        alertDialog.setMessage("GPS 셋팅이 되지 않았을수도 있습니다. \n 설정창으로 가시겠습니까?");
//
//        // OK 를 누르게 되면 설정창으로 이동합니다.
//        alertDialog.setPositiveButton("Settings",
//                new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog,int which) {
//                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//                        getActivity().startActivity(intent);
//                    }
//                });
//        // Cancle 하면 종료 합니다.
//        alertDialog.setNegativeButton("Cancel",
//                new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int which) {
//                        dialog.cancel();
//                    }
//                });
//
//        alertDialog.show();
//    }

    @Override
//    public IBinder onBind(Intent arg0) {
//        return null;
//    }

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

    public boolean onTouch(View v, MotionEvent event) {
        try {
            Activity activity = getActivity();
            CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraId);
            float maxzoom = (characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)) * 10;
            Log.d("zoom_level", ":" + zoom_level);
            Rect m = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
            int action = event.getAction();
            float current_finger_spacing;

            if (event.getPointerCount() > 1 && zoom_level <= 45) {
                // Multi touch logic
                current_finger_spacing = getFingerSpacing(event);
                if (finger_spacing != 0) {
                    if (current_finger_spacing > finger_spacing && maxzoom > zoom_level) {
                        zoom_level++;
                    } else if (current_finger_spacing < finger_spacing && zoom_level > 1) {
                        zoom_level--;
                    }
                    int minW = (int) (m.width() / maxzoom);
                    int minH = (int) (m.height() / maxzoom);
                    int difW = m.width() - minW;
                    int difH = m.height() - minH;
                    int cropW = difW / 100 * (int) zoom_level;
                    int cropH = difH / 100 * (int) zoom_level;
                    cropW -= cropW & 3;
                    cropH -= cropH & 3;
                    zoom = new Rect(cropW, cropH, m.width() - cropW, m.height() - cropH);
                    mPreviewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoom);
                }
                finger_spacing = current_finger_spacing;
            } else if (event.getPointerCount() > 1 && zoom_level > 45) {
                current_finger_spacing = getFingerSpacing(event);
                if (finger_spacing != 0) {
                    if (current_finger_spacing < finger_spacing && zoom_level > 1) {
                        zoom_level--;
                    }
                    int minW = (int) (m.width() / maxzoom);
                    int minH = (int) (m.height() / maxzoom);
                    int difW = m.width() - minW;
                    int difH = m.height() - minH;
                    int cropW = difW / 100 * (int) zoom_level;
                    int cropH = difH / 100 * (int) zoom_level;
                    cropW -= cropW & 3;
                    cropH -= cropH & 3;
                    zoom = new Rect(cropW, cropH, m.width() - cropW, m.height() - cropH);
                    mPreviewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoom);
                }
                finger_spacing = current_finger_spacing;
            } else {
                if (action == MotionEvent.ACTION_UP) {
                    //single touch logic
                }
            }

            try {
                mCaptureSession
                        .setRepeatingRequest(mPreviewRequestBuilder.build(), mCaptureCallback, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            } catch (NullPointerException ex) {
                ex.printStackTrace();
            }
        } catch (CameraAccessException e) {
            throw new RuntimeException("can not access camera.", e);
        }
        return true;
    }


    //Determine the space between the first two fingers
    @SuppressWarnings("deprecation")
    private float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

//    public String saveBitmapToJpeg(Bitmap bitmap, String name){ // 임시저장하고 불러와서 crop 한후 crop 한 이미지를 저장하면 1440 * 2960 될거 같은데 몰겠음
//
//        File storage = getActivity().getCacheDir(); // 이 부분이 임시파일 저장 경로
//
//        String fileName = name + ".jpg";  // 파일이름은 마음대로!
//
//        File tempFile = new File(storage,fileName);
//
//        try{
//            tempFile.createNewFile();  // 파일을 생성해주고
//
//            FileOutputStream out = new FileOutputStream(tempFile);
//
//            bitmap.compress(Bitmap.CompressFormat.JPEG, 90 , out);  // 넘거 받은 bitmap을 jpeg(손실압축)으로 저장해줌
//
//            out.close(); // 마무리로 닫아줍니다.
//
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        return tempFile.getAbsolutePath();   // 임시파일 저장경로를 리턴해주면 끝!
//    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    /**
     * Shows an error message dialog.
     */
    public static class ErrorDialog extends DialogFragment {

        private static final String ARG_MESSAGE = "message";

        public static ErrorDialog newInstance(String message) {
            ErrorDialog dialog = new ErrorDialog();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            activity.finish();
                        }
                    })
                    .create();
        }

    }

    /**
     * Shows OK/Cancel confirmation dialog about camera permission.
     */
    public static class ConfirmationDialog extends DialogFragment {

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Fragment parent = getParentFragment();
            return new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.request_permission)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            parent.requestPermissions(new String[]{Manifest.permission.CAMERA},
                                    REQUEST_CAMERA_PERMISSION);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Activity activity = parent.getActivity();
                                    if (activity != null) {
                                        activity.finish();
                                    }
                                }
                            })
                    .create();
        }
    }

}
