package com.nineclown.lbarsns.sns;


import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.nineclown.lbarsns.R;
import com.nineclown.lbarsns.databinding.FragmentMapBinding;
import com.nineclown.lbarsns.model.TravelDTO;

import net.daum.mf.map.api.CameraUpdateFactory;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapPointBounds;
import net.daum.mf.map.api.MapPolyline;
import net.daum.mf.map.api.MapView;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 */
public class MapFragment extends Fragment {
    private FragmentMapBinding binding;
    private FirebaseFirestore mFirestore;
    private MapView mapView;
    private MainActivity mainActivity;
    private MapPolyline polyline;
    private ArrayList<TravelDTO.LatLon> latLons;
    private String travelName;

    public MapFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_map, container, false);


        mFirestore = FirebaseFirestore.getInstance();
        mainActivity = (MainActivity) getActivity();
        mapView = new MapView(mainActivity);
        polyline = new MapPolyline();
        latLons = new ArrayList<>();


        polyline.setTag(1000);
        polyline.setLineColor(Color.argb(128, 255, 51, 0)); // Polyline 컬러 지정.

        getTravel();
        return binding.getRoot();
    }

    private void drawMaps() {
        for (TravelDTO.LatLon data : latLons) {
            Double lat = data.getLatitude();
            Double lon = data.getLongitude();
            polyline.addPoint(MapPoint.mapPointWithGeoCoord(lat, lon));
        }

        String size = Integer.toString(latLons.size());
        Toast.makeText(mainActivity, "size: " + size, Toast.LENGTH_SHORT).show();
    }

    private void getTravel() {
        mFirestore.collection("travels").orderBy("timestamp").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                travelName = task.getResult().getDocuments().get(0).getId();
                getData();
            }
        });
    }

    private void getData() {
        // [START get_all_gps]
        mFirestore.collection("travels").document(travelName)
                .collection("locations")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            latLons.add(document.toObject(TravelDTO.LatLon.class));
                        }
                        Toast.makeText(mainActivity, "제발 좀: " + latLons.size(), Toast.LENGTH_SHORT).show();
                        drawMaps();
                        mapView.addPolyline(polyline);

                        // 지도뷰의 중심좌표와 줌레벨을 Polyline이 모두 나오도록 조정.
                        MapPointBounds mapPointBounds = new MapPointBounds(polyline.getMapPoints());
                        int padding = 100; // px
                        mapView.moveCamera(CameraUpdateFactory.newMapPointBounds(mapPointBounds, padding));

                        binding.mapView.addView(mapView);

                    } else {
                        Toast.makeText(mainActivity, "실패했어.", Toast.LENGTH_SHORT).show();
                    }
                });

        // [END get_all_gps]
    }
}
