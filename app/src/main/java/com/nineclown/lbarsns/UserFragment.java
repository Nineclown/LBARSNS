package com.nineclown.lbarsns;


import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction;
import com.nineclown.lbarsns.databinding.FragmentUserBinding;
import com.nineclown.lbarsns.model.ContentDTO;
import com.nineclown.lbarsns.model.FollowDTO;

import java.util.ArrayList;

public class UserFragment extends Fragment {
    private static final int PICK_PROFILE_FROM_ALBUM = 10;
    private FragmentUserBinding binding;
    private FirebaseFirestore mFirestore;

    // 현재 나의 uid
    private String mCurrentUid;

    // 내가 선택한 uid
    private String mUid;

    public UserFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mFirestore = FirebaseFirestore.getInstance();
        mCurrentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (getArguments() != null) {
            mUid = getArguments().getString("destinationUid");
        }

        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_user, container, false);


        binding.accountBtnFollowSignout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestFollow();
            }
        });
        binding.accountIvProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");

                // 프래그먼트는 결과값을 일로 받기 위해서 이렇게 하나봐. 그러면 결과는 어디서 받아? 여기서 절대 받으면 안댄다.
                // 얘를 갖고 있는 액티비티가 갖고 있기 때문에 MainActivity 로 가서 설정해주면 된다.
                getActivity().startActivityForResult(photoPickerIntent, PICK_PROFILE_FROM_ALBUM);
            }
        });

        binding.accountRecyclerview.setAdapter(new UserFragmentRecyclerViewAdapter());
        binding.accountRecyclerview.setLayoutManager(new GridLayoutManager(getActivity(), 3));

        getProfileImage();
        return binding.getRoot();
    }

    private void requestFollow() {
        final DocumentReference tsDocFollowing = mFirestore.collection("users").document(mCurrentUid);

        // 크게 내 입장, 상대 입장 2가지 트랜잭션이 발생한다.
        // 먼저 내가 걔를 팔로잉 하는 상태와
        // 이후에 걔의 입장에선 내가 걔를 팔로잉 하는 거니까 걔는 팔로워를 조정해야 겠지.
        mFirestore.runTransaction(new Transaction.Function<Void>() {
            @Nullable
            @Override
            public Void apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {

                FollowDTO followDTO = transaction.get(tsDocFollowing).toObject(FollowDTO.class);
                if (followDTO == null) {
                    // 아무도 팔로잉 하지 않은 경우,
                    followDTO = new FollowDTO();
                    followDTO.setFollowingCount(1);
                    followDTO.setFollowings(mUid, true);

                    transaction.set(tsDocFollowing, followDTO);
                    return null;
                }
                // 내가 팔로잉하려는 사람(uid)이 이미 내가 팔로잉 한 상태인 경우.
                if (followDTO.getFollowings().containsKey(mUid)) {
                    followDTO.setFollowingCount(followDTO.getFollowingCount() - 1);
                    followDTO.getFollowings().remove(mUid);

                } else {
                    // 내가 팔로잉하려는 사람을 아직 팔로잉 안한 경우 --> 팔로잉 한다.
                    followDTO.setFollowingCount(followDTO.getFollowingCount() + 1);
                    followDTO.setFollowings(mUid, true);
                }

                transaction.set(tsDocFollowing, followDTO);
                return null;
            }
        });

        final DocumentReference tsDocFollower = mFirestore.collection("users").document(mUid);

        mFirestore.runTransaction(new Transaction.Function<Void>() {
            @Nullable
            @Override
            public Void apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {
                FollowDTO followDTO = transaction.get(tsDocFollower).toObject(FollowDTO.class);

                if (followDTO == null) {
                    //내가 팔로잉 하려는 상대가 아직 팔로워가 없는 경우. 상대를 아무도 팔로잉 안해준 거지.
                    followDTO = new FollowDTO();
                    followDTO.setFollowerCount(1);
                    followDTO.setFollowers(mCurrentUid, true);

                    transaction.set(tsDocFollower, followDTO);
                    return null;
                }
                // 내가 팔로잉 하려는 상대의 입장에선 내가 이미 그를 팔로우 하고 있는 경우, 내가 이미 걔를 팔로우 하고 있는 상태.
                if (followDTO.getFollowers().containsKey(mCurrentUid)) {
                    followDTO.setFollowerCount(followDTO.getFollowerCount() - 1);
                    followDTO.getFollowers().remove(mCurrentUid);


                } else {
                    //내가 팔로잉 하려는 상대의 입장에서 내가 아직 그를 팔로우 안한 경우,
                    followDTO.setFollowerCount(followDTO.getFollowerCount() + 1);
                    followDTO.setFollowers(mCurrentUid, true);

                }
                transaction.set(tsDocFollower, followDTO);
                System.out.println("CurruntUid: " + mCurrentUid + " @@@@@@@@@@여기는 남이 나를 팔로우 해줄때.");
                return null;
            }
        });

    }

    private void getProfileImage() {

        // SnapshotListener() push- driven 형식으로 동작. DB를 계속 쳐다보다가 데이터가 변화하면 그 순간 호출된다.
        mFirestore.collection("profileImages").document(mCurrentUid).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (documentSnapshot.getData() != null) {
                    // store에서 가져온 데이터는 HashMap이다. 그래서 키 값으로 url을 값을 가져옴.
                    String url = (String) documentSnapshot.getData().get("image");

                    Glide.with(getActivity()).load(url).apply(new RequestOptions().circleCrop()).into(binding.accountIvProfile);
                }
            }
        });

    }

    private class UserFragmentRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        // 얘도 마찬가지로 이미지뷰 하나만 쓸꺼라서 따로 xml 파일을 불러올 필요 없다.

        private ArrayList<ContentDTO> contentDTOs;
        private String size;

        public UserFragmentRecyclerViewAdapter() {
            contentDTOs = new ArrayList<>();
            mFirestore.collection("images").whereEqualTo("uid", mCurrentUid).addSnapshotListener(new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                    for (DocumentSnapshot snapshot : queryDocumentSnapshots.getDocuments()) {
                        contentDTOs.add(snapshot.toObject(ContentDTO.class));
                    }
                    size = Integer.toString(contentDTOs.size());
                    binding.accountTvPostCount.setText(size);
                    notifyDataSetChanged();
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

            Glide.with(holder.itemView.getContext()).load(contentDTOs.get(position).getImageUrl()).apply(new RequestOptions().centerCrop()).into(viewHolder.imageView);

        }

        @Override
        public int getItemCount() {
            return contentDTOs.size();
        }

        private class CustomViewHolder extends RecyclerView.ViewHolder {
            // 바인딩할 xml이 없으니까 그냥 바로 이미지뷰 만들어서 갖고 있으면 댐.
            private ImageView imageView;

            public CustomViewHolder(ImageView imageView) {
                super(imageView);
                this.imageView = imageView;

            }
        }
    }

}
