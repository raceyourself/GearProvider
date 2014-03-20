package com.raceyourself.android.samsung.models;

import org.json.JSONException;
import org.json.JSONObject;

import android.location.LocationManager;

import com.glassfitgames.glassfitplatform.gpstracker.GPSTracker;

public class GpsStatusResp extends SAModel {

    public static final String GPS_STATUS_KEY = "gps-status";
    public static final String GPS_ENABLED = "enabled";
    public static final String GPS_DISABLED = "disabled";
    public static final String GPS_READY = "ready";
    
    private String mGpsStatus = GPS_DISABLED;
    
    public GpsStatusResp(String status) {
        if (status.equals(GPS_DISABLED) || status.equals(GPS_ENABLED) || status.equals(GPS_READY)) {
            mGpsStatus = status;
        } else {
            throw new RuntimeException("GpsStatusResp: invalid status passed to contructor, cannot continue");
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