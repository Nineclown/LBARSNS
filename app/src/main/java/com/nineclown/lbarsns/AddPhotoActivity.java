package com.nineclown.lbarsns;

import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class AddPhotoActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_FROM_ALBUM = 0;
    private ActivityAddPhotoBinding binding;
    private Uri photoUri;
    private FirebaseStorage mStorage;
    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_add_photo);

        // 싱글톤 패턴.
        mStorage = FirebaseStorage.getInstance();
        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();

        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, PICK_IMAGE_FROM_ALBUM);

        binding.addPhotoImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, PICK_IMAGE_FROM_ALBUM);
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
        if (requestCode == PICK_IMAGE_FROM_ALBUM) {
            if (resultCode == Activity.RESULT_OK) {
                //사진 선택 할때
                photoUri = data.getData();
                binding.addPhotoImage.setImageURI(data.getData());
            } else if (resultCode == Activity.RESULT_CANCELED) {
                //뒤로가기 누를때
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
                    throw Objects.requireNonNull(task.getException());
                }

                return mStorageRef.getDownloadUrl();
            }
        }).addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Toast.makeText(AddPhotoActivity.this, "Successfully uploaded", Toast.LENGTH_SHORT).show();
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
