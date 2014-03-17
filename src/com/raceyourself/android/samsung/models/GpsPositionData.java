package com.raceyourself.android.samsung.models;

import org.json.JSONException;
import org.json.JSONObject;

import com.glassfitgames.glassfitplatform.gpstracker.GPSTracker;

public class GpsPositionData extends SAModel {
    
    public final String GPS_DISTANCE = "GPS_DISTANCE";
    public final String GPS_TIME = "GPS_TIME";
    public final String GPS_SPEED = "GPS_SPEED";
    public final String GPS_STATE = "GPS_STATE";

    private double mDistance = 0;
    private long mTime = 0L;
    private float mSpeed = 0f;
    private String mState = "STATE_STOPPED";
    
    public GpsPositionData(GPSTracker gpst) {
        if (gpst.hasPosition()) {
            mDistance = gpst.getElapsedDistance();
            mTime = gpst.getElapsedTime();
            mSpeed = gpst.getCurrentSpeed();
            mState = gpst.getState().name();
        }
    }
    
    public double getDistance() {
        return mDistance;
    }
    
    public long getTime() {
        return mTime;
    }
    
    public float getSpeed() {
        return mSpeed;
    }
    
    public String getState() {
        return mState;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject json = super.toJSON();
        json.put(GPS_DISTANCE, mDistance);
        json.put(GPS_TIME, mTime);
        json.put(GPS_SPEED, mSpeed);
        json.put(GPS_STATE, mState);
        return json;
    }

    public void fromJSON(JSONObject obj) throws JSONException {
        this.mDistance = obj.getDouble(GPS_DISTANCE);
        this.mTime = obj.getLong(GPS_TIME);
        this.mSpeed = (float)obj.getDouble(GPS_SPEED);
        this.mState = obj.getString(GPS_STATE);
    }

    @Override
    public String getMessageType() {
        return SAModel.GPS_POSITION_DATA;
    }

}