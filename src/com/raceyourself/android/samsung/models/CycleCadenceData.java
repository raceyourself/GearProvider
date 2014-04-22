package com.raceyourself.android.samsung.models;

import org.json.JSONException;
import org.json.JSONObject;

import com.glassfitgames.glassfitplatform.gpstracker.GPSTracker;

public class CycleCadenceData extends SAModel {
    
    public final String CYCLE_CADENCE_RPM = "CYCLE_CADENCE_RPM";

    private float mCadence = 0.0f;
    
    public CycleCadenceData(float cadenceRpm) {
        mCadence = cadenceRpm; 
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject json = super.toJSON();
        json.put(CYCLE_CADENCE_RPM, mCadence);
        return json;
    }

    @Override
    public String getMessageType() {
        return SAModel.CYCLE_CADENCE_DATA;
    }

}