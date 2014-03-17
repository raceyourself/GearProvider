package com.raceyourself.android.samsung.models;

import org.json.JSONException;
import org.json.JSONObject;

import com.glassfitgames.glassfitplatform.gpstracker.GPSTracker;

public class GpsStatusResp extends SAModel {

    public final String GPS_STATUS_KEY = "gps-status";
    public final String GPS_ENABLED = "enabled";
    public final String GPS_DISABLED = "disabled";
    public final String GPS_READY = "ready";
    
    private String mGpsStatus = GPS_DISABLED;
    
    public GpsStatusResp(GPSTracker gpst) {
        if (gpst.hasPosition()) {
            mGpsStatus = GPS_READY;
        } else {
            mGpsStatus = GPS_ENABLED;
        }
    }
    
    public String getGpsStatus() {
        return mGpsStatus;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject json = super.toJSON();
        json.put("GPS_STATUS_KEY", mGpsStatus);
        return json;
    }

    public void fromJSON(JSONObject obj) throws JSONException {
       // this.mGpsStatus = obj.getString(GPS_STATUS_KEY);
    }

    @Override
    public String getMessageType() {
        return SAModel.GPS_STATUS_RESP;
    }

}