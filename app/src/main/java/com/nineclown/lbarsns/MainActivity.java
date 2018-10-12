package com.nineclown.lbarsns;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.nineclown.lbarsns.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        binding.bottomNavigation.setOnNavigationItemSelectedListener(this);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);

    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_home: {
                Fragment detailViewFragment = new DetailViewFragment();
                getSupportFragmentManager().beginTransaction().replace(R.id.main_content, detailViewFragment).commit();
                return true;
            }

            case R.id.action_search: {

                Fragment gridFragment = new GridFragment();

                getSupportFragmentManager().beginTransaction().replace(R.id.main_content, gridFragment).commit();
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

                getSupportFragmentManager().beginTransaction().replace(R.id.main_content, userFragment).commit();
                return true;
            }


        }

        return false;
    }
}