package com.nineclown.lbarsns;


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
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.nineclown.lbarsns.databinding.FragmentAlarmBinding;
import com.nineclown.lbarsns.databinding.ItemCommentBinding;
import com.nineclown.lbarsns.model.AlarmDTO;

import java.util.ArrayList;

public class AlarmFragment extends Fragment {

    private FragmentAlarmBinding binding;

    private FirebaseFirestore mFirestore;
    private FirebaseAuth mAuth;
    private String mUid;

    private ListenerRegistration alarmListenerRegistration;

    public AlarmFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_alarm, container, false);

        // firebase
        mFirestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        mUid = mAuth.getCurrentUser().getUid();
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        binding.alarmfragmentRecyclerview.setAdapter(new AlarmRecyclerViewAdapter());
        binding.alarmfragmentRecyclerview.setLayoutManager(new LinearLayoutManager(getActivity()));

    }

    @Override
    public void onStop() {
        super.onStop();
        alarmListenerRegistration.remove();
    }

    private class AlarmRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private ArrayList<AlarmDTO> alarmDTOs;
        private ItemCommentBinding aBinding;

        public AlarmRecyclerViewAdapter() {

            alarmDTOs = new ArrayList<>();

            alarmListenerRegistration = mFirestore.collection("alarms").whereEqualTo("destinationUid", mUid)
                    .orderBy("timestamp", Query.Direction.DESCENDING).addSnapshotListener(new EventListener<QuerySnapshot>() {
                        @Override
                        public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                            if (queryDocumentSnapshots == null) return;
                            alarmDTOs.clear();
                            for (DocumentSnapshot snapshot : queryDocumentSnapshots.getDocuments()) {
                                alarmDTOs.add(snapshot.toObject(AlarmDTO.class));
                            }

                            notifyDataSetChanged();
                        }
                    });
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            //View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
            aBinding = ItemCommentBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);

            return new CustomViewHolder(aBinding);
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
            final CustomViewHolder viewHolder = (CustomViewHolder) holder;

            final ImageView profileImageView = viewHolder.hBinding.commentviewitemImageviewProfile;
            TextView commentTextView = viewHolder.hBinding.commentviewitemTextviewProfile;

            mFirestore.collection("profileImages")
                    .document(alarmDTOs.get(position).getUid()).get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            // 사진에 프로필
                            if (task.isSuccessful()) {
                                Object url = task.getResult().get("image");
                                Glide.with(holder.itemView.getContext()).load(url)
                                        .apply(new RequestOptions().circleCrop())
                                        .into(profileImageView);
                            }
                        }
                    });


            switch (alarmDTOs.get(position).getKind()) {
                case 0:
                    String str_0 = alarmDTOs.get(position).getUserId() + getString(R.string.alarm_favorite);
                    commentTextView.setText(str_0);
                    break;
                case 1:
                    String str_1 = alarmDTOs.get(position).getUserId() +
                            getString(R.string.alarm_who) + " \"" +
                            alarmDTOs.get(position).getMessage() + "\" " +
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
