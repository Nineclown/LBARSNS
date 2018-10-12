package com.nineclown.lbarsns;


import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.nineclown.lbarsns.databinding.FragmentDetailViewBinding;
import com.nineclown.lbarsns.databinding.ItemDetailBinding;

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

        binding.detailViewFragmentRecyclerView.setAdapter(new DetailRecyclerViewAdapter());
        binding.detailViewFragmentRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));


        return binding.getRoot();
    }

    private class DetailRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {


        private ItemDetailBinding binding;

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            binding = ItemDetailBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);

            return new CustomViewHolder(binding);

        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        }

        @Override
        public int getItemCount() {
            return 3;
        }

        private class CustomViewHolder extends RecyclerView.ViewHolder {
            private ItemDetailBinding binding;

            public CustomViewHolder(ItemDetailBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

        }
    }


}
