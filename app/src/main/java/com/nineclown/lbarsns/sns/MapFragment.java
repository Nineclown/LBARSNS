package com.nineclown.lbarsns.sns;


import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.nineclown.lbarsns.R;
import com.nineclown.lbarsns.databinding.FragmentMapBinding;

import net.daum.mf.map.api.CameraUpdateFactory;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapPointBounds;
import net.daum.mf.map.api.MapPolyline;
import net.daum.mf.map.api.MapView;

/**
 * A simple {@link Fragment} subclass.
 */
public class MapFragment extends Fragment {
    private FragmentMapBinding binding;
    private MapView mapView;
    private MainActivity mainActivity;

    public MapFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_map, container, false);

        mainActivity = (MainActivity) getActivity();

        mapView = new MapView(mainActivity);



        MapPolyline polyline = new MapPolyline();
        polyline.setTag(1000);
        polyline.setLineColor(Color.argb(128, 255, 51, 0)); // Polyline 컬러 지정.

        // Polyline 좌표 지정.
        //polyline.addPoint(MapPoint.mapPointWithGeoCoord(37.537291, 127.005531));
        polyline.addPoint(MapPoint.mapPointWithGeoCoord(37.537400, 127.004515));
        polyline.addPoint(MapPoint.mapPointWithGeoCoord(37.537390, 127.004817));
        polyline.addPoint(MapPoint.mapPointWithGeoCoord(37.537688, 127.005319));
        polyline.addPoint(MapPoint.mapPointWithGeoCoord(37.537887, 127.005525));
        polyline.addPoint(MapPoint.mapPointWithGeoCoord(37.537963, 127.005695));
        polyline.addPoint(MapPoint.mapPointWithGeoCoord(37.538126, 127.005772));
        polyline.addPoint(MapPoint.mapPointWithGeoCoord(37.538553, 127.005750));
        polyline.addPoint(MapPoint.mapPointWithGeoCoord(37.538313, 127.005775));
        polyline.addPoint(MapPoint.mapPointWithGeoCoord(37.538542, 127.005805));
        polyline.addPoint(MapPoint.mapPointWithGeoCoord(37.538485, 127.005831));
        polyline.addPoint(MapPoint.mapPointWithGeoCoord(37.539051, 127.005875));
        polyline.addPoint(MapPoint.mapPointWithGeoCoord(37.539123, 127.005853));
        polyline.addPoint(MapPoint.mapPointWithGeoCoord(37.539231, 127.005902));
        polyline.addPoint(MapPoint.mapPointWithGeoCoord(37.539322, 127.006118));

        // Polyline 지도에 올리기.
        mapView.addPolyline(polyline);

        // 지도뷰의 중심좌표와 줌레벨을 Polyline이 모두 나오도록 조정.
        MapPointBounds mapPointBounds = new MapPointBounds(polyline.getMapPoints());
        int padding = 100; // px
        mapView.moveCamera(CameraUpdateFactory.newMapPointBounds(mapPointBounds, padding));

        binding.mapView.addView(mapView);





        return binding.getRoot();
    }

}
