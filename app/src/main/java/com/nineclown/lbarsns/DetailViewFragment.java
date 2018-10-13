package com.nineclown.lbarsns;


import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.nineclown.lbarsns.databinding.FragmentDetailViewBinding;
import com.nineclown.lbarsns.databinding.ItemDetailBinding;
import com.nineclown.lbarsns.model.ContentDTO;

import java.util.ArrayList;

public class DetailViewFragment extends Fragment {

    private FragmentDetailViewBinding fBinding;
    private FirebaseFirestore mFirestore;

    public DetailViewFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mFirestore = FirebaseFirestore.getInstance();

        // Inflate the layout for this fragment
        fBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_detail_view, container, false);

        fBinding.detailViewFragmentRecyclerview.setAdapter(new DetailRecyclerViewAdapter());
        fBinding.detailViewFragmentRecyclerview.setLayoutManager(new LinearLayoutManager(getActivity()));

        return fBinding.getRoot();
    }

    private class DetailRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private ArrayList<ContentDTO> contentDTOs;
        private ArrayList<String> contentUidList;
        private ItemDetailBinding iBinding;

        public DetailRecyclerViewAdapter () {

            contentDTOs = new ArrayList<>();
            contentUidList = new ArrayList<>();

            // 현재 로그인된 유저의 UID(해쉬 키)
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

            mFirestore.collection("images").orderBy("timestamp").addSnapshotListener(new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(QuerySnapshot queryDocumentSnapshots, FirebaseFirestoreException e) {
                    contentDTOs.clear();
                    for(DocumentSnapshot snapshot : queryDocumentSnapshots.getDocuments()) {
                        //DB에 있는 데이터를 snapshot이라는 변수에 담은 후에, ContentDTO 데이터 형식으로 변환.
                        ContentDTO item = snapshot.toObject(ContentDTO.class);
                        contentDTOs.add(item);
                        contentUidList.add(item.getUid());
                    }

                    // 새로고침 해주는 역할. push-driven 방식이라서,
                    // DB가 바뀐걸 감지할 때마다 뿌려주기 위해 mFireStore.collection()~~~ 이 구문 안에 있어야 한다.
                    notifyDataSetChanged();
                }
            });
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            // 뷰를 설정하는 곳. 가져오는 곳.
            iBinding = ItemDetailBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new CustomViewHolder(iBinding);

        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            // 뷰 안의 데이터를 설정하는 곳.
            CustomViewHolder viewHolder = (CustomViewHolder)holder;

            // 유저 아이디
            viewHolder.iBinding.detailviewitemProfileTextview.setText(contentDTOs.get(position).getUserId());

            // 이미지.  콜백 방식. 이미지를 ~~한 다음에 마지막에 into()로 결과 값을 받아서 뷰에 집어넣는대. 스레드?
            Glide.with(holder.itemView.getContext()).load(contentDTOs.get(position).getImageUrl()).into(viewHolder.iBinding.detailviewitemImageviewContent);

            // 설명 텍스트
            viewHolder.iBinding.detailviewitemExplainTextview.setText(contentDTOs.get(position).getExplain());

            // 좋아요 카운터 설정
            String memo = "좋아요 " + contentDTOs.get(position).getFavoriteCount() + "개";
            viewHolder.iBinding.detailviewitemFavoritecounterTextview.setText(memo);

        }

        @Override
        public int getItemCount() {
            return contentDTOs.size();
        }

        private class CustomViewHolder extends RecyclerView.ViewHolder {
            private ItemDetailBinding iBinding;

            public CustomViewHolder(ItemDetailBinding binding) {
                super(binding.getRoot());
                this.iBinding = binding;

            }

        }
    }


}
