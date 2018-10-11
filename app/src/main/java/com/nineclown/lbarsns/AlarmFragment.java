package com.nineclown.lbarsns;


import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.nineclown.lbarsns.databinding.FragmentAlarmBinding;

public class AlarmFragment extends Fragment {

    private FragmentAlarmBinding binding;

    public AlarmFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_alarm, container, false);

        return binding.getRoot();
    }

}
