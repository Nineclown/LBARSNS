package com.nineclown.lbarsns;

import android.Manifest;
import android.app.Activity;
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

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.nineclown.lbarsns.databinding.ActivityMainBinding;

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
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
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

        if (requestCode == PICK_PROFILE_FROM_ALBUM && resultCode == Activity.RESULT_OK) {
            Uri imageUri = data.getData();

            // 일단 프로필 사진을 storage에 올린다.
            final StorageReference reference = mStorage.getReference().child("userProfileImages").child(mUid);
            UploadTask uploadTask = reference.putFile(imageUri);

            Task<Uri> uriTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    // Continue with the task to get the download URL
                    return reference.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        Uri downloadUri = task.getResult();
                        String url = downloadUri.toString();

                        HashMap<String, Object> map = new HashMap<>();
                        map.put("image", url);
                        FirebaseFirestore.getInstance().collection("profileImages").document(mUid).set(map);

                    } else {
                        // Handle failures
                        // ...
                    }
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
                getSupportFragmentManager().beginTransaction().replace(R.id.main_content, dailyLifeFragment).commit();
                return true;
            }

            case R.id.action_search: {

                Fragment infoFragment = new InfoFragment();

                getSupportFragmentManager().beginTransaction().replace(R.id.main_content, infoFragment).commit();
                return true;
            }

            case R.id.action_add_photo: {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    startActivity(new Intent(this, AddActivity.class));
                }

                return true;
            }
            case R.id.action_favorite_alarm: {
                Fragment alarmFragment = new AlarmFragment();

                getSupportFragmentManager().beginTransaction().replace(R.id.main_content, alarmFragment).commit();
                return true;
            }

            case R.id.action_account: {
                Fragment userFragment = new UserFragment();
                Bundle bundle = new Bundle();
                bundle.putString("destinationUid", mUid);
                userFragment.setArguments(bundle);
                getSupportFragmentManager().beginTransaction().replace(R.id.main_content, userFragment).commit();
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
            finish();
            // 이게 무슨 구문인지 안배웠는데.
            android.os.Process.killProcess(android.os.Process.myPid());
        }

    }
}

