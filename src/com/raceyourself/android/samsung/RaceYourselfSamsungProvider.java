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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.raceyourself.samsungprovider.R;
import com.samsung.android.sdk.accessory.SAAgent;
import com.samsung.android.sdk.accessory.SAPeerAgent;
import com.samsung.android.sdk.accessory.SASocket;

/**
 * @author s.amit
 *
 */
public class RaceYourselfSamsungProvider extends SAAgent {
	
    public static final String TAG = "SmartViewProviderService";

	private final IBinder mBinder = new LocalBinder();

	/**
	 * @author s.amit
	 *
	 */
	public class LocalBinder extends Binder {
		public RaceYourselfSamsungProvider getService() {
			return RaceYourselfSamsungProvider.this;
		}
	}

	 /**
     * 
     * @param intent
     * @return IBinder
     */
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

    /**
     * 
     */
	@Override
	public void onCreate() {
		super.onCreate();
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
	public RaceYourselfSamsungProvider() {
		super(TAG, SAGalleryProviderConnection.class);
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
				SAGalleryProviderConnection myConnection = (SAGalleryProviderConnection) uThisConnection;
				if (mConnectionsMap == null) {
					mConnectionsMap = new HashMap<Integer, SAGalleryProviderConnection>();
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

		Log.i(TAG, "incoming data on channel = " + channelId + ": from peer ="
				+ connectedPeerId);
		if (data.contains(Model.THUMBNAIL_LIST_RQST)) {

			sendThumbnails(connectedPeerId, data);
		} else if (data.contains(Model.DOWNSCALE_IMG_RQST)) {
			sendDownscaledImage(connectedPeerId, data);
		} else {
			Log.e(TAG, "onDataAvailableonChannel: Unknown jSon PDU received");
		}

	}
	
    /**
     * 
     * @param imageCursor
     */
	private void publishMediaStoreInfo(Cursor imageCursor){
		
		Log.d(TAG,"publishMediaStoreInfo: Enter");
		for (int j = 0; j < imageCursor.getCount(); j++) {
			Log.d(TAG, "images no " + j + " id   = " + imageCursor.getInt(0)
					+ "name = " + imageCursor.getColumnName(0));
			imageCursor.moveToNext();
		}
		imageCursor.moveToFirst();
		Log.d(TAG,"publishMediaStoreInfo: Exit");
	}
	
    /**
     * 
     * @param imageCursor
     * @return boolean
     */
	private boolean pullThumbnails(Cursor imageCursor){
		Log.d(TAG, "pullThumbnails: Enter");
		String data = "";
		long img_id = imageCursor.getLong(imageCursor
				.getColumnIndex(MediaStore.Images.Media._ID));
		Log.d(TAG, "image id = " + img_id);
		Bitmap bm = MediaStore.Images.Thumbnails.getThumbnail(
				getApplicationContext().getContentResolver(), img_id,
				MediaStore.Images.Thumbnails.MICRO_KIND, null);
		if(bm == null){
			Log.e(TAG,"Failed to get bitmap thumbnail id: "+img_id);
			return false;
		}
		Log.d(TAG, "Bitmap bm size = " + bm.getByteCount());
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		bm.compress(Bitmap.CompressFormat.JPEG, 80, stream);
		Log.d(TAG, "compressed stream size  = " + stream.size());
		if (stream.toByteArray() != null) {
			data = Base64.encodeToString(stream.toByteArray(),
					Base64.NO_WRAP);
			try {
				stream.close();
			} catch (IOException e) {

				Log.e(TAG,
						"sendThumbnails() Cannot close byte array stream");
				e.printStackTrace();
			}
		}
		Log.d(TAG, "image data base 64 length = " + data.length());

		long img_size = imageCursor.getLong(imageCursor
				.getColumnIndex(MediaStore.Images.Media.SIZE));
		Log.d(TAG, "img_size  = " + img_size);
		String name = imageCursor.getString(imageCursor
				.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME));
		Log.d(TAG, "name  = " + name);
		int width = imageCursor.getInt(imageCursor
				.getColumnIndex(MediaStore.Images.Media.WIDTH));
		Log.d(TAG, "width  = " + width);
		int height = imageCursor.getInt(imageCursor
				.getColumnIndex(MediaStore.Images.Media.HEIGHT));
		Log.d(TAG, "height  = " + height);

		TBModelJson msg = new TBModelJson(img_id, name, data, img_size,
				width, height);
		mTb.add(msg);
		return true;
		
	}

    /**
     * 
     * @param connectedPeerId
     * @param request
     */
	private void sendThumbnails(String connectedPeerId, String request) {
		Log.d(TAG,"sendThumbnails: Enter");
		boolean ret = true;
		mResult = "failure";
		mReason = REASON_IMAGE_ID_INVALID;
		int count = 0;
		if (!mTb.isEmpty())
			mTb.clear();
		JSONObject obj = null;
		try {
			obj = new JSONObject(request);
		} catch (JSONException e) {
			e.printStackTrace();
			Log.e(TAG, "sendThumbnails() Cannot Convert to Json");
			return;
		}
		TBListReqMsg uRequest = new TBListReqMsg();
		try {
			uRequest.fromJSON(obj);
		} catch (JSONException e) {

			Log.e(TAG, "sendThumbnails() Cannot Convert from Json");
			e.printStackTrace();
			return;
		}
		long id = uRequest.getID();
		Log.d(TAG, " json msg identifier " + uRequest.getMessageIdentifier());
		Log.d(TAG, " json message_request parameters " + id);

		Cursor imageCursor = getContentResolver().query(
				MediaStore.Images.Media.EXTERNAL_CONTENT_URI, mProjection,
				null, null, null);
		if(imageCursor == null){
			Log.e(TAG,"FAILED  to obtain cursor for Media DB");
			mReason = REASON_DATABASE_ERROR;
			sendTbListMsg(connectedPeerId);
			return;
		}
		Log.d(TAG, "record count in media store DB =" + imageCursor.getCount());
		Log.d(TAG, "img cursor b4 first entry= " + imageCursor.isBeforeFirst());
		imageCursor.moveToFirst();
		publishMediaStoreInfo(imageCursor);

		if (id != INITIAL_IMAGE_INDEX) {
			Log.d(TAG, "consumer  requested  thumbnails after image id  = "
					+ id);
			for (int i = 0; i < imageCursor.getCount(); i++) {
				if (id == imageCursor.getInt(imageCursor
						.getColumnIndex(MediaStore.Images.Media._ID))) {
					Log.d(TAG, "requested  ID is   after count  = " + i);
					ret = imageCursor.moveToNext();
					break;
				}
				if (imageCursor.moveToNext() == false) {
					Log.d(TAG, "not found in media store DB id =" + id);
					ret = false;
					break;
				}
			}
		}
		Log.d(TAG, "Fetch Thumbnails cursor  positioned  is  fixed");
		int size = imageCursor.getCount();
		if (size > 0 && ret == true) {
			Log.d(TAG,
					"1 IMG ID = "
							+ imageCursor.getInt(imageCursor
									.getColumnIndex(MediaStore.Images.Media._ID)));
		}
		Log.d(TAG, "cusor at last = " + imageCursor.isAfterLast());
		if ((ret == true) && (size > 0)) {
			Log.d(TAG, "there is  image atleast one image after ID");
			do {
				boolean status = pullThumbnails(imageCursor);
				if(status == true)
					count++;

			} while (count < 3 && imageCursor.moveToNext());
			mResult = "success";
			mReason = REASON_OK;
		} // check to ignore in case id is last record in DB
		else {
			Log.d(TAG, "the  last record is hit ");
			mReason = REASON_EOF_IMAGE;
		}
		if (!imageCursor.isClosed()) {
			imageCursor.close();
			imageCursor = null;
		}
		sendTbListMsg(connectedPeerId);
	}

    /**
     * 
     * @param connectedPeerId
     */
	private void sendTbListMsg(String connectedPeerId){
	
		Log.d(TAG,"sendTbListMsg : Enter");
		TBListRespMsg uRMessage = new TBListRespMsg(mResult, mReason,
				mTb.size(), mTb);
		String uJsonStringToSend = "";
		try {
			uJsonStringToSend = uRMessage.toJSON().toString();
		} catch (JSONException e) {

			Log.e(TAG, "sendThumbnails() Cannot convert json to string");
			e.printStackTrace();
		}
		Log.d(TAG, "tb rsp msg size = " + uJsonStringToSend.length());
		if (mConnectionsMap != null) {
			SAGalleryProviderConnection uHandler = mConnectionsMap
					.get(Integer.parseInt(connectedPeerId));

			try {
				uHandler.send(GALLERY_CHANNEL_ID, uJsonStringToSend.getBytes());
			} catch (IOException e) {
				Log.e(TAG,"I/O Error occured while send");
				e.printStackTrace();
			}
		}
		
	}
	
    /**
     * 
     * @param path
     * @param width
     * @param height
     */
	private void pullDownscaledImg(String path, int width, int height){
		Log.d(TAG,"pullDownscaledImg :Enter");
		
		// micro thumbnails
		/*
		 * Bitmap bm =
		 * MediaStore.Images.Thumbnails.getThumbnail(getApplicationContext
		 * ().getContentResolver(), id,
		 * MediaStore.Images.Thumbnails.MINI_KIND, null);
		 * Log.d(TAG,"MINI  THUMBANIL  size bitmap  from  factory size is = "
		 * +bm.getByteCount()); ByteArrayOutputStream streams = new
		 * ByteArrayOutputStream(); bm.compress(Bitmap.CompressFormat.JPEG,
		 * 80, streams);
		 * Log.d(TAG,"MINI  THUMBANIL compressed JPEG stream size  = "
		 * +streams.size()); String datas = ""; if (streams.toByteArray() !=
		 * null) { datas = Base64.encodeToString( streams.toByteArray(),
		 * Base64.NO_WRAP); try { streams.close(); } catch (IOException e) {
		 * 
		 * e.printStackTrace(); } }
		 * Log.d(TAG,"MINI  THUMBANIL BASE 64 encoded size length = "
		 * +datas.length());
		 */
		// micro thumbnails

		BitmapFactory.Options opt = new BitmapFactory.Options();
		opt.inScaled = false;
		opt.inSampleSize = 4; // logic based on original and requested size.
		Bitmap scaledbitmap = Bitmap.createScaledBitmap(
				BitmapFactory.decodeFile(path, opt), width, height, false);
		if (scaledbitmap != null) {
			Log.d(TAG, "scaled  bitmap  from  factory size is = "
					+ scaledbitmap.getByteCount());
			Log.d(TAG, "Bitmap bm size = " + scaledbitmap.getByteCount());
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			scaledbitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream);
			Log.d(TAG, "compressed JPEG stream size  = " + stream.size());
			if (stream.toByteArray() != null) {
				mImgData = Base64.encodeToString(stream.toByteArray(),
						Base64.NO_WRAP);
				try {
					stream.close();
				} catch (IOException e) {
					Log.e(TAG, "sendDownscaledImage() cannot  close stream");
					e.printStackTrace();
				}
			}
			Log.d(TAG, " BASE 64 encoded size length = " + mImgData.length());
			mResult = "success"; // success
			mReason = REASON_OK; // ok
		} else {
			Log.d(TAG, "scaled bitmap  has  failed from paths :(");
			mResult = "failure";// result failure
			mReason = REASON_BITMAP_ENCODING_FAILURE; // rendering failed
		}
		
	}
	
