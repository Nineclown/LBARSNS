package com.nineclown.lbarsns;


import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
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
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction;
import com.nineclown.lbarsns.databinding.FragmentUserBinding;
import com.nineclown.lbarsns.model.AlarmDTO;
import com.nineclown.lbarsns.model.ContentDTO;
import com.nineclown.lbarsns.model.FollowDTO;

import java.util.ArrayList;

import static com.nineclown.lbarsns.R.id;
import static com.nineclown.lbarsns.R.layout;
import static com.nineclown.lbarsns.R.string.follow;
import static com.nineclown.lbarsns.R.string.signout;

public class UserFragment extends Fragment implements MainActivity.OnBackPressedListener {
    private static final int PICK_PROFILE_FROM_ALBUM = 10;
    private FragmentUserBinding binding;
    private FirebaseFirestore mFirestore;
    private FcmPush fcmPush;
    private MainActivity mainActivity;
    private FirebaseAuth mAuth;
    // 현재 나의 uid
    private String mCurrentUid;

    // 내가 선택한 uid
    private String mUid;


    private ListenerRegistration followListenerRegistration;
    private ListenerRegistration followingListenerRegistration;
    private ListenerRegistration imageProfileListenerRegistration;
    private ListenerRegistration recyclerListenerRegistration;


    public UserFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // 뷰를 생성해주는건 최상단에 위치해야 함.
        binding = DataBindingUtil.inflate(inflater, layout.fragment_user, container, false);

        // 파이어베이스 및 변수 값들 초기화.
        fcmPush = FcmPush.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        mCurrentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // userfragment 에서 mainActivity 값을 갖고 조작하기 위해 가져옴.
        mainActivity = (MainActivity) getActivity();

