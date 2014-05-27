package com.raceyourself.android.samsung.models;

import org.json.JSONException;
import org.json.JSONObject;

public class ShareScoreReq extends SAModel {
    
    private String mService = null;
    private Integer mScore = null;
    private Integer mHighscore = null;
    private String mSubtext = null;
    
    public ShareScoreReq(String service, int score, int highscore, String subtext) {
        this.mService = service;
        this.mScore = score;
        this.mHighscore = highscore;
        this.mSubtext = subtext;
    }
    
    public String getService() {
        return mService;
    }
    
    public Integer getScore() {
        return mScore;
    }
    
    public Integer getHighscore() {
        return mHighscore;
    }
    
    public String getSubtext() {
        return mSubtext;
    }
    
    public JSONObject toJSON() throws JSONException {
        JSONObject json = super.toJSON();
        json.put("service", mService);
        json.put("score", mScore);
        json.put("highscore", mHighscore);
        json.put("subtext", mSubtext);
        return json;
    }

    public static ShareScoreReq fromJSON(JSONObject obj) throws JSONException {
        
        return new ShareScoreReq(obj.getString("service"), obj.getInt("score"), obj.getInt("highscore"), obj.getString("subtext"));
    }

    @Override
    public String getMessageType() {
        return SAModel.SHARE_SCORE_REQ;
    }

}