    /**
     * 
     * @param connectedPeerId
     * @param request
     */
	private void sendDownscaledImage(String connectedPeerId, String request) {
		// put a upper cap like say 320x240 for image
		Log.d(TAG, "sendDownscaledImage enter");
		mImgData = "";
		mResult = "failure";
		mReason = REASON_IMAGE_ID_INVALID;
		int orgWidth = 0, orgHeight = 0;
		long orgSize = 0;
		String orgName = null;

		JSONObject obj = null;
		try {
			obj = new JSONObject(request);
		} catch (JSONException e) {
			e.printStackTrace();
			Log.e(TAG, "sendDownscaledImage() Cannot Convert to Json");
			return;
		}
		
		ImgReqMsg uMessage = new ImgReqMsg();
		try {
			uMessage.fromJSON(obj);
		} catch (JSONException e) {
			Log.e(TAG, "sendDownscaledImage() Cannot Convert from Json");
			e.printStackTrace();
			return;
		}

		long id = uMessage.getID();
		int width = uMessage.getWidth();
		int height = uMessage.getHeight();
		Log.d(TAG, "json msg identifier:" + uMessage.getMessageIdentifier());
		Log.d(TAG, "json message_request parameters id = " + id);
		Log.d(TAG, "json message_request parameters width =  " + width);
		Log.d(TAG, "json message_request parameters height =  " + height);

		Cursor imageCursor = getContentResolver().query(
				MediaStore.Images.Media.EXTERNAL_CONTENT_URI, mProjection,
				MediaStore.Images.Media._ID + " = " + id, null, null);
		if (imageCursor != null) {
			imageCursor.moveToFirst();

			String path = imageCursor.getString(imageCursor
					.getColumnIndex(MediaStore.Images.Media.DATA));
			orgWidth = imageCursor.getInt(imageCursor
					.getColumnIndex(MediaStore.Images.Media.WIDTH));
			orgHeight = imageCursor.getInt(imageCursor
					.getColumnIndex(MediaStore.Images.Media.HEIGHT));
			orgName = imageCursor.getString(imageCursor
					.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME));
			orgSize = imageCursor.getLong(imageCursor
					.getColumnIndex(MediaStore.Images.Media.SIZE));
			int ed = imageCursor.getInt(imageCursor
					.getColumnIndex(MediaStore.Images.Media._ID));
			
			if (!imageCursor.isClosed()) {
				imageCursor.close();
				imageCursor = null;
			}
			Log.d(TAG, "orginal  width = " + orgWidth);
			Log.d(TAG, "orginal  height = " + orgHeight);
			Log.d(TAG, "orginal  id  = " + ed);
			Log.d(TAG, "orginal  path  = " + path);
			Log.d(TAG, "orginal  name  = " + orgName);
			Log.d(TAG, "orginal  size  = " + orgSize);		
			
			pullDownscaledImg(path,width,height);
		
		} else {
			Log.d(TAG, "the image  with  id  invalid or deleted ,  id = " + id);
			mResult = "failure";// failure
			mReason = REASON_IMAGE_ID_INVALID; // invalid image ID.
		}
		
