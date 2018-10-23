package com.nineclown.lbarsns;


import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.nineclown.lbarsns.databinding.FragmentAlarmBinding;
import com.nineclown.lbarsns.databinding.ItemCommentBinding;

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
        binding.alarmfragmentRecyclerview.setAdapter(new AlarmRecyclerViewAdapter());
        binding.alarmfragmentRecyclerview.setLayoutManager(new LinearLayoutManager(getActivity()));

        return binding.getRoot();
    }

    private class AlarmRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private ItemCommentBinding aBinding;
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            //View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
            aBinding = ItemCommentBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);

            return new CustomViewHolder(aBinding);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        }

        @Override
        public int getItemCount() {
            return 0;
        }

        private class CustomViewHolder extends RecyclerView.ViewHolder {
            private ItemCommentBinding hBinding;

            public CustomViewHolder(ItemCommentBinding aBinding) {
                super(aBinding.getRoot());
                this.hBinding = aBinding;
            }
        }
    }

}
