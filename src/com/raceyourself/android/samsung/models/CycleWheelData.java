package com.raceyourself.android.samsung.models;

import org.json.JSONException;
import org.json.JSONObject;

import com.glassfitgames.glassfitplatform.gpstracker.GPSTracker;

public class CycleWheelData extends SAModel {
    
    public final String CYCLE_SPEED_RPM = "CYCLE_SPEED_RPM";

    private float mSpeedRpm = 0.0f;
    
    public CycleWheelData(float speedRpm) {
        mSpeedRpm = speedRpm;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject json = super.toJSON();
        json.put(CYCLE_SPEED_RPM, mSpeedRpm);
        return json;
    }

    @Override
    public String getMessageType() {
        return SAModel.CYCLE_SPEED_DATA;
    }

}