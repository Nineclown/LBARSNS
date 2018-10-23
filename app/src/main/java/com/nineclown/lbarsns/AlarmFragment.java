package com.nineclown.lbarsns;


import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
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

        binding.alarmfragmentRecyclerview.setAdapter(new AlarmRecyclerViewAdapter());
        binding.alarmfragmentRecyclerview.setLayoutManager(new LinearLayoutManager(getActivity()));

        return binding.getRoot();
    }

    private class AlarmRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private ArrayList<AlarmDTO> alarmDTOs;
        private ItemCommentBinding aBinding;

        public AlarmRecyclerViewAdapter() {

            alarmDTOs = new ArrayList<>();

            mFirestore.collection("alarms").whereEqualTo("destinationUid", mUid).orderBy("timestamp", Query.Direction.DESCENDING).addSnapshotListener(new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                    alarmDTOs.clear();
                    if (queryDocumentSnapshots == null) return;

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
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            CustomViewHolder viewHolder = (CustomViewHolder) holder;

            TextView commentTextView = viewHolder.hBinding.commentviewitemTextviewProfile;
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
