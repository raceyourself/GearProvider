package com.raceyourself.android.samsung.models;

import org.json.JSONException;
import org.json.JSONObject;

public class RemoteConfigurationResp extends SAModel {    
    private final JSONObject configuration;
    
    public RemoteConfigurationResp(String configuration) {
        try {
            this.configuration = new JSONObject(configuration);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
    
    public JSONObject toJSON() throws JSONException {
        JSONObject json = super.toJSON();
        json.put("CONFIGURATION", configuration);
        return json;
    }

    @Override
    public String getMessageType() {
        return SAModel.REMOTE_CONFIGURATION_RESP;
    }

}