package com.nineclown.lbarsns.camera;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.ux.ArFragment;

public class ExArFragment extends ArFragment {

    public void onUpdate(FrameTime frameTime) {
        //Frame frame = getArSceneView().getArFrame();
        //getArSceneView().getArFrame().getCamera().
        super.onUpdate(frameTime);
        Log.d("camera.poseX", ":" + getArSceneView().getArFrame().getCamera().getPose().getTranslation()[0]);
        Log.d("camera.poseY", ":" + getArSceneView().getArFrame().getCamera().getPose().getTranslation()[1]);
        Log.d("camera.poseZ", ":" + getArSceneView().getArFrame().getCamera().getPose().getTranslation()[2]);
        //getArSceneView().getSession().getConfig().setPlaneFindingMode(Config.PlaneFindingMode.DISABLED);
//        for(Plane plane : frame.getUpdatedTrackables(Plane.class))
//        {
//            if(plane.getTrackingState() == TrackingState.TRACKING)
//            {
//
//            }
//        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
//        Log.d("ARFragment.x:",":"+getArSceneView().getArFrame().getCamera().getPose().getTranslation()[0]);
//        Log.d("ARFragment.y:",":"+getArSceneView().getArFrame().getCamera().getPose().getTranslation()[1]);
//        Log.d("ARFragment.z:",":"+getArSceneView().getArFrame().getCamera().getPose().getTranslation()[2]);
//        Log.d("ARFragment.x:",":"+getArSceneView().getArFrame().getCamera().getPose().getRotationQuaternion()[0]);
//        Log.d("ARFragment.y:",":"+getArSceneView().getArFrame().getCamera().getPose().getRotationQuaternion()[1]);
//        Log.d("ARFragment.z:",":"+getArSceneView().getArFrame().getCamera().getPose().getRotationQuaternion()[2]);
//        Log.d("ARFragment.w:",":"+getArSceneView().getArFrame().getCamera().getPose().getRotationQuaternion()[2]);


    }

    @Override
    public void onResume() {
        super.onResume();
//        Log.d("ARFragment.x:",":"+getArSceneView().getArFrame().getCamera().getPose().getTranslation()[0]);
//        Log.d("ARFragment.y:",":"+getArSceneView().getArFrame().getCamera().getPose().getTranslation()[1]);
//        Log.d("ARFragment.z:",":"+getArSceneView().getArFrame().getCamera().getPose().getTranslation()[2]);
//        Log.d("ARFragment.x:",":"+getArSceneView().getArFrame().getCamera().getPose().getRotationQuaternion()[0]);
//        Log.d("ARFragment.y:",":"+getArSceneView().getArFrame().getCamera().getPose().getRotationQuaternion()[1]);
//        Log.d("ARFragment.z:",":"+getArSceneView().getArFrame().getCamera().getPose().getRotationQuaternion()[2]);
//        Log.d("ARFragment.w:",":"+getArSceneView().getArFrame().getCamera().getPose().getRotationQuaternion()[2]);
    }

    protected Config getSessionConfiguration(Session session) {
        //super.getSessionConfiguration(session);
        Config config = new Config(session);
        config.setPlaneFindingMode(Config.PlaneFindingMode.DISABLED);
        Log.d("getSession", ":");
        return config;
    }


}
