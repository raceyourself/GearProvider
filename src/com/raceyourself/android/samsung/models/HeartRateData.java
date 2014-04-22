package com.raceyourself.android.samsung.models;

import org.json.JSONException;
import org.json.JSONObject;

import com.glassfitgames.glassfitplatform.gpstracker.GPSTracker;

public class HeartRateData extends SAModel {
    
    public final String HEART_RATE_BPM = "HEART_RATE_BPM";

    private int mHeartRate = 0;
    
    public HeartRateData(int heartRateBpm) {
        mHeartRate = heartRateBpm;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject json = super.toJSON();
        json.put(HEART_RATE_BPM, mHeartRate);
        return json;
    }

    @Override
    public String getMessageType() {
        return SAModel.HEART_RATE_DATA;
    }

}