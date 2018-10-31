package com.nineclown.lbarsns.sns;

import android.support.annotation.NonNull;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.nineclown.lbarsns.model.FcmDTO;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FcmPush {
    private MediaType JSON;
    private String url;
    private String serverKey;
    private OkHttpClient okHttpClient;
    private Gson gson;


    private FcmPush() {
        JSON = MediaType.parse("application/json; charset=utf-8");
        url = "https://fcm.googleapis.com/fcm/send";
        serverKey = "AAAA7KIfafc:APA91bFFzhKl7sYDy2NMvzBykb97kRgsD06hoq7ffL4Lz-fG3yFZUbYs9a5dcsLsxXfW0c_1F2njIHW3e0_Vfjnjf5-r7tmrKOKG7t5qjF6K98TNG0OZrG5y-nHO84m9dzmZ8ypCvCvH";
        gson = new Gson();
        okHttpClient = new OkHttpClient();
    }

    public void sendMessage(String destinationUid, final String title, final String message) {
        // get().addOncom~~ 이 방식이랑 addShapshot~~ 이 방식의 차이는 전자는 한번만, 후자는 DB가 바뀔 때마다 알아서 처리한다.
        // DB에 pushTokens 라는 컬렉션에 uid 라는 문서이름을 가진 파일에 접근한다.
        FirebaseFirestore.getInstance().collection("pushTokens").document(destinationUid).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // 접근에 성공하면, 해당 문서에서 pushToken이라는 키를 갖는 값을 token 에 담는다.
                String token = task.getResult().get("pushToken").toString();
                FcmDTO fcmDTO = new FcmDTO();

                fcmDTO.setTo(token);

                fcmDTO.setNotification(title, message);

                String test = gson.toJson(fcmDTO);

                //System.out.println("@@@@@@@@@@@@@@@@@@@@@" + test);
                RequestBody body = RequestBody.create(JSON, gson.toJson(fcmDTO));
                Request request = new Request.Builder().addHeader("Content-Type", "application/json")
                        .addHeader("Authorization", "key=" + serverKey)
                        .url(url)
                        .post(body)
                        .build();
                okHttpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {

                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) {
                        //System.out.println(response.body().string());
                    }
                });
            }
        });
    }

    private static class LazyHolder {
        static final FcmPush INSTANCE = new FcmPush();
    }

    public static FcmPush getInstance() {
        return LazyHolder.INSTANCE;
    }

}
