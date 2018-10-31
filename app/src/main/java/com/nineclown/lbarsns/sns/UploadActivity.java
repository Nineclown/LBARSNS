package com.nineclown.lbarsns.sns;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.media.ExifInterface;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.nineclown.lbarsns.R;
import com.nineclown.lbarsns.databinding.ActivityUploadBinding;
import com.nineclown.lbarsns.model.ContentDTO;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class UploadActivity extends AppCompatActivity {
    private final int PICK_IMAGE_FROM_ALBUM = 10;
    private ActivityUploadBinding binding;
    private Uri photoUri;
    private Double Latitude;
    private Double Longitude;
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

        Latitude = null;
        Longitude = null;


        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, PICK_IMAGE_FROM_ALBUM);


        binding.uploadIvPhoto.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE_FROM_ALBUM);
        });

        binding.addPhotoBtnUpload.setOnClickListener(v -> contentUpload());
    }

    private void extractGPS(String imageUri) {
        try {
            ExifInterface mExifInterface = new ExifInterface(imageUri);
            String latlon = mExifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
            if (latlon != null) {
                Latitude = convertToDegree(latlon);
                Longitude = convertToDegree(mExifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setGPSInfo() {
        // 사진이 GPS 정보가 없는 경우,
        if (Latitude == null || Longitude == null) {
            binding.uploadTvLat.setText("");
            binding.uploadTvLon.setText("");
            binding.uploadIvGps.setImageResource(R.drawable.gps_off);
        }
        // GPS 정보가 있는 경우,
        else {
            String lat = Latitude.toString();
            binding.uploadTvLat.setText(lat);
            String lon = Longitude.toString();
            binding.uploadTvLon.setText(lon);
            binding.uploadIvGps.setImageResource(R.drawable.gps_on);
        }

    }

    private Double convertToDegree(String loglat) {
        Double result = null;
        String[] DMS = loglat.split(",", 3);

        String[] stringD = DMS[0].split("/", 2);
        Double D0 = Double.valueOf(stringD[0]);
        Double D1 = Double.valueOf(stringD[1]);
        Double FloatD = D0 / D1;

        String[] stringM = DMS[1].split("/", 2);
        Double M0 = Double.valueOf(stringM[0]);
        Double M1 = Double.valueOf(stringM[1]);
        Double FloatM = M0 / M1;

        String[] stringS = DMS[2].split("/", 2);
        Double S0 = Double.valueOf(stringS[0]);
        Double S1 = Double.valueOf(stringS[1]);
        Double FloatS = S0 / S1;

        result = FloatD + (FloatM / (double) 60) + (FloatS / (double) 3600);

        return result;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_FROM_ALBUM) {
            //이미지 선택시
            if (resultCode == Activity.RESULT_OK) {
                // 사용할 이미지를 photoUri에 등록.
                photoUri = data.getData();

                // 사진을 선택할 때마다 초기화 해줘야 한다.
                Longitude = null;
                Latitude = null;
                // 절대경로로 변환한다.
                String uri = ConvertUri.getPath(this, photoUri);

                // 사용할 이미지가 가진 exif 정보를 빼낸다.
                extractGPS(uri);


                if (photoUri != null) {
                    // 사용할 이미지를 크롭한다.
                    CropImage.activity(photoUri)
                            .setGuidelines(CropImageView.Guidelines.ON_TOUCH)
                            .start(addPhotoActivity);
                }
            }
        }

        // 사진 크롭해서 그 잘린 사진의 주소.
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if (resultCode == RESULT_OK) {
                // Uri에 그 주소를 담아.
                photoUri = result.getUri();
                // 네모칸에 사진을 올려.
                binding.uploadIvPhoto.setImageURI(photoUri);
                setGPSInfo();
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            } else {
                //finish();
            }
        }

    }

    private void contentUpload() {
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "PNG_" + timeStamp + ".png";

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

                // GPS 정보
                contentDTO.setLatitude(Latitude);
                contentDTO.setLongitude(Longitude);
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