        // 프로필을 눌렀을 때,
        if (getArguments() != null) {

            mUid = getArguments().getString("destinationUid");
            // 내 프로필을 눌렀을 때, (dailyLife 탭에서)
            if (mUid != null && mUid.equals(mCurrentUid)) {
                binding.accountBtnFollowSignout.setText(signout);
                binding.accountBtnFollowSignout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        getActivity().finish();
                        startActivity(new Intent(getActivity(), LoginActivity.class));
                        mAuth.signOut();
                    }
                });
            }
            // 상대방 프로필을 눌렀을 때,
            else {

                binding.accountBtnFollowSignout.setText(follow);
                mainActivity.getBinding().toolbarBtnBack.setVisibility(View.VISIBLE);
                mainActivity.getBinding().toolbarUsername.setVisibility(View.VISIBLE);
                mainActivity.getBinding().toolbarTitleImage.setVisibility(View.GONE);
                mainActivity.getBinding().toolbarUsername.setText(getArguments().getString("userId"));
                mainActivity.getBinding().toolbarBtnBack.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mainActivity.getBinding().bottomNavigation.setSelectedItemId(id.action_home);
                    }
                });

                binding.accountBtnFollowSignout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        requestFollow();
                    }
                });
            }
        }

        // 내 계정일 경우에만 프로필 사진을 변경시킬 수 있다.
        if (mUid != null && mUid.equals(mCurrentUid)) {
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
        }
        binding.accountRecyclerview.setAdapter(new UserFragmentRecyclerViewAdapter());
        binding.accountRecyclerview.setLayoutManager(new GridLayoutManager(getActivity(), 3));

        getProfileImage();
        getFollower();
        getFollowing();
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        // 화면을 다시 이쪽으로 가져올때 스냅샷도 불러온다. onCreate에 붙여놓는게 더 안정적일 수 있다.
        /*getProfileImage();
        getFollower();
        getFollowing();
        binding.accountRecyclerview.setAdapter(new UserFragmentRecyclerViewAdapter());
        binding.accountRecyclerview.setLayoutManager(new GridLayoutManager(getActivity(), 3));*/


    }

    @Override
    public void onStop() {
        super.onStop();
        // 스냅샷 리스너들이 화면이 이동하면 꺼지도록 구현.
        followListenerRegistration.remove();
        followingListenerRegistration.remove();
        imageProfileListenerRegistration.remove();
        recyclerListenerRegistration.remove();
    }

    private void requestFollow() {
        // collection 에 없는 이름을 작성하면 자동으로 table 이 만들어진다.
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
                    // 알람이 나한테 오는게 아니라 쟤한테 가야지.
                    followerAlarm(mUid);

                }
                transaction.set(tsDocFollower, followDTO);
                return null;
            }
        });

    }

    private void getProfileImage() {
        // SnapshotListener() push- driven 형식으로 동작. DB를 계속 쳐다보다가 데이터가 변화하면 그 순간 호출된다.
        imageProfileListenerRegistration = mFirestore.collection("profileImages").document(mUid).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                // snapshot이 살아 있는데 뷰를 없애버리면 크러쉬가 발생한다.
                if (documentSnapshot == null) return;
                if (documentSnapshot.getData() != null) {
                    // store에서 가져온 데이터는 HashMap이다. 그래서 키 값으로 url을 값을 가져옴.
                    String url = (String) documentSnapshot.getData().get("image");

                    if (getActivity() == null) return;
                    Glide.with(getActivity()).load(url).apply(new RequestOptions().circleCrop()).into(binding.accountIvProfile);
                }
            }
        });

    }

    private void followerAlarm(String destinationUid) {
        AlarmDTO alarmDTO = new AlarmDTO();
        alarmDTO.setDestinationUid(destinationUid);
        alarmDTO.setUserId(mAuth.getCurrentUser().getEmail());
        alarmDTO.setUid(mUid);
        alarmDTO.setKind(2);
        alarmDTO.setTimestamp(System.currentTimeMillis());

        mFirestore.collection("alarms").document().set(alarmDTO);

        String message = mAuth.getCurrentUser().getEmail() + getString(R.string.alarm_follow);
        fcmPush.sendMessage(destinationUid, "알림 메시지", message);
    }

    private void getFollower() {
        followListenerRegistration = mFirestore.collection("users").document(mUid).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (documentSnapshot == null) return;
                FollowDTO followDTO = documentSnapshot.toObject(FollowDTO.class);
                if (followDTO == null) return;
                String count = Integer.toString(followDTO.getFollowerCount());
                binding.accountTvFollowerCount.setText(count);

                if (followDTO.getFollowers().containsKey(mCurrentUid)) {
                    binding.accountBtnFollowSignout.setText(getString(R.string.follow_cancel));
                    binding.accountBtnFollowSignout.setTypeface(null, Typeface.NORMAL);
                    binding.accountBtnFollowSignout.setTextColor(ContextCompat.getColor(getContext(), R.color.colorSkyBlue));
                    binding.accountBtnFollowSignout.getBackground().setColorFilter(null);
                } else {
                    if (mUid != mCurrentUid) {
                        binding.accountBtnFollowSignout.setText(getString(R.string.follow));
                        binding.accountBtnFollowSignout.setTypeface(null, Typeface.BOLD);
                        binding.accountBtnFollowSignout.setTextColor(ContextCompat.getColor(getContext(), R.color.colorWhite));
                        binding.accountBtnFollowSignout.getBackground().setColorFilter(ContextCompat.getColor(getActivity(), R.color.colorDeepBlue), PorterDuff.Mode.MULTIPLY);

                    }
                }
            }
        });

    }

    private void getFollowing() {
        followingListenerRegistration = mFirestore.collection("users").document(mUid).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (documentSnapshot == null) return;
                FollowDTO followDTO = documentSnapshot.toObject(FollowDTO.class);
                if (followDTO == null) return;
                String count = Integer.toString(followDTO.getFollowingCount());
                binding.accountTvFollowingCount.setText(count);
            }
        });
    }

    @Override
    public void onBackPressed() {
        // 리스너를 설정하기 위해 메인을 가져온다. (이미 상단에 전역으로 갖고 있네?)

        // 이 메소드로 들어온다 == 뒤로가기를 눌렀다.
        // null 처리
        if (mainActivity == null) return;
        mainActivity.setOnBackPressedListener(null);


        // [START return to daily life fragment]
        // 프래그먼트 전환 과정


        /*mainActivity.getSupportFragmentManager().beginTransaction()
                .remove(this).commit();
        mainActivity.getSupportFragmentManager()
                .popBackStack();*/

        /*DailyLifeFragment dailyLifeFragment = new DailyLifeFragment();
        mainActivity.getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_content, dailyLifeFragment)
                .addToBackStack(null)
                .commit();*/

        // BottomNavigationView 전환 과정
        mainActivity.getBinding().bottomNavigation.setSelectedItemId(id.action_home);
        // [END return to daily life fragment]
    }


    // Fragment 호출시 반드시 호출되는 오버라이드 메소드라는데, 역할이 머야.
    // 어떤 액티비티에 붙을건지를 설정하는 것 같음.
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        ((MainActivity) context).setOnBackPressedListener(this);
    }

    private class UserFragmentRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        // 이미지뷰 하나만 쓸꺼라서 따로 xml 파일을 불러올 필요 없다.

        private ArrayList<ContentDTO> contentDTOs;
        private String size;

        public UserFragmentRecyclerViewAdapter() {
            contentDTOs = new ArrayList<>();
            recyclerListenerRegistration = mFirestore.collection("images").whereEqualTo("uid", mUid).addSnapshotListener(new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                    contentDTOs.clear();
                    if (queryDocumentSnapshots == null) return;
                    //assert queryDocumentSnapshots != null;
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

            Glide.with(holder.itemView.getContext())
                    .load(contentDTOs.get(position).getImageUrl())
                    .apply(new RequestOptions().centerCrop()).into(viewHolder.imageView);

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
