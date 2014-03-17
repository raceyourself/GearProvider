/**============================== FILE HEADER ==================================
*
*           Copyright (c) 2013 Samsung R&D Institute India- Bangalore Pvt. Ltd (SRI-B)
*                      Samsung Confidential Proprietary
*                            All Rights Reserved
*          This software is the confidential and proprietary information 
*          of Samsung R&D Institute India- Bangalore Pvt. Ltd (SRI-B).
*
*         You shall not disclose such Confidential Information and shall use
*		  it only in accordance with the terms of the license agreement you entered 
*		  into with Samsung R&D Institute India- Bangalore Pvt. Ltd (SRI-B).
================================================================================
*                               Module Name :Gallery Provider B App
================================================================================
* File                : AlarmjSonDataModel.java
*
* Author(s)           : Amit Singh
*
* Date                : 12 December 2013
*
* Description         : This file holds the Alarm app  json data models
================================================================================
*                            Modification History
*-------------------------------------------------------------------------------
*    Date    |       Name      |        Modification
*-------------------------------------------------------------------------------
*            |                 |
==============================================================================*/

package com.raceyourself.android.samsung;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.glassfitgames.glassfitplatform.gpstracker.GPSTracker;

public class SAModelImpl {
    
	public class GpsStatusResp extends SAModel {

	    public final String GPS_ENABLED = "GPS_ENABLED";
	    public final String GPS_DISABLED = "GPS_DISABLED";
	    public final String GPS_READY = "GPS_READY";
	    
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
			json.put(GPS_STATUS_RESP, mGpsStatus);
			return json;
		}

		public void fromJSON(JSONObject obj) throws JSONException {
		    super.fromJSON(obj);
		    this.mGpsStatus = obj.getString(GPS_STATUS_RESP);
		}

        @Override
        public String getMessageType() {
            return SAModel.GPS_STATUS_RESP;
        }

	}

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
            super.fromJSON(obj);
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

}
