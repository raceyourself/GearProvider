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
* File                : Model.java
*
* Author(s)           : Amit Singh
*
* Date                : 12 December 2013
*
* Description         : This file holds the model  class  with  json keys and local structures
================================================================================
*                            Modification History
*-------------------------------------------------------------------------------
*    Date    |       Name      |        Modification
*-------------------------------------------------------------------------------
*            |                 |
==============================================================================*/
package com.raceyourself.android.samsung;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class SAModel {

	public static final String GPS_STATUS_REQ = "gallery-thumbnail-req";
	public static final String GPS_STATUS_RESP = "gallery-thumbnail-rsp";
	public static final String START_TRACKING_REQ = "gallery-image-req";
	public static final String GPS_POSITION_DATA = "gallery-image-req";
	public static final String STOP_TRACKING_REQ = "gallery-image-rsp";
	
	public static final String MSG_ID = "msgId";
	public static final String COUNT = "count";
    public static final String LIST = "list";
    public static final String REASON = "reason";
    public static final String RESULT = "result";
	
    String mMessgaeId = "";
    String mResult = "";
    int mReason = 0;
    int mCount = 0;
	
	public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put(SAModel.MSG_ID, mMessgaeId);
        json.put(RESULT, mResult);
        json.put(REASON, mReason);
        json.put(COUNT, mCount);
        return json;
    }

    public void fromJSON(JSONObject json) throws JSONException {
        mMessgaeId = json.getString(SAModel.MSG_ID);
        mResult = json.getString(RESULT);
        mReason = json.getInt(REASON);
        mCount = json.getInt(COUNT);
    }

    public String getMessageIdentifier() {
        return mMessgaeId;
    }

    public int getMsgCount() {
        return mCount;
    }

    public String getResult() {
        return mResult;
    }

    public int getReason() {
        return mReason;
    }
    
    public abstract String getMessageType();

}
