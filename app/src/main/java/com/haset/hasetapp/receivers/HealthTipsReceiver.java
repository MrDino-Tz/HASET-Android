package com.haset.hasetapp.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.haset.hasetapp.utils.HealthTipsHelper;

public class HealthTipsReceiver extends BroadcastReceiver {
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("HealthTipsReceiver", "Health tip alarm received");
        
        if (intent != null) {
            String tipType = intent.getStringExtra("tip_type");
            int tipId = intent.getIntExtra("tip_id", -1);
            
            if (tipType != null && tipId != -1) {
                HealthTipsHelper healthTipsHelper = new HealthTipsHelper(context);
                healthTipsHelper.showHealthTip(tipType, tipId);
                healthTipsHelper.saveLastTipDate();
                
                Log.d("HealthTipsReceiver", "Health tip displayed: " + tipType);
            } else {
                Log.e("HealthTipsReceiver", "Invalid intent data received");
            }
        } else {
            Log.e("HealthTipsReceiver", "Null intent received");
        }
    }
}
