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
* File                : SASmartViewProviderImpl.java
*
* Author(s)           : Amit Singh
*
* Date                : 12 December 2013
*
* Description         : This file handles the backend  service of the  provider  and  extends the  SAAgent class
================================================================================
*                            Modification History
*-------------------------------------------------------------------------------
*    Date    |       Name      |        Modification
*-------------------------------------------------------------------------------
*            |                 |
==============================================================================*/

package com.raceyourself.android.samsung;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONException;

import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.glassfitgames.glassfitplatform.auth.AuthenticationActivity;
import com.glassfitgames.glassfitplatform.gpstracker.GPSTracker;
import com.glassfitgames.glassfitplatform.gpstracker.Helper;
import com.raceyourself.samsungprovider.R;
import com.raceyourself.android.samsung.models.GpsPositionData;
import com.raceyourself.android.samsung.models.GpsStatusResp;
import com.raceyourself.android.samsung.models.SAModel;
import com.roscopeco.ormdroid.ORMDroidApplication;
import com.samsung.android.sdk.accessory.SAAgent;
import com.samsung.android.sdk.accessory.SAPeerAgent;
import com.samsung.android.sdk.accessory.SASocket;

/**
 * @author s.amit
 *
 */
public class ProviderService extends SAAgent implements GPSTracker.PositionListener {
	
    public static final String TAG = "RaceYourselfProvider";
    public final int DEFAULT_CHANNEL_ID = 104;

	private final IBinder mBinder = new LocalBinder();
	private static HashMap<Integer, RaceYourselfSamsungProviderConnection> mConnectionsMap = null;
	private static GPSTracker gpsTracker = null;
	
	private boolean registered = false; // have we registered the device with the server yet? Required for inserting stuff into the db.

	/**
	 * @author s.amit
	 *
	 */
	public class LocalBinder extends Binder {
		public ProviderService getService() {
			return ProviderService.this;
		}
	}

	 /**
     * 
     * @param intent
     * @return IBinder
     */
	@Override
	public IBinder onBind(Intent intent) {
	    gpsTracker = new GPSTracker(this);
        gpsTracker.registerPositionListener(this);
		return mBinder;
	}
	
	@Override
    public boolean onUnbind(Intent intent) {
        gpsTracker.stopTracking();
        gpsTracker = null;
        return false;
    }
	
	

    /**
     * 
     */
	@Override
	public void onCreate() {
		super.onCreate();
		ORMDroidApplication.initialize(this);  // init the database
		Helper.getDevice();  // if this is the first launch, trigger a background thread to register device with our server
		Log.i(TAG, "onCreate of smart view Provider Service");
	}
	
    /**
     * 
     */
	@Override
	public void onLowMemory() {
		Log.e(TAG, "onLowMemory  has been hit better to do  graceful  exit now");
//		Toast.makeText(getBaseContext(), "!!!onLowMemory!!!", Toast.LENGTH_LONG)
//		.show();
		closeConnection();
		super.onLowMemory();
	}

