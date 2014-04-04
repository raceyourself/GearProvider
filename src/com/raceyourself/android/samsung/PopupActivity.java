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

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.glassfitgames.glassfitplatform.models.Device;
import com.raceyourself.android.samsung.ProviderService.LocalBinder;
import com.raceyourself.samsungprovider.R;

public class PopupActivity extends Activity {
    public static final String TAG = "PopupActivity";

    private ProviderService mService;
    private boolean mBound = false;

    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
          
        //makes full screen and takes away title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        setContentView(R.layout.activity_splash);
        
//        Intent service = new Intent(this, ProviderService.class);
//        startService(service); 
// TODO: Finish as soon as service is started in some cases
//        finish();
                
        setContentView(R.layout.activity_main);
        setTabColour(R.id.textview1, "#d4358a");
        setTabColour(R.id.textview2, "#6666ff");
        setTabColour(R.id.textview3, "#47ad4c");
    }
    
    private void setTabColour(int id, String colour) {
        View btn = findViewById(id);
        StateListDrawable sel = (StateListDrawable)btn.getBackground();
        LayerDrawable ld = (LayerDrawable)sel.getCurrent();
        GradientDrawable tab = (GradientDrawable)ld.findDrawableByLayerId(R.id.button_tab);
        tab.setColor(Color.parseColor(colour));
    }
        
    public void onHowtoClick(View view) {
        Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.raceyourself.com/gear#howto"));
        myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(myIntent);
    }
    
    public void onConnectionClick(View view) {
        if (mBound) {
            Log.e("TAG", "Bluetooth: " + mService.hasBluetooth() + ", gear: " + mService.hasGear());
            boolean connected = mService.hasBluetooth() && mService.hasGear();
            setConnectedStyle(connected);
            if (!connected) mService.runUserInit();
        } else {
            Log.e("TAG", "Service not bound");
        }
    }
    
    public void onFeedbackClick(View view) {
        String device = "unregistered device";
        try {
            Device self = Device.self();
            if (self != null) device = "device " + self.getId();
        } catch (Throwable t) {
            Log.e(TAG, "Could not get device" , t);
        }
        
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto","support@raceyourself.com", null));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Gear support request for " + device);
        startActivity(Intent.createChooser(emailIntent, "Send support email.."));
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, ProviderService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    private void setConnectedStyle(boolean connected) {
        if (connected) {
            TextView tv = (TextView)findViewById(R.id.textview1);
            tv.setText(R.string.connected);
            tv.setTextColor(Color.parseColor("#00FF00"));
        } else {
            TextView tv = (TextView)findViewById(R.id.textview1);
            tv.setText(R.string.connect);
            tv.setTextColor(Color.parseColor("#FF0000"));
        }
    }
    
    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocalBinder binder = (LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            Log.i(TAG, "Activity bound to service");
            setConnectedStyle(mService.hasBluetooth() && mService.hasGear());
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.i(TAG, "Activity unbound to service");
            mBound = false;
        }
    };    
    
}
