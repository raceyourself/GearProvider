/**
 * Copyright (c) 2014 RaceYourself Inc
 * All Rights Reserved
 *
 * No part of this application or any of its contents may be reproduced, copied, modified or 
 * adapted, without the prior written consent of the author, unless otherwise indicated.
 *
 * Commercial use and distribution of the application or any part is not allowed without 
 * express and prior written consent of the author.
 *
 * The application makes use of some publicly available libraries, some of which have their 
 * own copyright notices and licences. These notices are reproduced in the Open Source License 
 * Acknowledgement file included with this software.
 * 
 */


package com.raceyourself.android.samsung;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Base64;
import android.util.Log;
import android.view.WindowManager;

import com.glassfitgames.glassfitplatform.gpstracker.GPSTracker;
import com.glassfitgames.glassfitplatform.gpstracker.Helper;
import com.glassfitgames.glassfitplatform.gpstracker.SyncHelper;
import com.glassfitgames.glassfitplatform.models.Device;
import com.glassfitgames.glassfitplatform.models.Preference;
import com.glassfitgames.glassfitplatform.models.RemoteConfiguration;
import com.raceyourself.android.samsung.models.GpsPositionData;
import com.raceyourself.android.samsung.models.GpsStatusResp;
import com.raceyourself.android.samsung.models.RemoteConfigurationResp;
import com.raceyourself.android.samsung.models.SAModel;
import com.raceyourself.android.samsung.models.WebLinkReq;
import com.raceyourself.samsungprovider.R;
import com.roscopeco.ormdroid.ORMDroidApplication;
import com.samsung.android.sdk.accessory.SAAgent;
import com.samsung.android.sdk.accessory.SAPeerAgent;
import com.samsung.android.sdk.accessory.SASocket;

public class ProviderService extends SAAgent {
	
    public static final String TAG = "RaceYourselfProvider";
    public final int DEFAULT_CHANNEL_ID = 104;
    private final String SERVER_TOKEN = "3hrJfCEZwQbACyUB";
    private static final String EULA_KEY = "EulaAccept";
    private static final String DISCLAIMER_KEY = "DisclaimerAccept";

	private final IBinder mBinder = new LocalBinder();
	private static HashMap<Integer, RaceYourselfSamsungProviderConnection> mConnectionsMap = new HashMap<Integer, RaceYourselfSamsungProviderConnection>();
	private static GPSTracker gpsTracker = null;
	private static GpsDataSender gpsDataSender = null;
	private Timer timer = new Timer();
	
	private AlertDialog alert;
	private AlertDialog waitingAlert;
	private boolean initialisingInProgress = false;
	
	private boolean registered = false; // have we registered the device with the server yet? Required for inserting stuff into the db.
	private Thread deviceRegistration = null;
	
	private final int TETHER_NOTIFICATION_ID = 1;
	private boolean iconEnabled = false;

