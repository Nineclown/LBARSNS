package com.nineclown.lbarsns.sns;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.nineclown.lbarsns.R;
import com.nineclown.lbarsns.databinding.ActivityMainBinding;
import com.theartofdev.edmodo.cropper.CropImage;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {
    private static final int PICK_PROFILE_FROM_ALBUM = 10;
    private ActivityMainBinding binding;
    private OnBackPressedListener mListener;
    private FirebaseStorage mStorage;
    private FirebaseFirestore mFirestore;
    private FirebaseAuth mAuth;
    private String mUid;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        // firebase
        mStorage = FirebaseStorage.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        mUid = mAuth.getCurrentUser().getUid();

        binding.bottomNavigation.setOnNavigationItemSelectedListener(this);
        binding.bottomNavigation.setSelectedItemId(R.id.action_home);
        // 앨범 접근 권한 요청
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);

        // 푸시토큰 서버 등록
        registerPushToken();

    }

    private void registerPushToken() {
        String pushToken = FirebaseInstanceId.getInstance().getToken();
        HashMap<String, Object> map = new HashMap<>();
        map.put("pushToken", pushToken);
        mFirestore.collection("pushTokens").document(mUid).set(map);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //binding.bottomNavigation.setSelectedItemId(R.id.action_home);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK) {

            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            Uri imageUri = result.getUri();
            final StorageReference reference = mStorage.getReference().child("userProfileImages").child(mUid);
            UploadTask uploadTask = reference.putFile(imageUri);

            Task<Uri> uriTask = uploadTask.continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                // Continue with the task to get the download URL
                return reference.getDownloadUrl();
            }).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Uri downloadUri = task.getResult();
                    if (downloadUri == null) return;
                    String url = downloadUri.toString();

                    HashMap<String, Object> map = new HashMap<>();
                    map.put("image", url);
                    FirebaseFirestore.getInstance().collection("profileImages").document(mUid).set(map);
                } else {
                    // Handle failures
                    // ...
                }
            });
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        setToolbarDefault();
        switch (item.getItemId()) {
            case R.id.action_home: {
                Fragment dailyLifeFragment = new DailyLifeFragment();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.main_content, dailyLifeFragment)
                        .commit();
                return true;
            }

            case R.id.action_search: {

                Fragment infoFragment = new InfoFragment();

                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.main_content, infoFragment)
                        .commit();
                return true;
            }

            case R.id.action_upload: {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    startActivity(new Intent(this, UploadActivity.class));
                }

                return true;
            }
            case R.id.action_map: {
                //Fragment alarmFragment = new AlarmFragment();
                Fragment mapFragment = new MapFragment();

                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.main_content, mapFragment)
                        .commit();
                return true;
            }

            case R.id.action_account: {
                Fragment userFragment = new UserFragment();
                Bundle bundle = new Bundle();
                bundle.putString("destinationUid", mUid);
                userFragment.setArguments(bundle);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.main_content, userFragment)
                        .commit();
                return true;
            }
        }

        return false;
    }

    public ActivityMainBinding getBinding() {
        return this.binding;
    }

    private void setToolbarDefault() {
        binding.toolbarBtnBack.setVisibility(View.GONE);
        binding.toolbarBtnAr.setVisibility(View.GONE);
        binding.toolbarUsername.setVisibility(View.GONE);
        binding.toolbarTitleImage.setVisibility(View.VISIBLE);
    }

    public interface OnBackPressedListener {
        public void onBackPressed();

    }

    public void setOnBackPressedListener(OnBackPressedListener listener) {
        mListener = listener;
    }

    @Override
    public void onBackPressed() {

        // Fragment에서 listener를 달았을 때 여기로 처리 된다.
        if (mListener != null) {
            mListener.onBackPressed();
        }
        // listener를 안달았을 경우, 여기에서 처리된다.
        else {

            super.onBackPressed();
            //finish();
            // 이게 무슨 구문인지 안배웠는데.
            //android.os.Process.killProcess(android.os.Process.myPid());
        }

    }
}

