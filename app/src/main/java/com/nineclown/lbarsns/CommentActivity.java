package com.nineclown.lbarsns;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.nineclown.lbarsns.databinding.ActivityCommentBinding;
import com.nineclown.lbarsns.databinding.ItemCommentBinding;
import com.nineclown.lbarsns.model.ContentDTO;

public class CommentActivity extends AppCompatActivity {
    private ActivityCommentBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private String mUid;
    private String contentUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_comment);

        // firebase
        mFirestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        mUid = mAuth.getCurrentUser().getUid();

        // fragment는 argument, activity는 intent를 통해 데이터를 받아온다.
        contentUid = getIntent().getStringExtra("contentUid");
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
            }
        });
    }

    private class CommentRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private ItemCommentBinding iBinding;

        // xml 파일을 우선 불러와야 한다.

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            // 뷰를 설정하는 곳. 가져오는 곳.
            iBinding = ItemCommentBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new CustomViewHolder(iBinding);

        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        }

        @Override
        public int getItemCount() {
            return 3;
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