		sendImgRsp(connectedPeerId,id,orgName,orgSize,orgWidth,orgHeight);

	}
	
    /**
     * 
     * @param connectedPeerId
     * @param id
     * @param orgName
     * @param orgSize
     * @param orgWidth
     * @param orgHeight
     */
	private void sendImgRsp(String connectedPeerId, long id, String orgName,long orgSize, int orgWidth, int orgHeight){
		
		Log.d(TAG,"sendImgRsp : enter");
		TBModelJson msg = new TBModelJson(id, orgName, mImgData, orgSize, orgWidth,
				orgHeight);
		ImgRespMsg uresponse = new ImgRespMsg(mResult, mReason, msg);

		String uJsonStringToSend = "";
		try {
			uJsonStringToSend = uresponse.toJSON().toString();
		} catch (JSONException e) {
			Log.e(TAG, "sendDownscaledImage() Cannot Convert json to string");
			e.printStackTrace();
		}

		Log.d(TAG, "downscaled img rsp  size = " + uJsonStringToSend.length());
		if (mConnectionsMap != null) {
			SAGalleryProviderConnection uHandler = mConnectionsMap
					.get(Integer.parseInt(connectedPeerId));

			try {
				uHandler.send(GALLERY_CHANNEL_ID, uJsonStringToSend.getBytes());
			} catch (IOException e) {
				Log.e(TAG,"I/O Error occured while send");
				e.printStackTrace();
			}
		}
	}
	
    /**
     * 
     * @param peerAgent
     * @param result
     */
	@Override
	protected void onFindPeerAgentResponse(SAPeerAgent peerAgent, int result) {

/*		Log.i(TAG,
				"onPeerAgentAvailable: Use this info when you want provider to initiate peer id = "
						+ peerAgent.getPeerId());
		Log.i(TAG,
				"onPeerAgentAvailable: Use this info when you want provider to initiate peer name= "
						+ peerAgent.getAccessory().getName());*/
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
	public class SAGalleryProviderConnection extends
			SASocket {

		public static final String TAG = "SAGalleryProviderConnection";
		private int mConnectionId;

	    /**
	     * 
	     */
		public SAGalleryProviderConnection() {
			super(SAGalleryProviderConnection.class.getName());
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
