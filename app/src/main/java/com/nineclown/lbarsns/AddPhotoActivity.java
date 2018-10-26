package com.nineclown.lbarsns;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.nineclown.lbarsns.databinding.ActivityAddPhotoBinding;
import com.nineclown.lbarsns.model.ContentDTO;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class AddPhotoActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 0;
    private static final int PICK_IMAGE_FROM_ALBUM = 1;
    private ActivityAddPhotoBinding binding;
    private Uri photoUri;
    private FirebaseStorage mStorage;
    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private Activity addPhotoActivity;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_add_photo);

        // firebase 관련.
        mStorage = FirebaseStorage.getInstance();
        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        addPhotoActivity = this;


        /*Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, PICK_IMAGE_FROM_ALBUM);*/

        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON_TOUCH)
                .start(this);

        binding.addPhotoImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON_TOUCH)
                        .start(addPhotoActivity);

                /*Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, PICK_IMAGE_FROM_ALBUM);*/
            }
        });

        binding.addPhotoBtnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                contentUpload();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                photoUri = result.getUri();
                binding.addPhotoImage.setImageURI(photoUri);
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            } else {
                finish();
            }
        }

    }

    private void contentUpload() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "PNG_" + timeStamp + ".png";

        final StorageReference mStorageRef = mStorage.getReference().child(imageFileName);

        UploadTask uploadTask = mStorageRef.putFile(photoUri);

        Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                if (!task.isSuccessful()) {
                    // 실패하면 예외처리.
                    throw Objects.requireNonNull(task.getException());
                }

                // url을 넘겨줘서 storage --> store로 전달하기 위한 전처리.
                return mStorageRef.getDownloadUrl();
            }
        }).addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Toast.makeText(AddPhotoActivity.this, "등록 완료.", Toast.LENGTH_SHORT).show();
                if (uri != null) {
                    // 업로드된 이미지 주소
                    String photoStringLink = uri.toString(); //YOU WILL GET THE DOWNLOAD URL HERE !!!!

                    ContentDTO contentDTO = new ContentDTO();

                    // 이미지 주소
                    contentDTO.setImageUrl(photoStringLink);

                    // 유저의 UID
                    contentDTO.setUid(mAuth.getCurrentUser().getUid());

                    // 게시물 설명
                    contentDTO.setExplain(binding.addPhotoEditExplain.getText().toString());

                    // 유저 ID
                    contentDTO.setUserId(mAuth.getCurrentUser().getEmail());

                    // 게시물 업로드 시간
                    contentDTO.setTimestamp(System.currentTimeMillis());

                    // 게시물 댓글.
                    mFirestore.collection("images").document().set(contentDTO);
                    setResult(Activity.RESULT_OK);

                    finish();
                }

            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });/*.addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful()) {
                    Uri downloadUri = task.getResult();
                    System.out.println("Upload " + downloadUri);
                    Toast.makeText(AddPhotoActivity.this, "Successfully uploaded", Toast.LENGTH_SHORT).show();
                    if (downloadUri != null) {

                        String photoStringLink = downloadUri.toString(); //YOU WILL GET THE DOWNLOAD URL HERE !!!!
                        System.out.println("Upload " + photoStringLink);

                    }

                } else {
                    // Handle failures
                    // ...
                }
            }
        });*/
    }
}
