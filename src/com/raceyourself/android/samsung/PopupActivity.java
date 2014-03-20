package com.raceyourself.android.samsung;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class PopupActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Intent service = new Intent(this, ProviderService.class);
        startService(service);
        finish();
    }
    
}
