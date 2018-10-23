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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.UploadTask;
import com.nineclown.lbarsns.databinding.ActivityMainBinding;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {
    private static final int PICK_PROFILE_FROM_ALBUM = 10;
    private ActivityMainBinding binding;
    private FirebaseAuth mAuth;
    private String mUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        mAuth = FirebaseAuth.getInstance();
        mUid = mAuth.getCurrentUser().getUid();
        binding.bottomNavigation.setOnNavigationItemSelectedListener(this);
        binding.bottomNavigation.setSelectedItemId(R.id.action_home);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);

    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.bottomNavigation.setSelectedItemId(R.id.action_home);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_PROFILE_FROM_ALBUM && resultCode == Activity.RESULT_OK) {
            Uri imageUri = data.getData();
            //final String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            FirebaseStorage.getInstance().getReference().child("userProfileImages").child(mUid).putFile(imageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                    String url = task.getResult().getDownloadUrl().toString();
                    HashMap<String, Object> map = new HashMap<>();
                    map.put("image", url);
                    FirebaseFirestore.getInstance().collection("profileImages").document(mUid).set(map);
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
                    startActivity(new Intent(this, AddPhotoActivity.class));
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
}