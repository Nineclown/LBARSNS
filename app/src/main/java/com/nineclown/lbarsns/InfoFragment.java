package com.nineclown.lbarsns;


import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.nineclown.lbarsns.databinding.FragmentInfoBinding;
import com.nineclown.lbarsns.model.ContentDTO;

import java.util.ArrayList;

public class InfoFragment extends Fragment {

    private FirebaseFirestore mFirestore;
    private FragmentInfoBinding binding;

    public InfoFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        //mainView = inflater.inflate(R.layout.fragment_grid, container, false);
        mFirestore = FirebaseFirestore.getInstance();
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_info, container, false);
        binding.infoFragmentRecyclerview.setAdapter(new InfoFragmentRecyclerViewAdapter());
        binding.infoFragmentRecyclerview.setLayoutManager(new GridLayoutManager(getActivity(), 3));

        return binding.getRoot();
    }

    private class InfoFragmentRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private ArrayList<ContentDTO> contentDTOs;

        public InfoFragmentRecyclerViewAdapter() {
            contentDTOs = new ArrayList<>();

            mFirestore.collection("images").orderBy("timestamp").addSnapshotListener(new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                    for (DocumentSnapshot snapshot : queryDocumentSnapshots.getDocuments()) {
                        contentDTOs.add(snapshot.toObject(ContentDTO.class));
                    }
                    notifyDataSetChanged();
                    // 새로고침을 하면, 리사이클러 뷰 메소드들이 다 다시 동작한데, 밑에 있는 onBind, onCerate, getItem 다.
                }
            });
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            int width = getResources().getDisplayMetrics().widthPixels / 3;
            ImageView imageView = new ImageView(parent.getContext());
            imageView.setLayoutParams(new LinearLayoutCompat.LayoutParams(width, width));

            return new CustomViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            CustomViewHolder viewHolder = (CustomViewHolder) holder;

            // viewHolder.imageView.setImageResource(R.drawable.btn_signin_facebook);
            Glide.with(holder.itemView.getContext()).load(contentDTOs.get(position).getImageUrl()).apply(new RequestOptions().centerCrop()).into(viewHolder.imageView);
        }

        @Override
        public int getItemCount() {
            return contentDTOs.size();
        }

        private class CustomViewHolder extends RecyclerView.ViewHolder {
            private ImageView imageView;

            public CustomViewHolder(ImageView imageView) {
                super(imageView);
                this.imageView = imageView;
            }
        }
    }
}
