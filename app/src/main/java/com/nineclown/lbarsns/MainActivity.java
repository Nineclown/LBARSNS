package com.nineclown.lbarsns;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
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

    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_home : {
                Fragment detailViewFragment = new DetailViewFragment();
                getSupportFragmentManager().beginTransaction().replace(R.id.main_content, detailViewFragment).commit();
                return true;
            }

            case R.id.action_search : {

                Fragment gridFragment = new GridFragment();

                getSupportFragmentManager().beginTransaction().replace(R.id.main_content, gridFragment).commit();
                return true;
            }

            case R.id.action_add_photo : {
                return true;
            }
            case R.id.action_favorite_alarm : {
                Fragment alarmFragment = new AlarmFragment();

                getSupportFragmentManager().beginTransaction().replace(R.id.main_content, alarmFragment).commit();
                return true;
            }

            case R.id.action_account : {
                Fragment userFragment = new UserFragment();

                getSupportFragmentManager().beginTransaction().replace(R.id.main_content, userFragment).commit();
                return true;
            }


        }

        return false;
    }
}