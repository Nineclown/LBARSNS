package com.nineclown.lbarsns.sns;


import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.nineclown.lbarsns.R;
import com.nineclown.lbarsns.databinding.FragmentAlarmBinding;
import com.nineclown.lbarsns.databinding.ItemCommentBinding;
import com.nineclown.lbarsns.model.AlarmDTO;

import java.util.ArrayList;

public class AlarmFragment extends Fragment implements MainActivity.OnBackPressedListener {

    private FragmentAlarmBinding binding;
    private MainActivity mainActivity;
    private FirebaseFirestore mFirestore;
    private FirebaseAuth mAuth;
    private String mUid;

    private ListenerRegistration alarmListenerRegistration;

    public AlarmFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_alarm, container, false);

        // firebase
        mFirestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        mUid = mAuth.getCurrentUser().getUid();

        mainActivity = (MainActivity) getActivity();

        binding.alarmfragmentRecyclerview.setAdapter(new AlarmRecyclerViewAdapter());
        binding.alarmfragmentRecyclerview.setLayoutManager(new LinearLayoutManager(getActivity()));

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onStop() {
        super.onStop();
        alarmListenerRegistration.remove();
    }

    @Override
    public void onBackPressed() {
        // 리스너를 설정하기 위해 메인을 가져온다. (이미 상단에 전역으로 갖고 있네?)
        // 이 메소드로 들어온다 == 뒤로가기를 눌렀다.
        // null 처리
        if (mainActivity == null) return;
        mainActivity.setOnBackPressedListener(null);

        // [START return to daily life fragment]
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

    private class AlarmRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private ArrayList<AlarmDTO> alarmDTOs;
        private ItemCommentBinding aBinding;

        public AlarmRecyclerViewAdapter() {
            alarmDTOs = new ArrayList<>();

            alarmListenerRegistration = mFirestore.collection("alarms")
                    .whereEqualTo("destinationUid", mUid)
                    .orderBy("timestamp", Query.Direction.DESCENDING).addSnapshotListener((queryDocumentSnapshots, e) -> {
                        alarmDTOs.clear();
                        if (queryDocumentSnapshots == null) return;
                        for (DocumentSnapshot snapshot : queryDocumentSnapshots.getDocuments()) {
                            alarmDTOs.add(snapshot.toObject(AlarmDTO.class));
                        }
                        notifyDataSetChanged();
                    });
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            //View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
            aBinding = ItemCommentBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);

            return new CustomViewHolder(aBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, int position) {
            final CustomViewHolder viewHolder = (CustomViewHolder) holder;

            final ImageView profileImageView = viewHolder.hBinding.commentviewitemImageviewProfile;
            TextView commentTextView = viewHolder.hBinding.commentviewitemTextviewProfile;

            mFirestore.collection("profileImages")
                    .document(alarmDTOs.get(position).getUid()).get()
                    .addOnCompleteListener(task -> {
                        // 사진에 프로필
                        if (task.isSuccessful()) {
                            //Object url = task.getResult().get("image");
                            Object url = task.getResult().get("image");
                            Glide.with(holder.itemView.getContext()).load(url)
                                    .apply(new RequestOptions().circleCrop())
                                    .into(profileImageView);
                        }
                    });


            switch (alarmDTOs.get(position).getKind()) {
                case 0:
                    String str_0 = alarmDTOs.get(position).getUserId() + getString(R.string.alarm_favorite);
                    commentTextView.setText(str_0);
                    break;
                case 1:
                    String str_1 = alarmDTOs.get(position).getUserId() +
                            getString(R.string.alarm_who) +
                            alarmDTOs.get(position).getMessage() +
                            getString(R.string.alarm_comment);
                    commentTextView.setText(str_1);
                    break;
                case 2:
                    String str_2 = alarmDTOs.get(position).getUserId() + getString(R.string.alarm_follow);
                    commentTextView.setText(str_2);
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return alarmDTOs.size();
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
