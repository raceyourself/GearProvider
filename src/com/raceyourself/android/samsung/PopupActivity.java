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
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.glassfitgames.glassfitplatform.models.Device;
import com.raceyourself.samsungprovider.R;

public class PopupActivity extends Activity {
    public static final String TAG = "PopupActivity";
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
          
        //makes full screen and takes away title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        setContentView(R.layout.activity_splash);
        
        Intent service = new Intent(this, ProviderService.class);
        startService(service); 
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
        Log.e("TAG", "Connection");
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
    
}
