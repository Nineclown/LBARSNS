package com.nineclown.lbarsns;


import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.nineclown.lbarsns.databinding.FragmentDetailViewBinding;

public class DetailViewFragment extends Fragment {

    private FragmentDetailViewBinding binding;

    public DetailViewFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_detail_view, container, false);

        return binding.getRoot();
    }

}
