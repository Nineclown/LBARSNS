package com.nineclown.lbarsns.camera;

import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.sceneform.ux.ArFragment;

public class ExArFragment extends ArFragment {

    protected Config getSessionConfiguration(Session session) {
        //super.getSessionConfiguration(session);
        Config config = new Config(session);
        config.setPlaneFindingMode(Config.PlaneFindingMode.DISABLED);
        return config;
    }


}
