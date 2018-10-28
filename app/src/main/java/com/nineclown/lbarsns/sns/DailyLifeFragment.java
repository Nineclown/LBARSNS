package com.nineclown.lbarsns.sns;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction;
import com.nineclown.lbarsns.service.FcmPush;
import com.nineclown.lbarsns.R;
import com.nineclown.lbarsns.camera.CameraActivity;
import com.nineclown.lbarsns.databinding.FragmentDailyLifeBinding;
import com.nineclown.lbarsns.databinding.ItemDailyBinding;
import com.nineclown.lbarsns.model.AlarmDTO;
import com.nineclown.lbarsns.model.ContentDTO;
import com.nineclown.lbarsns.model.FollowDTO;

import java.util.ArrayList;
import java.util.HashMap;

public class DailyLifeFragment extends Fragment {
    private FragmentDailyLifeBinding binding;
    private FcmPush fcmPush;
    private FirebaseFirestore mFirestore;
    private FirebaseAuth mAuth;
    private String mUid;
    private ListenerRegistration imageListenerRegistration;
    private boolean alone;
    private MainActivity mainActivity;

    public DailyLifeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_daily_life, container, false);

        // firebase
        mFirestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        mUid = mAuth.getCurrentUser().getUid();
        fcmPush = FcmPush.getInstance();
        alone = true;
        mainActivity = (MainActivity) getActivity();

        // 일단 리사이클러 뷰를 위로 올림.
        binding.dailylifefragmentRecyclerview.setAdapter(new DailyLifeRecyclerViewAdapter());
        binding.dailylifefragmentRecyclerview.setLayoutManager(new LinearLayoutManager(getActivity()));

        mainActivity.getBinding().toolbarBtnAr.setVisibility(View.VISIBLE);
        mainActivity.getBinding().toolbarBtnAr.setOnClickListener(v -> {
            Intent intent = new Intent(mainActivity, CameraActivity.class);
            //Intent intent = new Intent(getActivity(), CameraActivity.class);

            // 프래그먼트는 결과값을 일로 받기 위해서 이렇게 하나봐. 그러면 결과는 어디서 받아? 여기서 절대 받으면 안댄다.
            // 얘를 갖고 있는 액티비티가 갖고 있기 때문에 MainActivity 로 가서 설정해주면 된다.
            mainActivity.startActivity(intent);
            //getActivity().startActivity(intent);
        });
        return binding.getRoot();
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

        public DailyLifeRecyclerViewAdapter() {

            contentDTOs = new ArrayList<>();
            contentUidList = new ArrayList<>();

            // 내가 팔로잉 하는 사람의 데이터를 가져온다.
            mFirestore.collection("users").document(mUid).get().addOnCompleteListener(task -> {
                alone = false;
                if (task.isSuccessful()) {
                    FollowDTO userDTO = task.getResult().toObject(FollowDTO.class);
                    if (userDTO != null) {
                        getContents(userDTO.getFollowings());
                    }
                }
            });

            // 팔로잉 하는 사람이 없을 경우
            if (alone) {
                getContents();
            }
        }


        private void getContents(final HashMap<String, Boolean> following) {
            // 이미지를 가져오는 코드
            imageListenerRegistration = mFirestore.collection("images").orderBy("timestamp", Query.Direction.DESCENDING)
                    .addSnapshotListener((queryDocumentSnapshots, e) -> {
                        if (queryDocumentSnapshots == null) return;
                        contentDTOs.clear();
                        contentUidList.clear(); //이거 있으면 머가 달라지냐?
                        for (DocumentSnapshot snapshot : queryDocumentSnapshots.getDocuments()) {
                            //DB에 있는 데이터를 snapshot이라는 변수에 담은 후에, ContentDTO 데이터 형식으로 변환.
                            ContentDTO item = snapshot.toObject(ContentDTO.class);

                            // 모든 이미지를 다돌아다니면서 현재 로그인한 사용자가 팔로잉하고 있는 사람의 글을 가져온다.
                            if (mUid.equals(item.getUid()) || following.keySet().contains(item.getUid())) {
                                contentDTOs.add(item);
                                contentUidList.add(snapshot.getId());
                            }
                        }

                        // 새로고침 해주는 역할. push-driven 방식이라서,
                        // DB가 바뀐걸 감지할 때마다 뿌려주기 위해 mFireStore.collection()~~~ 이 구문 안에 있어야 한다.
                        notifyDataSetChanged();
                    });
        }

        private void getContents() {
            // 이미지를 가져오는 코드
            imageListenerRegistration = mFirestore.collection("images").orderBy("timestamp", Query.Direction.DESCENDING)
                    .addSnapshotListener((queryDocumentSnapshots, e) -> {
                        if (queryDocumentSnapshots == null) return;
                        contentDTOs.clear();
                        contentUidList.clear(); //이거 있으면 머가 달라지냐?
                        for (DocumentSnapshot snapshot : queryDocumentSnapshots.getDocuments()) {
                            //DB에 있는 데이터를 snapshot이라는 변수에 담은 후에, ContentDTO 데이터 형식으로 변환.
                            ContentDTO item = snapshot.toObject(ContentDTO.class);

                            // 모든 이미지를 다돌아다니면서 현재 로그인한 사용자인 image만 가져온다.
                            if (mUid.equals(item.getUid())) {
                                contentDTOs.add(item);
                                contentUidList.add(snapshot.getId());
                            }
                        }

                        // 새로고침 해주는 역할. push-driven 방식이라서,
                        // DB가 바뀐걸 감지할 때마다 뿌려주기 위해 mFireStore.collection()~~~ 이 구문 안에 있어야 한다.
                        notifyDataSetChanged();
                    });
        }

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
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            // 사진에 프로필
                            if (task.isSuccessful()) {
                                Object url = task.getResult().get("image");
                                Glide.with(holder.itemView.getContext()).load(url)
                                        .apply(new RequestOptions().circleCrop())
                                        .into(viewHolder.hBinding.dailyviewitemIvProfile);
                            }
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
            viewHolder.hBinding.dailyviewitemImageviewFavorite.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    favoriteEvent(position);
                }
            });

            // 좋아요 클릭
            if (contentDTOs.get(position).getFavorites().containsKey(mUid)) {
                viewHolder.hBinding.dailyviewitemImageviewFavorite.setImageResource(R.drawable.ic_favorite);

                // 좋아요 다시 클릭.
            } else {
                viewHolder.hBinding.dailyviewitemImageviewFavorite.setImageResource(R.drawable.ic_favorite_border);
            }

            // 게시 글의 프로필 클릭. (프래그먼트 이동)
            viewHolder.hBinding.dailyviewitemIvProfile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Fragment fragment = new UserFragment();
                    Bundle bundle = new Bundle();
                    bundle.putString("destinationUid", contentDTOs.get(position).getUid());
                    bundle.putString("userId", contentDTOs.get(position).getUserId());
                    // 액티비티의 PutExtra와 같은 개념이 프래그먼트에서 Argument라고 생각하면 된다.
                    // 시작할 때만 사용할 수 있다,
                    fragment.setArguments(bundle);
                    getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.main_content, fragment).commit();
                }
            });

            // 코멘트 클릭 할 때.
            viewHolder.hBinding.dailyviewitemImageviewComment.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(v.getContext(), CommentActivity.class);

                    intent.putExtra("contentUid", contentUidList.get(position));
                    intent.putExtra("destinationUid", contentDTOs.get(position).getUid());
                    // 컨텍스트를 받아 오는 방법은 다양하다고 한다. View를 통해서 가져올 수도 있고, 근데 난 잘 몰라. 컨텍스트가 머하는 놈인지도 잘 몰라.
                    startActivity(intent);
                }
            });

        }

        @Override
        public int getItemCount() {
            return contentDTOs.size();
        }


        // 좋아요를 눌렀을 때 발생하는 이벤트
        private void favoriteEvent(final int position) {
            final DocumentReference tsDoc = mFirestore.collection("images").document(contentUidList.get(position));
            mFirestore.runTransaction(new Transaction.Function<Void>() {
                @Nullable
                @Override
                public Void apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {
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
                }
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