    /**
     * 
     */
	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "Service Stopped.");

	}

    /**
     * 
     */
	public ProviderService() {
		super(TAG, RaceYourselfSamsungProviderConnection.class);
	}

    /**
     * @return boolean
     */
	public boolean closeConnection() {

		if (mConnectionsMap != null) {
			List<Integer> listConnections = new ArrayList<Integer>(
					mConnectionsMap.keySet());
			if (listConnections != null) {
				for (Integer s : listConnections) {
					Log.i(TAG, "KEYS found are" + s);
					mConnectionsMap.get(s).close();
					mConnectionsMap.remove(s);
				}
			}
		} else
			Log.e(TAG, "mConnectionsMap is null");

		return true;
	}

    /**
     * 
     * @param uThisConnection
     * @param result
     */
	@Override
	protected void onServiceConnectionResponse(SASocket uThisConnection,
			int result) {
		if (result == CONNECTION_SUCCESS) {
			if (uThisConnection != null) {
				RaceYourselfSamsungProviderConnection myConnection = (RaceYourselfSamsungProviderConnection) uThisConnection;
				if (mConnectionsMap == null) {
					mConnectionsMap = new HashMap<Integer, RaceYourselfSamsungProviderConnection>();
				}
				myConnection.mConnectionId = (int) (System.currentTimeMillis() & 255);
				Log.d(TAG,"onServiceConnection connectionID = "+myConnection.mConnectionId);
				mConnectionsMap.put(myConnection.mConnectionId, myConnection);
				// String toastString = R.string.ConnectionEstablishedMsg + ":"
				// + uThisConnection.getRemotePeerId();
				Toast.makeText(getBaseContext(),
						R.string.ConnectionEstablishedMsg, Toast.LENGTH_LONG)
						.show();
			} else
				Log.e(TAG, "SASocket object is null");
		} else
			Log.e(TAG, "onServiceConnectionResponse result error =" + result);
		
	}

    /**
     * 
     * @param connectedPeerId
     * @param channelId
     * @param data
     */
	private void onDataAvailableonChannel(String connectedPeerId,
			long channelId, String data) {

        Log.d(TAG, "Received message on channel " + channelId + " from peer " + connectedPeerId
                + ": " + data);
		
		SAModel response = null;
		
		if (gpsTracker == null) {
		    // TODO: Figure out lifetime of GPSTracker
		    gpsTracker = new GPSTracker(this);
		    gpsTracker.registerPositionListener(this);
        }
		
		if (!registered) {
		    //TODO: some useful user feedback, and way of breaking infinite loop on no network..
		    Log.e(TAG, "Registering device, please ensure you have internet connection..");
		    while (Helper.getDevice() == null) continue;
		    registered = true;
		}
		
		// decide what to do based on the message
		if (data.contains(SAModel.GPS_STATUS_REQ)) {
		    response = new GpsStatusResp(gpsTracker);
		} else if (data.contains(SAModel.START_TRACKING_REQ)) {
		    gpsTracker.startTracking();
		    //response = new SAModelImpl.Ack(SAModel.START_TRACKING_REQ);
		} else if (data.contains(SAModel.STOP_TRACKING_REQ)) {
            gpsTracker.stopTracking();
            //response = new SAModelImpl.Ack(SAModel.START_TRACKING_REQ);
		} else if (data.contains(SAModel.AUTHENTICATION_REQ)) {
		    Intent authenticationIntent = new Intent (this, AuthenticationActivity.class);
		    authenticationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		    authenticationIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		    startActivity(authenticationIntent);
		} else if (data.contains(SAModel.LOG_ANALYTICS)) {
		    Helper.logEvent(data);
		} else {
			Log.e(TAG, "onDataAvailableonChannel: Unknown request received");
		}
		
		// send the response
		if (response != null) {
		    send(connectedPeerId, response);
		}

	}
	
	public void newPosition() {
	    Log.e(TAG, "Sending new position over SAP");
	    SAModel gpsData = new GpsPositionData(gpsTracker);
	    // send to all connected peers
	    for (RaceYourselfSamsungProviderConnection c : mConnectionsMap.values()) {
	        send(String.valueOf(c.mConnectionId), gpsData);
	    }
    }
	
	private void send(String connectedPeerId, SAModel message) {
	    RaceYourselfSamsungProviderConnection conn = mConnectionsMap.get(Integer.parseInt(connectedPeerId));
        try {
            Log.d(TAG, "Sending message on channel " + DEFAULT_CHANNEL_ID + ": " + message.toJSON().toString());
            conn.send(DEFAULT_CHANNEL_ID, message.toJSON().toString().getBytes());
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
	}
	
	
    /**
     * 
     * @param peerAgent
     * @param result
     */
	@Override
	protected void onFindPeerAgentResponse(SAPeerAgent peerAgent, int result) {

		Log.i(TAG,
				"onPeerAgentAvailable: Use this info when you want provider to initiate peer id = "
						+ peerAgent.getPeerId());
		Log.i(TAG,
				"onPeerAgentAvailable: Use this info when you want provider to initiate peer name= "
						+ peerAgent.getAccessory().getName());
	}

    /**
     * 
     * @param error
     * @param errorCode
     */
	@Override
	protected void onError(String error, int errorCode) {
		// TODO Auto-generated method stub
		Log.e(TAG,"ERROR: " + errorCode + ": " + error);
	}
	
	// service connection inner class
	
	 /**
     * 
     * @author amit.s5
     *
     */
	public class RaceYourselfSamsungProviderConnection extends SASocket {

		public static final String TAG = "RYSamsungProviderConnection";
		private int mConnectionId;

	    /**
	     * 
	     */
		public RaceYourselfSamsungProviderConnection() {
			super(RaceYourselfSamsungProviderConnection.class.getName());
		}

	    /**
	     * 
	     * @param channelId
	     * @param data
	     * @return
	     */
		@Override
		public void onReceive(int channelId, byte[] data) {
			Log.i(TAG, "onReceive ENTER channel = " + channelId);
			String strToUpdateUI = new String(data);
			onDataAvailableonChannel(String.valueOf(mConnectionId), channelId, //getRemotePeerId()
					strToUpdateUI);
		}

		//@Override
//		public void onSpaceAvailable(int channelId) {
//			 Log.v(TAG, "onSpaceAvailable: " + channelId);
//		}

	    /**
	     * 
	     * @param channelId
	     * @param errorString
	     * @param error
	     */
		@Override
		public void onError(int channelId, String errorString, int error) {
			Log.e(TAG, "Connection is not alive ERROR: " + errorString + "  "
					+ error);
		}

	    /**
	     * 
	     * @param errorCode
	     */
		@Override
		public void onServiceConnectionLost(int errorCode) {

			Log.e(TAG, "onServiceConectionLost  for peer = "
					+ mConnectionId + "error code =" + errorCode);
			if (mConnectionsMap != null) {
				    mConnectionsMap.remove(mConnectionId);


		}

	}

  }
}
