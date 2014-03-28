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
