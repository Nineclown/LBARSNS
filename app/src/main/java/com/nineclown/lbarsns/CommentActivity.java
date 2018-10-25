package com.nineclown.lbarsns;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.nineclown.lbarsns.databinding.ActivityCommentBinding;
import com.nineclown.lbarsns.databinding.ItemCommentBinding;
import com.nineclown.lbarsns.model.AlarmDTO;
import com.nineclown.lbarsns.model.ContentDTO;

import java.util.ArrayList;

public class CommentActivity extends AppCompatActivity {
    private ActivityCommentBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private FcmPush fcmPush;
    private String mUid;
    private String contentUid;
    private String destinationUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_comment);

        // firebase
        mFirestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        mUid = mAuth.getCurrentUser().getUid();
        fcmPush = FcmPush.getInstance();

        // fragment는 argument, activity는 intent를 통해 데이터를 받아온다.
        // 그리고 content의 id를 받아오는 부분이 recyclerview를 붙이는 부분 보다 위에 위치해야 한다. 아래에 위치하면 null 이 들어감.
        contentUid = getIntent().getStringExtra("contentUid");
        destinationUid = getIntent().getStringExtra("destinationUid");

        // 리사이클러 뷰를 액티비티에 붙이는 부분?
        binding.commentRecyclerview.setAdapter(new CommentRecyclerViewAdapter());
        binding.commentRecyclerview.setLayoutManager(new LinearLayoutManager(this));
        binding.commentBtnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ContentDTO.Comment comment = new ContentDTO.Comment();
                comment.setUserId(mAuth.getCurrentUser().getEmail());
                comment.setComment(binding.commentEditMessage.getText().toString());
                comment.setUid(mUid);
                comment.setTimestamp(System.currentTimeMillis());
                if (contentUid == null) {
                    System.out.println("여긴 오면 안대!!!!!!!!!!");
                    return;
                }
                mFirestore.collection("images").document(contentUid).collection("comments").document().set(comment);
                commentAlarm(destinationUid, binding.commentEditMessage.getText().toString());
                // 입력 후, edittext 초기화.
                binding.commentEditMessage.setText("");
            }
        });
    }

    private void commentAlarm(String destinationUid, String message) {
        AlarmDTO alarmDTO = new AlarmDTO();
        alarmDTO.setDestinationUid(destinationUid);
        alarmDTO.setMessage(message);
        alarmDTO.setUserId(mAuth.getCurrentUser().getEmail());
        alarmDTO.setUid(mUid);
        alarmDTO.setKind(1);
        alarmDTO.setTimestamp(System.currentTimeMillis());

        mFirestore.collection("alarms").document().set(alarmDTO);

        String msg = mAuth.getCurrentUser().getEmail() + getString(R.string.alarm_who) + message + getString(R.string.alarm_comment);
        fcmPush.sendMessage(destinationUid, "알림 메시지", message);
    }

    private class CommentRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private ItemCommentBinding iBinding;
        private ArrayList<ContentDTO.Comment> comments;
        // xml 파일을 우선 불러와야 한다.

        public CommentRecyclerViewAdapter() {
            comments = new ArrayList<>();
            mFirestore.collection("images").document(contentUid).collection("comments").orderBy("timestamp").addSnapshotListener(new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                    // 추가될때마다 호출되는 메소드기 때문에, 새로 싹다 보여줘야 해서 comment 를 밀어버린다.
                    comments.clear();
                    if (queryDocumentSnapshots == null) return;
                    for (DocumentSnapshot snapshot : queryDocumentSnapshots.getDocuments()) {
                        //DB에 있는 데이터를 snapshot이라는 변수에 담은 후에, ContentDTO 데이터 형식으로 변환.
                        comments.add(snapshot.toObject(ContentDTO.Comment.class));
                    }
                    notifyDataSetChanged();
                }
            });
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            // 뷰를 설정하는 곳. 가져오는 곳.
            iBinding = ItemCommentBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new CustomViewHolder(iBinding);

        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
            final CustomViewHolder viewHolder = (CustomViewHolder) holder;

            mFirestore.collection("profileImages")
                    .document(comments.get(position).getUid()).get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            // 사진에 프로필
                            if(task.isSuccessful()) {
                                Object url = task.getResult().get("image");
                                Glide.with(holder.itemView.getContext()).load(url)
                                        .apply(new RequestOptions().circleCrop())
                                        .into(viewHolder.iBinding.commentviewitemImageviewProfile);
                            }
                        }
                    });

            viewHolder.iBinding.commentviewitemTextviewComment.setText(comments.get(position).getComment());
            viewHolder.iBinding.commentviewitemTextviewProfile.setText(comments.get(position).getUserId());


        }

        @Override
        public int getItemCount() {
            return comments.size();
        }

        private class CustomViewHolder extends RecyclerView.ViewHolder {
            private ItemCommentBinding iBinding;
            // 뷰홀더의 역할은 메모리 누수를 막아준다고 하는데? 정확히 뭐하는 앤지 공부 좀.

            public CustomViewHolder(ItemCommentBinding iBinding) {
                super(iBinding.getRoot());
                this.iBinding = iBinding;
            }
        }
    }
}