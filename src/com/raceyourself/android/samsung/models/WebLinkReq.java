package com.raceyourself.android.samsung.models;

import org.json.JSONException;
import org.json.JSONObject;

import android.location.LocationManager;

import com.glassfitgames.glassfitplatform.gpstracker.GPSTracker;

public class WebLinkReq extends SAModel {
    
    private String mUri = null;
    
    public WebLinkReq(String uri) {
        this.mUri = uri;
    }
    
    public String getUri() {
        return mUri;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject json = super.toJSON();
        json.put("URI", mUri);
        return json;
    }

    public static WebLinkReq fromJSON(JSONObject obj) throws JSONException {
        
        return new WebLinkReq(obj.getString("URI"));
    }

    @Override
    public String getMessageType() {
        return SAModel.WEB_LINK_REQ;
    }

}