package com.nineclown.lbarsns.sns;


import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Transaction;
import com.nineclown.lbarsns.R;
import com.nineclown.lbarsns.camera.CameraActivity;
import com.nineclown.lbarsns.databinding.FragmentDailyLifeBinding;
import com.nineclown.lbarsns.databinding.ItemDailyBinding;
import com.nineclown.lbarsns.model.AlarmDTO;
import com.nineclown.lbarsns.model.ContentDTO;
import com.nineclown.lbarsns.model.FollowDTO;
import com.nineclown.lbarsns.service.GPSService;

import java.util.ArrayList;
import java.util.HashMap;

public class DailyLifeFragment extends Fragment {
    private FragmentDailyLifeBinding binding;
    private FcmPush fcmPush;
    private FirebaseFirestore mFirestore;
    private FirebaseAuth mAuth;
    private String mUid;
    private ListenerRegistration imageListenerRegistration;
    private MainActivity mainActivity;

    public DailyLifeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_daily_life, container, false);

        // firebase 및 변수들 초기화.
        mFirestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        mUid = mAuth.getCurrentUser().getUid();
        fcmPush = FcmPush.getInstance();
        mainActivity = (MainActivity) getActivity();

        // 툴바 보이게 안보이게 작업하는 부분.
        mainActivity.getBinding().toolbarBtnAr.setVisibility(View.VISIBLE);
        mainActivity.getBinding().toolbarBtnAr.setOnClickListener(v -> {
            // 프래그먼트에서 액티비티를 호출하는 부분. 호출의 주체 및 결과는 프래그먼트를 갖고있는 액티비티에서 처리한다.
            Intent intent = new Intent(mainActivity, CameraActivity.class);
            //Intent intent = new Intent(getActivity(), CameraActivity.class);
            mainActivity.startActivity(intent);
            //getActivity().startActivity(intent);
        });

        if (isMyServiceRunning(GPSService.class)) {
            mainActivity.getBinding().toolbarBtnRecord.setVisibility(View.VISIBLE);
            mainActivity.getBinding().toolbarBtnRecord.setOnClickListener(v -> {
                Intent intent = new Intent(mainActivity, GPSService.class);
                intent.putExtra("travel", "end");
                //Intent intent = new Intent(getActivity(), CameraActivity.class);
                mainActivity.stopService(intent);
                //getActivity().startActivity(intent);
                mainActivity.getBinding().toolbarBtnRecord.setVisibility(View.GONE);
            });
        }

        // 리사이클러 뷰 생성 및 선언은 제일 하단으로 처리함. 왜?
        binding.dailylifefragmentRecyclerview.setAdapter(new DailyLifeRecyclerViewAdapter());
        binding.dailylifefragmentRecyclerview.setLayoutManager(new LinearLayoutManager(getActivity()));

        return binding.getRoot();
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) mainActivity.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onStop() {
        super.onStop();
        if (imageListenerRegistration != null)
            imageListenerRegistration.remove();
    }

    private class DailyLifeRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private ArrayList<ContentDTO> contentDTOs;
        private ArrayList<String> contentUidList;
        private ItemDailyBinding aBinding;

        private DailyLifeRecyclerViewAdapter() {
            // 게시글을 담기 위한 리스트 초기화.
            contentDTOs = new ArrayList<>();
            contentUidList = new ArrayList<>();

            // 뷰가 처음 만들어질 때, 여기 호출이 안되던데?
            mFirestore.collection("users").document(mUid).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FollowDTO userDTO = task.getResult().toObject(FollowDTO.class);
                    if (userDTO != null) {
                        getContents(userDTO.getFollowings());
                    }
                }
            });
        }


        private void getContents(final HashMap<String, Boolean> following) {
            imageListenerRegistration = mFirestore.collection("images").orderBy("timestamp", Query.Direction.DESCENDING)
                    .addSnapshotListener((queryDocumentSnapshots, e) -> {
                        // snapshot 이 push-driven 방식이라서, DB가 바뀐걸 알아채고 아래 연산을 수행한다.
                        if (queryDocumentSnapshots == null) return;
                        contentDTOs.clear();
                        contentUidList.clear(); //이거 있으면 머가 달라지냐?
                        for (DocumentSnapshot snapshot : queryDocumentSnapshots.getDocuments()) {
                            //DB에 있는 데이터를 snapshot 변수에 담은 후에, ContentDTO 데이터 형식으로 변환.
                            ContentDTO item = snapshot.toObject(ContentDTO.class);
                            // 모든 이미지를 다돌아다니면서 현재 로그인한 사용자가 팔로잉하고 있는 사람의 글을 가져온다.
                            if (mUid.equals(item.getUid()) || following.keySet().contains(item.getUid())) {
                                contentDTOs.add(item);
                                // getId()를 통해 document 의 id를 가져옴.
                                contentUidList.add(snapshot.getId());
                            }
                        }
                        // 그리고 마지막에 notify를 호출해서 view를 업데이트.
                        notifyDataSetChanged();
                    });
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // 뷰를 설정하는 곳. 가져오는 곳.
            aBinding = ItemDailyBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new CustomViewHolder(aBinding);

        }

        @Override
        public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, @SuppressLint("RecyclerView") final int position) {
            // 뷰 안의 데이터를 설정하는 곳.
            final CustomViewHolder viewHolder = (CustomViewHolder) holder;

            mFirestore.collection("profileImages")
                    .document(contentDTOs.get(position).getUid()).get()
                    .addOnCompleteListener(task -> {
                        // 사진에 프로필
                        if (task.isSuccessful()) {
                            Object url = task.getResult().get("image");
                            Glide.with(holder.itemView.getContext()).load(url)
                                    .apply(new RequestOptions().circleCrop())
                                    .into(viewHolder.hBinding.dailyviewitemIvProfile);
                        }
                    });
            // 유저 아이디
            viewHolder.hBinding.dailyviewitemTvProfile.setText(contentDTOs.get(position).getUserId());
            //iBinding.detailviewitemProfileTextview.setText(contentDTOs.get(position).getUserId()); 이렇게 하면 뷰홀더를 안거쳐서 안되는 건가??

            // 이미지.  콜백 방식. 이미지를 ~~한 다음에 마지막에 into()로 결과 값을 받아서 뷰에 집어넣는대. 스레드?
            Glide.with(holder.itemView.getContext())
                    .load(contentDTOs.get(position).getImageUrl())
                    .into(viewHolder.hBinding.dailyviewitemIvContent);

            // 설명 텍스트
            viewHolder.hBinding.dailyviewitemTvExplain.setText(contentDTOs.get(position).getExplain());

            // 좋아요 카운터 설정
            String memo = "좋아요 " + contentDTOs.get(position).getFavoriteCount() + "개";
            viewHolder.hBinding.dailyviewitemTvFavoritecounter.setText(memo);
            viewHolder.hBinding.dailyviewitemImageviewFavorite.setOnClickListener(v -> favoriteEvent(position));

            // 좋아요 클릭
            if (contentDTOs.get(position).getFavorites().containsKey(mUid)) {
                viewHolder.hBinding.dailyviewitemImageviewFavorite.setImageResource(R.drawable.ic_favorite);
            }
            // 좋아요 다시 클릭.
            else {
                viewHolder.hBinding.dailyviewitemImageviewFavorite.setImageResource(R.drawable.ic_favorite_border);
            }

            // 게시글의 프로필 클릭. (프래그먼트 이동)
            viewHolder.hBinding.dailyviewitemIvProfile.setOnClickListener(v -> {
                Fragment fragment = new UserFragment();
                Bundle bundle = new Bundle();
                bundle.putString("destinationUid", contentDTOs.get(position).getUid());
                bundle.putString("userId", contentDTOs.get(position).getUserId());
                // 액티비티의 PutExtra와 같은 개념이 프래그먼트에서 Argument라고 생각하면 된다.
                // 시작할 때만 사용할 수 있다,
                fragment.setArguments(bundle);

                //getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.main_content, fragment).commit();
                mainActivity.getSupportFragmentManager().beginTransaction().replace(R.id.main_content, fragment).commit();
            });

            // 코멘트 클릭 할 때.
            viewHolder.hBinding.dailyviewitemImageviewComment.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), CommentActivity.class);
                intent.putExtra("contentUid", contentUidList.get(position));
                intent.putExtra("destinationUid", contentDTOs.get(position).getUid());
                // 컨텍스트를 받아 오는 방법은 다양하다고 한다. View를 통해서 가져올 수도 있고, 근데 난 잘 몰라. 컨텍스트가 머하는 놈인지도 잘 몰라.
                startActivity(intent);
            });

        }

        @Override
        public int getItemCount() {
            return contentDTOs.size();
        }


        // 좋아요를 눌렀을 때 발생하는 이벤트
        private void favoriteEvent(final int position) {
            final DocumentReference tsDoc = mFirestore.collection("images").document(contentUidList.get(position));
            mFirestore.runTransaction((Transaction.Function<Void>) transaction -> {
                DocumentSnapshot snapshot = transaction.get(tsDoc);
                ContentDTO contentDTO = snapshot.toObject(ContentDTO.class);

                // 좋아요가 이미 눌려진 상태.
                if (contentDTO == null) return null;
                if (contentDTO.getFavorites().containsKey(mUid)) {
                    contentDTO.setFavoriteCount(contentDTO.getFavoriteCount() - 1);
                    contentDTO.getFavorites().remove(mUid);

                    // 좋아요를 아직 안누른 상태.
                } else {
                    contentDTO.setFavorites(mUid, true);
                    contentDTO.setFavoriteCount(contentDTO.getFavoriteCount() + 1);
                    favoriteAlarm(contentDTOs.get(position).getUid());
                }

                transaction.set(tsDoc, contentDTO);
                return null;
            });

        }

        // 좋아요를 누르면 알람을 보내는 함수.
        private void favoriteAlarm(String destinationUid) {
            AlarmDTO alarmDTO = new AlarmDTO();
            alarmDTO.setDestinationUid(destinationUid);
            alarmDTO.setUserId(mAuth.getCurrentUser().getEmail());
            alarmDTO.setUid(mUid);
            alarmDTO.setKind(0);
            alarmDTO.setTimestamp(System.currentTimeMillis());

            mFirestore.collection("alarms").document().set(alarmDTO);
            String message = mAuth.getCurrentUser().getEmail() + getString(R.string.alarm_favorite);
            fcmPush.sendMessage(destinationUid, "알림 메시지", message);

        }

        private class CustomViewHolder extends RecyclerView.ViewHolder {
            private ItemDailyBinding hBinding;

            public CustomViewHolder(ItemDailyBinding aBinding) {
                super(aBinding.getRoot());
                this.hBinding = aBinding;

            }

        }
    }


}
