package com.nineclown.lbarsns.sns;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.nineclown.lbarsns.R;
import com.nineclown.lbarsns.camera.CameraFragment;
import com.nineclown.lbarsns.databinding.ActivityUploadBinding;
import com.nineclown.lbarsns.model.ContentDTO;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class UploadActivity extends AppCompatActivity {
    private ActivityUploadBinding binding;
    private Uri photoUri;
    private FirebaseStorage mStorage;
    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private Activity addPhotoActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_upload);

        // firebase 관련.
        mStorage = FirebaseStorage.getInstance();
        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        addPhotoActivity = this;

        binding.uploadIvPhoto.setOnClickListener(v -> {
            CropImage.activity()
                    .setGuidelines(CropImageView.Guidelines.ON_TOUCH)
                    .start(addPhotoActivity);
            });

        binding.addPhotoBtnUpload.setOnClickListener(v -> contentUpload());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // 사진 크롭해서 그 잘린 사진의 주소.
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if (resultCode == RESULT_OK) {
                // Uri에 그 주소를 담아.
                photoUri = result.getUri();
                // 네모칸에 사진을 올려.
                binding.uploadIvPhoto.setImageURI(photoUri);

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            } else {
                finish();
            }
        }

    }

    private void contentUpload() {
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "PNG_" + timeStamp + ".png";
        File m_File = new File(Environment.getExternalStorageDirectory(), "Pictures/Lbarsns");
        if (!m_File.exists()) {
            m_File.mkdirs();
        }

        File mFile = new File(m_File, timeStamp);


        final StorageReference mStorageRef = mStorage.getReference().child(imageFileName);

        UploadTask uploadTask = mStorageRef.putFile(photoUri);

        Task<Uri> urlTask = uploadTask.continueWithTask(task -> {
            if (!task.isSuccessful()) {
                // 실패하면 예외처리.
                throw Objects.requireNonNull(task.getException());
            }

            // url을 넘겨줘서 storage --> store로 전달하기 위한 전처리.
            return mStorageRef.getDownloadUrl();
        }).addOnSuccessListener(uri -> {
            Toast.makeText(UploadActivity.this, "등록 완료.", Toast.LENGTH_SHORT).show();
            if (uri != null) {
                // 업로드된 이미지 주소
                String photoStringLink = uri.toString(); //YOU WILL GET THE DOWNLOAD URL HERE !!!!

                ContentDTO contentDTO = new ContentDTO();

                // 이미지 주소
                contentDTO.setImageUrl(photoStringLink);

                // 유저의 UID
                contentDTO.setUid(mAuth.getCurrentUser().getUid());

                // 게시물 설명
                contentDTO.setExplain(binding.uploadEtExplain.getText().toString());

                // 유저 ID
                contentDTO.setUserId(mAuth.getCurrentUser().getEmail());

                // 게시물 업로드 시간
                contentDTO.setTimestamp(System.currentTimeMillis());

                // 게시물 댓글.
                mFirestore.collection("images").document().set(contentDTO);
                setResult(Activity.RESULT_OK);

                finish();
            }

        }).addOnFailureListener(e -> {

        });
    }
}