	public class LocalBinder extends Binder {
		public ProviderService getService() {
			return ProviderService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	@Override
    public boolean onUnbind(Intent intent) {
        stopTracking();
        return false;
    }
	
	/**
	 *  Called when the service is started, even if already running
	 */
	@Override
	public int onStartCommand(Intent i, int j, int k) {
        
        ORMDroidApplication.initialize(this);  // init the database
        
        // check the user has done everything they need to
        // separate thread to allow onCreate() to complete and SAP to 
        // deploy the wgt to gear.
        // Don't trigger if init is already in progress, or we're already connected to gear
        if (!initialisingInProgress && mConnectionsMap.isEmpty()) {
            new Handler().post(new Runnable() {
                public void run() {
                    runUserInit();
                }
            });
        }
        // make sure we have a record for the user
        Helper.getUser();
        
        int result = super.onStartCommand(i,j,k);
        
        Log.v(TAG, "service created");
        
        return result;
    }
	
	
	public void runUserInit() {
	    
	    Log.d(TAG, "Running user init");
	    initialisingInProgress = true;
	    
	    // check EULA
	    Boolean eulaAccept = Preference.getBoolean(EULA_KEY);
        if (eulaAccept == null || !eulaAccept.booleanValue()) {
            popupEula();
            return;
        }
        
        // register with server
        if (!ensureDeviceIsRegistered()) {
            popupNetworkDialog();
            return;
        }
        
        // check bluetooth - if they want to launch the app now
        if (!Helper.getInstance(this).isBluetoothBonded()) {
            popupBluetoothDialog();
            return;
        }
        
        // check the gear app is running
        
        if (mConnectionsMap.isEmpty()) {
            popupWaitingForGearDialog();
            return;
        }
        
        Log.d(TAG, "User init completed successfully");
        initialisingInProgress = false;
        
        // do in background
        if(registered){
            authorize();
            trySync();
        } 
	}
	
	@Override
	public void onLowMemory() {
		Log.e(TAG, "onLowMemory  has been hit better to do  graceful  exit now");
//		Toast.makeText(getBaseContext(), "!!!onLowMemory!!!", Toast.LENGTH_LONG)
//		.show();
		closeConnection();
		super.onLowMemory();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Helper.getInstance(this).destroy();  // unregister intent receivers etc
		Log.i(TAG, "Service Stopped.");
	}

	public ProviderService() {
		super(TAG, RaceYourselfSamsungProviderConnection.class);
	}

	public boolean closeConnection() {
	    
	    Log.d(TAG, "closeConnection called");

		if (mConnectionsMap != null) {
			List<Integer> listConnections = new ArrayList<Integer>(
					mConnectionsMap.keySet());
			if (listConnections != null) {
				for (Integer s : listConnections) {
					Log.i(TAG, "KEYS found are" + s);
					mConnectionsMap.get(s).close();
					mConnectionsMap.remove(s);
				}
				
				// if no connections left
				if (mConnectionsMap.isEmpty()) {
				    // stop sending updates
				    Log.d(TAG, "No connections remaining, destroying GPS tracker");
				    stopTracking();
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
     * 
     * Based on Samsung sample app by s.amit
     * 
     */
	@Override
	protected void onServiceConnectionResponse(SASocket uThisConnection, int result) {
		if (result == CONNECTION_SUCCESS) {
			if (uThisConnection != null) {
			    
			    RaceYourselfSamsungProviderConnection myConnection = (RaceYourselfSamsungProviderConnection) uThisConnection;
			    Log.d(TAG,"onServiceConnection connectionID = "+myConnection.mConnectionId);
				
				myConnection.mConnectionId = (int) (System.currentTimeMillis() & 255);
				mConnectionsMap.put(myConnection.mConnectionId, myConnection);
				
				// Enabled 'tethered' icon
				enableIcon();

				// make sure we have a device ID (initially from the server) - req for writes to db
				ensureDeviceIsRegistered();
				
				// init GPS tracker to start searching for position
				ensureGps();
				
				try {
				    waitingAlert.cancel();
				} catch (Exception e) {
				    // waiting Alert may have been null, or not popped up,
				    // in which case don't worry
				}
				
		        popupSuccessDialog();
				
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
		
		ensureDeviceIsRegistered();
		
		// decide what to do based on the message
		if (data.contains(SAModel.GPS_STATUS_REQ)) {
		    ensureGps();
		    if (gpsTracker.hasPosition()) {
		        response = new GpsStatusResp(GpsStatusResp.GPS_READY);
		    } else if (gpsTracker.isGpsEnabled()) {
		        response = new GpsStatusResp(GpsStatusResp.GPS_ENABLED);
		    } else {
		        response = new GpsStatusResp(GpsStatusResp.GPS_DISABLED);
		        popupGpsDialog();
		    }
		} else if (data.contains(SAModel.START_TRACKING_REQ)) {
		    startTracking();
		} else if (data.contains(SAModel.STOP_TRACKING_REQ)) {
		    stopTracking();
		} else if (data.contains(SAModel.AUTHENTICATION_REQ)) {
		    // usually called when the user launches the app on gear
		    authorize();
		    trySync();
		    // Popup webview with user/passwd boxes
		    //Intent authenticationIntent = new Intent (this, AuthenticationActivity.class);
		    //authenticationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		    //authenticationIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		    //startActivity(authenticationIntent);
		} else if (data.contains(SAModel.LOG_ANALYTICS)) {
            try {
                JSONObject json = new JSONObject(data);
                Helper.logEvent(json.getString("value"));
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing analytics event", e);
            }

		} else if (data.contains(SAModel.WEB_LINK_REQ)) {
		    JSONObject json;
            try {
                json = new JSONObject(data);
                String uri = WebLinkReq.fromJSON(json).getUri();
                launchWebBrowser(uri);
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing WebLinkReq", e);
            }
        } else if (data.contains(SAModel.REMOTE_CONFIGURATION_REQ)) {
            RemoteConfiguration config = SyncHelper.get("configurations/gear", RemoteConfiguration.class);
            if (config != null) response = new RemoteConfigurationResp(config.configuration);
            
		} else {
			Log.e(TAG, "onDataAvailableonChannel: Unknown request received");
		}
		
		// send the response
		if (response != null) {
		    send(connectedPeerId, response);
		}
	}
	
	private void popupBluetoothDialog() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		
		builder.setMessage("Your bluetooth is not enabled.\n\nPlease enable, connect to Gear and press retry.")
			   .setCancelable(false)
			   .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					//Intent bluetooth = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
					//bluetooth.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					//startActivity(bluetooth);
					
					// continue with init
					runUserInit();
				}
			})
			.setNegativeButton("Quit", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
					// TODO Auto-generated method stub
					dialog.cancel();
				}
			})
            .setTitle("RaceYourself Gear Edition");
		
		alert = builder.create();
		alert.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
		alert.show();
	}
	
	private void popupGpsDialog() {
	    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("RaceYourself works best with GPS.\n\nWould you like to enable your GPS now?")
               .setCancelable(false)
               .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                   public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, 
                                       @SuppressWarnings("unused") final int id) {
                	   Intent gps = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                	   gps.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                       startActivity(gps);
                   }
               })
               .setNegativeButton("No", new DialogInterface.OnClickListener() {
                   public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                   }
               })
               .setTitle("RaceYourself Gear Edition");
        alert = builder.create();
        alert.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        alert.show();
	}
	
	private void popupEula() {
	    
	    final String message = new String("End-user license agreement and disclaimer.\n\nBy clicking accept you agree to abide by RaceYourself's terms and conditions of use. You also agree to take full responsibility for you own safety whilst using RaceYourself, and accept that RaceYourself will not be held liable for any personal injury or illness sustained through use of this application.\n\nYou agree to the full EULA and Disclaimers which can be viewed online:\n\nhttp://www.raceyourself.com/gear/#eula\n\nhttp://www.raceyourself.com/gear/#disclaimer");
	    //Linkify.addLinks(message, Linkify.WEB_URLS);
	    
	    //final TextView view = new TextView(this);
	    //view.setText(message);
	    //view.setMovementMethod(LinkMovementMethod.getInstance());
	    //view.setOnClickListener(new OnClickListener() {
	    //    public void onClick(View onClick) {                 
	    //        launchWebBrowser("http://www.raceyourself.com/gear/#eula");
	    //    }
	    //});
	    
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
               .setCancelable(true)
               .setPositiveButton("Agree", new DialogInterface.OnClickListener() {
                   public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, 
                                       @SuppressWarnings("unused") final int id) {
                       Preference.setBoolean(EULA_KEY, Boolean.TRUE);
                      
                       // continue with init
                       new Handler().post(new Runnable() {
                           public void run() {
                               runUserInit();
                           }
                       });
                   }
               })
               /* The following doesn't allow synchronous return to the dialog. Probably need an activity here.
                * TODO: fix this so the link is clickable
                * .setNeutralButton("Details", new DialogInterface.OnClickListener() {
                   public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        // link to website
                       launchWebBrowser("http://www.raceyourself.com/gear/#eula");
                   }
               })*/
               .setNegativeButton("Decline", new DialogInterface.OnClickListener() {
                   public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        ProviderService.this.stopSelf();
                   }
               })
               .setTitle("RaceYourself Gear Edition");
        alert = builder.create();
        alert.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        alert.show();
    }
	
	private void popupDisclaimer() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("RaceYourself Gear Edition")
               .setMessage("Disclaimer\n\nBy clicking accept you agree to take full responsibility for you own safety whilst using RaceYourself, and accept that RaceYourself will not be held liable for any personal injury or illness sustained through use of this application.\n\nYou agree to the full RaceYourself disclaimer which can be viewed online at https://www.raceyourself.com/gear/#disclaimer")
               .setCancelable(false)
               .setPositiveButton("Accept", new DialogInterface.OnClickListener() {
                   public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, 
                                       @SuppressWarnings("unused") final int id) {
                        Preference.setBoolean(DISCLAIMER_KEY, Boolean.TRUE);

                        // continue with init
                        new Handler().post(new Runnable() {
                            public void run() {
                                runUserInit();
                            }
                        });
                   }
               })
               .setNegativeButton("Decline", new DialogInterface.OnClickListener() {
                   public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        ProviderService.this.stopSelf();
                   }
               });
        alert = builder.create();
        alert.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        alert.show();
    }
	
	private void popupNetworkDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("RaceYourself needs to connect to the internet to register your device. Please check your connection and press retry.")
               .setCancelable(false)
               .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                   public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, 
                                       @SuppressWarnings("unused") final int id) {
                       runUserInit();
                   }
               })
               .setNegativeButton("Quit", new DialogInterface.OnClickListener() {
                   public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        ProviderService.this.stopSelf();
                   }
               })
               .setTitle("RaceYourself Gear Edition");
        alert = builder.create();
        alert.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        alert.show();
    }
	
    private void popupWaitingForGearDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("RaceYourself Gear Edition")
                .setMessage(
                        "Please launch RaceYourself on Gear.\n\nIf you have just installed RaceYourself, the icon may take a few moments to appear. \n\nIf you are waiting a while, make sure UnifiedHostManager is connected to gear or try disabling/re-enabing bluetooth on Gear.")
                .setCancelable(false)
                .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog,
                            @SuppressWarnings("unused") final int id) {
                        new Handler().post(new Runnable() {
                            public void run() {
                                runUserInit();
                            }
                        });
                    }
                }).setNegativeButton("Quit", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog,
                            @SuppressWarnings("unused") final int id) {
                        ProviderService.this.stopSelf();
                    }
                });
        waitingAlert = builder.create();
        waitingAlert.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        waitingAlert.show();
    }
	
	private void popupSuccessDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("RaceYourself Gear Edition")
               .setMessage("Connected to Gear.\n\nRaceYourself is ready to go!\n\nIt's important to keep Gear connected to your phone when you workout, so RaceYourself can use the GPS in your phone.")
               .setCancelable(false)
               .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                   public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, 
                                       @SuppressWarnings("unused") final int id) {
                        // nothing - just dismiss dialog
                   }
               })
               .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                   public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                       // nothing - just dismiss dialog
                   }
               });
        alert = builder.create();
        alert.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        alert.show();
    }

	
	private void send(String connectedPeerId, SAModel message) {
	    RaceYourselfSamsungProviderConnection conn = mConnectionsMap.get(Integer.parseInt(connectedPeerId));
        try {
            Log.d(TAG, "Sending message on channel " + DEFAULT_CHANNEL_ID + ": " + message.toJSON().toString());
            conn.send(DEFAULT_CHANNEL_ID, message.toJSON().toString().getBytes());
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "IllegalArgumentException sending SAP message", e);
        } catch (IOException e) {
            Log.e(TAG, "IOException sending SAP message", e);
        } catch (JSONException e) {
            Log.e(TAG, "JSONException sending SAP message", e);
        }
	}
	
	private boolean ensureDeviceIsRegistered() {
	    if (registered) {
	        // registered, and we know about it locally
	        return true;
	    } else if (Device.self() != null) {
	        // registered previously, update local variable
	        registered = true;
	        return true;
	    } else if (Helper.getInstance(this).hasInternet()){
	        if (deviceRegistration != null && deviceRegistration.isAlive()) {
	            return true;
	        }
	        // not yet registered, attempt to register
            deviceRegistration = new Thread(new Runnable() {
                @Override
                public void run() {
                        
                    try {
                    	Device self = SyncHelper.registerDevice();
                        self.self = true;
                        self.save();
                        registered = true;
                        
                        authorize();
       					trySync();
       					
                    	} catch (IOException e) {
                    		Log.e(TAG, "Error registering device", e);
                    }
                }
            });
            deviceRegistration.start();
            return true;
        } else {
            // not yet registered, no internet, need to prompt user
            return false;
        }
	}
	
	private void ensureGps() {
	    // start listening for GPS updates
	    Log.d(TAG,"ensureGps called");
        if (gpsTracker == null) gpsTracker = new GPSTracker(this);
        gpsTracker.setIndoorMode(false);
        gpsTracker.onResume();
	}
	
	public boolean hasBluetooth() {
	    return Helper.getInstance(this).isBluetoothBonded();
	}
    
	public boolean hasGear() {
	    return !mConnectionsMap.isEmpty();
	}
	
    private void startTracking() {

        Log.d(TAG,"startTracking called");
        ensureGps();
        gpsTracker.startTracking();
        
        if (gpsDataSender == null) {
            Log.d(TAG, "Starting to send regular GPS data messages");
            gpsDataSender = new GpsDataSender();
            timer.scheduleAtFixedRate(gpsDataSender, 0, 500);
        }
    }

    private void stopTracking() {
	    
	    // stop sending updates
        Log.d(TAG,"stopTracking called");
	    if (gpsDataSender != null) {
            Log.d(TAG, "Stopping regular GPS data messages");
            gpsDataSender.cancel();
            gpsDataSender = null;
        }
	    
	    // stop listening for GPS/sensors
	    if (gpsTracker != null) {
	        gpsTracker.stopTracking();
	        gpsTracker.onPause();
	    }
        trySync();  // need to sync every now and then. End of each race seems reasonable. It runs in a background thread.
	}
	
	private void ensureInternet() {
	    if (!Helper.getInstance(this).hasInternet()) {
            popupNetworkDialog();
        }
	}
	
	private void authorize() {
    	AccountManager mAccountManager = AccountManager.get(this);
    	List<Account> accounts = new ArrayList<Account>();
    	accounts.addAll(Arrays.asList(mAccountManager.getAccountsByType("com.google")));
    	accounts.addAll(Arrays.asList(mAccountManager.getAccountsByType("com.googlemail")));
        String email = null;
        for (Account account : accounts) {
            if (account.name != null && account.name.contains("@")) {
                email = account.name;
                break;
            }
        }
        // Potential fault: Can there be multiple accounts? Do we need to sort or provide a selector?
       
        // hash email so we don't send user's identity to server
        // can't guarantee uniqueness but want very low probability of collisions
        // using SHA-256 means we'd expect a collision on approx. our 1-millionth user
        // TODO: make this more unique before Samsung sell 1m Gear IIs.
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(email.getBytes());
            String hash = new String(messageDigest.digest());
            hash = new String(Base64.encode(hash.getBytes(), Base64.DEFAULT)).replace("@", "_").replace("\n", "_");  //base64 encode and substitute @ symbols
            hash += "@hashed.raceyourself.com";  // make it look like an email so it passes server validation
            Helper.login(hash, SERVER_TOKEN);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Implementation of SHA-256 algorithm not found. Authorisation failed. Exiting.");
            throw new RuntimeException();
        }
	}
	
	private void trySync() {
	    if (Helper.getInstance(this).hasInternet()) {
	        authorize();
	        SyncHelper.getInstance(this).start();
	        // if wither of these fail, they dump a stack trace to the log and execution continues
	    }
	    // if no internet, don't bother
	}
	
	private class GpsDataSender extends TimerTask {
        public void run() {
            if (gpsTracker.hasPosition()) {
                Log.d(TAG, "Sending new position over SAP");
                SAModel gpsData = new GpsPositionData(gpsTracker);
                // send to all connected peers
                for (RaceYourselfSamsungProviderConnection c : mConnectionsMap.values()) {
                    send(String.valueOf(c.mConnectionId), gpsData);
                }
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
	
	private void launchWebBrowser(String uri) {
	    Log.i(TAG, "Launching browser for URI: " + uri);
	    Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(myIntent);
	}
	
	/**
     * Service connection
     * Based on Samsung sample app by s.amit
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
				    if (mConnectionsMap.isEmpty()) {
				        // Disable 'tethered' icon
				        disableIcon();
	                    // turn off GPS tracker and sensor service
	                    Log.d(TAG, "No connections remaining, destroying GPS tracker");
	                    stopTracking();
	                }

            }

        }

    }
	
	private void enableIcon() {
	    if (iconEnabled) return;
        iconEnabled = true;
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_message));
        
        Intent intent = new Intent(this, PopupActivity.class);
        PendingIntent resultPendingIntent =
            PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        );        
        mBuilder.setContentIntent(resultPendingIntent);
        
        // Gets an instance of the NotificationManager service
        NotificationManager mNotifyMgr = 
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // Builds the notification and issues it.
        mNotifyMgr.notify(TETHER_NOTIFICATION_ID, mBuilder.build());
	}
	
	private void disableIcon() {
	    if (!iconEnabled) return;
        // Gets an instance of the NotificationManager service
        NotificationManager mNotifyMgr = 
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.cancel(TETHER_NOTIFICATION_ID);
        iconEnabled = false;
	}
}
