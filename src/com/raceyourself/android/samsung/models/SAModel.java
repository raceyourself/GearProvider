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
package com.raceyourself.android.samsung.models;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class SAModel {

    public static final String MESSAGE_TYPE = "messageType";
	public static final String GPS_STATUS_REQ = "gps-status-req";
	public static final String GPS_STATUS_RESP = "gps-status-resp";
	public static final String GPS_POSITION_DATA = "gps-position-data";
	public static final String START_TRACKING_REQ = "start_tracking-req";
	public static final String STOP_TRACKING_REQ = "stop-tracking-req";
	public static final String AUTHENTICATION_REQ = "authentication-req";
	public static final String LOG_ANALYTICS = "log-analytics";
	public static final String LOG_TO_ADB = "log-to-adb";
	public static final String WEB_LINK_REQ = "web-link-req";
	public static final String REMOTE_CONFIGURATION_REQ = "remote-configuration-req";
    public static final String REMOTE_CONFIGURATION_RESP = "remote-configuration-resp";
    public static final String SHARE_SCORE_REQ = "share-score-req";
	
	public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put(MESSAGE_TYPE, getMessageType());
        return json;
    }
    
    public abstract String getMessageType();

}
