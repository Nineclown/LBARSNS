package com.nineclown.lbarsns;


import android.content.Context;
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
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.nineclown.lbarsns.databinding.FragmentInfoBinding;
import com.nineclown.lbarsns.model.ContentDTO;

import java.util.ArrayList;

public class InfoFragment extends Fragment implements MainActivity.OnBackPressedListener{

    private FirebaseFirestore mFirestore;
    private FragmentInfoBinding binding;
    private ListenerRegistration infoListenerRegistration;

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

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        binding.infofragmentRecyclerview.setAdapter(new InfoFragmentRecyclerViewAdapter());
        binding.infofragmentRecyclerview.setLayoutManager(new GridLayoutManager(getActivity(), 3));

    }

    @Override
    public void onStop() {
        super.onStop();
        infoListenerRegistration.remove();
    }

    @Override
    public void onBackPressed() {
        // 리스너를 설정하기 위해 메인을 가져온다. (이미 상단에 전역으로 갖고 있네?)
        MainActivity mainActivity = (MainActivity) getActivity();
        // 이 메소드로 들어온다 == 뒤로가기를 눌렀다.
        // null 처리
        if (mainActivity == null) return;
        mainActivity.setOnBackPressedListener(null);


        // [START return to daily life fragment]
        // 프래그먼트 전환 과정
        DailyLifeFragment dailyLifeFragment = new DailyLifeFragment();
        getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.main_content, dailyLifeFragment).commit();

        // BottomNavigationView 전환 과정
        mainActivity.getBinding().bottomNavigation.setSelectedItemId(R.id.action_home);
        // [END return to daily life fragment]
    }


    // Fragment 호출시 반드시 호출되는 오버라이드 메소드라는데, 역할이 머야.
    // 어떤 액티비티에 붙을건지를 설정하는 것 같음.
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        ((MainActivity) context).setOnBackPressedListener(this);
    }

    private class InfoFragmentRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private ArrayList<ContentDTO> contentDTOs;

        public InfoFragmentRecyclerViewAdapter() {
            contentDTOs = new ArrayList<>();

            infoListenerRegistration = mFirestore.collection("images").orderBy("timestamp")
                    .addSnapshotListener(new EventListener<QuerySnapshot>() {
                        @Override
                        public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                            if (queryDocumentSnapshots == null) return;
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
        public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
            CustomViewHolder viewHolder = (CustomViewHolder) holder;

            // viewHolder.imageView.setImageResource(R.drawable.btn_signin_facebook);
            Glide.with(holder.itemView.getContext()).load(contentDTOs.get(position).getImageUrl())
                    .apply(new RequestOptions().centerCrop()).into(viewHolder.imageView);
            viewHolder.imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    UserFragment userFragment = new UserFragment();
                    Bundle bundle = new Bundle();
                    bundle.putString("destinationUid", contentDTOs.get(position).getUid());
                    bundle.putString("userId", contentDTOs.get(position).getUserId());

                    userFragment.setArguments(bundle);

                    getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.main_content, userFragment).commit();

                }
            });
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
